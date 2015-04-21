package core

import dbpedia.YagoGeoTypes
import elasticsearch.ElasticsearchClient
import nlp._

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.math._


/**
 * Takes a text and creates corresponding sparql query w.r.t focus of text.
 * Assumption the focus is a certain location.
 */
class SparqlQueryCreator extends TextAnalyzerPipeline {

  val elastic = new ElasticsearchClient

  def createSparqlQuery(text: String): Unit = {

    //clavin, stanford and openie
    val annotatedText = analyzeText(text)
    for (e <- annotatedText.failed) println("Text annotation failed. Cannot create sparql query" + e)

    //split each word in sentence on "/". This converts words form word/pos into tuple (word,pos)
    val tokenizedSentencesPos: Future[Sentences] = annotatedText.map { x => formatPosSentences(x)}

    //replaces stanford pos tags with patty tags
    val posRelations = for {
      ann <- annotatedText
      s <- tokenizedSentencesPos
    } yield {
        posRelAnnotation(s, ann.relations)
      }

    for (e <- posRelations.failed) println("POS annotation failed. Cannot create sparql query" + e)

    //converts stanfrod sentence offset into char offset
    val offsetConverter = tokenizedSentencesPos.map(s => new OffsetConverter(s))


    val entityCandidatesAnnotation = for {
      p <- posRelations
      s <- tokenizedSentencesPos
      ann <- annotatedText
      offS <- offsetConverter
    } yield {
        createEntityCandidates(p, ann.spotlight, ann.clavin, ann.stanford.coreference, s, offS)
      }
    for (e <- entityCandidatesAnnotation.failed) println("Candidate set creation failed. Cannot create sparql query" + e)

    //maps raw relations into patty dbpedia predicates
    val predicateAnnotation = entityCandidatesAnnotation.map { c =>
      val rel = c.groupsMap.map(k => (k._1,
        k._2.map(r => new AnnontatedRelation(r.arg1, r.rel, r.relOffset, r.arg2,
          Some(elastic.findPattyRelation(r.rel._2)), Some(elastic.findDBPediaProperties(r.rel._1))))))
      val smallerSets = rel.filter(x => x._2.size < 2).flatMap(x => x._2)
      val biggerSets = rel.filterNot(x => x._2.size < 2)
      val newUnkownGroup = biggerSets.getOrElse(-1, Seq()) ++ smallerSets
      biggerSets + (-1 -> newUnkownGroup)
    }


    //chains relations thus creates trees
    val trees: Future[Seq[Tree]] = predicateAnnotation.map { relations =>
      //annotate all entities with dbpedia classes and ygo geo types
      def annRelation(r: AnnontatedRelation) = {
        val arg1Lemon = elastic.findDBPediaClasses(r.arg1.arg)
        val arg1Yago = YagoGeoTypes.getYagoEntities(r.arg1.arg)
        val arg1 = new AnnotatedArgument(r.arg1.spotlight, r.arg1.clavin, r.arg1.dbpediaLookup, r.arg1.arg, r.arg1.argType,
          r.arg1.argOffset, Some(arg1Yago), Some(arg1Lemon))
        val args2 = for (argS <- r.arg2) yield {
          val arg2Lemon = elastic.findDBPediaClasses(argS.arg)
          val arg2Yago = YagoGeoTypes.getYagoEntities(argS.arg)
          new AnnotatedArgument(argS.spotlight, argS.clavin, argS.dbpediaLookup, argS.arg, argS.argType,
            argS.argOffset, Some(arg2Yago), Some(arg2Lemon))
        }
        new AnnontatedRelation(arg1, r.rel, r.relOffset, args2, r.pattyResult, r.dbpediaProps)
      }

      //annotates all entities with dbpedia classes and  yago geo types
      //if annotation doesn't exists then the attribute remains empty list
      val annRelations = relations.map(x => (x._1, x._2.map(y => annRelation(y))))


      val focusWords = Seq("this", "these", "it", "there", "where", "here", "one", "its")

      val relationScorer = new NaiveScorer

      @tailrec
      //takes all relations of unknown group and tries to find subgroups on common data
      //all made decision are supported by some weight, which is stored in tree
      def joinEqualNodes(relations: Seq[AnnontatedRelation], trees: Seq[Tree] = Seq(),
                         focusWordsTree: Option[Map[AnnontatedRelation, Weight]] = None): Seq[Tree] = {
        if (relations.isEmpty) {
          trees ++ (if (focusWordsTree.isDefined) Seq(new Tree(focusWordsTree.getOrElse(Map()))) else Nil)
        }
        else {
          val r = relations.head
          val arg = r.arg1
          var matchFound = false

          val newTrees = for (tree <- trees) yield {
            //compare two relation and calculate best match score w.r.t used support source
            val edges = tree.edges.foldLeft(Seq[(Boolean, Double)]()) {
              (s, edge) =>
                val comparison = compareArguments(edge._1.arg1, arg)
                if (comparison._1) s :+ comparison else s
            }

            val bestMatch = edges.foldLeft((false, 0.0))((g, e) => if (e._1 && e._2 > g._2) e else g)

            //if there are a match then add new relation to actual tree node
            if (bestMatch._1) {
              matchFound = true
              new Tree(tree.edges + (r -> new Weight(relationScorer.calculateRelationScore(r), bestMatch._2)))
            }
            else tree
          }

          val newFocusWordsTree = if (focusWords.find(word => arg.arg.contains(word)).size > 0)
            Some(focusWordsTree.getOrElse(Map()) + (r -> new Weight(relationScorer.calculateRelationScore(r), 0.5)))
          else focusWordsTree


          val newRoot = if (matchFound) newTrees
          else newTrees :+ new Tree(Map(r -> new Weight(relationScorer.calculateRelationScore(r), 1.0)))

          joinEqualNodes(relations.tail, newRoot, newFocusWordsTree)
        }
      }

      //compares two arguments and returns boolean meaning there are some equal mentions
      // the double value indicates a similarity score
      def compareArguments(arg1: AnnotatedArgument, arg2: AnnotatedArgument): (Boolean, Double) = {
        val s = (arg1.spotlight, arg2.spotlight) match {
          case (Some(x), Some(xy)) => (x.uri.equals(xy.uri), x.score + xy.score - (x.score * xy.score))
          case _ => (false, 0.0)
        }
        val c = (arg1.clavin, arg2.clavin) match {
          case (Some(x), Some(xy)) => (x.asciiName.equals(xy.asciiName), 1.0)
          case _ => (false, 0.0)
        }
        val l = (arg1.lemon, arg2.lemon) match {
          case (Some(x), Some(xy)) => (x.map(x => x.uri).intersect(xy.map(y => y.uri)).nonEmpty, 1.0)
          case _ => (false, 0.0)
        }
        val y = (arg1.yago, arg2.yago) match {
          case (Some(x), Some(xy)) => (x.intersect(xy).nonEmpty, 1.0)
          case _ => (false, 0.0)
        }
        val d = (arg1.dbpediaLookup, arg2.dbpediaLookup) match {
          case (Some(x), Some(xy)) => {
            for (a <- x; b <- xy if a.uri.equals(b.uri))
              yield {
                (true, a.score + b.score - (a.score * b.score))
              }
          }
            .foldLeft((false, 0.0))((t, r) => if (r._2 > t._2) r else t)
          case _ => (false, 0.0)
        }
        //TODO blanknodes if equals
        (s._1 || c._1 || l._1 || y._1 || d._1, Vector(s._2, c._2, l._2, y._2, d._2).foldLeft(0.0)((g, n) => max(g, n)))
      }

      val unknownGroupTrees = joinEqualNodes(annRelations.getOrElse(-1, Seq()).toSeq)

      //converts annotate relations into tree with score and append trees of unknown group
      val trees = annRelations.map(relations =>
        new Tree(relations._2.map(r => r -> new Weight(relationScorer.calculateRelationScore(r), 1.0)).toMap))
        .toSeq ++ unknownGroupTrees

      //takes one relation (root) and a set of another relations and tries to chain the root with some relations of the set
      def chainRelations(root: Tree, trees: Seq[Tree]): Tree = {
        val relations = root.edges.keySet

        //find some children of given parent based on similarity measure
        //returns a set of subtrees that have matched the parent
        @tailrec
        def findChildren(parent: AnnontatedRelation, trees: Seq[Tree], children: Seq[Tree] = Seq()): Seq[Tree] = {
          if (trees.isEmpty) children
          else {
            val tree = trees.head.edges
            val endNodes = parent.arg2
            val comparison = for (t <- tree; n <- endNodes) yield {
              val c = compareArguments(n, t._1.arg1)
              (c._1, c._2, t)
            }
            val bestMatch = comparison.foldLeft((false, 0.0), None: Option[(AnnontatedRelation, Weight)])((t, c) =>
              if (c._1 && c._2 > t._1._2) ((c._1, c._2), Some(c._3)) else t)
            bestMatch._2 match {
              case Some(r) if bestMatch._1._1 => children :+ new Tree(Map(r._1 -> new Weight(r._2.weight, bestMatch._1._2)))
              case _ => findChildren(parent, trees.tail, children)
            }
          }
        }

        //for each relation in tree finds some children
        val children = for (relation <- relations) yield {
          val children = findChildren(relation, trees)
          relation -> children
        }
        new Tree(root.edges, Some(children.toMap))
      }

      @tailrec
      //traverse a seq of relations and chain it with some children
      def traverseTree(head: Seq[Tree], tail: Seq[Tree], trees: Seq[Tree] = Seq()): Seq[Tree] = {
        if (tail.nonEmpty) {
          val tree = chainRelations(tail.head, head ++ tail.tail)
          traverseTree(head :+ tail.head, tail.tail, trees :+ tree)
        }
        else trees
      }
      traverseTree(Nil, trees)
    }


    Await.result(trees, 1000.seconds).foreach {
      x => println(x.score)
    }


    //TODO create query

    //TODO Yago rdf: type


  }


