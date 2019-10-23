package com.ssl.spark_recommender

import com.ssl.spark_recommender.recommender.RecommenderController
import com.ssl.spark_recommender.utils.{ESConfig, Logging, MongoConfig}
import scopt.OptionParser

/**
  * Created by Brahma.
  */
object RecommenderServerApp extends App with Logging {
  override def main(args: Array[String]) {
    val defaultParams = scala.collection.mutable.Map[String, Any]()
    defaultParams += "server.port" -> "8080"
    defaultParams += "mongo.uri" -> "mongodb://127.0.0.1:27017/spark_recommender"
    defaultParams += "mongo.db" -> "spark_recommender"
    defaultParams += "es.httpHosts" -> "127.0.0.1:9200"
    defaultParams += "es.transportHosts" -> "127.0.0.1:9300"
    defaultParams += "es.index" -> "spark_recommender"
    defaultParams += "server.port" -> "8080"

    val parser = new OptionParser[scala.collection.mutable.Map[String, Any]]("RecommenderServerApp") {
      head("Recommendation System Server")
      opt[String]("server.port")
        .text("HTTP server port")
        .action((x, c) => {
          c += "server.port" -> x
        })
      opt[String]("mongo.uri")
        .text("Mongo Hosts")
        .action((x, c) => {
          c += "mongo.uri" -> x
        })
      opt[String]("mongo.db")
        .text("Mongo Database")
        .action((x, c) => {
          c += "mongo.db" -> x
        })
      opt[String]("es.httpHosts")
        .text("ElasicSearch HTTP Hosts")
        .action((x, c) => {
          c += "es.httpHosts" -> x
        })
      opt[String]("es.transportHosts")
        .text("ElasicSearch Transport Hosts")
        .action((x, c) => {
          c += "es.transportHosts" -> x
        })
      opt[String]("es.index")
        .text("ElasicSearch index")
        .action((x, c) => {
          c += "es.index" -> x
        })
      help("help") text ("prints this usage text")
    }
    parser.parse(args, defaultParams).map { params =>
      run(params.toMap)
    } getOrElse {
      System.exit(1)
    }
  }

  private def run(params: Map[String, Any]): Unit = {
    val serverPort = params("server.port").asInstanceOf[String].toInt

    implicit val mongoConf = new MongoConfig(params("mongo.uri").asInstanceOf[String], params("mongo.db").asInstanceOf[String])
    implicit val esConf = new ESConfig(params("es.httpHosts").asInstanceOf[String], params("es.transportHosts").asInstanceOf[String], params("es.index").asInstanceOf[String])

    try {
      RecommenderController.run(serverPort)
    }
    catch {
      case e: Exception =>
        logger.error("Error executing RecommenderServerApp", e)
        sys.exit(1)
    }
  }
}
