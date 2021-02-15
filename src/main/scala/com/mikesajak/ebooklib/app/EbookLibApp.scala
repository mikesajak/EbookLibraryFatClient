package com.mikesajak.ebooklib.app

import com.google.inject.Key
import com.mikesajak.ebooklib.app.config.Config
import com.mikesajak.ebooklib.app.ui.{ResourceManager, UILoader}
import scalafx.Includes._
import scalafx.animation.{KeyFrame, KeyValue, Timeline}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.Scene
import scalafx.util.Duration
import scribe.Logging

object EbookLibApp extends JFXApp with Logging {
  private val mainPanelDef: String = "/layout/main_panel.fxml"

  ScribeCfg.initScribeLogging()

  logger.info(s"EbookLibApp starting")

  private val injector = ApplicationContext.globalInjector.createChildInjector()
  private val config = injector.getInstance(classOf[Config])
  private val appController = injector.getInstance(classOf[AppController])
  private implicit val resourceMgr: ResourceManager = injector.getInstance(Key.get(classOf[ResourceManager]))

  val (root, _) = UILoader.loadScene(mainPanelDef)

  import ResourceManager._

  stage = new PrimaryStage() {
    title = "app.name".textResource
    icons += "icons8-books2-64.png".imgResource
    scene = new Scene(root)
  }

  //  Platform.implicitExit = false
  stage.onCloseRequest = we => {
    we.consume()
    appController.exitApplication { () =>
      new Timeline {
        keyFrames.add(KeyFrame(Duration(600), "fadeOut", null, Set(KeyValue(stage.opacity, 0))))
        onFinished = { () =>
          Platform.exit()
          System.exit(0)
        }
      }.play()
      true
    }
  }

  appController.init(stage, this)

  stage.width = config.settings.window.width
  stage.height = config.settings.window.height

  stage.toFront()
  stage.opacity.value = 0
  stage.show()

  new Timeline {
    keyFrames.add(KeyFrame(Duration(600), "fadeIn", null, Set(KeyValue(stage.opacity, 1))))
  }.play()

  override def main(args: Array[String]): Unit = {
    super.main(args)
  }
}


