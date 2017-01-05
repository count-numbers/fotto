package com.github.count_numbers.fotto

import java.io.{File, OutputStream}
import java.lang.Float

import com.itextpdf.io.font.FontConstants
import com.itextpdf.io.image.{ImageData, ImageDataFactory}
import com.itextpdf.kernel.color.{Color, DeviceRgb}
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.{PageSize, Rectangle}
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState
import com.itextpdf.kernel.pdf.{PdfDocument, PdfName, PdfWriter}
import com.itextpdf.layout.Document
import com.itextpdf.layout.element._
import com.itextpdf.layout.Canvas
import com.itextpdf.layout.hyphenation.HyphenationConfig
import com.itextpdf.layout.property.{TabAlignment, TextAlignment, VerticalAlignment}
import com.itextpdf.layout.renderer.IRenderer

object PdfOutput {

  def apply(searchPath: File, out: OutputStream, format: Format, language: String, country: String): PdfOutput = {
    val writer = new PdfWriter(out)
    val pdfDoc = new PdfDocument(writer)

    val document = new Document(pdfDoc, new PageSize(format.width, format.height))

    pdfDoc.getDocumentInfo.setCreator("Fotto")

    new PdfOutput(searchPath, format, language, country, document, pdfDoc, writer)
  }
}

class PdfOutput (val searchPath: File,
                 val format: Format,
                 val language: String, val country: String,
                 val doc: Document, val pdfDoc: PdfDocument, val writer: PdfWriter) {

  var pageCanvas: PdfCanvas = _

  def startPage() = {
    pageCanvas = new PdfCanvas(pdfDoc.addNewPage())
  }

  def addText(textContent: Content, x: Float, y: Float, width: Float, height: Float, styleOpt: Option[Style]) = {
    withStyle(styleOpt, x, y, width, height)(canvas => {
      for (text <- textContent.text) {
       val par: Paragraph = new Paragraph()
          .setWidth(percentageToUserSpaceX(width))
          .setVerticalAlignment(VerticalAlignment.TOP)
          .setFont(PdfFontFactory.createFont(FontConstants.HELVETICA))

        for (style <- styleOpt) {
          style.color.foreach(c => par.setFontColor(PdfUtil.colorFromHex(c)))
          style.fontSize.foreach(s => par.setFontSize(s))
          style.textAlign match {
            case Some("center") =>
              par.addTabStops(new TabStop(percentageToUserSpaceX(width) / 2, TabAlignment.CENTER))
              par.add(new Tab())
            case _ => Unit
          }
          style.fontWeight match {
            case Some("bold") => par.setFont(PdfFontFactory.createFont(FontConstants.HELVETICA_BOLD)) //TIMES_BOLD))
            case _ => Unit
          }
        }

        par.setHyphenation(new HyphenationConfig(language, country, 3, 3))

        val paragraphs: Seq[Paragraph] = MarkdownParser(percentageToUserSpaceX(width), styleOpt)
          .parse(text)


        // vertical alignment and y position depends on the specified alignment.
        // top starts at y+height, bottom starts at y
        val (yPos: Float, verticalAlignment) = styleOpt.flatMap(_.verticalAlign).getOrElse("top") match {
          case "top" => (y + height, VerticalAlignment.TOP)
          case "bottom" => (y, VerticalAlignment.BOTTOM)
          case "center" => (y + height / 2, VerticalAlignment.MIDDLE)
        }

        // Wrap everything in a div so we can apply vertical layout
        val div = new Div()
        div.setVerticalAlignment(verticalAlignment)
        div.setFixedPosition(percentageToUserSpaceX(x), percentageToUserSpaceY(y), percentageToUserSpaceX(width))
        div.setHeight(percentageToUserSpaceY(height))
        div.setRole(PdfName.Artifact);
        paragraphs.foreach(div.add(_))

        // create canvas on page and add div
        val canvas: Canvas = new Canvas(pageCanvas, pdfDoc, new Rectangle(percentageToUserSpaceX(x), percentageToUserSpaceY(y),
          percentageToUserSpaceX(width), percentageToUserSpaceY(height)))
        canvas.add(div)
      }
    })
  }


  def addImage(image: Content, x: Float, y: Float, width: Float, height: Float, styleOpt: Option[Style]) = {
    withStyle(styleOpt, x, y, width, height)(canvas => {
      for (imageUrl <- image.url) {
        val img: ImageData = ImageDataFactory.create(new File(searchPath, imageUrl).toURI.toURL)
        val imgModel = new Image(img)

        canvas.rectangle(percentageToUserSpace(x, y, width, height))
        canvas.clip()
        canvas.newPath()
        val xRatio = percentageToUserSpaceX(width) / imgModel.getImageScaledWidth
        val yRatio = percentageToUserSpaceY(height) / imgModel.getImageScaledHeight
        val scale = Math.max(xRatio, yRatio)
        val newWidth: Float = imgModel.getImageScaledWidth * scale
        val newHeight: Float = imgModel.getImageScaledHeight * scale
        val xCorrection = (newWidth - percentageToUserSpaceX(width)) * image.cropDisplacement.getOrElse(.5f)
        val yCorrection = (newHeight - percentageToUserSpaceY(height)) * image.cropDisplacement.getOrElse(.5f)
        val bounds = new Rectangle(
          percentageToUserSpaceX(x) - xCorrection,
          percentageToUserSpaceY(y) - yCorrection,
          newWidth, newHeight)
        canvas.addImage(img, bounds, false)
      }
    })
  }

  def withStyle(styleOpt: Option[Style], x: Float, y: Float, width: Float, height: Float)(f: PdfCanvas => Unit): Unit = {
    pageCanvas.saveState()
    val state = new PdfExtGState()
    for (style <- styleOpt) {
      style.opacity.foreach({o =>
        state.setFillOpacity(o)
        state.setStrokeOpacity(o)
      })
    }
    pageCanvas.setExtGState(state)

    f(pageCanvas)


    for (style <- styleOpt) {
      if (style.borderWidth.isDefined || style.borderColor.isDefined) {
        val borderRect = percentageToUserSpace(x, y, width, height)
        style.borderWidth.foreach(pageCanvas.setLineWidth(_))
        style.borderColor.foreach(c => pageCanvas.setStrokeColor(PdfUtil.colorFromHex(c)))
        pageCanvas.rectangle(borderRect)
        pageCanvas.stroke
      }
    }

    pageCanvas.restoreState
  }

  def percentageToUserSpaceX(x: Float): Float = x * format.width / 100

  def percentageToUserSpaceY(y: Float): Float = y * format.height / 100

  def percentageToUserSpace(x: Float, y: Float, width: Float, height: Float): Rectangle = {
    new Rectangle(
      percentageToUserSpaceX(x),
      percentageToUserSpaceY(y),
      percentageToUserSpaceX(width),
      percentageToUserSpaceY(height)
    )
  }

  def close(): Unit = {
    pdfDoc.close()
  }

}
