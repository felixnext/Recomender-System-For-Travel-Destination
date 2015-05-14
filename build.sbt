

name := """Destination-Recomender-System"""

version := "0.1"

scalaVersion := "2.10.5"

scalacOptions ++= Seq("-deprecation", "-feature", "utf8")

//custom options for high memory usage

javaOptions += "-Xmx6G"

javaOptions += "-XX:+UseConcMarkSweepGC"

fork in run := true

fork in Test := true

mainClass in assembly := Some("tools.ParallelTfIdfCalculator")

//dependencies

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.3.1",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "org.scalaj" %% "scalaj-http" % "1.1.4",
  "io.spray" %% "spray-can" % "1.3.3",
  "io.spray" %%  "spray-routing" % "1.3.3",
  "io.spray" %%  "spray-json"  % "1.3.1",
  "com.google.code.gson" % "gson" % "2.3.1",
  "org.apache.jena" % "jena-core" % "2.13.0",
  "org.apache.jena" % "jena-arq" % "2.13.0",
  "net.java.dev.textile-j" % "textile-j" % "2.2.864",
  "edu.washington.cs.knowitall.openie" % "openie_2.10" % "4.1.3",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.5.1" artifacts (Artifact("stanford-corenlp", "models"), Artifact("stanford-corenlp")),
  "edu.mit" % "jwi" % "2.2.3"
)


//resolve assembly conflicts

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
{
  case "log4j.properties" =>  MergeStrategy.first
  case "logback.xml" => MergeStrategy.first
  case PathList("nu", "xom", xs @ _*)  => MergeStrategy.first
  case PathList("java_cup", "runtime", xs @ _*)  => MergeStrategy.first
  case PathList("akka", xs @ _*)  => MergeStrategy.first
  case PathList("com", xs @ _*)  => MergeStrategy.first
  case PathList("org", xs @ _*)  => MergeStrategy.first
  case PathList("javax", xs @ _*)  => MergeStrategy.first
  case x => old(x)
}
}