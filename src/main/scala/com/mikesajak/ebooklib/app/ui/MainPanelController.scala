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
                          resourceManager: ResourceManager) {

  eventBus.register(this)

  serverConnectionController.startMonitoring()

  @Subscribe
  def serverStatusChange(status: ServerStatus): Unit = {
    Platform.runLater {
      serverStatusLabel.text = status match {
        case ServerStatus(ConnectionStatus.Disconnected, _) => s"Disconnected to ${appSettings.server.address}"
        case ServerStatus(connStatus, Some(connInfo)) =>
          s"Connected to ${appSettings.server.address} (${connInfo.serverInfo.getName}:${connInfo.serverInfo.getVersion})"
      }

      val iconName = status.connectionStatus match {
        case ConnectionStatus.Connected => "icons8-connected-40.png"
        case ConnectionStatus.Warning => "icons8-error-48.png"
        case ConnectionStatus.Disconnected => "icons8-disconnected-40.png"
      }

      val image = new ImageView(resourceManager.getImage(iconName))
      image.fitWidth = 16
      image.fitHeight = 16
      serverStatusLabel.graphic = image

    }
  }
}
