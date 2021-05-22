package com.mikesajak.ebooklib.app.ui

import com.google.common.eventbus.Subscribe
import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.config.AppSettings
import com.mikesajak.ebooklib.app.config.ConfigEvents.BookTableColumnWidthChange
import com.mikesajak.ebooklib.app.model.Book
import com.mikesajak.ebooklib.app.rest.ServerReconnectedEvent
import com.mikesajak.ebooklib.app.ui.controls.PopOverEx
import com.mikesajak.ebooklib.app.util.EventBus
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
import scribe.Logging

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.{Failure, Success}

class BookRow(val book: Book, val bookFormatResolver: BookFormatResolver) {
  val title = new StringProperty(book.metadata.title)
  val authors = new StringProperty(book.metadata.authors.mkString(", "))
  val tags = new StringProperty(book.metadata.tags.mkString(", "))
  val identifiers = new StringProperty((book.metadata.identifiers :+ s"internal:${book.id}").mkString(", "))
  val creationDate = new StringProperty(book.metadata.creationDate.map(_.toString).orNull) // TODO: format date
  val publicationDate = new StringProperty(book.metadata.publicationDate.map(_.toString).orNull) // TODO: format date
  val publisher = new StringProperty(book.metadata.publisher.orNull)
  val languages = new StringProperty(book.metadata.languages.mkString(", "))
  val formats = new StringProperty(book.metadata.formats.map(fmt => bookFormatResolver.forMimeType(fmt.formatType).description).mkString(", "))
}

trait BookTableController {
  def init(booksProvider: BooksProvider): Unit
}

