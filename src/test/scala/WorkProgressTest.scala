import java.io.File

import org.scalatest.{Matchers, FlatSpec}
import tools.WorkProgress

/**
 * Tests the relations extraction progress.
 */
class WorkProgressTest extends FlatSpec with Matchers{


  "progress" should "contain every added value" in {
    val progress1 = new WorkProgress(System.getProperty("java.io.tmpdir") + "progress.log")
    progress1.addFinishedID("1")
    progress1.addFinishedID("2")
    progress1.addFinishedID("3")

    val progress2 = new WorkProgress(System.getProperty("java.io.tmpdir") + "progress.log")
    progress2.wasNotProcessed("1") should be (right = false)
    progress2.wasNotProcessed("2") should be (right = false)
    progress2.wasNotProcessed("3") should be (right = false)

    progress2.wasNotProcessed("4") should be (right = true)
    progress2.wasNotProcessed("5") should be (right = true)
    progress2.wasNotProcessed("6") should be (right = true)


    val progressFile = new File(System.getProperty("java.io.tmpdir") + "progress.log")
    progressFile.delete()

  }

}