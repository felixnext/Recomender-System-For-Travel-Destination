import java.io.File

import org.scalatest.{Matchers, FlatSpec}
import tools.WorkProgress

/**
 * Tests the relations extraction progress.
 */
class WorkProgressTest extends FlatSpec with Matchers{

  val path = System.getProperty("java.io.tmpdir") + "/progress.log"
  val file = new File(path)
  file.delete()

  "progress" should "contain every added value" in {
    val progress1 = new WorkProgress(path)
    progress1.wasNotProcessed("3") should be (right = true)
    progress1.addFinishedID("3")
    progress1.wasNotProcessed("7") should be (right = true)
    progress1.addFinishedID("7")
    progress1.wasNotProcessed("1") should be (right = true)
    progress1.addFinishedID("1")

    val progress2 = new WorkProgress(path)
    progress2.wasNotProcessed("1") should be (right = false)
    progress2.wasNotProcessed("2") should be (right = true)
    progress2.wasNotProcessed("1") should be (right = false)

    progress2.wasNotProcessed("7") should be (right = false)
    progress2.wasNotProcessed("5") should be (right = true)
    progress2.wasNotProcessed("6") should be (right = true)


    val progressFile = new File(System.getProperty("java.io.tmpdir") + "progress.log")
    progressFile.delete()

  }

}
