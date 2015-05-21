package tools.script

import core.QueryHandler
import org.apache.spark.{SparkContext, SparkConf}
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

  val data = ""


  lazy val handler = new QueryHandler


}
