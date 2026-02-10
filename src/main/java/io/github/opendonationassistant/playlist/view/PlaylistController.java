package io.github.opendonationassistant.playlist.view;

import io.github.opendonationassistant.commons.micronaut.BaseController;
import io.github.opendonationassistant.integration.youtube.PlaylistItem;
import io.github.opendonationassistant.integration.youtube.PlaylistItemList;
import io.github.opendonationassistant.integration.youtube.Video;
import io.github.opendonationassistant.integration.youtube.YouTube;
import io.github.opendonationassistant.media.dto.Playlist;
import io.github.opendonationassistant.playlist.repository.PlaylistRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class PlaylistController extends BaseController {

  private Logger log = LoggerFactory.getLogger(PlaylistController.class);

  private final YouTube youTube;
  private final PlaylistRepository playlistRepository;

  public PlaylistController(
    YouTube youTube,
    PlaylistRepository playlistRepository
  ) {
    this.youTube = youTube;
    this.playlistRepository = playlistRepository;
  }

  @Get("/playlists")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public HttpResponse<Page<PlaylistDto>> playlists(
    Authentication auth,
    Pageable pageable
  ) {
    var ownerId = getOwnerId(auth);
    if (ownerId.isEmpty()) {
      return HttpResponse.unauthorized();
    }
    return HttpResponse.ok(
      playlistRepository
        .list(ownerId.get(), pageable)
        .map(playlist -> playlist.asPlaylistDto())
    );
  }

  @Get("/playlists/{playlistId}")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public HttpResponse<PlaylistDto> playlist(
    Authentication auth,
    @PathVariable String playlistId
  ) {
    var ownerId = getOwnerId(auth);
    if (ownerId.isEmpty()) {
      return HttpResponse.unauthorized();
    }
    return playlistRepository
      .get(playlistId)
      .map(playlist -> playlist.asPlaylistDto())
      .map(HttpResponse::ok)
      .orElse(HttpResponse.notFound());
  }

  @Get("/media/playlists/{playlistId}")
  @Secured(SecurityRule.IS_ANONYMOUS)
  public HttpResponse<Playlist> get(@PathVariable String playlistId) {
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
    List<Video> mappedVideos = new ArrayList<>(
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
    Collections.shuffle(mappedVideos);
    log.debug("Mapped videos: {}", mappedVideos);
    String title = youTube
      .playlistInfo(playlistId)
      .getItems()
      .get(0)
      .getSnippet()
      .getTitle();
    var playlist = new Playlist();
    playlist.setTitle(title);
    playlist.setItems(mappedVideos);
    log.debug("Playlist: {}", playlist);
    return HttpResponse.ok(playlist);
  }
}
