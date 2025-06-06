package io.github.opendonationassistant.media.playlist.commands;

import io.github.opendonationassistant.media.playlist.Playlist;
import io.github.opendonationassistant.media.playlist.repository.PlaylistRepository;
import io.github.opendonationassistant.media.playlist.view.PlaylistDto;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.annotation.Nonnull;

@Controller
public class PlaylistCommandController {

  private final PlaylistRepository playlistRepository;

  @Post("/playlists/commands/create")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public PlaylistDto create(
    @Nonnull Authentication auth,
    @Nonnull @Body CreatePlaylistCommand command
  ) {
    return command
      .execute(getOwnerId(auth), playlistRepository)
      .asPlaylistDto();
  }

  @Post("/playlists/commands/update")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public HttpResponse<PlaylistDto> update(
    @Nonnull Authentication auth,
    @Nonnull @Body UpdatePlaylistCommand command
  ) {
    return command.execute(playlistRepository)
      .map(Playlist::asPlaylistDto)
      .map(HttpResponse::ok)
      .orElse(HttpResponse.notFound());
  }

  @Post("/playlists/commands/delete")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public HttpResponse<PlaylistDto> deletePlaylist(
    @Nonnull Authentication auth,
    @Nonnull @Body DeletePlaylistCommand command
  ) {
    try {
      command.execute(playlistRepository);
      return HttpResponse.ok();
    } catch (Exception e){
      return HttpResponse.serverError();
    }
  }

  protected String getOwnerId(Authentication auth) {
    return String.valueOf(
      auth.getAttributes().getOrDefault("preferred_username", "")
    );
  }

  public PlaylistCommandController(PlaylistRepository playlistRepository) {
    this.playlistRepository = playlistRepository;
  }
}
