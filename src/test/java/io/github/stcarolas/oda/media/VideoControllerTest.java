package io.github.stcarolas.oda.media;

import static org.hamcrest.Matchers.*;

import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@MicronautTest(environments = "allinone")
public class VideoControllerTest {

  @Inject
  ApplicationContext context;

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
