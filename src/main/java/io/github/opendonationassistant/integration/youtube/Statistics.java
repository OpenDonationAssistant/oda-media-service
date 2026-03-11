package io.github.opendonationassistant.integration.youtube;

import org.jspecify.annotations.Nullable;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record Statistics(@Nullable String viewCount) {}
