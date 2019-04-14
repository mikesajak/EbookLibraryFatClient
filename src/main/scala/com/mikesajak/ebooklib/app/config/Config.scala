package com.mikesajak.ebooklib.app.config

class Config(configReader: ConfigReader) {
  val configFile = "config.yaml"

  private var settings0: AppSettings = _

  def load() = {
    settings0 = configReader.read(configFile)
  }
  def save() = {
    configReader.write(settings0, configFile)
  }

  def settings: AppSettings = settings0

}
