package core

import clavin.ClavinClient
import dbpedia.{Location => L}
import elasticsearch.{ElasticLocationDoc, ElasticsearchClient}
import tools.Config
import tools.Math._
import clavin.{Location => CL}

import scala.annotation.tailrec

/**
 * Locations in the result set, which are within a certain radius should be combined together.
 * This object provides functionality for merging locations of sparql, elasticsearch and relation KB results.
 */
object Merge {

  type Matrix =  Array[Array[(Int,Int,Option[Double])]]
  type SparqlLocation = Iterable[(Iterable[L], Double)]

  def mergeElasticLocations(locations: List[ElasticLocationDoc]) = {
    val normalizer = normalizedScore(locations.map(x => x.score.get))
    val locationList = locations.map(x => new Location(x.title.get, x.lat, x.lon, normalizer(x.score.get)))
    merge(locationList)
  }

  def mergeSparqlLocations(locations: SparqlLocation) = {
    val locationList = locations.flatMap(ls => ls._1.map(l =>
      new Location(l.name, l.lat.map(v => v.toDouble), l.long.map(v => v.toDouble), ls._2)))
    merge(locationList.toSeq)
  }

  def mergeRelationLocations(locations: Iterable[(String, Double)]) = {
    val locationList = locations.map { l =>
      val docs = ElasticsearchClient.matchTitle(l._1).flatten
      val docWithGeopint = docs.find(doc => doc.lat.isDefined && doc.lon.isDefined)
      val lat = if (docWithGeopint.isDefined) docWithGeopint.get.lat else None
      val lon = if (docWithGeopint.isDefined) docWithGeopint.get.lon else None
      new Location(l._1, lat, lon, l._2)
    }
    merge(locationList.toSeq)
  }


  //replace missing coordinates if possible
  val coordinatesReplace: Seq[Location] => Seq[Location] = locations => {
    val missingCoord = locations.filter(l => !l.lat.isDefined)
    val knownCoord = locations.filter(l => l.lat.isDefined)
    val replaced = missingCoord.map{l1 =>
      knownCoord.find(l2 => l2.name.equals(l1.name)) match {
        case Some(l) => new Location(l1.name, l.lat, l.lon, l1.score)
        case None => l1
      }
    }
    knownCoord ++ replaced
  }

  def merge(location: Seq[Location]) = {

    val clusters = coordinatesReplace(location).map(l => new Cluster(l))

    val dd = distanceDecayFunction(Config.innerR)(Config.outerR)

    //computes matrix where each cell is a distance between two clusters
    def computeDistanceMatrix(clusters: Seq[Cluster]): Matrix = {

      var matrix: Matrix = Array[Array[(Int, Int, Option[Double])]]()

      //loop counter
      var i = 0
      //fill the matrix with distance values
      //create only triangular matrix due to symmetry
      while (i < clusters.length) {
        var j = i
        var a = Array[(Int, Int, Option[Double])]()
        while (j > 0) {
          j -= 1
          val c1 = clusters(i)
          val c2 = clusters(j)
          val d = {
            val d = c1.centroidDistance(c2)
            //apply decay function on distance
            if (d.isDefined) Some(dd(d.get)) else d
          }
          a = a :+(i, j, d)
        }
        matrix = matrix :+ a
        i += 1
      }
      matrix
    }

    val MERGE_THRESHOLD = 0.1

    @tailrec
    def reunion(clusters: Seq[Cluster]): Seq[Cluster] = {

      val matrix: Matrix = computeDistanceMatrix(clusters)


      //find best match, two cluster that have maximal similarity (decay distance)
      //join only if the similarity is greater than  DISTANCE_THRESHOLD
      var i = 0

      //variables for holding two clusters with best score
      var c1,c2 = -1
      var s = - 1.0

      //find best merge candidate
      while(i < matrix.length) {
        val column = matrix(i)
        var j = 0
        while(j < column.length) {
          val cell = column(j)
          if(cell._3.isDefined && cell._3.get > s && cell._3.get > MERGE_THRESHOLD) {
            c1 = cell._1
            c2 = cell._2
            s = cell._3.get
          }
          j += 1
        }
        i += 1
      }

      //if best match exists, then join two columns and recompute matrix
      if(s > 0 && c1 >= 0 && c2 >= 0) {
        val firstCluster = clusters(c1)
        val secondCluster = clusters(c2)

        //creates new list of clusters with both elements which going to be merged
        val filteredClusters = clusters.filter(c => c != firstCluster && c != secondCluster)

        //merge two clusters
        val mergedCluster = firstCluster.add(secondCluster)

        //add two cluster to the list
        val newClusters = filteredClusters :+ mergedCluster

        reunion(newClusters)
      } else {
        //finished
        clusters
      }
    }
    reunion(clusters)
  }

}

case class Location(name: String, lat: Option[Double], lon: Option[Double], score: Double)

object Location {
  //distance between two locations in km
  //if latitude and longitude is not defined then -1
  val distance: Location => Location => Double = l1 => l2 =>
    if (l1.lat.isDefined && l2.lat.isDefined && l1.lon.isDefined && l2.lon.isDefined)
      haversine(l1.lat.get, l1.lon.get, l2.lat.get, l2.lon.get)
    else -1.0

}

class Cluster(l: Location) {

  var ls = Array[Location](l)

  //adds elements from another cluster to that one
  def add(c: Cluster): Cluster = {
    ls = ls ++ c.ls
    this
  }

  //calculates a distance between two clusters based on centroid distance
  def centroidDistance(c: Cluster): Option[Double] = {
    val distances = for (l1 <- ls; l2 <- c.ls; d = Location.distance(l1)(l2) if d >= 0.0) yield d
    //assert(distances.length > 0)
    val sum = distances.sum
    if (sum > 0 || distances.length > 0) Some(sum / distances.length.toDouble)
    else None
  }

  //the new name of this cluster
  //the general name in that cluster will be taken
  //e.g. in cluster with elements "New York City" and "Statue of Liberty" the name will be "New York City"
  lazy val name = {
    val geoNames: Array[List[CL]] = ls.map(l => ClavinClient.extractLocations(l.name)).filter(l => l.size > 0)
    if(geoNames.length > 0) geoNames.foldLeft(geoNames.head.head.population,geoNames.head.head.name)((acc,l) =>
      if(acc._1 < l.head.population) (l.head.population,l.head.name) else acc)._2
    else ls.maxBy(l => l.score).name
  }

  //decay score is computed based on decay sum and corresponds to new score of  grouped locations
  lazy val decayScore = decaySum(ls.map(l => l.score))

}