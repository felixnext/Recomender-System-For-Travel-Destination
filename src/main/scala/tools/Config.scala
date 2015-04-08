package tools

/**
 * Reafs config file and returns configurations.
 */
object Config {

  //TODO create config file and read properties from file

  def getElasticsearchUrl = {
    "http://134.169.32.163:9200/"
  }

  def getElasticIndices = {
    List("wikipedia", "travellerspoint", "wikitravel")
  }
}
