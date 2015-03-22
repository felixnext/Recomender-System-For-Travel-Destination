import AssemblyKeys._

mainClass in assembly := Some("de.tu_bs.cs.ifis.wille.nyt.crawling.ContentNYTCrawler")


name := """Destination Recomender System"""

version := "0.1"


scalaVersion := "2.11.1"

javacOptions += "-Xmx6G"

javaOptions += "-Xmx6G"

scalacOptions ++= Seq("-deprecation", "-feature")



libraryDependencies ++= Seq(
	"org.apache.spark" % "spark-core_2.10" % "1.3.0"
)
