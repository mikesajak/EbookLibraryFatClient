package com.mikesajak.ebooklib.app.dto

case class BookFormatId(value: String) {
  override def toString: String = value
}

case class BookFormatMetadataDto(id: BookFormatId, metadata: BookFormatMetadata)

case class BookFormat(metadata: BookFormatMetadata, contents: Array[Byte])
case class BookFormatMetadata(bookId: BookId, formatType: String, filename: String)

case class BookCover(bookId: BookId, coverImage: CoverImage)
case class CoverImage(name: String, contentType: String, imageData: Array[Byte])