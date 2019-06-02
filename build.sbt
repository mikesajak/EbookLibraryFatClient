import sbt.CrossVersion
import sbt.Keys.libraryDependencies

name := "EbookLibraryFatClient"

version := "0.1"

scalaVersion := "2.12.8"

scalacOptions += "-Ypartial-unification"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

//resolvers += "PSiegman repo with Epublib" at "https://github.com/psiegman/mvn-repo/raw/master/releases"

//unmanagedBase := baseDirectory.value / "custom_lib"

// Add dependency on ScalaFX library
libraryDependencies += "org.scalafx" %% "scalafx" % "12.0.1-R17"

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
  val scalafxmlVersion = "0.4"
  Seq("org.scalafx" %% "scalafxml-core-sfx8" % scalafxmlVersion,
      "org.scalafx" % "scalafxml-guice-sfx8_2.12" % scalafxmlVersion)
  // todo: cleanup %% / %
}



libraryDependencies ++= { // guice dependency injection
  val guiceVersion = "4.2.2"
  Seq("com.google.inject" % "guice" % guiceVersion,
      "net.codingwell" %% "scala-guice" % guiceVersion)
}

// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "25.1-jre"

libraryDependencies ++= { // logging
  Seq("com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3")
}

// ICU4J https://mvnrepository.com/artifact/com.ibm.icu/icu4j
libraryDependencies += "com.ibm.icu" % "icu4j" % "60.2"

libraryDependencies += "com.beachape" %% "enumeratum" % "1.5.13"

libraryDependencies ++= { // yaml parser
  val circeVersion = "0.11.1"
  Seq("io.circe" %% "circe-yaml" % "0.10.0",//circeVersion,
//      "io.circe" %% "circe-optics" % "0.11.0", //circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "org.typelevel" %% "cats-core" % "1.5.0")
  //libraryDependencies += "io.circe" %% "circe-config" % "0.6.1"
}

libraryDependencies ++= { // spring boot
  val springBootVersion = "2.1.5.RELEASE"
  val springVersion = "5.1.7.RELEASE"
  val jacksonVersion = "2.9.9"
  Seq("org.springframework.boot" % "spring-boot-starter" % springBootVersion,
      "org.springframework" % "spring-web" % springVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
      "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
      "com.fasterxml.jackson.module" % "jackson-module-kotlin" % jacksonVersion
  )
}

libraryDependencies += "org.jetbrains.kotlin" % "kotlin-stdlib" % "1.3.31"

libraryDependencies += "org.controlsfx" % "controlsfx" % "9.0.0"

//libraryDependencies += "nl.siegmann.epublib" % "epublib-core" % "3.1"
libraryDependencies += "com.positiondev.epublib" % "epublib-core" % "3.1" excludeAll(
    ExclusionRule(organization = "org.slf4j")
)
