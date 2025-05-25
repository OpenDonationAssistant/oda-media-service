package io.github.stcarolas.oda.media.listener;

import io.github.opendonationassistant.events.widget.WidgetChangedEvent;
import io.github.opendonationassistant.events.widget.WidgetConfig;
import io.github.opendonationassistant.events.widget.WidgetProperty;
import io.github.opendonationassistant.rabbit.Queue.Configs;
import io.github.stcarolas.oda.media.repository.settings.MediaSettings;
import io.github.stcarolas.oda.media.repository.settings.MediaSettingsData;
import io.github.stcarolas.oda.media.repository.settings.MediaSettingsRepository;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import jakarta.inject.Inject;
import java.util.Optional;

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
      changes.getWidget().getOwnerId()
    );
    final WidgetConfig config = changes.getWidget().getConfig();
    settings.apply(config);
  }
}
