package io.github.opendonationassistant.media.senders;

import io.github.opendonationassistant.media.repository.VideoData;
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;
import java.util.concurrent.CompletableFuture;

@RabbitClient("amq.topic")
public interface ReadyVideoNotificationSender {
  CompletableFuture<Void> send(@Binding String binding, VideoData notification);
}
