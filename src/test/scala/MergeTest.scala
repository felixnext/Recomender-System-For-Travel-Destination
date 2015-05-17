import _root_.tools.Config
import org.scalatest._
import core.{Merge, Location}

/**
 * Created by yevgen on 17.05.15.
 */
class MergeTest extends FlatSpec with Matchers {

  val l1 = new Location("Hamburg", Some(53.550556), Some(9.993333), 0.11)
  val l2 = new Location("Hamburg Hafen", Some(53.54), Some(9.982778), 0.12)
  val l3 = new Location("Bremen", Some(53.075878), Some(8.807311), 0.13)
  val l4 = new Location("Bremen", None, None, 0.14)
  val l5 = new Location("New York City", Some(40.712778), Some(-74.005833), 0.15)
  val l6 = new Location("Statue of Liberty", Some(40.68923), Some(-74.04447), 0.16)
  val l7 = new Location("Paris", None, None, 0.14)
  val l8 = new Location("Times Square", Some(40.755833), Some(-73.986389), 0.17)
  val l9 = new Location("Madrid", None, None, 0.17)


  val locations = Seq(l1,l2,l3,l4,l5,l6,l7,l8,l9)

  val radius = Config.innerR

  s"All locations with distance smaller than $radius" should "be grouped together to a single cluster" in {

    val clusters = Merge.merge(locations)

    clusters.size should be (5)

    clusters.foreach{c =>
      println("Cluster:")
      println(c.ls.mkString("\n"))
      println("\n\n\n")
    }
  }

}
