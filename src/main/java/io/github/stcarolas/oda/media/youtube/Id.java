package io.github.stcarolas.oda.media.youtube;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class Id {

  private String kind;
  private String videoId;

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getVideoId() {
    return videoId;
  }

  public void setVideoId(String videoId) {
    this.videoId = videoId;
  }
}
