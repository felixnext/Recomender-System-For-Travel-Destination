package core

import clavin.{ClavinClient, Location => CL}
import dbpedia.{DBPediaLookup, Location => L}
import elasticsearch.{ElasticLocationDoc, ElasticsearchClient}
import tools.Config
import tools.Math._

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

/**
 * Locations in the result set, which are within a certain radius should be combined together.
 * This object provides functionality for merging locations of sparql, elasticsearch and relation KB results.
 */
object Merge {

  type Matrix = Array[Array[(Int, Int, Option[Double])]]
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
  val coordinatesReplace: Seq[Location] => Seq[Location] => Seq[Location] = seedLocations => locations => {
    val knownCoord = seedLocations.filter(l => l.lat.isDefined)
    val missingCoord = locations.filter(l => !l.lat.isDefined)
    val replaced = missingCoord.map { l1 =>
      val c = ClavinClient.extractLocations(l1.name)
      if (c.size > 0) new Location(l1.name, Some(c.head.lat), Some(c.head.lon), l1.score)
      else knownCoord.find(l2 => l2.name.equals(l1.name)) match {
        case Some(l) => new Location(l1.name, l.lat, l.lon, l1.score)
        case None => l1
      }
    }
    locations.filter(l => l.lat.isDefined) ++ replaced
  }

  //replace missing coordinates in cluster if possible
  val coordinatesReplaceCluster: Seq[Cluster] => Seq[Cluster] = cluster => {
    val locations = cluster.flatMap(c => c.ls)
    val crFc = coordinatesReplace(locations)
    cluster.foreach(c => c.ls = crFc(c.ls).toArray)
    cluster
  }

  lazy val dd = distanceDecayFunction(Config.innerR)(Config.outerR)

