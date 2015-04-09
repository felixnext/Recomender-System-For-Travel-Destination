package core

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import core.http.RESTfulHTTPServer
import elasticsearch.ElasticsearchClient
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import tools.Config
import scala.concurrent.duration._


/**
 * Starts the app.
 */
object Boot extends App{


  val c = new ElasticsearchClient()
  val r  = c.phraseQuery("The island in spain.").flatten

  r.foreach{
    a => println(a)
  }


  /*
  // create our actor system with the name smartjava
  implicit val system = ActorSystem("recommender-system")
  val service = system.actorOf(Props[RESTfulHTTPServer], "sj-rest-service")

  // Bind HTTP to the specified service.
  implicit val timeout = Timeout(5.seconds)
  IO(Http) ? Http.Bind(service, Config.serviceHost, Config.servicePort)


  */
}

