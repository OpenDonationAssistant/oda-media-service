package io.github.stcarolas.oda.media.youtube;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class Video {

  private String id;
  private Snippet snippet;
  private ContentDetails contentDetails;
  private Statistics statistics;

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

  public ContentDetails getContentDetails() {
    return contentDetails;
  }

  public void setContentDetails(ContentDetails contentDetails) {
    this.contentDetails = contentDetails;
  }

  public Statistics getStatistics() {
    return statistics;
  }

  public void setStatistics(Statistics statistics) {
    this.statistics = statistics;
  }

  @Override
  public String toString() {
    return "{\"_type\"=\"Video\",\"id\"=\"" + id + "\", snippet\"=\"" + snippet + "\", contentDetails\"=\""
        + contentDetails + "\", statistics\"=\"" + statistics + "}";
  }
}
