package io.github.opendonationassistant.media.repository;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.sourcegen.annotations.Wither;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

@Serdeable
@MappedEntity("video")
@Wither
@Schema(description = "Video data entity")
public record VideoData (
  @Id @Schema(description = "Unique video identifier") String id,
  @Schema(description = "Original video ID from provider") String originId,
  @Schema(description = "Video provider (e.g., youtube)") String provider,
  @Schema(description = "Video URL") String url,
  @Schema(description = "Video title") String title,
  @Schema(description = "Video thumbnail URL") String thumbnail,
  @Schema(description = "Video status") String status,
  @Nullable @Schema(description = "Owner of the video") String owner,
  @Nullable @Schema(description = "Recipient ID associated with this video") String recipientId,
  @Nullable @Schema(description = "Timestamp when video is ready") Instant readyTimestamp,
  @Nullable @Schema(description = "Payment ID associated with this video") String paymentId
) implements VideoDataWither {}
