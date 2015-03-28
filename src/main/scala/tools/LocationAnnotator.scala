package tools

import com.google.gson.{JsonElement, Gson}
import spotlight.RemoteSpotlightClient

/**
 * Created by yevgen on 28.03.15.
 */
object LocationAnnotator extends App{


  val annotation = (name: String) => {
    val response = new RemoteSpotlightClient().requestLocation(name)
    val annotation = new Gson().fromJson(response, classOf[JsonElement]).getAsJsonObject()
    val ressources = annotation.getAsJsonArray("Resources").iterator().next()

    (annotation.get("@confidence"), ressources.getAsJsonObject.get("@URI"),ressources.getAsJsonObject.get("@types"))
  }


  def getLocation(locationName: String): (Float,Float) = {
    val spotlightClient = new RemoteSpotlightClient()
    spotlightClient.requestLocation(locationName)
    (0,0)
  }

  println(annotation("Berlin"))
}
