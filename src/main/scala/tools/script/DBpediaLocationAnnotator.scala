package tools.script

import dbpedia.{DBPediaClient, SpotlightClient}
import tools.xml.{DumpXMLReader, TravelerPoint, Travelerswiki, Wikipedia}

import scalaj.http.{Http, HttpResponse}

/**
 * This class takes a location name and annotate the location with dbpedia data.
 */
object DBpediaLocationAnnotator extends App {

  //spotlight annotation
  lazy val spotlight = new SpotlightClient()
  val annotationSpotlight: String => String = name => spotlight.requestLocation(name, 10).head.uri

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
        val urls = dbpedia.findDBpediaLocation(locationName, 10)
        if (urls.nonEmpty) urls.head
        else try {
          annotationSpotlight(locationName)
        }
        catch {
          case e: Exception => {
            println("No dbpedia result was found for: " + locationName)
            "http://dbpedia.org/resource/" + locationName.replaceAll(" ", "_")
          }
        }
    }
    dbpedia.parseDBpediaPageOfLocation(url, 10)
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

      var page: Map[String, Map[String, Set[String]]] = xml.readPage
      //assumption: title is not empt
      val title = page.getOrElse("title", Map("" -> Set(""))).getOrElse("title", Set("")).head

      if (!dbpedia.isPerson(title, 10) && !title.equals("")) {
        //doesn't annotate persons
        annotateLocation(title) match {
          case Some(annotation) => page += ("dbpedia" -> annotation)
          case None => page += ("dbpedia" -> Map("" -> Set("")))
        }

        xml.writePage(page)

      }


    }

    xml.close()

  }


}
