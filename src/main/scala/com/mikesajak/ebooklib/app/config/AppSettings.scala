package com.mikesajak.ebooklib.app.config

import scala.collection.mutable

case class AppSettings(var confirmExit: Boolean = true,
                       var window: WindowSettings = DefaultSettings.window,
                       var server: ServerSettings = DefaultSettings.server,
                       booksTable: BooksTableSettings = DefaultSettings.booksTable)

object DefaultSettings {
  val window: WindowSettings = WindowSettings(1200, 400)
  val server: ServerSettings = ServerSettings("http://localhost:8080", 15, 3)
  val booksTable: BooksTableSettings = BooksTableSettings(mutable.Map())//Array(100, 150, 100, 150, 100, 100, 100, 150))
}

case class WindowSettings(width: Int, height: Int) {
  def setWidth(newWidth: Int): WindowSettings = WindowSettings(newWidth, height)
  def setHeight(newHeight: Int): WindowSettings = WindowSettings(width, newHeight)
}

case class ServerSettings(address: String, connCheckPeriod: Int, disconnectedThreshold: Int) {
  def setAddress(newAddress: String): ServerSettings = ServerSettings(newAddress, connCheckPeriod, disconnectedThreshold)
}

case class BooksTableSettings(columnWidths: mutable.Map[String, Int])
