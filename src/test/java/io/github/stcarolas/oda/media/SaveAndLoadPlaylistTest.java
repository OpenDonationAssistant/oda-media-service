package io.github.stcarolas.oda.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.playlist.commands.CreatePlaylist;
import io.github.opendonationassistant.playlist.commands.CreatePlaylist.CreatePlaylistCommand;
import io.github.opendonationassistant.playlist.repository.PlaylistData.PlaylistItem;
import io.github.opendonationassistant.playlist.view.PlaylistController;
import io.github.opendonationassistant.playlist.view.PlaylistDto;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MicronautTest(environments = "allinone")
public class SaveAndLoadPlaylistTest {

  private Logger log = LoggerFactory.getLogger(SaveAndLoadPlaylistTest.class);

  @Inject
  PlaylistController playlistController;

  @Inject
  CreatePlaylist createPlaylist;

  @Test
  public void testSaveAndLoadPlaylist() {
    Authentication auth = mock(Authentication.class);
    when(auth.getAttributes()).thenReturn(
      Map.of("preferred_username", "testuser")
    );

    CreatePlaylistCommand command = new CreatePlaylistCommand(
      "testplaylist",
      List.of(
        new PlaylistItem("test", "https://www.youtube.com/watch?v=VJtg7pJO3hQ")
      )
    );
    final PlaylistDto created = createPlaylist
      .create(auth, command)
      .getBody()
      .get();

    final Page<PlaylistDto> page = playlistController
      .playlists(auth, Pageable.from(0, 10))
      .getBody()
      .get();
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
