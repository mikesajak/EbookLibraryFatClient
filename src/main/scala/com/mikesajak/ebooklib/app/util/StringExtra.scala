package com.mikesajak.ebooklib.app.util

object StringExtra {
  implicit class StringExtra(str: String) {
    def trimInner: String =
      str.replaceAll("\\s+", " ")
         .trim

    def toIntOpt: Option[Int] =
      try {
        val intVal = str.toInt
        Some(intVal)
      } catch {
        case _: Exception => None
      }
  }
}


