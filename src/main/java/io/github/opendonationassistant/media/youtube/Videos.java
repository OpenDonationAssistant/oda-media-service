package io.github.opendonationassistant.media.youtube;

import java.util.List;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class Videos {

  List<Video> items;

  public java.util.List<Video> getItems() {
    return items;
  }

  public void setItems(java.util.List<Video> items) {
    this.items = items;
  }
}
