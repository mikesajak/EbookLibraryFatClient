package com.mikesajak.ebooklib.app.reader
import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.model.CoverImage
import nl.siegmann.epublib.domain.Date
import nl.siegmann.epublib.epub.EpubReader
import org.apache.tika.Tika

import java.io.InputStream
import java.time.LocalDate
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

class EpubBookFormatDataParser extends BookFormatDataParser {
  override def acceptContentType(contentType: String): Boolean = contentType == BookFormatResolver.EpubContentType
  private val epubReader = new EpubReader()
  private val tika = new Tika()

  override def read(bookDataInputStream: InputStream): Either[Throwable, BookFormatData] = Try {
    val epubData = epubReader.readEpub(bookDataInputStream)

    val metadata = epubData.getMetadata
    val description = metadata.getDescriptions.asScala.foldLeft("")((acc, d) => s"$acc\n\n$d").strip

    def creationOrPublication(d: Date) = d.getEvent match {
      case Date.Event.CREATION | Date.Event.PUBLICATION => true
      case _ => false
    }

    val creationDates = metadata.getDates.asScala
                                .filter(creationOrPublication).map(d => LocalDate.parse(d.getValue))
                                .toSeq

    val identifiers = metadata.getIdentifiers.asScala
                              .map(id => s"${id.getScheme}:${id.getValue}")
                              .map(_.strip)
                              .map(id => id.replaceAll("^:+(.+)", "$1"))
                              .toList

    BookFormatData(
      contentType = BookFormatResolver.EpubContentType,
      titles = metadata.getTitles.asScala.toList,
      authors = metadata.getAuthors.asScala.map(author => s"${author.getFirstname} ${author.getLastname}").toList,
      identifiers = identifiers,
      creationDate = creationDates.headOption,
      publicationDate = creationDates.drop(1).headOption,
      publisher = metadata.getPublishers.asScala.headOption,
      description = if (!description.isBlank) Some(description) else None,
      keywords = Seq(),
      language = Some(metadata.getLanguage),
      stats = None
//      series = None,
//      formats = Seq()
      )
  }.toEither

  override def readCover(bookDataInputStream: InputStream): Option[CoverImage] = Try {
    val epubData = epubReader.readEpub(bookDataInputStream)
    Option(epubData.getCoverImage)
        .flatMap { res => Option(res.getData)
            .flatMap { data => Option(tika.detect(data))
                .map(mimeType => CoverImage("epubCover", mimeType, data))
            }
        }
  }.toOption.flatten
}
