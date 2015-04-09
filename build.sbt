
mainClass in assembly := Some("tools.DBpediaLocationAnnotator")


name := """Destination-Recomender-System"""

version := "0.1"

scalaVersion := "2.11.6"

scalacOptions ++= Seq("-deprecation", "-feature", "utf8")



libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "org.scalaj" %% "scalaj-http" % "1.1.4",
  "com.google.code.gson" % "gson" % "2.3.1",
  "org.apache.jena" % "jena-core" % "2.13.0",
  "org.apache.jena" % "jena-arq" % "2.13.0",
  "net.java.dev.textile-j" % "textile-j" % "2.2.864",
  "io.spray" %% "spray-can" % "1.3.3",
  "io.spray" %%  "spray-routing" % "1.3.3",
  "io.spray" %%  "spray-json"  % "1.3.1",
  "net.liftweb" % "lift-json_2.10" % "2.6.2"
)

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
{
  case "org/apache/commons/logging/Log.class" => MergeStrategy.first
  case "org/apache/commons/logging/LogConfigurationException.class" => MergeStrategy.first
  case "org/apache/commons/logging/LogFactory.class" => MergeStrategy.first
  case "org/apache/commons/logging/impl/NoOpLog.class" => MergeStrategy.first
  case "org/apache/commons/logging/impl/SimpleLog$1.class" => MergeStrategy.first
  case "org/apache/commons/logging/impl/SimpleLog.class" => MergeStrategy.first
  case x => old(x)
}
}