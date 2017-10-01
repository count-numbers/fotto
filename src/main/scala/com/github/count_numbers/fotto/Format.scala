package com.github.count_numbers.fotto

object Format {
  val namedFormats: Map[String, Format] = Map(
    "cewe-mini" -> fromCM(11, 14),
    "cewe-klein" -> fromCM(14, 13),
    "cewe-compact-panorama" -> fromCM(19, 15),
    "cewe-quadratisch" -> fromCM(21, 21),
    "cewe-gross" -> fromCM(21, 28),
    "cewe-gross-panorama" -> fromCM(28, 21),
    "cewe-xl" -> fromCM(30, 30),
    "cewe-xxl" -> fromCM(28, 36),
    "cewe-xxl-panorama" -> fromCM(38, 29),

    "blurb-quadratisch-klein" -> Format(495, 495, 9, 9, 9, 27)
  )

  def fromCM(width: Float, height: Float): Format = {
    Format(Util.cmToPt(width), Util.cmToPt(height))
  }
}

case class Format(width: Float, height: Float, trimTop: Float = 0, trimBottom: Float = 0, trimOuter: Float = 0, trimInner: Float = 0) {
  def totalWidth() = width + trimOuter + trimInner

  def totalHeight() = height + trimTop + trimBottom
}
