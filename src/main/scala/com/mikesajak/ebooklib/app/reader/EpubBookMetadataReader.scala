package com.mikesajak.ebooklib.app.reader
import java.io.{IOException, InputStream}
import java.time.LocalDate

import com.mikesajak.ebooklib.app.dto.{BookMetadata, CoverImage}
import nl.siegmann.epublib.domain.Date
import nl.siegmann.epublib.epub.EpubReader

import scala.collection.JavaConverters._

class EpubBookMetadataReader extends BookMetadataReader() {
  override val mimeType: String = "application/epub+zip"

  override def canRead(bookData: InputStream): Boolean = {
    try {
      val reader = new EpubReader()
      reader.readEpub(bookData)
      true
    } catch {
      case _: IOException => false
    }
  }

  override def read(bookData: InputStream): BookMetadata = {
    val reader = new EpubReader()
    val epub = reader.readEpub(bookData)

    val metadata = epub.getMetadata
    val description = metadata.getDescriptions.asScala.foldLeft("")((acc, d) => s"$acc\n\n$d").trim

    BookMetadata(
      title = metadata.getFirstTitle,
      authors = metadata.getAuthors.asScala.map(author => s"${author.getFirstname} ${author.getLastname}"),
      identifiers = metadata.getIdentifiers.asScala.map(id => s"${id.getScheme}:${id.getValue}"),
      creationDate = metadata.getDates.asScala.find(d => d.getEvent == Date.Event.CREATION).map(d => LocalDate.parse(d.getValue)),
      publicationDate = metadata.getDates.asScala.find(d => d.getEvent == Date.Event.PUBLICATION).map(d => LocalDate.parse(d.getValue)),
      publisher = metadata.getPublishers.asScala.headOption,
      description = if (!description.isBlank) Some(description) else None,
      tags = Seq(),
      languages = Seq(metadata.getLanguage),
      series = None)
  }

  override def readCover(bookData: InputStream): Option[CoverImage] = None
}
