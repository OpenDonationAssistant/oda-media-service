package io.github.opendonationassistant.integration.vk;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.serde.annotation.Serdeable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Client(id = "vk")
public interface VKApi {
  @Post(
    value = "/method/video.getOembed",
    consumes = MediaType.APPLICATION_FORM_URLENCODED,
    produces = MediaType.APPLICATION_JSON
  )
  CompletableFuture<EmbeddedInfo> getEmbeddedInfo(
    @Body Map<String, String> request
  );

  @Serdeable
  public static record Response(EmbeddedInfo response) {}

  @Serdeable
  public static record EmbeddedInfo(
    String title,
    @JsonProperty("thumbnail_url") String thumbnailUrl,
    String html
  ) {}
}
