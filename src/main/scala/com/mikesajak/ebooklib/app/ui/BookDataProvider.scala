package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklibrary.payload.{BookFormat, BookFormatMetadata, BookId, BookMetadata}
import scalafx.scene.image.Image

trait BookDataProvider {
  def bookId: BookId
  def bookMetadata: BookMetadata
  def bookCover: Option[Image]
  def bookFormatsMetadata: Seq[BookFormatMetadata]
  def bookFormat(): BookFormat
}
