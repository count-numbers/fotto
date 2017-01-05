package com.github.count_numbers.fotto

import play.api.libs.json.{Json, Reads}

/**
  * Created by simfischer on 12/23/16.
  */
case class Template(format: String, defaultStyleRef: Option[String], pageTemplates: Map[String,PageTemplate], styles: Map[String,Style], margins: Margins)
case class Margins(top: Float, bottom: Float, inner: Float, outer: Float)
case class PageTemplate(placeholders: Map[String, Placeholder], twoSided: Option[Boolean])

case class Placeholder(contentType: String, x: Float, y: Float, width: Float, height: Float,
                       defaultTo: Option[String], styleRef: Option[String], relativeTo: Option[String], layer: Option[Int] = Some(1)) {
  def mirror(): Placeholder = {
    copy(x = 100 - this.x - this.width)
  }

  def relativeTo(margins: Margins, odd: Boolean): Placeholder = {
    val contentW: Float = 100 - margins.inner - margins.outer
    val contentH: Float = 100 - margins.top - margins.bottom
    val left: Float = if (odd)  margins.inner else margins.outer

    copy(x = (left + this.x * contentW / 100),
      y = margins.bottom + this.y * contentH / 100,
      width = this.width * contentW / 100,
      height = this.height * contentH / 100)
  }
}

case class Style(
  parentRef: Option[String],
  borderWidth: Option[Int],
  borderColor: Option[String],
  opacity: Option[Float],
  color: Option[String],
  fontSize: Option[Float],
  textAlign: Option[String],
  fontWeight: Option[String],
  verticalAlign: Option[String],
  backgroundColor: Option[String]) {

  def orElse(other: Style): Style = {
    Style(
      None,
      borderWidth.orElse(other.borderWidth),
      borderColor.orElse(other.borderColor),
      opacity.orElse(other.opacity),
      color.orElse(other.color),
      fontSize.orElse(other.fontSize),
      textAlign.orElse(other.textAlign),
      fontWeight.orElse(other.fontWeight),
      verticalAlign.orElse(other.verticalAlign),
      backgroundColor.orElse(other.backgroundColor)
    )
  }
}

object Style {
  implicit val styleReads: Reads[Style] = Json.reads[Style]
}

object Template {
  implicit val marginReads: Reads[Margins] = Json.reads[Margins]
  implicit val phReads: Reads[Placeholder] = Json.reads[Placeholder]
  implicit val ptReads: Reads[PageTemplate] = Json.reads[PageTemplate]
  implicit val templateReads: Reads[Template] = Json.reads[Template]
}