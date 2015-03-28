import AssemblyKeys._

mainClass in assembly := Some("de.tu_bs.cs.ifis.wille.nyt.crawling.ContentNYTCrawler")


name := """Destination Recomender System"""

version := "0.1"


scalaVersion := "2.11.6"

javacOptions += "-Xmx6G"

javaOptions += "-Xmx6G"

scalacOptions ++= Seq("-deprecation", "-feature")



libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "org.apache.httpcomponents" % "httpclient" % "4.4",
  "org.scalaj" %% "scalaj-http" % "1.1.4",
  "com.google.code.gson" % "gson" % "2.3.1"
)
