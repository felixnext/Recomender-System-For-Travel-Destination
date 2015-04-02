package dbpedia

import scalaj.http.{Http, HttpResponse}


/**
 * This class represents spotlight client, using remote dbpedia endpoint
 */
class SpotlightClient {

  //there are two possible rest endpoints
  val url = if (Http("http://spotlight.sztaki.hu:2222/rest/annotate?text=\"\"").method("HEAD").asString.code == 200)
    "http://spotlight.sztaki.hu:2222/rest/annotate"
  else if (Http("http://spotlight.dbpedia.org/rest/annotate?text=\"\"").method("HEAD").asString.code == 200)
    "http://spotlight.dbpedia.org/rest/annotate"
  else throw new Exception("Spotlight rest api not available")

  //tests: if request timeouts occur, number of tryings
  //return: Result as json string
  def requestLocation(text: String, tests: Int): String = {
    try {
      //http://spotlight.sztaki.hu:2222/rest/annotate
      val response: HttpResponse[String] = Http(url)
        .params(Map("confidence" -> "0.25", "text" -> text, "support" -> "5", "types" -> "Place,YagoGeoEntity,SpatialThing"))
        .header("Accept", "application/json").timeout(connTimeoutMs = 2000, readTimeoutMs = 7000).asString
      response.body
    } catch {
      case e: java.net.SocketTimeoutException =>
        println(e)
        if(tests != 0) requestLocation(text, tests - 1)
        else ""
      case e: Exception => println(e); ""
    }
  }

}



