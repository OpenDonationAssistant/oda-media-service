package io.github.opendonationassistant.media.playlist.commands;

import io.github.opendonationassistant.media.playlist.Playlist;
import io.github.opendonationassistant.media.playlist.PlaylistItem;
import io.github.opendonationassistant.media.playlist.repository.PlaylistRepository;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
public class CreatePlaylistCommand {

  private final String title;
  private final List<PlaylistItem> items;

  public CreatePlaylistCommand(String title, List<PlaylistItem> items) {
    this.title = title;
    this.items = items;
  }

  public Playlist execute(String ownerId, PlaylistRepository repository) {
    return repository.create(title, ownerId, items);
  }
}
