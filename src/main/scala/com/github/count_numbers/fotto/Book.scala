package com.github.count_numbers.fotto

import play.api.libs.json.Json

case class Book(templateFile: String, language: String, country: String, pages: Seq[Page])
case class Page(pageTemplateRef: String, assignments: Map[String, String], styleOverrides: Option[Map[String, Style]])

object Book {
  implicit val pageReads = Json.reads[Page]
  implicit val bookReads = Json.reads[Book]
}