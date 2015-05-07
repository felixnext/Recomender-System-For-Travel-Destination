package core

import elasticsearch.ElasticsearchClient

/**
 * Finds all relations within elasticsearch index that are similar two set of relations.
 * All retrieved documents are aggregated on location attribute and new score is computed.
 */
object RelationLocationFinder {

  //Finds all relations within elasticsearch index that are similar two set of relations.
  //All retrieved documents are aggregated on location attribute and new score is computed.
  def findLocations(relations: List[RawRelation]): Seq[(String,Double)] = {

    val elastic = new ElasticsearchClient
    val foundRelations = relations.map(r => elastic.findSimilarRelations(r)).flatten

    //group on the same document id
    val grouped = foundRelations.groupBy(_._2.id)
    //caculate score based on relation tf-idf score and elastic score
    val rescoredResult = grouped.map(l => l._2.map(t => (t._1 * t._2.tfIdf, t._2)))

    //aggregate all relation which belong to the same document together
    val aggregated = rescoredResult.map{doc =>
      val maxScore = doc.map(r => r._1).max
      val normalized = doc.map(r => r._1/maxScore)
      (doc.head._2.locationName, normalized.sum)
    }

    //normalize score
    val maxScore = aggregated.map(x => x._2).max
    aggregated.map(l => (l._1,l._2/maxScore)).toSeq
  }

}
