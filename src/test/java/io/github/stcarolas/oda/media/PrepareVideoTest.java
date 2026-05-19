package io.github.stcarolas.oda.media;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.opendonationassistant.media.commands.PrepareVideo;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@MicronautTest(environments = "allinone")
@Property(
  name = "micronaut.http.services.vk.url",
  value = "http://localhost:15001"
)
@WireMockTest(httpPort = 15001)
public class PrepareVideoTest {

  @Test
  public void testParsingYoutubeUrl() {
    assertTrue(
      PrepareVideo.pattern
        .matcher("https://www.youtube.com/watch?v=kXYiU_JCYtU")
        .matches()
    );
    assertTrue(
      PrepareVideo.pattern
        .matcher("https://youtu.be/kXYiU_JCYtU?si=nBzKU--XeI2kTPkd")
        .matches()
    );
  }

  @Test
  @Disabled
  public void testPrepareVideoWithVkUrl() {
    VKStubs.successVideoOembedResponse();

    given()
      .contentType("application/json")
      .body(
        """
        {
          "url": "https://vkvideo.ru/video-165282829_456239200",
          "recipientId": "test-recipient-id"
        }
        """
      )
      .when()
      .put("/media/video")
      .then()
      .statusCode(200)
      .body("id", notNullValue())
      .body("title", is("Linux by Rebrai: Linux from scratch — 04"))
      .body("provider", is("vk"))
      .body("url", containsString("vk.ru"));
  }
}
