package io.github.opendonationassistant.media.youtube;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class PlaylistItemList {

  private String kind;
  private String nextPageToken;
  private java.util.List<PlaylistItem> items;

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getNextPageToken() {
    return nextPageToken;
  }

  public void setNextPageToken(String nextPageToken) {
    this.nextPageToken = nextPageToken;
  }

  public java.util.List<PlaylistItem> getItems() {
    return items;
  }

  public void setItems(java.util.List<PlaylistItem> items) {
    this.items = items;
  }
}
