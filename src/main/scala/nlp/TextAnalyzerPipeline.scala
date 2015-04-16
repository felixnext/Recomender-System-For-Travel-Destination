package nlp

import java.util

import clavin.{ClavinClient, Location}
import dbpedia.{SpotlightClient, SpotlightResult}
import edu.stanford.nlp.dcoref.CorefChain
import edu.stanford.nlp.trees.Tree

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._


/**
 * Analyzes the text with basic annotators. The analyze is done in parallel.
 */
trait TextAnalyzerPipeline {

  //initialize required resources
  val relationExtractor = new RelationExtractor
  val spotlight = new SpotlightClient
  val clavin = new ClavinClient
  val stanford = new StanfordAnnotator

  //annotates text with four annotators:
  //StanfordNLP, RelationExtraction (Opnie), Clavin gazetteer and Spotlight
  def analyzeText(text: String): Future[AnnotatedText] = {
    //process text
    //val relations = future{relationExtractor.extractRelations(text)}
    val spotlightAnnotation = future {
      spotlight.discoverEntities(text)
    }
    val clavinAnnotation = future {
      clavin.extractLocations(text)
    }
    val stanfordAnnotation = future {
      stanford.annotateText(text)
    }

    val relations: Future[Array[Seq[Relation]]] = stanfordAnnotation.map(s =>
      s.tokenizedSentences.map(s => relationExtractor.extractRelations(s)))


    //if some future fails print error message
    for(e <- relations.failed) println("Relation extraction failed. Cannot create sparql query" + e)
    for (e <- spotlightAnnotation.failed) println("Relation extraction failed. Cannot create sparql query" + e)
    for (e <- clavinAnnotation.failed) println("Relation extraction failed. Cannot create sparql query" + e)
    for (e <- stanfordAnnotation.failed) println("Relation extraction failed. Cannot create sparql query" + e)

    val annText = for{
      r <- relations
      s <- spotlightAnnotation
      c <- clavinAnnotation
      stfrd <- stanfordAnnotation
    } yield {
        new AnnotatedText(r,s,c,stfrd)
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

  def createSprqlQuery(text: String): Unit = {

    val annotatedText = analyzeText(text)


    //TODO annotate relation with pos


    //TODO finds focus with coreference and extend entities candidates

    //TODO extand entity candidates with clavin and spotlight

    //TODO the unkown entities should be searched with dbpedia lookup

    //TODO annotata pradicates with patty

    //TODO create query


  }


  def posRelAnnotation(sentencesPos: Array[String], relations: Array[Seq[Relation]]): Unit = {


    //Annotate with patty tag names
    //sentence: Seq[(Word,Tag)]
    def annotatePos(relation: Relation, sentence: Seq[(String,String)]) = {
      val mapToPattyTags = Map("CD" -> "[[num]]","DT" -> "[[det]]", "PRP" -> "[[prp]]",
        "JJ" -> "[[adj]]", "MD" -> "[[mod]]", "IN" -> "[[con]]", "CC" -> "[[con]]")

      //TODO semicolon present?
      val relationWords = relation.rel.split(" ")
      val slidingOverSentence  = sentence.sliding(relationWords.size)

      //if contains patty tag, than replace word with pos tag else take word
      val posTaggedRelationList = for(subSentence <- slidingOverSentence if subSentence.map(s => s._1) == relationWords) yield {
        subSentence.map(tuple =>
          if(mapToPattyTags.keySet.contains(tuple._2)) mapToPattyTags.get(tuple._2)
          else tuple._1
        )
      }
      posTaggedRelationList.toString()
    }

  }


}

//relations are annotated per sentence.
case class AnnotatedText(relations: Array[Seq[Relation]], spotlight: List[SpotlightResult], clavin: List[Location], stanford: StanfordAnnotation)

/**
 * It is a container for stanford core nlp annotation.
 */
case class StanfordAnnotation(sentimentTree: Array[Tree], sentencesPos: Array[String], coreference: util.Map[Integer, CorefChain], tokenizedSentences: Array[String])