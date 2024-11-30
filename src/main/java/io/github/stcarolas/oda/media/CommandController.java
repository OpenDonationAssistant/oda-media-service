package io.github.stcarolas.oda.media;

import io.github.stcarolas.oda.media.commands.MarkAllListenedCommand;
import io.github.stcarolas.oda.media.commands.PrepareVideoCommand;
import io.github.stcarolas.oda.media.video.VideoRepository;
import io.github.stcarolas.oda.media.video.prepared.PreparedVideo;
import io.github.stcarolas.oda.media.youtube.YouTube;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.annotation.Nonnull;

@Controller("/commands/media")
public class CommandController {

  private final YouTube youTube;
  private final VideoRepository videoRepository;

  public CommandController(YouTube youTube, VideoRepository videoRepository) {
    this.youTube = youTube;
    this.videoRepository = videoRepository;
  }

  @Post("prepare")
  public PreparedVideo prepare(@Body PrepareVideoCommand command) {
    return command.execute(youTube);
  }

  @Post("markAllListened")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public void markAllListened(
    @Nonnull Authentication auth,
    @Body MarkAllListenedCommand command
  ) {
    command.execute(auth, videoRepository);
  }
}
