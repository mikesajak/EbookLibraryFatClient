package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.model._
import com.mikesajak.ebooklib.app.rest.BookServerController
import scalafx.scene.image.Image

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor}

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
  private implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  override def bookId: Option[BookId] = Some(book.id)

  override def bookMetadata: BookMetadata = book.metadata

  override def bookCover: Option[Image] = bookServerController.getBookCover(book.id)

  override def bookFormatsMetadata: Seq[BookFormatMetadata] = {
    val eventualMetadatas = bookServerController.getBookFormatsMetadata(book.id)
    Await.result(eventualMetadatas, 3.seconds)
  }

  override def bookFormat(formatId: BookFormatId): BookFormat = {
//    bookServerController.getBookFormatsMetadata()
    ???
  }
}
