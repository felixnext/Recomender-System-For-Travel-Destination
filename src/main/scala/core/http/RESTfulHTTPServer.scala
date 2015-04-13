package core.http

import akka.actor.{ActorLogging, Props, Actor}
import core.http.QueryJsonProtocol._
import core.http.routing.PerRequestCreator
import spray.http.MediaTypes
import spray.httpx.SprayJsonSupport._
import spray.routing._
import MediaTypes._


/**
 * Created by yevgen on 09.04.15.
 */
class RESTfulHTTPServer extends Actor with HttpService with PerRequestCreator  with ActorLogging{

  def actorRefFactory = context
  //TODO search route
  def receive = runRoute(aSimpleRoute)

  // handles the api path, we could also define these in separate files
  // this path respons to get queries, and make a selection on the
  // media-type.
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

  def findLocations(query: UserQuery): Route = {
    ctx => perRequest(ctx, Props(new LocationFinderActor), query)
  }

}
