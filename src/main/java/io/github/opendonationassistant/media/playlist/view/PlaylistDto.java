package io.github.opendonationassistant.media.playlist.view;

import io.github.opendonationassistant.media.playlist.PlaylistItem;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
public record PlaylistDto(
  String id,
  String title,
  String ownerId,
  List<PlaylistItem> items
) {}
