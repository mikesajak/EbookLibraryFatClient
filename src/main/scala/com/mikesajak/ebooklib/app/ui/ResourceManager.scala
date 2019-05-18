package com.mikesajak.ebooklib.app.ui

import java.util.{Locale, ResourceBundle}

import com.ibm.icu.text.MessageFormat
import com.typesafe.scalalogging.Logger
import scalafx.scene.image.Image

import scala.language.implicitConversions

class ResourceManager(val resourceFile: String = "ui", locale: Locale = Locale.getDefault()) {

  def getMessage(key: String): String =
    ResourceBundle.getBundle(resourceFile).getString(key)

  def getMessage(key: String, args: Any*): String = {
    val pattern = getMessage(key)
    val formatter = new MessageFormat("")
    formatter.setLocale(locale)
    formatter.applyPattern(pattern)
    formatter.format(args.toArray)
  }

  def getImage(name: String): Image = {
    val imagePath = s"/images/$name"
    try {
      new Image(imagePath)
    } catch {
      case e: Exception =>
        Logger[ResourceManager].warn(s"Exception thrown during getting icon $imagePath", e)
        throw e
    }
  }
}

object ResourceManager {
  implicit class ResourceUtil(val value: String) extends AnyVal {
    def imgResource(implicit resMgr: ResourceManager): Image = resMgr.getImage(value)
    def textResource(implicit resMgr: ResourceManager): String = resMgr.getMessage(value)
  }
}

