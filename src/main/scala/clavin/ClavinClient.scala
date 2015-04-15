package clavin


import com.google.gson.{JsonElement, Gson, JsonObject}
import tools.Config

import scala.annotation.tailrec
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

  //parses clavin rest response in json format
  //geotag path
  def parseResponse(responseBody: String): List[Location] = {
    try {
      val jsonRoot = new Gson().fromJson(responseBody, classOf[JsonObject])
      val  resolvedLocations = jsonRoot.get("resolvedLocations").getAsJsonArray.iterator()

      @tailrec
      def extractInformation(docIterator: java.util.Iterator[JsonElement], results: List[Location] = List()): List[Location] = {
        if(!docIterator.hasNext) results
        else {
          val doc =  docIterator.next.getAsJsonObject
          val resource = doc.get("geoname")
          val asciiName = resource.getAsJsonObject.get("asciiName").getAsString
          val name = resource.getAsJsonObject.get("name").getAsString
          val lat = resource.getAsJsonObject.get("latitude").getAsDouble
          val lon = resource.getAsJsonObject.get("longitude").getAsDouble
          val country = resource.getAsJsonObject.get("primaryCountryName").getAsString
          val offset = doc.get("location").getAsJsonObject.get("position").getAsInt
          val population = resource.getAsJsonObject.get("population").getAsInt
          extractInformation(docIterator, results :+ new Location(name,asciiName,lat,lon, population, country, offset))
        }
      }

      extractInformation(resolvedLocations)
    } catch {
      case e: Exception => println("Error on parsing clavin response: " + e); List()
    }
  }

}

case class Location(name: String, asciiName: String, lat: Double, lon: Double, population: Int, country: String, offset: Int)
