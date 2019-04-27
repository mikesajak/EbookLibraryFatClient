package com.mikesajak.ebooklib.app.rest

import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklibrary.payload.{Book, BookId, ServerInfo}
import org.springframework.web.client.RestTemplate
import scalafx.scene.image.Image

class BookServerController(serverRestTemplate: RestTemplate, appSettings: AppSettings) {

  def serverInfo(): ServerInfo = {
    serverRestTemplate.getForObject("/status", classOf[ServerInfo])
  }

  def listBooks(): Seq[Book] = {
    serverRestTemplate.getForObject("/books", classOf[Array[Book]])
  }

  def getBook(id: BookId): Book = {
    serverRestTemplate.getForObject(s"/books/${id.getValue}", classOf[Book])
  }

  def getBookCover(id: BookId) = {
    val image =
      try {
        new Image(s"${appSettings.server.address}/coverImages/${id.getValue}", true)
      } catch {
        case e: Exception => null
      }

    Option(image)
  }

}
