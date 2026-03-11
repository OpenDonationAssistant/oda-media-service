package io.github.opendonationassistant.media.listeners;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.history.event.HistoryItemEvent;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.github.opendonationassistant.rabbit.Queue.Media;
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RabbitListener
public class EventsListener {

  private final VideoRepository repository;
  private final ODALogger log = new ODALogger(this);

  @Inject
  public EventsListener(VideoRepository repository) {
    this.repository = repository;
  }

  @Queue(Media.EVENTS)
  @Transactional
  public void listenMediaEvents(
    @MessageHeader("type") String type,
    byte[] data,
    RabbitAcknowledgement acknowledgement
  ) throws IOException {
    log.debug("Received Event", Map.of("type", type));
    switch (type) {
      case "HistoryItemEvent":
        var notification = ObjectMapper.getDefault()
          .readValue(data, HistoryItemEvent.class);
        log.debug(
          "Received HistoryItemEvent",
          Map.of("type", type, "notification", notification)
        );
        if (notification == null) {
          break;
        }
        final var originId = notification.originId();
        if (originId == null) {
          break;
        }
        repository
          .findPreparedVideosForPayment(originId)
          .thenAccept(videos -> {
            videos.forEach(video ->
              video.makeReady(
                Optional.ofNullable(notification.nickname()).orElse(""),
                notification.recipientId(),
                originId
              )
            );
            acknowledgement.ack();
          })
          .join();
        break;
      default:
        break;
    }
  }
}
