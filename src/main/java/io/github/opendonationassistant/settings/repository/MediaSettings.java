package io.github.opendonationassistant.settings.repository;

import io.github.opendonationassistant.commons.ToString;
import io.github.opendonationassistant.events.config.ConfigCommand;
import io.github.opendonationassistant.events.config.ConfigCommandSender;
import io.github.opendonationassistant.events.widget.WidgetConfig;
import io.github.opendonationassistant.events.widget.WidgetProperty;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaSettings {

  private Logger log = LoggerFactory.getLogger(MediaSettings.class);

  private MediaSettingsData data;
  private MediaSettingsDataRepository repository;
  private ConfigCommandSender configCommandSender;

  public MediaSettings(
    MediaSettingsDataRepository repository,
    ConfigCommandSender configCommandSender,
    MediaSettingsData data
  ) {
    this.repository = repository;
    this.configCommandSender = configCommandSender;
    this.data = data;
  }

  public void update(MediaSettingsData data) {
    this.data = data;
  }

  public void save() {
    this.repository.findById(data.id()).ifPresentOrElse(
        old -> {
          log.info("Updating settings: {}", data.id());
          repository.update(data);
        },
        () -> {
          log.info("Saving new settings:{}", ToString.asJson(data));
          repository.save(data);
        }
      );
  }

  public void apply(WidgetConfig config) {
    var tooltip = get(config, "requestTooltip")
      .map(property -> (String) property.value())
      .orElse("");
    var requestCost = get(config, "songRequestCost")
      .map(property -> (Integer) property.value())
      .orElse(100);
    var maxAmount = get(config, "songMaxAmount")
      .map(property -> (Integer) property.value())
      .orElse(12);
    var requestsEnabled = get(config, "requestsEnabled")
      .map(property -> (Boolean) property.value())
      .orElse(true);

    var updated = new MediaSettingsData(
      data.id(),
      data.recipientId(),
      requestCost,
      maxAmount,
      get(config, "requestViewAmount")
        .map(property -> (Integer) property.value())
        .orElse(0),
      requestsEnabled,
      get(config, "youtubeEnabled")
        .map(property -> (Boolean) property.value())
        .orElse(true),
      get(config, "vkvideoEnabled")
        .map(property -> (Boolean) property.value())
        .orElse(false),
      tooltip,
      Arrays.asList(
        get(config, "wordsBlacklist")
          .map(property -> (String) property.value())
          .orElse("")
          .split("\n")
      )
    );

    configCommandSender.send(
      new ConfigCommand.PutKeyValue(
        data.recipientId(),
        "paymentpage",
        "media.requests.disabled.permanently",
        false
      )
    );
    configCommandSender.send(
      new ConfigCommand.PutKeyValue(
        data.recipientId(),
        "paymentpage",
        "media.requests.amount",
        maxAmount
      )
    );
    configCommandSender.send(
      new ConfigCommand.PutKeyValue(
        data.recipientId(),
        "paymentpage",
        "media.requests.tooltip",
        tooltip
      )
    );
    configCommandSender.send(
      new ConfigCommand.PutKeyValue(
        data.recipientId(),
        "paymentpage",
        "media.requests.cost",
        requestCost
      )
    );
    configCommandSender.send(
      new ConfigCommand.PutKeyValue(
        data.recipientId(),
        "paymentpage",
        "media.requests.enabled",
        requestsEnabled
      )
    );

    this.data = updated;
    this.save();
  }

  public MediaSettingsData getData() {
    return this.data;
  }

  public Integer minViewAmount() {
    return this.data.requestViewAmount();
  }

  public boolean passWordsBlacklist(String title) {
    var normalizedTitle = title.toLowerCase();
    return this.data.wordsBlacklist()
      .stream()
      .filter(word -> normalizedTitle.contains(word))
      .findFirst()
      .isEmpty();
  }

  private Optional<WidgetProperty> get(WidgetConfig config, String property) {
    return Optional.ofNullable(config.properties())
      .orElse(List.of())
      .stream()
      .filter(it -> property.equals(it.name()))
      .findFirst();
  }
}
