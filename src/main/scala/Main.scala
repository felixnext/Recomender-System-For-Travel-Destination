/**
 * Created by yevgen on 11.04.15.
 */

import clavin.ClavinClient
import dbpedia.{YagoGeoTypes, SpotlightClient, DBPediaLookup}
import edu.knowitall.openie.{TemporalArgument, SpatialArgument, SimpleArgument, OpenIE}

import elasticsearch.ElasticsearchClient
import nlp.{RelationExtractor, StanfordAnnotator}
import core.SparqlQueryCreator
import tools.Levenshtein

object Main  extends App{

  val s = "Hamburg is a state in the Northeastern and Mid-Atlantic regions of the United States. New York won 10 time award of a year. New York is the 27th-most extensive, the fourth-most populous, and the seventh-most densely populated of the 50 United States. New York is bordered by New Jersey and Pennsylvania to the south and Connecticut, Massachusetts, and Vermont to the east. The state has a maritime border with Rhode Island east of Long Island, as well as an international border with the Canadian provinces of Quebec to the north and Ontario to the west and north. New York state of that place is often referred to as New York State or the State of New York to distinguish it from New York City, the state's most populous city and its economic hub."

  /*
  val stanford = new StanfordAnnotator
  stanford.annotateText(s)
  //stanford.annotateText(s + "bla")
  */


  val query = new SparqlQueryCreator
  query.createSparqlQuery(s)

  /*
  val clavin = new ClavinClient()
  val l = "Paris is a nice cite."
  println(clavin.extractLocations(s))


  val openie = new RelationExtractor
  println(openie.extractRelations(s))


  val spotlightClient = new SpotlightClient
  println(spotlightClient.discoverEntities(s))

  */

/*
  val openie = new RelationExtractor
  openie.extractRelations(s).foreach{
    c => println(c)
  }
*/
/*
  val elastic = new ElasticsearchClient
  elastic.findDBPediaClasses("New York").foreach{
    x => println(x)
  }
*/
/*
  val openie = new RelationExtractor
  openie.extractRelations(s).foreach{
    x => println(x)
  }
*/

}
