package com.mikesajak.ebooklib.app.reader

import java.io.{ByteArrayInputStream, InputStream}

import com.mikesajak.ebooklib.app.model.CoverImage
import com.typesafe.scalalogging.Logger

class BookFormatDataReaderAggregator extends BookFormatDataReader {
  private val logger = Logger[BookFormatDataReaderAggregator]
  private val readers = List(new TikaBookFormatDataReader())//, new EpubBookFormatDataReader)

  override def read(bookDataInputStream: InputStream): Either[Exception, BookFormatData] = {
    val bookData = bookDataInputStream.readAllBytes()
    readers.flatMap(reader => parse(bookData, reader))
           .reduceLeftOption((book1, book2) => book1.merge(book2))
           .toRight(new NoSuitableParserFound)
  }

  private def parse(bookData: Array[Byte], reader: BookFormatDataReader) = {
    logger.info(s"Parsing book data with reader: ${reader.getClass.getSimpleName}")
    reader.read(new ByteArrayInputStream(bookData)) match {
      case Right(parsedData) => Some(parsedData)
      case Left(exception) =>
        logger.info(s"An error occurred while parsing book with ${reader.getClass.getSimpleName}: ${exception.getLocalizedMessage}", exception)
        None
    }
  }

  override def readCover(bookDataInputStream: InputStream): Option[CoverImage] = None

  class NoSuitableParserFound extends Exception

}
