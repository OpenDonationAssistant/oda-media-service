package io.github.opendonationassistant.playlist.commands;

import io.github.opendonationassistant.commons.micronaut.BaseController;
import io.github.opendonationassistant.playlist.repository.PlaylistRepository;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

@Controller
public class DeletePlaylist extends BaseController {

  private final PlaylistRepository repository;

  @Inject
  public DeletePlaylist(PlaylistRepository repository) {
    this.repository = repository;
  }

  @Post("/playlists/commands/delete")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public HttpResponse<Void> deletePlaylist(
    Authentication auth,
    @Body DeletePlaylistCommand command
  ) {
    var ownerId = getOwnerId(auth);
    if (ownerId.isEmpty()) {
      return HttpResponse.unauthorized();
    }
    repository
      .get(command.id())
      .filter(p -> ownerId.get().equals(ownerId.get()))
      .ifPresent(p -> p.delete());
    return HttpResponse.ok();
  }

  @Serdeable
  public static record DeletePlaylistCommand(String id) {}
}
