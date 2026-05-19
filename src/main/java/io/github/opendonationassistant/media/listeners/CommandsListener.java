package io.github.opendonationassistant.media.listeners;

import io.github.opendonationassistant.events.MessageProcessor;
import io.github.opendonationassistant.rabbit.Queue;
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;

@RabbitListener
public class CommandsListener {

  public static final String QUEUE_NAME = "media.commands";
  public static final Queue QUEUE = new Queue(QUEUE_NAME);

  private final MessageProcessor processor;

  @Inject
  public CommandsListener(MessageProcessor processor) {
    this.processor = processor;
  }

  @Transactional
  @io.micronaut.rabbitmq.annotation.Queue(QUEUE_NAME)
  public void listenMediaCommands(
    @MessageHeader("type") String type,
    byte[] data,
    RabbitAcknowledgement acknowledgement
  ) throws IOException {
    processor.process(type, data, acknowledgement);
  }
}
