package com.mikesajak.ebooklib.app.rest

import java.time.LocalDateTime
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.model.ServerInfo
import com.mikesajak.ebooklib.app.rest.ConnectionStatus.{Connected, Disconnected, Warning}
import com.mikesajak.ebooklib.app.util.EventBus
import com.typesafe.scalalogging.Logger
import enumeratum.{EnumEntry, _}

import scala.collection.immutable
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

sealed trait ConnectionStatus extends EnumEntry

object ConnectionStatus extends Enum[ConnectionStatus] {
  val values: immutable.IndexedSeq[ConnectionStatus] = findValues

  case object Connected extends ConnectionStatus
  case object Warning extends ConnectionStatus
  case object Disconnected extends ConnectionStatus
}

case class ConnectionInfo(serverInfo: ServerInfo, timestamp: LocalDateTime)

case class ServerStatus(connectionStatus: ConnectionStatus,
                        connectionInfo: Option[ConnectionInfo])

object ServerStatus{
  def apply(connectionStatus: ConnectionStatus, connectionInfo: ConnectionInfo): ServerStatus =
    ServerStatus(connectionStatus, Option(connectionInfo))

  def disconnected(): ServerStatus = ServerStatus(ConnectionStatus.Disconnected, None)
}

class ServerConnectionService(serverController: BookServerController,
                              appSettings: AppSettings,
                              eventBus: EventBus) {
  private val logger = Logger[ServerConnectionService]
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  @volatile
  private var missedConnections = Int.MaxValue

  @volatile
  private var connectionInfo: ConnectionInfo = _

  private val executor = new ScheduledThreadPoolExecutor(1)
  private val task: Runnable = () => updateServerStatus()

  def startMonitoring() {
    executor.scheduleAtFixedRate(task, 0, appSettings.server.connCheckPeriod, TimeUnit.SECONDS)
  }

  def connectionStatus: ConnectionStatus = missedConnections match {
    case 0 => Connected
    case n if n >= appSettings.server.disconnectedThreshold => Disconnected
    case _ => Warning
  }

  def serverStatus: ServerStatus = ServerStatus(connectionStatus, connectionInfo)


  private def updateServerStatus(): Unit = {
    serverController.serverInfoAsync.onComplete{ triedServerInfo =>
      val oldServerStatus = serverStatus
      triedServerInfo match {
        case Success(serverInfo) =>
          missedConnections = 0
          connectionInfo = ConnectionInfo(serverInfo, LocalDateTime.now())
        case Failure(ex) =>
          logger.warn(s"Error connecting to the server: ${ex.getLocalizedMessage}")
          if (missedConnections != Int.MaxValue) missedConnections += 1
      }
      val curServerStatus = serverStatus
      eventBus.publish(curServerStatus)
      if (curServerStatus.connectionStatus == Connected && oldServerStatus.connectionStatus != Connected) {
        logger.info(s"Server reconnected: $connectionInfo")
        eventBus.publish(ServerReconnectedEvent(connectionInfo))
      }
    }
  }
}

case class ServerReconnectedEvent(connectionInfo: ConnectionInfo)