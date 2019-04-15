package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.AppController
import com.mikesajak.ebooklibrary.payload.Book
import com.typesafe.scalalogging.Logger
import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.stage.{Modality, Stage, StageStyle}

class ActionsController(resourceMgr: ResourceManager,
                        appController: AppController) {
  private val logger = Logger[ActionsController]

  def openMetadataEditDialog(book: Book): Unit = {
    val layout = "/layout/edit_book_metadata.fxml"

    val (root, controller) = UILoader.loadScene[EditBookMetadataController](layout)
    val stage = new Stage() {
      initModality(Modality.ApplicationModal)
      initStyle(StageStyle.Utility)
      initOwner(appController.mainStage)
      scene = new Scene(root, 1000, 750)
      sizeToScene()
    }
    controller.initialize(book)
    stage.show()
  }
}
