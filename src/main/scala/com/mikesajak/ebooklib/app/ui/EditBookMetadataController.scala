package com.mikesajak.ebooklib.app.ui

import java.time.LocalDate

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

  creationDateSelButton.margin = Insets(0,0,0,0)
  creationDateSelButton.padding = Insets(0,0,0,0)
  publicationDateSelButton.margin = Insets(0,0,0,0)
  publicationDateSelButton.padding = Insets(0,0,0,0)

  seriesNumSpinner.valueFactory = new IntegerSpinnerValueFactory(0, 100, 0).asInstanceOf[SpinnerValueFactory[Int]]

  def initialize(book: BookMetadata, cover: Option[Image]): Unit = {
    titleTextField.text = book.getTitle
    authorsCombo.value = strVal(book.getAuthors, " & ")

    seriesNumSpinner.disable = true
    seriesCombo.editor.value.textProperty().addListener{ (_, _, newValue) =>
      seriesCombo.value = newValue
      seriesNumSpinner.disable = Option(seriesCombo.value.value).forall(_.isEmpty)
    }
    Option(book.getSeries).foreach { series =>
      seriesCombo.value = series.getTitle
      seriesNumSpinner.valueFactory.value.setValue(series.getNumber)
    }

    tagsCombo.value = strVal(book.getTags, ", ")
    identifiersTextField.text = strVal(book.getIdentifiers, ", ")
    creationDateTextField.text = strVal(book.getCreationDate)
    publicationDateTextField.text = strVal(book.getPublicationDate)
    publisherCombo.value = book.getPublisher
    languagesCombo.value = strVal(book.getLanguages, ", ")

    for (image <- cover) coverImageView.image = image
  }

  private def strVal(date: LocalDate) =
    if (date == null) "" else date.toString

  private def strVal[A](coll: java.util.Collection[A], sep: String) = {
    if (coll == null) ""
    else coll.asScala.mkString(sep)
  }

}
