package com.mikesajak.ebooklib.app.ui

import com.google.common.eventbus.Subscribe
import com.mikesajak.ebooklib.app.AppController
import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.model.{Book, BookId}
import com.mikesajak.ebooklib.app.rest.{BookServerController, ServerReconnectedEvent}
import com.mikesajak.ebooklib.app.ui.controls.PopOverEx
import com.mikesajak.ebooklib.app.util.EventBus
import com.typesafe.scalalogging.Logger
import org.controlsfx.control.PopOver
import org.controlsfx.control.textfield.{CustomTextField, TextFields}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.collections.transformation.{FilteredBuffer, SortedBuffer}
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafx.scene.input.{MouseButton, MouseEvent}
import scalafx.scene.layout.{HBox, Priority}
import scalafxml.core.macros.{nested, sfxml}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class BookRow(val book: Book, var bookFormats: List[String]) {
  val title = new StringProperty(book.metadata.title)
  val authors = new StringProperty(book.metadata.authors.mkString(", "))
  val tags = new StringProperty(book.metadata.tags.mkString(", "))
  val identifiers = new StringProperty(book.metadata.identifiers.mkString(", "))
  val creationDate = new StringProperty(book.metadata.creationDate.map(_.toString).orNull)
  val publicationDate = new StringProperty(book.metadata.publicationDate.map(_.toString).orNull)
  val publisher = new StringProperty(book.metadata.publisher.orNull)
  val languages = new StringProperty(book.metadata.languages.mkString(", "))
  val formats = new StringProperty(bookFormats.mkString)
}


//noinspection UnstableApiUsage
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
                          formatsColumn: TableColumn[BookRow, String],

                          filterTextFieldHBox: HBox,
                          searchHistoryButton: Button,

                          @nested[BookDetailsPreviewControllerImpl]
                          bookPreviewDetailsPanelController: BookDetailsPreviewController,

                          bookDataProviderFactory: BookDataProviderFactory,
                          appSettings: AppSettings,
                          appController: AppController,
                          bookServerController: BookServerController,
                          actionsController: ActionsController,
                          eventBus: EventBus,
                          implicit val resourceMgr: ResourceManager) {
  import ResourceManager._

  private val logger = Logger[BookTableController]

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  private val bookRows = ObservableBuffer[BookRow]()
  private var bookRowsMap = Map[BookId, BookRow]()
  private val filteredRows = new FilteredBuffer(bookRows)
  private val sortedRows = new SortedBuffer(filteredRows)

  private var booksNavigator = BooksCollectionNavigator.empty

  titleColumn.cellValueFactory = { _.value.title }
  authorsColumn.cellValueFactory = { _.value.authors }
  tagsColumn.cellValueFactory = { _.value.tags }
  identifiersColumn.cellValueFactory = { _.value.identifiers }
  creationDateColumn.cellValueFactory = { _.value.creationDate }
  publicationDateColumn.cellValueFactory = { _.value.publicationDate }
  publisherColumn.cellValueFactory = { _.value.publisher }
  languagesColumn.cellValueFactory = { _.value.languages }
  formatsColumn.cellValueFactory = { _.value.formats }

  booksTableView.columns.zip(appSettings.booksTable.columnWidths)
          .foreach { case (column, width) => column.setPrefWidth(width) }

  booksTableView.rowFactory = { tableView =>
    val row = new TableRow[BookRow]()

    row.contextMenu = new ContextMenu() {
      private val removeBookItem = new MenuItem() {
        text = "Remove book" // TODO: i18
        onAction = { event =>
          actionsController.handleRemoveBookAction(row.item.value.book)
        }
      }
      items.add(removeBookItem)
    }

    row.handleEvent(MouseEvent.MouseClicked) { event: MouseEvent =>
      if (!row.isEmpty) {
        event.button match {
          case MouseButton.Primary =>
            val book = row.item.value.book
            val provider = bookDataProviderFactory.getServerBookDataProvider(book)
            event.clickCount match {
              case 1 => bookPreviewDetailsPanelController.initBook(provider)
              case 2 => actionsController.openMetadataEditDialog(provider, None)
            }
          case MouseButton.Secondary =>
          case MouseButton.Middle =>
          case _ =>
        }
      }
    }
    row
  }

  booksTableView.items = sortedRows

  eventBus.register(this)

  private val filterTextField = {
    val tf = TextFields.createClearableTextField().asInstanceOf[CustomTextField]
    tf.promptText = "Search book library"
    tf.hgrow = Priority.Always
    val imageView = new ImageView("icons8-search-48.png".imgResource)
    imageView.fitWidth = 16
    imageView.fitHeight = 16
    tf.setLeft(imageView)
    tf.onAction = _ => refreshBooks()
    tf
  }
  filterTextFieldHBox.children.setAll(filterTextField)

  refreshBooks()

  @Subscribe
  def refreshBooks(serverReconnectedEvent: ServerReconnectedEvent): Unit = {
    logger.info(s"Server connection reestablished - refreshing books")
    refreshBooks()
  }

  @Subscribe
  def refreshBooks(bookChangeEvent: BookChangeEvent): Unit = {
    logger.info(s"Book change event: $bookChangeEvent - refreshing books")
    refreshBooks()
  }

  def refreshBooks(): Unit = {
    val searchQuery = filterTextField.text.value
    logger.debug(s"set filter action: $searchQuery")
    readBooks(if (searchQuery.isBlank) None else Some(searchQuery))
  }

  def readBooks(searchQuery: Option[String]=None): Unit = {
    logger.debug("Loading books in the background")
    searchQuery.map(query => bookServerController.searchBooks(query))
               .getOrElse(bookServerController.listBooks())
               .onComplete {
                 case Success(books) =>
                   Platform.runLater { updateBooksTable(books) }
                 case Failure(exception) => logger.warn(s"Error fetching list of books", exception)
               }
  }

  private def updateBooksTable(books: Seq[Book]): Unit = {
    logger.debug(s"Finished loading books from server (count=${books.size}). Refreshing list.")
    val rows = books.map(new BookRow(_, List()))
    bookRowsMap = rows.map(row => (row.book.id, row)).toMap
    bookRows.setAll(rows.asJava)
  }

  def onImportBookAction() {
    actionsController.handleImportBookAction()
  }

  def onRefreshListAction(): Unit = {
    refreshBooks()
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
          onHidden = _ => filterHistoryPopoverVisible = false
        }.show(ae.source.asInstanceOf[javafx.scene.Node])
      }

  }

  def onSavedSearchesAction(ae: ActionEvent) {
    logger.debug("saved searches action")
  }
}

