package core

import elasticsearch.ElasticsearchClient
import nlp.{OffsetConverter, AnnontatedRelation, TextAnalyzerPipeline, Relation}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Takes a text and creates corresponding sparql query w.r.t focus of text.
 * Assumption the focus is a certain location.
 */
class SparqlQueryCreator extends TextAnalyzerPipeline {

  val elastic = new ElasticsearchClient

  def createSprqlQuery(text: String): Unit = {


    //clavin, stanford and opnie
    val annotatedText = analyzeText(text)
    for (e <- annotatedText.failed) println("Text annotation failed. Cannot create sparql query" + e)

    //split each word in sentece on "/". This converts words form word/pos into tuple (word,pos)
    //TODO make method
    val tokenizedSentensesPos: Future[Array[Array[(String, String)]]] = annotatedText.map{ x =>
      x.stanford.sentencesPos.map { x =>
        x.split(" ").map { x =>
          val split = x.split("/")
          try {
            (split(0), split(1))
          } catch {
            case e: Exception => (split(0), "")
          }
        }
      }
    }


    //replaces stanford pos tags with patty tags
    val posRelations = for {
      ann <- annotatedText
      s <- tokenizedSentensesPos
    } yield {
        posRelAnnotation(s, ann.relations)
      }

    for (e <- posRelations.failed) println("POS annotation failed. Cannot create sparql query" + e)

    //maps raw relations into patty dbpedia predicates
    //val pattyRelations = posRelations.map { posRel =>
     // posRel.map(sentence => sentence.map(relation => elastic.findPattyRelation(relation.rel)))
    //}
    ///for (e <- pattyRelations.failed) println("Patty relation retrival failed. Cannot create sparql query" + e)

    //converts stanfrod sentence offset into char offset
    val offsetConverter = tokenizedSentensesPos.map(s => new OffsetConverter(s))


    val entityCandidatesAnnotation = for {
      p <- posRelations
      s <- tokenizedSentensesPos
      ann <- annotatedText
      offS <- offsetConverter
    } yield {
        createEntityCandidates(p, ann.spotlight, ann.clavin, ann.stanford.coreference, s, offS)
      }
    for (e <- entityCandidatesAnnotation.failed) println("Candidate set creation failed. Cannot create sparql query" + e)

    //maps raw relations into patty dbpedia predicates
    val pattyAnnotation = entityCandidatesAnnotation.map{c =>
      c.groupsMap.map(k => (k._1, k._2.map(r => new AnnontatedRelation(r.arg1,r.rel,r.relOffset,r.arg2,
        Some(elastic.findPattyRelation(r.rel._2)), Some(elastic.findDBPediaProperties(r.rel._1))))))
    }

    Await.result(pattyAnnotation, 1000 seconds).foreach{
      x => println("\n\n" + x._1)
        x._2.foreach(x => println(x))
    }




    //TODO finds focus with coreference and extend entitiy candidates

    //TODO create query

    //TODO Yago

    //TODO test lookup similarity with levenstein

  }


  //Takes relations and replace predicates with patty pos tags
  def posRelAnnotation(sentences: Array[Array[(String, String)]], relations: Array[Seq[Relation]]): Array[Seq[Relation]] = {

    //Annotate with patty tag names
    //sentence: Seq[(Word,Tag)]
    def annotatePos(relation: Relation, sentence: Array[(String, String)]) = {
      val mapToPattyTags = Map("CD" -> "[[num]]", "DT" -> "[[det]]", "PRP" -> "[[prp]]",
        "JJ" -> "[[adj]]", "MD" -> "[[mod]]", "IN" -> "[[con]]", "CC" -> "[[con]]")

      val relationWords = relation.rel._1.split(" ")
      val slidingOverSentence = sentence.sliding(relationWords.size)

      //if contains patty tag, than replace word with pos tag else take a word
      val posTaggedRelationList: Iterator[Array[String]] = for (subSentence <- slidingOverSentence
                                                                if subSentence.map(t => t._1).sameElements(relationWords))
        yield {
          subSentence.map(t =>  mapToPattyTags.getOrElse(t._2, t._1))
        }

      val relTagged: String = if (posTaggedRelationList.isEmpty) relation.rel._1
      else posTaggedRelationList.next().reduce((a,b) => a + " " + b)

      new Relation(relation.arg1, (relation.rel._1, relTagged), relation.relOffset, relation.arg2)
    }

    //iterate over all sentence and relations
    val posRelations: Array[Seq[Relation]] = for (sr <- sentences.zip(relations)) yield {
      val sentence = sr._1
      val relation = sr._2
      relation.map { r => annotatePos(r, sentence) }
    }

    posRelations
  }

}
