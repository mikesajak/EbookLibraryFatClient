package com.mikesajak.ebooklib.app.model

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

case class Series(title: String, number: Int)
