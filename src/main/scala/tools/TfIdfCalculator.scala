package tools

import java.nio.charset.Charset

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.io.Source
import scala.math._

/**
 * Script for calculating tf-idf score on existing json dump.
 */
object TfIdfCalculator extends App {

  val filePath = args.head

  val decoder = Charset.forName("UTF-8").newDecoder()
  val lines = Source.fromFile(filePath)(decoder).getLines()

  val writer = new JsonDumpWriter(filePath.replace(".json", "_idf.json"))

  def hasNext = lines.hasNext

  private def nextLine = lines.next()

  val relations = new scala.collection.mutable.ListBuffer[Relation]()

  //read a file with relation data
  while(hasNext) {
    val index = if (hasNext) nextLine else "{}"
    val data = if (hasNext) nextLine else "{}"

    implicit val RelationFormat = jsonFormat7(Relation)

    val jsonAst = data.parseJson
    val r = jsonAst.convertTo[Relation]
    relations += r
  }

  //calculate tf idf score
  //Assumption: tfidf value of each relation object is the term frequency of the relation
  def tfIdf(relations: Seq[Relation]) = {

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

    val sizeOfCorpora = relations.size.toDouble
    for (relation <- relations) yield {
      val occurrenceInCorpus = countRelations(relation).toDouble

      //term frequency
      val tf = relation.tfIdf

      //calculate Tf-Idf
      val tfIdf = tf * log10(sizeOfCorpora / occurrenceInCorpus)
      relation.tfIdf = tfIdf
      writer.writeRelation(relation)
    }
  }

  tfIdf(relations)

}
