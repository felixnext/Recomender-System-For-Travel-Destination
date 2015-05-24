import _root_.tools.Config
import org.scalatest._
import core.{Merge, Location}

/**
 * Created by yevgen on 17.05.15.
 */
class MergeTest extends FlatSpec with Matchers {


  val locationsElastic = {
    val l1 = new Location("Hamburg", Some(53.550556), Some(9.993333), 0.11)
    val l2 = new Location("Hamburg Hafen", Some(53.54), Some(9.982778), 0.12)
    val l3 = new Location("Bremen", Some(53.075878), Some(8.807311), 0.13)
    val l4 = new Location("Bremen", None, None, 0.14)
    val l5 = new Location("New York City", Some(40.712778), Some(-74.005833), 0.15)
    val l6 = new Location("Statue of Liberty", Some(40.68923), Some(-74.04447), 0.16)
    val l7 = new Location("Paris", None, None, 0.14)
    val l8 = new Location("Times Square", Some(40.755833), Some(-73.986389), 0.17)
    val l9 = new Location("Madrid", None, None, 0.17)
    val l10 = new Location("Unknown", None, None, 0.0)
    
    Seq(l1,l2,l3,l4,l5,l6,l7,l8,l9, l10)   
  }

  val locationsSparql = {
    val l1 = new Location("Hamburg", Some(53.550556), Some(9.993333), 0.51)
    val l2 = new Location("Hamburg Hafen", Some(53.54), Some(9.982778), 0.52)
    val l4 = new Location("Bremen", None, None, 0.14)
    val l5 = new Location("New York City", Some(40.712778), Some(-74.005833), 0.55)
    val l6 = new Location("Statue of Liberty", Some(40.68923), Some(-74.04447), 0.56)
    val l7 = new Location("Paris", Some(48.856667), Some(2.351667), 0.54)
    val l8 = new Location("Times Square", Some(40.755833), Some(-73.986389), 0.57)
    val l9 = new Location("Madrid", None, None, 0.57)
    val l10 = new Location("Unknown", Some(40.755833), Some(-73.986389), 0.0)

    Seq(l1,l2,l4,l5,l6,l7,l8,l9, l10)
  }

  val locationsRKB = {
    val l3 = new Location("Bremen", Some(53.075878), Some(8.807311), 0.93)
    val l4 = new Location("Bremen", None, None, 0.14)
    val l5 = new Location("New York City", Some(40.712778), Some(-74.005833), 0.95)
    val l6 = new Location("Statue of Liberty", Some(40.68923), Some(-74.04447), 0.96)
    val l8 = new Location("Times Square", Some(40.755833), Some(-73.986389), 0.97)
    val l9 = new Location("Madrid", None, None, 0.97)

    Seq(l3,l4,l5,l6,l8,l9)
  }
 

  val radius = Config.innerR

  s"All locations with distance smaller than $radius" should "be grouped together to a single cluster" in {

    val clustersElastic = Merge.merge(locationsElastic)

    clustersElastic.size should be (6)
    /*
    clustersElastic.foreach{c =>
      println("Cluster name: " + c.name + "\nScore: " + c.decayScore)
      println(c.ls.mkString("\n"))
      println("\n\n\n")
    }
    */

    val clustersSparql = Merge.merge(locationsSparql)

    val clustersRKB = Merge.merge(locationsRKB)

    val combined = Merge.combine(clustersSparql, clustersElastic, clustersRKB)
    combined.size should be (5)

    /*
    combined.foreach{c =>
      println("Cluster name: " +c.name)
      println("lat " + c.lat)
      println("lon " + c.lon)
      println("sparql: " + c.sparqlScore)
      println("elastic: " + c.elasticScore)
      println("rkb: " + c.rkbScore)
      println("popularity: " + c.popularityScore)
      println("\n\n\n")
    }
    */

  }

}
