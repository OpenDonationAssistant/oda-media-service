package io.github.opendonationassistant.settings.listener;

import io.github.opendonationassistant.events.widget.WidgetChangedEvent;
import io.github.opendonationassistant.rabbit.Queue.Configs;
import io.github.opendonationassistant.settings.repository.MediaSettings;
import io.github.opendonationassistant.settings.repository.MediaSettingsRepository;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import jakarta.inject.Inject;

@RabbitListener
public class WidgetConfigListener {

  private MediaSettingsRepository repository;

  @Inject
  public WidgetConfigListener(MediaSettingsRepository repository) {
    this.repository = repository;
  }

  @Queue(Configs.MEDIA)
  public void listen(WidgetChangedEvent changes) {
    final MediaSettings settings = repository.getByRecipientId(
      changes.widget().ownerId()
    );
    settings.apply(changes.widget().config());
  }
}
