package io.github.opendonationassistant.media.listeners;

import io.github.opendonationassistant.events.MessageProcessor;
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;

@RabbitListener
public class EventsListener {

  private final MessageProcessor processor;

  @Inject
  public EventsListener(MessageProcessor processor) {
    this.processor = processor;
  }

  @Queue("media.events")
  @Transactional
  public void listenMediaEvents(
    @MessageHeader("type") String type,
    byte[] data,
    RabbitAcknowledgement acknowledgement
  ) throws IOException {
    processor.process(type, data, acknowledgement);
  }
}
