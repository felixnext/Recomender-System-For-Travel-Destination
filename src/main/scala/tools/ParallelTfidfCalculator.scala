package tools

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

/**
 * Calculates tf-idf score of relation in parallel.
 * Built on top of apache spark.
 */
object ParallelTfidfCalculator extends App {

  lazy val nr = Config.numberOfSparkCores
  lazy val conf = new SparkConf().setAppName("Parallel-TF-IDF-Calculation").setMaster(s"local[$nr]")
  lazy val sc = new SparkContext(conf)

  //read relations from file
  lazy val relations = RelationsDeserializer.deserialize(args.head)

  //make relations available for parallel processing
  lazy val distRelations = sc.parallelize(relations)




}

