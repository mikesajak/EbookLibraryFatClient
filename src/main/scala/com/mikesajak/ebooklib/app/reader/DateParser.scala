package com.mikesajak.ebooklib.app.reader

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.Locale
import scala.util.Try

class DateParser {

  private val DateTimeFormatters = List(DateTimeFormatter.BASIC_ISO_DATE, DateTimeFormatter.ISO_DATE,
                                        DateTimeFormatter.ISO_DATE_TIME, DateTimeFormatter.ISO_LOCAL_DATE,
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME, DateTimeFormatter.ISO_OFFSET_DATE,
                                        DateTimeFormatter.ISO_OFFSET_DATE_TIME, DateTimeFormatter.ISO_ORDINAL_DATE,
                                        DateTimeFormatter.ISO_ZONED_DATE_TIME, DateTimeFormatter.RFC_1123_DATE_TIME,
                                        DateTimeFormatter.ISO_INSTANT)

  private val CustomTimeFormatters = List(new SimpleDateFormat("MMMMM yyyy", Locale.US),
                                          new SimpleDateFormat("yyyy-MM-dd HH:mm:ssXXX"),
                                          new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                                          new SimpleDateFormat("yyyy-MM-dd"),
                                          new SimpleDateFormat("yyyy-MM"),
                                          new SimpleDateFormat("yyyy"))

  def parseDate(dateStr: String): Option[LocalDate] = {
    parseLocalDate(dateStr)
        .orElse(parseLocalDateTime(dateStr).map(_.toLocalDate))
        .orElse(parseZonedDateTime(dateStr).map(_.toLocalDate))
        .orElse(parseCustomDateTime(dateStr).map(_.toInstant.atZone(ZoneId.systemDefault()).toLocalDate))
  }

  private def parseLocalDate(dateStr: String) = {
    Try { LocalDate.parse(dateStr) }
        .toOption
        .orElse {
          DateTimeFormatters.map { formatter =>
            Try { LocalDate.parse(dateStr, formatter) }.toOption
          }.collectFirst { case Some(date) => date }
        }
  }

  private def parseLocalDateTime(dateStr: String) = {
    Try { LocalDateTime.parse(dateStr) }
        .toOption
        .orElse {
          DateTimeFormatters.map { formatter =>
            Try { LocalDateTime.parse(dateStr, formatter) }.toOption
          }.collectFirst { case Some(date) => date }
        }
  }

  private def parseZonedDateTime(dateStr: String) = {
    Try { ZonedDateTime.parse(dateStr) }
        .toOption
        .orElse {
          DateTimeFormatters.map { formatter =>
            Try { ZonedDateTime.parse(dateStr, formatter) }.toOption
          }.collectFirst { case Some(date) => date }
        }
  }

  private def parseCustomDateTime(dateStr: String) = {
    CustomTimeFormatters.map { formatter =>
      Try { formatter.parse(dateStr) }.toOption
    }.collectFirst { case Some(date) => date }
  }
}