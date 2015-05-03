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
import scala.math._
import scala.util.Success

/**
 * Created by yevgen on 01.05.15.
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


  def processFile(reader: JsonDumpReader) = {
    val analyzerPipe = new TextAnalyzerPipeline
    val relationExtractor = new RelationExtraction(analyzerPipe)

    val relations = for (locationArticle <- reader) yield {
      val text = locationArticle.text.mkString(" ")
      val analyzed = analyzerPipe.analyzeText(text)
      val rel = relationExtractor.extractRelations(analyzed)
      val extracteRawdRel = Await.result(rel, 1000.second)
      val transformedRel = extracteRawdRel.map(r => new Relation(locationArticle.title, locationArticle.id, r.objectCandidates,
        r.relation, r.subjectCandidates, r.sentiment.getOrElse(-1), countRawRelations(r, extracteRawdRel)))
      transformedRel
    }
    /*
    val relations = reader.toArray.par.map{ locationArticle =>
      val text = locationArticle.text.mkString(" ")
      val analyzed = analyzerPipe.analyzeText(text)
      val rel = relationExtractor.extractRelations(analyzed)
      val extracteRawdRel = Await.result(rel, 300.second)
      val transformedRel = extracteRawdRel.map(r => new Relation(locationArticle.title, locationArticle.id, r.objectCandidates,
        r.relation, r.subjectCandidates, r.sentiment.getOrElse(-1), countRawRelations(r, extracteRawdRel)))
      transformedRel
    }
    */
    relations.flatten.toList
  }

  val relations = (for (path <- args) yield {
      val reader = new JsonDumpReader(path)
      processFile(reader)
  }).flatten

  tfIdf(relations)


  /*
  result.onSuccess {
    case x =>
      val s = Future.sequence(x)
      s.onSuccess {
        case r =>
          val relations = r.flatten
          tfIdf(relations)
      }
  }

  Await.result(result, 1000.seconds)
  */

  def tfIdf(relations: Array[Relation]) = {

    def equalRelations(r1: Relation, r2: Relation) = {
      if (r1.rel.equals(r2.rel)) {
        val shareObject = r1.objCand.intersect(r2.objCand).size > 0
        if (shareObject) {
          val shareSubject = r1.subjCand.intersect(r2.subjCand).size > 0
          if (shareSubject) {
            true
          } else false
        } else false
      } else false
    }

    def countRelations(r: Relation) = relations.count(rel => equalRelations(rel, r))

    val path = args.head.split("/")
    val outFile = path.slice(0, path.size - 1).mkString("/") + "/relations.json"
    val writer = new JsonDumpWriter(outFile)

    val sizeOfCorpora = relations.size.toDouble
    for (relation <- relations) yield {
      val occurrenceInCorpus = countRelations(relation).toDouble
      val tf = relation.tfIdf
      //calculate Tf-Idf
      val tfIdf = tf * log10(sizeOfCorpora / occurrenceInCorpus)
      relation.tfIdf = tfIdf
      writer.writeRelation(relation)
    }
  }
}


/**
 * Json dump reader. Reads a file and returns data within LocationArticle objects.
 * @param filePath Path to dump file.
 */
class JsonDumpReader(filePath: String) extends Iterator[LocationArticle] {

  val decoder = Charset.forName("UTF-8").newDecoder()
  val lines = Source.fromFile(filePath)(decoder).getLines()

  def hasNext: Boolean = lines.hasNext

  //true if more locations are available
  private def nextLine: String = lines.next()

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

    def disambiguatedIndex = indexName match {
      case "travellerspoint" => 1 + id
      case "wikipedia" => 2 + id
      case "wikitravel" => 3 + id
      case _ => id
    }
    new LocationArticle(disambiguatedIndex, title, text)
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

  //writes relation to file
  def writeRelation(rel: Relation) = {
    val index = "{\"create\": { \"_index\": \"structuredrelations\", \"_type\": \"relations\", \"_id\" : \"" + rel.id + "\" }}\n"
    br.write(index)
    br.write(rel.toJson + "\n")
    Future {
      br.flush()
    }
  }

}

case class LocationArticle(id: String, title: String, text: List[String])

case class Relation(locationName: String, id: String, objCand: List[String], rel: String,
                    subjCand: List[String], sent: Int, var tfIdf: Double = 0.0)
