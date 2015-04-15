/**
 * Created by yevgen on 11.04.15.
 */

import clavin.ClavinClient
import dbpedia.{SpotlightClient, DBPediaLookup}
import edu.knowitall.openie.{TemporalArgument, SpatialArgument, SimpleArgument, OpenIE}
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.postag.OpenNlpPostagger
import edu.knowitall.tool.tokenize.OpenNlpTokenizer

import elasticsearch.ElasticsearchClient
import nlp.StanfordAnnotator

object Main  extends App{

  val s = "New York is a state in the Northeastern and Mid-Atlantic regions of the United States. New York is the 27th-most extensive, the fourth-most populous, and the seventh-most densely populated of the 50 United States. New York is bordered by New Jersey and Pennsylvania to the south and Connecticut, Massachusetts, and Vermont to the east. The state has a maritime border with Rhode Island east of Long Island, as well as an international border with the Canadian provinces of Quebec to the north and Ontario to the west and north. The state of New York is often referred to as New York State or the State of New York to distinguish it from New York City, the state's most populous city and its economic hub."

/*
  val stanford = new StanfordAnnotator
  stanford.annotateText(s)
  stanford.annotateText(s + "bla")
*/

  val clavin = new ClavinClient()
  val l = "Paris is a nice cite."
  println(clavin.extractLocations(l))



}
