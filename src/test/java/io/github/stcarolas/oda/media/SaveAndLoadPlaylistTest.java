package io.github.stcarolas.oda.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.stcarolas.oda.media.playlist.PlaylistItem;
import io.github.stcarolas.oda.media.playlist.commands.CreatePlaylistCommand;
import io.github.stcarolas.oda.media.playlist.commands.PlaylistCommandController;
import io.github.stcarolas.oda.media.playlist.view.PlaylistController;
import io.github.stcarolas.oda.media.playlist.view.PlaylistDto;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;

@MicronautTest(environments = "allinone")
public class SaveAndLoadPlaylistTest {
  private Logger log = LoggerFactory.getLogger(SaveAndLoadPlaylistTest.class);

  @Inject
  PlaylistController playlistController;

  @Inject
  PlaylistCommandController commandController;

  @Test
  public void testSaveAndLoadPlaylist() {
    Authentication auth = mock(Authentication.class);
    when(auth.getAttributes())
      .thenReturn(Map.of("preferred_username", "testuser"));

    CreatePlaylistCommand command = new CreatePlaylistCommand(
      "testplaylist",
      List.of(
        new PlaylistItem("test", "https://www.youtube.com/watch?v=VJtg7pJO3hQ")
      )
    );
    final PlaylistDto created = commandController.create(auth, command);

    final Page<PlaylistDto> page = playlistController.playlists(
      auth,
      Pageable.from(0, 10)
    );
    log.info("{}", page);
    assertEquals(1, page.getTotalSize());

    PlaylistDto expected = new PlaylistDto(
      created.id(),
      "testplaylist",
      "testuser",
      List.of(
        new PlaylistItem("test", "https://www.youtube.com/watch?v=VJtg7pJO3hQ")
      )
    );
    assertEquals(expected, page.getContent().iterator().next());
  }
}
