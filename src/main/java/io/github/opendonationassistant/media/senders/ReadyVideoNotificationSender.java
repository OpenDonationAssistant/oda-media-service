package io.github.opendonationassistant.media.senders;

import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;

@RabbitClient("amq.topic")
public interface ReadyVideoNotificationSender {
  void send(@Binding String binding, ReadyVideo notification);
}
