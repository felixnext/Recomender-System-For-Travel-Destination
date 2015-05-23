/**
 * Created by yevgen on 11.04.15.
 */

import java.io.File

import clavin.ClavinClient
import com.google.gson.{JsonElement, Gson}
import dbpedia.{DBPediaClient, YagoGeoTypes, SpotlightClient, DBPediaLookup}
import edu.knowitall.openie.{TemporalArgument, SpatialArgument, SimpleArgument, OpenIE}
import edu.mit.jwi.item.POS

import elasticsearch.{DeepParsing, ElasticsearchClient}
import nlp.wordnet.WordNet
import nlp.{RelationExtractor => RE, TextAnalyzerPipeline, StanfordAnnotator}
import core.{RelationExtraction => RWS, _}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaj.http.{HttpException, Http, HttpResponse}
import scala.collection.JavaConversions._



object Main  extends App{

  val s =
    s"""
       |The Caribbean is commonly known as one of the best places in the world to go diving, snorkelling and relaxing at one of its many beaches. It's also a popular stop-off for cruises. Of course, there's more to the Caribbean than just beaches, beaches and more beaches - head a little further inland and you'll discover interesting historical cities and mountainous areas with great trekking opportunities. Lots of people come here on package trips but for the independent traveller who likes to explore more, countries like Cuba and the Dominican Republic are big enough to spend weeks travelling around.
     """.stripMargin

/*
  val stanford = new StanfordAnnotator
  val a = stanford.annotateText(s)
  val e = new Sentiment(a.sentimentTree.head)
  //stanford.extractSentiment(a.sentimentTree.head, null,null,null,null)
  //stanford.annotateText(s + "bla")
*/



  val analyzingPipe = new TextAnalyzerPipeline
  val annotatedText = analyzingPipe.analyzeText(s)
  val queries = SparqlQueryCreator.createSparqlQuery(annotatedText)
  println("Number of queries: " + queries.size)
  val result = queries.par.map { qs =>
    val f = Future {
      (DBPediaClient.executeLocationQuery(qs._1), qs._2)
    }
    try{

      Await.result(f, 20.seconds)
    } catch {
      case e: Exception => println("Exception during waiting for dbpedia response: " + e); List()
    }
  }

  result.foreach(l => println(l))




  //println(ClavinClient.extractLocations("Berlin"))
  val spotlightClient = new SpotlightClient
  println(spotlightClient.discoverEntities(s))

/*
  val openie = new RelationExtractor
  println(openie.extractRelations(s))


  val spotlightClient = new SpotlightClient
  println(spotlightClient.discoverEntities(s))

  */


/*
  val r = new RawRelation(List("town"),(1,1),"has",(1,1),List("place"),(1,1), None,None)
  val elastic = new ElasticsearchClient
  elastic.findSimilarRelations(r).foreach{
    x => println(x)
  }
*/

/*
  val openie = new RelationExtractor
  openie.extractRelations(s).foreach{
    x => println(x)
  }
*/

  /*
  val adasda = "{\"query\": \"Washington, D.C., formally the District of Columbia and commonly referred to as Washington, the District, or simply D.C., is the capital of the United States. The signing of the Residence Act on July 16, 1790, approved the creation of a capital district located along the Potomac River on the countrys East Coast. The U.S. Constitution provided for a federal district under the exclusive jurisdiction of the Congress and the District is therefore not a part of any U.S. state.rnrnThe states of Maryland and Virginia each donated land to form the federal district, which included the pre-existing settlements of Georgetown and Alexandria. Named in honor of George Washington, the City of Washington was founded in 1791 to serve as the new national capital. In 1846, Congress returned the land originally ceded by Virginia; in 1871, it created a single municipal government for the remaining portion of the District.rnrnWashington, D.C., had an estimated population of 658,893 in 2014, the 23rd-most populous city in the United States. Commuters from the surrounding Maryland and Virginia suburbs raise the citys population to more than one million during the workweek. The Washington metropolitan area, of which the District is a part, has a population of 5.8 million, the seventh-largest metropolitan statistical area in the country.rnrnThe centers of all three branches of the federal government of the United States are in the District, including the Congress, President, and Supreme Court. Washington is home to many national monuments and museums, which are primarily situated on or around the National Mall. The city hosts 176 foreign embassies as well as the headquarters of many international organizations, trade unions, non-profit organizations, lobbying groups, and professional associations.rnrnA locally elected mayor and a 13u2011member council have governed the District since 1973. However, the Congress maintains supreme authority over the city and may overturn local laws. D.C. residents elect a non-voting, at-large congressional delegate to the U.S. House of Representatives, but the District has no representation in the U.S. Senate. The District receives three electoral votes in presidential elections as permitted by the Twenty-third Amendment to the United States Constitution, ratified in 1961.\"}"
  val response: HttpResponse[String] = Http("http://localhost:8080/search")
   .header("content-type", "application/json").timeout(connTimeoutMs = Integer.MAX_VALUE, readTimeoutMs = Integer.MAX_VALUE).postData(adasda).asString
  println(response.body)

  */

  /*
  val wordNet = new WordNet
  val syn = wordNet.getBestSynonyms(POS.NOUN, "children")
  syn.foreach(s => println(s))
*/
  /*

  val analyzingPipe = new TextAnalyzerPipeline
  val relationExtractor = new RWS
  val annotatedText = analyzingPipe.analyzeText(s)
  val relations = relationExtractor.extractRelations(annotatedText)

  println(relations)
*/

  /*
  val reader = new JsonDumpReader("/Users/yevgen/Documents/data/master/dumps/elastic/travellerspoint0.json")
  println(reader.next)
  val writer = new JsonDumpWriter("/Users/yevgen/Documents/data/master/dumps/elastic/test.json")
  val r = new Relation("Mallorca", "123",List("Sun"),"is", List("bright"), 1, 2.0)
  writer.writeRelation(r)
  */
  //println(System.getProperty("user.dir"))

  /*
  val finder = RelationLocationFinder
  val r = finder.findLocations(relations)
  println(r.mkString("\n"))
*/

  //println(ElasticsearchClient.matchTitle("Berlin").flatten.map(x => x.title).mkString("\n"))

/*
  val handler = new QueryHandler
  val r = handler.handleQuery(s)
  println(r.mkString("\n"))
*/

  //DeepParsing.parseQuery("Austria and UK are a nice countries. By the way Berlin is a capital of Germany. Spanish is spoken in Spain. In Brazil the most people speak German.")
}

