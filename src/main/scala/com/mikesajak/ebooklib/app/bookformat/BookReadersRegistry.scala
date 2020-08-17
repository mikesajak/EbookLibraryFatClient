package com.mikesajak.ebooklib.app.bookformat

import com.mikesajak.ebooklib.app.reader.BookMetadataReader2

class BookReadersRegistry {
  private var readersMap: Map[String, BookMetadataReader2] = Map()

  def register(reader: BookMetadataReader2): Unit =
    if (!readersMap.contains(reader.mimeType)) {
      readersMap += reader.mimeType -> reader
    }

  def allReaders: Iterable[BookMetadataReader2] = readersMap.values

}
