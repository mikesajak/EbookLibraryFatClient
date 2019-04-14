package com.mikesajak.ebooklib.app.rest

import java.time.LocalDateTime

import com.mikesajak.ebooklibrary.payload.ServerInfo

case class ServerStatus(serverTime: Option[LocalDateTime], info: Option[ServerInfo])
