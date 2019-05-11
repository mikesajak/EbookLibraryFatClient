package com.mikesajak.ebooklib.app.ui

import java.util.{Locale, ResourceBundle}

import com.ibm.icu.text.MessageFormat
import com.mikesajak.ebooklib.app.ui.ResourceManager.{ImageResource, MessageResource}
import com.typesafe.scalalogging.Logger
import scalafx.scene.image.Image

import scala.language.implicitConversions

class ResourceManager {

  def getMessage(resource: MessageResource): String =
    if (resource.parameters.isEmpty) getMessage(resource.key)
    else getMessageWithArgs(resource.key, resource.parameters)

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

  def getImage(resource: ImageResource): Image = getImageImpl(resource.key)

  private def getImageImpl(name: String): Image = {
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
  case class MessageResource(key: String, parameters: Any*)
  case class ImageResource(key: String) extends AnyVal

  implicit class ResourceUtil(val value: String) extends AnyVal {
    def image = ImageResource(value)
    def message = MessageResource(value)
  }
}

