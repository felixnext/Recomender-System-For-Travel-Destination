package tools.script

import scala.math._

/**
 * Script for calculating tf-idf score on existing json dump.
 */
object TfIdfCalculator extends App {

  lazy val filePath = args.head

  lazy val writer = new JsonDumpWriter(filePath.replace(".json", "_idf.json"))
  
  lazy val relations = RelationsDeserializer.deserialize(filePath)
  

  //calculate tf idf score
  //Assumption: tfidf value of each relation object is the term frequency of the relation
  def tfIdf(relations: Seq[Relation]) = {

    val countOccurrences = RelationsUtils.countOccurrences(relations)

    val sizeOfCorpora = relations.size.toDouble
    var counter = 0
    for (relation <- relations) {
      //term frequency
      val tf = relation.tfIdf

      //approximation for speed up
      val occurrenceInCorpus = if(tf == 1) 1 else countOccurrences(relation).toDouble

      //calculate Tf-Idf
      val tfIdf = tf * log10(sizeOfCorpora / occurrenceInCorpus)
      relation.tfIdf = tfIdf
      writer.writeRelation(relation)
      counter += 1
      println(counter +"/" + relations.size)
    }
  }

  tfIdf(relations)

}

object RelationsUtils {

  //compares two relation
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

  //counts the corpus frequency of a given relation
  val countOccurrences: Seq[Relation] => Relation  => Int =
    relations => r => relations.count(rel => equalRelations(rel, r))

}

import java.nio.charset.Charset

import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.io.Source

//Provide a function for deserialization of relations
object RelationsDeserializer {

  val deserialize: String => Seq[Relation] = filePath => {
    val decoder = Charset.forName("UTF-8").newDecoder()
    val lines = Source.fromFile(filePath)(decoder).getLines()

    def hasNext = lines.hasNext

    def nextLine() = lines.next()

    val relations = new scala.collection.mutable.ListBuffer[Relation]()

    //read a file with relation data
    while(hasNext) {
      val index = if (hasNext) nextLine() else "{}"
      val data = if (hasNext) nextLine() else "{}"

      implicit val RelationFormat = jsonFormat7(Relation)

      val jsonAst = data.parseJson
      val r = jsonAst.convertTo[Relation]
      relations += r
    }
    relations
  }
}
