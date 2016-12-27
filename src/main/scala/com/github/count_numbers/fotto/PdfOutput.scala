package com.github.count_numbers.fotto

import java.io.{File, FileOutputStream, OutputStream}

import com.itextpdf.io.font.FontConstants
import com.itextpdf.io.image.{ImageData, ImageDataFactory}
import com.itextpdf.kernel.color.{Color, DeviceRgb}
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.{PageSize, Rectangle}
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState
import com.itextpdf.kernel.pdf.xobject.{PdfFormXObject, PdfXObject}
import com.itextpdf.kernel.pdf.{PdfDocument, PdfName, PdfString, PdfWriter}
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.{Image, Paragraph, Tab, TabStop}
import com.itextpdf.layout.Canvas
import com.itextpdf.layout.hyphenation.HyphenationConfig
import com.itextpdf.layout.property.{HorizontalAlignment, TabAlignment, TextAlignment, VerticalAlignment}

object PdfOutput {

  def apply(searchPath: File, out: OutputStream, width: Float, height: Float, language: String, country: String): PdfOutput = {
    val writer = new PdfWriter(out)
    val pdfDoc = new PdfDocument(writer)

    val document = new Document(pdfDoc, new PageSize(width, height))

    pdfDoc.getDocumentInfo.setCreator("Fotto")

    new PdfOutput(searchPath, width, height, language, country, document, pdfDoc, writer)
  }
}

