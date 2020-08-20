package com.mikesajak.ebooklib.app

import com.google.inject._
import com.mikesajak.ebooklib.app.bookformat.{BookFormatResolver, BookReadersRegistry}
import com.mikesajak.ebooklib.app.config.{AppSettings, Config, ConfigReader}
import com.mikesajak.ebooklib.app.reader.EpubBookMetadataReader
import com.mikesajak.ebooklib.app.rest.{BookServerController, ServerConnectionController}
import com.mikesajak.ebooklib.app.ui.{ActionsController, BookDataProviderFactory, ResourceManager}
import com.mikesajak.ebooklib.app.util.EventBus
import net.codingwell.scalaguice.ScalaModule

class ApplicationContext extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    install(new ConfigContext)
    install(new WebContext)
    install(new UIContext)
  }

  @Provides
  @Singleton
  def bookReadersRegistry(): BookReadersRegistry = {
    val registry = new BookReadersRegistry
    // TODO: add some autodiscovery of book formats
    registry.register(new EpubBookMetadataReader)
    registry
  }

  @Provides
  @Singleton
  def bookDataProviderFactory(bookServerController: BookServerController) =
    new BookDataProviderFactory(bookServerController)

  @Provides
  @Singleton
  def getBookFormatResolver = new BookFormatResolver()

  @Provides
  @Singleton
  def appController(config: Config) = new AppController(config)

  @Provides
  @Singleton
  def resourceManager() = new ResourceManager()

  @Provides
  @Singleton
  def eventBus() = new EventBus
}

class UIContext extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
  }

  @Provides
  @Singleton
  def actionsController(appController: AppController, bookReadersRegistry: BookReadersRegistry,
                        bookFormatResolver: BookFormatResolver) =
    new ActionsController(appController, bookReadersRegistry, bookFormatResolver)
}

class WebContext extends AbstractModule with ScalaModule {
  override def configure(): Unit = {

  }

  @Provides
  @Singleton
  def serverConnectionController(bookServerController: BookServerController,
                                 appSettings: AppSettings,
                                 eventBus: EventBus) =
    new ServerConnectionController(bookServerController, appSettings, eventBus)

  @Provides
  @Singleton
  def bookServerController(appSettings: AppSettings) =
    new BookServerController(appSettings)
}

class ConfigContext extends AbstractModule with ScalaModule {
  override def configure(): Unit = {

  }

  @Provides
  @Singleton
  def configReader() = new ConfigReader()

  @Provides
  @Singleton
  def config(reader: ConfigReader): Config = {
    val config = new Config(reader)
    config.load()
    config
  }

  @Provides
  @Singleton
  def appSettings(config: Config): AppSettings = config.settings

}

object ApplicationContext {
  lazy val globalInjector: Injector = Guice.createInjector(new ApplicationContext)
}