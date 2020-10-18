package com.mikesajak.ebooklib.app.dto

import java.time.LocalDate

case class BookDto(id: Option[String],
                  title: String, authors: Seq[String], tags: Seq[String],
                  identifiers: Seq[String], creationDate: Option[LocalDate],
                  publicationDate: Option[LocalDate], publisher: Option[String],
                  languages: Seq[String], series: Option[SeriesDto],
                  description: Option[String],
                  formatIds: Option[Seq[String]])

case class SeriesDto(title: String, number: Int)
