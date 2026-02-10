package io.github.opendonationassistant.settings.repository;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.events.config.ConfigCommandSender;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MediaSettingsRepository {

  private Logger log = LoggerFactory.getLogger(MediaSettingsRepository.class);

  private MediaSettingsDataRepository repository;
  private ConfigCommandSender configCommandSender;

  @Inject
  public MediaSettingsRepository(
    MediaSettingsDataRepository repository,
    ConfigCommandSender configCommandSender
  ) {
    this.repository = repository;
    this.configCommandSender = configCommandSender;
  }

  public MediaSettings getByRecipientId(String recipientId) {
    return repository
      .findByRecipientId(recipientId)
      .map(data -> new MediaSettings(repository, configCommandSender, data))
      .orElseGet(() -> {
        var data = new MediaSettingsData(
          Generators.timeBasedEpochGenerator().generate().toString(),
          recipientId,
          100,
          12,
          100,
          true,
          true,
          true,
          "",
          List.of()
        );
        var settings = new MediaSettings(repository, configCommandSender, data);
        log.info("Created MediaSettings", Map.of("data", data));
        return settings;
      });
  }
}
