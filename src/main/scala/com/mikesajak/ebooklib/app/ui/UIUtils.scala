package com.mikesajak.ebooklib.app.ui

import scalafx.Includes._
import scalafx.scene.Parent
import scalafx.scene.control.{Control, Dialog}
import scalafx.stage.{Modality, Stage, StageStyle}

object UIUtils {
  def mkModalDialog[ResultType](ownerStage: Stage, content: Parent): Dialog[ResultType] = new Dialog[ResultType]() {
    initOwner(ownerStage)
    initStyle(StageStyle.Utility)
    initModality(Modality.ApplicationModal)
    dialogPane().content = content
  }

  def bindHeight(control: Control, targetControl: Control): Unit = {
    control.maxHeight.bind(targetControl.height)
    control.prefHeight.bind(targetControl.height)
    control.minHeight.bind(targetControl.height)
  }
}
