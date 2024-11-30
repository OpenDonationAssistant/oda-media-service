package io.github.stcarolas.oda.media.youtube;

import static io.micronaut.http.HttpHeaders.CONTENT_TYPE;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;

@Client(id = "youtube")
@Header(name = CONTENT_TYPE, value = "application/json")
public interface YouTubeLowLevelApi {

  @Get("/youtube/v3/videos?part=contentDetails&part=snippet&part=statistics")
  Videos list(@QueryValue("id") String id, @QueryValue("key") String key);

  @Get("/youtube/v3/search?part=snippet&type=video")
  SearchResponse search(
    @QueryValue("q") String query,
    @QueryValue("key") String key
  );

  @Get("/youtube/v3/playlistItems?part=snippet%2Cid")
  PlaylistItemList playlistItems(
    @QueryValue("key") String key,
    @QueryValue("playlistId") String playlistId,
    @QueryValue("pageToken") String pageToken
  );

  @Get("/youtube/v3/playlists?part=snippet")
  PlaylistItemList playlistInfo(
    @QueryValue("key") String key,
    @QueryValue("id") String id
  );
}
