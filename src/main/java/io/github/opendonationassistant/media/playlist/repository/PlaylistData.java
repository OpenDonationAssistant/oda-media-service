package io.github.opendonationassistant.media.playlist.repository;

import io.github.opendonationassistant.media.playlist.Playlist;
import io.github.opendonationassistant.media.playlist.PlaylistItem;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.model.DataType;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
@MappedEntity("playlist")
public class PlaylistData {

  @Id
  private String id;

  private String title;

  private String ownerId;

  @MappedProperty(type = DataType.JSON)
  private List<PlaylistItem> items;

  public PlaylistData(
    String id,
    String title,
    String ownerId,
    List<PlaylistItem> items
  ) {
    this.id = id;
    this.title = title;
    this.ownerId = ownerId;
    this.items = items;
  }

  public Playlist asPlaylist(PlaylistDataRepository dataRepository){
    return new Playlist(id, title, ownerId, items, dataRepository);
  }

  public String getId() {
    return id;
  }

  public List<PlaylistItem> getItems() {
    return items;
  }

  public String getTitle() {
    return title;
  }

  public String getOwnerId() {
    return ownerId;
  }
}
