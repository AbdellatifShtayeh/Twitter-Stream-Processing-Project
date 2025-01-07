<!-- ABOUT THE PROJECT -->
## About The Project


Twitter Stream Processing Project uses technologies like Apache Spark, Kafka, Elasticsearch, and Kibana to process and analyze real-time tweet streams. Using Structured Streaming in Spark, the code ingests tweets from Kafka, applies a sentiment analysis NLP model, and indexes insights (sentiment scores) into Elasticsearch, which are then visualized in Kibana dashboards, including a heatmap of geotagged sentiments.

### Work Flow

 <img src="https://i.imgur.com/068VJzf.jpeg" alt="Icon 1" width="542" height="243"> <br/>

### Go to Trello

 https://trello.com/invite/b/6776a49e940b66f371028dc8/ATTId5d7b71850abfee8cb05accf6a5566f06ED4A0F9/big-data-project

 
### Directory Structure

```sh
📄 README.md
📦 build.sbt
📦 export.ndjson
📁 project
│   ├── 📄 build.properties
│   └── 📁 target
📁 src
│   ├── 📁 main
│   │   └── 📁 scala
│   │       ├── 📄 ElasticsearchSinkConsumer.scala
│   │       ├── 📄 ExtractFieldsConsumer.scala
│   │       ├── 📄 HashtagsConsumer.scala
│   │       ├── 📄 KafkaJsonProducer.scala
│   │       ├── 📄 SentimentAnalysisConsumer.scala
│   │       ├── 📄 TweetSchema.scala
│   │       └── 📄 locationConsumer.scala
│   └── 📁 test
│       └── 📁 scala
📁 target
 ```


### Built With

• Apache Spark <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQk8KLrz1OYfFRDTNayIdRwdanvCy0_Jk8ajg&s" alt="Icon 1" width="42" height="32"> <br/>
• Apache Kafka <img src="https://miro.medium.com/v2/resize:fit:1400/1*5V1PnKn68SvmEpXYI-3CPw.png" width="52" height="32"> <br />
• Elasticsearch <img src="https://www.websolutions.cy/technologies/elasticsearch/logo.svg" width="32" height="32"> <br />
• Kibana <img src="https://cdn.prod.website-files.com/61bcfa6e82d1a9800fdaddf3/646e6263710e07588ee6c1c2_elastic-kibana-logo-vector.svg" width="32" height="32">



<!-- GETTING STARTED -->
## Getting Started


### Prerequisites

You should run these prerequisites on your local machine

* start Zookeeper
  ```sh
  bin/zookeeper-server-start.sh config/zookeeper.properties
  ```
* start Kafka
  ```sh
  bin/kafka-server-start.sh config/server.properties

  ```
* Create these kafka topics
  ```sh
  bin/kafka-topics.sh --create --topic raw-tweets --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

  ```
  ```sh
  bin/kafka-topics.sh --create --topic cleaned-tweets --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

  ```
  ```sh
  bin/kafka-topics.sh --create --topic hash-tweets --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

  ```
   ```sh
  bin/kafka-topics.sh --create --topic sentiment-tweets --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

  ```
* start elasticsearch
  * for MacOS

  ```sh
  bin/elasticsearch

  ```
  * for Windows

  ```sh
  bin\elasticsearch.bat 

  ```
* start Kibana

  ```sh
  ./bin/kibana

  ```
  ### File Modifications
* KafkaJsonProducer line 7:
  ```sh
  val filePath = "replace/with/your/path.json";

  ```

* ElasticSinkConsumer: line 26-28
   (no need for certifications because the code creates an issuer that accepts all certificates)
```sh
   val esUsername = "replace with your username" 
    val esPassword = "replace with your password"

  ```
* Go to localhost 5601:
  - navigate to menu icon
  - scroll down to management & press dev tools
  - in dev tools add this code and run it
```sh  
   PUT /tweets
 {
  "mappings": {
    "properties": {
      "id": {
        "type": "keyword" 
      },
      "text": {
        "type": "text",
        "analyzer": "standard", 
        "fields": {
          "raw": {
            "type": "keyword" 
          }
        }
      },
      "user": {
        "properties": {
          "name": {
            "type": "text",
            "analyzer": "standard",
            "fields": {
              "raw": {
                "type": "keyword"
              }
            }
          },
          "location": {
            "type": "text",
            "analyzer": "standard", 
            "fields": {
              "raw": {
                "type": "keyword"
              }
            }
          }
        }
      },
      "created_at": {
        "type": "date",
        "format": "strict_date_optional_time||epoch_millis" 
      },
      "coordinates": {
        "type": "geo_point" 
      },
      "hashtags": {
        "type": "keyword" 
      },
      "cleaned_text": {
        "type": "text",
        "analyzer": "english", 
        "fields": {
          "raw": {
            "type": "keyword" 
          }
        }
      },
      "sentiment_label": {
        "type": "keyword" 
      },
      "sentiment_score": {
        "type": "float" 
      }
    }
  }
  }
 ```
  - Navigate to menu icon again
  - press on management
  - scroll down to Kibana -> saved objects
  - on the top bar click on import icon & import this file (export.ndjson)
  - Dashboard should be created

      ### Dashboard Demo
    
  <img src="https://i.imgur.com/T1IdFtC.png" alt="Icon 1" width="542" height="201"> <br/>
  <img src="https://i.imgur.com/jkYmoaI.png" alt="Icon 1" width="542" height="313"> <br/>
  <img src="https://i.imgur.com/UhOItNQ.png" alt="Icon 1" width="542" height="367"> <br/>




