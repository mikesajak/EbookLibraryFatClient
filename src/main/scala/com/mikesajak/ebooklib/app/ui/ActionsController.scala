package com.mikesajak.ebooklib.app.ui

import java.io.{ByteArrayInputStream, File}
import java.nio.file.{Files, Paths}

import com.mikesajak.ebooklib.app.AppController
import com.mikesajak.ebooklib.app.bookformat.BookReadersRegistry
import com.mikesajak.ebooklibrary.payload.{BookFormat, BookFormatMetadata, BookId, BookMetadata}
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
                        bookReadersRegistry: BookReadersRegistry) {
  private val logger = Logger[ActionsController]

  def openMetadataEditDialog(bookDataProvider: BookDataProvider, booksNav: Option[BooksNav]): Unit = {
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
          .map(r => new ExtensionFilter(r.getBookFormatType.getName, s"*.${r.getBookFormatType.getName.toLowerCase()}"))
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

            val provider = new BookDataProvider {
              override def bookId: BookId = new BookId("NotExisting")
              override def bookMetadata: BookMetadata = reader.read(bookData)

              override def bookCover: Option[Image] =
                Option(reader.readCover(bookData))
                    .map(c => new Image(new ByteArrayInputStream(c.getImageData)))

              override def bookFormatsMetadata: Seq[BookFormatMetadata] = Seq(metadata)

              override def bookFormat(): BookFormat = new BookFormat(metadata, bookData)

              private def metadata =
                new BookFormatMetadata(bookId, reader.getBookFormatType.getMimeType(), bookFile.getAbsolutePath)
            }

            openMetadataEditDialog(provider, None)
          }
    }
  }
}
