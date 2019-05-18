package com.mikesajak.ebooklib.app.ui

import com.google.common.eventbus.Subscribe
import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.rest.{ConnectionStatus, ServerConnectionController, ServerStatus}
import com.mikesajak.ebooklib.app.util.EventBus
import scalafx.application.Platform
import scalafx.scene.control.Label
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

@sfxml
class MainPanelController(serverStatusLabel: Label,

                          appSettings: AppSettings,
                          eventBus: EventBus,
                          serverConnectionController: ServerConnectionController,
                          implicit val resourceMgr: ResourceManager) {
  eventBus.register(this)

  serverConnectionController.startMonitoring()

  @Subscribe
  def serverStatusChange(status: ServerStatus): Unit = {
    Platform.runLater {
      status match {
        case ServerStatus(ConnectionStatus.Connected, Some(connInfo)) =>
          setServerStatusIcon("icons8-connected-40.png")
          serverStatusLabel.text = resourceMgr.getMessage("main_panel.status.connected.label",
                                                          appSettings.server.address, connInfo.serverInfo)
          serverStatusLabel.tooltip = resourceMgr.getMessage("main_panel.status.connected.tooltip",
                                                             appSettings.server.address, connInfo.serverInfo, connInfo.timestamp)

        case ServerStatus(ConnectionStatus.Warning, Some(connInfo)) =>
          setServerStatusIcon("icons8-error-48.png")
          serverStatusLabel.text = resourceMgr.getMessage("main_panel.status.connected_warning.label",
                                                          appSettings.server.address, connInfo.serverInfo)
          serverStatusLabel.tooltip = resourceMgr.getMessage("main_panel.status.connected_warning.tooltip",
                                                             appSettings.server.address, connInfo.serverInfo, connInfo.timestamp)

        case ServerStatus(ConnectionStatus.Disconnected, Some(connInfo)) =>
          setServerStatusIcon("icons8-disconnected-40.png")
          serverStatusLabel.text = resourceMgr.getMessage("main_panel.status.disconnected.label",
                                                          appSettings.server.address, connInfo.serverInfo)
          serverStatusLabel.tooltip = resourceMgr.getMessage("main_panel.status.disconnected.tooltip",
                                                             appSettings.server.address, connInfo.serverInfo, connInfo.timestamp)

        case ServerStatus(ConnectionStatus.Disconnected, None) =>
          setServerStatusIcon("icons8-disconnected-40.png")
          serverStatusLabel.text = resourceMgr.getMessage("main_panel.status.disconnected_never.label", appSettings.server.address)
          serverStatusLabel.tooltip = resourceMgr.getMessage("main_panel.status.disconnected_never.tooltip", appSettings.server.address)
      }

    }

    def setServerStatusIcon(iconName: String)  {
      val image = new ImageView(resourceMgr.getImage(iconName))
      image.fitWidth = 16
      image.fitHeight = 16
      serverStatusLabel.graphic = image
    }
  }
}
