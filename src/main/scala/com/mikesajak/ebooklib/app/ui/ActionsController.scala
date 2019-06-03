package com.mikesajak.ebooklib.app.ui

import java.io.{ByteArrayInputStream, File}
import java.nio.file.{Files, Paths}

import com.mikesajak.ebooklib.app.AppController
import com.mikesajak.ebooklib.app.bookformat.{BookFormatResolver, BookReadersRegistry}
import com.mikesajak.ebooklibrary.payload._
import com.typesafe.scalalogging.Logger
import scalafx.Includes._
import scalafx.scene.control.ButtonType
import scalafx.scene.image.Image
import scalafx.scene.layout.Region
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter

import scala.language.implicitConversions

class ActionsController(resourceMgr: ResourceManager,
                        appController: AppController,
                        bookReadersRegistry: BookReadersRegistry,
                        bookFormatResolver: BookFormatResolver) {
  private val logger = Logger[ActionsController]

  def openMetadataEditDialog(bookDataProvider: BookDataProvider, booksNav: Option[BooksNavigator]): Unit = {
    val layout = "/layout/edit_book_metadata1.fxml"

    val (content, controller) = UILoader.loadScene[EditBookMetadataController](layout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, content)
    controller.initialize(bookDataProvider, dialog, booksNav)
    dialog.dialogPane.value.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE)
    dialog.dialogPane.value.getScene.getWindow.sizeToScene()
    dialog.showAndWait() match {
      case Some(ButtonType.OK) =>
        logger.debug("OK button selected")
        val book = controller.bookMetadata
        logger.debug(s"Metadata dialog confirmed: book:\n$book")
      case bt @ _ => logger.debug(s"$bt button selected")
    }
  }

  def handleImportBookAction(): Unit = {
    val fileChooser = new FileChooser() {
      initialDirectory = new File(System.getProperty("user.dir"))
      extensionFilters ++= bookReadersRegistry.allReaders
          .map { r =>
            val extension = bookFormatResolver.forMimeType(r.getMimeType).toUpperCase
            new ExtensionFilter(extension, s"*.${extension.toLowerCase()}")
          }
          .map(ef => ef.delegate)
    }
    val result = Option(fileChooser.showOpenDialog(appController.mainStage))

    logger.debug(s"Selected book file: $result")

    result.flatMap(getBookDataProviderFor)
            .foreach(provider => openMetadataEditDialog(provider, None))
  }

  def getBookDataProviderFor(bookFile: File): Option[BookDataProvider] = {
    val bookData = Files.readAllBytes(Paths.get(bookFile.getAbsolutePath))

    bookReadersRegistry.allReaders
        .collectFirst { case r if r.canRead(new ByteArrayInputStream(bookData)) => r }
        .map { reader => new BookDataProvider() {
            override def bookId: Option[BookId] = None

            override def bookMetadata: BookMetadata = reader.read(new ByteArrayInputStream(bookData))

            override def bookCover: Option[Image] =
              Option(reader.readCover(new ByteArrayInputStream(bookData)))
              .map(c => new Image(new ByteArrayInputStream(c.getImageData)))

            override def bookFormatsMetadata: Seq[BookFormatMetadata] = Seq(formatMetadata)

            override def bookFormat(formatId: BookFormatId): BookFormat = new BookFormat(formatMetadata, bookData)

            private def formatMetadata =
              new BookFormatMetadata(new BookId("not existing"), reader.getMimeType, bookFile.getAbsolutePath)
          }
        }
    }
}
