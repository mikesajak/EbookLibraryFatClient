package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklibrary.payload.BookMetadata
import com.typesafe.scalalogging.Logger
import scalafx.geometry.Insets
import scalafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.text.Text
import scalafxml.core.macros.sfxml

import scala.collection.JavaConverters._

trait EditBookMetadataController {
  def initialize(book: BookMetadata, cover: Option[Image]): Unit
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
                                     coverImageView: ImageView,
                                     coverOverlayText: Text,

                                     resourceMgr: ResourceManager)
    extends EditBookMetadataController {

  private val logger = Logger[EditBookMetadataControllerImpl]

  zeroMargins(creationDateSelButton, publicationDateSelButton)

  seriesNumSpinner.valueFactory = new IntegerSpinnerValueFactory(0, 100, 0).asInstanceOf[SpinnerValueFactory[Int]]

  def initialize(book: BookMetadata, cover: Option[Image]): Unit = {
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

    setCoverImage("default-book-cover.jpg", "metadata_dialog.cover-loading.label")

    for (image <- cover) {
      if (image.backgroundLoading) {
        image.progress.onChange { (_, _, progress) =>
          if (progress == 1.0) {
            if (image.isError) {
              logger.info(s"Error loading cover image", image.exception.value)
              setCoverImage("default-book-cover.jpg", "metadata_dialog.default-cover.label")
            } else {
              coverImageView.image = image
              updateCoverOverlay(image)
            }
          }
        }
      }
      else updateCoverOverlay(image)
    }
  }

  private def setCoverImage(imgName: String, overlayTextKey: String) {
    val image = resourceMgr.getImage(imgName)
    coverImageView.image = image
    val overlayText = resourceMgr.getMessage(overlayTextKey)
    updateCoverOverlay(image, overlayText)
  }

  private def updateCoverOverlay(image: Image): Unit = {
    coverOverlayText.text = s"${image.width.toInt}x${image.height.toInt}"
    coverOverlayText.layoutX = coverImageView.boundsInLocal.value.getWidth - coverOverlayText.boundsInLocal.value.getWidth - 4
    coverOverlayText.layoutY = coverImageView.boundsInLocal.value.getHeight - 8
  }

  private def updateCoverOverlay(image: Image, message: String): Unit = {
    coverOverlayText.text = message
    coverOverlayText.layoutX = coverImageView.boundsInLocal.value.getWidth - coverOverlayText.boundsInLocal.value.getWidth - 4
    coverOverlayText.layoutY = coverImageView.boundsInLocal.value.getHeight - 8
  }

  def handleSwapTitleAndAuthors() = {
    val title = titleTextField.text.value
    titleTextField.text = authorsCombo.value.value
    authorsCombo.value = title
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
