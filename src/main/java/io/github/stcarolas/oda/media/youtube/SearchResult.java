package io.github.stcarolas.oda.media.youtube;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class SearchResult {

  private Id id;
  private String kind;
  private String etag;
  private Snippet snippet;

  public Id getId() {
    return id;
  }

  public void setId(Id id) {
    this.id = id;
  }

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

  public Snippet getSnippet() {
    return snippet;
  }

  public void setSnippet(Snippet snippet) {
    this.snippet = snippet;
  }
}
