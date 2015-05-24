import org.scalatest._
import elasticsearch.{DeepParsing => dp}


/**
 * Created by yevgen on 23.05.15.
 */
class DeepParsingTest extends FlatSpec with Matchers {

  val t1 = "Mean high temperatures for late July are primarily in the low 90s Fahrenheit 32–34 °C. Mean low temperatures for early to mid January range from the low 40s Fahrenheit (4–7 °C) in northern Florida to above 60 °F (16 °C) from Miami on southward. With an average daily temperature of 70.7 °F (21.5 °C), it is the warmest state in the country."

  val t2 = "In the summer, high temperatures in the state seldom exceed 100 °F (38 °C). Several record cold maxima have been in the 30s °F (-1 to 4 °C) and record lows have been in the 10s -12 to -7 °C. These temperatures normally extend at most a few days at a time in the northern and central parts of Florida. Southern Florida, however, rarely encounters freezing temperatures."

  val t3 = "The average annual temperature of the sea is 24 °C (75 °F), ranging from 21 °C (70 °F) in February and March to 28 °C (82 °F) in August."

  val t4 = "The average annual temperature of the sea is 75 °F, ranging from 70 °F in February and March to 82 °F in August."
  "result" should "contain all temperature mentions" in {
    val r1 = dp.parseQuery(t1)

    r1.range.contains((32.0,34.0)) should be (right = true)
    r1.range.contains((4.0,7.0)) should be (right = true)
    r1.range.size should be (2)

    val r2 = dp.parseQuery(t2)
    r2.range.contains((-1.0,4.0)) should be (right = true)
    r2.range.contains((-12.0,-7.0)) should be (right = true)
    r2.range.size should be (2)


    val r3 = dp.parseQuery(t3)
    r3.temperature.contains(24.0f) should be (right = true)
    r3.temperature.contains(21.0f) should be (right = true)
    r3.temperature.contains(28.0f) should be (right = true)
    r3.temperature.size should be (3)


    val r4 = dp.parseQuery(t4)
    val rounded = r4.temperature.map(_.round)
    rounded.contains(24.0f) should be (right = true)
    rounded.contains(21.0f) should be (right = true)
    rounded.contains(28.0f) should be (right = true)
    r3.temperature.size should be (3)
  }

}
