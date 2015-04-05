package tools

import com.google.gson.{Gson, JsonElement}
import dbpedia.{DBPediaClient, SpotlightClient}

import scalaj.http.{Http, HttpResponse}

/**
 * This class takes a location name and annotate the location with dbpedia data.
 */
object DBpediaLocationAnnotator extends App {

  //TODO annotate what ?
  //spotlight annotation
  val annotationSpotlight = (name: String) => {
    val response = new SpotlightClient().requestLocation(name,10)
    val annotation = new Gson().fromJson(response, classOf[JsonElement]).getAsJsonObject
    val resources = annotation.getAsJsonArray("Resources").iterator().next()
    (annotation.get("@confidence"), resources.getAsJsonObject.get("@URI"), resources.getAsJsonObject.get("@types"))
  }

  //annotates location with dbpedia data
  def annotateLocation(locationName: String): Option[Map[String, Set[String]]] = {
    val dbpedia = new DBPediaClient()
    def httpRequest(numberOfRequests: Int): HttpResponse[String] = {
      try {
        Http("http://dbpedia.org/page/" + locationName.replaceAll(" ", "_")).method("HEAD").asString
      }
      catch {
        case e: java.net.SocketTimeoutException => {
          println(e)
          if (numberOfRequests != 0) httpRequest(numberOfRequests - 1)
          //break the recursion
          else new HttpResponse[String]("", 404, Map())
        }
        case e: Exception => new HttpResponse[String]("", 404, Map())
      }
    }

    //find out the dbpedia resource url for further parsing
    val url = httpRequest(10).location match {
      case Some(l) => l
      case _ =>
        val urls = dbpedia.findDBpediaLocation(locationName)
        if (urls.nonEmpty) urls.head
        else try {
          annotationSpotlight(locationName)._2.toString.replaceAll("\"", "")
        }
        catch {
          case e: Exception => {
            println("No dbpedia result was found for: " + locationName)
            "http://dbpedia.org/resource/" + locationName.replaceAll(" ", "_")
          }
        }
    }
    dbpedia.parseDBpediaPageOfLocation(url)
  }

  //##################
  //#Launched the app#
  //##################

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
      case _ => println("ERROR: Dump source doesn't match any of known sources"); null
    }

    val dbpedia = new DBPediaClient()

    while (xml.hasMorePages) {
      // Future {
      var page: Map[String, Map[String, Set[String]]] = xml.readPage
      //assumption: title is not empty
      val title = page.getOrElse("title", Map("" -> Set(""))).getOrElse("title", Set("")).head

      if (!dbpedia.isPerson(title)) {
        //doesn't annotate persons
        annotateLocation(title) match {
          case Some(annotation) => page += ("dbpedia" -> annotation)
          case None => page += ("dbpedia" -> Map("" -> Set("")))
        }

        xml.writePage(page)

      }


    }
    //    }

    xml.close()

  }


}
