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
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "org.apache.httpcomponents" % "httpclient" % "4.4",
  "org.scalaj" %% "scalaj-http" % "1.1.4",
  "com.google.code.gson" % "gson" % "2.3.1",
  "org.apache.jena" % "jena-core" % "2.13.0",
  "org.apache.jena" % "jena-arq" % "2.13.0",
  "com.github.scala-incubator.io" % "scala-io-core_2.9.1" % "0.4.1-seq"
)
