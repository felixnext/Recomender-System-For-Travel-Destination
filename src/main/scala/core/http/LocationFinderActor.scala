package core.http

import akka.actor.{ActorLogging, Actor}
import edu.knowitall.openie.OpenIE
import elasticsearch.ElasticsearchClient

/**
 * Actor, that handles user requests.
 */
class LocationFinderActor  extends Actor with ActorLogging{

  val openie2 = new OpenIE()


  log.debug("LocationFinder created!")

  val elasticClient = new ElasticsearchClient()

  def receive = {
    case UserQuery(query: String) if query.equals("") => log.debug("Query is empty"); throw new Exception("Query is empty.")
    case UserQuery(query: String) =>
      //TODO make requests
      log.debug(elasticClient.matchQuery(query).flatten.head.title.getOrElse(""))
      log.debug("Query is non empty.")
      val result2 = openie2.extract(query)
      val result =  Locations(List(Map("name"-> "Mallorca","lat" -> "12", "lon" -> "0.12" ,"score"->"0.23")))
      //context.parent ! result
      sender ! result
    case _ => log.debug("Received unexpected message object.")
  }

}
