package com.mikesajak.ebooklib.app.ui

import java.io.{ByteArrayInputStream, File}
import java.nio.file.{Files, Paths}

import com.mikesajak.ebooklib.app.AppController
import com.mikesajak.ebooklib.app.bookformat.BookReadersRegistry
import com.mikesajak.ebooklibrary.payload.BookMetadata
import com.typesafe.scalalogging.Logger
import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.image.Image
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.{FileChooser, Modality, Stage, StageStyle}

import scala.language.implicitConversions

class ActionsController(resourceMgr: ResourceManager,
                        appController: AppController,
                        bookReadersRegistry: BookReadersRegistry) {
  private val logger = Logger[ActionsController]

  def openMetadataEditDialog(book: BookMetadata, coverImage: Option[Image]): Unit = {
    val layout = "/layout/edit_book_metadata.fxml"

    val (root, controller) = UILoader.loadScene[EditBookMetadataController](layout)
    val stage = new Stage() {
      initModality(Modality.ApplicationModal)
      initStyle(StageStyle.Utility)
      initOwner(appController.mainStage)
      scene = new Scene(root, 1000, 750)
      sizeToScene()
    }
    controller.initialize(book, coverImage)
    stage.show()
  }

  def handleImportBookAction(): Unit = {
    val fileChooser = new FileChooser() {
      initialDirectory = new File(System.getProperty("user.dir"))
      extensionFilters ++= bookReadersRegistry.allReaders
          .map(r => new ExtensionFilter(r.getBookType, s"*.${r.getBookType.toLowerCase()}"))
          .map(ef => ef.delegate)

    }
    val result = Option(fileChooser.showOpenDialog(appController.mainStage))

    println(result)

    result.foreach { bookFile =>
      val bookData = Files.readAllBytes(Paths.get(bookFile.getAbsolutePath))

      bookReadersRegistry.allReaders
          .collectFirst { case r if r.canRead(bookData) => r }
          .foreach { reader =>
            val metadata = reader.read(bookData)
            val coverImage = Option(reader.readCover(bookData))
                .map(c => new Image(new ByteArrayInputStream(c.getImageData)))

            openMetadataEditDialog(metadata, coverImage)
          }
    }
  }
}
