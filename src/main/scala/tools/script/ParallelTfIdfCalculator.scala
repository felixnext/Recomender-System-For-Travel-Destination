package tools.script

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import tools._

import scala.math._

/**
 * Calculates tf-idf score of relations in parallel.
 * Built on top of apache spark.
 */
object ParallelTfIdfCalculator extends App {

  lazy val nr = Config.numberOfSparkCores
  lazy val conf = new SparkConf().setAppName("Parallel-TF-IDF-Calculation")
    .setMaster(s"local[$nr]")
    .set("spark.driver.maxResultSize", "0")
  lazy val sc = new SparkContext(conf)

  //read relations from file
  lazy val relations = RelationsDeserializer.deserialize(args.head)

  //make relations available for corpus frequency calculation
  val broadcastRelations = sc.broadcast(relations)

  //make relations available for parallel processing (as input)
  val distRelations = sc.parallelize(relations, Config.numberOfSparkTasks)
  val distRelationsCached = distRelations.cache()


  lazy val sizeOfCorpora = sc.broadcast(relations.size)

  lazy val relWithTfIdf = distRelations.map{relation =>
    lazy val rel = broadcastRelations.value
    lazy val countOccurrences = RelationsUtils.countOccurrences(rel)

    lazy val tf = relation.tfIdf

    //approximation for speed up
    lazy val occurrenceInCorpus =  if(tf == 1) 1 else countOccurrences(relation).toDouble

    //calculate Tf-Idf
    lazy val s = sizeOfCorpora.value
    lazy val tfIdf = tf * log10(s / occurrenceInCorpus)
    relation.tfIdf = tfIdf
    relation
  }

  //save the result
  lazy val writer = new JsonDumpWriter(args.head.replace(".json", "_idf.json"))
  lazy val collected = relWithTfIdf.collect()
  //write the relations to file
  collected.foreach(relation => writer.writeRelation(relation))

  sc.stop()

}

