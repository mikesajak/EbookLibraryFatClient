package com.mikesajak.ebooklib.app.ui

import com.google.common.eventbus.Subscribe
import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.rest.{ServerController, ServerStatus}
import com.mikesajak.ebooklib.app.util.EventBus
import scalafx.application.Platform
import scalafx.event.ActionEvent
import scalafx.scene.control.Label
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

@sfxml
class MainPanelController(serverStatusLabel: Label,

                          appSettings: AppSettings,
                          eventBus: EventBus,
                          serverController: ServerController,
                          resourceManager: ResourceManager) {

  eventBus.register(this)

  serverController.startMonitoring()

  @Subscribe
  def serverStatusChange(status: ServerStatus): Unit = {
    Platform.runLater {
      serverStatusLabel.text =
          status.info.map(i => s"Connected to ${appSettings.server.address} (${i.getName}:${i.getVersion})")
          .getOrElse("Disconnected...")

      val iconName = status.info.map(_ => "icons8-connected-40.png")
          .getOrElse("icons8-disconnected-40-png")
      val image = new ImageView(resourceManager.getImage(iconName))
      image.fitWidth = 16
      image.fitHeight = 16
      serverStatusLabel.graphic = image

    }
  }
}
