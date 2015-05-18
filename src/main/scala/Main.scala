/**
 * Created by yevgen on 11.04.15.
 */

import java.io.File

import clavin.ClavinClient
import com.google.gson.{JsonElement, Gson}
import dbpedia.{DBPediaClient, YagoGeoTypes, SpotlightClient, DBPediaLookup}
import edu.knowitall.openie.{TemporalArgument, SpatialArgument, SimpleArgument, OpenIE}
import edu.mit.jwi.item.POS

import elasticsearch.ElasticsearchClient
import nlp.wordnet.WordNet
import nlp.{RelationExtractor => RE, TextAnalyzerPipeline, StanfordAnnotator}
import core.{RelationExtraction => RWS, RelationLocationFinder, RawRelation, Sentiment, SparqlQueryCreator}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaj.http.{HttpException, Http, HttpResponse}
import scala.collection.JavaConversions._



object Main  extends App{

  val s =
    s"""
       |Situated on one of the world's largest natural harbors,[20] New York City consists of five boroughs, each of which is a county of New York State.[21] The five boroughs – Brooklyn, Queens, Manhattan, the Bronx, and Staten Island – were consolidated into a single city in 1898.[22] With a census-estimated 2014 population of 8,491,079[1] distributed over a land area of just 305 square miles (790 km2),[23] New York is the most densely populated major city in the United States.[24] As many as 800 languages are spoken in New York,[25][26] making it the most linguistically diverse city in the world.[27] By 2014 census estimates, the New York City metropolitan region remains by a significant margin the most populous in the United States, as defined by both the Metropolitan Statistical Area (20.1 million residents)[5] and the Combined Statistical Area (23.6 million residents).[6] In 2013, the MSA produced a gross metropolitan product (GMP) of nearly US 1.39 trillion,[28] while in 2012, the CSA[29] generated a GMP of over US 1.55 trillion, both ranking first nationally by a wide margin and behind the GDP of only twelve nations and eleven nations, respectively.[30]
     """.stripMargin

/*
  val stanford = new StanfordAnnotator
  val a = stanford.annotateText(s)
  val e = new Sentiment(a.sentimentTree.head)
  //stanford.extractSentiment(a.sentimentTree.head, null,null,null,null)
  //stanford.annotateText(s + "bla")
*/
/*

  val analyzingPipe = new TextAnalyzerPipeline
  val queryCreator = new SparqlQueryCreator(analyzingPipe)
  val annotatedText = analyzingPipe.analyzeText(s)
  val queries = queryCreator.createSparqlQuery(annotatedText)
  val dbpediaCleint = new DBPediaClient
  val result = queries.map(qs =>
    qs.map(q => Future{(dbpediaCleint.executeLocationQuery(q._1), q._2)})
  )

  Await.result(result, 1000.seconds).foreach{y =>
    println("HERE")
    Await.result(y, 1000.seconds)._1.foreach(l => println(l))
  }
*/


  val l = "Paris is a nice cite."
  println(ClavinClient.extractLocations("Berlin"))
/*
  val openie = new RelationExtractor
  println(openie.extractRelations(s))


  val spotlightClient = new SpotlightClient
  println(spotlightClient.discoverEntities(s))

  */

/*
  val openie = new RelationExtractor
  openie.extractRelations(s).foreach{
    c => println(c)
  }
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
/*
  val e = new ElasticsearchClient
  println(e.matchQuery("I want to visit an island in turkey.").flatten.mkString(" \n "))

  */

  import tools.Math._

  val v = Seq(0.99,0.99,0.99)
  println(decaySum(v))

}

