package com.mikesajak.ebooklib.app.bookformat

class BookFormatResolver {
  def forMimeType(mimeType: String): String = mimeType match {
    case "application/epub+zip" => "EPUB"
    case _ => throw new IllegalArgumentException(s"Unknown mime type $mimeType. Cannot resolve book format.")
  }
}
