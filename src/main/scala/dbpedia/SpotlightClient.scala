package dbpedia

import tools.Config

import scalaj.http.{HttpException, Http, HttpResponse}


/**
 * This class represents spotlight client, using remote dbpedia endpoint
 */
class SpotlightClient {

  //rest endpoint
  val url = Config.spotlightUrl

  //tests: if request timeouts occur, number of tryings
  //return: Result as json string
  def requestLocation(text: String, tests: Int): String = {
    try {
      //http://spotlight.sztaki.hu:2222/rest/annotate
      val response: HttpResponse[String] = Http(url + "/rest/annotate")
        .params(Map("confidence" -> "0.5", "text" -> text, "support" -> "10", "types" -> "Place,YagoGeoEntity,SpatialThing"))
        .header("Accept", "application/json").timeout(connTimeoutMs = 2000, readTimeoutMs = 7000).asString
      response.body
    } catch {
      case e: java.net.SocketTimeoutException =>
        println(e)
        if(tests != 0) requestLocation(text, tests - 1)
        else ""
      case e: HttpException =>
        println(e)
        Thread.sleep(1000)
        if(tests != 0) requestLocation(text, tests - 1)
        else ""
      case e: Exception => println(e); ""
    }
  }

}



