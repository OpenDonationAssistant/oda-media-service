package io.github.opendonationassistant.media.listeners.handlers;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;
import java.io.IOException;

@Singleton
public class AddMediaCommandHandler
  extends AbstractMessageHandler<AddMediaCommandHandler.AddMediaCommand> {

  public AddMediaCommandHandler(ObjectMapper mapper) {
    super(mapper);
  }

  @Override
  public void handle(AddMediaCommand message) throws IOException {

  }

  @Serdeable
  public static record AddMediaCommand() {}
}
