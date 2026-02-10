package io.github.opendonationassistant.playlist;

import io.github.opendonationassistant.playlist.repository.PlaylistData;
import io.github.opendonationassistant.playlist.repository.PlaylistData.PlaylistItem;
import io.github.opendonationassistant.playlist.repository.PlaylistDataRepository;
import io.github.opendonationassistant.playlist.view.PlaylistDto;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
public class Playlist {

  private final String id;

  private final String title;

  private final String ownerId;

  private final List<PlaylistItem> items;

  private PlaylistDataRepository repository;

  public Playlist(
    String id,
    String title,
    String ownerId,
    List<PlaylistItem> items,
    PlaylistDataRepository repository
  ) {
    this.id = id;
    this.title = title;
    this.ownerId = ownerId;
    this.items = items;
    this.repository = repository;
  }

  public Playlist update(List<PlaylistItem> items) {
    return new Playlist(id, title, ownerId, items, repository).save();
  }

  public Playlist save() {
    PlaylistData data = new PlaylistData(id, title, ownerId, items);
    repository.update(data);
    return this;
  }

  public void delete() {
    repository.deleteById(id);
  }

  public PlaylistDto asPlaylistDto() {
    return new PlaylistDto(id, title, ownerId, items);
  }
}
