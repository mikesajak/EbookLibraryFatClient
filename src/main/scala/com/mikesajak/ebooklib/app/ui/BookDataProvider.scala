package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.dto._
import com.mikesajak.ebooklib.app.rest.BookServerController
import scalafx.scene.image.Image

import scala.concurrent.{ExecutionContextExecutor, Future}

trait BookDataProvider {
  def bookId: Option[BookId]
  def bookMetadata: BookMetadata
  def bookCover: Option[Image]
  def bookFormatsMetadata: Future[Seq[BookFormatMetadata]]
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

  override def bookFormatsMetadata: Future[Seq[BookFormatMetadata]] = {
    val eventualFormatIds = bookServerController.getBookFormatIds(book.id)
    eventualFormatIds.flatMap { formatIds =>
      val futures = formatIds.map { id => bookServerController.getBookFormatMetadata(book.id, id) }
      Future.sequence(futures)
    }
  }

  override def bookFormat(formatId: BookFormatId): BookFormat = ???
}
