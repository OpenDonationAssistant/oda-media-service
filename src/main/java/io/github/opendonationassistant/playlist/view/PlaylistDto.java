package io.github.opendonationassistant.playlist.view;

import io.github.opendonationassistant.playlist.repository.PlaylistData.PlaylistItem;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Serdeable
@Schema(description = "Playlist data transfer object")
public record PlaylistDto(
  @Schema(description = "Unique playlist identifier") String id,
  @Schema(description = "Playlist title") String title,
  @Schema(description = "Owner user ID") String ownerId,
  @Schema(description = "List of playlist items") List<PlaylistItem> items
) {}
