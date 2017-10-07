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

    // http://www.blurb.de/make/pdf_to_book/booksize_calculator#book-attributes
    // margin top, bottom, left, outer >18, inner margin > 45
    "blurb-squared-klein" -> Format(486, 477, 9, 9, 9, 0),
    "blurb-standard-portrait" -> Format(576, 720, 9, 9, 9, 0),
    "blurb-standard-landscape" -> Format(684, 576, 9, 9, 9, 0),
    "blurb-large-landscape" -> Format(900,765, 9, 9, 9, 0),
    "blurb-large-squared" -> Format(846, 846, 9, 9, 9, 0), // inner margin 45

    "blurb-standard-landscape-hardcover-cover" -> Format(1439, 604, 22, 22, 22, 22)

  )

  def fromCM(width: Float, height: Float): Format = {
    Format(Util.cmToPt(width), Util.cmToPt(height))
  }
}

case class Format(width: Float, height: Float, trimTop: Float = 0, trimBottom: Float = 0, trimOuter: Float = 0, trimInner: Float = 0) {
  def totalWidth() = width + trimOuter + trimInner

  def totalHeight() = height + trimTop + trimBottom
}
