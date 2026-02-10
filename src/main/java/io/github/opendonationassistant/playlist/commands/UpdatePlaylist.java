package io.github.opendonationassistant.playlist.commands;

import io.github.opendonationassistant.commons.micronaut.BaseController;
import io.github.opendonationassistant.playlist.Playlist;
import io.github.opendonationassistant.playlist.repository.PlaylistData.PlaylistItem;
import io.github.opendonationassistant.playlist.repository.PlaylistRepository;
import io.github.opendonationassistant.playlist.view.PlaylistDto;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.util.List;

@Serdeable
public class UpdatePlaylist extends BaseController {

  private final PlaylistRepository repository;

  @Inject
  public UpdatePlaylist(PlaylistRepository repository) {
    this.repository = repository;
  }

  @Post("/playlists/commands/update")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public HttpResponse<PlaylistDto> update(
    Authentication auth,
    @Body UpdatePlaylistCommand command
  ) {
    var ownerId = getOwnerId(auth);
    if (ownerId.isEmpty()) {
      return HttpResponse.unauthorized();
    }
    // todo check owner id
    return repository
      .get(command.id())
      .map(playlist -> playlist.update(command.items()))
      .map(Playlist::asPlaylistDto)
      .map(HttpResponse::ok)
      .orElse(HttpResponse.unauthorized());
  }

  @Serdeable
  public static record UpdatePlaylistCommand(
    String id,
    List<PlaylistItem> items
  ) {}
}
