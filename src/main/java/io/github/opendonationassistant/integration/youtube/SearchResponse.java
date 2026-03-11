package io.github.opendonationassistant.integration.youtube;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

import org.jspecify.annotations.Nullable;

@Serdeable
public record SearchResponse(@Nullable String kind, @Nullable List<SearchResult> items) {}
