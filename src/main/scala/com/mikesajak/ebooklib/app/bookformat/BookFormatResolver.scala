package com.mikesajak.ebooklib.app.bookformat

case class BookFormatType(contentType: String, description: String, extensions: Set[String])
object BookFormatType {
  def apply(contentType: String, description: String, extension: String): BookFormatType =
    BookFormatType(contentType, description, Set(extension))
}

object BookFormatResolver {
  val EpubContentType = "application/epub"
  val PlainTextContentType = "text/plain"
  val PdfContentType = "application/pdf"
  val ChmContentType = "application/vnd.ms-htmlhelp"
  val MobiContentType = "application/x-mobipocket-ebook"
}

class BookFormatResolver {
  import BookFormatResolver._

  val supportedFormats = List(BookFormatType(EpubContentType, "Epub ebook", "epub"),
                              BookFormatType(PlainTextContentType, "Plain text file", "txt"),
                              BookFormatType(PdfContentType, "PDF file", "pdf"),
                              BookFormatType(MobiContentType, "MOBI ebook", "mobi"))

  def forMimeType(mimeType: String): BookFormatType = {
    supportedFormats.find(bft => mimeType.startsWith(bft.contentType))
                    .getOrElse(BookFormatType(mimeType, mimeType, mimeType))
  }

  def forExtension(extension: String): Option[BookFormatType] =
    supportedFormats.find(bft => bft.extensions.contains(extension))

}
