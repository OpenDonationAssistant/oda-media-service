package io.github.opendonationassistant;

import io.github.opendonationassistant.media.listeners.CommandsListener;
import io.github.opendonationassistant.media.listeners.EventsListener;
import io.github.opendonationassistant.rabbit.AMQPConfiguration;
import io.github.opendonationassistant.rabbit.Exchange;
import io.github.opendonationassistant.rabbit.Queue;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.rabbitmq.connect.ChannelInitializer;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;

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
    return new AMQPConfiguration(
      List.of(
        Exchange.Exchange(
          "commands",
          Map.of("command.AddMediaCommand", CommandsListener.QUEUE)
        ),
        Exchange.Exchange(
          "history",
          Map.of(
            "event.HistoryItemEvent",
            EventsListener.QUEUE
          )
        )
      )
    );
  }
}
