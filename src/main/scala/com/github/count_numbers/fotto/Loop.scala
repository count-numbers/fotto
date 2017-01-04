package com.github.count_numbers.fotto

import java.io.File

import com.typesafe.scalalogging.Logger

import scala.util.Try
import scala.util.control.NonFatal

/**
  * Created by simfischer on 12/27/16.
  */
object Loop {

  var logger = Logger("fotto")

  def apply(files: Seq[File], f: Unit => Unit) = {

    var lastModified: Long = files.map(_.lastModified()).max

    while (true) {
      try {
        f()
      } catch {
        case e: Exception => logger.error("No luck. Try again.", e)
      }

      val lastProcessedTimestamp = lastModified
      logger.info(s"Waiting for changes in ${files}.")
      do {
        Thread.sleep(1000)
        lastModified = files.map(_.lastModified()).max
      } while (lastModified <= lastProcessedTimestamp)
      logger.info(s"Change detected.")
    }
  }
}
