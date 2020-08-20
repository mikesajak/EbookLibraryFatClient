package com.mikesajak.ebooklib.app.bookformat

import com.mikesajak.ebooklib.app.reader.BookMetadataReader

class BookReadersRegistry {
  private var readersMap: Map[String, BookMetadataReader] = Map()

  def register(reader: BookMetadataReader): Unit =
    if (!readersMap.contains(reader.mimeType)) {
      readersMap += reader.mimeType -> reader
    }

  def allReaders: Iterable[BookMetadataReader] = readersMap.values

}
