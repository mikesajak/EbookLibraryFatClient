package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.AppController
import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.rest.BookServerController
import com.mikesajak.ebooklib.app.ui.controls.PopOverEx
import com.mikesajak.ebooklibrary.payload._
import com.typesafe.scalalogging.Logger
import javafx.concurrent.Task
import javafx.{concurrent => jfxc}
import org.controlsfx.control.PopOver
import org.controlsfx.control.textfield.{CustomTextField, TextFields}
import scalafx.Includes._
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.collections.transformation.{FilteredBuffer, SortedBuffer}
import scalafx.concurrent.Service
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, TableColumn, TableRow, TableView}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.{MouseButton, MouseEvent}
import scalafx.scene.layout.{HBox, Priority}
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

trait BookDataProvider {
  def bookId: BookId
  def bookMetadata: BookMetadata
  def bookCover: Option[Image]
  def bookFormatsMetadata: Seq[BookFormatMetadata]
  def bookFormat(): BookFormat
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

                          filterTextFieldHBox: HBox,
                          searchHistoryButton: Button,

                          appSettings: AppSettings,
                          appController: AppController,
                          bookServerController: BookServerController,
                          actionsController: ActionsController,
                          implicit val resourceMgr: ResourceManager) {
  import ResourceManager._

  private val logger = Logger[BookTableController]

  private val fetchBooksService = new FetchBooksService()

  private val bookRows = ObservableBuffer[BookRow]()
  private val filteredRows = new FilteredBuffer(bookRows)
  private val sortedRows = new SortedBuffer(filteredRows)

  private var booksNavigator = BooksCollectionNav.empty

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

  class BookDataProviderImpl(val book: Book) extends BookDataProvider {
    override def bookId: BookId = book.getId
    override def bookMetadata: BookMetadata = book.getMetadata
    override def bookCover: Option[Image] = bookServerController.getBookCover(book.getId)

    override def bookFormatsMetadata: Seq[BookFormatMetadata] = {
      val formatIds = bookServerController.getBookFormatIds(book.getId)
      formatIds.map { id => bookServerController.getBookFormatMetadata(book.getId, id) }
    }

    override def bookFormat(): BookFormat = ???
  }

  booksTableView.rowFactory = { tableView =>
    val row = new TableRow[BookRow]()

    row.handleEvent(MouseEvent.MouseClicked) { event: MouseEvent =>
      if (!row.isEmpty) {
        event.button match {
          case MouseButton.Primary if event.clickCount == 2 =>

            val book = row.item.value.book
            val cover = bookServerController.getBookCover(book.getId)

            actionsController.openMetadataEditDialog(new BookDataProviderImpl(book), None)

          case MouseButton.Secondary =>
          case MouseButton.Middle =>
          case _ =>
        }
      }
    }
    row
  }

  booksTableView.items = sortedRows

  private val filterTextField = {
    val tf = TextFields.createClearableTextField().asInstanceOf[CustomTextField]
    tf.promptText = "Search book library"
    tf.hgrow = Priority.Always
    val imageView = new ImageView("icons8-search-48.png".imgResource)
    imageView.fitWidth = 16
    imageView.fitHeight = 16
    tf.setLeft(imageView)
    tf.onAction = { ae =>
      logger.debug(s"set filter action: ${tf.text.value}")
    }
    tf
  }
  filterTextFieldHBox.children.setAll(filterTextField)

  readBooks()

  def readBooks2(): Unit = {
    val books = bookServerController.listBooks()
    bookRows.setAll(books.map(new BookRow(_)).asJava)
  }

  def readBooks(): Unit = {
    fetchBooksService.start()
  }

  class FetchBooksService extends Service(new jfxc.Service[Seq[Book]]() {
    override def createTask(): Task[Seq[Book]] = () => {
      bookServerController.listBooks()
    }

    override def succeeded(): Unit = {
      super.succeeded()
      val books = getValue
      bookRows.setAll(books.map(new BookRow(_)).asJava)
    }
  })

  def onImportBookAction(ae: ActionEvent) {
    actionsController.handleImportBookAction()
  }

  var filterHistoryPopoverVisible = false
  def onFilterHistoryAction(ae: ActionEvent) {
    logger.debug("filter history action")

      if (!filterHistoryPopoverVisible) {
        filterHistoryPopoverVisible = true

        new PopOverEx {
          title = "Previous filters"
          detachable = false
          autoHide = true
          headerAlwaysVisible = true
          arrowLocation = PopOver.ArrowLocation.TOP_RIGHT
          onHidden = we => filterHistoryPopoverVisible = false
        }.show(ae.source.asInstanceOf[javafx.scene.Node])
      }

  }

  def onSavedSearchesAction(ae: ActionEvent) {
    logger.debug("saved searches action")
  }
}

