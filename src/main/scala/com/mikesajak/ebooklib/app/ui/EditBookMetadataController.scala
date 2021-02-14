package com.mikesajak.ebooklib.app.ui

import com.google.inject.name.Names
import com.mikesajak.ebooklib.app.ApplicationContext
import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.model.{Book, BookFormatMetadata, BookMetadata, Series}
import com.mikesajak.ebooklib.app.ui.UIUtils.bindHeight
import com.mikesajak.ebooklib.app.util.Util.using
import com.typesafe.scalalogging.Logger
import javafx.scene.input.MouseButton
import javafx.scene.{control => jfxctrl}
import javafx.{scene => jfxs}
import net.codingwell.scalaguice.InjectorExtensions.ScalaInjector
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{Priority, Region, VBox}
import scalafx.scene.text.Text
import scalafxml.core.macros.sfxml

import java.awt.Desktop
import java.io.{BufferedOutputStream, File, FileOutputStream}
import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

trait EditBookMetadataController {
  def initialize(bookDataProvider: BookDataProvider, dialog: Dialog[ButtonType],
                 booksNavigator: Option[BooksNavigator] = None): Unit

  def bookMetadata: BookMetadata

  def bookFormats(): Seq[BookFormatMetadata]
}

trait BooksNavigator {
  def hasPrevious: Boolean
  def previous(): Book

  def hasNext: Boolean
  def next(): Book

  def current(): Book
}

class BooksCollectionNavigator(private val books: Seq[Book], private var curPos: Int = 0) extends BooksNavigator {

  def hasPrevious: Boolean = books.nonEmpty && curPos > 0
  def previous(): Book = {
    curPos -= 1
    books(curPos)
  }

  def hasNext: Boolean = curPos < books.size - 1
  def next(): Book = {
    curPos += 1
    books(curPos)
  }

  def current(): Book = books(curPos)
}

object BooksCollectionNavigator {
  val empty = new BooksCollectionNavigator(Seq.empty)
}

