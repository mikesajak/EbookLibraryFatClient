package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.model._
import com.mikesajak.ebooklib.app.rest.BookServerService
import scalafx.scene.image.Image
import scribe.Logging

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.Try

class BookServerBooksProvider(bookServerService: BookServerService) extends BooksProvider with Logging {

  def readBooks(searchQuery: Option[String]=None): Try[Seq[Book]] = Try {
    logger.debug("Loading books in the background")
    val booksResultFuture = searchQuery.map(query => bookServerService.searchBooks(query))
                                       .getOrElse(bookServerService.listBooks())

    Await.result(booksResultFuture, 3.seconds)
  }

  def getBookDataProvider(book: Book): BookDataProvider =
    new ServerBookDataProvider(book)(bookServerService)
}

class ServerBookDataProvider(book: Book)(bookServerService: BookServerService) extends BookDataProvider {
  private implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  override def bookId: Option[BookId] = Some(book.id)

  override def bookMetadata: BookMetadata = book.metadata

  override def bookCover: Option[Image] = bookServerService.getBookCover(book.id)

  override def bookFormatsMetadata: Seq[BookFormatMetadata] = {
    val eventualMetadatas = bookServerService.getBookFormatsMetadata(book.id)
    Await.result(eventualMetadatas, 3.seconds)
  }

  override def bookFormat(formatId: BookFormatId): BookFormat = {
    val eventualFormatMetadata = bookServerService.getBookFormatMetadata(formatId)
    val eventualFormatData = bookServerService.getBookFormat(formatId)
    val metadata = Await.result(eventualFormatMetadata, 3.seconds)
    val bytes = Await.result(eventualFormatData, 3.seconds)
    BookFormat(metadata, bytes)
  }
}
