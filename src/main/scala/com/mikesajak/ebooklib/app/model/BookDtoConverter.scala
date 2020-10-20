package com.mikesajak.ebooklib.app.model

import com.mikesajak.ebooklib.app.dto.{BookDto, BookFormatMetadataDto, SeriesDto}

class BookDtoConverter {

  def bookFromDto(dto: BookDto): Book =
    Book(BookId(dto.id.get), BookMetadata(dto.title, dto.authors, dto.tags, dto.identifiers,
                                          dto.creationDate, dto.publicationDate, dto.publisher,
                                          dto.languages, dto.series.map(s => Series(s.title, s.number)),
                                          dto.description,
                                          dto.formats.map(fmt => BookFormatMetadata(fmt.id, fmt.bookId, fmt.formatType, None, fmt.size))))

  def mkDtoFrom(meta: BookMetadata): BookDto =
    BookDto(None, meta.title, meta.authors, meta.tags, meta.identifiers, meta.creationDate, meta.publicationDate,
            meta.publisher, meta.languages, meta.series.map(s => SeriesDto(s.title, s.number)), meta.description,
            meta.formats.map(fmt => BookFormatMetadataDto(fmt.formatId, fmt.bookId, fmt.formatType, fmt.size)))

}
