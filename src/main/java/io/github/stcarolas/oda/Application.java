package io.github.stcarolas.oda;

import io.github.opendonationassistant.rabbit.RabbitConfiguration;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.rabbitmq.connect.ChannelInitializer;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import jakarta.inject.Singleton;

@OpenAPIDefinition(
  info = @Info(
    title = "ODA Media Service",
    description = "Media Service API",
    license = @License(
      name = "GPL-3.0",
      url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
    ),
    contact = @Contact(name = "stCarolas", email = "stcarolas@gmail.com")
  )
)
public class Application {

  public static void main(String[] args) {
    Beans.context = Micronaut.build(args).banner(false).start();
  }

  @ContextConfigurer
  public static class DefaultEnvironmentConfigurer
    implements ApplicationContextConfigurer {

    @Override
    public void configure(ApplicationContextBuilder builder) {
      builder.defaultEnvironments("standalone");
    }
  }

  @Singleton
  public ChannelInitializer rabbitConfiguration() {
    return new RabbitConfiguration();
  }
}
