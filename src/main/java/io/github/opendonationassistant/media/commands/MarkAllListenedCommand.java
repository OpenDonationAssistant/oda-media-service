package io.github.opendonationassistant.media.commands;

import io.github.opendonationassistant.media.video.VideoRepository;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class MarkAllListenedCommand {

  public void execute(Authentication auth, VideoRepository repository) {
    repository
      .findByRecipientIdAndStatusOrderByReadyTimestamp(
        getOwnerId(auth),
        "ready"
      )
      .stream()
      .map(ReadyVideo::from)
      .forEach(ReadyVideo::makeHandled);
  }

  private String getOwnerId(Authentication auth) {
    return String.valueOf(
      auth.getAttributes().getOrDefault("preferred_username", "")
    );
  }
}
