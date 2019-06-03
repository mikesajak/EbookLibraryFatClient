package com.mikesajak.ebooklib.app.bookformat

import com.mikesajak.ebooklibrary.bookformat.BookMetadataReader

class BookReadersRegistry {
  private var readersMap: Map[String, BookMetadataReader] = Map()

  def register(reader: BookMetadataReader) =
    if (!readersMap.contains(reader.getMimeType())) {
      readersMap += reader.getMimeType() -> reader
    }

  def allReaders = readersMap.values

}
