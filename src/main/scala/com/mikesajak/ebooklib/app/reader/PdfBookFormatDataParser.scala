package com.mikesajak.ebooklib.app.reader
import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.model.CoverImage
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.{PDFTextStripper, PDFTextStripperByArea}

import java.io.{ByteArrayOutputStream, InputStream}
import java.time.{LocalDate, ZoneId}
import javax.imageio.ImageIO
import scala.util.Try

class PdfBookFormatDataParser(isbnParser: ISBNParser) extends BookFormatDataParser {
  override def acceptContentType(contentType: String): Boolean = contentType == BookFormatResolver.PdfContentType

  override def read(bookDataInputStream: InputStream): Either[Throwable, BookFormatData] = Try {
    val pdfDocument = PDDocument.load(bookDataInputStream)
    val docInfo = pdfDocument.getDocumentInformation
    val author = Option(docInfo.getAuthor).map(_.strip)
    val titles = (Option(docInfo.getTitle).toList ++ Option(docInfo.getSubject).toList)
        .filter(t => !t.isBlank)
        .map(_.strip)
        .distinct
    val creationDate = Option(docInfo.getCreationDate.toInstant)
        .map(instant => LocalDate.ofInstant(instant, ZoneId.systemDefault()))

    val keywords = Option(docInfo.getKeywords).map(_.split(raw"\s*,\s*").toList)
                                              .getOrElse(List.empty)
                                              .map(_.strip)
    val identifiers = if (!pdfDocument.isEncrypted) {
      val stripper = new PDFTextStripperByArea()
      stripper.setSortByPosition(true)
      val textStripper = new PDFTextStripper()
      val pdfText = textStripper.getText(pdfDocument)

      isbnParser.extractISBNsFrom(pdfText)
    } else List.empty

    BookFormatData(BookFormatResolver.PdfContentType, titles, author.toList, keywords, creationDate.toList,
                   None, identifiers, None, None, None)
  }.toEither

  override def readCover(bookDataInputStream: InputStream): Option[CoverImage] = Try {
    val pdfDocument = PDDocument.load(bookDataInputStream)
    val pdfRenderer = new PDFRenderer(pdfDocument)
    val bufferedImage = pdfRenderer.renderImage(0)
    val byteArrayOutputStream = new ByteArrayOutputStream()
    if (ImageIO.write(bufferedImage, "jpeg", byteArrayOutputStream)) {
      val dataArray = byteArrayOutputStream.toByteArray
      Some(CoverImage("pdfCover", "application/jpeg", dataArray))
    } else None
  }.toOption.flatten

  class EncryptedPdfDocumentNotSupportedException extends Exception
}
