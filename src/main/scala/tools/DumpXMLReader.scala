package tools

import scalax.io.{Output, Resource, Codec}


/**
 * Allows to read and write dumps in xml format
 */
abstract class DumpXMLReader(var path: String) {

  val output = {
    val output:Output = Resource.fromFile(path.substring(0,path.length-4)+"_annotated.xml")
    output.write("<pages>")(Codec.UTF8)
    output
  }

  //Map(title -> Map(mytitle -> mytitle), text -> Map(p1 -> contetn, p2 -> content,..),lat -> Map(lat->lat), long -> ..)
  def readPage: Map[String, Map[String, Set[String]]]

  def hasMorePages: Boolean

  def writePage(page: Map[String, Map[String, Set[String]]]) = {
    output.write("  <page>")


    output.write("  </page>")

  }

  def close() = {
    output.write("</pages>")
  }

}

class TravelerPoint(path: String) extends DumpXMLReader(path) {


  val source = scala.io.Source.fromFile(path)

  var pages: List[Map[String, Map[String, Set[String]]]] = List()

  var title = ""
  var paragraphs: Map[String, Set[String]] = Map()
  var paragraphsName = ""

  for (line <- source.getLines()) {
    if (line.contains("<place>")) {
      title = line.replace("<place>", "").replace("<\\place>", "")
    }
    if (line.contains("<\\description>")) {
      pages = pages :+ Map(title -> paragraphs)
      title = ""
      paragraphs = Map()
      paragraphsName = ""
    }
    if (line.contains("<paragraph name")) {
      paragraphsName = line.replace( """<paragraph name="""", "").replace("\">", "")
    }
    if (line.contains("<p>") && !line.contains("All Rights Reserved Utrecht")) {
      val text = line.substring(3, line.length - 4)
      val set: Set[String] = paragraphs.getOrElse(paragraphsName,Set()) + text
      paragraphs += (paragraphsName -> set)
    }

  }

  override def readPage: Map[String, Map[String, Set[String]]] = {
    pages.synchronized {
      val page = pages.head
      pages = pages.tail
      page
    }
  }

  override def writePage(page: Map[String, Map[String, Set[String]]]): Unit = {

  }

  override def hasMorePages: Boolean = pages.isEmpty
}

class Wikipedia(path: String) extends DumpXMLReader(path) {
  override def readPage: Map[String, Map[String, Set[String]]] = ???

  override def writePage(page: Map[String, Map[String, Set[String]]]): Unit = ???

  override def hasMorePages: Boolean = ???
}

class Travelerswiki(path: String) extends DumpXMLReader(path) {
  override def readPage: Map[String, Map[String, Set[String]]] = ???

  override def writePage(page: Map[String, Map[String, Set[String]]]): Unit = ???

  override def hasMorePages: Boolean = ???
}