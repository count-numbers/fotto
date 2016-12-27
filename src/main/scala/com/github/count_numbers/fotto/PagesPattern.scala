package com.github.count_numbers.fotto

import scala.util.matching.Regex


case class Range(from: Int, to: Int) {
  def contains(page: Int) = {
    page >= from && page <= to
  }
}

object PagesPattern {
  def apply(pattern: String): PagesPattern = {
    if (pattern.isEmpty) {
      PagesPattern(Seq())
    } else {
      val ranges: Seq[Range] =
        pattern
          .split(',')
          .map(substr => {
            val rangeP: Regex = "([\\d]+)-([\\d]+)".r
            val singleP: Regex = "([\\d]+)".r
            substr match {
              case rangeP(first, last) => Range(first.toInt, last.toInt)
              case singleP(page) => val p = page.toInt; Range(p, p)
            }
          })
      PagesPattern(ranges)
    }
  }
}

case class PagesPattern(ranges: Seq[Range]) {

  def contains(page: Int) = {
    ranges.isEmpty || !ranges.filter(_.contains(page)).isEmpty
  }
}
