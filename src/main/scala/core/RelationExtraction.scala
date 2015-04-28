package core

import java.util

import core.Sentiment.Child
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.trees.{Tree => SentimentTree}
import nlp.{Relation, AnnotatedText, TextAnalyzerPipeline}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.collection.JavaConversions._
/**
 * Provides functionality for relation extraction.
 */
class RelationExtraction(analyzingPipe: TextAnalyzerPipeline) {

  type Sentences =  Array[Array[(String, String)]]


  /**
   * Extracts all relations and annotates them with candidate lists.
   * @param annotatedText Annotated text.
   * @return List with candidate list for each subject and object in relation.
   */
  def extractRelations(annotatedText: Future[AnnotatedText]) = {

    //split each word in sentence on "/". This converts words form word/pos into tuple (word,pos)
    val tokenizedSentencesPos: Future[Sentences] = annotatedText.map { x => analyzingPipe.formatPosSentences(x)}


    for {
      sent <- tokenizedSentencesPos
      annText <- annotatedText
    } yield {

      //due to each element in array belong to a sentence
      assert(annText.stanford.sentimentTree.length == annText.relations.length)
      val treesWithRelations = annText.stanford.sentimentTree.zip(annText.relations)
      for(sent <- treesWithRelations) {
        val sentimentExtractor = new Sentiment(sent._1)
        //sentimentExtractor.children.foreach(c => println(c.words))

        val relations = sent._2.map(r => sentimentExtractor.findSentiment(r))
        relations.foreach(s => println(s))
      }


    }





    //TODO clavin annoation
    //TODO sentiment annoation of relations
    //TODO Wordnet annontation
    //TODO coreference groups

  }

}

class Sentiment(sentence: SentimentTree) {

  //all sentiment information splitted into children
  val children = {
    //because simple iterator doesn't work correctly after conversion
    val iter = sentence.iterator.toList

    val t = for(t <- iter if t.label.asInstanceOf[CoreLabel].containsKey(classOf[RNNCoreAnnotations.PredictedClass])) yield {
        val predictedClass = RNNCoreAnnotations.getPredictedClass(t)
        val words = t.yieldWords().toList.toSeq.map(w => w.word())
        new Child(words, predictedClass)
    }
    t
  }

  /**
   * Finds sentiment to given relation and converts it into RawRelation object.
   * @param relations Relation.
   * @return Seq of new relations which were splitted at subject lists.
   */
  def findSentiment(relations: Relation): Seq[RawRelation] = {
    val relSpitted = relations.arg2.map(r => new RawRelation(List(relations.arg1.arg), relations.arg1.argOffset,
      relations.rel._1, relations.relOffset, List(r.arg), r.argOffset))

    for(r <- relSpitted) yield {
      val bowRelation = (r.relation :: r.objectCandidates ::: r.subjectCandidates).flatMap(s => s.split(" "))
      //iterate over all children and find best match to actual relation
      val sentiment = children.foldLeft(None: Option[Int], Integer.MAX_VALUE){(g,c) =>
        if(c.words.size < g._2 && bowRelation.forall(w => c.words.contains(w))) (Some(c.sentiment), c.words.size)
        else g
      }
      new RawRelation(r.objectCandidates,r.objectOffset,r.relation,r.relationOffset,r.subjectCandidates, r.subjectOffset,
      sentiment._1)
    }
  }

}

object Sentiment {
  case class Child(words: Seq[String], sentiment: Int)
}


case class RawRelation(objectCandidates: List[String], objectOffset: (Int,Int), relation: String, relationOffset: (Int,Int),
                       subjectCandidates: List[String], subjectOffset: (Int,Int), sentiment: Option[Int] = None,
                       tfIdf: Option[Double] = None)