@sfxml
class EditBookMetadataControllerImpl(titleTextField: TextField,
                                     authorsCombo: ComboBox[String],
                                     seriesCombo: ComboBox[String],
                                     seriesNumSpinner: Spinner[Int],
                                     tagsCombo: ComboBox[String],
                                     identifiersTextField: TextField,
                                     creationDateTextField: TextField,
                                     creationDateSelButton: Button,
                                     publisherCombo: ComboBox[String],
                                     languagesCombo: ComboBox[String],
                                     descriptionTextArea: TextArea,
                                     coverImageView: ImageView,
                                     coverOverlayText: Text,

                                     bookFormatsListView: ListView[BookFormatMetadata],

                                     navigationPanel: VBox,
                                     nextPageButton: Button,
                                     prevPageButton: Button,

                                     toolbarSpacer: Region,

                                     bookFormatResolver: BookFormatResolver,
                                     implicit val resourceMgr: ResourceManager)
    extends EditBookMetadataController {
  import ResourceManager._

  private val logger = Logger[EditBookMetadataControllerImpl]
  private implicit val ec: ExecutionContext =
    ApplicationContext.globalInjector.instance[ExecutionContext](Names.named("externalOpenExecutionContext"))

  private var dialog: Dialog[ButtonType] = _

  bindHeight(creationDateSelButton, creationDateTextField)
  creationDateSelButton.graphic.value.asInstanceOf[jfxs.image.ImageView].fitHeight.bind(creationDateSelButton.height.subtract(2))
  toolbarSpacer.hgrow = Priority.Always

  seriesNumSpinner.valueFactory = new IntegerSpinnerValueFactory(0, 100, 0).asInstanceOf[SpinnerValueFactory[Int]]

  override def initialize(bookDataProvider: BookDataProvider, dialog: Dialog[ButtonType],
                          booksNavigator: Option[BooksNavigator]): Unit = {
    this.dialog = dialog

    coverImageView.image = "default-book-cover.jpg".imgResource

    initBookContent(bookDataProvider.bookMetadata)
    initCoverImage(bookDataProvider.bookCover)
    initFormats(bookDataProvider.bookFormatsMetadata)

    bookFormatsListView.onMouseClicked = { me =>
      if (me.getClickCount == 2) {
        if (Desktop.isDesktopSupported) Future {
          openExternally(bookDataProvider)
        }
      }
    }

    booksNavigator match {
      case Some(nav) =>
      case None =>
        prevPageButton.disable = true
        nextPageButton.disable = true
    }

    val dialogPane = dialog.dialogPane.value
    dialogPane.buttonTypes = Seq(ButtonType.Cancel, ButtonType.OK)

    val cancelButton = prepareDialogButton(ButtonType.Cancel, "icons8-cancel-48.png")
    val okButton = prepareDialogButton(ButtonType.OK, "icons8-ok-48.png")
    okButton.defaultButton = true
  }

  private def openExternally(bookDataProvider: BookDataProvider): Unit = {
    val selectedBookData = bookFormatsListView.selectionModel.value.getSelectedItem
    val bookFormat = bookDataProvider.bookFormat(selectedBookData.formatId)

    val extension = bookFormatResolver.forMimeType(selectedBookData.formatType)
                                      .extensions.headOption

    val filename = selectedBookData.filename.map(name => name.replaceFirst("(.+/)?(.+)$", "$2"))

    val tempFile = File.createTempFile(s"EbookLibraryClient-", //${filename.getOrElse("")}",
                                       s"${filename.getOrElse(extension.get)}")
    using(new BufferedOutputStream(new FileOutputStream(tempFile))) { outStream =>
      outStream.write(bookFormat.contents)
    }

    logger.debug(s"Running desktop application for (temporary book file: ${tempFile.getCanonicalPath}")
    Desktop.getDesktop.open(tempFile)
  }

  private def initBookContent(book: BookMetadata): Unit = {
    titleTextField.text = book.title
    initTextFieldTooltip(titleTextField)
    authorsCombo.value = book.authors.mkString(" & ")
    initComboTooltip(authorsCombo, "\\s*&\\s*")

    seriesNumSpinner.disable = true
    seriesCombo.editor.value.textProperty().addListener{ (_, _, newValue) =>
      seriesCombo.value = newValue
      seriesNumSpinner.disable = Option(seriesCombo.value.value).forall(_.isEmpty)
    }
    initComboTooltip(seriesCombo)
    book.series.foreach { series =>
      seriesCombo.value = series.title
      seriesNumSpinner.valueFactory.value.setValue(series.number)
    }

    tagsCombo.value = book.tags.mkString(", ")
    initComboTooltip(tagsCombo, "\\s*,\\s*")
    identifiersTextField.text = book.identifiers.mkString(", ")
    initTextFieldTooltip(identifiersTextField, "\\s*,\\s*")
    creationDateTextField.text = book.creationDates.map(_.toString).headOption.orNull
    publisherCombo.value = book.publisher.orNull
    initComboTooltip(publisherCombo)
    languagesCombo.value = book.languages.mkString(", ")
    initComboTooltip(languagesCombo, "\\s*,\\s*")
    descriptionTextArea.text = book.description.orNull
  }

  private def initCoverImage(cover: Option[Image]): Unit = {
    import ResourceManager._

    updateCoverOverlay("metadata_dialog.cover-loading.label".textResource)

    for (image <- cover) {
      if (image.backgroundLoading) {
        image.progress.onChange { (_, _, progress) =>
          if (progress.doubleValue == 1.0) {
            if (image.isError) {
              logger.info(s"Error loading cover image", image.exception.value)
              setCoverImage("default-book-cover.jpg".imgResource, "metadata_dialog.default-cover.label".textResource)
            } else {
              setCoverImage(image, s"${image.width.toInt}x${image.height.toInt}")
            }
          }
        }
      }
      else setCoverImage(image, s"${image.width.toInt}x${image.height.toInt}")
    }
  }

  private def initFormats(bookFormats: Seq[BookFormatMetadata]): Unit = {
    bookFormatsListView.items.value.clear()
    bookFormatsListView.cellFactory = { p =>
      val cell = new ListCell[BookFormatMetadata]
      cell.item.onChange { (_,_, format) =>
        if (format != null)
          cell.text = bookFormatResolver.forMimeType(format.formatType).description
      }
      cell.onMouseClicked = { me =>
        if (me.getButton == MouseButton.SECONDARY) {
          println(s"Right click on cell: $cell")
        }
      }
      cell
    }
    bookFormatsListView.items.value ++= bookFormats
  }

  private def prepareDialogButton(buttonType: ButtonType, iconName: String) = {
    import ResourceManager._

    val b = dialog.getDialogPane.lookupButton(buttonType).asInstanceOf[jfxctrl.Button]
    val btn = new Button(b)
    btn.defaultButton = false
    btn.graphic = imageView(iconName.imgResource, 16)
    btn
  }

  private def imageView(image: Image, fitSize: Int) = {
    val iv = new ImageView(image)
    iv.fitWidth = fitSize
    iv.fitHeight = fitSize
    iv
  }

  private def setCoverImage(image: Image, overlayText: String): Unit = {
    coverImageView.image = image
    updateCoverOverlay(overlayText)
  }

  private def updateCoverOverlay(message: String): Unit = {
    coverOverlayText.text = message
    coverOverlayText.layoutX = coverImageView.boundsInLocal.value.getWidth - coverOverlayText.boundsInLocal.value.getWidth - 4
    coverOverlayText.layoutY = coverImageView.boundsInLocal.value.getHeight - 8
  }

  override def bookMetadata: BookMetadata = {
    def parseText(text: String) = if (!empty(text)) Some(text) else None
    def parseSeq(text: String, separator: String) = {
      val elems = text.split(s"\\s*$separator\\s*")
      if (elems.nonEmpty) elems.toSeq else null
    }
    def parseDate(text: String) =
      if (!empty(text)) Some(LocalDate.parse(text)) else None

    val seriesTitle = seriesCombo.value.value
    val seriesNum = seriesNumSpinner.value.value
    val series = if (!empty(seriesTitle)) Some(Series(seriesTitle, seriesNum))
                 else None

    BookMetadata.from(title = titleTextField.text.value,
                      authors = parseSeq(authorsCombo.value.value, "&"),
                      tags = parseSeq(tagsCombo.value.value, ","),
                      identifiers = parseSeq(identifiersTextField.text.value, ","),
                      creationDates = parseDate(creationDateTextField.text.value).toList,
                      publisher = parseText(publisherCombo.value.value),
                      languages = parseSeq(languagesCombo.value.value, ","),
                      series = series,
                      description = parseText(descriptionTextArea.text.value))
  }

  override def bookFormats(): Seq[BookFormatMetadata] = {
    bookFormatsListView.items.value.toList
  }

  private def empty(text: String) = text == null || text.isEmpty

  def handleSwapTitleAndAuthors(): Unit = {
    val title = titleTextField.text.value
    titleTextField.text = authorsCombo.value.value
    authorsCombo.value = title
  }

  def handlePublicationDateSelAction(): Unit = {
    logger.debug("handlePublicationDateSelAction")
  }

  // ---------------------------------------------------
  // toolbar actions

  def handleDownloadMetadataAction(): Unit = {
    logger.debug("handleDownloadMetadataAction")
  }

  def handleOpenCoverFileAction(): Unit = {
    logger.debug("handleOpenCoverFileAction")
  }

  def handleCropCoverAction(): Unit = {
    logger.debug("handleCropCoverAction")
  }

  def handleRemoveCoverAction(): Unit = {
    logger.debug("handleRemoveCoverAction")
  }

  def handleDownloadCoverAction(): Unit = {
    logger.debug("handleDownloadCoverAction")
  }


  def handleAddBookFormatAction(): Unit = {
    logger.debug("handleAddBookFormatAction")
  }

  def handleRemoveBookFormatAction(): Unit = {
    logger.debug("handleAddBookFormatAction")
  }

  def handleExtractImageFromFormatAction(): Unit = {
    logger.debug("handleExtractImageFromFormatAction")
  }

  def handleExtractMetadataFromFormatAction(): Unit = {
    logger.debug("handleExtractMetadataFromFormatAction")
  }



  def handleNextPageAction(): Unit = {
    logger.debug("handleNextAction")
  }

  def handlePrevPageAction(): Unit = {
    logger.debug("handlePrevAction")
  }


  private def zeroMargins(buttons: Button*): Unit =
    buttons.foreach { b =>
      b.margin = Insets(0,0,0,0)
      b.padding = Insets(0,0,0,0)
    }

  private def initTextFieldTooltip(textControl: TextInputControl, separator: String = "^"): Unit =
    initTooltipImpl(() => strVal(textControl.text.value), textControl, separator)

  private def initComboTooltip(combo: ComboBox[_], separator: String = "^"): Unit =
    initTooltipImpl(() => strVal(combo.value.value), combo, separator)

  private def initTooltipImpl(textFun: () => String, control: Control, separator: String = "^"): Unit = {
    if (control.tooltip.value == null) control.tooltip = ""
    val elements = textFun().split(separator)
    val tooltip: Tooltip = new Tooltip(control.tooltip.value)
    tooltip.onShowing = _ => tooltip.text = elements.reduce{(elem1, elem2) => s"$elem1\n$elem2"}
  }

  private def strVal(ob: Any) = if (ob == null) "" else ob.toString
}
