package com.mikesajak.ebooklib.app.ui

import java.util.{Locale, ResourceBundle}

import com.ibm.icu.text.MessageFormat
import com.typesafe.scalalogging.Logger
import scalafx.scene.image.Image

class ResourceManager {

  def getMessage(key: String, resourceFile: String = "ui", locale: Locale = Locale.getDefault()): String =
    ResourceBundle.getBundle(resourceFile).getString(key)

  def getMessageOpt(key: String, resourceFile: String = "ui", locale: Locale = Locale.getDefault()): Option[String] =
    if (ResourceBundle.getBundle(resourceFile).containsKey(key))
      Some(getMessage(key, resourceFile, locale))
    else None


  def getMessageWithArgs(key: String, args: Seq[Any],
                         resourceFile: String = "ui", locale: Locale = Locale.getDefault): String = {
    val pattern = getMessage(key, resourceFile)
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