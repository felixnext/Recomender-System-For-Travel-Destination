package core.http

/**
 * Created by yevgen on 09.04.15.
 */
import spray.json.DefaultJsonProtocol

object QueryJsonProtocol extends DefaultJsonProtocol {
  implicit val QueryFormat = jsonFormat1(UserQuery)
  implicit val LocationFormat = jsonFormat1(Locations)
  implicit val ErrorFormat = jsonFormat1(Error)

}

case class UserQuery(query: String)
case class Locations(locations: List[Map[String,String]])


case class Error(message: String)
