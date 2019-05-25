package com.mikesajak.ebooklib.app.ui

import scalafx.Includes._
import scalafx.scene.Parent
import scalafx.scene.control.Dialog
import scalafx.stage.{Modality, Stage, StageStyle}

object UIUtils {
  def mkModalDialog[ResultType](owner: Stage, content: Parent): Dialog[ResultType] = new Dialog[ResultType]() {
    initOwner(owner)
    initStyle(StageStyle.Utility)
    initModality(Modality.ApplicationModal)
    dialogPane().content = content
  }
}
