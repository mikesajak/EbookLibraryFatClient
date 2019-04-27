package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.AppController
import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.rest.BookServerController
import com.mikesajak.ebooklibrary.payload.{Book, BookMetadata}
import com.typesafe.scalalogging.Logger
import javafx.concurrent.Task
import javafx.{concurrent => jfxc}
import scalafx.Includes._
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.collections.transformation.{FilteredBuffer, SortedBuffer}
import scalafx.concurrent.Service
import scalafx.event.ActionEvent
import scalafx.scene.control.{TableColumn, TableRow, TableView}
import scalafx.scene.image.Image
import scalafx.scene.input.{MouseButton, MouseEvent}
import scalafxml.core.macros.sfxml

import scala.collection.JavaConverters._

class BookRow(val book: Book) {
  val title = new StringProperty(book.getMetadata.getTitle)
  val authors = new StringProperty(book.getMetadata.getAuthors.asScala.mkString(", "))
  val tags = new StringProperty(book.getMetadata.getTags.asScala.mkString(", "))
  val identifiers = new StringProperty(book.getMetadata.getIdentifiers.asScala.mkString(", "))
  val creationDate = new StringProperty(strValue(book.getMetadata.getCreationDate))
  val publicationDate = new StringProperty(strValue(book.getMetadata.getPublicationDate))
  val publisher = new StringProperty(book.getMetadata.getPublisher)
  val languages = new StringProperty(book.getMetadata.getLanguages.asScala.mkString(", "))

  private def strValue(v: Any) = Option(v).map(_.toString).orNull
}

@sfxml
class BookTableController(booksTableView: TableView[BookRow],
                          titleColumn: TableColumn[BookRow, String],
                          authorsColumn: TableColumn[BookRow, String],
                          tagsColumn: TableColumn[BookRow, String],
                          identifiersColumn: TableColumn[BookRow, String],
                          creationDateColumn: TableColumn[BookRow, String],
                          publicationDateColumn: TableColumn[BookRow, String],
                          publisherColumn: TableColumn[BookRow, String],
                          languagesColumn: TableColumn[BookRow, String],

                          appSettings: AppSettings,
                          appController: AppController,
                          bookServerController: BookServerController,
                          actionsController: ActionsController) {
  private val logger = Logger[BookTableController]

  private val booksService = new BooksService()

  private val bookRows = ObservableBuffer[BookRow]()
  private val filteredRows = new FilteredBuffer(bookRows)
  private val sortedRows = new SortedBuffer(filteredRows)

  titleColumn.cellValueFactory = { _.value.title }
  authorsColumn.cellValueFactory = { _.value.authors }
  tagsColumn.cellValueFactory = { _.value.tags }
  identifiersColumn.cellValueFactory = { _.value.identifiers }
  creationDateColumn.cellValueFactory = { _.value.creationDate }
  publicationDateColumn.cellValueFactory = { _.value.publicationDate }
  publisherColumn.cellValueFactory = { _.value.publisher }
  languagesColumn.cellValueFactory = { _.value.languages }

  booksTableView.columns.zip(appSettings.booksTable.columnWidths)
          .foreach { case (column, width) => column.setPrefWidth(width) }

  booksTableView.rowFactory = { tableView =>
    val row = new TableRow[BookRow]()

    row.handleEvent(MouseEvent.MouseClicked) { event: MouseEvent =>
      if (!row.isEmpty) {
        event.button match {
          case MouseButton.Primary if event.clickCount == 2 =>

            val book = row.item.value.book
            val cover = bookServerController.getBookCover(book.getId)

            openMetaDataDialog(book.getMetadata, cover) // FIXME: add cover

          case MouseButton.Secondary =>
          case MouseButton.Middle =>
          case _ =>
        }
      }
    }
    row
  }

  booksTableView.items = sortedRows

  readBooks()

  def readBooks2(): Unit = {
    val books = bookServerController.listBooks()
    bookRows.setAll(books.map(new BookRow(_)).asJava)
  }

  def readBooks(): Unit = {
    booksService.start()
  }

  class BooksService extends Service(new jfxc.Service[Seq[Book]]() {
    override def createTask(): Task[Seq[Book]] = () => {
      bookServerController.listBooks()
    }

    override def succeeded(): Unit = {
      super.succeeded()
      val books = getValue
      bookRows.setAll(books.map(new BookRow(_)).asJava)
    }
  })

  def onImportBookAction(ae: ActionEvent): Unit = {
    logger.debug("Edit meta button")

    actionsController.handleImportBookAction()
  }

  def openMetaDataDialog(book: BookMetadata, coverImage: Option[Image]): Unit = {
    actionsController.openMetadataEditDialog(book, coverImage)
  }
}

