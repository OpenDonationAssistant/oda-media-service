package io.github.stcarolas.oda.media.youtube;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class SearchResponse {

  public String kind;
  public java.util.List<SearchResult> items;

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public java.util.List<SearchResult> getItems() {
    return items;
  }

  public void setItems(java.util.List<SearchResult> items) {
    this.items = items;
  }
}
