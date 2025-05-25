package io.github.stcarolas.oda.media;

import io.github.stcarolas.oda.media.dto.NewVideoRequestRestModel;
import io.github.stcarolas.oda.media.repository.settings.MediaSettingsRepository;
import io.github.stcarolas.oda.media.video.Video;
import io.github.stcarolas.oda.media.video.VideoRepository;
import io.github.stcarolas.oda.media.video.prepared.PreparedVideo;
import io.github.stcarolas.oda.media.video.ready.ReadyVideo;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Put;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/media/video")
public class VideoController {

  private Logger log = LoggerFactory.getLogger(VideoController.class);

  @Inject
  VideoRepository repository;

  @Inject
  MediaSettingsRepository settings;

  @Put
  @Secured(SecurityRule.IS_ANONYMOUS)
  public PreparedVideo add(@Body NewVideoRequestRestModel request) {
    PreparedVideo prepared = request.asDomain(settings).prepare();
    prepared.save();
    return prepared;
  }

  @Patch("{id}")
  @Secured(SecurityRule.IS_ANONYMOUS)
  public void update(@PathVariable String id) {
    log.info("Try to make {} handled", id);
    Optional<Video> video = repository.findById(id);
    video
      // .filter(it -> "ready".equals(it.getStatus()))
      .ifPresent(it -> ReadyVideo.from(it, it.getReadyTimestamp()).makeHandled());
  }

  @Get("{ids}")
  @Secured(SecurityRule.IS_ANONYMOUS)
  public List<Video> get(@NonNull @PathVariable String ids) {
    List<Video> videos = new ArrayList<>();
    Arrays
      .asList(ids.split(","))
      .forEach(id ->
        repository
          .findById(id)
          .map(PreparedVideo::from)
          .ifPresent(video -> videos.add(video))
      );
    return videos;
  }

  @Get
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public List<Video> list(Authentication auth) {
    return repository.findByRecipientIdAndStatusOrderByReadyTimestamp(
      getOwnerId(auth),
      "ready"
    );
  }

  private String getOwnerId(Authentication auth) {
    return String.valueOf(
      auth.getAttributes().getOrDefault("preferred_username", "")
    );
  }
}
