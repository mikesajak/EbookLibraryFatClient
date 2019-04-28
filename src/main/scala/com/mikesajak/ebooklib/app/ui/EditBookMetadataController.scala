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
                                     coverImageView: ImageView)
    extends EditBookMetadataController {

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

    for (image <- cover) coverImageView.image = image
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
