package io.github.opendonationassistant.media.listeners;

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

@RabbitListener
public class EventsListener {

  private final VideoRepository repository;

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
    switch (type) {
      case "HistoryItemEvent":
        var notification = ObjectMapper.getDefault()
          .readValue(data, HistoryItemEvent.class);
        repository
          .findPreparedVideosForPayment(notification.originId())
          .thenAccept(videos -> {
            videos.forEach(video ->
              video.makeReady(
                notification.nickname(),
                notification.recipientId()
              )
            );
            acknowledgement.ack();
          });
        break;
      default:
        break;
    }
  }
}
