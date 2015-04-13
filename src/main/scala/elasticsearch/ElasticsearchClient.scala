package elasticsearch

import com.google.gson.{JsonElement, JsonObject, Gson}
import tools.Config

import scala.annotation.tailrec
import scala.util.Try
import scalaj.http.Http

/**
  * This class represents a elasticsearch client.
 * It makes possible to query elasticsearch REST Api und to parse results.
 */
class ElasticsearchClient {

  // elasticsearch endpoint
  val elasticUrl = Config.elasticsearchUrl

  //indices that should be queried
  val indices: List[String] = Config.elasticsearchIndices

  //parses the elasticsearch response in json format and wraps the data into location object
  //responseBody is a string in json format
  def parseLocationResult(response: String): List[ElasticLocationDoc] = {

    @tailrec
    //fetch data from json strings and wraps them into location object
    def fetchData(document: java.util.Iterator[java.util.Map.Entry[String, JsonElement]], location: ElasticLocationDoc): ElasticLocationDoc = {
      if (!document.hasNext) location
      else {
        val keyValue = document.next()
        keyValue.getKey.toString match {
          case "country" => location.country = Try(Some(new Gson().fromJson(keyValue.getValue.getAsJsonArray, classOf[Array[String]]).toList)).getOrElse(None)
          case "title" => location.title = Try(Some(keyValue.getValue.getAsString)).getOrElse(None)
          case "sameAs" => location.someAs = Try(Some(new Gson().fromJson(keyValue.getValue.getAsJsonArray, classOf[Array[String]]).toList)).getOrElse(None)
          case "paragraph_texts" => location.paragraphTexts = Try(Some(new Gson().fromJson(keyValue.getValue.getAsJsonArray, classOf[Array[String]]).toList)).getOrElse(None)
          case "paragraph_names" => location.paragraphNames = Try(Some(new Gson().fromJson(keyValue.getValue.getAsJsonArray, classOf[Array[String]]).toList)).getOrElse(None)
          case "populationTotal" => location.populationTotal = Try(Some(keyValue.getValue.getAsInt)).getOrElse(None)
          case "areaTotal" => location.areaTotal = Try(Some(keyValue.getValue.getAsDouble)).getOrElse(None)
          case "location" =>
              location.lat = Try(Some(keyValue.getValue.getAsJsonObject.get("lat").getAsDouble)).getOrElse(None)
              location.lon = Try(Some(keyValue.getValue.getAsJsonObject.get("lon").getAsDouble)).getOrElse(None)
          case _ => location.climate = Some(location.climate.getOrElse(Map()) ++ Try(Map(keyValue.getKey -> keyValue.getValue.getAsString)).getOrElse(Map()))
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
      val jsonRoot = new Gson().fromJson(response, classOf[JsonObject])
      val jsonHits = jsonRoot.get("hits").getAsJsonObject
      val jsonHitsResults = jsonHits.get("hits").getAsJsonArray.iterator()
      iterateOverResults(jsonHitsResults ,List())
    } catch {
      case e: Exception => println("Excpetion during result parsing "+ e); e.printStackTrace(); List()
    }

  }


  //query all indices with given query string
  def matchQuery(query: String, topK: Int = 10, from: Int = 0): List[List[ElasticLocationDoc]] = {
    val jsonQuery =
      s"""
         |{
         |  "from" : $from,
         |  "size" : $topK,
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

    indices.map(index => parseLocationResult(request(jsonQuery, index)))
  }

  //match query with phrase rescoring
  def phraseQuery(query: String, topK: Int = 10, from: Int = 0) = {
    val jsonQuery =
      s"""
         |{
         |  "from" : $from,
         |  "size" : $topK,
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
    indices.map(index => parseLocationResult(request(jsonQuery, index)))
  }

  //returns all location from defined lat, lon point within given radius
  def distanceQuery(distance: Int, lat: Double, lon: Double, topK: Int = 10, from: Int = 0) = {
    //TODO is it ok? 110 km
    val jsonQuery =
      s"""
         |{
         |  "from" : $from,
         |  "size" : $topK,
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
    indices.map(index => parseLocationResult(request(jsonQuery, index)))
  }


  //returns all relevant dbpedia relations for a given raw text relation
  //for the relation analysing simple whitespace analyzer is used
  //due to type mapping all relations should be converted to lowercase
  def findPattyRelation(relation: String, topK: Int = 10, from: Int = 0): List[PattyRelation] = {
    val lowercase = relation.toLowerCase
    val jsonQuery =
    s"""
       |{
       |  "from" : $from,
       |  "size" : $topK,
       |  "query":{
       |    "match":{
       |      "text_relation":{
       |        "query":"$lowercase",
       |        "minimum_should_match":"30%"
       |      }
       |    }
       |  }
       |}
     """.stripMargin

    parsePattyResult(request(jsonQuery,"patty"))
  }

  //parses the patty index response and returns a list of retried relations
  //if any relation were found, the list is empty
  def parsePattyResult(response: String) = {
    try {
        val jsonRoot = new Gson().fromJson(response, classOf[JsonObject])
        val jsonHits = jsonRoot.get("hits").getAsJsonObject
        val jsonHitsResults = jsonHits.get("hits").getAsJsonArray.iterator()

      @tailrec
      def extractData(docIterator: java.util.Iterator[JsonElement], relations: List[PattyRelation] = List()): List[PattyRelation] = {
        if( !docIterator.hasNext) relations
        else {
          val result = docIterator.next().getAsJsonObject
          val score = result.get("_score").getAsDouble
          val dbpedia = result.get("_source").getAsJsonObject.get("dbpedia_relation").getAsString
          val relation = result.get("_source").getAsJsonObject.get("text_relation").getAsString
          val newList  = relations :+ new PattyRelation(dbpedia,relation,score)
          extractData(docIterator, newList)
        }
      }

      extractData(jsonHitsResults)
    } catch {
      case e: Exception => println("Error occurred during patty response parsing: " + e); List()
    }
  }

  //makes elastic search request
  //index that should be used for search
  def request(jsonQuery: String, index: String): String = {
    try {
      Http(elasticUrl + index +"/_search").postData(jsonQuery)
        .timeout(connTimeoutMs = 2000, readTimeoutMs = 7000).asString.body
    } catch {
      case e: Exception => println("Elasticsearch request exception" + e); "{}"
    }
  }
}


/**
 * Represents a location object returned from elasticsearch.
 * Description, coordinates and metadata is stored within the object.
 */
case class ElasticLocationDoc(var title: Option[String] = None,var  country: Option[List[String]] = None,var  someAs: Option[List[String]] = None,
                              var  paragraphTexts: Option[List[String]] = None,var  populationTotal: Option[Int] = None,
                              var areaTotal: Option[Double] = None,var  climate: Option[Map[String,String]] = None,var  index: Option[String] = None,
                              var  id: Option[Int] = None,var  score: Option[Double] = None, var paragraphNames: Option[List[String]] = None,
                              var lat: Option[Double] = None,var lon: Option[Double] = None)


/**
 * Represents founded patty relation
 */
case class PattyRelation(dbpediaRelation: String, rawTextRelation: String, score: Double)
