package com.ssl.spark_recommender.utils

import org.slf4j.LoggerFactory

/**
  * Created by Brahma.
  */
trait Logging {
  @transient
  protected val logger = LoggerFactory.getLogger(getClass().getName())
}
