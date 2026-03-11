package io.github.opendonationassistant.media.dto;

import io.github.opendonationassistant.integration.youtube.Video;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
public record Playlist(List<Video> items, String title) {}
