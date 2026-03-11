package io.github.opendonationassistant.integration.youtube;

import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;
import org.jspecify.annotations.Nullable;

@Serdeable
public record ContentDetails(
  @Nullable String duration,
  @Nullable RegionRestriction regionRestriction,
  @Nullable HashMap<String, String> contentRating
) {}
