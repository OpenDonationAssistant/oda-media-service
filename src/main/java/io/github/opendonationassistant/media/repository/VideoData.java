package io.github.opendonationassistant.media.repository;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.sourcegen.annotations.Wither;
import jakarta.annotation.Nullable;
import java.time.Instant;

@Serdeable
@MappedEntity("video")
@Wither
public record VideoData (
  @Id String id,
  String originId,
  String provider,
  String url,
  String title,
  String thumbnail,
  String status,
  @Nullable String owner,
  @Nullable String recipientId,
  @Nullable Instant readyTimestamp,
  @Nullable String paymentId
) implements VideoDataWither {}
