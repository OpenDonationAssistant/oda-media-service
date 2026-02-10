package io.github.opendonationassistant.playlist.repository;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface PlaylistDataRepository
  extends CrudRepository<PlaylistData, String> {
  Page<PlaylistData> findByOwnerIdOrderById(String ownerId, Pageable pageable);
}
