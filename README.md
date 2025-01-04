<!-- ABOUT THE PROJECT -->
## About The Project


Twitter Stream Processing Project uses technologies like Apache Spark, Kafka, Elasticsearch, and Kibana to process and analyze real-time tweet streams. Using Structured Streaming in Spark, the code ingests tweets from Kafka, applies a sentiment analysis NLP model, and indexes insights (sentiment scores) into Elasticsearch, which are then visualized in Kibana dashboards, including a heatmap of geotagged sentiments.



### Built With

• Apache Spark <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQk8KLrz1OYfFRDTNayIdRwdanvCy0_Jk8ajg&s" alt="Icon 1" width="42" height="32"> <br/>
• Apache Kafka <img src="https://miro.medium.com/v2/resize:fit:1400/1*5V1PnKn68SvmEpXYI-3CPw.png" width="52" height="32"> <br />
• Elasticsearch <img src="https://www.websolutions.cy/technologies/elasticsearch/logo.svg" width="32" height="32">



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
