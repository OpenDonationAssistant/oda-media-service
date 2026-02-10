package io.github.opendonationassistant.playlist.view;

import io.github.opendonationassistant.playlist.repository.PlaylistData.PlaylistItem;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
public record PlaylistDto(
  String id,
  String title,
  String ownerId,
  List<PlaylistItem> items
) {}
