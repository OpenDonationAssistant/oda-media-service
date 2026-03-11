package io.github.opendonationassistant.integration.youtube;

import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;

import org.jspecify.annotations.Nullable;

@Serdeable
public record Snippet(@Nullable Id resourceId, @Nullable String title, @Nullable HashMap<String, Thumbnail> thumbnails) {}
