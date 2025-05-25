package io.github.opendonationassistant.media.playlist.repository;

import com.fasterxml.uuid.Generators;

import io.github.opendonationassistant.media.playlist.Playlist;
import io.github.opendonationassistant.media.playlist.PlaylistItem;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class PlaylistRepository {

  private final PlaylistDataRepository dataRepository;

  public PlaylistRepository(PlaylistDataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  public Playlist create(
    String title,
    String ownerId,
    List<PlaylistItem> items
  ) {
    var data = new PlaylistData(
      Generators.timeBasedEpochGenerator().generate().toString(),
      title,
      ownerId,
      items
    );
    dataRepository.save(data);
    return data.asPlaylist(dataRepository);
  }

  public @Nonnull Optional<Playlist> get(@Nullable String id) {
    return Optional
      .ofNullable(id)
      .flatMap(dataRepository::findById)
      .map(data -> data.asPlaylist(dataRepository));
  }

  public Page<Playlist> list(String ownerId, Pageable pageable) {
    return dataRepository
      .findByOwnerIdOrderById(ownerId, pageable)
      .map(data -> data.asPlaylist(dataRepository));
  }
}
