package tools.script

import core.QueryHandler
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.mllib.linalg.DenseVector
import tools.Config

/**
 * Created by yevgen on 20.05.15.
 */
object TrainDataCreator extends App {

  lazy val nr = Config.numberOfSparkCores
  lazy val conf = new SparkConf().setAppName("Extract-Training-Data")
    .setMaster(s"local[$nr]")
    .set("spark.driver.maxResultSize", "0")
  lazy val sc = new SparkContext(conf)

  lazy val data = {
    val articles = for(file <- args) yield {
      val reader = new JsonDumpReader(file)
      for(article <- reader) yield article
    }
    articles.flatMap(i => i.toSeq)
  }

  lazy val articles = sc.parallelize(data, Config.numberOfSparkTasks).cache()

  lazy val handler = new QueryHandler

  lazy val trainData = articles.flatMap{ article =>
    if(article.text.size > 0 && article.text.head.length > 20){
      val query =  article.text.head.replace(article.title, "location")
      val vectors = handler.handleQuery(query)
      vectors.map { vector =>
        val label = if (vector.name.equalsIgnoreCase(article.title)) 1.0 else 0.0
        new LabeledPoint(label, new DenseVector(Array(vector.elasticScore.getOrElse(0.0), vector.sparqlScore.getOrElse(0.0),
          vector.rkbScore.getOrElse(0.0), vector.popularityScore.getOrElse(0.0))))
      }
    } else Seq()
  }

  MLUtils.saveAsLibSVMFile(trainData, args.head.replace(".json",".libsvm"))

}
