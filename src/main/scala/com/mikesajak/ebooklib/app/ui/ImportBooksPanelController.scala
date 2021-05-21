package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.model.BookFormatMetadata
import scalafx.Includes._
import scalafx.beans.property.{BooleanProperty, StringProperty}
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.collections.transformation.{FilteredBuffer, SortedBuffer}
import scalafx.geometry.Pos
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

import java.io.File
import scala.jdk.CollectionConverters._

//noinspection TypeAnnotation
class ImportedBookRow(val file: File, val bookDataProvider: Option[BookDataProvider], formatResolver: BookFormatResolver) {
  val filename = StringProperty(file.getPath)
  val status = BooleanProperty(bookDataProvider.isDefined)
  val title = StringProperty(bookDataProvider.map(_.bookMetadata.title).getOrElse(""))
  val authors = StringProperty(bookDataProvider.map(_.bookMetadata.authors.mkString(", ")).getOrElse(""))
  val tags = StringProperty(bookDataProvider.map(_.bookMetadata.tags.mkString(", ")).getOrElse(""))
  val identifiers = StringProperty(bookDataProvider.map(_.bookMetadata.identifiers.mkString(", ")).getOrElse(""))
  val creationDate = StringProperty(bookDataProvider.map(_.bookMetadata.creationDate).map(_.toString).getOrElse(""))
  val publicationDate = StringProperty(bookDataProvider.map(_.bookMetadata.publicationDate).map(_.toString).getOrElse(""))
  val publisher = StringProperty(bookDataProvider.flatMap(_.bookMetadata.publisher).orNull)
  val languages = StringProperty(bookDataProvider.map(_.bookMetadata.languages.mkString(", ")).getOrElse(""))
  val formats = StringProperty(bookDataProvider.map(_.bookMetadata.formats.map(resolveFormat).mkString(", ")).getOrElse(""))

  private def resolveFormat(formatMeta: BookFormatMetadata) =
    formatResolver.forMimeType(formatMeta.formatType).description
}

trait ImportBooksPanelController {
  def init(importedBooks: Seq[(File, Option[BookDataProvider])]): Unit
}

@sfxml
class ImportBooksPanelControllerImpl(booksTableView: TableView[ImportedBookRow],
                                     filenameColumn: TableColumn[ImportedBookRow, String],
                                     statusColumn: TableColumn[ImportedBookRow, Boolean],
                                     titleColumn: TableColumn[ImportedBookRow, String],
                                     authorsColumn: TableColumn[ImportedBookRow, String],
                                     identifiersColumn: TableColumn[ImportedBookRow, String],
                                     createdColumn: TableColumn[ImportedBookRow, String],
                                     publishedColumn: TableColumn[ImportedBookRow, String],
                                     publisherColumn: TableColumn[ImportedBookRow, String],
                                     languageColumn: TableColumn[ImportedBookRow, String],
                                     formatColumn: TableColumn[ImportedBookRow, String],

                                     bookFormatResolver: BookFormatResolver,
                                     resourceMgr: ResourceManager)
    extends ImportBooksPanelController {

  private val bookRows = ObservableBuffer[ImportedBookRow]()
  private val filteredRows = new FilteredBuffer(bookRows)
  private val sortedRows = new SortedBuffer(filteredRows)

  filenameColumn.cellValueFactory = {_.value.filename}
  filenameColumn.cellFactory = { _: TableColumn[ImportedBookRow, String] =>
    new TableCell[ImportedBookRow, String]() {
      textOverrun = OverrunStyle.LeadingEllipsis
      item.onChange { (_, _, newValue) =>
        text = newValue
        //        graphic = Option(newValue)
        //            .flatMap(cellPath => iconResolver.findIconFor(cellPath))
        //            .orNull
        val curRowIdx = index.value
        val tableItems = tableView.value.items.value
        val fileRowOpt = if (curRowIdx >= 0 && curRowIdx < tableItems.size) Option(tableItems.get(curRowIdx))
                         else None
        tooltip = fileRowOpt.map { row =>
          row.bookDataProvider.map { provider =>
            resourceMgr.getMessage("import_book_table_panel.row_tooltip",
                                   row.filename.value, row.title.value, row.authors.value,
                                   row.identifiers.value, row.creationDate.value, row.creationDate.value, // publication date
                                   row.publisher.value, row.languages.value, row.formats.value)
          }.getOrElse(resourceMgr.getMessage("import_book_table.panel.row_error_tooltip",
                                             row.filename.value, "Failed to parse book file")) // TODO: better message text (maybe exception message contain some details about error)
        }.map(text => new Tooltip(text)).orNull
      }
    }
  }
  statusColumn.cellValueFactory = { t =>
    BooleanProperty(t.value.bookDataProvider.isDefined).asInstanceOf[ObservableValue[Boolean, Boolean]]
  }
  statusColumn.cellFactory = { _: TableColumn[ImportedBookRow, Boolean] =>
    new TableCell[ImportedBookRow, Boolean]() {
      alignment = Pos.Center
      item.onChange { (_, _, newValue) =>
        val icon = if (newValue) "icons8-ok-48.png" else "icons8-cancel-48.png"
        graphic = getIconIView(icon)

      }
    }
  }

  def getIconIView(iconName: String): ImageView = {
    val image = new ImageView(resourceMgr.getImage(iconName))
    image.fitWidth = 16
    image.fitHeight = 16
    image
  }


  titleColumn.cellValueFactory = {_.value.title}
  authorsColumn.cellValueFactory = {_.value.authors}
  identifiersColumn.cellValueFactory = {_.value.identifiers}
  createdColumn.cellValueFactory = {_.value.creationDate}
  publishedColumn.cellValueFactory = {_.value.publicationDate}
  publisherColumn.cellValueFactory = {_.value.publisher}
  languageColumn.cellValueFactory = {_.value.languages}
  formatColumn.cellValueFactory = {_.value.formats}

  override def init(importedBooks: Seq[(File, Option[BookDataProvider])]): Unit = {
    booksTableView.items = sortedRows
    val bookRowsSeq = importedBooks.map(ib => new ImportedBookRow(ib._1, ib._2, bookFormatResolver))
    bookRows.setAll(bookRowsSeq.asJava)
  }
}
