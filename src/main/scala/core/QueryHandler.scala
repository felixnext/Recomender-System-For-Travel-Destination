package core

import dbpedia.DBPediaClient
import elasticsearch.{ElasticsearchClient, DeepParsing}
import nlp.TextAnalyzerPipeline

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try

/**
 * Created by yevgen on 20.05.15.
 */
class QueryHandler {

  val analyzingPipe = new TextAnalyzerPipeline

  def handleQuery(query: String) = {

    val t0 = System.nanoTime()

    //Elasticsearch location request
    val elastic = Future {
      val locations = DeepParsing(query, 50)
      Merge.mergeElasticLocations(locations)
    }

    val annotatedText = analyzingPipe.analyzeText(query)

    //sparql pieline
    val sparql = Future {
      val queries = SparqlQueryCreator.createSparqlQuery(annotatedText)
      println("Number of queries: " + queries.size)
      val locations = queries.par.map { qs =>
        val f = Future {
          (DBPediaClient.executeLocationQuery(qs._1), qs._2)
        }
        try{
          Await.result(f, 15.seconds)
        } catch {
          case e: Exception => println("Exception during waiting for dbpedia query execution: " + e); (List(),0.0)
        }
      }
      Merge.mergeSparqlLocations(locations.seq)
    }

    //Relation KB pipeline
    val rkb = Future {
      val relations = RelationExtraction.extractRelations(annotatedText)
      val locations = RelationLocationFinder.findLocations(relations)
      Merge.mergeRelationLocations(locations)
    }

    val elasticResult = Try(Await.result(elastic, 20.seconds)).getOrElse(Seq())
    val sparqlResult =  Try(Await.result(sparql, 20.seconds)).getOrElse(Seq())
    val rkbResult = Try(Await.result(rkb, 20.seconds)).getOrElse(Seq())

    val combined = Merge.combine(sparqlResult,elasticResult,rkbResult)

    val t1 = System.nanoTime()

    val elapsedTime =  (t1 - t0) / 1000000000.0
    println("Elapsed time: " + elapsedTime + " s")
    //TODO ranking

    combined
  }
}
