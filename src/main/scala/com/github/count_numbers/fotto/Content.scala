package com.github.count_numbers.fotto

import play.api.libs.json._

/**
  * Content of a placeholder. Can either be represented as a single string or compound object.
  */
case class Content(url: Option[String] = None, text: Option[String] = None, cropDisplacement: Option[Float] = Some(0.5f))

object Content {

  val contentReadsSimple = Json.reads[Content]

  implicit val contentEitherReads = Reads[Content] {
    case JsString(value) => JsSuccess(Content(url=Some(value), text=Some(value)))
    case jso: JsObject => jso.validate[Content](contentReadsSimple)
    case _ => JsError("Content must be string or object")
  }
}
