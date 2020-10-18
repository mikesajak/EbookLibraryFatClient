package com.mikesajak.ebooklib.app.rest

import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.stream.{ActorMaterializer, Materializer}
import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.dto.BookDto
import com.mikesajak.ebooklib.app.model._
import com.typesafe.scalalogging.Logger
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.syntax.EncoderOps
import net.softler.client.ClientRequest
import scalafx.scene.image.Image

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class BookServerControllerAkkaHttpCirce(appSettings: AppSettings, bookDtoConverter: BookDtoConverter)
    extends BookServerController with FailFastCirceSupport {

  private val logger = Logger[BookServerControllerAkkaHttpCirce]

  implicit lazy val system: ActorSystem = ActorSystem()
  implicit lazy val materializer: Materializer = ActorMaterializer()
  implicit lazy val executionContext: ExecutionContext = system.dispatcher
  import io.circe.generic.auto._

  def serverInfoAsync: Future[ServerInfo] = {
    val url = s"${appSettings.server.address}/info"
    logger.debug(s"Requesting server status: GET $url")
    ClientRequest(url).withJson.get[ServerInfo]
                      .map { result =>
                        logger.debug(s"Received server status response: $result")
                        result
                      }
  }

  def serverInfo: ServerInfo = {
    Await.result(serverInfoAsync, 2.seconds)
  }

  def listBooks(): Future[Seq[Book]] = {
    val url = s"${appSettings.server.address}/books"
    logger.debug(s"Requesting book list: GET $url")
    ClientRequest(url).withJson
                      .get[Seq[BookDto]]
                      .map { result =>
                        logger.debug(s"Received book list response: $result")
                        result.map(bookDtoConverter.bookFromDto)
                      }
  }

  def searchBooks(searchQuery: String): Future[Seq[Book]] = {
    val url = s"${appSettings.server.address}/books?query=${URLEncoder.encode(searchQuery, "UTF8")}"
    logger.debug(s"Requesting book search: GET $url")
    ClientRequest(url).withJson
                      .get[Seq[BookDto]]
                      .map { result =>
                        logger.debug(s"Received book search response: $result")
                        result.map(bookDtoConverter.bookFromDto)
                      }
  }

  def getBook(id: BookId): Future[Book] = {
    val url = s"${appSettings.server.address}/books/${id.value}"
    logger.debug(s"Requesting book data: GET $url")
    ClientRequest(url).withJson
                      .get[BookDto]
                      .map { result =>
                        logger.debug(s"Received book data response: $result")
                        bookDtoConverter.bookFromDto(result)
                      }
  }

  def addBook(bookMetadata: BookMetadata): Future[BookId] = {
    val url = s"${appSettings.server.address}/books"
    logger.debug(s"Adding book data: POST $url")
    val jsonString = bookMetadata.asJson.toString
    val clientResponse = ClientRequest(url).withText
                                           .entity(jsonString)
                                           .asJson
                                           .post()
                                           .map { result =>
                                             logger.debug(s"Received add book response: $result")
                                             result
                                           }

    clientResponse.flatMap { response =>
      println(response.status)
      val body = response.decoded.entity.toStrict(10.seconds).map(_.data.utf8String)
      response.status match {
        case StatusCodes.OK =>
          body.map(BookId(_))
        case code @ _ =>
          logger.info(s"""Received statusCode="$code" while adding book. Response body: $body""")
//          response.response.discardEntityBytes()
//          throw new AddBookException(s"$body")
          body.flatMap(bodyStr => Future.failed(new AddBookException(bodyStr)))
      }
    }
  }

  def deleteBook(bookId: BookId): Future[Unit] = {
    val url = s"${appSettings.server.address}/books"
    ClientRequest(url).entity(bookId.value)
                      .asJson
                      .post[String]
                      .map(_ => ())
  }

  def getBookCover(bookId: BookId): Option[Image] = {
    val coverImgUrl = s"${appSettings.server.address}/coverImages/$bookId"
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

  def getBookFormatIds(bookId: BookId): Future[Seq[BookFormatId]] = {
    val url = s"${appSettings.server.address}/bookFormats/${bookId.value}"
    logger.debug(s"Requesting book format ids list: GET $url")
    ClientRequest(url).withJson
                      .get[Seq[BookFormatId]]
                      .map { result =>
                        logger.debug(s"Received book format ids list response: $result")
                        result
                      }
  }

  def getBookFormatMetadata(bookId: BookId, formatId: BookFormatId): Future[BookFormatMetadata] = {
    val url = s"${appSettings.server.address}/bookFormats/${bookId.value}/${formatId.value}/metadata"
    logger.debug(s"Requesting book format metadata: GET $url")
    ClientRequest(url).withJson
                      .get[BookFormatMetadata]
                      .map { result =>
                         logger.debug(s"Received book format metadata response: $result")
                         result
                       }
  }

  def addBookFormat(bookId: BookId, bookFormat: BookFormat): Future[String] = {
    val url = s"${appSettings.server.address}/bookFormats/${bookId.value}"
    logger.debug(s"Adding book format: POST $url")
    ClientRequest(url).entity(bookFormat.asJson.toString)
                      .asJson
                      .post[String]
                      .map { result =>
                        logger.debug(s"Received add book response: $result")
                        result
                      }
  }


  override def getBookFormatsMetadata(bookId: BookId): Future[Seq[BookFormatMetadata]] = ???

  override def getBookFormat(formatId: BookFormatId): Future[BookFormat] = ???

  override def deleteBookCover(bookId: BookId): Future[Unit] = ???

  override def deleteBookFormat(bookId: BookId, formatId: BookFormatId): Future[Unit] = ???
}
