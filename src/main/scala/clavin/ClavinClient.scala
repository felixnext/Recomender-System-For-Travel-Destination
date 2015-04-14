package clavin


import com.google.gson.{Gson, JsonObject}
import tools.Config

import scalaj.http.{Http, HttpResponse}

/**
 * Clavin client make gazetteer request. The text will be annotated with geoNames entities.
 */
class ClavinClient {

  val clavin = Config.clavinUrl

  //annotation request
  //The text param wil be annotated.
  def extractLocations(text: String): List[Location] = {
    try {
      val response: HttpResponse[String] = Http(clavin + "/api/v0/geotag")
        .header("Content-Type", "text/plain").timeout(connTimeoutMs = 2000, readTimeoutMs = 10000).postData(text).asString
      parseResponse(response.body)
    } catch {
      case e: Exception => println("Clavin request error: " + e); List()
    }
  }

  def parseResponse(responseBody: String): List[Location] = {
    try {
      val jsonRoot = new Gson().fromJson(responseBody, classOf[JsonObject])
      val  resolvedLocations = jsonRoot.get("resolvedLocations").getAsJsonArray

      //TODO parse importat information

    List()
    } catch {
      case e: Exception => println("Error on parsing clavin response: " + e); List()
    }
  }

}

case class Location(asciiName: String, lat: Double, lon: Double)
