package com.mikesajak.ebooklib.app.reader

import java.io.{ByteArrayInputStream, File}
import java.nio.file.{Files, Paths}

import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.util.Util
import com.typesafe.scalalogging.Logger

class BookFormatDataReader(bookFormatResolver: BookFormatResolver, bookFormatDataParsers: Seq[BookFormatDataParser]) {
  private val logger = Logger[BookFormatDataReader]

  def readFormat(bookFile: File): Option[(BookFormatData, Array[Byte])] = {
    val bookData = Files.readAllBytes(Paths.get(bookFile.getAbsolutePath))
    val (_, extension) = Util.parseFileName(bookFile.getName)
    val bookFormatType = bookFormatResolver.forExtension(extension)

    val suitableReaders = bookFormatDataParsers
      .filter(reader => reader.acceptContentType(bookFormatType.map(_.contentType).orNull))

    logger.debug(
      s"""Parsing book file: ${bookFile.getName}, extension "$extension" mapped to $bookFormatType.
         | Suitable readers: ${suitableReaders.getClass.getSimpleName}""".stripMargin)

    if (suitableReaders.isEmpty)
      logger.info(s"Couldn't find suitable book format reader to parse book data for: ${bookFile.getName}")

    suitableReaders
      .flatMap(reader => parse(bookData, reader))
      .reduceLeftOption((book1, book2) => book1.merge(book2))
      .map(bookFormatData => (bookFormatData, bookData))
  }

  private def parse(bookData: Array[Byte], reader: BookFormatDataParser) = {
    logger.info(s"Parsing book data with reader: ${reader.getClass.getSimpleName}")
    reader.read(new ByteArrayInputStream(bookData)) match {
      case Right(parsedData) => Some(parsedData)
      case Left(exception) =>
        logger.info(s"An error occurred while parsing book with ${reader.getClass.getSimpleName}: ${exception.getLocalizedMessage}", exception)
        None
    }
  }

}