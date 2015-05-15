package core

import dbpedia.Location
import elasticsearch.ElasticLocationDoc
import tools.Math._

/**
 * Created by yevgen on 15.05.15.
 */
object Merge {

  type SparqlLocation =  Iterable[(Iterable[Location],Double)]

  def mergeElasticLocations(locations: List[ElasticLocationDoc]) = ???

  def mergeSparqlLocations(locations: SparqlLocation) = ???

  def mergeRelationLocations(locations: Iterable[(String, Int, Double)]) = ???




}
