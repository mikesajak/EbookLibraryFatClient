package com.mikesajak.ebooklib.app

import java.util.concurrent.{ExecutorService, Executors}

import com.google.inject._
import com.mikesajak.ebooklib.app.bookformat.{BookFormatResolver, BookReadersRegistry}
import com.mikesajak.ebooklib.app.config.{AppSettings, Config, ConfigReader}
import com.mikesajak.ebooklib.app.model.BookDtoConverter
import com.mikesajak.ebooklib.app.reader.EpubBookMetadataReader
import com.mikesajak.ebooklib.app.rest.{BookServerController, BookServerControllerSttp, ServerConnectionService}
import com.mikesajak.ebooklib.app.ui.{ActionsController, BookDataProviderFactory, ResourceManager}
import com.mikesajak.ebooklib.app.util.EventBus
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.ExecutionContext

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
                        bookFormatResolver: BookFormatResolver, bookServerController: BookServerController,
                        eventBus: EventBus) =
    new ActionsController(appController, bookReadersRegistry, bookFormatResolver, bookServerController, eventBus)
}

class WebContext extends AbstractModule with ScalaModule {
  override def configure(): Unit = {

  }

  @Provides
  @Singleton
  def httpCallExecutionContext(): ExecutionContext = {
    new ExecutionContext {
      val threadPool: ExecutorService = Executors.newFixedThreadPool(10)

      def execute(runnable: Runnable): Unit = {
        threadPool.submit(runnable)
      }

      def reportFailure(t: Throwable): Unit = {}
    }
  }

  @Provides
  @Singleton
  def serverConnectionController(bookServerController: BookServerController,
                                 appSettings: AppSettings,
                                 eventBus: EventBus) =
    new ServerConnectionService(bookServerController, appSettings, eventBus)

  @Provides
  @Singleton
  def bookServerController(appSettings: AppSettings, bookDtoConverter: BookDtoConverter,
                           httpCallExecutionContext: ExecutionContext): BookServerController =
    new BookServerControllerSttp(appSettings, bookDtoConverter, httpCallExecutionContext)

  @Provides
  @Singleton
  def bookDtoConverter() = new BookDtoConverter
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