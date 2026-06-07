package io.github.opendonationassistant.media.listeners;

import io.github.opendonationassistant.events.MessageProcessor;
import io.github.opendonationassistant.rabbit.Exchange;
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RabbitListener
public class EventsListener {

  public static final String QUEUE_NAME = "media.events";
  public static final io.github.opendonationassistant.rabbit.Queue QUEUE =
    new io.github.opendonationassistant.rabbit.Queue(QUEUE_NAME);
  public static final List<Exchange> BINDINGS = List.of(
    Exchange.Exchange(
      "events",
      Map.of("event.HistoryItemEvent", EventsListener.QUEUE)
    )
  );

  private final MessageProcessor processor;

  @Inject
  public EventsListener(MessageProcessor processor) {
    this.processor = processor;
  }

  @Queue(QUEUE_NAME)
  @Transactional
  public void listenMediaEvents(
    @MessageHeader("type") String type,
    byte[] data,
    RabbitAcknowledgement acknowledgement
  ) throws IOException {
    processor.process(type, data, acknowledgement);
  }
}
