package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.rest.ServerController
import com.mikesajak.ebooklibrary.payload.Book
import com.typesafe.scalalogging.Logger
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.collections.transformation.{FilteredBuffer, SortedBuffer}
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.scene.control.{Button, ComboBox, Spinner, TableColumn, TableView, TextField}
import scalafxml.core.macros.sfxml

import scala.collection.JavaConverters._

class BookRow(book: Book) {
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

                          serverController: ServerController,
                          appSettings: AppSettings) {
  val logger = Logger[BookTableController]

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

  booksTableView.items = sortedRows

  readBooks()

  def readBooks(): Unit = {
    val books = serverController.listBooks()
    bookRows.setAll(books.map(new BookRow(_)).asJava)
  }

  def onEditMetaButton(ae: ActionEvent): Unit = {
    logger.debug("Edit meta button")
  }
}

