ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.15"

lazy val root = (project in file("."))
  .settings(
    name := "structured streaming"
  )
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
  "org.apache.httpcomponents" % "httpclient" % "4.5.13",
  "org.elasticsearch" %% "elasticsearch-spark-30" % "8.10.2",
  "org.elasticsearch.client" % "elasticsearch-rest-client" % "7.17.10",
  "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "7.17.10",
  "org.apache.httpcomponents" % "httpclient" % "4.5.13",
 "co.elastic.clients" % "elasticsearch-java" % "8.10.2",
  "org.scalaj" %% "scalaj-http" % "2.4.0"

)
