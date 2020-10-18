package com.mikesajak.ebooklib.app.model

case class BookFormatId(value: String) {
  override def toString: String = value
}

object BookFormatId {
  val NotExisting: BookFormatId = BookFormatId("Not existing")
}

case class BookFormat(metadata: BookFormatMetadata, contents: Array[Byte])
case class BookFormatMetadata(formatId: BookFormatId, bookId: BookId, formatType: String, filename: Option[String], size: Int)

case class BookCover(bookId: BookId, coverImage: CoverImage)
case class CoverImage(name: String, contentType: String, imageData: Array[Byte])