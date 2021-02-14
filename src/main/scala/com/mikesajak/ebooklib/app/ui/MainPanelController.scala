package com.mikesajak.ebooklib.app.ui

import com.google.common.eventbus.Subscribe
import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.rest.{BookServerService, ConnectionStatus, ServerConnectionService, ServerStatus}
import com.mikesajak.ebooklib.app.util.EventBus
import com.typesafe.scalalogging.Logger
import scalafx.application.Platform
import scalafx.scene.control.Label
import scalafx.scene.image.ImageView
import scalafxml.core.macros.{nested, sfxml}

//noinspection UnstableApiUsage
@sfxml
class MainPanelController(serverStatusLabel: Label,
                          @nested[BookTableControllerImpl] bookTableController: BookTableController,

                          appSettings: AppSettings,
                          eventBus: EventBus,
                          bookServerService: BookServerService,
                          serverConnectionController: ServerConnectionService,
                          implicit val resourceMgr: ResourceManager) {
  private val logger = Logger[MainPanelController]

  bookTableController.init(new BookServerBooksProvider(bookServerService))

  eventBus.register(this)

  serverConnectionController.startMonitoring()

  @Subscribe
  def serverStatusChange(status: ServerStatus): Unit = {
    Platform.runLater {
      status match {
        case ServerStatus(ConnectionStatus.Connected, Some(connInfo)) =>
          setServerStatusIcon("icons8-connected-40.png")
          val serverInfo = s"${connInfo.serverInfo.name}:${connInfo.serverInfo.version}"
          serverStatusLabel.text = resourceMgr.getMessage("main_panel.status.connected.label",
                                                          appSettings.server.address, serverInfo)
          serverStatusLabel.tooltip = resourceMgr.getMessage("main_panel.status.connected.tooltip",
                                                             appSettings.server.address, serverInfo, connInfo.timestamp)

        case ServerStatus(ConnectionStatus.Warning, Some(connInfo)) =>
          setServerStatusIcon("icons8-error-48.png")
          val serverInfo = s"${connInfo.serverInfo.name}:${connInfo.serverInfo.version}"
          serverStatusLabel.text = resourceMgr.getMessage("main_panel.status.connected_warning.label",
                                                          appSettings.server.address, serverInfo)
          serverStatusLabel.tooltip = resourceMgr.getMessage("main_panel.status.connected_warning.tooltip",
                                                             appSettings.server.address, serverInfo, connInfo.timestamp)

        case ServerStatus(ConnectionStatus.Disconnected, Some(connInfo)) =>
          setServerStatusIcon("icons8-disconnected-40.png")
          val serverInfo = s"${connInfo.serverInfo.name}:${connInfo.serverInfo.version}"
          serverStatusLabel.text = resourceMgr.getMessage("main_panel.status.disconnected.label",
                                                          appSettings.server.address, serverInfo)
          serverStatusLabel.tooltip = resourceMgr.getMessage("main_panel.status.disconnected.tooltip",
                                                             appSettings.server.address, serverInfo, connInfo.timestamp)

        case ServerStatus(ConnectionStatus.Disconnected, None) =>
          setServerStatusIcon("icons8-disconnected-40.png")
          serverStatusLabel.text = resourceMgr.getMessage("main_panel.status.disconnected_never.label", appSettings.server.address)
          serverStatusLabel.tooltip = resourceMgr.getMessage("main_panel.status.disconnected_never.tooltip", appSettings.server.address)

        case status @ _ =>
          logger.warn(s"Invalid state: $status")
      }

    }

    def setServerStatusIcon(iconName: String): Unit = {
      val image = new ImageView(resourceMgr.getImage(iconName))
      image.fitWidth = 16
      image.fitHeight = 16
      serverStatusLabel.graphic = image
    }
  }
}
