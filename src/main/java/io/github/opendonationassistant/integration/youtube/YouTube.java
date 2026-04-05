package io.github.opendonationassistant.integration.youtube;

import java.util.concurrent.CompletableFuture;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class YouTube {

  @Value("${youtube.key}")
  private String key;

  @Inject
  private YouTubeLowLevelApi api;

  @Inject
  public YouTube(@Value("${youtube.key}") String key, YouTubeLowLevelApi api) {
    this.key = key;
    this.api = api;
  }

  public CompletableFuture<Videos> list(String id) {
    return api.list(id, key);
  }

  public SearchResponse search(String query) {
    return api.search(query, key);
  }

  public PlaylistItemList playlistItems(String playlistId, String pageToken) {
    return api.playlistItems(key, playlistId, pageToken);
  }

  public PlaylistItemList playlistInfo(String id) {
    return api.playlistInfo(key, id);
  }
}
