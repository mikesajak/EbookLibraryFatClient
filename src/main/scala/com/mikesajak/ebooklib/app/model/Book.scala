package com.mikesajak.ebooklib.app.model

import com.mikesajak.ebooklib.app.reader.BookFormatData

import java.time.LocalDate

case class BookId(value: String) {
  override def toString: String = value
}

object BookId {
  val NotExisting: BookId = BookId("Not existing")
}

case class Book(id: BookId, metadata: BookMetadata)

case class BookMetadata(title: String, authors: Seq[String], tags: Seq[String], identifiers: Seq[String],
                        creationDate: Option[LocalDate], publicationDate: Option[LocalDate], publisher: Option[String],
                        languages: Seq[String], series: Option[Series], description: Option[String],
                        formats: Seq[BookFormatMetadata])

object BookMetadata {
  def from(title: String, authors: Seq[String], tags: Seq[String], identifiers: Seq[String],
           creationDate: Option[LocalDate], publicationDate: Option[LocalDate], publisher: Option[String], languages: Seq[String],
           series: Option[Series], description: Option[String], formats: Seq[BookFormatMetadata] = Seq()): BookMetadata = {
    BookMetadata(title = title, authors = authors.distinct, tags = tags.distinct,
                 identifiers = identifiers.distinct, creationDate = creationDate, publicationDate = publicationDate,
                 publisher = publisher, languages = languages.distinct, series = series,
                 description = description, formats = formats)
  }

  def from(bookFormatData: BookFormatData, bookFormatsMetadata: Seq[BookFormatMetadata]): BookMetadata =
    BookMetadata(bookFormatData.titles.mkString(", "),
                 bookFormatData.authors.distinct,
                 bookFormatData.keywords.distinct,
                 bookFormatData.identifiers.distinct,
                 bookFormatData.creationDate,
                 bookFormatData.publicationDate,
                 bookFormatData.publisher,
                 bookFormatData.language.toSeq.distinct,
                 None, bookFormatData.description,
                 bookFormatsMetadata)
}

case class Series(title: String, number: Int)
