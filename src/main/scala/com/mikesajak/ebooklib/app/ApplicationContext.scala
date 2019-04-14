package com.mikesajak.ebooklib.app

import com.google.inject.{AbstractModule, Guice, Injector, Provides, Singleton}
import com.mikesajak.ebooklib.app.config.{AppSettings, Config, ConfigReader}
import com.mikesajak.ebooklib.app.rest.ServerController
import com.mikesajak.ebooklib.app.ui.ResourceManager
import com.mikesajak.ebooklib.app.util.EventBus
import net.codingwell.scalaguice.ScalaModule
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestTemplate


class ApplicationContext extends AbstractModule with ScalaModule {
  def configure(): Unit = {
//    install(new PanelContext(LeftPanel))
//    install(new PanelContext(RightPanel))
//
//    install(new UIOperationControllersContext)
    install(new ConfigContext)
    install(new WebContext)
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
  def serverController(appSettings: AppSettings, serverRestTemplate: RestTemplate,
                       eventBus: EventBus) =
    new ServerController(appSettings, serverRestTemplate, eventBus)
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