package core

import dbpedia.DBPediaClient
import elasticsearch.ElasticsearchClient
import nlp.TextAnalyzerPipeline

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Created by yevgen on 20.05.15.
 */
class QueryHandler {

  val analyzingPipe = new TextAnalyzerPipeline

  def handleQuery(query: String) = {

    //Elasticsearch location request
    val elastic = Future {
      val locations = ElasticsearchClient.matchQuery(query).flatten
      Merge.mergeElasticLocations(locations)
    }

    val annotatedText = analyzingPipe.analyzeText(query)

    //sparql pieline
    val sparql = Future{
      val queries = SparqlQueryCreator.createSparqlQuery(annotatedText)
      println("Number of queries: " + queries.size)
      val locations = queries.par.map { qs =>
        val f = Future {
          (DBPediaClient.executeLocationQuery(qs._1), qs._2)
        }
        try{
          Await.result(f, 20.seconds)
        } catch {
          case e: Exception => println("Exception during waiting for dbpedia query execution: " + e); (List(),0.0)
        }
      }
      Merge.mergeSparqlLocations(locations.seq)
    }

    //Relation KB pipeline
    val rkb = Future{
      val relations = RelationExtraction.extractRelations(annotatedText)
      val locations = RelationLocationFinder.findLocations(relations)
      Merge.mergeRelationLocations(locations)
    }

    val elasticResult = Await.result(elastic, 30.seconds)
    val sparqlResult =  Await.result(sparql, 30.seconds)
    val rkbResult = Await.result(rkb, 30.seconds)

    Merge.combine(sparqlResult,elasticResult,rkbResult)

    //TODO rank

  }
}
