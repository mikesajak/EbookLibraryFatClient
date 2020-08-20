package com.mikesajak.ebooklib.app.reader

import java.io.InputStream

import com.mikesajak.ebooklib.app.dto.{BookMetadata, CoverImage}

trait BookMetadataReader {
  val mimeType: String

  def canRead(bookData: InputStream): Boolean
  def read(bookData: InputStream): BookMetadata
  def readCover(bookData: InputStream): Option[CoverImage]
}
