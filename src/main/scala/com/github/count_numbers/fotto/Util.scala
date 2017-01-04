package com.github.count_numbers.fotto

/**
  * Created by simfischer on 12/26/16.
  */
object Util {

  def parseUnit(unit: String): Float = {
    val p = "([\\d\\.]+)(cm|in|pt)".r

    unit match {
      case p(number, "cm")  => number.toFloat * 72 * 0.393701f
      case p(number, "in")  => number.toFloat * 72
      case p(number, "pt")  => number.toFloat
    }
  }

  def cmToPt(x: Float) = x * 72 * 0.393701f

}
