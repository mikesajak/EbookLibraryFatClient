package com.mikesajak.ebooklib.app.ui

import com.google.common.eventbus.Subscribe
import com.mikesajak.ebooklib.app.rest.ConnectionStatus.{Connected, Disconnected}
import com.mikesajak.ebooklib.app.rest.ServerStatus
import com.mikesajak.ebooklib.app.util.EventBus
import scalafx.scene.control.Button
import scalafxml.core.macros.sfxml

//noinspection UnstableApiUsage
@sfxml
class ToolbarController(editMetaButton: Button,
                        addBooksButton: Button,
                        refreshListButton: Button,

                        actionsController: ActionsController,
                        eventBus: EventBus) {

  private val onlineOnlyControls = List(editMetaButton, addBooksButton, refreshListButton)

  eventBus.register(this)

  def onImportBookAction(): Unit = {
    actionsController.handleImportBookAction()
  }

  def onImportMultiBooksAction(): Unit = {
    actionsController.handleImportMultiBooksAction()
  }

  def onRefreshListAction(): Unit = {
    eventBus.publish(RefreshBooksAction())
  }

  @Subscribe
  def serverConnectionChanged(serverStatus: ServerStatus): Unit = {
    serverStatus.connectionStatus match {
      case Disconnected => onlineOnlyControls.foreach(_.disable = true)
      case Connected => onlineOnlyControls.foreach(_.disable = false)
      case _ =>
    }
  }

}

case class RefreshBooksAction()