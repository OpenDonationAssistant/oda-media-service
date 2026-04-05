package io.github.opendonationassistant.media;

import io.github.opendonationassistant.integration.youtube.PlaylistItem;
import io.github.opendonationassistant.integration.youtube.PlaylistItemList;
import io.github.opendonationassistant.integration.youtube.SearchResult;
import io.github.opendonationassistant.integration.youtube.Video;
import io.github.opendonationassistant.integration.youtube.Videos;
import io.github.opendonationassistant.integration.youtube.YouTube;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/media/available")
@Tag(name = "Available Media", description = "Search and retrieve available media from external providers")
public class AvailableMediaController {

  private Logger log = LoggerFactory.getLogger(AvailableMediaController.class);

  private final YouTube youTube;

  public AvailableMediaController(YouTube youTube) {
    this.youTube = youTube;
  }

  @Get
  @Operation(summary = "Get available media", description = "Retrieves available videos from YouTube based on query, playlist ID, or video ID. At least one of query, playlistId, or videoId must be provided.")
  @ApiResponse(responseCode = "200", description = "List of available videos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Video.class)))
  @ApiResponse(responseCode = "500", description = "Internal server error or video not found")
  @Secured(SecurityRule.IS_ANONYMOUS)
  @ExecuteOn(TaskExecutors.BLOCKING)
  public HttpResponse<java.util.List<Video>> available(
    @Parameter(description = "Search query for YouTube videos") @Nullable @QueryValue("query") String query,
    @Parameter(description = "YouTube playlist ID to retrieve videos from") @Nullable @QueryValue("playlistId") String playlistId,
    @Parameter(description = "Specific YouTube video ID") @Nullable @QueryValue("videoId") String videoId
  ) {
    List<Video> allVideos = new ArrayList<>();
    if (Objects.nonNull(query)) {
      List<SearchResult> results = Optional.ofNullable(
        youTube.search(query).items()
      ).orElse(List.of());
      log.debug("search results: {}", results);
      allVideos.addAll(
        results
          .stream()
          .map(result ->
            new Video(
              Optional.ofNullable(result.id())
                .map(it -> it.videoId())
                .orElseThrow(),
              result.snippet(),
              null,
              null
            )
          )
          .toList()
      );
    }
    if (Objects.nonNull(playlistId)) {
      log.debug("Getting items from playlist with id : {}", playlistId);
      PlaylistItemList page = youTube.playlistItems(playlistId, "");
      List<PlaylistItem> videos = Optional.ofNullable(page.items()).orElse(
        new ArrayList<>()
      );
      String nextPage = page.nextPageToken();
      while (Objects.nonNull(nextPage)) {
        log.debug(
          "Getting page of items from playlist with id : {} and page token: {}",
          playlistId,
          nextPage
        );
        page = youTube.playlistItems(playlistId, nextPage);
        nextPage = page.nextPageToken();
        videos.addAll(page.items());
      }
      log.debug("Playlist videos: {}", videos);
      allVideos.addAll(
        videos
          .stream()
          .map(item ->
            new Video(
              Optional.ofNullable(item.snippet())
                .map(it -> it.resourceId())
                .map(it -> it.videoId())
                .orElseThrow(),
              item.snippet(),
              null,
              null
            )
          )
          .toList()
      );
    }
    if (Objects.nonNull(videoId)) {
      Videos found = youTube.list(videoId).join();
      if (found.items() == null || found.items().isEmpty()) {
        throw new RuntimeException("Видео не найдено");
      }
      Video video = found.items().iterator().next();
      if (found.items() == null || found.items().isEmpty()) {
        throw new RuntimeException("Видео не найдено");
      }
      allVideos.add(video);
    }
    log.debug("total available media: {}", allVideos);
    return HttpResponse.ok(allVideos);
  }
}
