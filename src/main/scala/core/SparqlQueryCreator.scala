package core

import dbpedia.YagoGeoTypes
import elasticsearch.ElasticsearchClient
import nlp._

import scala.annotation.tailrec
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
    val predicateAnnotation = entityCandidatesAnnotation.map {c =>
      val rel = c.groupsMap.map(k => (k._1,
        k._2.map(r => new AnnontatedRelation(r.arg1,r.rel,r.relOffset,r.arg2,
        Some(elastic.findPattyRelation(r.rel._2)), Some(elastic.findDBPediaProperties(r.rel._1))))))
      val smallerSets = rel.filter(x => x._2.size < 2).flatMap(x => x._2)
      val biggerSets = rel.filterNot(x => x._2.size < 2)
      val newUnkownGroup = biggerSets.getOrElse(-1, Seq()) ++ smallerSets
      biggerSets + (-1 -> newUnkownGroup)
    }


    val trees = predicateAnnotation.map { relations =>
      //annotate all entities with dbpedia classes and ygo geo types
      def annRelation(r: AnnontatedRelation) = {
        val arg1Lemon = elastic.findDBPediaClasses(r.arg1.arg)
        val arg1Yago = YagoGeoTypes.getYagoEntities(r.arg1.arg)
        val arg1 = new AnnotatedArgument(r.arg1.spotlight,r.arg1.clavin, r.arg1.dbpediaLookup, r.arg1.arg, r.arg1.argType,
          r.arg1.argOffset, Some(arg1Yago), Some(arg1Lemon))
        val args2 = for(argS <- r.arg2) yield {
          val arg2Lemon = elastic.findDBPediaClasses(argS.arg)
          val arg2Yago = YagoGeoTypes.getYagoEntities(argS.arg)
          new AnnotatedArgument(argS.spotlight,argS.clavin, argS.dbpediaLookup, argS.arg, argS.argType,
            argS.argOffset, Some(arg2Yago), Some(arg2Lemon))
        }
        new AnnontatedRelation(arg1, r.rel, r.relOffset, args2, r.pattyResult, r.dbpediaProps)
      }

      //annotates all entities with dbpedia classes and  yago geo types
      //if annotation doesn't exists then the list attribute remain empty
      val annRelations = relations.map(x => (x._1, x._2.map(y => annRelation(y))))

      //TODO try to create new groups in unknown group
      val unknownGroup = annRelations.getOrElse(-1, Seq())

      val focusWords = Seq("this", "these", "it", "there", "where", "here", "one", "its")

      val subGroups = scala.collection.mutable.Map[String, Seq[AnnontatedRelation]]()

      @tailrec
      def joinEqualNodes(relations: Seq[AnnontatedRelation], trees: Seq[Tree] = Seq()):
      Seq[Seq[AnnontatedRelation]] = {
        val r = relations.head

        val newTree = for(tree <- trees) yield {
          //compare rel and r

          //use score !!!
          new Tree(null)
        }

        //newTree + new Tree(Map(r,))
        //TODO claculate relation score
        joinEqualNodes(relations.tail, trees)
      }

      def chainRelations() = ???


    }

    val scorer = new NaiveScorer
    Await.result(predicateAnnotation, 1000 seconds).foreach{
      x => println("\n\n" + x._1)
        //x._2.foreach(x => println(x.arg1.arg + " " + x.rel._1 + " " + x.arg2.map(y => y.arg).toString))
        x._2.foreach(x => println(scorer.getRelationScore(x)))
    }



    def constructTree() = ???

    //TODO finds focus with coreference and extend entitiy candidates

    //TODO create query

    //TODO Yago rdf: type

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

abstract class RelationScorer {
  def getRelationScore(r: AnnontatedRelation): Double
}

class NaiveScorer extends RelationScorer {
  override def getRelationScore(r: AnnontatedRelation): Double = {
    def calculateArgScore(r: AnnotatedArgument) = {
      val spotlight = r.spotlight match {
        case Some(x) => x.score
        case _ => 0.0
      }
      val clavin = if (r.clavin.isDefined) 1.0 else 0.0
      val dbpediaYago = if (r.yago.getOrElse(List()).size > 0 || r.lemon.getOrElse(List()).size > 0) 1.0 else 0.0
      val lookup = r.dbpediaLookup match {
        case Some(x) => x.foldLeft(0.0)((score, res) => if (res.score > score) res.score else score)
        case _ => 0.0
      }
      (spotlight + clavin + dbpediaYago + lookup) / 4.0
    }

    val relationScore = ((if(r.dbpediaProps.isDefined) 1.0 else 0.0) + (if(r.pattyResult.isDefined) 1.0 else 0.0)) / 2.0

    val arg1Score = calculateArgScore(r.arg1)

    val maxArg2Score = r.arg2.foldLeft(0.0){(score, arg) =>
      val actualScore = calculateArgScore(arg)
      if(actualScore > score) actualScore else score
    }

    (relationScore + arg1Score + maxArg2Score) / 3.0
  }
}

//the double value of the map represents the membership score of relation
case class Tree(edges: Map[AnnontatedRelation, Double], children: Option[Seq[Tree]] = None)