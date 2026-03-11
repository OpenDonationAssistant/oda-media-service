package io.github.opendonationassistant.integration.youtube;

import org.jspecify.annotations.Nullable;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record SearchResult(@Nullable Id id, @Nullable String kind, @Nullable String etag, @Nullable Snippet snippet) {}
