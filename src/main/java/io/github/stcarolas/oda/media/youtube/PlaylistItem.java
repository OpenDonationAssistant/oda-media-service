package io.github.stcarolas.oda.media.youtube;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class PlaylistItem {

  private String kind;
  private String etag;
  private String id;
  private Snippet snippet;

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getEtag() {
    return etag;
  }

  public void setEtag(String etag) {
    this.etag = etag;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Snippet getSnippet() {
    return snippet;
  }

  public void setSnippet(Snippet snippet) {
    this.snippet = snippet;
  }
}
