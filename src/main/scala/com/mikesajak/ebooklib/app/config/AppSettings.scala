package com.mikesajak.ebooklib.app.config

case class AppSettings(var confirmExit: Boolean = true,
                       var window: WindowSettings = DefaultSettings.window,
                       var server: ServerSettings = DefaultSettings.server,
                       booksTable: BooksTableSettings = DefaultSettings.booksTable)

object DefaultSettings {
  val window = WindowSettings(1200, 400)
  val server = ServerSettings("http://localhost:8080", 15, 3)
  val booksTable = BooksTableSettings(Array(100, 150, 100,  150, 100, 100, 100,  150))
}

case class WindowSettings(width: Int, height: Int)

case class ServerSettings(address: String, connCheckPeriod: Int, disconnectedThreshold: Int)

case class BooksTableSettings(columnWidths: Array[Int])
