package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklibrary.payload.BookMetadata
import scalafx.geometry.Insets
import scalafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafxml.core.macros.sfxml

import scala.collection.JavaConverters._

trait EditBookMetadataController {
  def initialize(book: BookMetadata, cover: Option[Image]): Unit
}

@sfxml
class EditBookMetadataControllerImpl(titleTextField: TextField,
                                     titleTooltip: Tooltip,
                                     authorsCombo: ComboBox[String],
                                     authorsTooltip: Tooltip,
                                     seriesCombo: ComboBox[String],
                                     seriesTooltip: Tooltip,
                                     seriesNumSpinner: Spinner[Int],
                                     tagsCombo: ComboBox[String],
                                     tagsTooltip: Tooltip,
                                     identifiersTextField: TextField,
                                     identifiersTooltip: Tooltip,
                                     creationDateTextField: TextField,
                                     creationDateSelButton: Button,
                                     publicationDateTextField: TextField,
                                     publicationDateSelButton: Button,
                                     publisherCombo: ComboBox[String],
                                     publisherTooltip: Tooltip,
                                     languagesCombo: ComboBox[String],
                                     languagesTooltip: Tooltip,
                                     coverImageView: ImageView)
    extends EditBookMetadataController {

  creationDateSelButton.margin = Insets(0,0,0,0)
  creationDateSelButton.padding = Insets(0,0,0,0)
  publicationDateSelButton.margin = Insets(0,0,0,0)
  publicationDateSelButton.padding = Insets(0,0,0,0)

  seriesNumSpinner.valueFactory = new IntegerSpinnerValueFactory(0, 100, 0).asInstanceOf[SpinnerValueFactory[Int]]

  def initialize(book: BookMetadata, cover: Option[Image]): Unit = {
    titleTextField.text = book.getTitle
    initTextFieldTooltip(titleTextField, titleTooltip)
    authorsCombo.value = strVal(book.getAuthors, " & ")
    initComboTooltip(authorsCombo, authorsTooltip, "\\s*&\\s*")

    seriesNumSpinner.disable = true
    seriesCombo.editor.value.textProperty().addListener{ (_, _, newValue) =>
      seriesCombo.value = newValue
      seriesNumSpinner.disable = Option(seriesCombo.value.value).forall(_.isEmpty)
    }
    initComboTooltip(seriesCombo, seriesTooltip)
    Option(book.getSeries).foreach { series =>
      seriesCombo.value = series.getTitle
      seriesNumSpinner.valueFactory.value.setValue(series.getNumber)
    }

    tagsCombo.value = strVal(book.getTags, ", ")
    initComboTooltip(tagsCombo, tagsTooltip, "\\s*,\\s*")
    identifiersTextField.text = strVal(book.getIdentifiers, ", ")
    initTextFieldTooltip(identifiersTextField, identifiersTooltip, "\\s*,\\s*")
    creationDateTextField.text = strVal(book.getCreationDate)
    publicationDateTextField.text = strVal(book.getPublicationDate)
    publisherCombo.value = book.getPublisher
    initComboTooltip(publisherCombo, publisherTooltip )
    languagesCombo.value = strVal(book.getLanguages, ", ")
    initComboTooltip(languagesCombo, languagesTooltip, "\\s*,\\s*")

    for (image <- cover) coverImageView.image = image
  }

  private def initTextFieldTooltip(textControl: TextInputControl, tooltip: Tooltip, separator: String = "^"): Unit =
    initTooltipImpl(() => strVal(textControl.text.value), tooltip, separator)

  private def initComboTooltip(combo: ComboBox[_], tooltip: Tooltip, separator: String = "^"): Unit =
    initTooltipImpl(() => strVal(combo.value.value), tooltip, separator)

  private def initTooltipImpl(textFun: () => String, tooltip: Tooltip, separator: String = "^"): Unit = {
    val elements = textFun().split(separator)
    tooltip.onShowing = _ => tooltip.text = elements.reduce{(elem1, elem2) => s"$elem1\n$elem2"}
  }

  private def strVal(ob: Any) = if (ob == null) "" else ob.toString

  private def strVal[A](coll: java.util.Collection[A], sep: String) =
    if (coll == null) "" else coll.asScala.mkString(sep)

}
