name := "fotto"

version := "1.0"

scalaVersion := "2.11.8"

mainClass in (Compile, run) := Some("com.github.count_numbers.fotto.Fotto")

libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.5.10"
//libraryDependencies += "org.scala-lang" % "scala-library" % "2.12.0"
//libraryDependencies += "com.itextpdf" % "itextpdf" % "5.5.10"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"

libraryDependencies += "com.itextpdf" % "layout" % "7.0.1"
libraryDependencies += "com.itextpdf" % "hyph" % "7.0.1"

libraryDependencies += "com.typesafe.scala-logging" % "scala-logging_2.11" % "3.5.0"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.22"
