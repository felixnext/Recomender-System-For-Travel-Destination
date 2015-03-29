package tools

import com.google.gson.{Gson, JsonElement}
import dbpedia.{DBPediaClient, RemoteSpotlightClient}

/**
 * Created by yevgen on 28.03.15.
 */
object DBpediaLocationAnnotator extends App {

  //spotlight annotation
  val annotationSpotlight = (name: String) => {
    val response = new RemoteSpotlightClient().requestLocation(name)
    val annotation = new Gson().fromJson(response, classOf[JsonElement]).getAsJsonObject()
    val resources = annotation.getAsJsonArray("Resources").iterator().next()
    (annotation.get("@confidence"), resources.getAsJsonObject.get("@URI"), resources.getAsJsonObject.get("@types"))
  }

  //annotates location with dbpedia data
  def annotateLocation(locationName: String ) = {
    val dbpedia = new DBPediaClient()

    try {
      dbpedia.parseDBpediaPageOfLocation(annotationSpotlight(locationName)._2.toString.replaceAll("\"", ""))
    } catch {
      case e: NullPointerException => {
        println("No spotlight results found for: " + locationName)
        //TODO maybe multiple uris
        dbpedia.parseDBpediaPageOfLocation(dbpedia.findDBpediaLocation(locationName).head)

      }
    }

  }

  annotateLocation("Hurghada")
  annotateLocation("Berlin")
  annotateLocation("Peguera")
  annotateLocation("Germany")
  annotateLocation("Antalya")
  annotateLocation("Hannover")

}
