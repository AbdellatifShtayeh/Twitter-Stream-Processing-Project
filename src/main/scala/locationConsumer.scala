import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import scalaj.http.Http
import org.apache.spark.sql.types._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import TweetSchema._

object loactionConsumer {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("FinalConsumer")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // fetching the coordinates from the location using nominatim
    def getCoordinates(location: String): Option[Array[Double]] = {
      try {
        if (location.isEmpty) return None
        val response = Http("https://nominatim.openstreetmap.org/search")
          .param("q", location)
          .param("format", "json")
          .asString

        implicit val formats: DefaultFormats.type = DefaultFormats
        val json = parse(response.body)

        json match {
          case JArray(arr) if arr.nonEmpty =>
            val firstResult = arr.head
            for {
              lat <- (firstResult \ "lat").extractOpt[String]
              lon <- (firstResult \ "lon").extractOpt[String]
            } yield Array(lat.toDouble, lon.toDouble)
          case _ =>
            None
        }
      } catch {
        case e: Exception =>
          println(s"Failed to get coordinates for location: $location, error: ${e.getMessage}")
          None
      }
    }

    // filling the coordinates status
    val fetchCoordinatesUDF = udf((location: String, existingCoordinates: Seq[Double]) => {
      if (location.isEmpty || (existingCoordinates.nonEmpty && existingCoordinates != Seq(0.0, 0.0))) {
        (existingCoordinates.toArray, "Not Updated")
      } else {
        getCoordinates(location) match {
          case Some(coords) => (coords, "Updated")
          case None => (Array(0.0, 0.0), "Not Updated")
        }
      }
    })

    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "sentiment-tweets")
      .option("startingOffsets", "latest")
      .load()

    // parse record as json and apply schema
    val finalTweets = kafkaDF.selectExpr("CAST(value AS STRING) as jsonData")
      .select(from_json($"jsonData", schema).as("data"))
      .select("data.*")

    val enrichedTweets = finalTweets
      .withColumn(
        "fetch_result",
        fetchCoordinatesUDF(coalesce(col("user.location"), lit("")).cast(StringType), coalesce(col("coordinates.coordinates"), array(lit(0.0), lit(0.0)))) // Pass existing coordinates
      )
      .withColumn("coordinates", col("fetch_result").getField("_1")) // Extract coordinates
      .withColumn("coordinates_status", col("fetch_result").getField("_2")) // Extract update status
      .drop("fetch_result") // Remove the intermediate column

    val consoleQuery = enrichedTweets.select(
        "id",
        "text",
        "coordinates",
        "coordinates_status", // Status of update
        "user.location",
        "sentiment_label",
        "sentiment_score"
      ).writeStream
      .outputMode("append")
      .format("console")
      .option("truncate", "false")
      .start()

    consoleQuery.awaitTermination()
  }
}