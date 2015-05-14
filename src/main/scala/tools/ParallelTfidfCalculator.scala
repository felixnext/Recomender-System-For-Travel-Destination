package tools

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD

import scala.math._

/**
 * Calculates tf-idf score of relation in parallel.
 * Built on top of apache spark.
 */
object ParallelTfIdfCalculator extends App {

  lazy val nr = Config.numberOfSparkCores
  lazy val conf = new SparkConf().setAppName("Parallel-TF-IDF-Calculation").setMaster(s"local[$nr]")
  lazy val sc = new SparkContext(conf)

  //read relations from file
  lazy val relations = RelationsDeserializer.deserialize(args.head)

  //make relations available for corpus frequency calculation
  val broadcastRelations = sc.broadcast(relations)

  //make relations available for parallel processing (as input)
  val distRelations = sc.parallelize(relations)

  def tfIdf(rel: RDD[Relation], relBroadcast: Broadcast[Seq[Relation]],
            f: Seq[Relation] => Relation => Int ,sizeOfCorpora: Double): RDD[Relation] = {
    val countOccurrences = f(relBroadcast.value)

    val relWithTfIdf = rel.map{relation =>
      val tf = relation.tfIdf

      //approximation for speed up
      val occurrenceInCorpus = countOccurrences(relation).toDouble//if(tf == 1) 1 else countOccurrences(relation).toDouble

      //calculate Tf-Idf
      val tfIdf = tf * log10(sizeOfCorpora / occurrenceInCorpus)
      relation.tfIdf = tfIdf
      relation
    }

    relWithTfIdf
  }

  //calculate tf idf in parallel
  val relWithTfIdf = tfIdf(distRelations, broadcastRelations,
    RelationsUtils.countOccurrences, relations.size.toDouble).collect()

  //save the result
  lazy val writer = new JsonDumpWriter(args.head.replace(".json", "_idf.json"))
  relWithTfIdf.foreach(relation => writer.writeRelation(relation))

  sc.stop()

}

