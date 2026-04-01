package io.github.stcarolas.oda.media.youtube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.opendonationassistant.integration.youtube.ContentDetails;
import io.github.opendonationassistant.integration.youtube.Snippet;
import io.github.opendonationassistant.integration.youtube.Thumbnail;
import io.github.opendonationassistant.integration.youtube.Video;
import io.github.opendonationassistant.integration.youtube.Videos;
import io.github.opendonationassistant.integration.youtube.YouTube;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.HashMap;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
  @Disabled
  public void testGettingVideoInfo() {
    YouTubeStubs.successListResponse();
    Videos found = youtube.list("z4FWUtsni7g");
    assertNotNull(found);
    assertNotNull(found.items());
    assertTrue(found.items().size() == 1);
    Video video = found.items().get(0);
    Snippet snippet = video.snippet();
    assertNotNull(snippet);
    assertEquals(
      "Beautiful Medieval Fantasy Tavern, Medieval Inn | Fantasy Music and Ambience Cozy",
      snippet.title()
    );
    HashMap<String, Thumbnail> thumbnails = video.snippet().thumbnails();
    assertNotNull(thumbnails);
    Thumbnail defaultThumb = thumbnails.get("default");
    assertNotNull(defaultThumb);
    assertEquals(
      "https://i.ytimg.com/vi/z4FWUtsni7g/default.jpg",
      defaultThumb.url()
    );
    ContentDetails contentDetails = video.contentDetails();
    assertNotNull(contentDetails);
  }
}
