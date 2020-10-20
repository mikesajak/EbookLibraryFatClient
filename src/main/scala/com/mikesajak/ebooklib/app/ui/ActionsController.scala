package com.mikesajak.ebooklib.app.ui

import java.io.{ByteArrayInputStream, File}
import java.nio.file.{Files, Paths}

import com.mikesajak.ebooklib.app.AppController
import com.mikesajak.ebooklib.app.bookformat.{BookFormatResolver, BookReadersRegistry}
import com.mikesajak.ebooklib.app.model._
import com.mikesajak.ebooklib.app.rest.BookServerController
import com.mikesajak.ebooklib.app.ui.BookChangeType.{BookAdd, BookDelete}
import com.mikesajak.ebooklib.app.util.EventBus
import com.typesafe.scalalogging.Logger
import enumeratum.{Enum, EnumEntry}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control.ButtonType
import scalafx.scene.image.Image
import scalafx.scene.layout.Region
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter

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
          .foreach(provider => addBook(provider, None))
  }

  def openMetadataEditDialog(bookDataProvider: BookDataProvider, booksNav: Option[BooksNavigator]): Option[(BookMetadata, Seq[BookFormatMetadata])] = {
    val layout = "/layout/edit_book_metadata1.fxml"

    val (content, controller) = UILoader.loadScene[EditBookMetadataController](layout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, content)
    controller.initialize(bookDataProvider, dialog, booksNav)
    dialog.dialogPane.value.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE)
    dialog.dialogPane.value.getScene.getWindow.sizeToScene()
    dialog.showAndWait() match {
      case Some(ButtonType.OK) => Some(controller.bookMetadata, controller.bookFormats())
      case bt @ _ =>
        logger.debug(s"Metadata dialog - $bt button selected")
        None
    }
  }

  def addBook(bookDataProvider: BookDataProvider, booksNav: Option[BooksNavigator]): Unit = {
    logger.debug(s"Opening book metadata dialog for add")
    openMetadataEditDialog(bookDataProvider, booksNav).foreach { case (bookMetadata, bookFormats) =>
      logger.debug(s"Metadata dialog confirmed: book:\n$bookMetadata")
      bookServerController.addBook(bookMetadata).onComplete {
        case Success(bookId) =>
          logger.info(s"Successfully added book metadata, bookId=$bookId")
          bookFormats.foreach { format =>
            logger.debug(s"Uploading book format: $format")
            bookServerController.addBookFormat(bookId, bookDataProvider.bookFormat(format.formatId))
          }
          eventBus.publish(BookAddedEvent(Book(bookId, bookMetadata)))

        case Failure(exception) =>
          logger.warn(s"An error occurred while adding book to library.", exception)
          openErrorDialog("Error adding book to library", // TODO: i18
                          exception)
      }
    }

  }

  def editBook(bookDataProvider: BookDataProvider, booksNavigator: Option[BooksNavigator]): Unit = {
    logger.debug(s"Opening book metadata dialog for edit")
    openMetadataEditDialog(bookDataProvider, booksNavigator) match {
      case Some((bookMetadata, bookFormats)) =>
        logger.debug(s"Book edit confirmed: book: $bookMetadata")
        // TODO
      case bt @ _ => logger.debug(s"Metadata dialog - $bt button selected")
    }
  }

  def handleRemoveBookAction(book: Book): Unit = {
    bookServerController.deleteBook(book.id)
                        .onComplete {
                          case Success(_) =>
                            eventBus.publish(BookRemovedEvent(book))

                          case Failure(exception) =>
                            logger.warn(s"An error occurred while removing book from library.", exception)
                            openErrorDialog("Error removing book to library", // TODO: i18
                                            exception)
                        }
  }

  private def openErrorDialog(title: String, exception: Throwable): Unit =
    openErrorDialog(title, exception.getLocalizedMessage)

  private def openErrorDialog(title: String, message: String): Unit = Platform.runLater {
    val layout = "/layout/error_dialog.fxml"

    val (content, controller) = UILoader.loadScene[ErrorDialogController](layout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, content)
    controller.init(dialog, title, message)

    dialog.dialogPane.value.setMinSize(500, 300)
    dialog.dialogPane.value.setPrefSize(500, 300)

    dialog.showAndWait()
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

//          override def bookFormats: Seq[BookFormat] = Seq(BookFormat(formatMetadata, bookData))
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
