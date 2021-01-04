package com.mikesajak.ebooklib.app.reader
import java.io.InputStream
import java.time.LocalDate

import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.model.CoverImage
import nl.siegmann.epublib.domain.Date
import nl.siegmann.epublib.epub.EpubReader

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

class EpubBookFormatDataParser extends BookFormatDataParser {
  override def acceptContentType(contentType: String): Boolean = contentType == BookFormatResolver.EpubContentType

  override def read(bookDataInputStream: InputStream): Either[Throwable, BookFormatData] = Try {
    val reader = new EpubReader()
    val epub = reader.readEpub(bookDataInputStream)

    val metadata = epub.getMetadata
    val description = metadata.getDescriptions.asScala.foldLeft("")((acc, d) => s"$acc\n\n$d").trim

    def creationOrPublication(d: Date) = d.getEvent match {
      case Date.Event.CREATION | Date.Event.PUBLICATION => true
      case _ => false
    }

    val creationDates = metadata.getDates.asScala
                                .filter(creationOrPublication).map(d => LocalDate.parse(d.getValue))
                                .toSeq

    BookFormatData(
      contentType = BookFormatResolver.EpubContentType,
      titles = metadata.getTitles.asScala.toList,
      authors = metadata.getAuthors.asScala.map(author => s"${author.getFirstname} ${author.getLastname}").toList,
      identifiers = metadata.getIdentifiers.asScala.map(id => s"${id.getScheme}:${id.getValue}").toList,
      creationDates = creationDates,
      publisher = metadata.getPublishers.asScala.headOption,
      description = if (!description.isBlank) Some(description) else None,
      keywords = Seq(),
      language = Some(metadata.getLanguage),
      stats = None
//      series = None,
//      formats = Seq()
      )
  }.toEither

  override def readCover(bookDataInputStream: InputStream): Option[CoverImage] = None
}
