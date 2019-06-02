package com.mikesajak.ebooklib.app.bookformat

import com.mikesajak.ebooklibrary.bookformat.{BookFormatType, BookMetadataReader}

class BookReadersRegistry {
  private var readersMap: Map[BookFormatType, BookMetadataReader] = Map()

  def register(reader: BookMetadataReader) =
    if (!readersMap.contains(reader.getBookFormatType())) {
      readersMap += reader.getBookFormatType() -> reader
    }

  def allReaders = readersMap.values

}
