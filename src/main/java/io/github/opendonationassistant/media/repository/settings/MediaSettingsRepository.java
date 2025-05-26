package io.github.opendonationassistant.media.repository.settings;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.commons.ToString;
import io.github.opendonationassistant.events.config.ConfigCommandSender;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
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
      .map(this::map)
      .orElseGet(() -> {
        var settings = new MediaSettings(
          repository,
          configCommandSender,
          new MediaSettingsData(
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
          )
        );
        log.info("Created settings: {}", ToString.asJson(settings));
        return settings;
      });
  }

  private MediaSettings map(MediaSettingsData data) {
    var settings = new MediaSettings(repository, configCommandSender, data);
    log.info("Converted settings: {}", ToString.asJson(settings));
    return settings;
  }
}
