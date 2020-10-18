package com.mikesajak.ebooklib.app.dto

case class ErrorResponse(timestamp: String, status: Int, error: String, message: String, path: String)
