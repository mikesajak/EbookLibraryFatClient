package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.model._
import com.mikesajak.ebooklib.app.reader.BookFormatData
import scalafx.scene.image.Image

import java.io.{ByteArrayInputStream, File}

class BookDataProviderFactory {
  def createBookDataProvider(bookFile: File, bookData: Array[Byte],
                             bookFormatData: BookFormatData, bookCoverData: Option[CoverImage]): BookDataProvider = {
    new BookDataProvider {
      override def bookId: Option[BookId] = None

      override def bookMetadata: BookMetadata =
        BookMetadata.from(bookFormatData, bookFormatsMetadata)

      override def bookCover: Option[Image] =
        bookCoverData.map(coverImage => new Image(new ByteArrayInputStream(coverImage.imageData)))

      override def bookFormatsMetadata: Seq[BookFormatMetadata] = Seq(formatMetadata)

      private def formatMetadata =
        BookFormatMetadata(BookFormatId.NotExisting, BookId.NotExisting, bookFormatData.contentType,
                           Some(bookFile.getAbsolutePath), bookFile.length().toInt)

      override def bookFormat(formatId: BookFormatId): BookFormat = BookFormat(formatMetadata, bookData)
    }
  }
}
