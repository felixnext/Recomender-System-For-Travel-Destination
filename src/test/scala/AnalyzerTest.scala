
import org.scalatest._
import nlp.TextAnalyzerPipeline

/**
 * Created by yevgen on 29.04.15.
 */
class AnalyzerTest extends FlatSpec with Matchers {

  val analyzerPipeline = new TextAnalyzerPipeline

  "The intersection between (1,2) and (3,4)" should "be desjoint" in {
    analyzerPipeline.intersect((1,2),(3,4)) should be (right = false)
  }

  "The elements (1,10) and (9,11)" should "intersect" in {
    analyzerPipeline.intersect((1,10),(9,11)) should be (right = true)
  }


}
