package com.mikesajak.ebooklib.app.reader

class ISBNParser {
  // ISBN 0-596-00025-1
  // ISBN 938-0-596-00025-1
  // ISBN: ePub: 978-83-246-9123-3, Mobi: 978-83-246-9321-0

  private val ISBNPattern = raw"(ISBN|ISBN-?10|ISBN-?13|ePub|Mobi)\s*:?\s*((\d{3}[- ])?(\d{1,6}[- ]\d{1,6}[- ]\d{1,6}[- ]\d))".r

  def extractISBNsFrom(text: String): Seq[String] = {
    ISBNPattern.findAllMatchIn(text)
               .map(m => s"ISBN:${m.group(2)}")
               .toSeq
  }
}
