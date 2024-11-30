package io.github.stcarolas.oda.media.youtube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;

@MicronautTest(environments = "allinone")
@Property(name = "youtube.key", value = "test")
@Property(
  name = "micronaut.http.services.youtube.url",
  value = "http://localhost:15000"
)
@WireMockTest(httpPort = 15000)
public class YouTubeTest {

  @Inject
  YouTube youtube;

  @Test
  public void testGettingVideoInfo() {
    YouTubeStubs.successListResponse();
    Videos found = youtube.list("z4FWUtsni7g");
    assertNotNull(found);
    assertNotNull(found.getItems());
    assertTrue(found.getItems().size() == 1);
    Video video = found.getItems().get(0);
    Snippet snippet = video.getSnippet();
    assertNotNull(snippet);
    assertEquals(
      "Beautiful Medieval Fantasy Tavern, Medieval Inn | Fantasy Music and Ambience Cozy",
      snippet.getTitle()
    );
    HashMap<String, Thumbnail> thumbnails = video.getSnippet().getThumbnails();
    assertNotNull(thumbnails);
    Thumbnail defaultThumb = thumbnails.get("default");
    assertNotNull(defaultThumb);
    assertEquals("https://i.ytimg.com/vi/z4FWUtsni7g/default.jpg", defaultThumb.getUrl());
    ContentDetails contentDetails = video.getContentDetails();
    assertNotNull(contentDetails);
  }

}
