package nlp

import java.util

import clavin.{ClavinClient, Location}
import dbpedia.{DBPediaLookup, LookupResult, SpotlightClient, SpotlightResult}
import edu.stanford.nlp.dcoref.CorefChain
import edu.stanford.nlp.trees.Tree
import elasticsearch.{DBPediaClass, DBPediaProps, PattyRelation}

import scala.annotation.tailrec
import scala.collection.convert.wrapAsScala._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.math._
import scala.util.Try


/**
 * Analyzes the text with basic annotators. The analyze is done in parallel.
 */
class TextAnalyzerPipeline {

  //initialize required resources
  val relationExtractor = new RelationExtractor
  val stanford = new StanfordAnnotator
  val spotlight = new SpotlightClient


  /**
   * Annotates text with four annotators:
   * StanfordNLP, RelationExtraction (Opnie), Clavin gazetteer and Spotlight.
   * @param text Raw text.
   * @return Annotation of given text.
   */
  def analyzeText(text: String): AnnotatedText = {
    //process text
    val futureRel = Future {
      relationExtractor.extractRelations(text)
    }

    //geoparsing
    val futureClavin = Future {
      ClavinClient.extractLocations(text)
    }

    //spotlight analysis
    val futureSpotlight = Future {
      spotlight.discoverEntities(text)
    }

    //nlp annotation
    val stanfordAnnotation = stanford.annotateText(text)

    val rawRel = Try(Await.result(futureRel, 30.seconds)).getOrElse(Seq())

    //split relations into subsets, each subset corresponds to one sentence
    val relations: Array[Seq[Relation]] = {
      val s = stanfordAnnotation
      val r = rawRel
      val sentenceBoundaries = s.tokenizedSentences.foldLeft(0, List[(Int, Int)]())((t, s) =>
        (t._1 + s.length + 1, t._2 :+(t._1, t._1 + s.length)))._2

      val groups: Map[(Int, Int), Seq[Relation]] = sentenceBoundaries.map(x => x -> Seq[Relation]()).toMap

      val occupiedGroups = r.foldLeft(groups) {
        (tmpGroups, x) =>
          val minOffset = (Seq(x.arg1.argOffset._1, x.relOffset._1) ++ x.arg2.map(x => x.argOffset._1)).min
          val maxOffset = (Seq(x.arg1.argOffset._2, x.relOffset._2) ++ x.arg2.map(x => x.argOffset._2)).max
          val newGroups = tmpGroups.keySet.find(x => x._1 <= minOffset && maxOffset <= x._2) match {
            case Some(key) => tmpGroups + (key -> (tmpGroups.getOrElse(key, Seq()) :+ x))
            case _ => tmpGroups
          }
          newGroups
      }
      occupiedGroups.toSeq.sortBy(_._1._1).map(t => t._2).toArray
    }

    //wait for results
    val spotlightAnnotation = Try(Await.result(futureSpotlight, 10.seconds)).getOrElse(List())
    val clavinAnnotation =  Try(Await.result(futureClavin, 10.seconds)).getOrElse(List())

    new AnnotatedText(relations, clavinAnnotation, stanfordAnnotation, spotlightAnnotation)
  }
}

object TextAnalyzerPipeline {

  def intersect(a: (Int, Int), b: (Int, Int)) = max(a._1, b._1) < min(a._2, b._2)

  type Sentences = Array[Array[(String, String)]]

