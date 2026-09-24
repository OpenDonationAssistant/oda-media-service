package io.github.stcarolas.oda.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.playlist.Playlist;
import io.github.opendonationassistant.playlist.commands.DeletePlaylist;
import io.github.opendonationassistant.playlist.commands.DeletePlaylist.DeletePlaylistCommand;
import io.github.opendonationassistant.playlist.commands.UpdatePlaylist;
import io.github.opendonationassistant.playlist.commands.UpdatePlaylist.UpdatePlaylistCommand;
import io.github.opendonationassistant.playlist.repository.PlaylistData.PlaylistItem;
import io.github.opendonationassistant.playlist.repository.PlaylistDataRepository;
import io.github.opendonationassistant.playlist.repository.PlaylistRepository;
import io.github.opendonationassistant.testutils.AuthenticationGenerator;
import io.micronaut.http.HttpStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class PlaylistAuthorizationTest {

  PlaylistRepository repository = mock(PlaylistRepository.class);
  PlaylistDataRepository dataRepository = mock(PlaylistDataRepository.class);

  UpdatePlaylist updatePlaylist = new UpdatePlaylist(repository);
  DeletePlaylist deletePlaylist = new DeletePlaylist(repository);

  private Playlist playlistOwnedBy(String owner) {
    return new Playlist("p1", "title", owner, List.of(), dataRepository);
  }

  @Test
  public void nonOwnerCannotUpdatePlaylist() {
    when(repository.get("p1")).thenReturn(
      Optional.of(playlistOwnedBy("alice"))
    );

    var response = updatePlaylist.update(
      AuthenticationGenerator.forUser("bob"),
      new UpdatePlaylistCommand("p1", List.of(new PlaylistItem("t", "s")))
    );

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
    verify(dataRepository, never()).update(any());
  }

  @Test
  public void ownerCanUpdatePlaylist() {
    when(repository.get("p1")).thenReturn(
      Optional.of(playlistOwnedBy("alice"))
    );

    var response = updatePlaylist.update(
      AuthenticationGenerator.forUser("alice"),
      new UpdatePlaylistCommand("p1", List.of(new PlaylistItem("t", "s")))
    );

    assertEquals(HttpStatus.OK, response.getStatus());
    verify(dataRepository).update(any());
  }

  @Test
  public void nonOwnerCannotDeletePlaylist() {
    when(repository.get("p1")).thenReturn(
      Optional.of(playlistOwnedBy("alice"))
    );

    var response = deletePlaylist.deletePlaylist(
      AuthenticationGenerator.forUser("bob"),
      new DeletePlaylistCommand("p1")
    );

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
    verify(dataRepository, never()).deleteById("p1");
  }

  @Test
  public void ownerCanDeletePlaylist() {
    when(repository.get("p1")).thenReturn(
      Optional.of(playlistOwnedBy("alice"))
    );

    var response = deletePlaylist.deletePlaylist(
      AuthenticationGenerator.forUser("alice"),
      new DeletePlaylistCommand("p1")
    );

    assertEquals(HttpStatus.OK, response.getStatus());
    verify(dataRepository).deleteById("p1");
  }
}
