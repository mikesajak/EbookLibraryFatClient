package com.mikesajak.ebooklib.app.reader

import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.model.CoverImage
import org.apache.tika.Tika
import org.rr.mobi4java.exth.StringRecordDelegate
import org.rr.mobi4java.{ByteUtils, EXTHRecord, MobiMetaData, MobiReader}
import scribe.Logging

import java.io.InputStream
import scala.jdk.CollectionConverters._
import scala.util.Try

class MobiBookFormatDataParser(dateParser: DateParser) extends BookFormatDataParser with Logging {
  private val mobiReader = new MobiReader()
  private val tika = new Tika()

  override def acceptContentType(contentType: String): Boolean = contentType == BookFormatResolver.MobiContentType

  override def read(bookDataInputStream: InputStream): Either[Throwable, BookFormatData] = Try {
    val mobiDocument = mobiReader.read(bookDataInputStream)
    val docTitle = Option(mobiDocument.getFullName)

    val metadata = mobiDocument.getMetaData

    val title = extractRecordAsString(metadata, EXTHRecord.RECORD_TYPE.UPDATED_TITLE)
    val title2 = extractRecordAsString(metadata, EXTHRecord.RECORD_TYPE.SECONDARY_TITLE)
    val titles = (docTitle.toList ++ title.toList ++ title2.toList).distinct

    val authors = extractStringSeq(metadata.getAuthorRecords)
    val descriptions = extractStringSeq(metadata.getDescriptionRecords)
    val description = if (descriptions.isEmpty) None
                      else Some(descriptions.mkString("\n\n"))
    val identifiers = metadata.getISBNRecords.iterator.asScala
                              .map(_.getIsbn)
                              .toSeq
    val language = Option(metadata.getLanguageRecord).map(_.getLanguageCode)
    val publishersSeq = extractStringSeq(metadata.getPublisherRecords)
    val publisher = if (publishersSeq.isEmpty) None
                     else Some(publishersSeq.mkString(", "))

    val publicationDates = metadata.getPublishingDateRecords.asScala.toSeq
                                   .map(_.getAsString("UTF-8"))
                                   .flatMap(dateStr => dateParser.parseDate(dateStr))
    val keywords = extractStringSeq(metadata.getSubjectRecords)

    logger.debug(s"""MOBI records:
                 |${metadata.getEXTHRecords.iterator.asScala
                            .map(r => s"${r.getRecordType} -> ${ByteUtils.getString(r.getData, "UTF8")}")
                            .mkString("\n")}""".stripMargin)

    BookFormatData(
      contentType = BookFormatResolver.MobiContentType,
      titles = titles,
      authors = authors,
      identifiers = identifiers,
      creationDate = publicationDates.drop(1).headOption,
      publicationDate = publicationDates.headOption,
      publisher = publisher,
      description = description,
      keywords = keywords,
      language = language,
      stats = None
      //      series = None,
      //      formats = Seq()
      )
  }.toEither

  private def extractStringSeq(recordList: java.util.List[StringRecordDelegate]) =
    recordList.iterator.asScala
              .map(_.getAsString("UTF-8"))
              .toSeq

  private def extractRecordAsString(mobiMeta: MobiMetaData, recordType: EXTHRecord.RECORD_TYPE) =
    mobiMeta.getEXTHRecords.iterator.asScala
            .filter(r => r.getRecordType == recordType)
            .nextOption()
            .map(_.getData)
            .map(data => ByteUtils.getString(data, "UTF-8"))


  override def readCover(bookDataInputStream: InputStream): Option[CoverImage] = Try {
    val mobiDocument = mobiReader.read(bookDataInputStream)
    Option(mobiDocument.getCover)
        .flatMap { imageData => Option(tika.detect(imageData))
            .map(mimeType => CoverImage("mobiCover", mimeType, imageData))
        }
  }.toOption.flatten
}

