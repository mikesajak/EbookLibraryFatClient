package com.mikesajak.ebooklib.app.ui

import java.time.LocalDate

import com.mikesajak.ebooklibrary.payload.{BookMetadata, Series}
import com.typesafe.scalalogging.Logger
import javafx.scene.{control => jfxctrl}
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{Pane, Priority, VBox}
import scalafx.scene.text.Text
import scalafxml.core.macros.sfxml

import scala.collection.JavaConverters._

trait EditBookMetadataController {
  def initialize(book: BookMetadata, cover: Option[Image], dialog: Dialog[ButtonType]): Unit

  def bookMetadata: BookMetadata
}

trait CollectionNav {
  def hasPrev: Boolean
  def prevBook(): BookMetadata
  def hasNext: Boolean
  def nextBook(): BookMetadata
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
                                     descriptionArea: TextArea,
                                     coverImageView: ImageView,
                                     coverOverlayText: Text,

                                     navigationPanel: VBox,
                                     nextButton: Button,
                                     prevButton: Button,

                                     toolbarSpacer: Pane,

                                     implicit val resourceMgr: ResourceManager)
    extends EditBookMetadataController {
  import ResourceManager._


  private val logger = Logger[EditBookMetadataControllerImpl]

  private var dialog: Dialog[ButtonType] = _

  zeroMargins(creationDateSelButton, publicationDateSelButton)
  toolbarSpacer.hgrow = Priority.Always

  seriesNumSpinner.valueFactory = new IntegerSpinnerValueFactory(0, 100, 0).asInstanceOf[SpinnerValueFactory[Int]]

  override def initialize(book: BookMetadata, cover: Option[Image], dialog: Dialog[ButtonType]): Unit = {
    this.dialog = dialog

    coverImageView.image = "default-book-cover.jpg".imgResource

    initBookContent(book)
    initCoverImage(cover)

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
    creationDateTextField.text = strVal(book.getCreationDate)
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
                     parseDate(creationDateTextField.text.value),
                     parseDate(publicationDateTextField.text.value),
                     parseText(publisherCombo.value.value),
                     parseSeq(languagesCombo.value.value, ",").asJava,
                     series,
                     parseText(descriptionArea.text.value))
  }

  private def empty(text: String) = text == null || text.isEmpty

  def handleSwapTitleAndAuthors(): Unit = {
    val title = titleTextField.text.value
    titleTextField.text = authorsCombo.value.value
    authorsCombo.value = title
  }

  def onNextAction(): Unit = {
    logger.debug("onNextAction")
  }

  def onPrevAction(): Unit = {
    logger.debug("onPrevAction")
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
