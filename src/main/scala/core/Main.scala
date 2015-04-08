package core

import elasticsearch.ElasticsearchClient

import scalaj.http.Http



/**
 * Starts the app.
 */
object Main extends App{

  val c = new ElasticsearchClient()
  val r  = c.phraseQuery("The island in spain.").flatten

  r.foreach{
    a => println(a)
  }
}

