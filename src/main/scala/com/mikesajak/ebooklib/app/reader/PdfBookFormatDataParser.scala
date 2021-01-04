package com.mikesajak.ebooklib.app.reader
import java.io.InputStream

import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.model.CoverImage
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.{PDFTextStripper, PDFTextStripperByArea}

import scala.util.Try

class PdfBookFormatDataParser extends BookFormatDataParser {
  override def acceptContentType(contentType: String): Boolean = contentType == BookFormatResolver.PdfContentType

  override def read(bookDataInputStream: InputStream): Either[Throwable, BookFormatData] = Try {
    val document = PDDocument.load(bookDataInputStream)
    val docInfo = document.getDocumentInformation
    val author = Option(docInfo.getAuthor)
    val titles = (Option(docInfo.getTitle).toList ++ Option(docInfo.getSubject).toList)
        .filter(t => !t.isBlank)
        .distinct

    val keywords = Option(docInfo.getKeywords)
    val identifier = if (!document.isEncrypted) {
      val stripper = new PDFTextStripperByArea()
      stripper.setSortByPosition(true)
      val textStripper = new PDFTextStripper()
      val pdfText = textStripper.getText(document)

      // ISBN 0-596-00025-1
      // ISBN 938-0-596-00025-1
      val ISBNPattern = raw"ISBN\s*:?\s*((\d{3}[- ])?(\d{1,6}[- ]\d{1,6}[- ]\d{1,6}[- ]\d))".r

      ISBNPattern.findFirstMatchIn(pdfText)
                 .map(m => s"ISBN:${m.group(1)}")
    } else None

    BookFormatData(BookFormatResolver.PdfContentType, titles, author.toList, List.empty, None, None,
                   None, identifier.toList, None, None, None)
  }.toEither

  override def readCover(bookDataInputStream: InputStream): Option[CoverImage] = ???

  class EncryptedPdfDocumentNotSupportedException extends Exception
}
