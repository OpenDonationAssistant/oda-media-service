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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/media/available")
public class AvailableMediaController {

  private Logger log = LoggerFactory.getLogger(AvailableMediaController.class);

  private final YouTube youTube;

  public AvailableMediaController(YouTube youTube) {
    this.youTube = youTube;
  }

  @Get
  @Secured(SecurityRule.IS_ANONYMOUS)
  @ExecuteOn(TaskExecutors.BLOCKING)
  public HttpResponse<java.util.List<Video>> available(
    @Nullable @QueryValue("query") String query,
    @Nullable @QueryValue("playlistId") String playlistId,
    @Nullable @QueryValue("videoId") String videoId
  ) {
    List<Video> allVideos = new ArrayList<>();
    if (Objects.nonNull(query)) {
      List<SearchResult> results = youTube.search(query).getItems();
      log.debug("search results: {}", results);
      allVideos.addAll(
        results
          .stream()
          .map(result -> {
            var video = new Video();
            video.setId(result.getId().getVideoId());
            video.setSnippet(result.getSnippet());
            return video;
          })
          .toList()
      );
    }
    if (Objects.nonNull(playlistId)) {
      log.debug("Getting items from playlist with id : {}", playlistId);
      PlaylistItemList page = youTube.playlistItems(playlistId, "");
      List<PlaylistItem> videos = page.getItems();
      String nextPage = page.getNextPageToken();
      while (Objects.nonNull(nextPage)) {
        log.debug(
          "Getting page of items from playlist with id : {} and page token: {}",
          playlistId,
          nextPage
        );
        page = youTube.playlistItems(playlistId, nextPage);
        nextPage = page.getNextPageToken();
        videos.addAll(page.getItems());
      }
      log.debug("Playlist videos: {}", videos);
      allVideos.addAll(
        videos
          .stream()
          .map(item -> {
            Video video = new Video();
            video.setId(item.getSnippet().getResourceId().getVideoId());
            video.setSnippet(item.getSnippet());
            return video;
          })
          .toList()
      );
    }
    if (Objects.nonNull(videoId)) {
      Videos found = youTube.list(videoId);
      if (found.getItems() == null || found.getItems().isEmpty()) {
        throw new RuntimeException("Видео не найдено");
      }
      Video video = found.getItems().iterator().next();
      if (found.getItems() == null || found.getItems().isEmpty()) {
        throw new RuntimeException("Видео не найдено");
      }
      allVideos.add(video);
    }
    log.debug("total available media: {}", allVideos);
    return HttpResponse.ok(allVideos);
  }
}
