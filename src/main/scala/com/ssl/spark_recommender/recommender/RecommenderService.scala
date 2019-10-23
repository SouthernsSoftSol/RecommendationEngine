package com.ssl.spark_recommender.recommender

import com.mongodb.casbah.Imports._
import com.ssl.spark_recommender.model._
import com.ssl.spark_recommender.parser.DatasetIngestion
import com.ssl.spark_recommender.trainer.ALSTrainer
import com.ssl.spark_recommender.utils.{ESConfig, HashUtils, MongoConfig}
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{MoreLikeThisQueryBuilder, QueryBuilders}
import org.elasticsearch.search.SearchHits

/**
  * Created by Brahma.
  */
object RecommenderService {
  private val MAX_RECOMMENDATIONS = 10
  private val CF_RATING_FACTOR = 0.8

  private def parseUserRecs(o: DBObject, maxItems: Int): List[Recommendation] = {
    o.getAs[MongoDBList]("recs").getOrElse(MongoDBList()).map { case (o: DBObject) => parseRec(o) }.toList.sortBy(x => x.rating).reverse.take(maxItems)
  }

  private def parseProductRecs(o: DBObject, maxItems: Int): List[Recommendation] = {
    o.getAs[MongoDBList]("recs").getOrElse(MongoDBList()).map { case (o: DBObject) => parseRec(o) }.toList.sortBy(x => x.rating).reverse.take(maxItems)
  }

  private def parseESResponse(response: SearchResponse): List[Recommendation] = {
    response.getHits match {
      case null => List[Recommendation]()
      case hits: SearchHits if hits.getTotalHits == 0 => List[Recommendation]()
      case hits: SearchHits if hits.getTotalHits > 0 => hits.getHits.map { hit => new Recommendation(hit.getId.toInt, hit.getScore) }.toList
    }
  }

  private def parseRec(o: DBObject): Recommendation = {
    new Recommendation(o.getAs[Int]("pid").getOrElse(0), o.getAs[Double]("r").getOrElse(0))
  }

  private def findProductCFRecs(productId: Int, maxItems: Int)(implicit mongoClient: MongoClient, mongoConf: MongoConfig): List[Recommendation] = {
    val listRecs = mongoClient(mongoConf.db)(ALSTrainer.PRODUCT_RECS_COLLECTION_NAME).findOne(MongoDBObject("id" -> productId)).getOrElse(MongoDBObject())

    return parseProductRecs(listRecs, MAX_RECOMMENDATIONS)
  }

  private def findUserCFRecs(userId: Int, maxItems: Int)(implicit mongoClient: MongoClient, mongoConf: MongoConfig): List[Recommendation] = {
    val listRecs = mongoClient(mongoConf.db)(ALSTrainer.USER_RECS_COLLECTION_NAME).findOne(MongoDBObject("id" -> userId)).getOrElse(MongoDBObject())

    return parseUserRecs(listRecs, MAX_RECOMMENDATIONS)
  }

  private def findContentBasedMoreLikeThisRecommendations(productId: Int, maxItems: Int)(implicit esClient: Client, esConf: ESConfig): List[Recommendation] = {
    val indexName = esConf.index
    val query = QueryBuilders.moreLikeThisQuery(Array("id"),
      Array("features"),
      Array(new MoreLikeThisQueryBuilder.Item(indexName, DatasetIngestion.PRODUCTS_INDEX_NAME, productId.toString)))

    return parseESResponse(esClient.prepareSearch().setQuery(query).setSize(MAX_RECOMMENDATIONS).execute().actionGet())
  }

  private def findContentBasedSearchRecommendations(text: String, maxItems: Int)(implicit esClient: Client, esConf: ESConfig): List[Recommendation] = {
    val indexName = esConf.index

    val query = QueryBuilders.multiMatchQuery(text, "name", "features")

    return parseESResponse(esClient.prepareSearch().setIndices(indexName).setTypes(DatasetIngestion.PRODUCTS_INDEX_NAME).setQuery(query).setSize(maxItems).execute().actionGet())
  }

  private def findHybridRecommendations(productId: Int, maxItems: Int, cfRatingFactor: Double)(implicit mongoClient: MongoClient, mongoConf: MongoConfig, esClient: Client, esConf: ESConfig): List[HybridRecommendation] = {
    val cbRatingFactor = 1 - cfRatingFactor
    val cfRecs = findProductCFRecs(productId, ALSTrainer.MAX_RECOMMENDATIONS)
      .map(x => new HybridRecommendation(x.productId, x.rating, x.rating * cfRatingFactor))
    val cbRecs = findContentBasedMoreLikeThisRecommendations(productId, ALSTrainer.MAX_RECOMMENDATIONS)
      .map(x => new HybridRecommendation(x.productId, x.rating, x.rating * cbRatingFactor))

    val finalRecs = cfRecs ::: cbRecs

    return finalRecs.sortBy(x => -x.hybridRating).take(maxItems)
  }


  def getCollaborativeFilteringRecommendations(request: ProductRecommendationRequest)(implicit mongoClient: MongoClient, mongoConf: MongoConfig): List[Recommendation] = {
    return findProductCFRecs(request.productId, MAX_RECOMMENDATIONS)
  }

  def getCollaborativeFilteringRecommendations(request: UserRecommendationRequest)(implicit mongoClient: MongoClient, mongoConf: MongoConfig): List[Recommendation] = {
    return findUserCFRecs(request.userId, MAX_RECOMMENDATIONS)
  }

  def getContentBasedMoreLikeThisRecommendations(request: ProductRecommendationRequest)(implicit esClient: Client, esConf: ESConfig): List[Recommendation] = {
    return findContentBasedMoreLikeThisRecommendations(request.productId, MAX_RECOMMENDATIONS)
  }

  def getContentBasedSearchRecommendations(request: SearchRecommendationRequest)(implicit esClient: Client, esConf: ESConfig): List[Recommendation] = {
    return findContentBasedSearchRecommendations(request.text, MAX_RECOMMENDATIONS)
  }

  def getHybridRecommendations(request: ProductHybridRecommendationRequest)(implicit mongoClient: MongoClient, mongoConf: MongoConfig, esClient: Client, esConf: ESConfig): List[HybridRecommendation] = {
    return findHybridRecommendations(request.productId, MAX_RECOMMENDATIONS, CF_RATING_FACTOR)
  }
}
