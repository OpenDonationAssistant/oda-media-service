package io.github.stcarolas.oda.media.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.opendonationassistant.events.config.ConfigCommand;
import io.github.opendonationassistant.events.config.ConfigCommandSender;
import io.github.opendonationassistant.events.widget.Widget;
import io.github.opendonationassistant.settings.repository.MediaSettings;
import io.github.opendonationassistant.settings.repository.MediaSettingsData;
import io.github.opendonationassistant.settings.repository.MediaSettingsData.TARIFICATION;
import io.github.opendonationassistant.settings.repository.MediaSettingsDataRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class MediaSettingsTest {

  private static final String RECIPIENT_ID = "recipient-1";
  private static final String PAYMENT_PAGE = "paymentpage";
  private static final String SHOW_QUEUE_SIZE_KEY = "media.requests.showQueueSize";

  private final MediaSettingsDataRepository repository = mock(
    MediaSettingsDataRepository.class
  );
  private final ConfigCommandSender configCommandSender = mock(
    ConfigCommandSender.class
  );

  @Test
  public void sendsDisabledQueueSizeToPaymentPage() {
    var settings = new MediaSettings(
      repository,
      configCommandSender,
      data()
    );

    settings.apply(
      config(List.of(property("showQueueSizeOnPage", Boolean.FALSE)))
    );

    var command = capturedCommand(SHOW_QUEUE_SIZE_KEY);
    assertEquals(RECIPIENT_ID, command.ownerId());
    assertEquals(PAYMENT_PAGE, command.name());
    assertEquals(SHOW_QUEUE_SIZE_KEY, command.key());
    assertEquals(Boolean.FALSE, command.value());
  }

  @Test
  public void defaultsQueueSizeToEnabledWhenPropertyIsAbsent() {
    var settings = new MediaSettings(
      repository,
      configCommandSender,
      data()
    );

    settings.apply(config(List.of()));

    assertEquals(Boolean.TRUE, capturedCommand(SHOW_QUEUE_SIZE_KEY).value());
  }

  @Test
  public void defaultsQueueSizeToEnabledWhenPropertyValueIsNull() {
    var settings = new MediaSettings(
      repository,
      configCommandSender,
      data()
    );

    settings.apply(
      config(List.of(property("showQueueSizeOnPage", null)))
    );

    assertEquals(Boolean.TRUE, capturedCommand(SHOW_QUEUE_SIZE_KEY).value());
  }

  private ConfigCommand.PutKeyValue capturedCommand(String key) {
    var captor = ArgumentCaptor.forClass(ConfigCommand.PutKeyValue.class);
    verify(configCommandSender, atLeastOnce()).send(captor.capture());
    var commands = captor
      .getAllValues()
      .stream()
      .filter(command -> key.equals(command.key()))
      .toList();
    assertEquals(1, commands.size(), "exactly one command for " + key);
    return commands.get(0);
  }

  private static Widget.WidgetConfig config(
    List<Widget.WidgetProperty> properties
  ) {
    return new Widget.WidgetConfig(properties);
  }

  private static Widget.WidgetProperty property(String name, Object value) {
    return new Widget.WidgetProperty(name, name, "boolean", value);
  }

  private static MediaSettingsData data() {
    return new MediaSettingsData(
      "settings-1",
      RECIPIENT_ID,
      100,
      TARIFICATION.PER_LINK,
      60,
      12,
      0,
      true,
      true,
      true,
      "",
      List.of()
    );
  }
}
