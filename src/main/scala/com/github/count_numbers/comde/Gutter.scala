package com.github.count_numbers.comde

import java.io.{File, FileOutputStream}

import com.github.count_numbers.fotto.{Format, Util}
import com.itextpdf.kernel.color.Color
import com.itextpdf.kernel.geom.{PageSize, Rectangle}
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.{PdfDocument, PdfWriter}
import com.itextpdf.layout.Document

case class Margins(inner: Float, outer: Float, top: Float, bottom: Float)

case class PanelRow(weight: Float, widths: Seq[Float])

object Gutter {

  def render(page: Format, margins: Margins, rows: Seq[PanelRow], padding: Float, canvas: PdfCanvas) = {
    val totalWeight     = rows.map(_.weight).sum
    val contentHeight   = page.height - margins.bottom - margins.top
    val contentWidth    = page.width  - margins.inner  - margins.outer
    val availableHeight = contentHeight - ((rows.size-1) * padding)

    var y: Float = margins.bottom + contentHeight

    for (row: PanelRow <- rows) {
      val rowHeight      = availableHeight * row.weight / totalWeight
      y -= rowHeight

      val totalRowWeight = row.widths.sum
      val availableWidth = contentWidth - ((row.widths.size-1) * padding)

      var x: Float = margins.inner
      for (panelWeight <- row.widths) {
        val panelWidth = availableWidth * panelWeight / totalRowWeight

        val rect: Rectangle = new Rectangle(x, y, panelWidth, rowHeight)
        println(s"Drawing ${rect} @ ${x}, ${y}")
        canvas.rectangle(rect).stroke()

        x += panelWidth + padding
      }
      y -= padding
    }
  }

  def main(args: Array[String]): Unit = {
    val page = Format.fromCM(20, 30)
    val margins = Margins(Util.cmToPt(1f), Util.cmToPt(1f), Util.cmToPt(1f), Util.cmToPt(1f))
    val padding = Util.cmToPt(0.3f)

    val rows = Seq(
      PanelRow(1, Seq(1)),
      PanelRow(1, Seq(1, 1, 1, 1, 1)),
      PanelRow(1, Seq(1, 1)),
      PanelRow(1, Seq(1)))

    println("Starting")

    val out: File = new File("gutter.pdf")
    val writer = new PdfWriter(new FileOutputStream(out))
    val pdfDoc = new PdfDocument(writer)
    val document = new Document(pdfDoc, new PageSize(page.width, page.height))
    val pageCanvas = new PdfCanvas(pdfDoc.addNewPage())
    pageCanvas.setStrokeColor(Color.BLACK)
    pageCanvas.setLineWidth(2f)


    println(s"Rendering ${out}")

    render(page, margins, rows, padding, pageCanvas)

    document.close()

    println("Done")
  }
}
