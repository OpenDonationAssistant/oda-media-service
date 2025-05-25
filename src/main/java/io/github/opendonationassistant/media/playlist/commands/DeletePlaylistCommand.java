package io.github.opendonationassistant.media.playlist.commands;

import io.github.opendonationassistant.media.playlist.Playlist;
import io.github.opendonationassistant.media.playlist.repository.PlaylistRepository;
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