  //computes matrix where each cell is a distance between two clusters
  private def computeDistanceMatrix(clusters: Seq[Cluster]): Matrix = {

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

  def merge(location: Seq[Location]) = {
    //create clusters with single element
    val clusters = coordinatesReplace(location)(location).map(l => new Cluster(l))
    //reunion recursion
    findBestMatch(clusters)
  }

  @tailrec
  private def findBestMatch(clusters: Seq[Cluster]): Seq[Cluster] = {

    val matrix: Matrix = computeDistanceMatrix(clusters)

    //find best match, two cluster that have maximal similarity (decay distance)
    //join only if the similarity is greater than  DISTANCE_THRESHOLD
    var i = 0

    //variables for holding two clusters with best score
    var c1, c2 = -1
    var s = -1.0

    //find best merge candidate
    while (i < matrix.length) {
      val column = matrix(i)
      var j = 0
      while (j < column.length) {
        val cell = column(j)
        if (cell._3.isDefined && cell._3.get > s && cell._3.get > MERGE_THRESHOLD) {
          c1 = cell._1
          c2 = cell._2
          s = cell._3.get
        }
        j += 1
      }
      i += 1
    }

    //if best match exists, then join two columns and recompute matrix
    if (s > 0 && c1 >= 0 && c2 >= 0) {
      //union two most similar clusters
      val newClusters = reunion(clusters)(c1)(c2)
      findBestMatch(newClusters)
    } else {
      //finished
      clusters
    }
  }

  //union two elements and return new list of cluster with the new element
  //the origin elements going to be removed
  val reunion: Seq[Cluster] => Int => Int => Seq[Cluster] = clusters => c1 => c2 => {
    val firstCluster = clusters(c1)
    val secondCluster = clusters(c2)

    //creates new list of clusters with both elements which going to be merged
    val filteredClusters = clusters.filter(c => c != firstCluster && c != secondCluster)

    //merge two clusters
    val mergedCluster = firstCluster.add(secondCluster)

    //add two cluster to the list
    filteredClusters :+ mergedCluster
  }


  //combines sparql, elasticsearch and relation KB results
  def combine(s: Seq[Cluster], e: Seq[Cluster], r: Seq[Cluster]): Seq[CombinedCluster] = {
    val coordinatesReplaced = coordinatesReplaceCluster(s ++ e ++ r)

    val matrix = computeDistanceMatrix(coordinatesReplaced).flatten

    //inverse function two determine source of given cluster based on index
    val source: Int => SourceEnum = index => if (index < s.size) Sparql else if (index < e.size + s.size) Elastic else RelaltionKB

    //add scores to CombinedCluster w.r.t. source
    val addScoreToCluster: CombinedCluster => SourceEnum => Cluster => Unit = cc => t => c => t match {
      case Sparql =>
        if (!cc.sparqlScore.isDefined) cc.sparqlScore = Some(c.decayScore)
        else cc.sparqlScore = Some(decaySum(Seq(c.decayScore,cc.sparqlScore.get)))
      case Elastic =>
        if (!cc.elasticScore.isDefined) cc.elasticScore = Some(c.decayScore)
        else cc.elasticScore = Some(decaySum(Seq(c.decayScore,cc.elasticScore.get)))
      case RelaltionKB =>
        if (!cc.rkbScore.isDefined) cc.rkbScore = Some(c.decayScore)
        else cc.rkbScore = Some(decaySum(Seq(c.decayScore,cc.rkbScore.get)))
    }

    //wrapper for merging two clusters
    def addScoresToCluster(c: CombinedCluster, i1: Int, i2: Int, c1: Cluster, c2: Cluster) = {
      addScoreToCluster(c)(source(i1))(c1)
      addScoreToCluster(c)(source(i2))(c2)
    }

    val usedCluster = new ListBuffer[Int]
    val combinedClusters = new ListBuffer[CombinedCluster]
    //create new clusters for elements that should be merged
    for (cell <- matrix if cell._3.isDefined && cell._3.get > MERGE_THRESHOLD) {
      usedCluster += cell._1
      usedCluster += cell._2
      val c1 = coordinatesReplaced(cell._1)
      val c2 = coordinatesReplaced(cell._2)

      lazy val geoNames = c1.geoNames ++ c2.geoNames
      lazy val bestName = if (geoNames.length > 0) Cluster.bestName(geoNames) else (c1.ls ++ c2.ls).maxBy(l => l.score).name
      lazy val possibleNames = c1.ls.map(l => l.name) ++ c2.ls.map(l => l.name) :+ bestName

      val coordinatesMean: Option[Double] => Option[Double] => Option[Double] = c1 => c2 => {
        val defined = Seq(c1, c2).filter(c => c.isDefined).map(c => c.get)
        if (defined.size > 0) Some(defined.sum / defined.size.toDouble) else None
      }

      //if that cluster was processed then add new data to existing one else create new one
      combinedClusters.find(c => possibleNames.exists(name => name.equals(c.name))) match {
        case Some(c) => addScoresToCluster(c, cell._1, cell._2, c1, c2)
        case None =>
          val c = new CombinedCluster(bestName, coordinatesMean(c1.lat)(c2.lat), coordinatesMean(c1.lon)(c2.lon))
          combinedClusters += c
          addScoresToCluster(c, cell._1, cell._2, c1, c2)
      }

    }

    //keep elements without match
    val indexed = coordinatesReplaced.view.zipWithIndex
    val unused = indexed.map(c => c._2).filter(i1 => !usedCluster.contains(i1))

    val usedObj = new ListBuffer[Cluster]
    val usedIndex = new ListBuffer[Int]
    val cc = for (i <- unused if !usedIndex.contains(i)) yield {
      val cluster = coordinatesReplaced(i)

      //try to find other clusters with equal name
      val equalNamed = coordinatesReplaced.filter(c => cluster != c && cluster.name.equals(c.name))

      val c = new CombinedCluster(cluster.name, cluster.lat, cluster.lon)

      for (eq <- equalNamed if !usedObj.contains(eq)) {
        indexed.find { case (k, v) => k == eq } match {
          case Some((k, v)) =>
            addScoreToCluster(c)(source(v))(eq)
            usedObj += eq
            usedIndex += v
          case None =>
        }
      }

      addScoreToCluster(c)(source(i))(cluster)
      c
    }

    val combined = combinedClusters ++ cc

    //retrieve popularity score
    combined.foreach { c =>
      DBPediaLookup.findDBPediaURI(c.name).find(lookup => lookup.score - 1.0 < eps) match {
        case Some(l) => c.popularityScore = Some(l.refcount)
        case None =>
      }
    }

    //normalize popularity score
    val popularityNormalizer = normalizedScore(combined.filter(c => c.popularityScore.isDefined).map(c => c.popularityScore.get))
    combined.foreach(c => if (c.popularityScore.isDefined) c.popularityScore = Some(popularityNormalizer(c.popularityScore.get)))
    combined.toSeq
  }

  sealed trait SourceEnum

  case object Sparql extends SourceEnum

  case object Elastic extends SourceEnum

  case object RelaltionKB extends SourceEnum

}

case class Location(name: String, lat: Option[Double], lon: Option[Double], score: Double)

case class CombinedCluster(name: String, lat: Option[Double], lon: Option[Double], var elasticScore: Option[Double] = None,
                           var sparqlScore: Option[Double] = None, var rkbScore: Option[Double] = None,
                           var popularityScore: Option[Double] = None)

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

  lazy val geoNames: Array[List[CL]] = ls.map(l => ClavinClient.extractLocations(l.name)).filter(l => l.size > 0)

  //the new name of this cluster
  //the general name in that cluster will be taken
  //e.g. in cluster with elements "New York City" and "Statue of Liberty" the name will be "New York City"
  lazy val name = {
    if (geoNames.length > 0) Cluster.bestName(geoNames)
    else ls.maxBy(l => l.score).name
  }

  //decay score is computed based on decay sum and corresponds to new score of  grouped locations
  lazy val decayScore = decaySum(ls.map(l => l.score))

  lazy val lat = {
    val l = ls.map(l => l.lat).filter(l => l.isDefined).map(l => l.get)
    if (l.length > 0) Some(l.sum / l.length) else None
  }

  lazy val lon = {
    val l = ls.map(l => l.lon).filter(l => l.isDefined).map(l => l.get)
    if (l.length > 0) Some(l.sum / l.length) else None
  }

}

object Cluster {
  val bestName: Array[List[CL]] => String = geoNames =>
    geoNames.foldLeft(geoNames.head.head.population, geoNames.head.head.name)((acc, l) =>
      if (acc._1 < l.head.population) (l.head.population, l.head.name) else acc)._2
}
