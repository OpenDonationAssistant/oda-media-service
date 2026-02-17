package io.github.opendonationassistant.media.commands;

import io.github.opendonationassistant.commons.micronaut.BaseController;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.util.concurrent.CompletableFuture;

@Controller
public class MarkAllListened extends BaseController {

  private final VideoRepository repository;

  @Inject
  public MarkAllListened(VideoRepository repository) {
    this.repository = repository;
  }

  @Post("/commands/media/markAllListened")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public CompletableFuture<HttpResponse<Void>> markAllListened(
    Authentication auth,
    @Body MarkAllListenedCommand command
  ) {
    var ownerId = getOwnerId(auth);
    if (ownerId.isEmpty()) {
      return CompletableFuture.completedFuture(HttpResponse.unauthorized());
    }
    return repository
      .findReadyVideosForRecipientId(ownerId.get())
      .thenApply(videos -> {
        videos.forEach(ReadyVideo::makeHandled);
        return HttpResponse.ok();
      });
  }

  @Serdeable
  public static record MarkAllListenedCommand() {}
}
