package com.mikesajak.ebooklib.app.ui

import java.io.{ByteArrayInputStream, File}
import java.nio.file.{Files, Paths}

import com.mikesajak.ebooklib.app.AppController
import com.mikesajak.ebooklib.app.bookformat.{BookFormatResolver, BookReadersRegistry}
import com.mikesajak.ebooklib.app.dto.ErrorResponse
import com.mikesajak.ebooklib.app.model._
import com.mikesajak.ebooklib.app.rest.BookServerController
import com.mikesajak.ebooklib.app.ui.BookChangeType.{BookAdd, BookDelete}
import com.mikesajak.ebooklib.app.util.EventBus
import com.typesafe.scalalogging.Logger
import enumeratum.{Enum, EnumEntry}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.image.Image
import scalafx.scene.layout.Region
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import sttp.client3.HttpError

import scala.collection.immutable
import scala.concurrent.ExecutionContextExecutor
import scala.language.implicitConversions
import scala.util.{Failure, Success}

class ActionsController(appController: AppController,
                        bookReadersRegistry: BookReadersRegistry,
                        bookFormatResolver: BookFormatResolver,
                        bookServerController: BookServerController,
                        eventBus: EventBus) {
  private val logger = Logger[ActionsController]

  private implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  def handleImportBookAction(): Unit = {
    val fileChooser = new FileChooser() {
      initialDirectory = new File(System.getProperty("user.dir"))
      extensionFilters ++= bookReadersRegistry.allReaders
                                              .map { r =>
                                                val extension = bookFormatResolver.forMimeType(r.mimeType).toUpperCase
                                                new ExtensionFilter(extension, s"*.${extension.toLowerCase()}")
                                              }
                                              .map(ef => ef.delegate)
    }
    val result = Option(fileChooser.showOpenDialog(appController.mainStage))

    logger.debug(s"Selected book file: $result")

    result.flatMap(getBookDataProviderFor)
          .foreach(provider => openMetadataEditDialog(provider, None))
  }

  def openMetadataEditDialog(bookDataProvider: BookDataProvider, booksNav: Option[BooksNavigator]) {
    val layout = "/layout/edit_book_metadata1.fxml"

    val (content, controller) = UILoader.loadScene[EditBookMetadataController](layout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, content)
    controller.initialize(bookDataProvider, dialog, booksNav)
    dialog.dialogPane.value.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE)
    dialog.dialogPane.value.getScene.getWindow.sizeToScene()
    dialog.showAndWait() match {
      case Some(ButtonType.OK) => //Some(controller.bookMetadata, controller.bookFormatsMetadata())
        val bookMetadata = controller.bookMetadata
        val bookFormatsMetadata = controller.bookFormats()
        logger.debug(s"Metadata dialog confirmed: book:\n$bookMetadata")
        bookServerController.addBook(bookMetadata).onComplete {
          case Success(bookId) =>
            logger.info(s"Successfully added book metadata, bookId=$bookId")
            bookFormatsMetadata.foreach { formatMetadata =>
              logger.debug(s"Uploading book format: $formatMetadata")
              val bookFormat = bookDataProvider.bookFormat(formatMetadata.formatId)
              bookServerController.addBookFormat(bookId, bookFormat)
            }
            eventBus.publish(BookAddedEvent(Book(bookId, bookMetadata)))

          case Failure(exception) =>
            logger.warn(s"An error occurred while adding book to library.", exception)
            showErrorDialog("Error adding book to library", // TODO: i18
                            exception)
        }
      case bt @ _ =>
        logger.debug(s"Metadata dialog - $bt button selected")
        None
    }
  }

  def handleRemoveBookAction(book: Book): Unit = {
    bookServerController.deleteBook(book.id)
                        .onComplete {
                          case Success(_) =>
                            eventBus.publish(BookRemovedEvent(book))

                          case Failure(exception) =>
                            logger.warn(s"An error occurred while removing book from library.", exception)
                            showErrorDialog("Error removing book to library", // TODO: i18
                                            exception)
                        }
  }

  private def showErrorDialog(message: String, exception: Throwable): Unit = {
    val detailedMessage = exception match {
      case HttpError(re: ErrorResponse, statusCode) => re.message
      case _ => exception.getLocalizedMessage
    }
    Platform.runLater {
      new Alert(AlertType.Error) {
        initOwner(appController.mainStage)
        title = "Error"
        headerText = message
        contentText = detailedMessage
      }.showAndWait()
    }
  }

  def getBookDataProviderFor(bookFile: File): Option[BookDataProvider] = {
    val bookData = Files.readAllBytes(Paths.get(bookFile.getAbsolutePath))

    bookReadersRegistry.allReaders
        .collectFirst { case r if r.canRead(new ByteArrayInputStream(bookData)) => r }
        .map { reader => new BookDataProvider() {
          override def bookId: Option[BookId] = None

          override def bookMetadata: BookMetadata = reader.read(new ByteArrayInputStream(bookData))

          override def bookCover: Option[Image] = {
            reader.readCover(new ByteArrayInputStream(bookData))
          }.map(coverImage => new Image(new ByteArrayInputStream(coverImage.imageData)))

          override def bookFormatsMetadata: Seq[BookFormatMetadata] = Seq(formatMetadata)

          override def bookFormat(formatId: BookFormatId): BookFormat = BookFormat(formatMetadata, bookData)

          private def formatMetadata =
            BookFormatMetadata(BookFormatId.NotExisting, BookId.NotExisting, reader.mimeType,
                               Some(bookFile.getAbsolutePath), bookFile.length().toInt)
        }
      }
    }
}

sealed trait BookChangeType extends EnumEntry

object BookChangeType extends Enum[BookChangeType] {
  val values: immutable.IndexedSeq[BookChangeType] = findValues

  case object BookAdd extends BookChangeType
  case object BookDelete extends BookChangeType
}

abstract class BookChangeEvent(val book: Book, val changeType: BookChangeType)

case class BookAddedEvent(override val book: Book) extends BookChangeEvent(book, BookAdd)
case class BookRemovedEvent(override val book: Book) extends BookChangeEvent(book, BookDelete)
