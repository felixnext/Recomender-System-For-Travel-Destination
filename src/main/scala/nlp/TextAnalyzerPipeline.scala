package nlp

import java.util

import clavin.{ClavinClient, Location}
import dbpedia.{DBPediaLookup, LookupResult, SpotlightClient, SpotlightResult}
import edu.stanford.nlp.dcoref.CorefChain
import edu.stanford.nlp.trees.Tree
import elasticsearch.ElasticsearchClient

import scala.annotation.tailrec
import scala.collection.convert.wrapAsScala._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.math._


/**
 * Analyzes the text with basic annotators. The analyze is done in parallel.
 */
trait TextAnalyzerPipeline {

  //initialize required resources
  val relationExtractor = new RelationExtractor
  val clavin = new ClavinClient
  val stanford = new StanfordAnnotator
  val spotlight = new SpotlightClient
  val dbpediaLookup = new DBPediaLookup

  //annotates text with four annotators:
  //StanfordNLP, RelationExtraction (Opnie), Clavin gazetteer and Spotlight
  def analyzeText(text: String): Future[AnnotatedText] = {
    //process text
    //val relations = future{relationExtractor.extractRelations(text)}

    val clavinAnnotation = future {
      clavin.extractLocations(text)
    }
    val stanfordAnnotation = future {
      stanford.annotateText(text)
    }

    //spotlight analysis
    val spotlightAnnotation = future {
      spotlight.discoverEntities(text)
    }

    val rawRel = future{
      relationExtractor.extractRelations(text)
    }

    //split relations into subsets, each subset corresponds to one sentence
    val relations: Future[Array[Seq[Relation]]] = for {
      s <- stanfordAnnotation
      r <- rawRel
    } yield {
      val sentenceBoundaries = s.tokenizedSentences.foldLeft(0,List[(Int,Int)]())((t,s) =>
        (t._1 + s.length + 1, t._2 :+ (t._1,t._1 + s.length) ))._2
      val groups: Map[(Int,Int),Seq[Relation] ] = sentenceBoundaries.map(x => x -> Seq[Relation]()).toMap

      val occupiedGroups = r.foldLeft(groups){
        (tmpGroups,x) =>
          val minOffset = Seq(x.arg1.argOffset._1,x.relOffset._1, x.arg2.map(x => x.argOffset._1).min).min
          val maxOffset = Seq(x.arg1.argOffset._2,x.relOffset._2, x.arg2.map(x => x.argOffset._2).max).max
          val newGroups = tmpGroups.keySet.find(x => x._1.<=(minOffset) && maxOffset < x._2) match {
            case Some(key) => tmpGroups + (key -> (tmpGroups.getOrElse(key,Seq()) :+ x) )
            case _ => tmpGroups
          }
          newGroups
      }
      occupiedGroups.map{case (k,v) => v}.toArray
    }


    //if some future fails print error message
    for (e <- spotlightAnnotation.failed) println("Relation extraction failed. Cannot create sparql query" + e)
    for (e <- relations.failed) println("Relation extraction failed. Cannot create sparql query" + e)
    for (e <- clavinAnnotation.failed) println("Relation extraction failed. Cannot create sparql query" + e)
    for (e <- stanfordAnnotation.failed) println("Relation extraction failed. Cannot create sparql query" + e)

    val annText = for {
      r <- relations
      c <- clavinAnnotation
      stfrd <- stanfordAnnotation
      spot <- spotlightAnnotation
    } yield {
        new AnnotatedText(r, c, stfrd, spot)
      }

    //return future annotation
    annText
  }

}

/**
 * Takes a text and creates corresponding sparql query w.r.t focus of text.
 * Assumption the focus is a certain location.
 */
class SparqlQueryCreator extends TextAnalyzerPipeline {

  val elastic = new ElasticsearchClient

