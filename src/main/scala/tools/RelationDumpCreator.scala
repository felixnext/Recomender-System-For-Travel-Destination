package tools

import java.io.{BufferedWriter, File, FileOutputStream, OutputStreamWriter}
import java.nio.charset.Charset

import com.google.gson.{Gson, JsonElement}
import core.{RawRelation, RelationExtraction}
import nlp.TextAnalyzerPipeline

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source

/**
 * Extracts relations and creates new json dump for elasticsearch loading.
 */
object RelationDumpCreator extends App {

  if (args.length < 1) throw new RuntimeException("Files were not specified!")

  //true if two relations are equal
  def equalRawRelations(r1: RawRelation, r2: RawRelation): Boolean = {
    if (r1.relation.equals(r2.relation)) {
      val shareObject = r1.objectCandidates.intersect(r2.objectCandidates).size > 0
      if (shareObject) {
        val shareSubject = r1.subjectCandidates.intersect(r2.subjectCandidates).size > 0
        if (shareSubject) {
          true
        } else false
      } else false
    } else false
  }

  //counts the relation frequence within the document
  def countRawRelations(r: RawRelation, l: List[RawRelation]) = l.count(rel => equalRawRelations(rel, r))

  lazy val analyzer = new TextAnalyzerPipeline

  def processFile(reader: JsonDumpReader) = {
    val relationExtractor = new RelationExtraction

    val relations = for (locationArticle <- reader) yield {
      val t0 = System.currentTimeMillis()
      val text = locationArticle.text.mkString(" ")

      val result = Future{
        val analyzed = analyzer.analyzeText(text)
        relationExtractor.extractRelations(analyzed)
      }

      val transformedRel = try {
        val rel = Await.result(result, 300.seconds)
        rel.map(r => new Relation(locationArticle.title, locationArticle.id, r.objectCandidates,
          r.relation, r.subjectCandidates, r.sentiment.getOrElse(-1), countRawRelations(r, rel)))
      } catch {
        case e: Exception => println("Error during relation extraction" + e); List()
      }
      val t1 = System.currentTimeMillis()
      println("Relation done! Duration: " + (t1-t0)/1000 + " seconds")
      transformedRel
    }

    relations.flatten.toList
  }

  lazy val relations = (for (path <- args) yield {
      val reader = new JsonDumpReader(path)
      processFile(reader)
  }).flatten

  //tfIdf(relations)


  lazy val path = args.head.split("/")
  lazy val outFile = path.slice(0, path.size - 1).mkString("/") + "/relations.json"
  lazy val writer = new JsonDumpWriter(outFile)

  for(r <- relations) {
    writer.writeRelation(r)
  }

}


/**
 * Json dump reader. Reads a file and returns data within LocationArticle objects.
 * @param filePath Path to dump file.
 */
class JsonDumpReader(filePath: String) extends Iterator[LocationArticle] {

  val decoder = Charset.forName("UTF-8").newDecoder()
  val lines = Source.fromFile(filePath)(decoder).getLines()

  def hasNext = lines.hasNext

  //true if more locations are available
  private def nextLine = lines.next()

  //returns next location
  def next() = {
    val index = if (hasNext) nextLine else "{}"
    val data = if (hasNext) nextLine else "{}"

    val jsonIndex = new Gson().fromJson(index, classOf[JsonElement]).getAsJsonObject.getAsJsonObject("create")
    val id = jsonIndex.get("_id").getAsString
    val indexName = jsonIndex.get("_index").getAsString

    val jsonData = new Gson().fromJson(data, classOf[JsonElement]).getAsJsonObject
    val title = jsonData.get("title").getAsString
    val text = jsonData.getAsJsonArray("paragraph_texts").iterator.map(t => t.getAsString).toList


    new LocationArticle(IndexDisambiguation.disambiguatedIndex(indexName)(id), title, text)
  }

}

object IndexDisambiguation {
  val disambiguatedIndex: String => String => String = indexName => id => indexName match {
    case "travellerspoint" => 1 + id
    case "wikipedia" => 2 + id
    case "wikitravel" => 3 + id
    case _ => id
  }
}

import spray.json.DefaultJsonProtocol._
import spray.json._

/**
 * Json dump writer writes relations into file.
 * @param filePath Path to file.
 */
class JsonDumpWriter(filePath: String) {

  val target = new File(filePath)
  val fos = new FileOutputStream(target, true)
  val os = new OutputStreamWriter(fos, "UTF-8")
  val br = new BufferedWriter(os)


  implicit val RelationFormat = jsonFormat7(Relation)
  var indexId = 0

  //writes relation to file
  def writeRelation(rel: Relation) = {
    val index = "{\"create\": { \"_index\": \"structuredrelations\", \"_type\": \"relations\", \"_id\" : \"" + indexId + "\" }}\n"
    indexId += 1
    br.write(index)
    br.write(rel.toJson + "\n")
    br.flush()
  }

  //threadsafe write implementation
  //writes relation to file
  def writeRelationWithID(rel: Relation, indexId: Int) = {
    val index = "{\"create\": { \"_index\": \"structuredrelations\", \"_type\": \"relations\", \"_id\" : \"" + indexId + "\" }}\n"
    br.synchronized{
      br.write(index)
      br.write(rel.toJson + "\n")
    }
  }

  def flush() = br.flush()

}

case class LocationArticle(id: String, title: String, text: List[String])

case class Relation(locationName: String, id: String, objCand: List[String], rel: String,
                    subjCand: List[String], sent: Int, var tfIdf: Double = 0.0)
