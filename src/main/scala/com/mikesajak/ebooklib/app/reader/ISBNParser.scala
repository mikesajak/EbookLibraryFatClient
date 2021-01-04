package com.mikesajak.ebooklib.app.reader

class ISBNParser {
  // ISBN 0-596-00025-1
  // ISBN 938-0-596-00025-1
  private val ISBNPattern = raw"ISBN\s*:?\s*((\d{3}[- ])?(\d{1,6}[- ]\d{1,6}[- ]\d{1,6}[- ]\d))".r
  private val ISBN10Pattern = raw"ISBN-?10\s*:?\s*(\d{1,6}[- ]\d{1,6}[- ]\d{1,6}[- ]\d)".r
  private val ISBN13Pattern = raw"ISBN-?13\s*:?\s*(\d{3}[- ]\d{1,6}[- ]\d{1,6}[- ]\d{1,6}[- ]\d)".r

  def extractISBNsFrom(text: String): Seq[String] = {
    val isbnIdentifier = ISBNPattern.findFirstMatchIn(text)
                                    .map(m => s"ISBN:${m.group(1)}")

    val isbn10Identifier = ISBN10Pattern.findFirstMatchIn(text)
                                        .map(m => s"ISBN:${m.group(1)}")

    val isbn13Identifier = ISBN13Pattern.findFirstMatchIn(text)
                                        .map(m => s"ISBN:${m.group(1)}")

    isbnIdentifier.toList ++ isbn10Identifier.toList ++ isbn13Identifier.toList
  }
}
