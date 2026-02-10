package io.github.opendonationassistant.media.commands;

import io.github.opendonationassistant.commons.micronaut.BaseController;
import io.github.opendonationassistant.media.repository.VideoDataRepository;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

@Controller
public class MarkAllListened extends BaseController {

  private final VideoDataRepository repository;

  @Inject
  public MarkAllListened(VideoDataRepository repository) {
    this.repository = repository;
  }

  @Post("/commands/media/markAllListened")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public HttpResponse<Void> markAllListened(
    Authentication auth,
    @Body MarkAllListenedCommand command
  ) {
    var ownerId = getOwnerId(auth);
    if (ownerId.isEmpty()) {
      return HttpResponse.unauthorized();
    }
    repository
      .findByRecipientIdAndStatusOrderByReadyTimestamp(ownerId.get(), "ready")
      .stream();
    // .map(ReadyVideo)
    // .forEach(ReadyVideo::makeHandled);
    return HttpResponse.ok();
  }

  @Serdeable
  public static record MarkAllListenedCommand() {}
}
