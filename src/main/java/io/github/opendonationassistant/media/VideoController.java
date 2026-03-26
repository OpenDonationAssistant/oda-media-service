package io.github.opendonationassistant.media;

import io.github.opendonationassistant.commons.micronaut.BaseController;
import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/media/video")
@Tag(name = "Video", description = "Video management operations")
public class VideoController extends BaseController {

  private Logger log = LoggerFactory.getLogger(VideoController.class);

  private final VideoRepository repository;

  @Inject
  public VideoController(VideoRepository repository) {
    this.repository = repository;
  }

  @Patch("{id}")
  @Operation(
    summary = "Mark video as handled",
    description = "Marks a video as handled by its ID"
  )
  @ApiResponse(responseCode = "204", description = "Video marked as handled")
  @Secured(SecurityRule.IS_ANONYMOUS)
  public void update(
    @Parameter(
      description = "Video ID to mark as handled",
      required = true
    ) @PathVariable String id
  ) {
    log.info("Try to make {} handled", id);
    repository.findReadyVideo(id).ifPresent(ReadyVideo::makeHandled);
  }

  @Get("{ids}")
  @Operation(
    summary = "Get videos by IDs",
    description = "Retrieves video data for a comma-separated list of video IDs"
  )
  @ApiResponse(
    responseCode = "200",
    description = "List of videos retrieved successfully",
    content = @Content(
      mediaType = "application/json",
      schema = @Schema(implementation = VideoData.class)
    )
  )
  @Secured(SecurityRule.IS_ANONYMOUS)
  public List<VideoData> get(
    @Parameter(
      description = "Comma-separated video IDs",
      required = true
    ) @PathVariable String ids
  ) {
    return Arrays.asList(ids.split(","))
      .stream()
      .flatMap(id -> repository.findReadyVideo(id).stream())
      .map(ReadyVideo::data)
      .toList();
  }

  @Get
  @Operation(
    summary = "List authenticated user's videos",
    description = "Returns a list of ready videos for the authenticated recipient"
  )
  @ApiResponse(
    responseCode = "200",
    description = "List of user's videos",
    content = @Content(
      mediaType = "application/json",
      schema = @Schema(implementation = VideoData.class)
    )
  )
  @ApiResponse(
    responseCode = "401",
    description = "Unauthorized - authentication required"
  )
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public CompletableFuture<HttpResponse<List<VideoData>>> list(
    Authentication auth
  ) {
    var ownerId = getOwnerId(auth);
    if (ownerId.isEmpty()) {
      return CompletableFuture.completedFuture(HttpResponse.unauthorized());
    }
    return repository
      .findReadyVideosForRecipientId(ownerId.get())
      .thenApply(videos -> {
        return HttpResponse.ok(videos.stream().map(ReadyVideo::data).toList());
      });
  }
}
