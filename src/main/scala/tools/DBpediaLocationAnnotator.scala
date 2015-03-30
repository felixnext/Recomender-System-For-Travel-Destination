package tools

import com.google.gson.{Gson, JsonElement}
import dbpedia.{DBPediaClient, SpotlightClient}

/**
 * This class takes a location name and annotate the location with dbpedia data.
 */
object DBpediaLocationAnnotator extends App {

  //spotlight annotation
  val annotationSpotlight = (name: String) => {
    val response = new SpotlightClient().requestLocation(name)
    val annotation = new Gson().fromJson(response, classOf[JsonElement]).getAsJsonObject()
    val resources = annotation.getAsJsonArray("Resources").iterator().next()
    (annotation.get("@confidence"), resources.getAsJsonObject.get("@URI"), resources.getAsJsonObject.get("@types"))
  }

  //annotates location with dbpedia data
  def annotateLocation(locationName: String ) = {

    val dbpedia = new DBPediaClient()

    try {
      println (dbpedia.parseDBpediaPageOfLocation(annotationSpotlight(locationName)._2.toString.replaceAll("\"", "")))
    } catch {
      case e: NullPointerException => {
        println("No spotlight results found for: " + locationName)
        //TODO maybe multiple uris
        val result = dbpedia.findDBpediaLocation(locationName)
        if(!result.isEmpty) dbpedia.parseDBpediaPageOfLocation(result.head)
        else println("No dbpedia results found for: " + locationName)
      }
    }

  }

  annotateLocation("bla")
  annotateLocation("Berlin")
  annotateLocation("Peguera")
  annotateLocation("Germany")
  annotateLocation("Antalya")
  annotateLocation("Hannover")


  //TODO paras params path and wiki
  //dummy
  val path = ""
  val dump: DumpXMLReader = new TravelerPointReader(path)


}
