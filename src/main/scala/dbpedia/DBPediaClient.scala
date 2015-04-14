package dbpedia

import com.hp.hpl.jena.query._
import com.hp.hpl.jena.rdf.model.{Model, ModelFactory, Resource}
import tools.Config


/**
 * This class represents dppedia client.
 */
class DBPediaClient {

  //downloads dbpedia page of given uri
  def parseDBpediaPageOfLocation(uri: String, test: Int): Option[Map[String, Set[String]]] = {
    var resultMap: Map[String, Set[String]] = Map()

    try {
      val model: Model = ModelFactory.createDefaultModel()

      try {
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


          def removeLastBracket(x: String): String = x.substring(0, x.length - 1)
          def removeDBpediaURI(x: String): String = x.replace("http://dbpedia.org/resource/", "")
          def cleanedDBpediaString: String = removeLastBracket(removeDBpediaURI(triple(2))).trim
          def cleanedDBPediaNumber: String = triple(2).trim.split("\"")(1)

          def fahrenheitToCelsius = ((cleanedDBPediaNumber.toFloat - 32.0) / 1.8).toString

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
            addToResultMap("areaTotal", cleanedDBPediaNumber)
          }

          if (triple(1).contains("http://dbpedia.org/ontology/populationTotal")) {
            addToResultMap("populationTotal", cleanedDBPediaNumber)
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
          if (weatherC.contains(prop)) addToResultMap(prop, cleanedDBPediaNumber)
          if (weatherF.contains(prop)) addToResultMap(prop.replace("F","C"), fahrenheitToCelsius)

          if (triple(1).contains("http://www.w3.org/2002/07/owl#sameAs")) {
            val Place = "(http://)(\\w\\w)(.dbpedia.org/resource/)(\\w+)".r
            try {
              triple(2).substring(0, triple(2).length - 1).trim match {
                case Place(a, b, c, name) => addToResultMap("sameAs", name)
              }
            } catch {
              case e: Exception => None
            }

          }
        }
      }

      finally {
        model.close()
      }

    } catch {
      case e: org.apache.jena.atlas.web.HttpException =>
        Thread.sleep(1000)
        if(test != 0) resultMap = parseDBpediaPageOfLocation(uri, test - 1).getOrElse(Map())
      case e: Exception => println(e)
    }


    if (resultMap.isEmpty) None else Some(resultMap)
  }

  //returns a list of uris, which matches the location name
  def findDBpediaLocation(name: String, test: Int): List[String] = {
    var uris: List[String] = List()
    val queryString =
      s"""
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         |PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>
         |
         |SELECT DISTINCT *
         |WHERE {
         |?place rdf:type dbpedia-owl:Place .
         |?place rdfs:label "$name"@en .
         |}
      """.stripMargin
    try {
      val query: Query = QueryFactory.create(queryString)
      val qexec: QueryExecution = QueryExecutionFactory.sparqlService(Config.dbpediaUrl, query)

      try {
        val results: ResultSet = qexec.execSelect()
        while (results.hasNext()) {
          val uri = results.next().get("place").toString
          uris = uris :+ uri
        }
      }
      finally {
        qexec.close()
      }

    } catch {
      case e: org.apache.jena.atlas.web.HttpException =>
        Thread.sleep(1000)
        if(test != 0) uris = findDBpediaLocation(name, test -1)
      case e: Exception => println(e)
    }
    uris
  }

  //tests if the given entity is a person
  def isPerson(name: String, test: Int): Boolean = {
    val queryString =
      s"""
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         |PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>
         |
         |SELECT DISTINCT *
         |WHERE {
         |?place rdf:type dbpedia-owl:Person .
         |?place rdfs:label "$name"@en
         |}
      """.stripMargin
    try {

      val query: Query = QueryFactory.create(queryString)
      val qexec: QueryExecution = QueryExecutionFactory.sparqlService(Config.dbpediaUrl, query)

      try {
        val results: ResultSet = qexec.execSelect()
        results.hasNext
      }
      finally {
        qexec.close()
      }

    } catch {
      case e: org.apache.jena.atlas.web.HttpException =>
        Thread.sleep(1000)
        if(test != 0) isPerson(name, test -1)
        else false
      case e: Exception => false
    }
  }


}