  //Takes relations and replace predicates with patty pos tags
  def posRelAnnotation(sentences: Sentences, relations: Array[Seq[Relation]]): Array[Seq[Relation]] = {

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
          subSentence.map(t => mapToPattyTags.getOrElse(t._2, t._1))
        }

      val relTagged: String = if (posTaggedRelationList.isEmpty) relation.rel._1
      else posTaggedRelationList.next().reduce((a, b) => a + " " + b)

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
  def calculateRelationScore(r: AnnontatedRelation): Double
}

/**
 * Naive implementation of relation scoring based on size of retried information.
 */
class NaiveScorer extends RelationScorer {

  //calculates max score of given relation
  //score is defined between 0 and 1
  override def calculateRelationScore(r: AnnontatedRelation): Double = {
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

    val relationScore = ((if (r.dbpediaProps.isDefined) 1.0 else 0.0) + (if (r.pattyResult.isDefined) 1.0 else 0.0)) / 2.0

    val arg1Score = calculateArgScore(r.arg1)

    val maxArg2Score = r.arg2.foldLeft(0.0) { (score, arg) =>
      val actualScore = calculateArgScore(arg)
      if (actualScore > score) actualScore else score
    }

    (relationScore + arg1Score + maxArg2Score) / 3.0
  }
}

//the double value of the map represents the membership score of relation
class Tree(val edges: Map[AnnontatedRelation, Weight],val children: Option[Map[AnnontatedRelation, Seq[Tree]]] = None) {

  private def extractWeights(t: Tree) = t.edges.map(x => x._2).toSeq
  private def calculateWeights(s: Seq[Weight]): Double = s.foldLeft(0.0)((g,w) => g + w.weight * w.factor) / s.size.toDouble

  //TODO number of blank nodes

  //score of that tree calculated from score of single relations and support factors
  val score: Double = {
    children match {
      case Some(c) => calculateWeights(extractWeights(this) ++ c.flatMap(x => x._2.flatMap(y => extractWeights(y))))
      case _ => calculateWeights(extractWeights(this))
    }
  }


}

case class Weight(weight: Double, factor: Double)