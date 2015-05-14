package core

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import core.http.RESTfulHTTPServer
import spray.can.Http
import akka.pattern.ask
import tools.Config
import akka.util.Timeout
import scala.concurrent.duration._


/**
 * Starts the http server.
 */
object Boot extends App {

  // create our actor system with the name smartjava
  implicit val system = ActorSystem("recommender-system")
  val service = system.actorOf(Props[RESTfulHTTPServer], "sj-rest-service")

  // Bind HTTP to the specified service.
  implicit val timeout = Timeout(1000.seconds)
  IO(Http) ? Http.Bind(service, Config.serviceHost, Config.servicePort)

}

