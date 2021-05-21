package com.mikesajak.ebooklib.app.ui.util

case class CancelledException[A](value: A) extends Exception {
  def this() = this(null.asInstanceOf[A])
}