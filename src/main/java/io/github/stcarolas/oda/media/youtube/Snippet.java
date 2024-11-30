package io.github.stcarolas.oda.media.youtube;

import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;

@Serdeable
public class Snippet {

  private Id resourceId;
  private String title;
  private HashMap<String, Thumbnail> thumbnails;

  public Id getResourceId() {
    return resourceId;
  }

  public void setResourceId(Id resourceId) {
    this.resourceId = resourceId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public HashMap<String, Thumbnail> getThumbnails() {
    return thumbnails;
  }

  public void setThumbnails(HashMap<String, Thumbnail> thumbnails) {
    this.thumbnails = thumbnails;
  }

  @Override
  public String toString() {
    return "{\"_type\"=\"Snippet\",\"resourceId\"=\"" + resourceId + "\", title\"=\"" + title + "\", thumbnails\"=\""
        + thumbnails + "}";
  }
}
