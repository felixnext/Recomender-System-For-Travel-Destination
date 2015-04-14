package dbpedia

import com.google.gson.{JsonElement, Gson}
import tools.Config

import scala.annotation.tailrec
import scalaj.http.{HttpException, Http, HttpResponse}


/**
 * This class represents spotlight client, using remote dbpedia endpoint
 */
class SpotlightClient {

  //rest endpoint
  val url = Config.spotlightUrl
  val confidence = Config.spotlightConfidence
  val support = Config.spotlightSupport

  //tests: if request timeouts occur, number of tryings
  //assumption: the desired entities are of type location
  //return: Result as json string
  def requestLocation(text: String, tests: Int = 3): List[SpotlightResult]  = {
    discoverEntities(text, paramsArg = Map("confidence" -> s"$confidence", "text" -> text, "support" -> s"$support", "types" -> "Place,YagoGeoEntity,SpatialThing"))
  }

  
  //Finds all entities in text
  def discoverEntities(text:String, tests: Int = 3, paramsArg: Map[String,String] = Map("confidence" -> s"$confidence","support" -> s"$support")):  List[SpotlightResult]  = {
    try {
      //REST api request
      val response: HttpResponse[String] = Http(url + "/rest/annotate")
        .params(paramsArg).param("text", text)
        .header("Accept", "application/json").timeout(connTimeoutMs = 2000, readTimeoutMs = 7000).asString
      parseResponse(response.body)
    } catch {
      case e: java.net.SocketTimeoutException =>
        println(e)
        if(tests != 0) requestLocation(text, tests - 1)
        else List()
      case e: HttpException =>
        println(e)
        Thread.sleep(1000)
        if(tests != 0) requestLocation(text, tests - 1)
        else List()
      case e: Exception => println(e); List()
    }
  }

  //parses the spotlight response and returns SpotlightResult object
  def parseResponse(response: String): List[SpotlightResult] = {
    val annotation = new Gson().fromJson(response, classOf[JsonElement]).getAsJsonObject
    val resources = annotation.getAsJsonArray("Resources").iterator()

    @tailrec
    def extractInformation(docIterator: java.util.Iterator[JsonElement], results: List[SpotlightResult] = List()): List[SpotlightResult] = {
      if(!docIterator.hasNext) results
      else {
        val resource = docIterator.next
        val uri = resource.getAsJsonObject.get("@URI").getAsString
        val types = resource.getAsJsonObject.get("@types").getAsString.split(",")
        val support = resource.getAsJsonObject.get("@support").getAsInt
        val surfaceForm = resource.getAsJsonObject.get("@surfaceForm").getAsString
        val offset = resource.getAsJsonObject.get("@offset").getAsInt
        val simScore = resource.getAsJsonObject.get("@similarityScore").getAsDouble
        extractInformation(docIterator, results :+ new SpotlightResult(uri,support,types,surfaceForm,offset,simScore))
      }
    }

    extractInformation(resources)
  }

}

//Represents spotlight result
case class SpotlightResult(uri: String, support: Int, types: Seq[String], surfaceForm: String, offset: Int, score: Double)


