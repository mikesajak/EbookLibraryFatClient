package com.mikesajak.ebooklib.app.ui

import scalafx.scene.control.Label
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

import scala.collection.JavaConverters._

trait BookDetailsPreviewController {
  def initBook(bookDataProvider: BookDataProvider)
}

@sfxml
class BookDetailsPreviewControllerImpl(coverImageView: ImageView,
                                       titleValueLabel: Label,
                                       authorsValueLabel: Label,
                                       formatsValueLabel: Label,
                                       identifiersValueLabel: Label,
                                       tagsValueLabel: Label,
                                       langsValueLabel: Label)
    extends BookDetailsPreviewController {

  def initBook(bookDataProvider: BookDataProvider): Unit = {
    val book = bookDataProvider.bookMetadata
    titleValueLabel.text = book.getTitle

    authorsValueLabel.text = book.getAuthors.asScala.reduceLeftOption((a,b) => s"$a & $b").getOrElse("")
    formatsValueLabel.text = "TODO"
    identifiersValueLabel.text = book.getIdentifiers.asScala.reduceLeftOption((a,b) => s"$a, $b").getOrElse("")
    tagsValueLabel.text = book.getTags.asScala.reduceLeftOption((a,b) => s"$a, $b").getOrElse("")
    langsValueLabel.text = book.getLanguages.asScala.reduceLeftOption((a,b) => s"$a, $b").getOrElse("")

    bookDataProvider.bookCover.foreach(coverImage => coverImageView.image = coverImage)
  }

}
