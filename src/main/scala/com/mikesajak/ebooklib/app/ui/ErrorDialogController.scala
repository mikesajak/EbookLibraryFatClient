package com.mikesajak.ebooklib.app.ui

import scalafx.Includes._
import scalafx.scene.control.{ButtonType, Dialog, Label}
import scalafx.scene.text.{Text, TextFlow}
import scalafxml.core.macros.sfxml

trait ErrorDialogController {
  def init(dialog: Dialog[ButtonType], title: String, contentMessage: String): Unit
}

@sfxml
class ErrorDialogControllerImpl(titleMsgLabel: Label,
                                contentMsgTextFlow: TextFlow) extends ErrorDialogController {
  def init(dialog: Dialog[ButtonType], title: String, contentMessage: String): Unit = {
    val dialogPane = dialog.dialogPane.value
    dialogPane.buttonTypes = Seq(ButtonType.OK)

    titleMsgLabel.text = title
    contentMsgTextFlow.children.addAll(new Text(contentMessage))
  }
}
