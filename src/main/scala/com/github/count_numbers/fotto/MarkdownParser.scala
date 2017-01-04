package com.github.count_numbers.fotto

import com.itextpdf.io.font.FontConstants
import com.itextpdf.kernel.font.{PdfFontFactory}
import com.itextpdf.layout.element.{Paragraph, Text}
import com.itextpdf.layout.property.{TextAlignment, VerticalAlignment}

/** Limited markdown parser converting a markdown string into PDF paragraphs.
  */
case class MarkdownParser(width: Float, styleOpt: Option[Style]) {

  val normalFont = PdfFontFactory.createFont(FontConstants.HELVETICA)
  val boldFont = PdfFontFactory.createFont(FontConstants.HELVETICA_BOLD)
  val italicFont = PdfFontFactory.createFont(FontConstants.HELVETICA_OBLIQUE)

  /** Create a single empty paragraph based on the given style.
    * fontFactor can be used to scale up the font size, e.g. to indicate a header paragraph. */
  private def makeParagraph(fontFactor: Float): Paragraph = {
    val par: Paragraph = new Paragraph()
      .setWidth(width)
      .setVerticalAlignment(VerticalAlignment.TOP)
      .setFont(PdfFontFactory.createFont(FontConstants.HELVETICA))

    for (style <- styleOpt) {
      style.color.foreach(c => par.setFontColor(PdfUtil.colorFromHex(c)))
      style.fontSize.foreach(s => par.setFontSize(s * fontFactor))
      style.textAlign match {
        case Some("center") =>
          par.setTextAlignment(TextAlignment.CENTER)
        case Some("right") =>
          par.setTextAlignment(TextAlignment.RIGHT)
        case Some("justified") =>
          par.setTextAlignment(TextAlignment.JUSTIFIED)
        case _ =>
          par.setTextAlignment(TextAlignment.LEFT)

      }
      style.fontWeight match {
        case Some("bold") => par.setFont(PdfFontFactory.createFont(FontConstants.HELVETICA_BOLD)) //TIMES_BOLD))
        case _ => Unit
      }
    }
    par
  }

  val boldExpr = """(.*?)\*(.+?[^*\s])\*(.*)""".r
  val largeExpr = """#(.+)""".r

  /** Parses a single paragraph into multiple Text elements. */
  def parseSinglePar(inputPar: String): Seq[Text] = {
    inputPar match {
      case boldExpr(prefix, bold, suffix) =>
        Seq(new Text(prefix).setFont(normalFont),
          new Text(bold).setFont(boldFont)) ++ parseSinglePar(suffix)
      case _ =>
        Seq(new Text(inputPar))
    }
  }

  /** Parses one markdown string into a collection of PDF paragraphs. */
  def parse(input: String): Seq[Paragraph] = {
    val pars: Array[Paragraph] = for (parText <- input.split("\n"))
      yield {
        val (par: Paragraph, content: String) = parText match {
          case largeExpr(largeContent) => (makeParagraph(1.5f), largeContent)
          case _ => (makeParagraph(1), parText)
        }
        parseSinglePar(content).foreach(par.add)
        par
      }
    pars
  }
}
