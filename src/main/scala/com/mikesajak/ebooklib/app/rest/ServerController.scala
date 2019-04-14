package com.mikesajak.ebooklib.app.rest

import java.time.LocalDateTime
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.util.EventBus
import com.mikesajak.ebooklibrary.payload.{Book, BookId, ServerInfo}
import org.springframework.web.client.RestTemplate

class ServerController(appSettings: AppSettings, serverRestTemplate: RestTemplate,
                       eventBus: EventBus) {

  private var lastServerInfo: ServerInfo = _
  private var lastServerTime: LocalDateTime = _

  private val executor = new ScheduledThreadPoolExecutor(1)
  private val task = new Runnable {
    def run(): Unit = {
      updateServerStatus()
    }
  }

  def startMonitoring() {
    executor.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS)
  }

  def updateServerStatus(): Unit = {
    executor.submit(new Runnable() {
      def run(): Unit = {
        lastServerInfo = serverInfo()
        lastServerTime = LocalDateTime.now()
        eventBus.publish(ServerStatus(Option(lastServerTime), Option(lastServerInfo)))
      }
    })
  }

  def serverInfo(): ServerInfo = {
    serverRestTemplate.getForObject("/status", classOf[ServerInfo])
  }

  def listBooks(): Seq[Book] = {
    serverRestTemplate.getForObject("/books", classOf[Array[Book]])
  }

  def getBook(id: BookId): Book = {
    serverRestTemplate.getForObject(s"/books/${id.getValue}", classOf[Book])
  }

}
