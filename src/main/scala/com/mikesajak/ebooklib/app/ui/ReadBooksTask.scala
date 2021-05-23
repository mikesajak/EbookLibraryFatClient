package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.reader.BookFormatDataReader
import com.mikesajak.ebooklib.app.ui.util.CancelledException
import javafx.{concurrent => jfxc}
import scribe.Logging

import java.io.File

class ReadBooksTask(filesToOpen: Seq[File], bookFormatDataReader: BookFormatDataReader,
                    bookDataProviderFactory: BookDataProviderFactory)
    extends jfxc.Task[Seq[(File, Option[BookDataProvider])]] with Logging {

  private def checkCancelled(): Unit = {
    if (isCancelled) {
      logger.info("Task was cancelled, aborting current work by CancelledException")
      throw CancelledException("XX")
    }
  }

  private def doProgressUpdate(curFileNum: Int): Unit = {
    if (filesToOpen.size == 1) {
      updateProgress(-1, -1)
      updateMessage(s"Reading book") // TODO: i18
    }
    else {
      updateProgress(curFileNum, filesToOpen.size)
      updateMessage(s"Reading book $curFileNum/${filesToOpen.size}") // TODO: i18
    }
  }

  override def call(): Seq[(File, Option[BookDataProvider])] = {
    val result =
      filesToOpen.zipWithIndex
                 .map { case (bookFile, idx) =>
                   checkCancelled()
                   doProgressUpdate(idx + 1)
                   val bookDataProviderOpt = bookFormatDataReader.readFormat(bookFile)
                                                                 .map { case (bookFormatData, bookCover, bookData) =>
                                                                   bookDataProviderFactory.createBookDataProvider(bookFile, bookData, bookFormatData, bookCover)
                                                                 }
                   (bookFile, bookDataProviderOpt)
                 }
    updateProgress(filesToOpen.size, filesToOpen.size)
    result
  }
}
