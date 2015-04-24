package dbpedia

import core.Tree
import nlp.{AnnontatedRelation, AnnotatedArgument}

/**
 * Created by yevgen on 14.04.15.
 */
class SparqlQuery(val tree: Tree) {

  val maxRootScore = (tree: Tree) => tree.edges.foldLeft(0.0)((s, t) => if (t._2.weight > s) t._2.weight else s)
  val bestRoots = (tree: Tree) => tree.edges.filter(x => x._2.weight >= maxRootScore(tree)).map(x => x._1)

  //create sample names for blank nodes
  val alphabet = "abcdefghijklmnopqrstuvwxyz".split("")
  val generate = (s: String) => alphabet.map(c => c + s)
  val blankNodeNames = alphabet.flatMap(c => generate(c)).toIterator

  def blankName = () => if (blankNodeNames.hasNext) blankNodeNames.next()
  else {
    println("Error during blank node names creation. To many names were requested!")
    "default"
  }

  //true if uri found and false if a blank node was returned
  val extractUri = (a: AnnotatedArgument) => a.yago match {
    case Some(uri) => (uri, true)
    case _ => a.lemon match {
      case Some(uri) => (uri.foldLeft("", 0.0)((g, u) => if (u.score > g._2) (u.uri, u.score) else g)._1, true)
      case _ => a.spotlight match {
        case Some(uri) => (uri, true)
        case _ => a.dbpediaLookup match {
          case Some(uris) =>
            val t = uris.foldLeft(blankName, false)((b, r) => if (r.score > 0.5) (() => r.uri, true) else b)
            (t._1() , t._2)
          case _ => (blankName(), false)
        }
      }
    }
  }

  def bestPattyRelations(a: AnnontatedRelation) = a.pattyResult.getOrElse(List()).sortWith((p1, p2)  => p1.score > p2.score).slice(0,3)
  def bestDBPediaProps(a: AnnontatedRelation) = a.dbpediaProps.getOrElse(List()).sortWith((p1,p2) => p1.score > p2.score).slice(0,3)

  //creates a sparql query from given query tree
  lazy val query = {
    val root = bestRoots(tree).head

    val rootUris: Seq[String] = root.arg1.yago match {
      case Some(yagoEntities) => yagoEntities
      case _ => root.arg1.lemon match {
        case Some(lemonClasses) =>
          lemonClasses.map(x => x.uri)
        case _ => Seq(blankName())
      }
    }

    val pattyRelations = bestPattyRelations(root)
    val dbpediaRelations = bestDBPediaProps(root)

    


  }

  lazy val pruned = {

  }
}

case class Query(query: String, score: Double)