class PdfOutput (val searchPath: File,
                 val pageWidth: Float, val pageHeight: Float,
                 val language: String, val country: String,
                 val doc: Document, val pdfDoc: PdfDocument, val writer: PdfWriter) {

  var pageCanvas: PdfCanvas = null

  def startPage() = {
    pageCanvas = new PdfCanvas(pdfDoc.addNewPage())
  }

  def addText(text: String, x: Float, y: Float, width: Float, height: Float, styleOpt: Option[Style]) = {
    withStyle(styleOpt, x, y, width, height)(canvas => {
      val rect = percentageToUserSpace(x, y, width, height)

      val par: Paragraph = new Paragraph()
          .setWidth(percentageToUserSpaceX(width))
          .setVerticalAlignment(VerticalAlignment.TOP)
          .setFont(PdfFontFactory.createFont(FontConstants.HELVETICA))

      for (style <- styleOpt) {
        style.color.foreach(c => par.setFontColor(colorFromHex(c)))
        style.fontSize.foreach(s => par.setFontSize(s))
        style.textAlign match {
          case Some("center") =>
            par.addTabStops(new TabStop(percentageToUserSpaceX(width)/2, TabAlignment.CENTER))
            par.add(new Tab())
          case _ => Unit
        }
        style.fontWeight match {
          case Some("bold") => par.setFont(PdfFontFactory.createFont(FontConstants.HELVETICA_BOLD)) //TIMES_BOLD))
          case _ => Unit
        }
      }
      par.setHyphenation(new HyphenationConfig(language, country, 3, 3))
      par.add(text)

      // vertical alignment and y position depends on the specified alignment.
      // top starts at y+height, bottom starts at y
      val (yPos: Float, verticalAlignment) = styleOpt.flatMap(_.verticalAlign).getOrElse("top") match {
        case "top" => (y + height, VerticalAlignment.TOP)
        case "bottom" => (y , VerticalAlignment.BOTTOM)
        case "center" => (y + height/2 , VerticalAlignment.MIDDLE)
      }

      new Canvas(pageCanvas, pdfDoc, new PageSize(pageWidth, pageHeight))
        .showTextAligned(par, percentageToUserSpaceX(x), percentageToUserSpaceY(yPos), TextAlignment.JUSTIFIED, verticalAlignment)

    })
  }


  def addImage(imagePath: String, x: Float, y: Float, width: Float, height: Float, styleOpt: Option[Style]) = {
    withStyle(styleOpt, x, y, width, height)(canvas => {

      val img: ImageData = ImageDataFactory.create(new File(searchPath, imagePath).toURI.toURL)

      //val rect: Rectangle = percentageToUserSpace(x, y, width, height)
      val imgModel = new Image(img);

      /*
      val xObject = new PdfFormXObject(new Rectangle(850, 600));
      val xObjectCanvas = new PdfCanvas(xObject, pdfDoc);
      xObjectCanvas.ellipse(0, 0, 850, 600);
      xObjectCanvas.clip();
      xObjectCanvas.newPath();
      xObjectCanvas.addImage(img, imgModel.getImageScaledWidth, 0, 0, imgModel.getImageScaledHeight, 0, -600);
      //val clipped: Image = new com.itextpdf.layout.element.Image(xObject);

      canvas.addXObject(xObject, rect)
*/
/*
      //imgModel.setFixedPosition(0, 0)
      //imgModel.scaleToFit(percentageToUserSpaceX(width), percentageToUserSpaceY(height))
      val bounds: Rectangle = new Rectangle(400, 400)
      val clip = new PdfFormXObject(bounds)
      val clipCanvas = new PdfCanvas(clip, pdfDoc)
      clipCanvas.rectangle(bounds)
      clipCanvas.clip()
      clipCanvas.newPath()
      clipCanvas.addImage(img, 0, 0, false)

      val clippedImg = new com.itextpdf.layout.element.Image(clip)

      //canvas.addImage(clippedImg, percentageToUserSpace(x, y, width, height), false)
      //canvas.addXObject(clip, rect)
*/
      canvas.rectangle(percentageToUserSpace(x, y, width, height))
      canvas.clip()
      canvas.newPath()
      val xRatio = percentageToUserSpaceX(width) / imgModel.getImageScaledWidth
      val yRatio = percentageToUserSpaceY(height) / imgModel.getImageScaledHeight
      val scale = Math.max(xRatio, yRatio)
      val newWidth: Float = imgModel.getImageScaledWidth * scale
      val newHeight: Float = imgModel.getImageScaledHeight * scale
      val xCorrection = (newWidth - percentageToUserSpaceX(width)) / 2
      val yCorrection = (newHeight - percentageToUserSpaceX(height)) / 2
      val bounds = new Rectangle(
        percentageToUserSpaceX(x) - xCorrection,
        percentageToUserSpaceY(y) - yCorrection,
        newWidth, newHeight)
      canvas.addImage(img, bounds, false)

      // percentageToUserSpace(x, y, width, height)
    })
  }

  def withStyle(styleOpt: Option[Style], x: Float, y: Float, width: Float, height: Float)(f: PdfCanvas => Unit): Unit = {
    pageCanvas.saveState();
    val state = new PdfExtGState();
    for (style <- styleOpt) {
      style.opacity.foreach({o =>
        state.setFillOpacity(o)
        state.setStrokeOpacity(o)
      })
    }
    pageCanvas.setExtGState(state);

    f(pageCanvas)


    for (style <- styleOpt) {
      if (style.borderWidth.isDefined || style.borderColor.isDefined) {
        val borderRect = percentageToUserSpace(x, y, width, height)
        style.borderWidth.foreach(pageCanvas.setLineWidth(_))
        style.borderColor.foreach(c => pageCanvas.setStrokeColor(colorFromHex(c)))
        pageCanvas.rectangle(borderRect)
        pageCanvas.stroke()
      }
    }

    pageCanvas.restoreState();
  }

  def percentageToUserSpaceX(x: Float): Float = x * pageWidth / 100

  def percentageToUserSpaceY(y: Float): Float = y * pageHeight / 100

  def percentageToUserSpace(x: Float, y: Float, width: Float, height: Float): Rectangle = {
    return new Rectangle(
      percentageToUserSpaceX(x),
      percentageToUserSpaceY(y),
      percentageToUserSpaceX(width),
      percentageToUserSpaceY(height)
    )
  }

  def colorFromHex(hex: String): Color = {
    new DeviceRgb(
      Integer.valueOf(hex.substring( 1, 3 ), 16 ),
      Integer.valueOf(hex.substring( 3, 5 ), 16 ),
      Integer.valueOf(hex.substring( 5, 7 ), 16 ));
  }

  def close(): Unit = {
    pdfDoc.close()
  }

}
