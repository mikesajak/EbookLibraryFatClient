package com.mikesajak.ebooklib.app.bookformat

case class BookFormatType(contentType: String, description: String, extension: String)

class BookFormatResolver {
  val supportedFormats = List(BookFormatType("application/epub+zip", "EPub ebook", "epub"),
                              BookFormatType("text/plain", "Plain text file", "txt"),
                              BookFormatType("application/x-mobipocket-ebook", "MOBI ebook", "mobi"))

  def forMimeType(mimeType: String): BookFormatType = {
    supportedFormats.find(bft => mimeType.startsWith(bft.contentType))
                    .getOrElse(BookFormatType(mimeType, mimeType, mimeType))
//                    .getOrElse(throw new IllegalArgumentException(s"Unknown mime type $mimeType. Cannot resolve book format."))
  }
}
