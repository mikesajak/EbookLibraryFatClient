package com.mikesajak.ebooklib.app.ui.util

import javafx.stage
import scalafx.scene.control.Dialog

object MyScalaFxImplicits {
  //  implicit def jfxButton2SfxButton(button: jfxctrl.Button): Button = new Button(button)

  implicit class RichDialog[A](val self: Dialog[A]) {
    def setWindowSize(width: Int, height: Int): Unit = {
      val window = self.getDialogPane.getScene.getWindow.asInstanceOf[stage.Stage]
      window.setWidth(width)
      window.setHeight(height)
    }

    def setWindowMinSize(width: Int, height: Int): Unit = {
      val window = self.getDialogPane.getScene.getWindow.asInstanceOf[stage.Stage]
      window.setMinWidth(width)
      window.setHeight(height)
    }
  }
}
