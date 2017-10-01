package com.github.count_numbers.fotto

import java.awt.Desktop
import java.io.{File, FileOutputStream}

import com.itextpdf.kernel.font.PdfFontFactory
import com.typesafe.scalalogging.Logger
import play.api.libs.json.Json

import scala.io.Source

/**
  * Created by simfischer on 12/23/16.
  */
object Fotto {

  val logger = Logger("fotto")

  var viewerOpen = false

  def main(args: Array[String]): Unit = {
    val options: Map[Symbol, Any] = CLI.parseCLIArgs(args)
    for (infileName <- options.get('infile).map(_.asInstanceOf[String])) {

      val infile = new File(infileName)

      val outfile = options.getOrElse('out, new File(infileName.substring(0, infileName.lastIndexOf('.')) + ".pdf")).asInstanceOf[File]
      logger.info(s"Reading ${infile}, writing to ${outfile}.")

      options.get('watch) match {
        case Some(true) => Loop(Seq(infile), c => runOnce(infile, outfile, options))
        case _ => runOnce(infile, outfile, options)
      }
    }
  }

  def runOnce(infile: File, outfile: File, options: Map[Symbol, Any]) = {

    val book = Json
      .parse(Source
        .fromFile(infile)
        .getLines
        .mkString)
      .as[Book]

    val templateFile = new File(infile.getParentFile, book.templateFile)
    val templateRaw = Json
      .parse(Source
        .fromFile(templateFile)
        .getLines
        .mkString)
      .as[Template]

    // Resolve all Style.parentRef. Replace all entries in templateRaw.styles map...
    val stylesWithFallbacks: Map[String, Style] = templateRaw.styles.map({case (key, style) =>
      // ... by new entries with same key and a style which is....
      (key, style.parentRef match {
        // ... either the same if no parent was specified
        case None => style
        // ... or, if the parent was specified, ...
        case Some(parentRef) =>
          /// ... a new style falling back to the parent, if we find a parent with that name.
          templateRaw.styles.get(parentRef)
            .map(p => style.orElse(p))
            .getOrElse({logger.debug(s"Parent of ${key} not found: ${parentRef}."); style})
      })
    })

    val template: Template = templateRaw.copy(styles = stylesWithFallbacks)

    logger.debug("Book: "+book)
    logger.debug("Template: "+template)

    // User can override size as CLI arg. Otherwise, use named format from template.
    val format = (options.get('width), options.get('height)) match {
      case (Some(width: Float), Some(height: Float)) => Format(width, height)
      // TODO: Handle unknown format names
      case _ => Format.namedFormats(template.format)
    }

    val out = PdfOutput(infile.getParentFile, new FileOutputStream(outfile),
      format,
      book.language, book.country)


    val pagesPattern = options.getOrElse('pages, PagesPattern("")).asInstanceOf[PagesPattern]

    for ((page: Page, pageNumber: Int) <- book.pages.view.zipWithIndex if pagesPattern.contains(pageNumber+1)) {
      logger.info(s"### Processing page ${pageNumber+1} of type ${page.pageTemplateRef}.")

      out.startPage(pageNumber % 2 == 1)

      val pageTemplate = template.pageTemplates(page.pageTemplateRef)
      for ((key, placeholder) <- pageTemplate.placeholders.toSeq.sortBy(_._2.layer.getOrElse(1))) {
        // first, identify style for this placeholder. this is solely described by the template and indpendent of the album
        // placeholder's styleref falls back to template's default
        val styleFromTemplate: Option[Style] =
        placeholder.styleRef.orElse(template.defaultStyleRef)
          .flatMap(template.styles.get)

        val styleOverride: Option[Style] = page.styleOverrides.getOrElse(Map()).get(key)

        val style = (styleFromTemplate, styleOverride) match {
          case (None, None) => None
          case (Some(s), None) => Some(s)
          case (None, Some(s)) => Some(s)
          case (Some(s1), Some(s2)) => Some(s2.orElse(s1))
        }

        // See whether an image/text is specified for this placeholder
        val assignmentOpt: Option[Content] =
        // user defined assignment
        page.assignments.get(key)
          // if undefined, see what placeholder the placeholder wants to default to and take that placeholders image
          .orElse({
            logger.debug(s"No content assigned to ${key}. Trying default.")
            placeholder.defaultTo.flatMap(page.assignments.get)
          })
          .orElse({
            logger.warn(s"No content assigned to ${key}.")
            Some(Content())
          })

        val mirroredPlaceholder =
          if ((pageNumber % 2 == 1) && pageTemplate.twoSided.getOrElse(false))
            placeholder.mirror()
          else
            placeholder

        val relativePlaceholder =
          if (mirroredPlaceholder.relativeTo.getOrElse("page") == "content")
            mirroredPlaceholder.relativeTo(template.margins, pageNumber % 2 == 0)
          else
            mirroredPlaceholder

        // insert image or text, depending on type
        relativePlaceholder.contentType match {
          case "image" => {
            for (image: Content <- assignmentOpt) {
              logger.debug(s"   ${key} -> ${relativePlaceholder} -> ${image} -> ${style}.")
              out.addImage(image, relativePlaceholder.x, relativePlaceholder.y, relativePlaceholder.width, relativePlaceholder.height, style)
            }
          }
          case "text" => {
            for (text: Content <- assignmentOpt) {
              logger.debug(s"   ${key} -> ${relativePlaceholder} -> ${text} -> ${style}.")
              out.addText(text, relativePlaceholder.x, relativePlaceholder.y, relativePlaceholder.width, relativePlaceholder.height, style)
            }
          }
          case "page" => {
            logger.debug(s"   ${key} -> ${relativePlaceholder} -> ${pageNumber} -> ${style}.")
            out.addText(Content(text=Some((pageNumber+1).toString)), relativePlaceholder.x, relativePlaceholder.y, relativePlaceholder.width, relativePlaceholder.height, style)
          }
        }
      }


      options.get('cropMarks) match {
        case Some(false) => Unit
        case _ => out.trimLines()
      }
    }

    out.close()

    for (view <- options.get('view)) {
      if (view == true && !viewerOpen) {
        viewerOpen = true
        Desktop.getDesktop.open(outfile)
      }
    }
  }

}
