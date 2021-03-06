package core.http

import akka.actor.{Actor, ActorLogging}
import core.{RelationLocationFinder, RelationExtraction, SparqlQueryCreator}
import dbpedia.DBPediaClient
import elasticsearch.ElasticsearchClient
import nlp.TextAnalyzerPipeline

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
 * Actor, that handles user requests.
 */
class LocationFinderActor extends Actor with ActorLogging {


  log.debug("LocationFinder created!")


  //val analyzingPipe = new TextAnalyzerPipeline


  val analyzer = new TextAnalyzerPipeline

  def receive = {
    case UserQuery(query: String) if query.equals("") => log.debug("Query is empty"); throw new Exception("Query is empty.")
    case UserQuery(query: String) =>
      val reply = sender

      //TODO make requests
      log.debug("query: " + query)

      //Elasticsearch location request
      val elaticResult = Future {
        ElasticsearchClient.matchQuery(query).flatten
      }
      elaticResult.onComplete(r => log.debug("Elasticsearch result received. SUCCESS: " + r.isSuccess))

      //DBPedia location request
      val annotatedText = analyzer.analyzeText(query)

      val queries = SparqlQueryCreator.createSparqlQuery(annotatedText)

      val dbpediaLocations = queries.map(q => (DBPediaClient.executeLocationQuery(q._1), q._2))


      val relations = RelationExtraction.extractRelations(annotatedText)
      val locations = RelationLocationFinder.findLocations(relations)

      /*
      dbpediaLocations.onSuccess {
        case r =>
          val m = r.toList.flatMap(s => s._1.map(l =>
            Map("name" -> l.name, "lat" -> l.lat.getOrElse(""), "long" -> l.long.getOrElse(""), "score" -> s._2.toString)))
          log.debug("DBPedia locations converted. Number of retrieved locations: " + m.size)
          reply ! Locations(m.toList)
          log.debug("Response was send.")
      }
      */
    //TODO relation strore request

    //TODO merge results

    //TODO score results


    //val result =  Locations(List(Map("name"-> "Mallorca","lat" -> "12", "lon" -> "0.12" ,"score"->"0.23")))
    //context.parent ! result
    //sender ! result
    case _ => log.debug("Received unexpected message object.")
  }

}
