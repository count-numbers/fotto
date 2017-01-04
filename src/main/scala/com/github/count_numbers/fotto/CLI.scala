package com.github.count_numbers.fotto

import java.io.File

import com.typesafe.scalalogging.Logger

/**
  * Created by simfischer on 12/26/16.
  */
object CLI {

  val logger = Logger("fotto")

  def parseCLIArgs(args: Array[String]): Map[Symbol, Any] = {
    type OptionMap = Map[Symbol, Any]
    def parseOptions(map: OptionMap, args: List[String]): OptionMap = {
      args match {
        case Nil => map
        case "--width" :: width :: tail => parseOptions(map ++ Map('width -> Util.parseUnit(width)), tail)
        case "--height" :: height :: tail => parseOptions(map ++ Map('height -> Util.parseUnit(height)), tail)
        case "--watch" :: watch :: tail => parseOptions(map ++ Map('watch -> watch.toBoolean), tail)
        case "--view" :: view :: tail => parseOptions(map ++ Map('view -> view.toBoolean), tail)
        case "--out" :: outfile :: tail => parseOptions(map ++ Map('out -> new File(outfile)), tail)
        case "--pages" :: pagesPattern :: tail => parseOptions(map ++ Map('pages -> PagesPattern(pagesPattern)), tail)
        case string :: Nil => parseOptions(map ++ Map('infile -> string), args.tail)
        case option :: tail => {
          println(s"Unknown option ${option}.")
          System.exit(1)
          map
        }
      }
    }

    val result: Map[Symbol, Any] = parseOptions(Map(), args.toList.flatMap(arg => arg.split('=')))
    if (!result.contains('infile)) {
      logger.error("No infile specified!")
    }
    logger.debug(s"Options: ${result}")
    result
  }

}
