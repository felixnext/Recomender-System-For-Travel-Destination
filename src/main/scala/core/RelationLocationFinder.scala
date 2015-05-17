package core

import elasticsearch.ElasticsearchClient
import tools.Math._

/**
 * Finds all relations within elasticsearch index that are similar two set of relations.
 * All retrieved documents are aggregated on location attribute and new score is computed.
 */
object RelationLocationFinder {

  //Finds all relations within elasticsearch index that are similar two set of relations.
  //All retrieved documents are aggregated on location attribute and new score is computed.
  def findLocations(relations: List[RawRelation]): Iterable[(String, Double)] = {

    val foundRelations = relations.flatMap(r => ElasticsearchClient.findSimilarRelations(r))

    //group on the same document id
    //thus aggregates all relation which belong to the same document together
    val grouped = foundRelations.groupBy(_._2.id)
    //calculate score based on relation tf-idf score and elastic score
    val rescoreResult = grouped.map(l => l._2.map(t => (t._1 * t._2.tfIdf, t._2)))

    //aggregate the results and calculate new aggregated score
    val aggregated = rescoreResult.map{doc =>
      val score = doc.map(r => r._1).sum
      (doc.head._2.locationName, doc.head._2.id,  score)
    }

    //normalize score
    val normalizer = normalizedScore(aggregated.map(x => x._3))
    aggregated.map(location => (location._1, normalizer(location._3)))
  }

}