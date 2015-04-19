package tools

import com.typesafe.config.ConfigFactory

import scala.util.Try

/**
 * Reads config file and returns configurations.
 */
object Config {


  val config = ConfigFactory.load()

  lazy val servicePort = Try(config.getInt("service.port")).getOrElse(8080)
  lazy val serviceHost = Try(config.getString("service.host")).getOrElse("localhost:8080/")

  lazy val elasticsearchUrl = Try(config.getString("elastic.url")).getOrElse("localhost:9200/")

  lazy val elasticsearchIndices = Try({
    var scalaList: List[String] = List()
    val javaList = config.getStringList("elastic.indices")
    for(index <- 0 until javaList.size())
      scalaList = scalaList :+ javaList.get(index)
    scalaList
  }).getOrElse(List(""))

  lazy val dbpediaUrl = Try(config.getString("dbpedia.url")).getOrElse("http://dbpedia.org/sparql")
  lazy val dbpediaLookup = Try(config.getString("dbpedia.lookup")).getOrElse("http://lookup.dbpedia.org/api/search/KeywordSearch?")

  lazy val spotlightUrl = Try(config.getString("spotlight.url")).getOrElse("http://spotlight.dbpedia.org")
  lazy val spotlightConfidence = Try(config.getDouble("spotlight.confidence")).getOrElse(0.5)
  lazy val spotlightSupport = Try(config.getInt("spotlight.support")).getOrElse(10)

  lazy val clavinUrl = Try(config.getString("clavin.url")).getOrElse("http://134.169.32.169:9093")
}
