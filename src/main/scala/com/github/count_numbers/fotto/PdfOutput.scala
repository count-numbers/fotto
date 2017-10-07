package com.github.count_numbers.fotto

import java.awt.geom.AffineTransform
import java.awt.image.{AffineTransformOp, BufferedImage}
import java.io.{File, OutputStream}
import java.lang.Float
import javax.imageio.ImageIO

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
import com.typesafe.scalalogging.Logger

object PdfOutput {

  val logger = Logger("fotto")

  def apply(searchPath: File, out: OutputStream, format: Format, language: String, country: String, lowResolution: Boolean): PdfOutput = {
    val writer = new PdfWriter(out)
    val pdfDoc = new PdfDocument(writer)

    val document = new Document(pdfDoc, new PageSize(
      format.width + format.trimOuter + format.trimInner,
      format.height + format.trimBottom + format.trimTop))

    pdfDoc.getDocumentInfo.setCreator("Fotto")

    new PdfOutput(searchPath, format, language, country, document, pdfDoc, writer, lowResolution)
  }
}

class PdfOutput (val searchPath: File,
                 val format: Format,
                 val language: String, val country: String,
                 val doc: Document, val pdfDoc: PdfDocument, val writer: PdfWriter,
                 val lowResolution: Boolean) {

  var pageCanvas: PdfCanvas = _
  var left: Boolean = true

  def startPage(left: Boolean) = {
    pageCanvas = new PdfCanvas(pdfDoc.addNewPage())
    this.left = left
  }

  def trimLines() = {

    val trimlineVisibility = 2f/3f
    val (trimLeft, trimRight) = if (left) (format.trimOuter, format.trimInner) else (format.trimInner, format.trimOuter)

    pageCanvas
      .setStrokeColor(PdfUtil.colorFromHex("#000000"))
      .setLineWidth(0.5f)

      // bottom left vertical
      .moveTo(trimLeft, 0)
      .lineTo(trimLeft, format.trimBottom * trimlineVisibility)
      .closePathStroke()
      // bottom left horizontal
      .moveTo(0, format.trimBottom)
      .lineTo(trimLeft * trimlineVisibility, format.trimBottom)
      .closePathStroke()

      // top left vertical
      .moveTo(trimLeft, format.totalHeight)
      .lineTo(trimLeft, format.totalHeight - format.trimTop * trimlineVisibility)
      .closePathStroke()
      // top left horizontal
      .moveTo(0, format.totalHeight - format.trimTop)
      .lineTo(trimLeft * trimlineVisibility, format.totalHeight - format.trimTop)
      .closePathStroke()

      // bottom right vertical
      .moveTo(format.totalWidth - trimRight, 0)
      .lineTo(format.totalWidth - trimRight, format.trimBottom * trimlineVisibility)
      .closePathStroke()
      // bottom right horizontal
      .moveTo(format.totalWidth, format.trimBottom)
      .lineTo(format.totalWidth - trimRight * trimlineVisibility, format.trimBottom)
      .closePathStroke()

      // top right vertical
      .moveTo(format.totalWidth - trimRight, format.totalHeight)
      .lineTo(format.totalWidth - trimRight, format.totalHeight - format.trimTop * trimlineVisibility)
      .closePathStroke()
      // top right horizontal
      .moveTo(format.totalWidth, format.totalHeight - format.trimTop)
      .lineTo(format.totalWidth - trimRight * trimlineVisibility, format.totalHeight - format.trimTop)
      .closePathStroke()


  }

  def addText(textContent: Content, x: Float, y: Float, width: Float, height: Float, styleOpt: Option[Style]) = {
    withStyle(styleOpt, x, y, width, height)(canvas => {
      for (text <- textContent.text) {
       val par: Paragraph = new Paragraph()
          .setWidth(percentageToUserSpaceX(width, styleOpt.flatMap(_.addTrimmingMargin)))
          .setVerticalAlignment(VerticalAlignment.TOP)
          .setFont(PdfFontFactory.createFont(FontConstants.HELVETICA))


        for (style <- styleOpt) {
          style.color.foreach(c => par.setFontColor(PdfUtil.colorFromHex(c)))
          style.fontSize.foreach(s => par.setFontSize(s))
          style.leading.foreach(par.setMultipliedLeading(_))
          style.textAlign match {
            case Some("center") =>
              par.addTabStops(new TabStop(percentageToUserSpaceX(width, styleOpt.flatMap(_.addTrimmingMargin)) / 2, TabAlignment.CENTER))
              par.add(new Tab())
            case _ => Unit
          }
          style.fontWeight match {
            case Some("bold") => par.setFont(PdfFontFactory.createFont(FontConstants.HELVETICA_BOLD)) //TIMES_BOLD))
            case _ => Unit
          }
        }

        par.setHyphenation(new HyphenationConfig(language, country, 2, 2))

        val paragraphs: Seq[Paragraph] = MarkdownParser(percentageToUserSpaceW(width, styleOpt.flatMap(_.addTrimmingMargin)), styleOpt)
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
        div.setFixedPosition(percentageToUserSpaceX(x, styleOpt.flatMap(_.addTrimmingMargin)), percentageToUserSpaceY(y, styleOpt.flatMap(_.addTrimmingMargin)), percentageToUserSpaceW(width,styleOpt.flatMap(_.addTrimmingMargin)))
        div.setHeight(percentageToUserSpaceH(height, styleOpt.flatMap(_.addTrimmingMargin)))
        div.setRole(PdfName.Artifact);

        for {
          style: Style <- styleOpt
          rotation <- style.rotation
        } { div.setRotationAngle(scala.math.toRadians(rotation)) }
        paragraphs.foreach(div.add(_))

        // create canvas on page and add div
        val canvas: Canvas = new Canvas(pageCanvas, pdfDoc,
                                  new Rectangle(percentageToUserSpaceX(x, styleOpt.flatMap(_.addTrimmingMargin)),
                                                percentageToUserSpaceY(y, styleOpt.flatMap(_.addTrimmingMargin)),
                                                percentageToUserSpaceX(width, styleOpt.flatMap(_.addTrimmingMargin)),
                                                percentageToUserSpaceY(height, styleOpt.flatMap(_.addTrimmingMargin))))
        canvas.add(div)
      }
    })
  }


  /** Can be used to create a downsampled image in order to keep the PDF small and have a faster preview.
    * Increases creation time. */
  def loadDownsampledImage(file: File) = {
    val before = ImageIO.read(file)
    val w = before.getWidth()
    val h = before.getHeight()
    val after = new BufferedImage(w / 10, h / 10, before.getType)
    val at = new AffineTransform()
    at.scale(0.1, 0.1)
    val scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR)
    scaleOp.filter(before, after)
  }

  def addImage(image: Content, x: Float, y: Float, width: Float, height: Float, styleOpt: Option[Style]) = {
    withStyle(styleOpt, x, y, width, height)(canvas => {

      canvas.rectangle(percentageToUserSpace(x, y, width, height, styleOpt.flatMap(_.addTrimmingMargin)))
      canvas.clip()
      canvas.newPath()

      for (imageUrl <- image.url) {
        val img: ImageData =
          if (lowResolution) ImageDataFactory.create(loadDownsampledImage(new File(searchPath, imageUrl)), null, false)
          else ImageDataFactory.create(new File(searchPath, imageUrl).toURI.toURL)
        val imgModel = new Image(img)

        val xRatio = percentageToUserSpaceW(width, styleOpt.flatMap(_.addTrimmingMargin)) / imgModel.getImageScaledWidth
        val yRatio = percentageToUserSpaceH(height, styleOpt.flatMap(_.addTrimmingMargin)) / imgModel.getImageScaledHeight
        val scale = Math.max(xRatio, yRatio)
        val newWidth: Float = imgModel.getImageScaledWidth * scale
        val newHeight: Float = imgModel.getImageScaledHeight * scale
        val xCorrection = (newWidth - percentageToUserSpaceW(width, styleOpt.flatMap(_.addTrimmingMargin))) * image.cropDisplacement.getOrElse(.5f)
        val yCorrection = (newHeight - percentageToUserSpaceH(height, styleOpt.flatMap(_.addTrimmingMargin))) * image.cropDisplacement.getOrElse(.5f)
        val bounds = new Rectangle(
          percentageToUserSpaceX(x, styleOpt.flatMap(_.addTrimmingMargin)) - xCorrection,
          percentageToUserSpaceY(y, styleOpt.flatMap(_.addTrimmingMargin)) - yCorrection,
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

    for (style <- styleOpt) {
      for (backgroundColor <- style.backgroundColor) {
        val borderRect = percentageToUserSpace(x, y, width, height, style.addTrimmingMargin)
        pageCanvas.rectangle(borderRect)
        pageCanvas.setFillColor(PdfUtil.colorFromHex(backgroundColor))
        pageCanvas.fill()
      }
    }
    pageCanvas.setExtGState(state)

    f(pageCanvas)

    for (style <- styleOpt) {
      if (style.borderWidth.isDefined || style.borderColor.isDefined) {
        val borderRect = percentageToUserSpace(x, y, width, height, style.addTrimmingMargin)
        style.borderWidth.foreach(pageCanvas.setLineWidth(_))
        style.borderColor.foreach(c => pageCanvas.setStrokeColor(PdfUtil.colorFromHex(c)))
        pageCanvas.rectangle(borderRect)
        pageCanvas.stroke
      }
    }

    pageCanvas.restoreState
  }

  def percentageToUserSpaceX(x: Float, addTrimmingMargin: Option[Boolean]): Float =
    if (addTrimmingMargin.getOrElse(false))
      x * format.totalWidth / 100
    else
      x * format.width / 100 + (if (left) format.trimOuter else format.trimInner)

  def percentageToUserSpaceY(y: Float, addTrimmingMargin: Option[Boolean]): Float =
    if (addTrimmingMargin.getOrElse(false))
      y * format.totalHeight() / 100
    else
      y * format.height / 100 + format.trimBottom

  def percentageToUserSpaceW(x: Float, addTrimmingMargin: Option[Boolean]): Float =
    if (addTrimmingMargin.getOrElse(false))
      x * format.totalWidth / 100
    else
    x * format.width / 100

  def percentageToUserSpaceH(y: Float, addTrimmingMargin: Option[Boolean]): Float =
    if (addTrimmingMargin.getOrElse(false))
      y * format.totalHeight() / 100
    else
      y * format.height / 100

  def percentageToUserSpace(x: Float, y: Float, width: Float, height: Float, addTrimmingMargin: Option[Boolean]): Rectangle = {
      new Rectangle(
        percentageToUserSpaceX(x, addTrimmingMargin),
        percentageToUserSpaceY(y, addTrimmingMargin),
        percentageToUserSpaceW(width, addTrimmingMargin),
        percentageToUserSpaceH(height, addTrimmingMargin))
  }

  def close(): Unit = {
    pdfDoc.close()
  }

}
