package io.github.opendonationassistant.media.dto;

import java.util.List;

import io.github.opendonationassistant.integration.youtube.Video;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class Playlist {

  private List<Video> items;
  private String title;

  public List<Video> getItems() {
    return items;
  }

  public void setItems(List<Video> items) {
    this.items = items;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }
}
