package io.github.opendonationassistant.media.repository.settings;

import io.github.opendonationassistant.commons.ToString;
import io.github.opendonationassistant.events.config.ConfigCommandSender;
import io.github.opendonationassistant.events.config.ConfigPutCommand;
import io.github.opendonationassistant.events.widget.WidgetConfig;
import io.github.opendonationassistant.events.widget.WidgetProperty;
import java.util.Arrays;
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
      .map(property -> (String) property.getValue())
      .orElse("");
    var requestCost = get(config, "songRequestCost")
      .map(property -> (Integer) property.getValue())
      .orElse(0);
    var maxAmount = get(config, "songMaxAmount")
      .map(property -> (Integer) property.getValue())
      .orElse(0);
    var requestsEnabled = get(config, "requestsEnabled")
      .map(property -> (Boolean) property.getValue())
      .orElse(true);
    var updated = new MediaSettingsData(
      data.id(),
      data.recipientId(),
      requestCost,
      maxAmount,
      get(config, "requestViewAmount")
        .map(property -> (Integer) property.getValue())
        .orElse(0),
      requestsEnabled,
      get(config, "youtubeEnabled")
        .map(property -> (Boolean) property.getValue())
        .orElse(false),
      get(config, "vkvideoEnabled")
        .map(property -> (Boolean) property.getValue())
        .orElse(false),
      tooltip,
      Arrays.asList(
        get(config, "wordsBlacklist")
          .map(property -> (String) property.getValue())
          .orElse("")
          .split("\n")
      )
    );
    var command = new ConfigPutCommand();
    command.setOwnerId(this.data.recipientId());
    command.setName("paymentpage");
    command.setKey("media.requests.disabled.permanently");
    command.setValue(false);
    configCommandSender.send(command);

    command = new ConfigPutCommand();
    command.setOwnerId(this.data.recipientId());
    command.setName("paymentpage");
    command.setKey("media.requests.amount");
    command.setValue(maxAmount);
    configCommandSender.send(command);

    command = new ConfigPutCommand();
    command.setOwnerId(this.data.recipientId());
    command.setName("paymentpage");
    command.setKey("media.requests.tooltip");
    command.setValue(tooltip);
    configCommandSender.send(command);

    command = new ConfigPutCommand();
    command.setOwnerId(this.data.recipientId());
    command.setName("paymentpage");
    command.setKey("media.requests.cost");
    command.setValue(requestCost);
    configCommandSender.send(command);

    command = new ConfigPutCommand();
    command.setOwnerId(this.data.recipientId());
    command.setName("paymentpage");
    command.setKey("media.requests.enabled");
    command.setValue(requestsEnabled);
    configCommandSender.send(command);

    this.save();
    this.data = updated;
  }

  public MediaSettingsData getData(){
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
    return config
      .getProperties()
      .stream()
      .filter(it -> property.equals(it.getName()))
      .findFirst();
  }
}
