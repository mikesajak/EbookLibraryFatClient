package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.AppController
import com.mikesajak.ebooklib.app.model._
import com.mikesajak.ebooklib.app.reader.{BookFormatData, BookFormatDataReader}
import com.mikesajak.ebooklib.app.rest.BookServerService
import com.mikesajak.ebooklib.app.ui.BookChangeType.{BookAdd, BookDelete}
import com.mikesajak.ebooklib.app.ui.util.{CancelledException, UILoader, UIUtils}
import com.mikesajak.ebooklib.app.util.EventBus
import enumeratum.{Enum, EnumEntry}
import javafx.concurrent.Worker.State
import javafx.{concurrent => jfxc}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control.ButtonType
import scalafx.scene.image.Image
import scalafx.scene.layout.Region
import scalafx.stage.FileChooser
import scribe.Logging

import java.io.{ByteArrayInputStream, File}
import scala.collection.immutable
import scala.concurrent.{CancellationException, ExecutionContextExecutor}
import scala.language.implicitConversions
import scala.util.{Failure, Success}

class ActionsController(appController: AppController,
                        bookServerService: BookServerService,
                        eventBus: EventBus,
                        bookFormatDataReader: BookFormatDataReader) extends Logging {

  private implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  private var lastVisitedDir = System.getProperty("user.dir")
  def handleImportBookAction(): Unit = {
    val fileToOpenOpt = selectSingleBook()
    logger.debug(s"Selected book file: $fileToOpenOpt")

    fileToOpenOpt.flatMap { fileToOpen =>
      bookFormatDataReader.readFormat(fileToOpen)
                          .map { case (bookFormatData, bookCover, bookData) => createBookDataProvider(fileToOpen, bookData, bookFormatData, bookCover) }
    }.foreach(provider => addBook(provider, None))
  }

  def handleImportMultiBooksAction(): Unit = {
    logger.debug("handleImportMultiBooksAction")

    val selectedFilesOpt = selectMultipleBooks()
    logger.debug(s"Selected book files: $selectedFilesOpt")

    selectedFilesOpt.foreach { filesToOpen => loadFilesInProgressDialog(filesToOpen) }
  }

  private def loadFilesInProgressDialog(filesToOpen: Seq[File]): Unit = {
    val layout = "/layout/progress_panel.fxml"

    val task = new jfxc.Task[Seq[(File, Option[BookDataProvider])]]() with Logging {
      private def checkCancelled(): Unit = {
        if (isCancelled) {
          logger.info("Task was cancelled, aborting current work by CancelledException")
          throw CancelledException("XX")
        }
      }

      override def call(): Seq[(File, Option[BookDataProvider])] = {
        val result =
          filesToOpen.zipWithIndex
                     .map { case (bookFile, idx) =>
                       checkCancelled()
                       updateProgress(idx, filesToOpen.size)
                       val bookDataProviderOpt = bookFormatDataReader.readFormat(bookFile)
                                                                     .map { case (bookFormatData, bookCover, bookData) =>
                                                                       createBookDataProvider(bookFile, bookData, bookFormatData, bookCover)
                                                                     }
                       (bookFile, bookDataProviderOpt)
                     }
        updateProgress(filesToOpen.size, filesToOpen.size)
        result
      }
    }
    val service = new BackgroundService(task)

    val (content, ctrl) = UILoader.loadScene[ProgressPanelController](layout)
    ctrl.init(service)

    val dialog = UIUtils.mkModalDialog[Seq[(File, Option[BookDataProvider])]](appController.mainStage, content)
    dialog.title = s"Progress"
    dialog.dialogPane.value.buttonTypes = Seq(ButtonType.Close)
//    val window = dialog.dialogPane.value.getScene.getWindow
//    window.setOnCloseRequest { _ => dialog.hide() }

    service.state.onChange { (_, _, state) => state match {
      case State.FAILED | State.CANCELLED => dialog.close()
      case State.SUCCEEDED => dialog.result = service.value.value
      case _ =>
    }}

    dialog.onShown = _ => service.start()
    dialog.resizable = false
    dialog.dialogPane.value.setPrefSize(300, 50)
    val booksDataDialogResultOpt = dialog.showAndWait()

    try {
      logger.debug("Dialog closed, cancelling service.")
      service.cancel()
    } catch {
      case _: CancellationException => logger.debug(s"Background service cancelled")
    }

    booksDataDialogResultOpt.foreach(booksData =>
                                       openBooksImportTableDialog(booksData.asInstanceOf[Seq[(File, Option[BookDataProvider])]]))
  }

  private def openBooksImportTableDialog(parsedBooksData: Seq[(File, Option[BookDataProvider])]): Unit = {
    val layout = "/layout/import_books_panel.fxml"

    logger.debug(s"Opening books import table dialog for: $parsedBooksData")

    val (content,ctrl) = UILoader.loadScene[ImportBooksPanelController](layout)
    ctrl.init(parsedBooksData)

    val dialog = UIUtils.mkModalDialog[Unit](appController.mainStage, content)
    dialog.title = "Import books" // TODO: i18
    dialog.dialogPane.value.buttonTypes = Seq(ButtonType.Close)
    dialog.dialogPane.value.setPrefSize(1500, 800)

    dialog.showAndWait()
  }

  private def selectSingleBook(): Option[File] = selectSingleBook(_ => ())
  private def selectSingleBook(f: FileChooser => Unit): Option[File] =
    withBookFileChooserImpl { fc =>
      f(fc)
      val selectedFileOpt = Option(fc.showOpenDialog(appController.mainStage))
      (selectedFileOpt, selectedFileOpt)
    }

  private def selectMultipleBooks(): Option[Seq[File]] = selectMultipleBooks(_ => ())
  private def selectMultipleBooks(f: FileChooser => Unit): Option[Seq[File]] =
    withBookFileChooserImpl { fc =>
      f(fc)
      val selectedFiles = Option(fc.showOpenMultipleDialog(appController.mainStage))
      (selectedFiles.flatMap(_.headOption), selectedFiles)
    }

  private def withBookFileChooserImpl[B](f: FileChooser => (Option[File], B)): B = {
    val fileChooser = new FileChooser() {
      initialDirectory = new File(lastVisitedDir)
      //      extensionFilters ++= bookReadersRegistry.allReaders
      //                                              .map { r =>
      //                                                val extension = bookFormatResolver.forMimeType(r.mimeType).toUpperCase
      //                                                new ExtensionFilter(extension, s"*.${extension.toLowerCase()}")
      //                                              }
      //                                              .map(ef => ef.delegate)
    }
    val (selectedDir, result) = f(fileChooser)

    lastVisitedDir = selectedDir.map(d => if (d.isDirectory) d.getAbsolutePath else d.getParent)
                                .getOrElse(System.getProperty("user.dir"))
    result
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
      bookServerService.addBook(bookMetadata).onComplete {
        case Success(bookId) =>
          logger.info(s"Successfully added book metadata, bookId=$bookId")
          bookFormats.foreach { format =>
            logger.debug(s"Uploading book format: $format")
            bookServerService.addBookFormat(bookId, bookDataProvider.bookFormat(format.formatId))
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
      case Some((bookMetadata, _)) =>
        logger.debug(s"Book edit confirmed: book: $bookMetadata")
        // TODO
      case bt @ _ => logger.debug(s"Metadata dialog - $bt button selected")
    }
  }

  def handleRemoveBookAction(book: Book): Unit = {
    bookServerService.deleteBook(book.id)
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

  private def createBookDataProvider(bookFile: File, bookData: Array[Byte],
                                     bookFormatData: BookFormatData, bookCoverData: Option[CoverImage]) = {
    new BookDataProvider {
      override def bookId: Option[BookId] = None

      override def bookMetadata: BookMetadata =
        BookMetadata.from(bookFormatData, bookFormatsMetadata)

      override def bookCover: Option[Image] =
        bookCoverData.map(coverImage => new Image(new ByteArrayInputStream(coverImage.imageData)))

      override def bookFormatsMetadata: Seq[BookFormatMetadata] = Seq(formatMetadata)

      private def formatMetadata =
        BookFormatMetadata(BookFormatId.NotExisting, BookId.NotExisting, bookFormatData.contentType,
                           Some(bookFile.getAbsolutePath), bookFile.length().toInt)

      override def bookFormat(formatId: BookFormatId): BookFormat = BookFormat(formatMetadata, bookData)
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
