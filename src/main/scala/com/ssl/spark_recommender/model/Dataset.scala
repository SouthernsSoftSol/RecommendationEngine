package com.ssl.spark_recommender.model

import java.sql.Timestamp


case class AmazonReview(Author: String, Content: String, Date: String, Overall: String, ReviewID: String, Title: String)

case class AmazonProductInfo(Features: String, ImgURL: String, Name: String, Price: String, ProductID: String)

case class AmazonProductReviews(ProductInfo: AmazonProductInfo, Reviews: Seq[AmazonReview])

/**
  * Created by Brahma.
  */
case class Product(id: Int, extId: String, name: String, price: String, features: String, imgUrl: String)

/*
  * Created by Brahma.
 */
case class User(id: Int, extId: String)

/**
  * Created by Brahma.
  */
case class Review(reviewId: String, userId: Int, productId: Int, val title: String, overall: Option[Double], content: String, date: Timestamp)


case class ProductReview(product: Product, users: Array[User] = Array.empty, reviews: Array[Review] = Array.empty)
