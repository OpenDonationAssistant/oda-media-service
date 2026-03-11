package io.github.opendonationassistant.integration.youtube;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Serdeable
public record PlaylistItemList(
  @Nullable String kind,
  @Nullable String nextPageToken,
  @Nullable List<PlaylistItem> items
) {}
