package tools

import java.io.{BufferedWriter, File, FileOutputStream, OutputStreamWriter}


/**
 * Allows to read and write dumps in xml format
 */
abstract class DumpXMLReader(var path: String) {

  val target = new File(path.substring(0, path.length - 4) + "_annotated.xml")
  val fos = new FileOutputStream(target, true)
  val br = new BufferedWriter(new OutputStreamWriter(fos))
  br.write("<pages>\n")

  def write(text: String): Unit = {
    br.synchronized {
      br.write(text)
      br.flush()
    }
  }

  //Map(title -> Map(mytitle -> mytitle), text -> Map(p1 -> contetn, p2 -> content,..),lat -> Map(lat->lat), long -> ..)
  def readPage: Map[String, Map[String, Set[String]]]

  def hasMorePages: Boolean

  //Map(title -> Map(par1 -> Set(text), par2->(text)), dbpedia -> Map(lat->Set(12.111,13.22),long-> ..), lat -> ...)
  def writePage(page: Map[String, Map[String, Set[String]]]) = {
    val sb = new StringBuilder()

    sb.append("  <page>\n")

    val title = page.getOrElse("title", Map("" -> Set(""))).getOrElse("title", Set("")).head
    val paragraphs = page.getOrElse(title, Map("" -> Set("")))
    val dbpedia = page.getOrElse("dbpedia", Map("" -> Set("")))
    val lat = page.getOrElse("lat", Map("" -> Set(""))).getOrElse("lat", Set("")).head
    val long = page.getOrElse("long", Map("" -> Set(""))).getOrElse("long", Set("")).head

    //title
    sb.append(s"    <title>$title</title>\n")

    //dbpedia
    sb.append("    <dbpedia>\n")
    dbpedia.foreach {
      case (key, value: Set[String]) => {
        value.foreach {
          elem => if (!elem.equals("")) sb.append(s"      <$key>$elem</$key>\n")
        }
      }
    }
    sb.append("    <dbpedia>\n")

    //lat & long
    sb.append(s"    <lat>$lat</lat>\n")
    sb.append(s"    <long>$long</long>\n")

    //paragraphs
    sb.append("    <text>\n")
    paragraphs.foreach {
      case (key, value: Set[String]) => {
        sb.append( s"""      <paragraph name="$key">""")
        sb.append(value.reduce((a, b) => a + "\n" + b))
        sb.append("      </paragraph>\n")
      }
    }
    sb.append("    <text>\n")

    sb.append("  </page>\n")
    val s = sb.toString()
    write(s)
  }

  def close() = {
    br.write("</pages>\n")
    br.flush()
    fos.flush()
  }

}

class TravelerPoint(path: String) extends DumpXMLReader(path) {

  println("Laod file: " + path)

  val source = scala.io.Source.fromFile(path)

  var pages: List[Map[String, Map[String, Set[String]]]] = List()

  var title = ""
  var paragraphs: Map[String, Set[String]] = Map()
  var paragraphsName = ""

  for (line <- source.getLines()) {
    if (line.contains("<place>")) {
      title = line.replace("<place>", "").replace("<\\place>", "").trim
    }
    if (line.contains("<\\descryption>")) {
      //saves content in the map
      pages = pages :+ Map(title -> paragraphs, "title" -> Map("title" -> Set(title)))
      title = ""
      paragraphs = Map()
      paragraphsName = ""
    }
    if (line.contains("<paragraph name")) {
      paragraphsName = line.replace( """<paragraph name="""", "").replace("\">", "").trim
    }
    if (line.contains("<p>") && !line.contains("All Rights Reserved")) {
      val text = line.substring(3, line.length - 4)
      val set: Set[String] = paragraphs.getOrElse(paragraphsName, Set()) + text.replace("<p>", "").replace("<\\p>", "")
      paragraphs += (paragraphsName -> set)
    }
    if (line.contains("<li>") && !line.contains("All Rights Reserved")) {
      val text = line.substring(4, line.length - 5)
      val set: Set[String] = paragraphs.getOrElse(paragraphsName, Set()) + text.replace("<li>", "").replace("<\\li>", "")
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

  override def hasMorePages: Boolean = pages.nonEmpty
}

class Wikipedia(path: String) extends DumpXMLReader(path) {
  override def readPage: Map[String, Map[String, Set[String]]] = ???


  override def hasMorePages: Boolean = ???
}

class Travelerswiki(path: String) extends DumpXMLReader(path) {

  println("Laod file: " + path)

  val source = scala.io.Source.fromFile(path)

  var pages: List[Map[String, Map[String, Set[String]]]] = List()

  var title = ""
  var superParagraph = ""
  var subParagraph = ""
  var paragraphs: Map[String, Set[String]] = Map()
  var lat = ""
  var long = ""
  var content = false

  for (line <- source.getLines()) {
    if (line.contains("<title>")) {
      title = line.replace("<title>", "").replace("</title>", "")
    }

    if (line.contains("<coordinates")) {
      val part = line.split("lat")(1)
      val split = part.split("lon")
      lat = split(0).trim.substring(2, split(0).trim.length - 1)
      long = split(1).trim.substring(2, split(1).trim.length - 3)
    }

    if (content) {
      if (!line.contains("External links") && !line.contains("References") && !line.contains("Further reading")) {
        val subParaRegex = "(===)(.+)(===)".r
        val superParaRegex = "(==)(.+)(==)".r
        val text = line.trim
        try {
          text match {
            case subParaRegex(a, b, x) => subParagraph = b
          }
        } catch {
          case e: Exception => {
            try {
              text match {
                case superParaRegex(a, b, x) => superParagraph = b
              }
            } catch {
              case e: Exception => {
                if (!text.equals("")) {
                  val p = if(!subParagraph.equals("")) superParagraph + ":" + subParagraph else superParagraph
                  val set = paragraphs.getOrElse(p, Set()) + text
                  paragraphs += (superParagraph -> set)
                }
              }
            }
          }
        }
      }
    }

    if (line.contains("<content>")) {
      val text = line.replace("<content>", "")
      superParagraph = "abstract"
      paragraphs += (superParagraph -> Set(text))
      content = true
    }

    if (line.contains("</content>")) {
      val text = line.replace("</content>", "")
      pages = pages :+ Map(title -> paragraphs, "title" -> Map("title" -> Set(title)), "lat" -> Map("lat" -> Set(lat)),
        "long" -> Map("long" -> Set(lat)))

      lat = ""
      long = ""
      title = ""
      superParagraph = ""
      subParagraph = ""
      paragraphs = Map()

      content = false
    }


  }


  override def readPage: Map[String, Map[String, Set[String]]] = {
    pages.synchronized {
      val page = pages.head
      pages = pages.tail
      page
    }
  }

  override def hasMorePages: Boolean = pages.nonEmpty

}