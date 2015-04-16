package nlp

import edu.knowitall.openie.{OpenIE, SimpleArgument, SpatialArgument, TemporalArgument}

/**
 * Created by yevgen on 12.04.15.
 */
class RelationExtractor {

  val openie = new OpenIE()

  def extractRelations(sentences: String): Seq[Relation] = {

    val result = openie.extract(sentences)

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
      new Relation(arg1, res.extraction.rel.text, (res.extraction.rel.offsets.head.start, res.extraction.rel.offsets.head.end), arg2)
    }

    relations
  }
}

case class OpenieResponse(result: Seq[Relation])

case class Relation(arg1: Argument, rel: String, relOffset: (Int, Int), arg2: Seq[Argument])

case class Argument(arg: String, argType: String, argOffset: (Int, Int))
