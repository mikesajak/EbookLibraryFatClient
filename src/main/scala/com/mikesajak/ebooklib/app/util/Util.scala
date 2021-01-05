package com.mikesajak.ebooklib.app.util

import scala.language.reflectiveCalls

object Util {

  type WithClose = { def close(): Unit }

  def using[A <: WithClose, B](a: A)(code: A => B): B = try {
    code(a)
  } finally {
    a.close()
  }

  private val FileNamePattern = raw"(.+)\.(.+)".r

  def parseFileName(filename: String): (String, String) =
    filename match {
      case FileNamePattern(name, extension) => (name, extension)
      case _ => (filename, "")
    }
}
