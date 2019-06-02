package com.mikesajak.ebooklib.app

import com.google.inject._
import com.mikesajak.ebooklib.app.bookformat.BookReadersRegistry
import com.mikesajak.ebooklib.app.config.{AppSettings, Config, ConfigReader}
import com.mikesajak.ebooklib.app.rest.{BookServerController, ServerConnectionController}
import com.mikesajak.ebooklib.app.ui.{ActionsController, ResourceManager}
import com.mikesajak.ebooklib.app.util.EventBus
import com.mikesajak.ebooklibrary.bookformat.EpubBookMetadataReader
import net.codingwell.scalaguice.ScalaModule
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestTemplate


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
  def actionsController(resourcesMgr: ResourceManager, appController: AppController,
                        bookReadersRegistry: BookReadersRegistry) =
    new ActionsController(resourcesMgr, appController, bookReadersRegistry)
}

class WebContext extends AbstractModule with ScalaModule {
  override def configure(): Unit = {

  }

  @Provides
  @Singleton
  def serverRestTemplate(appSettings: AppSettings): RestTemplate = {
    //  val builder = new Jackson2ObjectMapperBuilder()
    //  builder.modulesToInstall(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))

    new RestTemplateBuilder()
        .rootUri(appSettings.server.address)
        //      .additionalMessageConverters(new MappingJackson2HttpMessageConverter(builder.build()))
        .build()
  }

  @Provides
  @Singleton
  def serverConnectionController(bookServerController: BookServerController,
                                 appSettings: AppSettings,
                                 eventBus: EventBus) =
    new ServerConnectionController(bookServerController, appSettings, eventBus)

  @Provides
  @Singleton
  def bookServerController(serverRestTemplate: RestTemplate, appSettings: AppSettings) =
    new BookServerController(serverRestTemplate, appSettings)
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