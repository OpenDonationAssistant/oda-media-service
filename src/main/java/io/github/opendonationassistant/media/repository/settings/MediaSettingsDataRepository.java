package io.github.opendonationassistant.media.repository.settings;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface MediaSettingsDataRepository
  extends CrudRepository<MediaSettingsData, String> {
  Optional<MediaSettingsData> findByRecipientId(String recipientId);
}
