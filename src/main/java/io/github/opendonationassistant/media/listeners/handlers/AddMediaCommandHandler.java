package io.github.opendonationassistant.media.listeners.handlers;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.media.metrics.MediaMetrics;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Map;

@Singleton
public class AddMediaCommandHandler
  extends AbstractMessageHandler<AddMediaCommandHandler.AddMediaCommand> {

  private ODALogger log = new ODALogger(this);

  private final VideoRepository videoRepository;
  private final MediaMetrics metrics;

  @Inject
  public AddMediaCommandHandler(
    ObjectMapper mapper,
    VideoRepository videoRepository,
    MediaMetrics metrics
  ) {
    super(mapper);
    this.videoRepository = videoRepository;
    this.metrics = metrics;
  }

  @Override
  public void handle(AddMediaCommand message) throws IOException {
    metrics.mediaRequested(message.system());
    try {
      videoRepository
        .create(message.recipientId(), message.url())
        .join()
        .makeReady(message.requester(), message.recipientId(), null);
      metrics.mediaAdded(message.system());
    } catch (Exception e) {
      log.info("Failed to add media", Map.of("message", message));
    }
  }

  @Serdeable
  public static record AddMediaCommand(
    String url,
    String requester,
    String recipientId,
    String system
  ) {}
}
