package com.mikesajak.ebooklib.app.model

import com.mikesajak.ebooklib.app.dto.{BookDto, BookFormatMetadataDto, SeriesDto}

class BookDtoConverter {

  def bookFromDto(dto: BookDto): Book =
    Book(BookId(dto.id.get), BookMetadata(dto.title, dto.authors, dto.tags, dto.identifiers,
                                          dto.creationDate.toList, dto.publisher, dto.languages,
                                          dto.series.map(s => Series(s.title, s.number)),
                                          dto.description,
                                          dto.formats.map(fmt => bookFormatMetadataFromDto(fmt))))

  def mkDtoFrom(meta: BookMetadata): BookDto =
    BookDto(None, meta.title, meta.authors, meta.tags, meta.identifiers, meta.creationDates.headOption,
            meta.publisher, meta.languages, meta.series.map(s => SeriesDto(s.title, s.number)), meta.description,
            meta.formats.map(fmt => mkBookFormatMetadataDtoFrom(fmt)))

  def bookFormatMetadataFromDto(dto: BookFormatMetadataDto): BookFormatMetadata =
    BookFormatMetadata(BookFormatId(dto.id), BookId(dto.bookId), dto.formatType, None, dto.size)

  def mkBookFormatMetadataDtoFrom(meta: BookFormatMetadata): BookFormatMetadataDto =
    BookFormatMetadataDto(meta.formatId.value, meta.bookId.value, meta.formatType, meta.size)

}
