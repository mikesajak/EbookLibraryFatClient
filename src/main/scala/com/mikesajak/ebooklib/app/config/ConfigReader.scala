package com.mikesajak.ebooklib.app.config

import java.io.{FileReader, FileWriter}

import cats.syntax.either._
import com.mikesajak.ebooklib.app.util.Util._
import io.circe.Error
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import io.circe.syntax._
import io.circe.yaml.Printer.FlowStyle

class ConfigReader {
  private val printer = io.circe.yaml.Printer(dropNullKeys = true,
                                              indent = 2,
                                              preserveOrder = true,
                                              sequenceStyle = FlowStyle.Flow)

  def read(filename: String) : AppSettings = {
    implicit val config: Configuration = Configuration.default.withDefaults

    val reader = new FileReader(filename)
    val json = io.circe.yaml.parser.parse(reader)

    json.leftMap(err => err: Error)
        .flatMap(_.as[AppSettings])
        .valueOr(throw _)
  }

  def write(appSettings: AppSettings, filename: String): Unit = {
    using(new FileWriter(filename)) { writer =>
      implicit val config: Configuration = Configuration.default

      val json = appSettings.asJson
      writer.write(printer.pretty(json))
    }
  }
}

object ConfigReader {
  def main(args: Array[String]): Unit = {
    val cfgReader = new ConfigReader()
    val appSettings = cfgReader.read("config.yaml")

    cfgReader.write(appSettings, "config2.yaml")
  }
}