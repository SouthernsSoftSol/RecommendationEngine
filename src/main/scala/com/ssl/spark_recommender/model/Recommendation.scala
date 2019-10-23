package com.ssl.spark_recommender.model

case class Rec(pid: Int, r: Double)
case class ProductRecommendation(id: Int, recs: Seq[Rec])
case class UserRecommendation(id: Int, recs: Seq[Rec])