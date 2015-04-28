package core.http

import akka.actor.{ActorLogging, Props, Actor}
import akka.routing.RoundRobinPool
import core.http.QueryJsonProtocol._
import core.http.routing.PerRequestCreator
import spray.http.MediaTypes
import spray.httpx.SprayJsonSupport._
import spray.routing._
import MediaTypes._
import akka.pattern.ask

import akka.util.Timeout
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


/**
 * Rest http service for finding travel destinations.
 */
class RESTfulHTTPServer extends Actor with HttpService with PerRequestCreator  with ActorLogging{

  def actorRefFactory = context

  def receive = runRoute(searchRoute)

  val workerActors = context.actorOf(Props[LocationFinderActor].withRouter(RoundRobinPool(1)), name = "WorkerActors")

  implicit val timeout = Timeout(1000.seconds)

  // handles the api path, we could also define these in separate files
  // this path respons to get queries, and make a selection on the
  // media-type.
  val searchRoute = {
    post {
      path("search") {
        entity(as[UserQuery]) {
          query => respondWithMediaType(`application/json`) {
            onComplete((workerActors ? query).mapTo[Locations]) {
              case Success(value) =>
                log.debug("Processed message received. Ready to response.")
                complete(value)
              case Failure(t) =>
                log.debug("Failure.")
                failWith(t)
            }
          }
        }
      }

    }
  }

  def findLocations(query: UserQuery): Route = {
    ctx => perRequest(ctx, Props(new LocationFinderActor), query)
  }
}
