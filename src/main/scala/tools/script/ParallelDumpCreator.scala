package tools.script

import java.io.{BufferedWriter, File, FileOutputStream, OutputStreamWriter}
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import akka.actor._
import akka.routing.RoundRobinPool
import core.{RawRelation, RelationExtraction}
import nlp.TextAnalyzerPipeline

import scala.io.Source

/**
 * Creates json dump of relation in parallel manner.
 * Text file should be specified in params all relations in text going to be extracted
 * and are saved in new dump file.
 */
object ParallelDumpCreator extends App {

  val paths = args

  val system = ActorSystem("DumpCreator")
  val master = system.actorOf(Props(new Master(paths)))

}

/**
 * Akka master. Distributes work and saves the results.
 * @param paths Path to file that should be processed.
 */
class Master(paths: Array[String]) extends Actor with ActorLogging {

  //TODO add configuration
  //number of actors
  val nrOfWorkers = 28

  log.debug("#########################################################")
  log.debug("Number of actors: " + nrOfWorkers)
  log.debug("#########################################################")

  val workerRouter = context.actorOf(
    Props[Worker].withRouter(RoundRobinPool(nrOfWorkers)),
    name = "workerRouter")


  val reader = new JsonDumpReader(paths.head)

  val splitPath = ParallelDumpCreator.paths.head.split("/")
  val pathToRecoveryFile = splitPath.slice(0, splitPath.length - 1).mkString("/") + "/process.log"
  val progress = new WorkProgress(pathToRecoveryFile)

  def next: LocationArticle = {
    val r = reader.next()
    if (progress.wasNotProcessed(r.id)) {
      r
    }
    else {
      if (!hasNext) ParallelDumpCreator.system.shutdown()
      next
    }
  }

  def hasNext = reader.hasNext

  val path = paths.head.split("/")
  val outFile = path.slice(0, path.size - 1).mkString("/") + "/relations.json"
  val writer = new JsonDumpWriter(outFile)

  def write(l: List[Relation]) = l.foreach { r =>
    writer.writeRelation(r)
    progress.addFinishedID(l.head.id)
  }

  var numberOfFinishedWorker = 0

  def done() = {
    log.info("DONE!!!")
    numberOfFinishedWorker += 1
    if (numberOfFinishedWorker == nrOfWorkers) ParallelDumpCreator.system.shutdown()
  }

  var numberOfProcessedArticle = 0

  override def receive: Receive = {
    //worker is requesting work
    case GimmeWork =>
      log.debug("Work request received")
      if (hasNext) sender ! next
      else done()
    //receive workers result
    case Result(rel) =>
      numberOfProcessedArticle += 1
      log.info("Relations extracted. Number of processed articles: " + numberOfProcessedArticle)
      //if (hasNext) sender ! next
      //else done()
      write(rel)
  }

}

/**
 * Simple worker. Ask master for work, if work is available
 * then extracts relations from received text. The result is send back to master.
 */
class Worker extends Actor with ActorLogging {

  //contact master and request work
  val master = ParallelDumpCreator.master
  master ! GimmeWork

  override def receive: Actor.Receive = {
    case l: LocationArticle =>
      log.debug("New job received")
      extractRelations(l)
  }

  //true if two relations are equal
  def equalRawRelations(r1: RawRelation, r2: RawRelation): Boolean = {
    if (r1.relation.equals(r2.relation)) {
      val shareObject = r1.objectCandidates.intersect(r2.objectCandidates).size > 0
      if (shareObject) {
        val shareSubject = r1.subjectCandidates.intersect(r2.subjectCandidates).size > 0
        if (shareSubject) {
          true
        } else false
      } else false
    } else false
  }

  //counts the relation frequence within the document
  def countRawRelations(r: RawRelation, l: List[RawRelation]) = l.count(rel => equalRawRelations(rel, r))

  val analyzer = new TextAnalyzerPipeline

  //DO WORK
  //extract relations from given article text
  def extractRelations(locationArticle: LocationArticle) = {

    //ensures that text chunks are not to large, due to stanford nlp processing
    val MAX_TEXT_LENGTH = 500
    val texts = locationArticle.text.map {
      text => if (text.length > MAX_TEXT_LENGTH) {
        val chars = text.toCharArray
        val l = scala.collection.mutable.ListBuffer[String]()
        var counter = 0
        var sb = new StringBuilder
        while (counter < chars.length) {
          sb.append(chars(counter))
          if (counter % MAX_TEXT_LENGTH == 0 || counter + 1 == chars.length) {
            l += sb.toString
            sb = new StringBuilder
          }
          counter += 1
        }
        log.debug("Text was shorted")
        l.toList
      } else List(text)
    }.flatten.toIterator


    log.debug("Start relation extraction")
    while(texts.hasNext) {
      val text = texts.next()
      try {
        log.debug("Start text analyzing")
        val analyzed = analyzer.analyzeText(text)
        log.debug("Relation extraction")
        val rel = RelationExtraction.extractRelations(analyzed)

        log.debug("Relation extraction finished.")
        val r = rel.map(r => new Relation(locationArticle.title, locationArticle.id, r.objectCandidates,
          r.relation, r.subjectCandidates, r.sentiment.getOrElse(-1), countRawRelations(r, rel)))
        log.info("RELATIONS SUCCESSFULLY EXTRACTED")
        master ! Result(r)
      } catch {
        case e: Exception => log.info("Error during relation extraction " + e.printStackTrace())
      }
    }

    //send the result to master
    log.debug("Send extracted relations to master")
    master ! GimmeWork
  }
}

/**
 * Saves the progress status. If error occur during processing,
 * then helps to recover after restart.
 */
class WorkProgress(path: String) {

  var cacheEmpty = true
  val processedIds = {
    val l = new scala.collection.mutable.ListBuffer[String]()
    if (Files.exists(Paths.get(path))) {
      val decoder = Charset.forName("UTF-8").newDecoder()
      val lines = Source.fromFile(path)(decoder).getLines().toArray
      l ++= lines
      cacheEmpty = false
    }
    l
  }

  val target = new File(path)
  val fos = new FileOutputStream(target, true)
  val os = new OutputStreamWriter(fos, "UTF-8")
  val br = new BufferedWriter(os)

  def addFinishedID(id: String) = {
    if(!processedIds.contains(id)) {
      processedIds += id
      br.write(id + "\n")
      br.flush()
    }
  }

  var idFound = false

  def wasNotProcessed(id: String): Boolean = {
    if (!cacheEmpty) {
      !processedIds.contains(id)
    } else true
  }

}

case class GimmeWork()

case class Result(r: List[Relation])