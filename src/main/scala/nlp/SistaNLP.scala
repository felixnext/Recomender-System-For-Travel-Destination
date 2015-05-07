package nlp

import edu.arizona.sista.processors.Processor
import edu.arizona.sista.processors.corenlp.CoreNLPProcessor
import edu.arizona.sista.processors.CorefChains
import utils.SentimentUnpacker

import scala.io.Source
import scala.math._

/**
 * Lightweight implementation of nlp annotation.
 */
class SistaNLP {

  val proc:Processor = new CoreNLPProcessor(withDiscourse = true)

  /**
   * Annotates tokenized text with co-reference and part of speech tags.
   * @param text Text which should be annotated
   * @return Annotation of the text.
   */
  def annotateText(text: String) = {
    val doc = proc.annotate(text)

    val sentences = for(sentence <- doc.sentences) yield {
      val words = sentence.lemmas
      val posTags = sentence.tags
      val start = sentence.startOffsets
      val end = sentence.endOffsets


      val tokens = if(words.isDefined && posTags.isDefined) {
        assert(words.get.length == posTags.get.length && posTags.get.length == start.length)
        for(token <- words.get; pos <- posTags.get; s <- start; e <- end ) yield {
          new Token(token,pos,s,e)
        }
      } else Array[Token]()
      tokens
    }


    new Annotation(doc.coreferenceChains, sentences)

  }

}

/**
 * Sentiment analyzer.
 */
object Sentiment {

  //load english opinion words
  private def loadWords(path: String) = Source.fromFile(SentimentUnpacker.getUnpackedWordNetDir(path)).getLines().toArray

  lazy val positiveWords = loadWords("models/opinion/positive-words.txt")
  lazy val negativeWords = loadWords("models/opinion//negative-words.txt")

  //returns sentiment of a snippet
  //1 => negative, 2 => neutral and 3 => positive
  def sentimentAnnotation(snippet: String): Int = {
    val lowerText = snippet.toLowerCase()
    val neg = if(negativeWords.exists(lowerText contains)) 1 else 0
    val pos = if(positiveWords.exists(lowerText contains)) 3 else 0
    val v  = abs(pos - neg)
    if(v == 0) 2 else v
  }
}

case class Annotation(coref: Option[CorefChains], tokens: Array[Array[Token]])

case class Token(token: String, pos: String, startOffset: Int, endOffset: Int)
