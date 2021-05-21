package com.mikesajak.ebooklib.app.reader

import com.google.common.base.Stopwatch
import com.mikesajak.ebooklib.app.model.CoverImage
import com.mikesajak.ebooklib.app.util.OptionOps.allEmpty
import com.mikesajak.ebooklib.app.util.StringExtra._
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler
import scribe.Logging

import java.io.InputStream
import scala.math.{max, min}
import scala.util.Try
import scala.util.matching.Regex

class TikaBookFormatDataParser(isbnParser: ISBNParser, dateParser: DateParser) extends BookFormatDataParser with Logging {

  private val AuthorKeys = List("author", "Author", "dc:author", "meta:author",
                                "creator", "dc:creator", "pdf:docinfo:creator",
                                "Last-Author", "meta:last-author")
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

  override def acceptContentType(contentType: String): Boolean = true // accept all content types

  override def read(bookDataInputStream: InputStream): Either[Throwable, BookFormatData] = Try {
    import TikaBookFormatDataParser._

    val parser = new AutoDetectParser()
    val metadata = new Metadata()
    val bodyHandler = new BodyContentHandler(Int.MaxValue)
    parser.parse(bookDataInputStream, bodyHandler, metadata)

    val bookText = Option(bodyHandler.toString)

    val FieldLimit = 300

    def headerRowElement(text: String, groupName: String, optional: Boolean = false) = {
      val pat = raw"$text\s*:?\s+(?<$groupName>\S.{0,$FieldLimit}\S)"
      if (optional) s"(:?$pat)?" else pat
    }

    val ws = raw"(?:(?![\n\r])\s)"
    val rowSeparator = raw"(:?$ws*\r?\n)+$ws*"

    val titlePattern = raw"($ws*\r?\n$ws*){2,}(?<title>($ws*\S(\S|$ws){0,$FieldLimit}\r?\n)+)"

    val pattern = ("(?s)" +
      List(titlePattern,
           headerRowElement("[Bb]y", "author"),
           raw"[\._\-=\p{Z}\t ]{1,200}",
           headerRowElement("[Pp]ublisher", "publisher"),
           headerRowElement("[Pp]ub [Dd]ate", "pubDate"),
           headerRowElement("[Ii][Ss][Bb][Nn]", "isbn"),
           headerRowElement("[Pp]ages", "pages"))
          .mkString("", rowSeparator, "\r?\n")
        ).r

    val searchPlacePattern = ("(?s)" +
        List(headerRowElement("[Pp]ublisher", "publisher"),
             headerRowElement("[Pp]ub [Dd]ate", "pubDate"),
             headerRowElement("[Ii][Ss][Bb][Nn]", "isbn"),
             headerRowElement("[Pp]ages", "pages"))
            .mkString("", rowSeparator, "\r?\n")
        ).r

    val textToSearch = bookText.flatMap { txt =>
      searchPlacePattern.findFirstMatchIn(txt)
                        .map(mat => txt.substring(max(mat.start - 5000, 0), min(mat.`end` + 500, txt.length)))
    }

    val stopwatch = Stopwatch.createStarted()
    val matcher = textToSearch.flatMap(text => pattern.findFirstMatchIn(text))
    logger.debug(s"Parsing text finished in $stopwatch")

    @inline
    def extract(mat: Regex.Match, groupName: String) = Try { mat.group(groupName) }.toOption


    val isbnIdentifiers = bookText.map(isbnParser.extractISBNsFrom).getOrElse(Seq.empty)

    val metadataStrValues = () => metadata.names()
                                          .map(key => s"$key -> ${metadata.getValues(key).mkString("[", ", ", "]")}")
                                          .sorted
                                          .mkString("\n")
    logger.debug(s"Read metadata from book:\n${metadataStrValues()}")

    val contentType = metadata.get("Content-Type")
    val titles = (metadata.extract(TitleKeys) ++ matcher.flatMap(m => extract(m, "title")).toList)
        .map(_.stripInner)
    val authors = (metadata.extract(AuthorKeys) ++ matcher.flatMap(m => extract(m, "author")).toList)
        .map(_.stripInner)
    val keywords = metadata.extract(KeywordsKeys)
    val creationDates = metadata.extract(CreationDateKeys)
                               .flatMap(dateParser.parseDate)
    val publicationDates = (metadata.extract(PublicationDateKeys)
          ++ matcher.flatMap(m => extract(m, "pubDate")).toList)
        .flatMap(dateParser.parseDate)
    val publisher = (metadata.extract(PublisherKeys) ++ matcher.flatMap(m => extract(m, "publisher")).toList)
        .headOption
        .map(_.stripInner)
    val identifiers = metadata.extract(IdentifierKeys) ++ isbnIdentifiers
    val language = metadata.extract(LanguageKeys).headOption
    val description = metadata.extract(DescriptionKeys).headOption

    val pageCount = (metadata.extract(PageCountKeys) ++ matcher.flatMap(m => extract(m, "pages")).toList)
                            .headOption.flatMap(_.toIntOpt)
    val wordCount = metadata.extract(WordCountKeys).headOption.flatMap(_.toIntOpt)
    val charCount = metadata.extract(CharacterCountKeys).headOption.flatMap(_.toIntOpt)

    val stats = if (allEmpty(pageCount, wordCount, charCount)) None
                else Some(BookStats(pageCount, wordCount, charCount))

    BookFormatData(contentType, titles, authors, keywords,
                   creationDates.headOption, publicationDates.headOption,
                   publisher, identifiers, language, description, stats)
  }.toEither

  override def readCover(bookDataInputStream: InputStream): Option[CoverImage] = None
}

object TikaBookFormatDataParser {

  implicit class MetadadataOps(metadata: Metadata) {
    def extract(keyList: Seq[String]): Seq[String] =
      keyList.flatMap(key => metadata.getValues(key))
             .map(_.strip)
             .distinct
             .sorted
  }

}