  //Takes list of relation, annotates subject, object with dbpedia uris and geoname location.
  //Creates tree with help of coreference. The relations with co-referent object are composed to single node in the tree.
  def createEntityCandidates(relations: Array[Seq[Relation]], spotlightResult: List[SpotlightResult],
                             clavinResult: List[Location], coreference: util.Map[Integer, CorefChain],
                             sentences: Sentences, offsetConverter: OffsetConverter): RelationTree = {

    //get all key of coref clusters
    //value corresponds to cluster id
    val keys: java.util.Set[Integer] = coreference.keySet()

    //converts stanford sentence and token indices into char offset, counted from beginning of the text
    def calculateOffset(sentenceNr: Int, tokenBegin: Int, tokenEnd: Int, token: String): (Int, Int) =
      offsetConverter.sentenceToCharLevelOffset(sentenceNr, tokenBegin, tokenEnd, token)

    //creates a new RelationTree object, fills the object with relations and groups relations with coreference argument together
    @tailrec
    def traverseRelationSentences(iterator: Iterator[Seq[Relation]], sentenceCounter: Int = 1,
                                  tree: RelationTree = new RelationTree): RelationTree = {
      if (!iterator.hasNext) tree
      else {
        val senteceRel = iterator.next()

        //relation loop
        for (rel <- senteceRel) {

          //if arg1 of relation is in coref than corefID else -1
          //List(1,2,1,-1,-1,-1)
          val keysInRelation = for (key <- keys) yield {
            val c: CorefChain = coreference.get(key)
            val cms: java.util.List[CorefChain.CorefMention] = c.getMentionsInTextualOrder
            val r: Int = cms.find(cm => cm.sentNum == sentenceCounter &&
              intersect(calculateOffset(cm.sentNum, cm.startIndex, cm.endIndex, cm.mentionSpan),
                rel.arg1.argOffset)) match {
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
            val lookupRes = if (!(locationArg1.isDefined || spotlightArg1.isDefined)) Some(DBPediaLookup.findDBPediaURI(rel.arg1.arg))
            else None

            val arg1 = new AnnotatedArgument(spotlight = spotlightArg1, clavin = locationArg1, arg = rel.arg1.arg,
              argType = rel.arg1.argType, argOffset = rel.arg1.argOffset, dbpediaLookup = lookupRes)

            val arg2: Seq[AnnotatedArgument] = for (arg2 <- rel.arg2) yield {
              val spotlightArg2 = spotlightResult.find(x =>
                intersect((x.offset, x.offset + x.surfaceForm.length), (arg2.argOffset._1, arg2.argOffset._2)))
              val locationArg2 = clavinResult.find(x =>
                intersect((x.offset, x.offset + x.asciiName.length), (arg2.argOffset._1, arg2.argOffset._2)))

              //if neither spotlight now clavin annotation known
              //then try dbpedia lookup
              val lookupRes = if (!(spotlightArg2.isDefined || locationArg2.isDefined)) Some(DBPediaLookup.findDBPediaURI(arg2.arg))
              else None

              new AnnotatedArgument(spotlight = spotlightArg2, clavin = locationArg2,
                arg = arg2.arg, argType = arg2.argType, argOffset = arg2.argOffset, dbpediaLookup = lookupRes)
            }

            new AnnontatedRelation(arg1, rel.rel, rel.relOffset, arg2)
          }

          if (reducedClusterIds.size == 1) {
            //No coreference found
            val relSet = tree.groupsMap.getOrElse(-1, Set()) + annotatedRel
            tree.groupsMap.+=(-1 -> relSet)
          } else {
            //some coreference found
            (reducedClusterIds - -1).foreach {
              case id => val relSet = tree.groupsMap.getOrElse(id, Set()) + annotatedRel
                tree.groupsMap.+=(id -> relSet)
            }
          }

        }

        traverseRelationSentences(iterator, sentenceCounter + 1, tree)
      }
    }

    //tests if the bigger argument contains the smaller one
    def containsTest(a: String, b: String) = if (a.length < b.length) b.contains(a) else a.contains(b)

    val tree = traverseRelationSentences(relations.iterator)
    tree
  }

  def formatPosSentences(x: AnnotatedText): Sentences = {
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

}

//Allows to convert sentence level offset into char level offset.
//Cache is used to simplify offset calculation for equal instances
class OffsetConverter(sentences: Array[Array[(String, String)]]) {

  val sentencesRaw: Array[Array[String]] = sentences.map(x => x.map(x => x._1))
  val charsPerSentence: Array[Int] = sentencesRaw.map(x => x.length - 1 + x.foldLeft(0)((l, c) => l + c.length))

  val cache = scala.collection.mutable.Map[(Int, Int), (Int, Int)]()

  //converts stanford sentence and tooken indices into char offset, counted from beginning of the text
  def sentenceToCharLevelOffset(sentenceNr: Int, tokenBegin: Int, tokenEnd: Int, token: String): (Int, Int) = {
    if (cache.contains((sentenceNr, tokenBegin))) cache.getOrElse((sentenceNr, tokenBegin), (-1, -1))
    else {
      try {
        val sentenceOffset = charsPerSentence.dropRight(charsPerSentence.length + 1 - sentenceNr).sum
        val startOffset = sentencesRaw(sentenceNr - 1).slice(0, tokenBegin - 1).map(a => a.length).
          foldLeft(0)((a, b) => a + b + 1) + sentenceOffset
        val endOffsset = sentencesRaw(sentenceNr - 1).slice(tokenBegin - 1, tokenEnd - 1).map(a => a.length)
          .foldLeft(0)((a, b) => a + b + 1) + startOffset
        cache += ((sentenceNr, tokenBegin) ->(startOffset, endOffsset))
        (startOffset, endOffsset)
      } catch {
        case e: Exception => (-1, -1)
      }
    }
  }
}

//relations are annotated per sentence.
case class AnnotatedText(relations: Array[Seq[Relation]], clavin: List[Location], stanford: StanfordAnnotation,
                         spotlight: List[SpotlightResult])


// It is a container for stanford core nlp annotation.
case class StanfordAnnotation(sentimentTree: Array[Tree], sentencesPos: Array[String], coreference: util.Map[Integer,
  CorefChain], tokenizedSentences: Array[String])

//Convention: Cluster id = -1 means no coreference is known
case class RelationTree(groupsMap: scala.collection.mutable.Map[Int, Set[AnnontatedRelation]] = scala.collection.mutable.Map())

//contains candidate list per relation argument (annotated with dbpedia resourcesd geonames)
case class AnnontatedRelation(arg1: AnnotatedArgument, rel: (String, String), relOffset: (Int, Int), arg2: Seq[AnnotatedArgument],
                              pattyResult: Option[List[PattyRelation]] = None, dbpediaProps: Option[List[DBPediaProps]] = None)

case class AnnotatedArgument(spotlight: Option[SpotlightResult] = None, clavin: Option[Location] = None,
                             dbpediaLookup: Option[List[LookupResult]] = None, arg: String, argType: String,
                             argOffset: (Int, Int), yago: Option[List[String]] = None, lemon: Option[List[DBPediaClass]] = None)


