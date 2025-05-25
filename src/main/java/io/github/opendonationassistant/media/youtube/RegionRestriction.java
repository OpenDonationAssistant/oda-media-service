package io.github.opendonationassistant.media.youtube;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
public class RegionRestriction {

  private List<String> blocked;

  public List<String> getBlocked() {
    return blocked;
  }

  public void setBlocked(List<String> blocked) {
    this.blocked = blocked;
  }

  @Override
  public String toString() {
    return "{\"_type\"=\"RegionRestriction\",\"blocked\"=\"" + blocked + "}";
  }
}
