package io.github.opendonationassistant.media.dto;

import io.github.opendonationassistant.integration.youtube.Video;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Serdeable
@Schema(description = "Playlist containing videos")
public record Playlist(
  @Schema(description = "List of videos in the playlist") List<Video> items,
  @Schema(description = "Playlist title") String title
) {}
