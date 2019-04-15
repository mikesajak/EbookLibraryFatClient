package com.mikesajak.ebooklib.app.ui

import java.time.LocalDate

import com.mikesajak.ebooklibrary.payload.Book
import scalafx.geometry.Insets
import scalafx.scene.control.{Button, ComboBox, Spinner, TextField}
import scalafxml.core.macros.sfxml

import scala.collection.JavaConverters._

trait EditBookMetadataController {
  def initialize(book: Book): Unit
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
                                     languagesCombo: ComboBox[String])
    extends EditBookMetadataController {

  creationDateSelButton.margin = Insets(0,0,0,0)
  creationDateSelButton.padding = Insets(0,0,0,0)
  publicationDateSelButton.margin = Insets(0,0,0,0)
  publicationDateSelButton.padding = Insets(0,0,0,0)

  def initialize(book: Book): Unit = {
    val meta = book.getMetadata
    titleTextField.text = meta.getTitle
    authorsCombo.value = strVal(meta.getAuthors, " & ")
//    seriesCombo.value = meta
//    seriesNumSpinner.value =
    tagsCombo.value = strVal(meta.getTags, ", ")
    identifiersTextField.text = strVal(meta.getIdentifiers, ", ")
    creationDateTextField.text = strVal(meta.getCreationDate)
    publicationDateTextField.text = strVal(meta.getPublicationDate)
    publisherCombo.value = meta.getPublisher
    languagesCombo.value = strVal(meta.getLanguages, ", ")
  }

  private def strVal(date: LocalDate) =
    if (date == null) "" else date.toString

  private def strVal[A](coll: java.util.Collection[A], sep: String) = {
    if (coll == null) ""
    else coll.asScala.mkString(sep)
  }

}
