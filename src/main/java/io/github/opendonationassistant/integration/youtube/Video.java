package io.github.opendonationassistant.integration.youtube;

import io.micronaut.serde.annotation.Serdeable;
import org.jspecify.annotations.Nullable;

@Serdeable
public record Video(
  @Nullable String id,
  @Nullable Snippet snippet,
  @Nullable ContentDetails contentDetails,
  @Nullable Statistics statistics
) {}
