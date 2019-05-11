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
                          resourceMgr: ResourceManager) {

  eventBus.register(this)

  serverConnectionController.startMonitoring()

  @Subscribe
  def serverStatusChange(status: ServerStatus): Unit = {
    Platform.runLater {
      status match {
        case ServerStatus(ConnectionStatus.Connected, Some(connInfo)) =>
          setServerStatusIcon("icons8-connected-40.png")
          serverStatusLabel.text = resourceMgr.getMessageWithArgs("main_panel.status.connected.label",
                                                                  Seq(appSettings.server.address, connInfo.serverInfo))
          serverStatusLabel.tooltip = resourceMgr.getMessageWithArgs("main_panel.status.connected.tooltip",
                                                                     Seq(appSettings.server.address, connInfo.serverInfo, connInfo.timestamp))

        case ServerStatus(ConnectionStatus.Warning, Some(connInfo)) =>
          setServerStatusIcon("icons8-error-48.png")
          serverStatusLabel.text = resourceMgr.getMessageWithArgs("main_panel.status.connected_warning.label",
                                                                  Seq(appSettings.server.address, connInfo.serverInfo))
          serverStatusLabel.tooltip = resourceMgr.getMessageWithArgs("main_panel.status.connected_warning.tooltip",
                                                                     Seq(appSettings.server.address, connInfo.serverInfo, connInfo.timestamp))

        case ServerStatus(ConnectionStatus.Disconnected, Some(connInfo)) =>
          setServerStatusIcon("icons8-disconnected-40.png")
          serverStatusLabel.text = resourceMgr.getMessageWithArgs("main_panel.status.disconnected.label",
                                                                     Seq(appSettings.server.address, connInfo.serverInfo))
          serverStatusLabel.tooltip = resourceMgr.getMessageWithArgs("main_panel.status.disconnected.tooltip",
                                                                     Seq(appSettings.server.address, connInfo.serverInfo, connInfo.timestamp))

        case ServerStatus(ConnectionStatus.Disconnected, None) =>
          setServerStatusIcon("icons8-disconnected-40.png")
          serverStatusLabel.text = resourceMgr.getMessageWithArgs("main_panel.status.disconnected_never.label", Seq(appSettings.server.address))
          serverStatusLabel.tooltip = resourceMgr.getMessageWithArgs("main_panel.status.disconnected_never.tooltip", Seq(appSettings.server.address))
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
