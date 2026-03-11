package io.github.opendonationassistant.integration.youtube;

import org.jspecify.annotations.Nullable;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record PlaylistItem(
  @Nullable String kind,
  @Nullable String etag,
  @Nullable String id,
  @Nullable Snippet snippet
) {}
