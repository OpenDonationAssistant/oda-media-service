package io.github.stcarolas.oda.media.playlist;

import io.github.stcarolas.oda.media.playlist.repository.PlaylistData;
import io.github.stcarolas.oda.media.playlist.repository.PlaylistDataRepository;
import io.github.stcarolas.oda.media.playlist.view.PlaylistDto;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nonnull;
import java.util.List;

@Serdeable
public class Playlist {

  private final @Nonnull String id;

  private final @Nonnull String title;

  private final @Nonnull String ownerId;

  private final @Nonnull List<PlaylistItem> items;

  private PlaylistDataRepository repository;

  public Playlist(
    @Nonnull String id,
    @Nonnull String title,
    @Nonnull String ownerId,
    @Nonnull List<PlaylistItem> items,
    PlaylistDataRepository repository
  ) {
    this.id = id;
    this.title = title;
    this.ownerId = ownerId;
    this.items = items;
    this.repository = repository;
  }

  public Playlist update(List<PlaylistItem> items){
    return new Playlist(id, title, ownerId, items, repository);
  }

  public Playlist save() {
    PlaylistData data = new PlaylistData(id, title, ownerId, items);
    repository.update(data);
    return this;
  }

  public PlaylistDto asPlaylistDto(){
    return new PlaylistDto(id, title, ownerId, items);
  }
}
