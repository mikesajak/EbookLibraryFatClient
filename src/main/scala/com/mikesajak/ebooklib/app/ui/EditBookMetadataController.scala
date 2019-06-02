package com.mikesajak.ebooklib.app.ui

import java.time.LocalDate

import com.mikesajak.ebooklib.app.ui.UIUtils.bindHeight
import com.mikesajak.ebooklibrary.payload.{Book, BookFormatMetadata, BookMetadata, Series}
import com.typesafe.scalalogging.Logger
import javafx.scene.{control => jfxctrl}
import javafx.{scene => jfxs}
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{Priority, Region, VBox}
import scalafx.scene.text.Text
import scalafxml.core.macros.sfxml

import scala.collection.JavaConverters._

trait EditBookMetadataController {
  def initialize(bookDataProvider: BookDataProvider, dialog: Dialog[ButtonType],
                 booksNavigator: Option[BooksNav] = None): Unit

  def bookMetadata: BookMetadata
}

trait BooksNav {
  def hasPrevious: Boolean
  def previous(): Book

  def hasNext: Boolean
  def next(): Book

  def current(): Book
}

class BooksCollectionNav(private val books: Seq[Book], private var curPos: Int = 0) extends BooksNav {

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

object BooksCollectionNav {
  val empty = new BooksCollectionNav(Seq.empty)
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
                                     publicationDateTextField: TextField,
                                     publicationDateSelButton: Button,
                                     publisherCombo: ComboBox[String],
                                     languagesCombo: ComboBox[String],
                                     descriptionTextArea: TextArea,
                                     coverImageView: ImageView,
                                     coverOverlayText: Text,

                                     navigationPanel: VBox,
                                     nextPageButton: Button,
                                     prevPageButton: Button,

                                     toolbarSpacer: Region,

                                     implicit val resourceMgr: ResourceManager)
    extends EditBookMetadataController {
  import ResourceManager._

  private val logger = Logger[EditBookMetadataControllerImpl]

  private var dialog: Dialog[ButtonType] = _

  bindHeight(publicationDateSelButton, publicationDateTextField)
  publicationDateSelButton.graphic.value.asInstanceOf[jfxs.image.ImageView].fitHeight.bind(publicationDateSelButton.height.subtract(2))
  toolbarSpacer.hgrow = Priority.Always

  seriesNumSpinner.valueFactory = new IntegerSpinnerValueFactory(0, 100, 0).asInstanceOf[SpinnerValueFactory[Int]]

  override def initialize(bookDataProvider: BookDataProvider, dialog: Dialog[ButtonType],
                          booksNavigator: Option[BooksNav]): Unit = {
    this.dialog = dialog

    coverImageView.image = "default-book-cover.jpg".imgResource

    initBookContent(bookDataProvider.bookMetadata)
    initCoverImage(bookDataProvider.bookCover)
    initFormats(bookDataProvider.bookFormatsMetadata)

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

  def initBookContent(book: BookMetadata): Unit = {
    titleTextField.text = book.getTitle
    initTextFieldTooltip(titleTextField)
    authorsCombo.value = strVal(book.getAuthors, " & ")
    initComboTooltip(authorsCombo, "\\s*&\\s*")

    seriesNumSpinner.disable = true
    seriesCombo.editor.value.textProperty().addListener{ (_, _, newValue) =>
      seriesCombo.value = newValue
      seriesNumSpinner.disable = Option(seriesCombo.value.value).forall(_.isEmpty)
                                                       }
    initComboTooltip(seriesCombo)
    Option(book.getSeries).foreach { series =>
      seriesCombo.value = series.getTitle
      seriesNumSpinner.valueFactory.value.setValue(series.getNumber)
                                   }

    tagsCombo.value = strVal(book.getTags, ", ")
    initComboTooltip(tagsCombo, "\\s*,\\s*")
    identifiersTextField.text = strVal(book.getIdentifiers, ", ")
    initTextFieldTooltip(identifiersTextField, "\\s*,\\s*")
//    creationDateTextField.text = strVal(book.getCreationDate)
    publicationDateTextField.text = strVal(book.getPublicationDate)
    publisherCombo.value = book.getPublisher
    initComboTooltip(publisherCombo)
    languagesCombo.value = strVal(book.getLanguages, ", ")
    initComboTooltip(languagesCombo, "\\s*,\\s*")
  }

  def initCoverImage(cover: Option[Image]): Unit = {
    import ResourceManager._

    updateCoverOverlay("metadata_dialog.cover-loading.label".textResource)

    for (image <- cover) {
      if (image.backgroundLoading) {
        image.progress.onChange { (_, _, progress) =>
          if (progress == 1.0) {
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

  def initFormats(formatsMetadata: Seq[BookFormatMetadata]) = {
    println(formatsMetadata)
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

  private def setCoverImage(image: Image, overlayText: String) {
    coverImageView.image = image
    updateCoverOverlay(overlayText)
  }

  private def updateCoverOverlay(message: String): Unit = {
    coverOverlayText.text = message
    coverOverlayText.layoutX = coverImageView.boundsInLocal.value.getWidth - coverOverlayText.boundsInLocal.value.getWidth - 4
    coverOverlayText.layoutY = coverImageView.boundsInLocal.value.getHeight - 8
  }

  override def bookMetadata: BookMetadata = {
    def parseText(text: String) = if (!empty(text)) text else null
    def parseSeq(text: String, separator: String) = {
      val elems = text.split(s"\\s*$separator\\s*")
      if (elems.nonEmpty) elems.toSeq else null
    }
    def parseDate(text: String) =
      if (!empty(text)) LocalDate.parse(text) else null

    val seriesTitle = seriesCombo.value.value
    val seriesNum = seriesNumSpinner.value.value
    val series = if (!empty(seriesTitle)) new Series(seriesTitle, seriesNum)
                 else null

    new BookMetadata(titleTextField.text.value,
                     parseSeq(authorsCombo.value.value, "&").asJava,
                     parseSeq(tagsCombo.value.value, ",").asJava,
                     parseSeq(identifiersTextField.text.value, ",").asJava,
//                     parseDate(creationDateTextField.text.value),
                     null,
                     parseDate(publicationDateTextField.text.value),
                     parseText(publisherCombo.value.value),
                     parseSeq(languagesCombo.value.value, ",").asJava,
                     series,
                     parseText(descriptionTextArea.text.value))
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

  private def strVal[A](coll: java.util.Collection[A], sep: String) =
    if (coll == null) "" else coll.asScala.mkString(sep)

}
