package com.mikesajak.ebooklib.app.rest

import com.mikesajak.ebooklib.app.model._
import scalafx.scene.image.Image

import scala.concurrent.Future

trait BookServerController {
  def serverInfoAsync: Future[ServerInfo]

  def listBooks(): Future[Seq[Book]]

  def searchBooks(searchQuery: String): Future[Seq[Book]]

  def getBook(id: BookId): Future[Book]

  def addBook(bookMetadata: BookMetadata): Future[BookId]

  def deleteBook(bookId: BookId): Future[Unit]

  def getBookCover(bookId: BookId): Option[Image]

  def deleteBookCover(bookId: BookId): Future[Unit]

  def getBookFormatIds(bookId: BookId): Future[Seq[BookFormatId]]

  def deleteBookFormat(bookId: BookId, formatId: BookFormatId): Future[Unit]

  def getBookFormatMetadata(bookId: BookId, formatId: BookFormatId): Future[BookFormatMetadata]

  def getBookFormatsMetadata(bookId: BookId): Future[Seq[BookFormatMetadata]]

  def getBookFormat(formatId: BookFormatId): Future[BookFormat]

  def addBookFormat(bookId: BookId, bookFormat: BookFormat): Future[String]
}

class AddBookException(message: String) extends Exception(message)
class AddBookFormatException(message: String) extends Exception(message)
class DeleteBookException(message: String) extends Exception(message)
class DeleteBookFormatException(message: String) extends Exception(message)
class DeleteBookCoverException(message: String) extends Exception(message)