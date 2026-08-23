package io.github.opendonationassistant.media.listeners.handlers;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.events.history.event.HistoryItemEvent;
import io.github.opendonationassistant.media.metrics.MediaMetrics;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Optional;

@Singleton
public class HistoryItemEventHandler
  extends AbstractMessageHandler<HistoryItemEvent> {

  private final VideoRepository repository;
  private final MediaMetrics metrics;

  @Inject
  public HistoryItemEventHandler(
    ObjectMapper mapper,
    VideoRepository repository,
    MediaMetrics metrics
  ) {
    super(mapper);
    this.repository = repository;
    this.metrics = metrics;
  }

  @Override
  public void handle(HistoryItemEvent event) throws IOException {
    final var originId = event.originId();
    if (originId == null) {
      return;
    }
    repository
      .findPreparedVideosForPayment(originId)
      .thenAccept(videos -> {
        videos.forEach(video -> {
          video.makeReady(
            Optional.ofNullable(event.nickname()).orElse(""),
            event.recipientId(),
            originId
          );
          metrics.mediaPaymentAdded(event.system());
        });
      })
      .join();
  }
}
