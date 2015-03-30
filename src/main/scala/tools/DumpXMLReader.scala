package tools

/**
 * Allows to read and write dumps in xml format
 */
trait DumpXMLReader {

  //Map(title -> Map(mytitle -> mytitle), text -> Map(p1 -> contetn, p2 -> content,..),lat -> Map(lat->lat), long -> ..)
  def readPage: Map[String, Map[String,String]]

  def writePage(page: Map[String, Map[String,String]])

}

class TravelerPointReader(path: String) extends DumpXMLReader{
  override def readPage: Map[String, Map[String,String]] = ???

  override def writePage(page: Map[String, Map[String,String]]): Unit = ???
}

class Wikipedia(path: String) extends DumpXMLReader {
  override def readPage: Map[String, Map[String,String]] = ???

  override def writePage(page: Map[String, Map[String,String]]): Unit = ???
}

class Travelerswiki(path: String) extends DumpXMLReader {
  override def readPage: Map[String, Map[String,String]] = ???

  override def writePage(page: Map[String, Map[String,String]]): Unit = ???
}