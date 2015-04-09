package core

import elasticsearch.ElasticsearchClient
import tools.Config




/**
 * Starts the app.
 */
object Main extends App{

  /*
  val c = new ElasticsearchClient()
  val r  = c.phraseQuery("The island in spain.").flatten

  r.foreach{
    a => println(a)
  }
  */


  val server = new RESTfulServer()
  val conf = Config
  println(conf.spotlightUrl)
}

