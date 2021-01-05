import sbt.Keys.libraryDependencies

name := "EbookLibraryFatClient"

version := "0.1"

scalaVersion := "2.13.3"

scalacOptions += "-Ymacro-annotations"
scalacOptions += "-deprecation"
//scalacOptions += "-Ypartial-unification"

// Add dependency on ScalaFX library
libraryDependencies += "org.scalafx" %% "scalafx" % "14-R19"

// Determine OS version of JavaFX binaries
lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}

lazy val javaFXModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
libraryDependencies ++= javaFXModules.map( m => "org.openjfx" % s"javafx-$m" % "12.0.1" classifier osName)

libraryDependencies ++= { // scalafx (and fxml)
  val scalafxmlVersion = "0.5"
  Seq("org.scalafx" %% "scalafxml-core-sfx8" % scalafxmlVersion,
      "org.scalafx" %% "scalafxml-guice-sfx8" % scalafxmlVersion)
}

libraryDependencies ++= { // guice dependency injection
  Seq("com.google.inject" % "guice" % "5.0.0-BETA-1",
      "net.codingwell" %% "scala-guice" % "4.2.11")
}

// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "29.0-jre"

libraryDependencies ++= { // logging
  Seq("com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "ch.qos.logback" % "logback-classic" % "1.2.3")
}

// ICU4J https://mvnrepository.com/artifact/com.ibm.icu/icu4j
libraryDependencies += "com.ibm.icu" % "icu4j" % "60.2"

libraryDependencies += "com.beachape" %% "enumeratum" % "1.5.13"

libraryDependencies ++= { // yaml parser
  val circeVersion = "0.13.0"
  Seq("io.circe" %% "circe-yaml" % circeVersion,
//      "io.circe" %% "circe-optics" % "0.11.0", //circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "io.circe" %% "circe-literal" % circeVersion,
      "org.typelevel" %% "cats-core" % "2.2.0")
  //libraryDependencies += "io.circe" %% "circe-config" % "0.6.1"
}

libraryDependencies += "org.controlsfx" % "controlsfx" % "9.0.0"

libraryDependencies += "com.positiondev.epublib" % "epublib-core" % "3.1" excludeAll(
    ExclusionRule(organization = "org.slf4j")
)

// https://mvnrepository.com/artifact/org.codehaus.groovy/groovy-jsr223
libraryDependencies += "org.codehaus.groovy" % "groovy-jsr223" % "2.5.2"

libraryDependencies ++= {
  val sttpVersion = "3.0.0-RC5"
  Seq("com.softwaremill.sttp.client3" %% "core" % sttpVersion,
      "com.softwaremill.sttp.client3" %% "circe" % sttpVersion)
}

// Apache Tika - mime type detection and file content scanning lib
libraryDependencies ++= {
  val tikaVersion = "1.24.1"
  val bouncyCastleVersion = "1.66"
  Seq("org.apache.tika" % "tika-core" % tikaVersion,
      "org.apache.tika" % "tika-parsers" % tikaVersion,
      "org.xerial" % "sqlite-jdbc" % "3.8.10.1",
      "org.apache.pdfbox" % "pdfbox" % "2.0.21",
      "org.apache.pdfbox" % "jbig2-imageio" % "3.0.3",
      "com.github.jai-imageio" % "jai-imageio-core" % "1.3.0",
      "org.bouncycastle" % "bcprov-jdk15on" % bouncyCastleVersion,
      "org.bouncycastle" % "bcmail-jdk15on" % bouncyCastleVersion,
      "org.bouncycastle" % "bcpkix-jdk15on" % bouncyCastleVersion)
}

resolvers += "Jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.chimenchen" % "jchmlib" % "v0.5.4"

resolvers += "mobi-api4java-mvn-repo" at "https://raw.github.com/rrauschenbach/mobi-api4java/mvn-repo"
libraryDependencies += "org.rr" % "mobi-api4java" % "0.0.2-SNAPSHOT"