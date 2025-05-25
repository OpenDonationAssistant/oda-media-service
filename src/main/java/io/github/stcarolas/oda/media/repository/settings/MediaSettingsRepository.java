package io.github.stcarolas.oda.media.repository.settings;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.events.config.ConfigCommandSender;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class MediaSettingsRepository {

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
      .orElseGet(() ->
        new MediaSettings(
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
        )
      );
  }

  private MediaSettings map(MediaSettingsData data) {
    return new MediaSettings(repository, configCommandSender, data);
  }
}
