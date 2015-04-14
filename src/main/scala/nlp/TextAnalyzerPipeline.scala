package nlp

import clavin.{Location, ClavinClient}
import dbpedia.{SpotlightResult, SpotlightClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._


/**
 * Takes a text and creates corresponding sparql query w.r.t focus of text.
 * Assumption the focus is a certain location.
 */
class TextAnalyzerPipeline {

  //initialize required resources
  val relationExtractor  = new RelationExtractor
  val spotlight = new SpotlightClient
  val clavin = new ClavinClient
  val stanford = new StanfordAnnotator

  //annotates text with four annotators:
  //StanfordNLP, RelationExtraction (Opnie), Clavin gazetteer and Spotlight
  def analyzeText(text: String): Future[AnnotatedText] = {
    //process text
    val relations = future{relationExtractor.extractRelations(text)}
    val spotlightAnnotation = future{spotlight.discoverEntities(text)}
    val clavinAnnotation = future{clavin.extractLocations(text)}
    val stanfordAnnotation = future{stanford.annotateText(text)}

    //if some future fails print error message
    for(e <- relations.failed) println("Relation extraction failed. Cannot create sparql query" + e)
    for(e <- spotlightAnnotation.failed) println("Relation extraction failed. Cannot create sparql query" + e)
    for(e <- clavinAnnotation.failed) println("Relation extraction failed. Cannot create sparql query" + e)
    for(e <- stanfordAnnotation.failed) println("Relation extraction failed. Cannot create sparql query" + e)

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

case class AnnotatedText(relations: Seq[Relation], spotlight: List[SpotlightResult], clavin: List[Location], stanford: Seq[StanfordAnnotation])