package io.github.opendonationassistant.playlist.view;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.commons.micronaut.BaseController;
import io.github.opendonationassistant.integration.youtube.PlaylistItem;
import io.github.opendonationassistant.integration.youtube.PlaylistItemList;
import io.github.opendonationassistant.integration.youtube.Snippet;
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
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class PlaylistController extends BaseController {

  private ODALogger log = new ODALogger(this);

  private final YouTube youTube;
  private final PlaylistRepository playlistRepository;

  @Inject
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
    log.debug("Getting items from playlist", Map.of("playlistId", playlistId));
    PlaylistItemList page = youTube.playlistItems(playlistId, "");
    List<PlaylistItem> videos = Optional.ofNullable(page.items()).orElse(
      new ArrayList<>()
    );
    String nextPage = page.nextPageToken();
    while (Objects.nonNull(nextPage)) {
      log.debug(
        "Getting page of items from playlist",
        Map.of("playlistId", playlistId, "nextPage", nextPage)
      );
      page = youTube.playlistItems(playlistId, nextPage);
      nextPage = page.nextPageToken();
      videos.addAll(page.items());
    }
    log.debug("Loaded playlist", Map.of("videos", videos));
    List<Video> mappedVideos = new ArrayList<>(
      videos
        .stream()
        .map(item -> item.snippet())
        .map(snippet -> {
          String videoId = Optional.ofNullable(snippet.resourceId())
            .map(resourceId -> resourceId.videoId())
            .orElseThrow();
          return new Video(videoId, snippet, null, null);
        })
        .toList()
    );
    Collections.shuffle(mappedVideos);
    log.debug("Mapped videos", Map.of("videos", mappedVideos));
    String title = Optional.ofNullable(youTube.playlistInfo(playlistId).items())
      .map(items -> items.getFirst())
      .map(PlaylistItem::snippet)
      .map(Snippet::title)
      .orElse("Playlist");
    var playlist = new Playlist(mappedVideos, title);
    log.debug("Playlist", Map.of("playlist", playlist));
    return HttpResponse.ok(playlist);
  }
}
