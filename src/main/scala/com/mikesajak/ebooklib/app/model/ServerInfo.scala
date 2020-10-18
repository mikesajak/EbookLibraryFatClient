package com.mikesajak.ebooklib.app.model

case class ServerInfo(name: String, version: String) {
  override def toString = s"$name:$version"
}

case class LibraryInfo(numBooks: Int, numFormats: Int, numCovers: Int, numAuthors: Int)
