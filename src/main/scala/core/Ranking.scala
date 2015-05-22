package core

import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.mllib.classification.LogisticRegressionModel
import tools.Config

/**
 * Created by yevgen on 22.05.15.
 */
object Ranking {

  lazy val nr = Config.numberOfSparkCores
  lazy val conf = new SparkConf().setAppName("Ranking")
    .setMaster(s"local[$nr]")
    .set("spark.driver.maxResultSize", "0")
  lazy val sc = new SparkContext(conf)

  //TODO
  lazy val modelPath = ""
  lazy val model = LogisticRegressionModel.load(sc, modelPath)


  val rank: CombinedCluster => Double = c => {
    val vector = new DenseVector(Array(c.elasticScore.getOrElse(0.0), c.sparqlScore.getOrElse(0.0),
        c.rkbScore.getOrElse(0.0), c.popularityScore.getOrElse(0.0)))
    model.predict(vector)
  }

}
