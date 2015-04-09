package core.http

import akka.actor.{ActorLogging, Actor}
import elasticsearch.ElasticsearchClient

/**
 * Created by yevgen on 09.04.15.
 */
class LocationFinderActor  extends Actor with ActorLogging{

  log.debug("LocationFinder created!")

  val elasticClient = new ElasticsearchClient()

  def receive = {
    case UserQuery(query: String) if query.equals("") => log.debug("Query is empty"); throw new Exception("Query is empty.")
    case UserQuery(query: String) =>
      //TODO make requests
      log.debug(elasticClient.matchQuery(query).flatten.head.title.getOrElse(""))
      log.debug("Query is non empty.")
      val result =  Locations(List(Map("name"-> "Mallorca","lat" -> "12", "lon" -> "0.12" ,"score"->"0.23")))
      context.parent ! result
    case _ => log.debug("Received unexpected message object.")
  }

}