  def createSprqlQuery(text: String): Unit = {


    //clavin, stanford and opnie
    val annotatedText = analyzeText(text)
    for (e <- annotatedText.failed) println("Text annotation failed. Cannot create sparql query" + e)

    //split each word in sentece on "/". This converts words form word/pos into tuple (word,pos)
    val tokenizedSentensesPos:Future[Array[Array[(String, String)]]] = annotatedText.map{ x =>
      x.stanford.sentencesPos.map { x =>
        x.split(" ").map { x =>
          val split = x.split("/")
          try {
            (split(0), split(1))
          } catch {
            case e: Exception => (split(0), "")
          }
        }
      }
    }


    //replaces stanford pos tags with patty tags
    val posRelations = for{
      ann <- annotatedText
      s <- tokenizedSentensesPos
    } yield {
        posRelAnnotation(s, ann.relations)
      }


    for (e <- posRelations.failed) println("POS annotation failed. Cannot create sparql query" + e)

    //maps raw relations into patty dbpedia predicates
    val pattyRelations = posRelations.map { posRel =>
      posRel.map(sentence => sentence.map(relation => elastic.findPattyRelation(relation.rel)))
    }
    for (e <- pattyRelations.failed) println("Patty relation retrival failed. Cannot create sparql query" + e)

    val entityCAndidatesAnnotation = for {
      p <- posRelations
      s <- tokenizedSentensesPos
      ann <- annotatedText
    } yield {
        createEntityCandidates(p, ann.spotlight, ann.clavin, ann.stanford.coreference, s)
      }
    for (e <- entityCAndidatesAnnotation.failed) println("Candidate set creation failed. Cannot create sparql query" + e)

    Await.result(entityCAndidatesAnnotation, 1000 seconds).map.foreach{
      x => println("\n\n" + x._1)
        x._2.foreach(x => println(x))
    }

    //TODO finds focus with coreference and extend entities candidates

    //TODO extand entity candidates with clavin and spotlight

    //TODO the unkown entities should be searched with dbpedia lookup

    //TODO combine patty predicates and annotated entities

    //TODO create query


  }


  //Takes relations and replace predicates with patty pos tags
  def posRelAnnotation(sentences: Array[Array[(String, String)]], relations: Array[Seq[Relation]]): Array[Seq[Relation]] = {

    //Annotate with patty tag names
    //sentence: Seq[(Word,Tag)]
    def annotatePos(relation: Relation, sentence: Array[(String, String)]) = {
      val mapToPattyTags = Map("CD" -> "[[num]]", "DT" -> "[[det]]", "PRP" -> "[[prp]]",
        "JJ" -> "[[adj]]", "MD" -> "[[mod]]", "IN" -> "[[con]]", "CC" -> "[[con]]")

      val relationWords = relation.rel.split(" ")
      val slidingOverSentence = sentence.sliding(relationWords.size)

      //if contains patty tag, than replace word with pos tag else take a word
      val posTaggedRelationList: Iterator[Array[String]] = for (subSentence <- slidingOverSentence
                                                                if subSentence.map(t => t._1).sameElements(relationWords))
        yield {
        subSentence.map(t =>  mapToPattyTags.getOrElse(t._2, t._1))
      }

      val relTagged = if (posTaggedRelationList.isEmpty) relation.rel
      else posTaggedRelationList.next().reduce((a,b) => a + " " + b)

      new Relation(relation.arg1, relTagged, relation.relOffset, relation.arg2)
    }

    //iterate over all sentence and relations
    val posRelations: Array[Seq[Relation]] = for (sr <- sentences.zip(relations)) yield {
      val sentence = sr._1
      val relation = sr._2
      relation.map { r => annotatePos(r, sentence) }
    }

    posRelations
  }


