package core

import core.Sentiment.Child

import edu.mit.jwi.item.POS
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.trees.{Tree => SentimentTree}

import nlp.wordnet.WordNet
import nlp.{AnnotatedText, OffsetConverter, Relation, TextAnalyzerPipeline}

import scala.annotation.tailrec
import scala.collection.JavaConversions._

/**
 * Provides functionality for relation extraction.
 */
object RelationExtraction {

  type Sentences = Array[Array[(String, String)]]


  /**
   * Extracts all relations and annotates them with candidate lists.
   * @param annText Annotated text.
   * @return List with candidate list for each subject and object in relation.
   */
  def extractRelations(annText: AnnotatedText): List[RawRelation] = {

    //split each word in sentence on "/". This converts words form word/pos into tuple (word,pos)
    lazy val sent: Sentences = TextAnalyzerPipeline.formatPosSentences(annText)

    lazy val offsetConverter = new OffsetConverter(sent)

    //converts stanford sentence and token indices into char offset, counted from beginning of the text
    def calculateOffset(sentenceNr: Int, tokenBegin: Int, tokenEnd: Int, token: String): (Int, Int) =
      offsetConverter.sentenceToCharLevelOffset(sentenceNr, tokenBegin, tokenEnd, token)

    //due to each element in array belong to a sentence
    assert(annText.stanford.sentimentTree.length == annText.relations.length)
    lazy val treesWithRelations = annText.stanford.sentimentTree.zip(annText.relations)
    val relations = for (sent <- treesWithRelations) yield {
      val sentimentExtractor = new Sentiment(sent._1)
      //sentimentExtractor.children.foreach(c => println(c.words))

      val relations = sent._2.flatMap(r => sentimentExtractor.findSentiment(r))
      //relations.foreach(s => println(s))
      relations
    }


    lazy val wordnet = WordNet.getInstance()

    //finds all possible synonyms to the given word sequence
    //returns a list of synonyms conataining original sequence
    def findSynonyms(words: String, sentenceNumber: Int): List[String] = {

      val tokenized = words.split(" ")
      val slidingOverSentence = sent(sentenceNumber).sliding(tokenized.size).toList

      val synonyms = for (subSentence <- slidingOverSentence if subSentence.map(t => t._1).sameElements(tokenized)) yield {
        val synonyms = subSentence.flatMap { w =>
          w._2 match {
            case "JJ" => wordnet.getBestSynonyms(POS.ADJECTIVE, w._1).toList
            case "RB" => wordnet.getBestSynonyms(POS.ADVERB, w._1).toList
            case "NN" | "NNP" => wordnet.getBestSynonyms(POS.NOUN, w._1).toList
            case "VB" => wordnet.getBestSynonyms(POS.VERB, w._1).toList
            case _ => List()
          }
        }
        synonyms
      }
      synonyms.flatten
    }

    val coreference = annText.stanford.coreference.toSeq

    @tailrec
    //annotates relation with sentiment information and creates candidate lists for
    //object and subject within each relation. Adds synonyms, location and co-reference information.
    def annotateRelation(relations: Array[Seq[RawRelation]], annRel: List[RawRelation] = List(),
                         sentenceNumber: Int = 1): List[RawRelation] = {
      if (relations.nonEmpty) {
        val relationsPerSentence = relations.head
        val annotation = for (rel <- relationsPerSentence) yield {

          val objSynonyms = findSynonyms(rel.objectCandidates.head, sentenceNumber - 1)
          val subjSynonyms = findSynonyms(rel.subjectCandidates.head, sentenceNumber - 1)

          //find coref for object
          val corefInObj = coreference.find { c =>
            val mentions = c._2.getMentionsInTextualOrder
            mentions.exists(cm => cm.sentNum == sentenceNumber &&
              TextAnalyzerPipeline.intersect(calculateOffset(cm.sentNum, cm.startIndex, cm.endIndex, cm.mentionSpan),
                rel.objectOffset))
          }

          //find coref for subject
          val corefInSubj = coreference.find { c =>
            val mentions = c._2.getMentionsInTextualOrder
            mentions.exists(cm => cm.sentNum == sentenceNumber &&
              TextAnalyzerPipeline.intersect(calculateOffset(cm.sentNum, cm.startIndex, cm.endIndex, cm.mentionSpan),
                rel.subjectOffset))

          }

          val locationsInObject = annText.clavin.find(x =>
            TextAnalyzerPipeline.intersect((x.offset, x.offset + x.asciiName.length), rel.objectOffset))

          val locationsInSubject = annText.clavin.find(x =>
            TextAnalyzerPipeline.intersect((x.offset, x.offset + x.asciiName.length), rel.subjectOffset))

          //add finded names to candidate list
          val newObjCand = (if (corefInObj.isDefined)
            corefInObj.get._2.getMentionsInTextualOrder.map(x => x.mentionSpan)
          else Set()).toSet

          //annotate subject with co-reference candidates
          val newSubjCand = (if (corefInSubj.isDefined)
            corefInSubj.get._2.getMentionsInTextualOrder.map(x => x.mentionSpan)
          else Set()).toSet

          //annotate subj and obj with ascii location names if available
          val locAnnObj = if (locationsInObject.isDefined) Set(locationsInObject.get.asciiName)
          else Set()
          val locAnnSubj = if (locationsInSubject.isDefined) Set(locationsInSubject.get.asciiName)
          else Set()

          //creates new candidate list with resolved synonyms, co-reference and locations
          val newObj = locAnnObj ++ objSynonyms ++ rel.objectCandidates ++ newObjCand
          val newSubj = locAnnSubj ++ subjSynonyms ++ rel.subjectCandidates ++ newSubjCand

          new RawRelation(newObj.toList, rel.objectOffset, rel.relation, rel.relationOffset, newSubj.toList,
            rel.subjectOffset, rel.sentiment, rel.tfIdf)
        }

        //recursive call with next sentence to be processed
        annotateRelation(relations.tail, annRel ++ annotation, sentenceNumber + 1)
      } else annRel
    }

    assert(relations.length == sent.size)
    val annRelations = annotateRelation(relations)
    //annRelations.foreach(x => println(x))
    annRelations
  }

}