//noinspection UnstableApiUsage
@sfxml
class BookTableControllerImpl(booksTableView: TableView[BookRow],
                              titleColumn: TableColumn[BookRow, String],
                              authorsColumn: TableColumn[BookRow, String],
                              tagsColumn: TableColumn[BookRow, String],
                              identifiersColumn: TableColumn[BookRow, String],
                              creationDateColumn: TableColumn[BookRow, String],
                              publisherColumn: TableColumn[BookRow, String],
                              languagesColumn: TableColumn[BookRow, String],
                              formatsColumn: TableColumn[BookRow, String],

                              filterTextFieldHBox: HBox,
                              searchHistoryButton: Button,

                              @nested[BookDetailsPreviewControllerImpl]
                              bookPreviewDetailsPanelController: BookDetailsPreviewController,

                              appSettings: AppSettings,
                              actionsController: ActionsController,
                              eventBus: EventBus,
                              bookFormatResolver: BookFormatResolver,
                              implicit val resourceMgr: ResourceManager) extends BookTableController with Logging {
  import ResourceManager._

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  private val bookRows = ObservableBuffer[BookRow]()
//  private var bookRowsMap = Map[BookId, BookRow]()
  private val filteredRows = new FilteredBuffer(bookRows)
  private val sortedRows = new SortedBuffer(filteredRows)

  private val filterTextField = {
    val tf = TextFields.createClearableTextField().asInstanceOf[CustomTextField]
    tf.promptText = "Search book library" // TODO: i18
    tf.hgrow = Priority.Always
    val imageView = new ImageView("icons8-search-48.png".imgResource)
    imageView.fitWidth = 16
    imageView.fitHeight = 16
    tf.setLeft(imageView)
    tf.onAction = _ => refreshBooks()
    tf
  }
  filterTextFieldHBox.children.setAll(filterTextField)

  private var booksNavigator = BooksCollectionNavigator.empty
  private var booksProvider: BooksProvider = _

  def init(booksProvider: BooksProvider): Unit = {
    this.booksProvider = booksProvider

    titleColumn.cellValueFactory = {_.value.title}
    authorsColumn.cellValueFactory = {_.value.authors}
    tagsColumn.cellValueFactory = {_.value.tags}
    identifiersColumn.cellValueFactory = {_.value.identifiers}
    creationDateColumn.cellValueFactory = {_.value.creationDate}
    publisherColumn.cellValueFactory = {_.value.publisher}
    languagesColumn.cellValueFactory = {_.value.languages}
    formatsColumn.cellValueFactory = {_.value.formats}

    booksTableView.columns
                  .filter(col => appSettings.booksTable.columnWidths.contains(col.getId))
                  .foreach(col => col.setPrefWidth(appSettings.booksTable.columnWidths(col.getId)))

    booksTableView.rowFactory = { _ =>
      val row = new TableRow[BookRow]()

      row.contextMenu = new ContextMenu() {
        private val removeBookItem = new MenuItem() {
          text = resourceMgr.getMessage("book_table_panel.context_menu.remove_book")
          onAction = { _ => actionsController.handleRemoveBookAction(row.item.value.book) }
        }
        items.add(removeBookItem)
      }

      row.handleEvent(MouseEvent.MouseClicked) { event: MouseEvent =>
        if (!row.isEmpty) {
          event.button match {
            case MouseButton.Primary =>
              val book = row.item.value.book
              val provider = booksProvider.getBookDataProvider(book)
              event.clickCount match {
                case 1 => bookPreviewDetailsPanelController.initBook(provider)
                case 2 => actionsController.editBook(provider, None)
              }
            case MouseButton.Secondary =>
            case MouseButton.Middle =>
            case _ =>
          }
        }
      }
      row
    }

    booksTableView.columns.foreach { column =>
      column.width.onChange { (_,_,newWidth) => eventBus.publish(BookTableColumnWidthChange(column.id.value, newWidth.intValue())) }
    }

    booksTableView.items = sortedRows

    eventBus.register(this)

    refreshBooks()
  }

  //noinspection ScalaUnusedSymbol
  @Subscribe
  def refreshBooks(serverReconnectedEvent: ServerReconnectedEvent): Unit = {
    logger.info(s"Server connection reestablished - refreshing books")
    refreshBooks()
  }

  //noinspection ScalaUnusedSymbol
  @Subscribe
  def refreshBooks(refreshBooksAction: RefreshBooksAction): Unit = {
    logger.debug(s"Refresh books event received")
    refreshBooks()
  }

  @Subscribe
  def refreshBooks(bookChangeEvent: BookChangeEvent): Unit = {
    logger.info(s"Book change event: $bookChangeEvent - refreshing books")
    refreshBooks()
  }

  def refreshBooks(): Unit = {
    val searchQuery = Option(filterTextField.text.value).filter(q => !q.isBlank)
    logger.debug(s"set filter action: $searchQuery")
    readBooks(searchQuery)
  }

  def readBooks(searchQuery: Option[String]=None): Unit = {
    logger.debug("Loading books in the background")
    Future(booksProvider.readBooks(searchQuery).get)
        .onComplete {
          case Success(books) =>
            Platform.runLater { updateBooksTable(books) }
          case Failure(exception) =>
            logger.warn(s"Error fetching list of books", exception)
        }
  }

  private def updateBooksTable(books: Seq[Book]): Unit = {
    logger.debug(s"Finished loading books from server (count=${books.size}). Refreshing list.")
    val rows = books.map(new BookRow(_, bookFormatResolver))
//    bookRowsMap = rows.map(row => (row.book.id, row)).toMap
    bookRows.setAll(rows.asJava)
  }

  var filterHistoryPopoverVisible = false
  def onFilterHistoryAction(ae: ActionEvent): Unit = {
    logger.debug("filter history action")
    if (!filterHistoryPopoverVisible) {
      filterHistoryPopoverVisible = true

      new PopOverEx {
        title = "Previous filters" // TODO: i18
        detachable = false
        autoHide = true
        headerAlwaysVisible = true
        arrowLocation = PopOver.ArrowLocation.TOP_RIGHT
        onHidden = _ => filterHistoryPopoverVisible = false
      }.show(ae.source.asInstanceOf[javafx.scene.Node])
    }
  }

  //noinspection ScalaUnusedSymbol
  def onSavedSearchesAction(ae: ActionEvent): Unit = {
    logger.debug("saved searches action")
  }
}

