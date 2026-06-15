package io.github.opendonationassistant.settings.listener;

import io.github.opendonationassistant.events.widget.WidgetChangedEvent;
import io.github.opendonationassistant.rabbit.Exchange;
import io.github.opendonationassistant.settings.repository.MediaSettings;
import io.github.opendonationassistant.settings.repository.MediaSettingsRepository;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

@RabbitListener
public class WidgetConfigListener {

  public static final String QUEUE_NAME = "config.media";
  public static final io.github.opendonationassistant.rabbit.Queue QUEUE =
    new io.github.opendonationassistant.rabbit.Queue(QUEUE_NAME);
  public static final List<Exchange> BINDINGS = List.of(
    Exchange.Exchange(
      "changes.widgets",
      Map.of("media", WidgetConfigListener.QUEUE)
    )
  );

  private MediaSettingsRepository repository;

  @Inject
  public WidgetConfigListener(MediaSettingsRepository repository) {
    this.repository = repository;
  }

  @Queue(QUEUE_NAME)
  public void listen(WidgetChangedEvent changes) {
    final MediaSettings settings = repository.getByRecipientId(
      changes.widget().ownerId()
    );
    settings.apply(changes.widget().config());
  }
}
