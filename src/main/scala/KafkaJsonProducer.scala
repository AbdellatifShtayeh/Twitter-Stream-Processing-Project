import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import scala.io.Source
import java.util.Properties

object KafkaJsonProducer {
  def main(args: Array[String]): Unit = {
    val filePath = "data/boulder_flood_geolocated_tweets.json"

    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](props)
    val source = Source.fromFile(filePath)

    try {
      println(s"Starting to send data to topic raw-tweets")

      for (line <- source.getLines()) {
        println(s"Sending message: $line") // Log each line being sent
        val record = new ProducerRecord[String, String]("raw-tweets", null, line)
        producer.send(record)

        // Delay of 1 second between sending each message
        Thread.sleep(1000)
      }

      println(s"Finished sending data to topic raw-tweets")
    } catch {
      case e: Exception =>
        println(s"An error occurred: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}
