package com.mikesajak.ebooklib.app

import com.mikesajak.ebooklib.app.util.Check
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.stage.Stage

class ApplicationController {

  private var mainStage0: Stage =_
  private var application0: JFXApp = _

  // TODO: probably not the best way to do it...
  def init(stage: Stage, app: JFXApp): Unit = {
    Check.state(mainStage0 == null && application0 == null, "UI window/stage is already initialized")
    mainStage0 = stage
    application0 = app
  }

  def mainStage: Stage = {
    Check.state(mainStage0 != null, "UI window/stage is not initialized yet")
    mainStage0
  }

  def application: JFXApp = {
    Check.state(application0 != null, "Application has not been defined")
    application0
  }

  def exitApplication(): Unit = exitApplication(() => false)
  def exitApplication(exitAction: () => Boolean): Unit = {
    if (canExit) {
      if (!exitAction())
        Platform.exit()
    }
  }

  private def canExit: Boolean = askUserForExit()

  private def askUserForExit(): Boolean = {
    val alert = new Alert(AlertType.Confirmation) {
      //        initOwner(stage)
      title = "Confirm application exit"
      headerText = "You're about to quit application."
      contentText = "Are you sure?"
      buttonTypes = Seq(ButtonType.No, ButtonType.Yes)
    }

    alert.showAndWait() match {
      case Some(ButtonType.Yes) => true
      case _ => false
    }
  }

}

object ApplicationController {
  val configPath = s"${System.getProperty("user.dir")}" // fixme: for debug purposes, change this to dir in user home, e.g. .afternooncommander/ or something
}
