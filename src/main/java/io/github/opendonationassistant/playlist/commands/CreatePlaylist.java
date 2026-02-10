package io.github.opendonationassistant.playlist.commands;

import io.github.opendonationassistant.commons.micronaut.BaseController;
import io.github.opendonationassistant.playlist.repository.PlaylistData.PlaylistItem;
import io.github.opendonationassistant.playlist.repository.PlaylistRepository;
import io.github.opendonationassistant.playlist.view.PlaylistDto;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.util.List;

@Controller
public class CreatePlaylist extends BaseController {

  private final PlaylistRepository repository;

  @Inject
  public CreatePlaylist(PlaylistRepository repository) {
    this.repository = repository;
  }

  @Post("/playlists/commands/create")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public HttpResponse<PlaylistDto> create(
    Authentication auth,
    @Body CreatePlaylistCommand command
  ) {
    var ownerId = getOwnerId(auth);
    if (ownerId.isEmpty()) {
      return HttpResponse.unauthorized();
    }
    return HttpResponse.ok(
      repository
        .create(command.title(), ownerId.get(), command.items())
        .asPlaylistDto()
    );
  }

  @Serdeable
  public static record CreatePlaylistCommand(
    String title,
    List<PlaylistItem> items
  ) {}
}
