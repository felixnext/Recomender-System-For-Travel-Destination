/**
 * Created by yevgen on 11.04.15.
 */

import clavin.ClavinClient
import edu.knowitall.openie.{TemporalArgument, SpatialArgument, SimpleArgument, OpenIE}
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.postag.OpenNlpPostagger
import edu.knowitall.tool.tokenize.OpenNlpTokenizer

import elasticsearch.ElasticsearchClient

object Main  extends App{
  /*
  val c = new ElasticsearchClient()
  val r  = c.matchQuery("tunnel of love ukraine").flatten

  r.foreach{
    a => println(a.title)
  }

  val s: String = s"Furthermore, Place has a ancient culture places. That place should have a good opportunity for surfing."


  val openie = new OpenIE()


  val tokenizertest = new OpenNlpTokenizer()
  val postaggertest = new OpenNlpPostagger(tokenizertest)
  val chunkertest = new OpenNlpChunker(postaggertest)

  val result = openie.extract(s, chunkertest)

  for(r <- result) {
    println()
    println()
    println(r.extraction.context)
    println()
    println()
  }
  */

  val clavin = new ClavinClient()
  val s = "Paris is a nice cite."
  println(clavin.extractLocations(s))

}
