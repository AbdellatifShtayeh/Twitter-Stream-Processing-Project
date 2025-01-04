ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.15"

libraryDependencies ++= Seq(
  // Apache Spark Structured Streaming
  "org.apache.spark" %% "spark-core" % "3.5.0",
  "org.apache.spark" %% "spark-sql" % "3.5.0",
  "org.apache.spark" %% "spark-streaming" % "3.5.0",
  "org.apache.kafka" % "kafka-clients" % "3.5.1",
  // Kafka integration for Structured Streaming
  "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.5.0",
  "com.johnsnowlabs.nlp" %% "spark-nlp" % "4.4.2",
  "org.apache.spark" %% "spark-mllib" % "3.5.3",
  "org.apache.httpcomponents" % "httpclient" % "4.5.13"
)

lazy val root = (project in file("."))
  .settings(
    name := "Twitter Stream Processing Project"
  )