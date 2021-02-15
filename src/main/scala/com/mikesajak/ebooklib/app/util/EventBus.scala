package com.mikesajak.ebooklib.app.util

import com.google.common.eventbus.{DeadEvent, Subscribe}
import scribe.Logging

//noinspection UnstableApiUsage
class EventBus extends Logging {
  private val eventBus = new com.google.common.eventbus.EventBus("App event bus")
  eventBus.register(new DeadEventHandler)

  def publish[A](event: A): Unit = {
    logger.trace(s"Publishing event $event")
    eventBus.post(event)
    logger.trace(s"Event published, event=$event")
  }

  def register(subscriber: AnyRef): Unit = eventBus.register(subscriber)
  def unregister(subscriber: AnyRef): Unit = eventBus.unregister(subscriber)

}

//noinspection UnstableApiUsage
class DeadEventHandler extends Logging {
  @Subscribe
  def handleDeadEvent(de: DeadEvent): Unit = {
    logger.debug(s"Dead event (not-delivered): $de")
  }
}