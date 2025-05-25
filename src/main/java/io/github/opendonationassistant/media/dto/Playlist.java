package io.github.opendonationassistant.media.dto;

import io.github.opendonationassistant.media.youtube.Video;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class Playlist {

  private java.util.List<Video> items;
  private String title;

  public java.util.List<Video> getItems() {
    return items;
  }

  public void setItems(java.util.List<Video> items) {
    this.items = items;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }
}
