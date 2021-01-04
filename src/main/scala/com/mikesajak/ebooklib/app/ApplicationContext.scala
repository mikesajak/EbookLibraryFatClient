package com.mikesajak.ebooklib.app

import java.util.concurrent.Executors

import com.google.inject._
import com.google.inject.name.Named
import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.config.{AppSettings, Config, ConfigReader}
import com.mikesajak.ebooklib.app.model.BookDtoConverter
import com.mikesajak.ebooklib.app.reader._
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
  def bookFormatDataParsers(isbnParser: ISBNParser): Seq[BookFormatDataParser] =
    Seq(new TikaBookFormatDataParser(isbnParser),
        new EpubBookFormatDataParser(),
        new PdfBookFormatDataParser(isbnParser),
        new ChmBookFormatDataParser())

  @Provides
  @Singleton
  def isbnParser() = new ISBNParser

  @Provides
  @Singleton
  def bookFormatDataReader(bookFormatResolver: BookFormatResolver, bookFormatDataParsers: Seq[BookFormatDataParser]) =
    new BookFormatDataReader(bookFormatResolver, bookFormatDataParsers)

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

  @Provides
  @Singleton
  @Named("externalOpenExecutionContext")
  def externalOpenExecutionContext(): ExecutionContext = {
    import java.util.concurrent.{ForkJoinPool, ForkJoinWorkerThread}

    val factory: ForkJoinPool.ForkJoinWorkerThreadFactory = new ForkJoinPool.ForkJoinWorkerThreadFactory() {
      override def newThread(pool: ForkJoinPool): ForkJoinWorkerThread = {
        val worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool)
        worker.setName("external-execution-thread-" + worker.getPoolIndex)
        worker
      }
    }

    ExecutionContext.fromExecutor(new ForkJoinPool(3, factory, null, true))
  }
}

class UIContext extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
  }

  @Provides
  @Singleton
  def actionsController(appController: AppController, bookServerController: BookServerController,
                        eventBus: EventBus, bookFormatDataReader: BookFormatDataReader) =
    new ActionsController(appController, bookServerController, eventBus, bookFormatDataReader)
}

class WebContext extends AbstractModule with ScalaModule {
  override def configure(): Unit = {

  }

  @Provides
  @Singleton
  @Named("httpCallExecutionContext")
  def httpCallExecutionContext(): ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  @Provides
  @Singleton
  def serverConnectionController(bookServerController: BookServerController,
                                 appSettings: AppSettings,
                                 eventBus: EventBus) =
    new ServerConnectionService(bookServerController, appSettings, eventBus)

  @Provides
  @Singleton
  def bookServerController(appSettings: AppSettings, bookDtoConverter: BookDtoConverter,
                           @Named("httpCallExecutionContext") httpCallExecutionContext: ExecutionContext): BookServerController =
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