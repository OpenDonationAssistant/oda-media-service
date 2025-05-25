package io.github.opendonationassistant.media.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.opendonationassistant.media.repository.settings.MediaSettings;
import io.github.opendonationassistant.media.repository.settings.MediaSettingsRepository;
import io.github.opendonationassistant.media.video.NewVideoRequest;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class NewVideoRequestRestModel {

  private Logger log = LoggerFactory.getLogger(NewVideoRequest.class);

  public static final String regex =
    "^((?:https?:)?\\/\\/)?((?:www|m)\\.)?((?:youtube(-nocookie)?\\.com|youtu.be))(\\/(?:[\\w\\-]+\\?v=|embed\\/|live\\/|v\\/)?)([\\w\\-]+)(\\S+)?$";

  private String url;
  private String recipientId;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getRecipientId() {
    return recipientId;
  }

  public void setRecipientId(String recipientId) {
    this.recipientId = recipientId;
  }

  public NewVideoRequest asDomain(MediaSettingsRepository repository) {
    final MediaSettings settings = repository.getByRecipientId(this.recipientId);
    return new NewVideoRequest(url, settings);
  }
}
