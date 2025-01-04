import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import com.johnsnowlabs.nlp.pretrained.PretrainedPipeline
import TweetSchema._

object SentimentAnalysisConsumer {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("SentimentAnalysisConsumer")
      .master("local[*]")
      .config("spark.jars.packages", "com.johnsnowlabs.nlp:spark-nlp-spark32_2.12:4.4.2")
      .getOrCreate()

    import spark.implicits._
    val pipeline = PretrainedPipeline("analyze_sentiment", lang = "en")

    //  preprocessing function
    def preprocessing(tweetText: String): String = {
      val lowercased = tweetText.toLowerCase()
      val withoutRt = lowercased.replaceAll("^rt\\s+", "").trim
      val withoutUrl = "https?://\\S+".r.replaceAllIn(withoutRt, "")
      val withoutMentions = "@\\w+".r.replaceAllIn(withoutUrl, "")
      val withoutSpecialChar = "[^a-zA-Z0-9\\s#]".r.replaceAllIn(withoutMentions, "")
      val tokens = withoutSpecialChar.split("\\s+")
      val stopWords = Set("the", "and", "is", "in", "at", "for")
      val filteredTokens = tokens.filterNot(stopWords.contains)
      filteredTokens.mkString(" ").trim
    }

    val preprocessingUDF = udf((text: String) => preprocessing(text))

    val analyzeSentimentUDF = udf((cleanedTweet: String) => {
      if (cleanedTweet == null || cleanedTweet.isEmpty) {
        ("neutral", 0.5)
      } else {
        try {
          val result = pipeline.annotate(cleanedTweet)
          val sentiment = result.get("sentiment").flatMap(_.headOption).getOrElse("neutral")
          val score = sentiment match {
            case "positive" => 1.0
            case "negative" => 0.0
            case "neutral" => 0.5
            case _ => 0.5
          }
          (sentiment, score)
        } catch {
          case e: Exception =>
            println(s"Error processing tweet: $cleanedTweet - ${e.getMessage}")
            ("neutral", 0.5)
        }
      }
    })

    // read tweets from  hash-tweets
    val kafkaStream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "hash-tweets")
      .load()

    // parse record as json and preprocess
    val tweets = kafkaStream.selectExpr("CAST(value AS STRING) as json")
      .select(from_json($"json", schema).as("data"))
      .select("data.*")
      .withColumn("cleaned_text", preprocessingUDF($"text")) // Apply preprocessing
      .withColumn("sentiment_data", analyzeSentimentUDF($"cleaned_text")) // Apply sentiment analysis
      .withColumn("sentiment_label", $"sentiment_data._1")
      .withColumn("sentiment_score", $"sentiment_data._2")
      .drop("sentiment_data")


    val finalTweets = tweets.selectExpr("to_json(struct(*)) AS value")

//sending to sentiment-tweets
    val kafkaQuery = finalTweets.writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("topic", "sentiment-tweets")
      .option("checkpointLocation", "/tmp/sentiment_analysis_kafka_checkpoint")
      .start()


//print
    val consoleQuery = tweets.writeStream
      .outputMode("append")
      .format("console")
      .option("truncate", "false")
      .option("checkpointLocation", "/tmp/sentiment_analysis_console_checkpoint")
      .start()

    kafkaQuery.awaitTermination()
    consoleQuery.awaitTermination()
  }
}
