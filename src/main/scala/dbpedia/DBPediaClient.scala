package dbpedia

import com.hp.hpl.jena.query._
import com.hp.hpl.jena.rdf.model.{Model, ModelFactory, Resource}

import scalaj.http.{Http, HttpOptions}


/**
 * This class represents dppedia client.
 */
class DBPediaClient {

  def request(uri: String): String = {
    Http(uri).header("Accept", "application/rdf+xml").option(HttpOptions.followRedirects(true)).asString.body
  }

  //downloads dbpedia page of given uri
  def parseDBpediaPageOfLocation(uri: String): Option[Map[String, Set[String]]] = {
    var resultMap: Map[String, Set[String]] = Map()

    try {
      val model: Model = ModelFactory.createDefaultModel()
      model.read(uri)
      val resource: Resource = model.getResource(uri)
      val iter = resource.listProperties()



      //there could exists multiple value for the same key
      //e.g. if multiple languages are spoken in a country
      def addToResultMap(key: String, value: String) = {
        val set = resultMap.getOrElse(key, Set())
        resultMap += (key -> (set + value))
      }

      while (iter.hasNext) {
        val triple = iter.next().toString.split(",")

        if (triple(2).contains("POINT")) {
          val tmp = triple(2).split("http")(0)
          val lat = tmp.substring(8, tmp.length - 4).split(" ")(1)
          val long = tmp.substring(8, tmp.length - 4).split(" ")(0)
          addToResultMap("lat", lat)
          addToResultMap("long", long)
        }


        def removeLastBraket(x: String): String = x.substring(0, x.length - 1)
        def removeDBpediaURI(x: String): String = x.replace("http://dbpedia.org/resource/", "")
        def cleanedDBpediaString: String = removeLastBraket(removeDBpediaURI(triple(2))).trim
        def cleanedDBPedoaNumber: String = triple(2).trim.split("\"")(1)

        def fahrenheitToCelsius = ((cleanedDBPedoaNumber.toFloat - 32.0) / 1.8).toString

        if (triple(1).contains("country")) {
          addToResultMap("country", cleanedDBpediaString)
        }

        if (triple(1).contains("elevation") || triple(1).contains("elevationM")) {
          addToResultMap("elevation", cleanedDBpediaString)
        }

        if (triple(1).contains("language") || triple(1).contains("officialLanguage")
          || triple(1).contains("languages")) {
          addToResultMap("language", cleanedDBpediaString)
        }

        if (triple(1).contains("http://dbpedia.org/ontology/areaTotal")) {
          addToResultMap("areaTotal", cleanedDBPedoaNumber)
        }

        //Temperature measured in C
        val weatherC = List("novMeanC", "julLowC", "julMeanC", "aprLowC", "marHighC", "janHighC", "febMeanC", "augLowC",
          "aprHighC", "decHighC", "octLowC", "decLowC", "junMeanC", "janLowC", "sepHighC", "mayLowC", "marMeanC",
          "sepMeanC", "julHighC", "janMeanC", "decMeanC", "novHighC", "marLowC", "febLowC", "junHighC", "novLowC",
          "junLowC", "augMeanC", "mayHighC", "febHighC", "augHighC", "aprMeanC", "octHighC", "octMeanC", "sepLowC", "mayMeanC",
          "novSun", "sepSun", "octSun", "janSun", "junSun", "augSun", "marSun", "decSun", "maySun", "aprSun", "febSun", "julSun")

        //Temperature measured in F
        val weatherF = List("novMeanF", "julLowF", "julMeanF", "aprLowF", "marHighF", "janHighF", "febMeanF", "augLowF",
          "aprHighF", "deFHighF", "oFtLowF", "deFLowF", "junMeanF", "janLowF", "sepHighF", "mayLowF", "marMeanF",
          "sepMeanF", "julHighF", "janMeanF", "deFMeanF", "novHighF", "marLowF", "febLowF", "junHighF", "novLowF",
          "junLowF", "augMeanF", "mayHighF", "febHighF", "augHighF", "aprMeanF", "oFtHighF", "oFtMeanF", "sepLowF", "mayMeanF")

        val prop = triple(1).trim.replace("http://dbpedia.org/property/", "")
        if (weatherC.contains(prop)) addToResultMap(prop, cleanedDBPedoaNumber)
        if (weatherF.contains(prop)) addToResultMap(prop, fahrenheitToCelsius)

        if (triple(1).contains("http://www.w3.org/2002/07/owl#sameAs")) {
          val Place = "(http://)(\\w\\w)(.dbpedia.org/resource/)(\\w+)".r
          try {
            triple(2).substring(0,triple(2).length-1).trim match {
              case Place(a,b,c, name) => addToResultMap("sameAs", name)
            }
          } catch {
            case e: Exception => None
          }

        }
      }
    } catch {
      case e: Exception => println(e)
    }

    if(resultMap.isEmpty) None else Some(resultMap)
  }

  //returns a list of uris, which matches the location name
  def findDBpediaLocation(name: String): List[String] = {
    var uris: List[String] = List()
    val queryString =
      s"""
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         |PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>
         |
         |SELECT DISTINCT *
         |WHERE {
         |  ?place rdf:type dbpedia-owl:Place .
         |  ?place rdfs:label "$name"@en .
                                      |}
      """.stripMargin

    val query: Query = QueryFactory.create(queryString)
    val qexec: QueryExecution = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query)

    try {
      val results: ResultSet = qexec.execSelect()
      while (results.hasNext()) {
        val uri = results.next().get("place").toString
        uris = uris :+ uri
      }
    } catch {
      case e: Exception => println(e)
    }
    finally {
      qexec.close();
    }
    uris
  }


}