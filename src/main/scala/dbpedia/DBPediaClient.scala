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
  def parseDBpediaPageOfLocation(uri: String) = {
    val model: Model = ModelFactory.createDefaultModel()
    model.read(uri)
    val resource: Resource = model.getResource(uri)
    val iter = resource.listProperties()

    var resultMap: Map[String, Set[String] ] = Map()

    //there could exists multiple value for the same key
    //e.g. if multiple languages are spoken in a country
    def addToResultMap(key: String, value: String) = {
        val set = resultMap.getOrElse(key,Set())
        resultMap += (key -> (set + value) )
    }

    while (iter.hasNext) {
      val triple = iter.next().toString.split(",")

      if (triple(2).contains("POINT") ) {
        val tmp = triple(2).split("http")(0)
        val lat = tmp.substring(8,tmp.length-4).split(" ")(1)
        val long = tmp.substring(8,tmp.length-4).split(" ")(0)
        addToResultMap("lat", lat)
        addToResultMap("long", long)
      }


      def removeLastBraket (x: String): String = x.substring(0,x.length-1)
      def removeDBpediaURI (x: String): String = x.replace("http://dbpedia.org/resource/","")
      def cleanedDBpediaString: String = removeLastBraket(removeDBpediaURI(triple(2))).trim
      def cleanedDBPedoaNumber: String = triple(2).trim.split("\"")(1)

      def fahrenheitToCelsius(f: String) = (f.toFloat - 32.0)/1.8

      if(triple(1).contains("country")) {
        addToResultMap("country", cleanedDBpediaString)
      }

      if(triple(1).contains("elevation") || triple(1).contains("elevationM")) {
        addToResultMap("elevation", cleanedDBpediaString)
      }

      if(triple(1).contains("language") || triple(1).contains("officialLanguage")
        || triple(1).contains("languages")) {
        addToResultMap("language", cleanedDBpediaString)
      }

      if(triple(1).contains("http://dbpedia.org/ontology/areaTotal")) {
        addToResultMap("areaTotal", cleanedDBPedoaNumber)
      }

      //April
      if(triple(1).contains("aprHighC")) {
        addToResultMap("aprHighC", cleanedDBPedoaNumber)
      }

      if(triple(1).contains("aprLowC")) {
        addToResultMap("aprLowC", cleanedDBPedoaNumber)
      }

      if(triple(1).contains("aprMeanC")) {
        addToResultMap("aprMeanC", cleanedDBPedoaNumber)
      }
      

      if(triple(1).contains("aprSun")) {
        addToResultMap("aprSun", cleanedDBPedoaNumber)
      }



      //TODO weather



    }
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
        |   ?place rdf:type dbpedia-owl:Place .
        |   ?place rdfs:label "$name"@en .
        |}
      """.stripMargin

    val query: Query = QueryFactory.create(queryString)
    val qexec:QueryExecution = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query)

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
