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
  def annotateLocation(locationName: String): Option[Map[String, Set[String]]] = {
    val dbpedia = new DBPediaClient()
    var annotations: Option[Map[String, Set[String]]] = None
    try {
      annotations = Some(dbpedia.parseDBpediaPageOfLocation(annotationSpotlight(locationName)._2.toString.replaceAll("\"", "")))
    } catch {
      case e: NullPointerException => {
        println("No spotlight results found for: " + locationName)
        //TODO maybe multiple uris
        val result = dbpedia.findDBpediaLocation(locationName)
        if (!result.isEmpty) annotations = Some(dbpedia.parseDBpediaPageOfLocation(result.head))
        else println("No dbpedia results found for: " + locationName)
      }
    }
    annotations
  }


  println(annotateLocation("Majorca"))
  //############
  //Launch App
  //############
  /*
  if (args.length != 2) {
    println("Excpetion: Cannot read parameters \n Format: dump_type{wikipedia, trevelerswiki, trevelerpoint} path ")
  }
  else {
    val dump_type = args(0)
    val path = args(1)


    val xml: DumpXMLReader = dump_type match {
      case "wikipedia" => new Wikipedia(path)
      case "trevelerswiki" => new Travelerswiki(path)
      case "trevelerpoint" => new TravelerPoint(path)
      case _ => println("ERROR: Dump source dosn't match any of known sources"); null
    }

    var i = 10
    while (xml.hasMorePages && i != 0) {
      //Future {
        var page: Map[String, Map[String, Set[String]]] = xml.readPage
        //assumption: title is not empty
        val title = page.getOrElse("title", Map("" -> Set(""))).getOrElse("title", Set("")).head

        annotateLocation(title) match {
          case Some(annotation) => page += ("dbpedia" -> annotation)
          case None => page += ("dbpedia" -> Map("" -> Set("")))
        }

        xml.writePage(page)
      i -= 1
      //}
    }

    xml.close()

  }
  */

}
