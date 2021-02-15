package com.mikesajak.ebooklib.app.reader

import com.google.common.base.Stopwatch
import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.model.CoverImage
import com.mikesajak.ebooklib.app.util.Util
import scribe.Logging

import java.io.{ByteArrayInputStream, File}
import java.nio.file.{Files, Paths}

class BookFormatDataReader(bookFormatResolver: BookFormatResolver, bookFormatDataParsers: Seq[BookFormatDataParser])
    extends Logging {

  def readFormat(bookFile: File): Option[(BookFormatData, Option[CoverImage], Array[Byte])] = {
    val bookData = Files.readAllBytes(Paths.get(bookFile.getAbsolutePath))
    val (_, extension) = Util.parseFileName(bookFile.getName)
    val bookFormatType = bookFormatResolver.forExtension(extension)

    val suitableParsers = bookFormatDataParsers
      .filter(reader => reader.acceptContentType(bookFormatType.map(_.contentType).orNull))

    logger.debug(
      s"""Parsing book file: ${bookFile.getName}, extension "$extension" mapped to $bookFormatType.
         | Suitable parsers: ${suitableParsers.map(_.getClass.getSimpleName).mkString(", ")}""".stripMargin)

    if (suitableParsers.isEmpty)
      logger.info(s"Couldn't find suitable book format parser to parse book data for: ${bookFile.getName}")

    readBookData(suitableParsers, bookData)
        .map(bookFormatData => (bookFormatData, readBookCover(suitableParsers, bookData), bookData))
  }

  private def readBookData(suitableParsers: Seq[BookFormatDataParser], bookData: Array[Byte]) =
    suitableParsers.flatMap(parser => parse(bookData, parser))
                   .reduceLeftOption((book1, book2) => book1.merge(book2))

  private def readBookCover(suitableParsers: Seq[BookFormatDataParser], bookData: Array[Byte]) =
    suitableParsers.flatMap(parser => readCoverImage(bookData, parser))
                   .reduceLeftOption((cover1, cover2) => cover1)

  private def readCoverImage(bookData: Array[Byte], parser: BookFormatDataParser) = {
    logger.info(s"Reading book cover image with reader: ${parser.getClass.getSimpleName}")
    val stopwatch = Stopwatch.createStarted()
    val result = parser.readCover(new ByteArrayInputStream(bookData))

    if (result.isEmpty) {
      logger.info(s"Could not read book cover with ${parser.getClass.getSimpleName}")
    }
    logger.debug(s"Reading book cover image with ${parser.getClass.getSimpleName} finished in $stopwatch")
    result
  }

  private def parse(bookData: Array[Byte], parser: BookFormatDataParser): Option[BookFormatData] = {
    logger.info(s"Parsing book data with parser: ${parser.getClass.getSimpleName}")
    val stopwatch = Stopwatch.createStarted()
    val result = parser.read(new ByteArrayInputStream(bookData)) match {
      case Right(parsedData) => Some(parsedData)
      case Left(exception) =>
        logger.info(s"An error occurred while parsing book with ${parser.getClass.getSimpleName}: ${exception.getLocalizedMessage}", exception)
        None
    }
    logger.debug(s"Parsing book data with ${parser.getClass.getSimpleName} finished in $stopwatch")
    result
  }

}