/**
 * This class is responsible for annotation of sentences with sentiment. Each object is created with specific sentiment
 * tree (created during stanford analysis), then each relation inside the sentence can be annotated with sentiment score.
 * @param sentence Sentiment tree of a single sentence. Result of stanford annotation.
 */
class Sentiment(sentence: SentimentTree) {

  //all sentiment information splitted into children
  val children = {
    //because simple iterator doesn't work correctly after conversion
    val iter = sentence.iterator.toList

    val t = for (t <- iter if t.label.asInstanceOf[CoreLabel].containsKey(classOf[RNNCoreAnnotations.PredictedClass])) yield {
      val predictedClass = RNNCoreAnnotations.getPredictedClass(t)
      val words = t.yieldWords().toSeq.map(w => w.word())
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

    for (r <- relSpitted) yield {
      val bowRelation = (r.relation :: r.objectCandidates ::: r.subjectCandidates).flatMap(s => s.split(" "))
      //iterate over all children and find best match to actual relation
      val sentiment = children.foldLeft(None: Option[Int], Integer.MAX_VALUE) { (g, c) =>
        if (c.words.size < g._2 && bowRelation.forall(w => c.words.contains(w))) (Some(c.sentiment), c.words.size)
        else g
      }
      new RawRelation(r.objectCandidates, r.objectOffset, r.relation, r.relationOffset, r.subjectCandidates, r.subjectOffset,
        sentiment._1)
    }
  }

}

object Sentiment {

  case class Child(words: Seq[String], sentiment: Int)

}


case class RawRelation(objectCandidates: List[String], objectOffset: (Int, Int), relation: String, relationOffset: (Int, Int),
                       subjectCandidates: List[String], subjectOffset: (Int, Int), sentiment: Option[Int] = None,
                       tfIdf: Option[Double] = None)
