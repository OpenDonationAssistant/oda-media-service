package io.github.stcarolas.oda.media.playlist.commands;

import io.github.stcarolas.oda.media.playlist.Playlist;
import io.github.stcarolas.oda.media.playlist.repository.PlaylistRepository;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class DeletePlaylistCommand {

  private final String id;

  public DeletePlaylistCommand(String id) {
    this.id = id;
  }

  public void execute(PlaylistRepository repository) {
    repository.get(id).ifPresent(Playlist::delete);
  }
}

