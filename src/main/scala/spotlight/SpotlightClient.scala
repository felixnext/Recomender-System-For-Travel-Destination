package spotlight

import scalaj.http._


//This class represents spotlight client, using remote dbpedia endpoint
class RemoteSpotlightClient {

  //there are two possible rest endpoints
  val url = if (Http("http://spotlight.sztaki.hu:2222/rest/annotate?text=\"\"").method("HEAD").asString.code == 200)
    "http://spotlight.sztaki.hu:2222/rest/annotate"
    else if(Http("http://spotlight.dbpedia.org/rest/annotate?text=\"\"").method("HEAD").asString.code == 200)
    "http://spotlight.dbpedia.org/rest/annotate"
    else throw new Exception("Spotlight rest api not available")

  //return: Result as json string
  def requestLocation(text: String): String = {
    try{
      //http://spotlight.sztaki.hu:2222/rest/annotate
      val response: HttpResponse[String] = Http(url)
        .params(Map("confidence" -> "0.25", "text" -> text, "support" -> "5", "types" -> "Place,YagoGeoEntity,SpatialThing"))
        .header("Accept", "application/json").timeout(connTimeoutMs = 2000, readTimeoutMs = 7000).asString
      response.body
    } catch {
        case e: java.net.SocketTimeoutException => {println(e); requestLocation(text) }
        case e: Exception => {println(e);  ""}
    }
  }

}



