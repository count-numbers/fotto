package com.github.count_numbers.fotto

/**
  * Created by simfischer on 1/6/17.
  */
case class AspectRatioHelper(val format: Format, val margins: Margins) {

  def constrainedHeight(width: Float, aspectRatio: Float, withMargins: Boolean): Float = {
    val referenceWidth = format.width - ((if (withMargins) (margins.inner + margins.outer) else 0))
    val referenceHeight = format.height- ((if (withMargins) (margins.top + margins.bottom) else 0))
    val desiredWidth = width * referenceWidth
    val desiredHeight = desiredWidth / aspectRatio
    val relativeHeight = desiredHeight / referenceHeight
    relativeHeight
  }

  def constrainedWidth(height: Float, aspectRatio: Float, withMargins: Boolean): Float = {
    val referenceWidth = format.width - ((if (withMargins) (margins.inner + margins.outer) else 0))
    val referenceHeight = format.height- ((if (withMargins) (margins.top + margins.bottom) else 0))
    val desiredHeight = height * referenceHeight
    val desiredWidth= desiredHeight * aspectRatio
    val relativeWidth = desiredWidth / referenceWidth
    relativeWidth
  }

}
