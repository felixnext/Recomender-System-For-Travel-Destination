package tools

import scala.math._

/**
 * Math functions.
 */
object Math {


  val minimum = (i1: Int, i2: Int, i3: Int) => min(min(i1, i2), i3)

  //Levenstein distance
  val levensteinDistance = (s1:String, s2:String) =>  {
    val dist=Array.tabulate(s2.length+1, s1.length+1){(j,i)=>if(j==0) i else if (i==0) j else 0}

    for(j<-1 to s2.length; i<-1 to s1.length)
      dist(j)(i)=if(s2(j-1)==s1(i-1)) dist(j-1)(i-1)
      else minimum(dist(j-1)(i)+1, dist(j)(i-1)+1, dist(j-1)(i-1)+1)

    dist(s2.length)(s1.length)
  }

  //normalized Levenstein distance
  val levensteinScore = (s1: String, s2: String) => 1.0 -
    (levensteinDistance(s1,s2).toDouble / max(s1.length,s2.length).toDouble)


  //radius of the earth in km
  val R = 6372.8

  //calculates the distance in km between two points, given their coordinates in latitude and longitude
  def haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double)={
    val dLat=(lat2 - lat1).toRadians
    val dLon=(lon2 - lon1).toRadians

    val a = pow(sin(dLat/2),2) + pow(sin(dLon/2),2) * cos(lat1.toRadians) * cos(lat2.toRadians)
    val c = 2 * asin(sqrt(a))
    R * c
  }

  val eps = 0.00001

  //calculates decay sum
  //all elements in the sum have different impact, the biggest one has more impact then smaller one
  def decaySum(values: Seq[Double]) = values.sortWith((v1,v2) => if(v1-v2 > eps) true else false)
    .foldLeft(0.0,0)((acc,v) => (acc._1 + v/pow(2,acc._2), acc._2 +1))._1


  //given inner and outer radius the function provides decay distance for the give distance between two point in km
  val distanceDecayFunction: Double => Double => Double => Double = inner => outer => distance => {
    if(distance - inner < eps) 1.0
    else if(outer - distance < eps) 0.0
    else {
      val lambda = Config.decaySensitivity
      val v = log(lambda) * (distance - inner)
      pow(E,v)
    }
  }

  //normalize score so that the sum is equal 1.0 thus each score is smaller then 1.0
  val normalizedScore: Iterable[Double] => Double => Double = scores => score => {
    val aggregated = scores.sum
    score/aggregated
  }

  val fahrenheitToCelsiusConverter: Float => Float = f =>  (f - 32.0f) / 1.8f

  val std: Vector[Double] => Double => Double = values => mean =>
    sqrt(values.map(v => pow(v - mean, 2)).sum/values.size.toDouble)
}
