package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.model._
import scalafx.scene.image.Image

import scala.util.Try

trait BooksProvider {
  def readBooks(searchQuery: Option[String]): Try[Seq[Book]]

  def getBookDataProvider(book: Book): BookDataProvider
}

trait BookDataProvider {
  def bookId: Option[BookId]
  def bookMetadata: BookMetadata
  def bookCover: Option[Image]
  def bookFormatsMetadata: Seq[BookFormatMetadata]
  def bookFormat(formatId: BookFormatId): BookFormat
}
