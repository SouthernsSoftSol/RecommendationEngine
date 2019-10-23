package com.ssl.spark_recommender.utils

/**
  * Created by Brahma.
  */
object HashUtils {
  def hash(string: String): Int = {
    string.hashCode  & 0xfffffff //Make it positive despite of collision
  }
}
