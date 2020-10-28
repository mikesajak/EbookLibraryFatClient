package com.mikesajak.ebooklib.app.reader

import java.io.InputStream
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, ZonedDateTime}

import com.mikesajak.ebooklib.app.model.CoverImage
import com.mikesajak.ebooklib.app.util.OptionOps.allEmpty
import com.typesafe.scalalogging.Logger
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler

import scala.util.Try

class TikaBookFormatDataReader extends BookFormatDataReader {
  private val logger = Logger[TikaBookFormatDataReader]

  private val AuthorKeys = List("author", "Author", "dc:author", "meta:author", "creator", "dc:creator", "Last-Author", "meta:last-author")
  private val TitleKeys = List("title", "Title", "dc:title")
  private val KeywordsKeys = List("Keywords", "meta:keyword", "subject", "dc:subject")
  private val LanguageKeys = List("language", "dc:language")
  private val IdentifierKeys = List("identifier", "dc:identifier")
  private val DescriptionKeys = List("description", "dc:description")
  private val CreationDateKeys = List("Creation-Date", "created", "dcterms:created", "meta:creation-date", "pdf:docinfo:created")
  private val PublicationDateKeys = List[String]()
  private val PublisherKeys = List("publisher", "dc:publisher")
  private val PageCountKeys = List("meta:page-count", "Page-Count", "Page Count", "xmpTPg:NPages")
  private val WordCountKeys = List("meta:word-count")
  private val CharacterCountKeys = List("meta:character-count", "Character-Count", "Character Count")

  private val DateTimeFormatters = List(DateTimeFormatter.BASIC_ISO_DATE, DateTimeFormatter.ISO_DATE,
                                        DateTimeFormatter.ISO_DATE_TIME, DateTimeFormatter.ISO_LOCAL_DATE,
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME, DateTimeFormatter.ISO_OFFSET_DATE,
                                        DateTimeFormatter.ISO_OFFSET_DATE_TIME, DateTimeFormatter.ISO_ORDINAL_DATE,
                                        DateTimeFormatter.ISO_ZONED_DATE_TIME, DateTimeFormatter.RFC_1123_DATE_TIME,
                                        DateTimeFormatter.ISO_INSTANT)

  override def read(bookDataInputStream: InputStream): Either[Throwable, BookFormatData] = Try {
    import TikaBookFormatDataReader._

    val parser = new AutoDetectParser()
    val metadata = new Metadata()
    parser.parse(bookDataInputStream, new BodyContentHandler(Int.MaxValue), metadata)

    val metadataStrValues = () => metadata.names()
                                          .map(key => s"$key -> ${metadata.getValues(key).mkString("[", ", ", "]")}")
                                          .sorted
                                          .mkString("\n")
    logger.debug(s"Read metadata from book:\n${metadataStrValues()}")

    val contentType = metadata.get("Content-Type")
    val titles = metadata.extract(TitleKeys)
    val authors = metadata.extract(AuthorKeys)
    val keywords = metadata.extract(KeywordsKeys)
    val creationDate = metadata.extract(CreationDateKeys)
                               .flatMap(parseDate)
                               .headOption
    val publicationDate = metadata.extract(PublicationDateKeys)
                                  .flatMap(parseDate)
                                  .headOption
    val publisher = metadata.extract(PublisherKeys).headOption
    val identifiers = metadata.extract(IdentifierKeys)
    val language = metadata.extract(LanguageKeys).headOption
    val description = metadata.extract(DescriptionKeys).headOption

    val pageCount = metadata.extract(PageCountKeys).headOption.flatMap(toIntOpt)
    val wordCount = metadata.extract(WordCountKeys).headOption.flatMap(toIntOpt)
    val charCount = metadata.extract(CharacterCountKeys).headOption.flatMap(toIntOpt)

    val stats = if (allEmpty(pageCount, wordCount, charCount)) None
                else Some(BookStats(pageCount, wordCount, charCount))

    BookFormatData(contentType, titles, authors, keywords, creationDate, publicationDate, publisher,
                   identifiers, language, description, stats)
  }.toEither

  private def toIntOpt(str: String): Option[Int] =
    try {
      val intVal = str.toInt
      Some(intVal)
    } catch {
      case _: Exception => None
    }

  private def parseDate(dateStr: String) = {
    parseLocalDate(dateStr)
      .orElse(parseLocalDateTime(dateStr).map(_.toLocalDate))
      .orElse(parseZonedDateTime(dateStr).map(_.toLocalDate))
  }

  private def parseLocalDate(dateStr: String) = {
    Try { LocalDate.parse(dateStr) }
      .toOption
      .orElse {
        DateTimeFormatters.map { formatter =>
          Try { LocalDate.parse(dateStr, formatter) }.toOption
        }.collectFirst { case Some(date) => date }
      }
  }

  private def parseLocalDateTime(dateStr: String) = {
    Try { LocalDateTime.parse(dateStr) }
      .toOption
      .orElse {
        DateTimeFormatters.map { formatter =>
          Try { LocalDateTime.parse(dateStr, formatter) }.toOption
        }.collectFirst { case Some(date) => date }
      }
  }

  private def parseZonedDateTime(dateStr: String) = {
    Try { ZonedDateTime.parse(dateStr) }
      .toOption
      .orElse {
        DateTimeFormatters.map { formatter =>
          Try { ZonedDateTime.parse(dateStr, formatter) }.toOption
        }.collectFirst { case Some(date) => date }
      }
  }

  override def readCover(bookDataInputStream: InputStream): Option[CoverImage] = None
}

object TikaBookFormatDataReader {

  implicit class MetadadataOps(metadata: Metadata) {
    def extract(keyList: Seq[String]): Seq[String] =
      keyList.flatMap(key => metadata.getValues(key))
             .distinct
             .sorted
  }

}