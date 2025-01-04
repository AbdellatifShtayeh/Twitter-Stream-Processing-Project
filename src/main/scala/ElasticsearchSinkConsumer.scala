import scala.collection.mutable.ListBuffer
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.serialization.StringDeserializer
import org.elasticsearch.client.{Request, RestClient, RestClientBuilder}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write
import java.util.Properties
import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import javax.net.ssl.{SSLContext, TrustManager, X509TrustManager}
import org.apache.http.conn.ssl.NoopHostnameVerifier
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat

object ElasticsearchSinkConsumer {
  def main(args: Array[String]): Unit = {
    //------------------------------------------------------main env variables--------------------------------------------------------
    val kafkaBootstrapServers = "localhost:9092"
    val kafkaGroupId = "scala-consumer-group"
    val kafkaTopic = "sentiment-tweets"
    val esHost = "localhost"
    val esPort = 9200
    val esUsername = "elastic"
    val esPassword = "2lfTYQH*6cq-vpohe3y5"

    // ------------------------------------------------Kafka consumer-------------------------------------------------------------
    val consumerProps = new Properties()
    consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers)
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId)
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    consumerProps.put("key.deserializer", classOf[StringDeserializer].getName)
    consumerProps.put("value.deserializer", classOf[StringDeserializer].getName)

    val consumer = new KafkaConsumer[String, String](consumerProps)
    consumer.subscribe(java.util.Arrays.asList(kafkaTopic))

    // --------------------------------------------- Elasticsearch client------------------------------------------------------
    val credentialsProvider = new BasicCredentialsProvider()
    credentialsProvider.setCredentials(
      AuthScope.ANY,
      new UsernamePasswordCredentials(esUsername, esPassword)
    )
//---------------------------------------------certs confg for the team (stackoverflow refrence)-------------------------------
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, Array[TrustManager](new X509TrustManager {
      def getAcceptedIssuers = null
      def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = {}
      def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = {}
    }), new java.security.SecureRandom())

    val client: RestClient = RestClient.builder(new HttpHost(esHost, esPort, "https"))
      .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
        override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
          httpClientBuilder.setSSLContext(sslContext)
            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            .setDefaultCredentialsProvider(credentialsProvider)
        }
      })
      .build()
//------------------------------------------------------------stram process --------------------------------------------------
    val indexName = "tweets"
    val bulkSize = 500 // after examination i noticed this is the number of each batch approx
    val bulkBuffer = ListBuffer[String]()

    try {
      while (true) {
        val records = consumer.poll(java.time.Duration.ofMillis(1000)).iterator()
        while (records.hasNext) {
          val record = records.next()
          val message = record.value()
          try {
            implicit val formats: DefaultFormats.type = DefaultFormats


            val parsedData = parse(message)
            //----------------------------------------Extract Data----------------------------------------------------------
            val id = (parsedData \ "id").extract[String]
            val text = (parsedData \ "text").extract[String]

            // Handle user as an object
            val user = (parsedData \ "user").extract[Map[String, JValue]]
            val userName = user.get("name") match {
              case Some(JString(name)) => name
              case _ => "Unknown"
            }
            val userLocation = user.get("location") match {
              case Some(JString(location)) => location
              case _ => "Unknown"
            }

            val createdAtStr = (parsedData \ "created_at").extract[String]

            // Correct extraction of coordinates field (nested inside "coordinates")
            val coordinatesArray = (parsedData \ "coordinates"\ "coordinates").extract[List[Double]]
            val coordinates = if (coordinatesArray.length == 2) {
              Map(
                "lat" -> coordinatesArray(1),  // latitude
                "lon" -> coordinatesArray(0)   // longitude
              )
            } else {
              Map("lat" -> 0.0, "lon" -> 0.0)  // Default value if coordinates are not correct
            }

            val hashtags = (parsedData \ "hashtags").extract[List[String]]
            val cleanedText = (parsedData \ "cleaned_text").extract[String]
            val sentimentLabel = (parsedData \ "sentiment_label").extract[String]
            val sentimentScore = (parsedData \ "sentiment_score").extract[Double]

            // Parse and format the timestamp
            val dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy")
            val createdAtTimestamp = try {
              dateFormat.parse(createdAtStr).getTime
            } catch {
              case _: Exception => 0L
            }


            //---------------------------match schema with elastic mapping-----------------------------------------------------
            val enrichedTweet = Map(
              "id" -> id,
              "text" -> text,
              "user" -> Map(
                "name" -> userName,
                "location" -> userLocation
              ),
              "hashtags" -> hashtags,
              "coordinates" -> coordinates,
              "cleaned_text" -> cleanedText,
              "sentiment_label" -> sentimentLabel,
              "sentiment_score" -> sentimentScore,
              "created_at" -> createdAtTimestamp,

            )

            // ------------------------Serialize to JSON and append to bulk buffer----------------------------------
            val enrichedTweetJson = write(enrichedTweet)
            bulkBuffer.append(s"""{ "index": { "_index": "$indexName", "_id": "$id" } }""")
            bulkBuffer.append(enrichedTweetJson)


            if (bulkBuffer.size >= bulkSize * 2) { //buffer full ?
              sendBulkRequest(client, bulkBuffer)
              bulkBuffer.clear()
            }
          } catch {
            case e: Exception =>
              println(s"Error processing message: ${e.getMessage}")
              e.printStackTrace()
          }
        }

        //send
        if (bulkBuffer.nonEmpty) {
          sendBulkRequest(client, bulkBuffer)
          bulkBuffer.clear()
        }
      }
    } finally {
      consumer.close()
      client.close()
      println("Kafka Consumer and Elasticsearch Client closed.")
    }
  }

  //-------------------------------- Helper method to send bulk request--------------------------------------------------
  def sendBulkRequest(client: RestClient, bulkBuffer: ListBuffer[String]): Unit = {
    try {
      val bulkPayload = bulkBuffer.mkString("\n") + "\n"
      val request = new Request("POST", "/_bulk")
      request.setJsonEntity(bulkPayload)
      val response = client.performRequest(request)
      println(s"Bulk request completed with status: ${response.getStatusLine.getStatusCode}")
    } catch {
      case e: Exception =>
        println(s"Error sending bulk request: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}
