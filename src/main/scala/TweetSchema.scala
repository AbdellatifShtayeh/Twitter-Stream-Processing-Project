import org.apache.spark.sql.types._

object TweetSchema {
  val schema = new StructType()
    .add("id", StringType)
    .add("text", StringType)
    .add("user", new StructType()
      .add("name", StringType)
      .add("location", StringType)
    )
    .add("created_at", StringType)
    .add("coordinates", new StructType()
      .add("coordinates", ArrayType(DoubleType))
    )
    .add("hashtags", ArrayType(StringType))
    .add("cleaned_text", StringType)
    .add("sentiment_label", StringType)
    .add("sentiment_score", DoubleType)
}
