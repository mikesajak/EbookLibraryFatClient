package com.mikesajak.ebooklib.app.dto

case class ServerInfo(name: String, version: String) {
  override def toString = s"$name:$version"
}
