package com.mikesajak.ebooklib.app

import scribe.format.{Formatter, FormatterInterpolator, date, levelColored, mdc, message, positionAbbreviated, string}
import scribe.{Level, format}
import sttp.client3.SttpBackend
import sttp.client3.logging.LogLevel
import sttp.client3.logging.scribe.ScribeLoggingBackend
import sttp.model.StatusCode

object ScribeCfg {
  lazy val compactAbbreviatedFormatter: Formatter =
    formatter"$date ${string("[")}$levelColored${string("]")} ${format.green(positionAbbreviated)} - $message$mdc"

  def initScribeLogging(): Unit = {
    scribe.Logger.root
          .clearHandlers()
          .clearModifiers()
          .withHandler(minimumLevel = Some(Level.Trace), formatter = compactAbbreviatedFormatter)
          .replace()
  }

  def debugScribeSttpBackendWrapper[F[_], S](backend: SttpBackend[F, S]): SttpBackend[F, S] =
    ScribeLoggingBackend(backend,
                         includeTiming = true,
                         beforeCurlInsteadOfShow = true,
                         logRequestBody = true,
                         logRequestHeaders = true,
                         logResponseBody = true,
                         logResponseHeaders = true,
                         beforeRequestSendLogLevel = LogLevel.Trace,
                         responseExceptionLogLevel = LogLevel.Warn,
                         responseLogLevel = responseLogLevelResolver)

  def responseLogLevelResolver(statusCode: StatusCode): LogLevel = statusCode match {
    case code if code.isClientError || code.isServerError => LogLevel.Warn
    case _ => LogLevel.Trace
  }
}
