package com.mikesajak.ebooklib.app

import com.mikesajak.ebooklib.app.config.{Config, WindowSettings}
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.stage.Stage

class AppController(config: Config) {

  private var mainStage0: Stage =_
  private var application0: JFXApp = _

  // TODO: probably not the best way to do it...
  def init(stage: Stage, app: JFXApp): Unit = {
    require(mainStage0 == null && application0 == null, "UI window/stage is already initialized")
    mainStage0 = stage
    application0 = app
  }

  def mainStage: Stage = {
    require(mainStage0 != null, "UI window/stage is not initialized yet")
    mainStage0
  }

  def application: JFXApp = {
    require(application0 != null, "Application has not been defined")
    application0
  }

  def exitApplication(): Unit = exitApplication(() => false)
  def exitApplication(exitAction: () => Boolean): Unit = {
    if (canExit) {
      // TODO: save config, close connections, etc.
      config.settings.window = WindowSettings(mainStage.width.toInt, mainStage.height.toInt)
      config.save()

      if (!exitAction()) {
        Platform.exit()
        System.exit(0)
      }
    }
  }

  private def canExit: Boolean = {
    if (config.settings.confirmExit) askUserForExit()
    else true
  }

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

object AppController {
  val configPath = s"${System.getProperty("user.dir")}" // fixme: for debug purposes, change this to dir in user home, e.g. .afternooncommander/ or something
  val configFile = "afternooncommander.conf"
}