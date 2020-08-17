package com.mikesajak.ebooklib.app.rest

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.dto._
import com.typesafe.scalalogging.Logger
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import net.softler.client.ClientRequest
import scalafx.scene.image.Image

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class BookServerController(appSettings: AppSettings) extends FailFastCirceSupport {

  private val logger = Logger[BookServerController]

  implicit lazy val system: ActorSystem = ActorSystem()
  implicit lazy val materializer: Materializer = ActorMaterializer()
  implicit lazy val executionContext: ExecutionContext = system.dispatcher
  import io.circe.generic.auto._

  def serverInfoAsync: Future[ServerInfo] = {
    val url = s"${appSettings.server.address}/status"
    logger.debug(s"Requesting server status: GET $url")
    ClientRequest(url).withJson.get[ServerInfo]
                      .map { result =>
                        logger.debug(s"Received server status response: $result")
                        result
                      }
  }

  def listBooksAsync(): Future[Seq[Book]] = {
    val url = s"${appSettings.server.address}/books"
    logger.debug(s"Requesting book list: GET $url")
    ClientRequest(url).withJson.get[Seq[Book]]
                      .map { result =>
                        logger.debug(s"Received book list response: $result")
                        result
                      }
  }

  def getBookAsync(id: BookId): Future[Book] = {
    val url = s"${appSettings.server.address}/books/${id.value}"
    logger.debug(s"Requesting book data: GET $url")
    ClientRequest(url).withJson.get[Book]
                      .map { result =>
                        logger.debug(s"Received book data response: $result")
                        result
                      }
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
    ClientRequest(url).withJson.get[Seq[BookFormatId]]
                      .map { result =>
                        logger.debug(s"Received book format ids list response: $result")
                        result
                      }
  }

  def getBookFormatMetadata(bookId: BookId, formatId: BookFormatId): Future[BookFormatMetadata] = {
    val url = s"${appSettings.server.address}/bookFormats/${bookId.value}/${formatId.value}/metadata"
    logger.debug(s"Requesting book format metadata: GET $url")
    ClientRequest(url).withJson.get[BookFormatMetadata]
                      .map { result =>
                        logger.debug(s"Received book format metadata response: $result")
                        result
                      }
  }

}
