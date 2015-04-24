package tools

import scala.math._

/**
 * Levenshtein distance.
 */
object Levenshtein {

  private val minimum = (i1: Int, i2: Int, i3: Int) => min(min(i1, i2), i3)

  val distance = (s1:String, s2:String) =>  {
    val dist=Array.tabulate(s2.length+1, s1.length+1){(j,i)=>if(j==0) i else if (i==0) j else 0}

    for(j<-1 to s2.length; i<-1 to s1.length)
      dist(j)(i)=if(s2(j-1)==s1(i-1)) dist(j-1)(i-1)
      else minimum(dist(j-1)(i)+1, dist(j)(i-1)+1, dist(j-1)(i-1)+1)

    dist(s2.length)(s1.length)
  }

  val score = (s1: String, s2: String) => 1.0 - (distance(s1,s2).toDouble / max(s1.length,s2.length).toDouble)

}
