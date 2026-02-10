package io.github.opendonationassistant.integration.youtube;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class Thumbnail {

  private String url;
  private String width;
  private String height;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getWidth() {
    return width;
  }

  public void setWidth(String width) {
    this.width = width;
  }

  public String getHeight() {
    return height;
  }

  public void setHeight(String height) {
    this.height = height;
  }
}
