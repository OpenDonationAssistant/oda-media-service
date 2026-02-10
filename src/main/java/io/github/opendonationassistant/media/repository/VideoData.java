package io.github.opendonationassistant.media.repository;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import java.time.Instant;

@Serdeable
@MappedEntity("video")
public record VideoData(
  @Id String id,
  String originId,
  String provider,
  String url,
  String title,
  String thumbnail,
  String status,
  @Nullable String owner,
  @Nullable String recipientId,
  @Nullable Instant readyTimestamp
) {
  public VideoData withStatus(String status) {
    return new VideoData(
      id,
      originId,
      provider,
      url,
      title,
      thumbnail,
      owner,
      recipientId,
      status,
      Instant.now()
    );
  }
  public VideoData withRecipientId(String recipientId) {
    return new VideoData(
      id,
      originId,
      provider,
      url,
      title,
      thumbnail,
      owner,
      recipientId,
      status,
      readyTimestamp
    );
  }
  public VideoData withOwner(String owner) {
    return new VideoData(
      id,
      originId,
      provider,
      url,
      title,
      thumbnail,
      owner,
      recipientId,
      status,
      readyTimestamp
    );
  }
  public VideoData withReadyTimestamp(Instant readyTimestamp) {
    return new VideoData(
      id,
      originId,
      provider,
      url,
      title,
      thumbnail,
      owner,
      recipientId,
      status,
      readyTimestamp
    );
  }
}
