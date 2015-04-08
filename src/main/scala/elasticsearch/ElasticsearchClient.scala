package elasticsearch

import com.google.gson.{JsonElement, JsonObject, Gson}
import core.ElasticLocationDoc
import tools.Config

import scala.annotation.tailrec
import scalaj.http.Http

/**
  * This class represents a elasticsearch client.
 * It makes possible to query elasticsearch REST Api und to parse results.
 */
class ElasticsearchClient {

  //endpoint if elasticsearch
  val elasticUrl = Config.getElasticsearchUrl

  //indices that should be queried
  val indices = Config.getElasticIndices

  //parses the elasticsearch response in json format and wraps the data into location object
  //responseBody is a string in json format
  def parseResult(responseBody: String): List[ElasticLocationDoc] = {

    @tailrec
    //fetch data from json strings and wraps them into location object
    def fetchData(document: java.util.Iterator[java.util.Map.Entry[String, JsonElement]], location: ElasticLocationDoc): ElasticLocationDoc = {
      if (!document.hasNext) location
      else {
        val keyValue = document.next()
        keyValue.getKey.toString match {
          //TODO lat and lon
          case "country" => location.country = Some(new Gson().fromJson(keyValue.getValue.getAsJsonArray, classOf[Array[String]]).toList)
          case "title" => location.title = Some(keyValue.getValue.getAsString)
          case "sameAs" => location.someAs = Some(new Gson().fromJson(keyValue.getValue.getAsJsonArray, classOf[Array[String]]).toList)
          case "paragraph_texts" => location.paragraphTexts = Some(new Gson().fromJson(keyValue.getValue.getAsJsonArray, classOf[Array[String]]).toList)
          case "paragraph_names" => location.paragraphNames = Some(new Gson().fromJson(keyValue.getValue.getAsJsonArray, classOf[Array[String]]).toList)
          case "populationTotal" => location.populationTotal = Some(keyValue.getValue.getAsInt)
          case "areaTotal" => location.areaTotal = Some(keyValue.getValue.getAsDouble)
          case _ => location.climate = Some(location.climate.getOrElse(Map()) ++ Map(keyValue.getKey -> keyValue.getValue.getAsString))
        }
        fetchData(document, location)
      }
    }

    @tailrec
    //iterates over all documents in result set and retrieve data
    def iterateOverResults(docIterator: java.util.Iterator[JsonElement], locations: List[ElasticLocationDoc]): List[ElasticLocationDoc] = {
      if( !docIterator.hasNext) locations
      else {
        val result = docIterator.next().getAsJsonObject
        val index = result.get("_index").getAsString
        val id = result.get("_id").getAsInt
        val score = result.get("_score").getAsDouble
        val document: java.util.Iterator[java.util.Map.Entry[String, JsonElement]] = result.get("_source").getAsJsonObject.entrySet().iterator
        val location = new ElasticLocationDoc(id = Some(id), index = Some(index), score = Some(score))

        iterateOverResults(docIterator, locations ++ List(fetchData(document, location)))
      }
    }

    try {
      val jsonRoot = new Gson().fromJson(responseBody, classOf[JsonObject])
      val jsonHits = jsonRoot.get("hits").getAsJsonObject
      val jsonHitsResults = jsonHits.get("hits").getAsJsonArray.iterator()
      iterateOverResults(jsonHitsResults ,List())
    } catch {
      case e: Exception => println("Excpetion during result parsing"+ e); List()
    }

  }


  //query all indices with given query string
  def matchQuery(query: String): List[List[ElasticLocationDoc]] = {
    val jsonQuery =
      s"""
         |{
         |  "query":{
         |    "match":{
         |      "paragraph_texts":{
         |        "query":"$query",
         |        "minimum_should_match":"30%"
         |      }
         |    }
         |  }
         |}
       """.stripMargin

    indices.map(index => parseResult(request(jsonQuery, index)))
  }

  //match query with phrase rescoring
  def phraseQuery(query: String) = {
    val jsonQuery =
      s"""
         |{
         |  "query":{
         |    "match":{
         |      "paragraph_texts":{
         |        "query":"$query",
         |        "minimum_should_match":"30%"
         |      }
         |    }
         |  },
         |  "rescore":{
         |    "window_size":100,
         |    "query":{
         |      "rescore_query":{
         |        "match_phrase":{
         |          "paragraph_texts":{
         |            "query":"$query",
         |            "slop":50
         |          }
         |        }
         |      }
         |    }
         |  }
         |}
       """.stripMargin
    indices.map(index => parseResult(request(jsonQuery, index)))
  }

  //returns all location from defined lat, lon point within given radius
  def distanceQuery(distance: Int, lat: Double, lon: Double) = {
    //TODO is it ok? 110 km
    val jsonQuery =
      s"""
         |{
         |  "query": {
         |    "filtered": {
         |      "filter": {
         |        "geo_distance": {
         |          "distance": "$distance km",
         |          "location": {
         |            "lat":  $lat,
         |            "lon": $lon
         |          }
         |        }
         |      }
         |    }
         |  }
         |}
       """.stripMargin
    indices.map(index => parseResult(request(jsonQuery, index)))
  }

  //makes elastic search request
  def request(jsonQuery: String, index: String): String = {
    try {
      Http(elasticUrl + index +"/_search").postData(jsonQuery)
        .timeout(connTimeoutMs = 2000, readTimeoutMs = 7000).asString.body
    } catch {
      case e: Exception => println("Elasticsearch request exception" + e); "{}"
    }
  }
}

