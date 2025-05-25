package io.github.opendonationassistant.media.youtube;

import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;

@Serdeable
public class ContentDetails {

  private String duration;
  private RegionRestriction regionRestriction;
  private HashMap<String, String> contentRating;

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public RegionRestriction getRegionRestriction() {
    return regionRestriction;
  }

  public void setRegionRestriction(RegionRestriction regionRestriction) {
    this.regionRestriction = regionRestriction;
  }

  public HashMap<String, String> getContentRating() {
    return contentRating;
  }

  public void setContentRating(HashMap<String, String> contentRating) {
    this.contentRating = contentRating;
  }

  @Override
  public String toString() {
    return "{\"_type\"=\"ContentDetails\",\"duration\"=\"" + duration + "\", regionRestriction\"=\"" + regionRestriction
        + "\", contentRating\"=\"" + contentRating + "}";
  }
}
