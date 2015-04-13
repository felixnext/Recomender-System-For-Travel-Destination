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
 * Created by yevgen on 09.04.15.
 */
class RESTfulHTTPServer extends Actor with HttpService with PerRequestCreator  with ActorLogging{

  def actorRefFactory = context
  //TODO search route
  def receive = runRoute(aSimpleRoute)

  val workerActors = context.actorOf(Props[LocationFinderActor].withRouter(RoundRobinPool(2)), name = "WorkerActors")

  // handles the api path, we could also define these in separate files
  // this path respons to get queries, and make a selection on the
  // media-type.
  /*
  val aSimpleRoute = {
    post {
      path("search") {
        entity(as[UserQuery]) {
          query => respondWithMediaType(`application/json`) {
            findLocations {
              query
            }
          }
        }
      }

    }
  }
  */

  implicit val timeout = Timeout(10.seconds)
  
  val aSimpleRoute = {
    post {
      path("search") {
        entity(as[UserQuery]) {
          query => respondWithMediaType(`application/json`) {
            onComplete((workerActors ? query).mapTo[Locations]) {
              case Success(value) => complete(value)
              case Failure(t) => failWith(t)
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
