package com.mikesajak.ebooklib.app.rest

import com.google.inject.name.{Named, Names}
import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.dto.{BookDto, BookFormatMetadataDto, ErrorResponse}
import com.mikesajak.ebooklib.app.model._
import com.mikesajak.ebooklib.app.{ApplicationContext, ScribeCfg}
import io.circe.generic.auto._
import net.codingwell.scalaguice.InjectorExtensions.ScalaInjector
import scalafx.scene.image.Image
import scribe.Logging
import sttp.client3._
import sttp.client3.circe.{asJson, _}
import sttp.model.StatusCode

import scala.concurrent.{ExecutionContext, Future}

class BookServerServiceSttp(appSettings: AppSettings, bookDtoConverter: BookDtoConverter,
                            @Named("httpCallExecutionContext") httpCallExecutionContext: ExecutionContext)
    extends BookServerService with Logging {

  private implicit val ec: ExecutionContext = // for some reason injecting via @Named parameter does not work, so I have to inject manually :(
    ApplicationContext.globalInjector.instance[ExecutionContext](Names.named("httpCallExecutionContext"))

  private val rawHttpBackend = HttpURLConnectionBackend()
  private val httpBackend = ScribeCfg.debugScribeSttpBackendWrapper(rawHttpBackend)

  private def serverRequest: RequestT[Empty, Either[String, String], Any] = basicRequest.header("Accept", "application/json")

  override def serverInfoAsync: Future[ServerInfo] = Future {
    val serverInfoUri = uri"${appSettings.server.address}/info"
    logger.trace(s"Requesting server status: GET $serverInfoUri")
    val request = serverRequest.get(serverInfoUri)
                               .response(asJson[ServerInfo])

    try {
      request.send(rawHttpBackend)
             .body.getOrThrowException("Get server status")
    } catch {
      case e: Exception =>
        logger.warn(s"Error getting server status: ${e.getLocalizedMessage}")
        throw e
    }
  }

  def searchBooks(searchQuery: String): Future[Seq[Book]] = Future {
    searchBooks(Some(searchQuery)).getOrThrowException(s"Search books with query: $searchQuery")
  }

  override def listBooks(): Future[Seq[Book]] = Future {
    searchBooks(None).getOrThrowException("List books")
  }

  private def searchBooks(searchQuery: Option[String]): Either[ResponseException[String, io.circe.Error], Seq[Book]] = {
    val searchBooksUri = uri"${appSettings.server.address}/books?query=$searchQuery"
    logger.debug(s"Requesting book search: GET $searchBooksUri")
    val request = serverRequest.get(searchBooksUri)
                               .response(asJson[Seq[BookDto]])
//                               .response(asBoth(asJson[Seq[BookDto]], asStringAlways))
                               .mapResponseRight(bookDtos => bookDtos.map(bookDtoConverter.bookFromDto))

//    val response: Response[(Either[ResponseException[String, circe.Error], Seq[BookDto]], String)] = request.send(httpBackend)

//    val (parsed, raw) = response.body
//    logger.debug(s"Received searchBooks response: $parsed, raw: $raw")

    request.send(httpBackend)
           .body
//           .getOrThrowException()
  }

  override def getBook(bookId: BookId): Future[Book] = Future {
    val getBookUri = uri"${appSettings.server.address}/books/$bookId"
    logger.debug(s"Requesting book data: GET $getBookUri")
    val request = serverRequest.get(getBookUri)
                               .response(asJson[BookDto])
                               .mapResponseRight(bookDtoConverter.bookFromDto)

    request.send(httpBackend)
           .body
           .getOrThrowException()
  }

  override def addBook(bookMetadata: BookMetadata): Future[BookId] = Future {
    val addBookUri = uri"${appSettings.server.address}/books"
    logger.debug(s"Adding book data: POST $addBookUri")

    val request = serverRequest.post(addBookUri)
                               .body(bookMetadata)
                               .response(asString)
                               .mapResponseRight(BookId(_))

    request.send(httpBackend)
           .body
           .getOrThrowException("Get book data", msg => new AddBookException(msg))
  }

  override def deleteBook(bookId: BookId): Future[Unit] = Future {
    val removeBookUri = uri"${appSettings.server.address}/books/$bookId"
    logger.debug(s"Removing book: DELETE $removeBookUri")

    val request = serverRequest.delete(removeBookUri)
                               .response(asJsonEither[ErrorResponse, Empty[Nothing]])


    request.send(httpBackend)
           .body
           .getOrThrowException(s"Delete book with bookId=$bookId")
  }

  override def getBookCover(bookId: BookId): Option[Image] = {
    val coverImgUrl = s"${appSettings.server.address}/books/$bookId/cover"
    val image =
      try {
        new Image(coverImgUrl, true)
      } catch {
        case ex: Exception =>
          logger.info(s"Error fetching image for $coverImgUrl", ex)
          null
      }

    Option(image)
  }


  override def deleteBookCover(bookId: BookId): Future[Unit] = Future {
    val removeCoverUri = uri"${appSettings.server.address}/books/$bookId/cover"
    logger.debug(s"Removing book cover: DELETE $removeCoverUri")

    val request = serverRequest.delete(removeCoverUri)

    request.send(httpBackend)
           .body
           .getOrThrowException(s"Delete book cover for bookId=$bookId", msg => new DeleteBookCoverException(msg))
  }

//  override def getBookFormatIds(bookId: BookId): Future[Seq[BookFormatId]] = Future {
//    val bookFormatsUri = uri"${appSettings.server.address}/bookFormats/$bookId"
//    logger.debug(s"Requesting book format Ids: GET $bookFormatsUri")
//    val request = serverRequest.get(bookFormatsUri)
//                               .response(asJson[Seq[BookFormatId]])
//
//    request.send(httpBackend)
//           .body
//           .getOrThrowException("Get book format ids")
//  }

  override def getBookFormatMetadata(formatId: BookFormatId): Future[BookFormatMetadata] = Future {
    val bookFormatMetadataUri = uri"${appSettings.server.address}/bookFormats/$formatId"
    logger.debug(s"Requesting book format metadata: GET $bookFormatMetadataUri")
    val request = serverRequest.get(bookFormatMetadataUri)
                               .response(asJson[BookFormatMetadataDto])
                               .mapResponseRight(bookDtoConverter.bookFormatMetadataFromDto)

    request.send(httpBackend)
           .body
           .getOrThrowException("Get book format metadata")
  }

  override def getBookFormatsMetadata(bookId: BookId): Future[Seq[BookFormatMetadata]] = Future {
    val bookFormatsMetadataUri = uri"${appSettings.server.address}/books/$bookId/formats"
    logger.debug(s"Requesting book formats metadata: GET $bookFormatsMetadataUri")
    val request = serverRequest.get(bookFormatsMetadataUri)
                               .response(asJson[Seq[BookFormatMetadataDto]])
                               .mapResponseRight(dtos => dtos.map(bookDtoConverter.bookFormatMetadataFromDto))

    request.send(httpBackend)
           .body
           .getOrThrowException("Get book formats metadata")
  }

  override def getBookFormat(formatId: BookFormatId): Future[Array[Byte]] = Future {
    val bookFormatUri = uri"${appSettings.server.address}/bookFormats/$formatId/contents"
    logger.debug(s"Requesting book format contents: GET $bookFormatUri")
    val request = serverRequest.get(bookFormatUri)
                               .response(asByteArray)

    val body = request.send(httpBackend)
                      .body
    body
           .getOrThrowException("Get book formats metadata", msg => new BookFormatFetchErrorException(msg))
  }

  override def addBookFormat(bookId: BookId, bookFormat: BookFormat): Future[String] = Future {
    val addBookFormatMetadataUri = uri"${appSettings.server.address}/books/$bookId/formats"
    logger.debug(s"Adding book format: POST $addBookFormatMetadataUri")
    val request = serverRequest.post(addBookFormatMetadataUri)
                               .multipartBody(multipart("formatType", bookFormat.metadata.formatType),
                                              multipart("file", bookFormat.contents).fileName(bookFormat.metadata.filename.getOrElse("unknown")))
                               .response(asString)

    request.send(httpBackend)
           .body
           .getOrThrowException("Add book format metadata", msg => new AddBookFormatException(msg))
  }


  override def deleteBookFormat(formatId: BookFormatId): Future[Unit] = Future {
    val deleteBookFormatUri = uri"${appSettings.server.address}/bookFormats/$formatId"
    logger.debug(s"Deleting book format: DELETE $deleteBookFormatUri")
    val request = serverRequest.get(deleteBookFormatUri) // FIXME: get??
    request.send(httpBackend) match {
      case response if response.code == StatusCode.Ok =>
      case response => throw new DeleteBookFormatException(s"Error while deleting book format, formatId=$formatId. Response: $response")
    }

    request.send(httpBackend)
           .body
           .getOrThrowException(s"Delete book format, formatId=$formatId", msg => new DeleteBookFormatException(msg))
  }

  implicit class EitherPlus[T](either: Either[Throwable, T]) {
    def getOrThrowException(): T = either match {
      case Right(result) =>
        result
      case Left(ex) =>
        throw ex
    }

    def getOrThrowException(contextMessage: => String): T = either match {
      case Right(result) =>
//        logger.debug(s"SUCCESS $contextMessage: $result")
        result
      case Left(ex) =>
        logger.warn(s"FAILURE $contextMessage", ex)
        throw ex
    }
  }

  implicit class EitherPlus2[T](either: Either[String, T]) {
    def getOrThrowException(exceptionCreator: ExceptionCreator): T = either match {
      case Right(result) => result
      case Left(errorMessage) => throw exceptionCreator(errorMessage)
    }

    def getOrThrowException(contextMessage: => String, exceptionCreator: ExceptionCreator): T = either match {
      case Right(result) =>
//        logger.debug(s"SUCCESS $contextMessage: $result")
        result
      case Left(errorMessage) =>
        logger.warn(s"FAILURE $contextMessage: Error message: $errorMessage")
        throw exceptionCreator(errorMessage)
    }
  }

  type ExceptionCreator = String => Exception

}
