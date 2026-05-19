package io.github.stcarolas.oda.media;

import static org.hamcrest.Matchers.*;
import static org.instancio.Select.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.opendonationassistant.media.VideoController;
import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.media.repository.VideoDataRepository;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.instancio.Instancio;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@MicronautTest(environments = "allinone")
public class VideoControllerTest {

  @Inject
  ApplicationContext context;

  @Inject
  VideoDataRepository dataRepository;

  @Inject
  VideoController controller;

  @Test
  public void testGettingCountOfReadyVideos() {
    var videoTemplate = Instancio.of(VideoData.class)
      .set(field(VideoData::recipientId), "testuser")
      .toModel();

    var response = controller.countReadyVideos("testuser").join();
    assertEquals(0, response.body().count());

    var readyVideos = Instancio.of(videoTemplate)
      .set(field(VideoData::status), "ready")
      .stream()
      .limit(5);
    readyVideos.forEach(dataRepository::save);
    var handledVideos = Instancio.of(videoTemplate)
      .set(field(VideoData::status), "handled")
      .stream()
      .limit(6);
    handledVideos.forEach(dataRepository::save);
    var preparedVideos = Instancio.of(videoTemplate)
      .set(field(VideoData::status), "prepared")
      .stream()
      .limit(7);
    preparedVideos.forEach(dataRepository::save);

    response = controller.countReadyVideos("testuser").join();
    assertEquals(5, response.body().count());
  }

  @Test
  public void testGettingSecondPageOfHandledVideos() {
    var videoTemplate = Instancio.of(VideoData.class)
      .set(field(VideoData::status), "handled")
      .set(field(VideoData::recipientId), "testuser")
      .toModel();
  }

  // @Test
  // public void testSrcRegexp() {
  //       final String regex = ".*src=\\\"(.*)\\\".*";
  //       final String html = "<iframe\n  src=\"https://vk.com/video_ext.php?oid=-48229771&id=456239600&hash=03ba5ea3f2e7c813&__ref=vk.web2\"\n  width=\"853\"\n  height=\"480\"\n  allow=\"autoplay; encrypted-media; fullscreen; picture-in-picture; screen-wake-lock;\"\n  frameborder=\"0\"\n  allowfullscreen></iframe>";
  //       final Pattern srcPattern = Pattern.compile(
  //         ".*src=\\\"(.*)\\\".*",
  //         Pattern.MULTILINE
  //       );
  //       var matcher = srcPattern.matcher(html.replaceAll("\\n", ""));
  //       assertTrue(matcher.matches());
  // }

  @Test
  @Disabled
  public void testCreatingNewVideo(RequestSpecification spec) {
    // prettier-ignore
    String id = spec.given()
      .body("""
        {
          "url": "https://youtu.be/XCcN-IoYIJA?si=jXOSyxbIX9lD7XpV"
        }
        """)
      .when()
        .header("Content-Type", "application/json")
        .put("/media/video")
      .then()
        .statusCode(200)
        .body("id", notNullValue())
        .body("title",is("Wiljan & Xandra - Woodlands"))
        .body("url", is("https://www.youtube.com/watch?v=XCcN-IoYIJA"))
      .extract().path("id");
    spec
      .when()
      .get("/media/video/" + id)
      .then()
      .body("[0].id", notNullValue())
      .body("[0].title", is("Wiljan & Xandra - Woodlands"))
      .body("[0].url", is("https://www.youtube.com/watch?v=XCcN-IoYIJA"))
      .statusCode(200);
    // prettier-ignore
  }
}
