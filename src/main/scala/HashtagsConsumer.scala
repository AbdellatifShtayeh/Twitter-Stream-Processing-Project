import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import TweetSchema._


object HashtagsConsumer {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("HashtagExtractorConsumer")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "cleaned-tweets")
      .option("failOnDataLoss", "false")
      .option("startingOffsets", "latest")
      .load()

    val tweets = kafkaDF.selectExpr("CAST(value AS STRING) as jsonData")
      .select(from_json($"jsonData", schema).as("data"))
      .select("data.*")

    val debugDF = tweets.select("id", "text")
    debugDF.writeStream
      .outputMode("append")
      .format("console")
      .option("truncate", "false")
      .start()

    val hashtagExtractor = udf((text: String) => {
      if (text != null) {
        val hashtagPattern = """#(\w+)""".r
        hashtagPattern.findAllMatchIn(text).map(_.group(1).toLowerCase).toArray
      } else {
        Array.empty[String]
      }
    })

    //added the hashtags column directly from the original text column

    val finalTweets = tweets.withColumn("hashtags", hashtagExtractor($"text"))

    finalTweets.writeStream
      .outputMode("append")
      .format("console")
      .option("truncate", "false")
      .start()

    val kafkaQuery = finalTweets.selectExpr("to_json(struct(*)) AS value")
      .writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("topic", "hash-tweets")
      .option("checkpointLocation", "/tmp/hashtag_extractor_checkpoint")
      .start()

    kafkaQuery.awaitTermination()
  }
}
