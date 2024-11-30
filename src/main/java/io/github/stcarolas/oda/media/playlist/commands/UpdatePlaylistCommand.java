package io.github.stcarolas.oda.media.playlist.commands;

import io.github.stcarolas.oda.media.playlist.Playlist;
import io.github.stcarolas.oda.media.playlist.PlaylistItem;
import io.github.stcarolas.oda.media.playlist.repository.PlaylistRepository;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;
import java.util.Optional;

@Serdeable
public class UpdatePlaylistCommand {

  private final String id;
  private final List<PlaylistItem> items;

  public UpdatePlaylistCommand(String id, List<PlaylistItem> items) {
    this.id = id;
    this.items = items;
  }

  public Optional<Playlist> execute(PlaylistRepository repository) {
    final Optional<Playlist> playlist = repository.get(id);
    return playlist.map(p -> p.update(items).save());
  }
}
