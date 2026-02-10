package io.github.opendonationassistant.media;

import io.github.opendonationassistant.commons.micronaut.BaseController;
import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.media.repository.VideoDataRepository;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/media/video")
public class VideoController extends BaseController {

  private Logger log = LoggerFactory.getLogger(VideoController.class);

  private final VideoDataRepository repository;

  @Inject
  public VideoController(VideoDataRepository repository) {
    this.repository = repository;
  }

  @Patch("{id}")
  @Secured(SecurityRule.IS_ANONYMOUS)
  public void update(@PathVariable String id) {
    log.info("Try to make {} handled", id);
    Optional<VideoData> video = repository.findById(id);
    video
      // .filter(it -> "ready".equals(it.getStatus()))
      .ifPresent(it -> new ReadyVideo(it, repository).makeHandled());
  }

  @Get("{ids}")
  @Secured(SecurityRule.IS_ANONYMOUS)
  public List<VideoData> get(@PathVariable String ids) {
    return Arrays.asList(ids.split(","))
      .stream()
      .flatMap(id -> repository.findById(id).stream())
      .toList();
  }

  @Get
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public HttpResponse<List<VideoData>> list(Authentication auth) {
    var ownerId = getOwnerId(auth);
    if (ownerId.isEmpty()) {
      return HttpResponse.unauthorized();
    }
    return HttpResponse.ok(
      repository.findByRecipientIdAndStatusOrderByReadyTimestamp(
        ownerId.get(),
        "ready"
      )
    );
  }
}
