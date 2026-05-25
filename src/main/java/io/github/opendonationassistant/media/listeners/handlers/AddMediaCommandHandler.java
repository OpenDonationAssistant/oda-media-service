package io.github.opendonationassistant.media.listeners.handlers;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;
import java.io.IOException;

@Singleton
public class AddMediaCommandHandler
  extends AbstractMessageHandler<AddMediaCommandHandler.AddMediaCommand> {

  private final VideoRepository videoRepository;

  public AddMediaCommandHandler(
    ObjectMapper mapper,
    VideoRepository videoRepository
  ) {
    super(mapper);
    this.videoRepository = videoRepository;
  }

  @Override
  public void handle(AddMediaCommand message) throws IOException {
    videoRepository
      .create(message.recipientId(), message.url())
      .join();
  }

  @Serdeable
  public static record AddMediaCommand(
    String url,
    String requester,
    String recipientId,
    String system
  ) {}
}
