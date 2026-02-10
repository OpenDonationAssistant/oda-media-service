package io.github.opendonationassistant.integration.youtube;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class Statistics {

  private String viewCount;

  public String getViewCount() {
    return viewCount;
  }

  public void setViewCount(String viewCount) {
    this.viewCount = viewCount;
  }

  @Override
  public String toString() {
    return "{\"_type\"=\"Statistics\",\"viewCount\"=\"" + viewCount + "}";
  }
}
