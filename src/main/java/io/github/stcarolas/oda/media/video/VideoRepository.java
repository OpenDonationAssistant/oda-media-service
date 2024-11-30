package io.github.stcarolas.oda.media.video;

import java.util.List;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface VideoRepository extends CrudRepository<Video, String> {
  List<Video> findByRecipientIdAndStatusOrderByReadyTimestamp(String recipientId, String status);
}
