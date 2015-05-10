package nlp

import edu.knowitall.openie._

/**
 * Extracts relation from raw text with help of openie util.
 */
class RelationExtractor {

  val openie = new OpenIE(triples = false)

  //extracts relations and returns a seq of relations
  def extractRelations(sentences: String): Seq[Relation] = {


    var result = Seq[Instance]()

    /*
    openie.synchronized{
      result = openie.extract(sentences)
    }
    */
    result = openie.extract(sentences)

    def matchArgType(arg: Any): String = {
      arg match {
        case SimpleArgument(_, _) => "SimpleArgument"
        case SpatialArgument(_, _) => "SpatialArgument"
        case TemporalArgument(_, _) => "TemporalArgument"
        case _ => ""
      }
    }

    val relations: Seq[Relation] = for (res <- result) yield {
      val arg1Text = res.extraction.arg1.text
      val arg1Offset = (res.extraction.arg1.offsets.head.start, res.extraction.arg1.offsets.head.end)
      val argType = matchArgType(res.extraction.arg1)
      val arg1 = new Argument(arg1Text, argType, arg1Offset)
      val arg2 = res.extraction.arg2s.map(arg2 => new Argument(arg2.text, matchArgType(arg2), (arg2.offsets.head.start, arg2.offsets.head.end)))
      new Relation(arg1, (res.extraction.rel.text,"") , (res.extraction.rel.offsets.head.start, res.extraction.rel.offsets.head.end), arg2)
    }

    relations
  }
}

/*
object Openie {

  val openie = new OpenIE(triples = false)
}
*/

case class OpenieResponse(result: Seq[Relation])

//in relation tuple (String, String) the first argument is a original relation string and the second one a pos tagged relation
case class Relation(arg1: Argument, rel: (String, String), relOffset: (Int, Int), arg2: Seq[Argument])

case class Argument(arg: String, argType: String, argOffset: (Int, Int))
