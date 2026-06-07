package io.github.opendonationassistant;

import io.github.opendonationassistant.media.listeners.CommandsListener;
import io.github.opendonationassistant.media.listeners.EventsListener;
import io.github.opendonationassistant.rabbit.AMQPConfiguration;
import io.github.opendonationassistant.rabbit.Exchange;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.rabbitmq.connect.ChannelInitializer;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@OpenAPIDefinition(info = @Info(title = "oda-media-service", version = "1.0.0"))
public class Application {

  public static void main(String[] args) {
    Micronaut.build(args).banner(false).start();
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
    List<Exchange> bindings = new ArrayList<>();
    bindings.addAll(CommandsListener.BINDINGS);
    bindings.addAll(EventsListener.BINDINGS);
    return new AMQPConfiguration(bindings);
  }
}
