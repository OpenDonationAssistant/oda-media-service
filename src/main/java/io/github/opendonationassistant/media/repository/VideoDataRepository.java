package io.github.opendonationassistant.media.repository;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface VideoDataRepository extends CrudRepository<VideoData, String> {
  CompletableFuture<
    List<VideoData>
  > findByRecipientIdAndStatusOrderByReadyTimestamp(
    String recipientId,
    String status
  );

  CompletableFuture<
    Page<VideoData>
  > findByRecipientIdAndStatusOrderByReadyTimestamp(
    String recipientId,
    String status,
    Pageable pageable
  );

  CompletableFuture<Long> countByRecipientIdAndStatus(
    String recipientId,
    String status
  );

  List<VideoData> findByPaymentId(String paymentId);
}
