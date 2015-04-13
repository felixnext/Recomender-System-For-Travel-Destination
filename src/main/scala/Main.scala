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
  */

  val s: String = s"The official name reflects its history as a member of the medieval Hanseatic League, as a free imperial city of the Holy Roman Empire, a city-state, and one of the 16 states of Germany. Before the 1871 Unification of Germany, it was a fully sovereign state. Prior to the constitutional changes in 1919, the stringent civic republic was ruled by a class of hereditary grand burghers or Hanseaten."


  val openie = new OpenIE(triples = true)


  val result = openie.extract(s)

  for(r <- result) {
    println()
    println()
    println(r)
    println()
    println()
  }


  val openie2 = new OpenIE()


  val result2 = openie2.extract(s)

  for(r <- result2) {
    println()
    println()
    println(r)
    println()
    println()
  }

  /*
  val clavin = new ClavinClient()
  val s = "Paris is a nice cite."
  println(clavin.extractLocations(s))
  */
}
