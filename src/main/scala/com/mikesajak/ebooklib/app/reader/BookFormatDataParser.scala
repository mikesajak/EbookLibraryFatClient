package com.mikesajak.ebooklib.app.reader

import java.io.InputStream

import com.mikesajak.ebooklib.app.model.CoverImage

trait BookFormatDataParser {
  def acceptContentType(contentType: String): Boolean
  def read(bookDataInputStream: InputStream): Either[Throwable, BookFormatData]
  def readCover(bookDataInputStream: InputStream): Option[CoverImage]
}

