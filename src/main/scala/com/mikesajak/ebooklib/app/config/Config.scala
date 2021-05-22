package com.mikesajak.ebooklib.app.config

import com.google.common.eventbus.Subscribe
import com.mikesajak.ebooklib.app.config.ConfigEvents.{BookTableColumnWidthChange, WindowSizeChange}
import scribe.Logging

class Config(configReader: ConfigReader) extends Logging {
  val configFile = "config.yaml"

  private var settings0: AppSettings = _

  def load() = {
    logger.info("Loading config...")
    settings0 = configReader.read(configFile)
  }
  def save() = {
    logger.info("Saving config...")
    configReader.write(settings0, configFile)
  }

  def settings: AppSettings = settings0

  @Subscribe
  def updateSetting(event: ConfigEvent): Unit = {
    logger.debug(s"Update settings: $event")
    event match {
      case BookTableColumnWidthChange(colId, width) => settings0.booksTable.columnWidths(colId) = width
      case WindowSizeChange(width, height) => settings0.window = WindowSettings(width, height)
    }
  }


}
sealed trait ConfigEvent
object ConfigEvents {
  case class BookTableColumnWidthChange(columnId: String, width: Int) extends ConfigEvent
  case class WindowSizeChange(width: Int, height: Int) extends ConfigEvent
}
