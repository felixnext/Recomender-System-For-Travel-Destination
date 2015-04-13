package dbpedia

import tools.Config

import scalaj.http.{Http, HttpResponse}
import scala.xml._

/**
 * DBPedia lookup client. Makes lookup requests and parses responses.
 */
class DBPediaLookup {

  val url = Config.dbpediaLookup

  //DBpedia lookup request
  def findDBPediaURI(label: String, queryClass: Option[String] = None): List[LookupResult] = {
    try {
      val requestParam = queryClass match {
        case Some(qclass) => Map("QueryClass" -> qclass, "QueryString" -> label)
        case None => Map("QueryString" -> label)
      }
      val response: HttpResponse[String] = Http(url)
        .header("Content-Type", "text/plain").timeout(connTimeoutMs = 2000, readTimeoutMs = 10000).params(requestParam).asString
      parseResponse(response.body)
    } catch {
      case e: Exception => println("DBPedia lookup request error: " + e); List()
    }
  }


  //parses the xml response
  def parseResponse(response: String): List[LookupResult] = {
    try {
      val root = XML.loadString(response)
      val results = (root \ "Result").iterator
      val parsedResults = for(x <- results) yield {
          val label = (x \ "Label").text
          val uri = (x \ "URI").text
          val description = (x \ "Description").text
          val classes = (x \ "Classes" \ "Class" ).toList.map(node => ((node \ "Label").text,(node \ "URI").text) )
          val categories = (x \ "Categories" \ "Category").toList.map(node => ((node \ "Label").text,(node \ "URI").text))
          val refcount = (x \ "Refcount").text.toInt
         new LookupResult(label, uri, description, classes, categories, refcount)
      }
      parsedResults.toList
    }
    catch {
      case e: Exception => println("Error during parsing dbpedia lookup result: " + e); List()
    }
  }
}

//Represents DBpedia lookup response
case class LookupResult(label: String, uri: String, description: String, classes: List[(String,String)], categories: List[(String,String)], refcount: Int)
