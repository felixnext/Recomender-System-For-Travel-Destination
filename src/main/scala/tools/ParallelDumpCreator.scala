package tools

import java.io.{BufferedWriter, OutputStreamWriter, FileOutputStream, File}
import java.nio.charset.Charset

import akka.actor._
import akka.routing.RoundRobinPool

import core.{RawRelation, RelationExtraction}
import nlp.TextAnalyzerPipeline

import java.nio.file.{Paths, Files}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
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

  //number of actors
  val nrOfWorkers = 2

  val workerRouter = context.actorOf(
    Props[Worker].withRouter(RoundRobinPool(nrOfWorkers)).withDispatcher("akka.actor.my-dispatcher"),
    name = "workerRouter")


  val reader = new JsonDumpReader(paths.head)
  val progress = new WorkProgress

  def next: LocationArticle = {
    val r = reader.next()
    if (progress.wasNotProcessed(r.id)) {
      progress.addFinishedID(r.id)
      r
    }
    else {
      if(!hasNext) ParallelDumpCreator.system.shutdown()
      next
    }
  }

  def hasNext = reader.hasNext

  val path = paths.head.split("/")
  val outFile = path.slice(0, path.size - 1).mkString("/") + "/relations.json"
  val writer = new JsonDumpWriter(outFile)

  def write(l: List[Relation]) = l.foreach { r =>
    writer.writeRelation(r)
  }

  var numberOfFinishedWorker = 0

  def done() = {
    log.info("DONE!!!")
    numberOfFinishedWorker += 1
    if (numberOfFinishedWorker == nrOfWorkers) ParallelDumpCreator.system.shutdown()
  }

  var numberOfProcessedArticle = 0

  override def receive: Receive = {
    //worke requests work
    case GimmeWork =>
      log.debug("Work request received")
      if (hasNext) workerRouter ! next
    //receive workers result
    case Result(rel) =>
      numberOfProcessedArticle += 1
      log.info("Relations extracted. Number of processed articles: " + numberOfProcessedArticle)
      if (hasNext) sender ! next
      else done()
      write(rel)
  }

}

/**
 * Simple worker. Ask master for work, if work is available
 * then extracts relations from received text. The result is send back to master.
 */
class Worker extends Actor with ActorLogging {

  val analyzerPipe = new TextAnalyzerPipeline
  val relationExtractor = new RelationExtraction(analyzerPipe)

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

  //DO WORK
  //extract relations from given article text
  def extractRelations(locationArticle: LocationArticle) = {
    val text = locationArticle.text.mkString(" ")

    log.debug("Start relation extraction")
    val result = Future {
      val analyzed = analyzerPipe.analyzeText(text)
      relationExtractor.extractRelations(analyzed)
    }

    val transformedRel = try {
      val rel = Await.result(result, 315.seconds)
      log.debug("Relation extraction finished.")
      rel.map(r => new Relation(locationArticle.title, locationArticle.id, r.objectCandidates,
        r.relation, r.subjectCandidates, r.sentiment.getOrElse(-1), countRawRelations(r, rel)))
    } catch {
      case e: Exception => log.info("Error during relation extraction" + e); List()
    }

    //send the result to master
    log.debug("Send extracted relations to master")
    master ! Result(transformedRel)
  }
}

/**
 * Saves the progress status. If error occur durings processing,
 * then helps to recover after restart.
 */
class WorkProgress {

  val splitPath = ParallelDumpCreator.paths.head.split("/")
  val path = splitPath.slice(0, splitPath.length - 1).mkString("/") + "/process.log"

  var cacheEmpty = true
  val processedIds = {
    val l = new scala.collection.mutable.ListBuffer[String]()
    if(Files.exists(Paths.get(path))) {
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
    if(!processedIds.contains(id)){
      processedIds += id
      br.write(id + "\n")
      br.flush()
    }
  }

  var idFound = false
  def wasNotProcessed(id: String): Boolean = {
    if(!cacheEmpty && !idFound) {
      if(processedIds(processedIds.length - 1).equals(id)){
        idFound = true
      }
      false
    } else true
  }

}

case class GimmeWork()

case class Result(r: List[Relation])