package core.http.routing

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{OneForOneStrategy, _}
import core.http.QueryJsonProtocol._
import core.http.routing.PerRequest.{WithProps, WithActorRef}
import core.http.{Locations, Error, UserQuery}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

import scala.concurrent.duration._


trait PerRequest extends Actor with ActorLogging {

  import context._

  def r: RequestContext
  def target: ActorRef
  def message: UserQuery

  setReceiveTimeout(10.seconds)
  target ! message

  def receive = {
    case res: Locations => r.complete(OK, res)
    case ReceiveTimeout   => r.complete(GatewayTimeout, Error("Request timeout!!!"))
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      case e => {
        r.complete(InternalServerError, Error(e.getMessage))
        Stop
      }
    }
}

object PerRequest {
  case class WithActorRef(r: RequestContext, target: ActorRef, message: UserQuery) extends PerRequest

  case class WithProps(r: RequestContext, props: Props, message: UserQuery) extends PerRequest {
    lazy val target = context.actorOf(props)
  }
}

trait PerRequestCreator {
  this: Actor =>

  def perRequest(r: RequestContext, target: ActorRef, message: UserQuery) =
    context.actorOf(Props(new WithActorRef(r, target, message)))

  def perRequest(r: RequestContext, props: Props, message: UserQuery) =
    context.actorOf(Props(new WithProps(r, props, message)))
}
