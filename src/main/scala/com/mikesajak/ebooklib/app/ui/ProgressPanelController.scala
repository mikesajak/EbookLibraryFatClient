package com.mikesajak.ebooklib.app.ui

import com.mikesajak.ebooklib.app.reader.BookFormatDataReader
import com.mikesajak.ebooklib.app.ui.util.CancelledException
import javafx.concurrent.Worker.State
import javafx.{concurrent => jfxc}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control.{Dialog, Label, ProgressBar, ProgressIndicator}
import scalafxml.core.macros.sfxml
import scribe.Logging

import java.io.File

trait ProgressPanelController {
  def init(filesToOpen: Seq[File], dialog: Dialog[Seq[(File, Option[BookDataProvider])]]): BackgroundService[Seq[(File, Option[BookDataProvider])]]
}

@sfxml
class ProgressPanelControllerImpl(val progressTitleLabel: Label,
                                  val progressBar: ProgressBar,

                                  bookFormatDataReader: BookFormatDataReader,
                                  bookDataProviderFactory: BookDataProviderFactory) extends ProgressPanelController with Logging {
  override def init(filesToOpen: Seq[File], dialog: Dialog[Seq[(File, Option[BookDataProvider])]]): BackgroundService[Seq[(File, Option[BookDataProvider])]] = {

    val task = new jfxc.Task[Seq[(File, Option[BookDataProvider])]]() with Logging {
      private def checkCancelled(): Unit = {
        if (isCancelled) {
          logger.info("Task was cancelled, aborting current work by CancelledException")
          throw CancelledException("XX")
        }
      }

      override def call(): Seq[(File, Option[BookDataProvider])] = {
        val result =
          filesToOpen.zipWithIndex
                     .map { case (bookFile, idx) =>
                       checkCancelled()
                       updateProgress(idx, filesToOpen.size)
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
    val service = new BackgroundService(task)

    progressBar.progress = if (filesToOpen.size == 1) ProgressIndicator.IndeterminateProgress
                           else 0

    service.progress.onChange { (_, _, newVal) =>
      logger.debug(s"Setting progress to $newVal")
      Platform.runLater {
        progressBar.progress = newVal.doubleValue()
        progressTitleLabel.text = s"Reading book ${service.workDone.value.toInt + 1}/${service.totalWork.value.toInt}"
      }
    }

    service.state.onChange { (_, _, state) => state match {
      case State.FAILED | State.CANCELLED =>
        dialog.close()
      case State.SUCCEEDED =>
        progressBar.progress = 1
        dialog.result = service.value.value
      case _ =>
    }}

    dialog.onShown = _ => service.start()

    service
  }
}
