package nlp

import java.util

import clavin.{ClavinClient, Location}
import dbpedia.{SpotlightClient, SpotlightResult}
import edu.stanford.nlp.dcoref.CorefChain
import edu.stanford.nlp.trees.Tree
import elasticsearch.ElasticsearchClient
import tools.Levenshtein

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

import scala.collection.convert.wrapAsScala._

import scala.math._


/**
 * Analyzes the text with basic annotators. The analyze is done in parallel.
 */
trait TextAnalyzerPipeline {

  //initialize required resources
  val relationExtractor = new RelationExtractor
  val clavin = new ClavinClient
  val stanford = new StanfordAnnotator

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

    val relations: Future[Array[Seq[Relation]]] = stanfordAnnotation.map(s =>
      s.tokenizedSentences.map(s => relationExtractor.extractRelations(s)))


    //if some future fails print error message
    for (e <- relations.failed) println("Relation extraction failed. Cannot create sparql query" + e)
    for (e <- clavinAnnotation.failed) println("Relation extraction failed. Cannot create sparql query" + e)
    for (e <- stanfordAnnotation.failed) println("Relation extraction failed. Cannot create sparql query" + e)

    val annText = for {
      r <- relations
      c <- clavinAnnotation
      stfrd <- stanfordAnnotation
    } yield {
        new AnnotatedText(r, c, stfrd)
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

  val spotlight = new SpotlightClient
  val elastic = new ElasticsearchClient

  def createSprqlQuery(text: String): Unit = {

    //spotlight analysis
    val spotlightAnnotation = future {spotlight.discoverEntities(text)}
    for (e <- spotlightAnnotation.failed) println("Relation extraction failed. Cannot create sparql query" + e)

    //clavin, stanford and opnie
    val annotatedText = analyzeText(text)

    //replaces stanford pos tags with patty tags
    val posRelations = annotatedText.map(x => posRelAnnotation(x.stanford.sentencesPos,x.relations))

    //maps raw relations into patty dbpedia predicates
    val pattyRelations = posRelations.map{posRel =>
      posRel.map(sentence => sentence.map(relation => elastic.findPattyRelation(relation.rel)))
    }


    //TODO finds focus with coreference and extend entities candidates

    //TODO extand entity candidates with clavin and spotlight

    //TODO the unkown entities should be searched with dbpedia lookup

    //TODO combine patty predicates and annotated entities

    //TODO create query


  }


  //Takes relations and replace predicates with patty pos tags
  def posRelAnnotation(sentencesPos: Array[String], relations: Array[Seq[Relation]]):  Array[Seq[Relation]] = {

    //split each word in sentece on "/". This converts words form word/pos into tuple (word,pos)
    val sentences: Array[Array[(String, String)]] = sentencesPos.map { x => x.split(" ").map { x =>
      val split = x.split("/")
      try {
        (split(0), split(1))
      } catch {
        case e: Exception => (split(0), "")
      }
    }}

    //Annotate with patty tag names
    //sentence: Seq[(Word,Tag)]
    def annotatePos(relation: Relation, sentence: Array[(String, String)]) = {
      val mapToPattyTags = Map("CD" -> "[[num]]", "DT" -> "[[det]]", "PRP" -> "[[prp]]",
        "JJ" -> "[[adj]]", "MD" -> "[[mod]]", "IN" -> "[[con]]", "CC" -> "[[con]]")

      val relationWords = relation.rel.split(" ")
      val slidingOverSentence = sentence.sliding(relationWords.size)

      //if contains patty tag, than replace word with pos tag else take a word
      val posTaggedRelationList = for (subSentence <- slidingOverSentence if subSentence.map(s => s._1) == relationWords) yield {
        subSentence.map(tuple =>
          if (mapToPattyTags.keySet.contains(tuple._2)) mapToPattyTags.get(tuple._2)
          else tuple._1
        )
      }
      new Relation(relation.arg1, posTaggedRelationList.toString(), relation.relOffset, relation.arg2)
    }

    //iterate over all sentence and relations
    val posRelations: Array[Seq[Relation]] = for (sentence <- sentences; relationsPerSentence <- relations) yield {
      relationsPerSentence.map {
        relation => annotatePos(relation, sentence)
      }
    }

    posRelations
  }


  def createEntityCandidates(relations: Array[Seq[Relation]], spotlightResult: List[SpotlightResult],
                             clavinResult: List[Location], coreference: util.Map[Integer, CorefChain]) ={
    //get all key of coref clusters
    //value coresponds to cluster id
    val keys: java.util.Set[Integer] = coreference.keySet()

    //creates a new RelationTree object, fills the object with relations and groups relations with coreference argument together
    @tailrec
    def traverseRelationSentences(iterator: Iterator[Seq[Relation]], sentenceCounter: Int = 1, tree:RelationTree = new RelationTree): RelationTree = {
      if(!iterator.hasNext) tree
      else {
        val senteceRel = iterator.next()
        
        //TODO Yago

        for(rel <- senteceRel) {
          //relation loop
          //if arg1 of relation is in coref than corefID else -1
          //List(1,2,1,-1,-1,-1)
          val keysInRelation =  for(key <- keys) yield {
            val c: CorefChain =  coreference.get(key)
            val cms: java.util.List[CorefChain.CorefMention] = c.getMentionsInTextualOrder
            val r: Int = cms.find(cm => cm.sentNum == sentenceCounter && containsTest(cm.mentionSpan,rel.arg1.arg)) match {
              case Some(x) => key.intValue()
              case None => -1 //Default cluster id
            }
            r
          }

          //converts List(1,2,1,-1,3,-1) to set Set(1,2,3,-1)
          val reducedClusterIds = keysInRelation.foldLeft(Set(-1))((a,b) => a + b)

          //annotates with clavin and spotlight result if exists
          val annotatedRel = {
            val spotlight = spotlightResult.find(x =>
              intersect(x.offset, x.offset + x.surfaceForm.length,rel.arg1.argOffset._1,rel.arg1.argOffset._2))
            val location = clavinResult.find(x =>
              intersect(x.offset, x.offset + x.asciiName.length,rel.arg1.argOffset._1,rel.arg1.argOffset._2))

            def intersect(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int) = max(aStart,bStart) < min(aEnd,bEnd)

            new AnnontatedRelation(spotlight = spotlight, clavin = location, relation = rel)
          }

          if(reducedClusterIds.size == 1) {
            //No coreference found
            val relSet = tree.map.getOrElse( -1, Set()) + annotatedRel
            tree.map.+=(-1 -> relSet)
          } else {
            //some coreference found
            (reducedClusterIds - -1).foreach{
              case id => val relSet = tree.map.getOrElse(id, Set()) + annotatedRel
              tree.map.+=(id -> relSet)
            }
          }

        }

        traverseRelationSentences(iterator, sentenceCounter + 1, tree)
      }
    }

    def containsTest(a: String, b:String) = if(a.length < b.length) b.contains(a) else a.contains(b)

    val tree = traverseRelationSentences(relations.iterator)

  }

}

//relations are annotated per sentence.
case class AnnotatedText(relations: Array[Seq[Relation]], clavin: List[Location], stanford: StanfordAnnotation)

/**
 * It is a container for stanford core nlp annotation.
 */
case class StanfordAnnotation(sentimentTree: Array[Tree], sentencesPos: Array[String], coreference: util.Map[Integer, CorefChain], tokenizedSentences: Array[String])

//Convention: Cluster id = -1 means no coreference is known
case class RelationTree(map: scala.collection.mutable.Map[Int, Set[AnnontatedRelation]] = scala.collection.mutable.Map())

case class AnnontatedRelation(spotlight: Option[SpotlightResult] = None , clavin: Option[Location] = None, relation: Relation)
