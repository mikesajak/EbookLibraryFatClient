package com.mikesajak.ebooklib.app

import com.google.inject._
import com.google.inject.name.Named
import com.mikesajak.ebooklib.app.bookformat.BookFormatResolver
import com.mikesajak.ebooklib.app.config.{AppSettings, Config, ConfigReader}
import com.mikesajak.ebooklib.app.model.BookDtoConverter
import com.mikesajak.ebooklib.app.reader._
import com.mikesajak.ebooklib.app.rest.{BookServerService, BookServerServiceSttp, ServerConnectionService}
import com.mikesajak.ebooklib.app.ui.{ActionsController, BookDataProviderFactory, ResourceManager}
import com.mikesajak.ebooklib.app.util.EventBus
import net.codingwell.scalaguice.ScalaModule

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class ApplicationContext extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    install(new ConfigContext)
    install(new WebContext)
    install(new UIContext)
  }

  @Provides
  @Singleton
  def bookFormatDataParsers(isbnParser: ISBNParser, dateParser: DateParser): Seq[BookFormatDataParser] =
    Seq(new TikaBookFormatDataParser(isbnParser, dateParser),
        new EpubBookFormatDataParser(),
        new PdfBookFormatDataParser(isbnParser),
        new ChmBookFormatDataParser(),
        new MobiBookFormatDataParser(dateParser))

  @Provides
  @Singleton
  def isbnParser() = new ISBNParser

  @Provides
  @Singleton
  def dateParser() = new DateParser

  @Provides
  @Singleton
  def bookFormatDataReader(bookFormatResolver: BookFormatResolver, bookFormatDataParsers: Seq[BookFormatDataParser]) =
    new BookFormatDataReader(bookFormatResolver, bookFormatDataParsers)

  @Provides
  @Singleton
  def getBookFormatResolver = new BookFormatResolver()

  @Provides
  @Singleton
  def bookDataProviderFactory() = new BookDataProviderFactory()

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
  def actionsController(appController: AppController, bookServerService: BookServerService, eventBus: EventBus) =
    new ActionsController(appController, bookServerService, eventBus)
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
  def serverConnectionController(bookServerService: BookServerService,
                                 appSettings: AppSettings,
                                 eventBus: EventBus) =
    new ServerConnectionService(bookServerService, appSettings, eventBus)

  @Provides
  @Singleton
  def bookServerService(appSettings: AppSettings, bookDtoConverter: BookDtoConverter,
                        @Named("httpCallExecutionContext") httpCallExecutionContext: ExecutionContext): BookServerService =
    new BookServerServiceSttp(appSettings, bookDtoConverter, httpCallExecutionContext)

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
  def config(reader: ConfigReader, eventBus: EventBus): Config = {
    val config = new Config(reader)
    config.load()
    eventBus.register(config)
    config
  }

  @Provides
  @Singleton
  def appSettings(config: Config): AppSettings = config.settings

}

object ApplicationContext {
  lazy val globalInjector: Injector = Guice.createInjector(new ApplicationContext)
}