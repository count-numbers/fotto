package com.github.count_numbers.fotto

import com.itextpdf.kernel.color.{Color, DeviceRgb}

/**
  * Created by simfischer on 1/4/17.
  */
object PdfUtil {

  def colorFromHex(hex: String): Color = {
    new DeviceRgb(
      Integer.valueOf(hex.substring( 1, 3 ), 16 ),
      Integer.valueOf(hex.substring( 3, 5 ), 16 ),
      Integer.valueOf(hex.substring( 5, 7 ), 16 ))
  }

}
