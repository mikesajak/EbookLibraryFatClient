package com.mikesajak.ebooklib.app.rest

import java.time.LocalDateTime
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.rest.ConnectionStatus.{Connected, Disconnected, Warning}
import com.mikesajak.ebooklib.app.util.EventBus
import com.mikesajak.ebooklibrary.payload.ServerInfo
import enumeratum.{EnumEntry, _}

import scala.collection.immutable

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

  def disconnected() = ServerStatus(ConnectionStatus.Disconnected, None)
}

class ServerConnectionController(serverController: BookServerController,
                                 appSettings: AppSettings,
                                 eventBus: EventBus) {
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

  def serverStatus = ServerStatus(connectionStatus, connectionInfo)

  private def updateServerStatus(): Unit = {
    executor.submit(new Runnable() {
      def run(): Unit = {
        try {
          val serverInfo = serverController.serverInfo()
          missedConnections = 0
          connectionInfo = ConnectionInfo(serverInfo, LocalDateTime.now())
        } catch {
          case e: Exception =>
            if (missedConnections != Int.MaxValue) missedConnections += 1
        }
        eventBus.publish(serverStatus)
      }
    })
  }
}


