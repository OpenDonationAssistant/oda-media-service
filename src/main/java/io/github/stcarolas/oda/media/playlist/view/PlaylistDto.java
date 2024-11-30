package io.github.stcarolas.oda.media.playlist.view;

import io.github.stcarolas.oda.media.playlist.PlaylistItem;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
public record PlaylistDto(
  String id,
  String title,
  String ownerId,
  List<PlaylistItem> items
) {}
