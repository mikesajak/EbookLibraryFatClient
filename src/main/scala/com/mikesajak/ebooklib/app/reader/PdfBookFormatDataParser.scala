package com.mikesajak.ebooklib.app.reader
import java.io.InputStream
import java.time.{LocalDate, ZoneId}

import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.model.CoverImage
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.{PDFTextStripper, PDFTextStripperByArea}

import scala.util.Try

class PdfBookFormatDataParser(isbnParser: ISBNParser) extends BookFormatDataParser {
  override def acceptContentType(contentType: String): Boolean = contentType == BookFormatResolver.PdfContentType

  override def read(bookDataInputStream: InputStream): Either[Throwable, BookFormatData] = Try {
    val document = PDDocument.load(bookDataInputStream)
    val docInfo = document.getDocumentInformation
    val author = Option(docInfo.getAuthor).map(_.trim)
    val titles = (Option(docInfo.getTitle).toList ++ Option(docInfo.getSubject).toList)
        .filter(t => !t.isBlank)
        .map(_.trim)
        .distinct
    val creationDate = Option(docInfo.getCreationDate.toInstant)
        .map(instant => LocalDate.ofInstant(instant, ZoneId.systemDefault()))

    val keywords = Option(docInfo.getKeywords).map(_.split(raw"\s*,\s*").toList)
                                              .getOrElse(List.empty)
                                              .map(_.trim)
    val identifiers = if (!document.isEncrypted) {
      val stripper = new PDFTextStripperByArea()
      stripper.setSortByPosition(true)
      val textStripper = new PDFTextStripper()
      val pdfText = textStripper.getText(document)

      isbnParser.extractISBNsFrom(pdfText)
    } else List.empty

    BookFormatData(BookFormatResolver.PdfContentType, titles, author.toList, keywords, creationDate.toList,
                   None, identifiers, None, None, None)
  }.toEither

  override def readCover(bookDataInputStream: InputStream): Option[CoverImage] = ???

  class EncryptedPdfDocumentNotSupportedException extends Exception
}
