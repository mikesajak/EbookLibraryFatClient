package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import scalafx.scene.control.Label
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

trait BookDetailsPreviewController {
  def initBook(bookDataProvider: BookDataProvider): Unit
}

@sfxml
class BookDetailsPreviewControllerImpl(coverImageView: ImageView,
                                       titleValueLabel: Label,
                                       authorsValueLabel: Label,
                                       formatsValueLabel: Label,
                                       identifiersValueLabel: Label,
                                       tagsValueLabel: Label,
                                       langsValueLabel: Label,

                                       formatResolver: BookFormatResolver)
    extends BookDetailsPreviewController {

  def initBook(bookDataProvider: BookDataProvider): Unit = {
    val book = bookDataProvider.bookMetadata
    titleValueLabel.text = book.title

    authorsValueLabel.text = book.authors.mkString(" & ")
    formatsValueLabel.text = book.formats.map(fmt => formatResolver.forMimeType(fmt.formatType).description).mkString(", ")
    identifiersValueLabel.text = (book.identifiers ++ bookDataProvider.bookId.map(id => s"internal:$id")).mkString(", ")
    tagsValueLabel.text = book.tags.mkString(", ")
    langsValueLabel.text = book.languages.mkString(", ")

    bookDataProvider.bookCover.foreach(coverImage => coverImageView.image = coverImage)
  }

}
