package com.mikesajak.ebooklib.app.dto

case class ServerInfo(name: String, version: String, numBooks: Long, numFormats: Long, numCovers: Long) {
  override def toString = s"$name:$version ($numBooks books, $numFormats formats, $numCovers covers)"
}
