package io.github.opendonationassistant.integration.youtube;

import java.util.List;

import org.jspecify.annotations.Nullable;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record Videos(@Nullable List<Video> items) {}
