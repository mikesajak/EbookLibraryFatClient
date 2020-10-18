package com.mikesajak.ebooklib.app.dto

import com.mikesajak.ebooklib.app.model.{BookFormatId, BookId}

case class BookFormatMetadataDto(id: BookFormatId, bookId: BookId, formatType: String, size: Int)
