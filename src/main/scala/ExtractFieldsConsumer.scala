import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import TweetSchema._


object ExtractFieldsConsumer {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("ExtractFieldsConsumer")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "raw-tweets")
      .option("failOnDataLoss", "false")
      .option("startingOffsets", "latest")
      .load()

    val tweets = kafkaDF.selectExpr("CAST(value AS STRING) as jsonData")
      .select(from_json($"jsonData", schema).as("data"))
      .select("data.*")

    val kafkaQuery = tweets.selectExpr("to_json(struct(*)) AS value")
      .writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("topic", "cleaned-tweets")
      .option("checkpointLocation", "/tmp/extract_fields_checkpoint")
      .start()

    val consoleQuery = tweets.writeStream
      .outputMode("append")
      .format("console")
      .option("truncate", "false")
      .start()


    consoleQuery.awaitTermination()
    kafkaQuery.awaitTermination()
  }
}
