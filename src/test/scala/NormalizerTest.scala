
import core.Normalizer
import org.scalatest._

/**
 * Created by yevgen on 14.05.15.
 */
class NormalizerTest extends FlatSpec with Matchers{

  "Each normalized value" should "be smaller then 1.0" in {
    val scores = Seq(1.0,2.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,8.0,9.0,0.1,0.2,0.3)

    //create normalizer
    val myNormalizer = Normalizer.normalizedScore(scores)

    //normalize scores
    val normalizedScore = scores.map(s => myNormalizer(s))

    normalizedScore.foreach(s => s should be < 1.0)
    normalizedScore.foreach(s => s should be >= 0.0)
    normalizedScore.sum should be <= 1.0
  }
}
