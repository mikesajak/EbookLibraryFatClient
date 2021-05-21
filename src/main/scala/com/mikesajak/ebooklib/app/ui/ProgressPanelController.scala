package com.mikesajak.ebooklib.app.ui

import javafx.concurrent.Worker
import scalafx.application.Platform
import scalafx.scene.control.{Label, ProgressBar}
import scalafxml.core.macros.sfxml

import java.io.File

trait ProgressPanelController {
  def init(service: BackgroundService[Seq[(File, Option[BookDataProvider])]])
}

@sfxml
class ProgressPanelControllerImpl(val progressTitleLabel: Label,
                                  val progressBar: ProgressBar) extends ProgressPanelController {
  override def init(service: BackgroundService[Seq[(File, Option[BookDataProvider])]]): Unit = {
    service.progress.onChange { (src, oldVal, newVal) =>
      println(s"Progress update $src $oldVal $newVal")
      Platform.runLater {
        progressBar.progress = newVal.doubleValue()

        progressTitleLabel.text = s"Reading book ${service.workDone.value.intValue() + 1}/${service.totalWork.value.intValue()}"
      }
    }


    service.state.onChange { (_, _, newState) =>
      println(s"Progress state: $newState")
      if (newState== Worker.State.SUCCEEDED) {
          progressBar.progress = 1

      }
    }
  }
}
