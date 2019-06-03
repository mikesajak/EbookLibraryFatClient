package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.rest.BookServerController
import com.mikesajak.ebooklibrary.payload._
import scalafx.scene.image.Image

trait BookDataProvider {
  def bookId: Option[BookId]
  def bookMetadata: BookMetadata
  def bookCover: Option[Image]
  def bookFormatsMetadata: Seq[BookFormatMetadata]
  def bookFormat(formatId: BookFormatId): BookFormat
}


class BookDataProviderFactory(bookServerController: BookServerController) {
  def getServerBookDataProvider(book: Book): ServerBookDataProvider =
    new ServerBookDataProvider(book)(bookServerController)
}

class ServerBookDataProvider(book: Book)(bookServerController: BookServerController) extends BookDataProvider {
  override def bookId: Option[BookId] = Some(book.getId)

  override def bookMetadata: BookMetadata = book.getMetadata

  override def bookCover: Option[Image] = bookServerController.getBookCover(book.getId)

  override def bookFormatsMetadata: Seq[BookFormatMetadata] = {
    val formatIds = bookServerController.getBookFormatIds(book.getId)
    formatIds.map { id => bookServerController.getBookFormatMetadata(book.getId, id) }
  }

  override def bookFormat(formatId: BookFormatId): BookFormat = ???
}
