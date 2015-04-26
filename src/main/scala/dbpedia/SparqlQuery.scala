package dbpedia

import core.Tree
import dbpedia.SparqlQuery.{Query, RDFTriple}
import elasticsearch.{DBPediaProps, PattyRelation}
import nlp.AnnontatedRelation

/**
 * This class represents sparql query. Main task is converting acyclic tree with multiple candidates per node and endge
 * into a sequence of sparql queries. Each query will be scored w.r.t. quality of used uris.
 */
class SparqlQuery(val tree: Tree) {

  type ChildrenQueries = Iterable[Option[RDFTriple]]
  type ScoredTriples = Seq[(String, Double)]

  //extracts children of a given root relation and returns a list of all possible queries
  private def extractChildren(subject: String, rel: AnnontatedRelation): Option[List[Query]] = {
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
          val spotlight: ScoredTriples = for (a <- root.arg2 if a.spotlight.isDefined) yield {
            (a.spotlight.get.uri, a.spotlight.get.score)
          }

          //if the object of a triple is an entity then no children are required
          //because of pruning of redundant information
          val dbpediaLookup: ScoredTriples = for (a <- root.arg2 if a.dbpediaLookup.isDefined) yield {
            val l = a.dbpediaLookup.get
            l.foldLeft(l.head.uri, l.head.score)((g, n) => if (n.score > g._2) (n.uri, n.score) else g)
          }

          findBestCombination(subject, pattyRelations, dbpediaRelations, spotlight, dbpediaLookup)
        }
        //use filter rdf triples, which are defined and use them for queries creating
        val triples = queries.foldLeft(List[RDFTriple]())((l, triple) => if (triple.isDefined) triple.get :: l else l)
        if (triples.size > 0) Some(new Query(triples)) else None
      }

      //only if some query exists then return a list else none
      val q = queries.filter(q => q.isDefined).map(q => q.get).toList
      if (q.size > 0) Some(q) else None
    }
    else {
      None
    }
  }

  val maxRootScore = (tree: Tree) => tree.edges.foldLeft(0.0)((s, t) => if (t._2.weight > s) t._2.weight else s)
  val bestRoots = (tree: Tree) => tree.edges.filter(x => x._2.weight >= maxRootScore(tree)).map(x => x._1)

  //create sample names for blank nodes
  val alphabet = "abcdefghijklmnopqrstuvwxyz".split("").toList
  val generate = (s: String) => alphabet.map(c => c + s)

  //create infinite stream of all combinations in alphabet
  //in each iteration the number of latters increases and new combinations are created
  val stream: Stream[List[String]] = alphabet #:: stream.flatMap(n => n.map(c => generate(c)))
  val blankNodeNames = stream.flatten.iterator

  private def blankName = () => "?" + blankNodeNames.next

  private def bestPattyRelations(a: AnnontatedRelation) = a.pattyResult.getOrElse(List()).sortWith((p1, p2) => p1.score > p2.score).slice(0, 2)

  private def bestDBPediaProps(a: AnnontatedRelation) = a.dbpediaProps.getOrElse(List()).sortWith((p1, p2) => p1.score > p2.score).slice(0, 2)

  val rdfType = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"


  //Finds the relations with best score and create it
  private def findBestCombination(subject: String, pattyRelations: List[PattyRelation], dbpediaRelations: List[DBPediaProps],
                                  spotlight: Seq[(String, Double)], dbpediaLookup: Seq[(String, Double)]): Option[RDFTriple] = {

    //if some dbpedia uris were  found then creates new triple
    val sPatty = for (r <- pattyRelations; o <- spotlight) yield new RDFTriple(subject, r.dbpediaRelation, o._1, o._2)
    val dPatty = for (r <- pattyRelations; o <- dbpediaLookup) yield new RDFTriple(subject, r.dbpediaRelation, o._1, o._2)
    val sPred = for (r <- dbpediaRelations; o <- spotlight) yield new RDFTriple(subject, r.dbpediaUri, o._1, o._2)
    val dPred = for (r <- dbpediaRelations; o <- dbpediaLookup) yield new RDFTriple(subject, r.dbpediaUri, o._1, o._2)
    val concat = sPatty ++ dPatty ++ sPred ++ dPred

    //if neither patty predicates nor dbpedia predicates were found then create blank predicates
    val blankPredicates = if (concat.size == 0) {
      for (o <- spotlight) yield new RDFTriple(subject, blankName(), o._1, o._2)
    } ++ {
      for (o <- dbpediaLookup) yield new RDFTriple(subject, blankName(), o._1, o._2)
    } else concat

    //choose a triple with best score
    if (blankPredicates.size > 0) Some(blankPredicates.foldLeft(blankPredicates.head)((g, t) => if (t.score > g.score) t else g)) else None
  }

  //creates sparql queries from given query tree
  lazy val queries: Seq[(String, Double)] = {

    val roots = bestRoots(tree)
    val rootUris: Seq[String] = Seq(blankName())

    //queries
    val queryPerRoot: ScoredTriples = (for (root <- roots) yield {
      //extract possible root uri

      //extract possible predicates
      val pattyRelations = bestPattyRelations(root)
      val dbpediaRelations = bestDBPediaProps(root)

      //extract object of rdf triples
      //triples
      val spotlight: ScoredTriples = for (a <- root.arg2 if a.spotlight.isDefined) yield {
        (a.spotlight.get.uri, a.spotlight.get.score)
      }

      //if the object of a triple is an entity then no children are required
      //because of pruning of redundant information
      val dbpediaLookup: ScoredTriples = for (a <- root.arg2
                                              if a.dbpediaLookup.isDefined && a.dbpediaLookup.get.size > 0) yield {
        val l = a.dbpediaLookup.get
        l.foldLeft(l.head.uri, l.head.score)((g, n) => if (n.score > g._2) (n.uri, n.score) else g)
      }

      val yago = for (a <- root.arg2 if a.yago.isDefined) yield {
        val y = a.yago.get
        (y, y.map(y => extractChildren(y, root))
          .foldLeft(List[Query]())((g, q) => if (q.isDefined) q.get ++ g else g))
      }

      val lemonClasses = for (a <- root.arg2 if a.lemon.isDefined) yield {
        val l = a.lemon.get
        val uris = l.map(x => (x.uri, x.score))
        (uris, uris.map(x => extractChildren(x._1, root))
          .foldLeft(List[Query]())((g, q) => if (q.isDefined) q.get ++ g else g))

      }

      val blankNodes = for (a <- root.arg2 if !a.spotlight.isDefined && !a.dbpediaLookup.isDefined
        && !a.yago.isDefined && !a.lemon.isDefined) yield {
        val name = blankName()
        (name, extractChildren(name, root))
      }

      //creates triple string, wich can be used in spaql query
      //no prefixes are required
      def tripleToString(l: Seq[RDFTriple]): Seq[(String, Double)] =
        if (l.size > 0) l.map(t => (t.s + " " + t.pred + " " + t.o + " .\n", t.score)) else Seq(("", 0.0))

      //push up the relations from yago entity as subject one level up
      def pushUpRelations(l: List[Query], newSubbject: String) = l.map(q => q.triples.map(old =>
        new RDFTriple(newSubbject, old.pred, old.o, old.score))).flatten

      //iterates over all possible root uris and creates for each uri new query
      val triplesPerSubject: ScoredTriples = (for (subject <- rootUris) yield {
        //triples with entity as object
        val entitiyTriples = findBestCombination(subject, pattyRelations, dbpediaRelations, spotlight, dbpediaLookup)

        //tripes with yago classes as object
        val yagoTriples = (for (y <- yago) yield {
          val triple1 = for (yagoType <- y._1) yield new RDFTriple(subject, rdfType, yagoType, 1.0)

          //push up the relations from yago entity as subject one level up
          val triples = pushUpRelations(y._2, subject)
          triple1 ++ triples
        }).flatten

        //triples with lemon classes as object
        val lemonTriples = (for (l <- lemonClasses) yield {
          val classTriplesPatty = l._1.map(o => pattyRelations.map(r =>
            new RDFTriple(subject, r.dbpediaRelation, o._1, o._2))).flatten

          val classTriplesProperties = l._1.map(o => dbpediaRelations.map(r =>
            new RDFTriple(subject, r.dbpediaUri, o._1, o._2))).flatten

          val triples = pushUpRelations(l._2, subject)


          //if now predicates were found then creates treple with bkank predicates
          val concat = if (classTriplesPatty.size == 0 && classTriplesProperties.size == 0) {
            List(l._1.map(o => new RDFTriple(subject, blankName(), o._1, o._2)) ++ triples)
          } else {
            List(classTriplesPatty ++ triples, classTriplesProperties ++ triples)
          }
          concat
        }).flatten.flatten


        //create triples with blank nodes as objects
        val blankNodesTriples = (for (blank <- blankNodes) yield {
          val triplesPatty = pattyRelations.map(r => new RDFTriple(subject, r.dbpediaRelation, blank._1, 0.1))
          val triplesPpred = dbpediaRelations.map(r => new RDFTriple(subject, r.dbpediaUri, blank._1, 0.1))

          if (blank._2.isDefined) {
            val children = pushUpRelations(blank._2.get, subject)
            List(triplesPatty ++ children, triplesPpred ++ children)
          } else {
            List(triplesPatty, triplesPpred)
          }

        }).flatten.flatten

        //creates rdf triple list and ensure that lists are not empty for later iteration
        val nonEmptyBlank = tripleToString(blankNodesTriples)
        val nonEmptyYago = tripleToString(yagoTriples)
        val nonEmptyLemon = tripleToString(lemonTriples)
        val nonEmptyEntity = if (entitiyTriples.isDefined) tripleToString(Seq(entitiyTriples.get)) else tripleToString(Seq())

        //extract yago entities of roots first argument
        val rootYagoClass = {
          val triples = if (root.arg1.yago.isDefined) {
            val triples = for (y <- root.arg1.yago.get) yield new RDFTriple(subject, rdfType, y, 1.0)
            tripleToString(triples)
          } else tripleToString(Seq())
          triples.map(t => t._1).mkString("")
        }

        //extract dbpedia classes of roots first argument
        val rootDBPediaClass = {
          val triples = if (root.arg1.lemon.isDefined) {
            val triples = for (l <- root.arg1.lemon.get) yield new RDFTriple(subject, rdfType, l.uri, 1.0)
            tripleToString(triples)
          } else tripleToString(Seq())
          triples.map(t => t._1).mkString("")
        }

        //create ready rdf triples for use in sparql query
        //each set of triple get a score
        //in a single string a saved multiple triples
        val readyTriples: ScoredTriples =
          for (b <- nonEmptyBlank; y <- nonEmptyYago; l <- nonEmptyLemon; e <- nonEmptyEntity) yield {
            val triples = b._1 + y._1 + l._1 + e._1
            val scores: Vector[Double] = Vector(b._2, y._2, l._2, e._2)
            val numberOfZeros = scores.count(v => v > 0.001)
            val score = scores.sum / (if (numberOfZeros == 0) 1.0 else numberOfZeros.toDouble)
            (rootYagoClass + rootDBPediaClass + triples, score)
          }
        readyTriples
      }).flatten

      triplesPerSubject
    }).flatten.toSeq


    val foundQueries = queryPerRoot.map { r =>
      val triples = r._1
      val rootName = rootUris.head
      val query =
        s"""
        |SELECT DISTINCT $rootName
        |WHERE {
        |$rootName $rdfType <http://dbpedia.org/class/yago/YagoGeoEntity> .
        |$triples
        |}
         """.stripMargin
      (query, r._2)
    }
    foundQueries.filter(q => q._2 > 0.001)
  }

}

//Companion object, namespace for query and rdf triple class
object SparqlQuery {

  case class Query(triples: List[RDFTriple])

  case class RDFTriple(s: String, pred: String, o: String, score: Double)

}

