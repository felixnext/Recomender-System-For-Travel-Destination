package tools

import com.typesafe.config.ConfigFactory

import scala.util.Try
import scala.collection.JavaConversions._

/**
 * Reads config file and returns configurations.
 */
object Config {


  val config = ConfigFactory.load()

  lazy val servicePort = Try(config.getInt("service.port")).getOrElse(8080)
  lazy val serviceHost = Try(config.getString("service.host")).getOrElse("localhost:8080/")
  lazy val numberOfActors = Try(config.getInt("service.number-of-actors")).getOrElse(1)

  lazy val elasticsearchUrl = Try(config.getString("elastic.url")).getOrElse("localhost:9200/")

  lazy val elasticsearchIndices = Try({
    config.getStringList("elastic.indices").toList
  }).getOrElse(List(""))

  lazy val dbpediaUrl = Try(config.getString("dbpedia.url")).getOrElse("http://dbpedia.org/sparql")
  lazy val dbpediaLookup = Try(config.getString("dbpedia.lookup")).getOrElse("http://lookup.dbpedia.org/api/search/KeywordSearch?")

  lazy val spotlightUrl = Try(config.getString("spotlight.url")).getOrElse("http://spotlight.dbpedia.org")
  lazy val spotlightConfidence = Try(config.getDouble("spotlight.confidence")).getOrElse(0.5)
  lazy val spotlightSupport = Try(config.getInt("spotlight.support")).getOrElse(10)

  lazy val clavinUrl = Try(config.getString("clavin.url")).getOrElse("http://134.169.32.169:9093")

  lazy val numberOfSparkCores = Try(config.getInt("spark.number-of-nodes")).getOrElse(1)

  lazy val decaySensitivity = Try(config.getDouble("decay.distance-sensitivity")).getOrElse(0.5)
  lazy val innerR = Try(config.getDouble("decay.inner-radius")).getOrElse(5.0)
  lazy val outerR = Try(config.getDouble("decay.outer-radius")).getOrElse(25.0)

}
