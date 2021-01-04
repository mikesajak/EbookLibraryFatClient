package com.mikesajak.ebooklib.app.reader

import java.time.LocalDate

import com.mikesajak.ebooklib.app.util.OptionOps._

case class BookFormatData(contentType: String, titles: Seq[String], authors: Seq[String], keywords: Seq[String],
                          creationDates: Seq[LocalDate], publisher: Option[String],
                          identifiers: Seq[String], language: Option[String], description: Option[String],
                          stats: Option[BookStats]) {

  def merge(other: BookFormatData): BookFormatData = {
    BookFormatData(contentType, (titles ++ other.titles).distinct.sorted, (authors ++ other.authors).distinct.sorted,
                   (keywords ++ other.keywords).distinct.sorted, (creationDates ++ other.creationDates).distinct.sorted,
                   publisher.orElse(other.publisher), (identifiers ++ other.identifiers).distinct.sorted,
                   language.orElse(other.language), description.orElse(other.description),
                   merge(stats, other.stats))
  }

  private def merge(stats1: Option[BookStats], stats2: Option[BookStats]) = {
    if (stats1.isDefined && stats2.isDefined) Some(stats1.get.merge(stats2.get))
    else stats1.selectExisting(stats2)
  }

}

case class BookStats(pageCount: Option[Int], wordCount: Option[Int], characterCount: Option[Int]) {
  def merge(other: BookStats): BookStats = BookStats(pageCount.selectExisting(other.pageCount),
                                                     wordCount.selectExisting(other.wordCount),
                                                     characterCount.selectExisting(other.characterCount))

}