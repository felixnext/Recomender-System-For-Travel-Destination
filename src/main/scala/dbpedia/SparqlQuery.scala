package dbpedia

import core.Tree
import dbpedia.SparqlQuery.{Query, RDFTriple}
import nlp.{AnnontatedRelation, AnnotatedArgument}

import scala.util.Try

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
            (t._1(), t._2)
          case _ => (blankName(), false)
        }
      }
    }
  }

  def bestPattyRelations(a: AnnontatedRelation) = a.pattyResult.getOrElse(List()).sortWith((p1, p2) => p1.score > p2.score).slice(0, 3)

  def bestDBPediaProps(a: AnnontatedRelation) = a.dbpediaProps.getOrElse(List()).sortWith((p1, p2) => p1.score > p2.score).slice(0, 3)


  //creates a sparql query from given query tree
  lazy val query1 = {
    val root = bestRoots(tree).head

    val rootUris: Seq[String] = root.arg1.yago match {
      case Some(yagoEntities) => yagoEntities
      case _ => root.arg1.lemon match {
        case Some(lemonClasses) =>
          lemonClasses.map(x => x.uri)
        case _ => Seq(blankName())
      }
    }


    val relScore: AnnontatedRelation => Double = rel => Try(tree.edges.get(rel).get.weight * tree.edges.get(rel).get.factor).getOrElse(0.0)

    def createBlankNode(name: String, rel: AnnontatedRelation): (String, Double) = {
      val rdfTriple = tree.children match {
        case Some(x) => x.get(rel) match {
          case Some(childrenRelations) => {
            for (childTree <- childrenRelations) {
              childTree.edges
            }
          }
          case _ => (name + "\n", relScore(rel))
        }
        case _ => (name + "\n", relScore(rel))
      }
      ("", 0.0)
    }

    def pushUpRelations(rel: AnnontatedRelation): String = ???

    val pattyRelations = bestPattyRelations(root)
    val dbpediaRelations = bestDBPediaProps(root)

    var rdfTriples = List()

    for (a <- root.arg2) {
      val uri = a.spotlight match {
        case Some(s) => s.uri
        case _ => a.dbpediaLookup match {
          case Some(l) =>
            //choose the best entity with max score, but score have to be greater then 0.5
            val possibleUri = l.foldLeft(blankName, false, 0.0)((b, r) => if (r.score > 0.5 && r.score > b._3) (() => r.uri, true, r.score) else b)
            if (possibleUri._2) possibleUri._1()
            else createBlankNode(possibleUri._1(), root)
          //if neither dbpedia lookup entity nor spotlight was found
          //then try to find dbpedia class instead of entity
          case _ => a.yago match {
            case Some(y) => pushUpRelations(root) :: rdfTriples; y
            case _ => a.lemon match {
              case Some(lemon) => pushUpRelations(root) :: rdfTriples; lemon
              case _ => createBlankNode(blankName(), root)
            }
          }
        }
      }
    }


  }

  lazy val query = {

    val roots = bestRoots(tree)

    //queries
    for (root <- roots) {
      //extract possible root uri
      val rootUris: Seq[String] = root.arg1.yago match {
        case Some(yagoEntities) => yagoEntities
        case _ => root.arg1.lemon match {
          case Some(lemonClasses) =>
            lemonClasses.map(x => x.uri)
          //creates blank node with saple name
          case _ => Seq(blankName())
        }
      }

      //extract possible predicates
      val pattyRelations = bestPattyRelations(root)
      val dbpediaRelations = bestDBPediaProps(root)

      //extract object of rdf triples
      //triples
      val spotlight: Seq[(String, Double)] = for (a <- root.arg2 if a.spotlight.isDefined) yield {
        a.spotlight match {
          case Some(u) => (u.uri, u.score)
        }
      }
      //if the object of a triple is an entity then no children are required
      //because of pruning of redundant information

      val dbpediaLookup: Seq[(String, Double)] = for (a <- root.arg2 if a.dbpediaLookup.isDefined) yield {
        a.dbpediaLookup match {
          case Some(l) => l.foldLeft(l.head.uri, l.head.score)((g, n) => if (n.score > g._2) (n.uri, n.score) else g)
        }
      }

      val yago = for (a <- root.arg2 if a.yago.isDefined) yield {
        a.yago match {
          case Some(y) => y //TODO extract children and push up relations
        }
      }

      val lemonClasses = for (a <- root.arg2 if a.lemon.isDefined) yield {
        a.lemon match {
          case Some(l) => l.map(x => (x.uri, x.score)) //TODO extract children and push up relations
        }
      }

      val blankNodes = for (a <- root.arg2 if !a.spotlight.isDefined && !a.dbpediaLookup.isDefined
        && !a.yago.isDefined && !a.lemon.isDefined) yield {
        //TODO create blank Node and extract properties
        blankName()
      }


    }

    type ChildrenQueries = Iterable[Option[RDFTriple]]

    //extracts children of a given root relation and returns a list of all possible queries
    def extractChildren(subject: String, rel: AnnontatedRelation): Option[List[Query]] = {
      if (tree.children.isDefined && tree.children.get.get(rel).isDefined) {
        val children = tree.children.get.get(rel).get
        val queries: Iterable[Option[Query]] = for (childrenTree <- children) yield {
          val relations = childrenTree.edges

          val queries: ChildrenQueries = for (relation <- relations) yield {
            val root = relation._1

            //extract possible predicates
            val pattyRelations = bestPattyRelations(root)
            val dbpediaRelations = bestDBPediaProps(root)

            //extract object of rdf triples
            //triples
            val spotlight: Seq[(String, Double)] = for (a <- root.arg2 if a.spotlight.isDefined) yield {
              a.spotlight match {
                case Some(u) => (u.uri, u.score)
              }
            }
            //if the object of a triple is an entity then no children are required
            //because of pruning of redundant information

            val dbpediaLookup: Seq[(String, Double)] = for (a <- root.arg2 if a.dbpediaLookup.isDefined) yield {
              a.dbpediaLookup match {
                case Some(l) => l.foldLeft(l.head.uri, l.head.score)((g, n) => if (n.score > g._2) (n.uri, n.score) else g)
              }
            }

            //if some dbpedia uris were  found then creates new Query
            val sPatty = for (r <- pattyRelations; o <- spotlight) yield new RDFTriple(subject, r.dbpediaRelation, o._1, o._2)
            val dPatty = for (r <- pattyRelations; o <- dbpediaLookup) yield new RDFTriple(subject, r.dbpediaRelation, o._1, o._2)
            val sPred = for (r <- dbpediaRelations; o <- spotlight) yield new RDFTriple(subject, r.dbpediaUri, o._1, o._2)
            val dPred = for (r <- dbpediaRelations; o <- dbpediaLookup) yield new RDFTriple(subject, r.dbpediaUri, o._1, o._2)
            val concat = sPatty ++ dPatty ++ sPred ++ dPred
            if(concat.size > 0) Some(concat.foldLeft(concat.head)((g,t) => if(t.score > g.score) t else g)) else None
          }
          //use filter rdf triples, which are defined and use them for queries creating
          val triples = queries.foldLeft(List[RDFTriple]())((l,triple) => if(triple.isDefined) triple.get :: l else l )
         if(triples.size > 0) Some(new Query(triples)) else None
        }

        //only if some query exists then return a list else none
        val q = queries.filter(q => q.isDefined).map(q => q.get).toList
        if(q.size > 0) Some(q) else None
      }
      else {
        None
      }
    }
  }

  lazy val pruned = {

  }
}

//Companion object, namespace for query and rdf triple class
object SparqlQuery {

  case class Query(triples: List[RDFTriple])

  case class RDFTriple(s: String, pred: String, o: String, score: Double)

}

