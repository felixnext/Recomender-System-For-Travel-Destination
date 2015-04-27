package core.http

import akka.actor.{Actor, ActorLogging}
import core.SparqlQueryCreator
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

  //initialize services
  val elasticClient = new ElasticsearchClient()
  val analyzingPipe = new TextAnalyzerPipeline
  val queryCreator = new SparqlQueryCreator(analyzingPipe)
  val dbpediaCleint = new DBPediaClient

  def receive = {
    case UserQuery(query: String) if query.equals("") => log.debug("Query is empty"); throw new Exception("Query is empty.")
    case UserQuery(query: String) =>
      val reply = sender

      //TODO make requests
      log.debug("query: " + query)

      //Elasticsearch location request
      val elaticResult = Future {
        elasticClient.matchQuery(query).flatten
      }
      elaticResult.onComplete(r => log.debug("Elasticsearch result received. SUCCESS: " + r.isSuccess))

      //DBPedia location request
      val annotatedText = analyzingPipe.analyzeText(query)
      annotatedText.onComplete(r => log.debug("Text annotation finished. SUCCESS: " + r.isSuccess))

      val queries = queryCreator.createSparqlQuery(annotatedText)
      queries.onComplete(r => log.debug("DBPedia queries were extracted. SUCCESS: " + r.isSuccess))


      val dbpediaLocations = queries.map(qs =>
        qs.map(q => (dbpediaCleint.executeLocationQuery(q._1), q._2))
      )
      dbpediaLocations.onComplete(r => log.debug("DBPedia locations were downloaded. SUCCESS: " + r.isSuccess))

      dbpediaLocations.onSuccess {
        case r =>
          val m = r.toList.flatMap(s => s._1.map(l =>
            Map("name" -> l.name, "lat" -> l.lat.getOrElse(""), "long" -> l.long.getOrElse(""), "score" -> s._2.toString)))
          log.debug("DBPedia locations converted. Number of retrieved locations: " + m.size)
          reply ! Locations(m.toList)
          log.debug("Response was send.")
      }

    //TODO relation strore request

    //TODO merge results

    //TODO score results


    //val result =  Locations(List(Map("name"-> "Mallorca","lat" -> "12", "lon" -> "0.12" ,"score"->"0.23")))
    //context.parent ! result
    //sender ! result
    case _ => log.debug("Received unexpected message object.")
  }

}