  def createEntityCandidates(relations: Array[Seq[Relation]], spotlightResult: List[SpotlightResult],
                             clavinResult: List[Location], coreference: util.Map[Integer, CorefChain],
                             sentences: Array[Array[(String, String)]]): RelationTree = {
    //get all key of coref clusters
    //value coresponds to cluster id
    val keys: java.util.Set[Integer] = coreference.keySet()

    def intersect(aStartEnd: (Int,Int) , bStartEnd: (Int,Int) ) = max(aStartEnd._1, bStartEnd._1) < min(aStartEnd._2, bStartEnd._2)

    //creates a new RelationTree object, fills the object with relations and groups relations with coreference argument together
    @tailrec
    def traverseRelationSentences(iterator: Iterator[Seq[Relation]], sentenceCounter: Int = 1,
                                  tree: RelationTree = new RelationTree): RelationTree = {
      if (!iterator.hasNext) tree
      else {
        val senteceRel = iterator.next()

        //TODO Yago

        val sentencesRaw: Array[Array[String]] = sentences.map(x => x.map(x => x._1))
        val charsPerSentence: Array[Int] = sentencesRaw.map(x => x.length - 1 + x.foldLeft(0)((l,c) => l + c.length))

        //converts stanford sentence and tooken indices into char offset, counted from beginning of the text
        def calculateOffset(sentenceNr: Int, tokenBegin: Int, tokenEnd: Int, token: String): (Int,Int) = {
          try{
            val sentenceOffset = charsPerSentence.dropRight(charsPerSentence.length + 1 - sentenceNr).sum
            val startOffset = sentencesRaw(sentenceNr - 1).slice(0,tokenBegin - 1).map(a => a.length).
              foldLeft(0)((a,b) => a + b + 1) + sentenceOffset
            val endOffsset = sentencesRaw(sentenceNr - 1).slice(tokenBegin - 1, tokenEnd -1).map(a => a.length)
              .foldLeft(0)((a,b) => a + b + 1) + startOffset
            (startOffset,endOffsset)
          } catch {
            case e: Exception => (-1,-1)
          }
        }

        //relation loop
        for (rel <- senteceRel) {

          //if arg1 of relation is in coref than corefID else -1
          //List(1,2,1,-1,-1,-1)
          val keysInRelation = for (key <- keys) yield {
            val c: CorefChain = coreference.get(key)
            val cms: java.util.List[CorefChain.CorefMention] = c.getMentionsInTextualOrder
            val r: Int = cms.find(cm => cm.sentNum == sentenceCounter &&
              intersect(calculateOffset(cm.sentNum, cm.startIndex, cm.endIndex, cm.mentionSpan),
                rel.arg1.argOffset) ) match {
              case Some(x) => key.intValue()
              case None => -1 //Default cluster id
            }
            r
          }

          //converts List(1,2,1,-1,3,-1) to set Set(1,2,3,-1)
          val reducedClusterIds = keysInRelation.foldLeft(Set(-1))((a, b) => a + b)

          //annotates with clavin and spotlight result if exists
          val annotatedRel = {

            val spotlightArg1 = spotlightResult.find(x =>
              intersect((x.offset, x.offset + x.surfaceForm.length), (rel.arg1.argOffset._1, rel.arg1.argOffset._2)))

            val locationArg1 = clavinResult.find(x =>
              intersect((x.offset, x.offset + x.asciiName.length), (rel.arg1.argOffset._1, rel.arg1.argOffset._2)))

            //if neither spotlight now clavin annotation known
            //then try dbpedia lookup
            val lookupRes = if(!(locationArg1.isDefined || spotlightArg1.isDefined) ) Some(dbpediaLookup.findDBPediaURI(rel.arg1.arg))
            else None

            val arg1 = new AnnotatedArgument(spotlight = spotlightArg1, clavin = locationArg1, arg = rel.arg1.arg,
              argType = rel.arg1.argType, argOffset = rel.arg1.argOffset, dbpediaLookup = lookupRes)

            val arg2: Seq[AnnotatedArgument] = for(arg2 <- rel.arg2) yield {
              val spotlightArg2 = spotlightResult.find(x =>
                intersect((x.offset, x.offset + x.surfaceForm.length), (arg2.argOffset._1, arg2.argOffset._2)))
              val locationArg2 =  clavinResult.find(x =>
                intersect((x.offset, x.offset + x.asciiName.length), (arg2.argOffset._1, arg2.argOffset._2)))

              val lookupRes = if(!(spotlightArg2.isDefined || locationArg2.isDefined) ) Some(dbpediaLookup.findDBPediaURI(arg2.arg))
              else None

              new AnnotatedArgument(spotlight = spotlightArg2, clavin = locationArg2,
                arg = arg2.arg, argType = arg2.argType, argOffset = arg2.argOffset, dbpediaLookup = lookupRes)
            }

            new AnnontatedRelation(arg1, rel.rel, rel.relOffset , arg2)
          }

          if (reducedClusterIds.size == 1) {
            //No coreference found
            val relSet = tree.map.getOrElse(-1, Set()) + annotatedRel
            tree.map.+=(-1 -> relSet)
          } else {
            //some coreference found
            (reducedClusterIds - -1).foreach {
              case id => val relSet = tree.map.getOrElse(id, Set()) + annotatedRel
                tree.map.+=(id -> relSet)
            }
          }

        }

        traverseRelationSentences(iterator, sentenceCounter + 1, tree)
      }
    }



    def containsTest(a: String, b: String) = if (a.length < b.length) b.contains(a) else a.contains(b)

    val tree = traverseRelationSentences(relations.iterator)

    tree
  }

}

//relations are annotated per sentence.
case class AnnotatedText(relations: Array[Seq[Relation]], clavin: List[Location], stanford: StanfordAnnotation,
                         spotlight: List[SpotlightResult])

/**
 * It is a container for stanford core nlp annotation.
 */
case class StanfordAnnotation(sentimentTree: Array[Tree], sentencesPos: Array[String], coreference: util.Map[Integer,
  CorefChain], tokenizedSentences: Array[String])

//Convention: Cluster id = -1 means no coreference is known
case class RelationTree(map: scala.collection.mutable.Map[Int, Set[AnnontatedRelation]] = scala.collection.mutable.Map())

//contains candidate list per relation argument (annotated with dbpedia resourcesd geonames)
case class AnnontatedRelation(arg1: AnnotatedArgument, rel: String, relOffset: (Int, Int), arg2: Seq[AnnotatedArgument])

case class AnnotatedArgument(spotlight: Option[SpotlightResult] = None, clavin: Option[Location] = None,
                             dbpediaLookup: Option[List[LookupResult]] = None,  arg: String, argType: String, argOffset: (Int, Int))
