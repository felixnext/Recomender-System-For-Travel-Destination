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
  /*
  val c = new ElasticsearchClient()
  val r  = c.matchQuery("tunnel of love ukraine").flatten

  r.foreach{
    a => println(a.title)
  }
  */

  /*
  val clavin = new ClavinClient()
  val s = "Paris is a nice cite."
  println(clavin.extractLocations(s))
  */

  val dbpediaLookup = new DBPediaLookup()
  dbpediaLookup.findDBPediaURI("berlin").foreach{
    x => println(x)
  }
}
