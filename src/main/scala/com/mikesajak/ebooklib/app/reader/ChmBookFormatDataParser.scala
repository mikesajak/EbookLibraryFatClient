package com.mikesajak.ebooklib.app.reader
import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.model.CoverImage
import org.jchmlib.ChmFile

import java.io.{File, FileOutputStream, InputStream}
import scala.util.Try

class ChmBookFormatDataParser extends BookFormatDataParser {
  override def acceptContentType(contentType: String): Boolean = contentType == BookFormatResolver.ChmContentType

  override def read(bookDataInputStream: InputStream): Either[Throwable, BookFormatData] = {
    val tmpFile = saveTmpFile(bookDataInputStream)
    try {
      Try {
        val chmFile = new ChmFile(tmpFile.getAbsolutePath)

        BookFormatData(contentType = BookFormatResolver.ChmContentType, titles = List(chmFile.getTitle),
                       authors = List.empty, keywords = List.empty, creationDate = None, publicationDate = None,
                       publisher = None, identifiers = List.empty, language = None, description = None, stats = None)
      }.toEither
    } finally {
      tmpFile.delete()
    }
  }

  private def saveTmpFile(inStr: InputStream) = {
    val tmpFile = File.createTempFile("chm", ".chm")
    val outStream = new FileOutputStream(tmpFile)
    outStream.write(inStr.readAllBytes())
    outStream.close()
    tmpFile
  }

  override def readCover(bookDataInputStream: InputStream): Option[CoverImage] = None
}
