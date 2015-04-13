/**
 * Created by yevgen on 11.04.15.
 */

import clavin.ClavinClient
import dbpedia.DBPediaLookup
import edu.knowitall.openie.{TemporalArgument, SpatialArgument, SimpleArgument, OpenIE}
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.postag.OpenNlpPostagger
import edu.knowitall.tool.tokenize.OpenNlpTokenizer

import elasticsearch.ElasticsearchClient

object Main  extends App{

  val c = new ElasticsearchClient()
  val r  = c.distanceQuery(1000000,39.616665,2.983333)

  r.foreach{
    a => println(a)
  }


  /*
  val clavin = new ClavinClient()
  val s = "Paris is a nice cite."
  println(clavin.extractLocations(s))
  */


}
