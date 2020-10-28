package com.mikesajak.ebooklib.app.util

object OptionOps {
  implicit class OptionEx[T](opt: Option[T]) {
    def selectExisting(otherOpt: Option[T]): Option[T] =
      if (opt.isDefined) opt else otherOpt
  }

  def allDefined[T](opts: Option[T]*): Boolean = opts.forall(_.isDefined)
  def allEmpty[T](opts: Option[T]*): Boolean = opts.forall(_.isEmpty)
}
