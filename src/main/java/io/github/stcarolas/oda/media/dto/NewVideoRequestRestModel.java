package io.github.stcarolas.oda.media.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.stcarolas.oda.media.video.NewVideoRequest;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class NewVideoRequestRestModel {

  private Logger log = LoggerFactory.getLogger(NewVideoRequest.class);

  public static final String regex =
    "^((?:https?:)?\\/\\/)?((?:www|m)\\.)?((?:youtube(-nocookie)?\\.com|youtu.be))(\\/(?:[\\w\\-]+\\?v=|embed\\/|live\\/|v\\/)?)([\\w\\-]+)(\\S+)?$";

  private String url;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public NewVideoRequest asDomain() {
    return new NewVideoRequest(url);
  }
}
