package io.github.stcarolas.oda.media.listeners.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.media.listeners.handlers.AddMediaCommandHandler;
import io.github.opendonationassistant.media.listeners.handlers.AddMediaCommandHandler.AddMediaRequest;
import io.github.opendonationassistant.media.listeners.handlers.AddMediaCommandHandler.AddMediaResponse;
import io.github.opendonationassistant.media.metrics.MediaMetrics;
import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.media.repository.VideoDataRepository;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import io.micronaut.http.HttpStatus;
import io.micronaut.problem.HttpStatusType;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zalando.problem.Problem;

public class AddMediaCommandHandlerTest {

  @Mock
  VideoRepository videoRepository;

  @Mock
  MediaMetrics metrics;

  @Mock
  PreparedVideo prepared;

  AddMediaCommandHandler handler;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new AddMediaCommandHandler(videoRepository, metrics);
  }

  private ReadyVideo readyVideo(String id) {
    var data = new VideoData(
      id,
      "origin",
      "youtube",
      "https://www.youtube.com/watch?v=" + id,
      "title",
      "thumbnail",
      "ready",
      "viewer",
      "recipient",
      Instant.now(),
      null,
      "ODA"
    );
    return new ReadyVideo(
      data,
      org.mockito.Mockito.mock(VideoDataRepository.class)
    );
  }

  private void stubSuccessfulCreation(String videoId) {
    when(videoRepository.create(any(), any())).thenReturn(
      CompletableFuture.completedFuture(prepared)
    );
    when(prepared.makeReady(any(), any(), any())).thenReturn(
      readyVideo(videoId)
    );
  }

  @Test
  public void returnsAddedVideoId() {
    var request = new AddMediaRequest(
      "https://youtu.be/kXYiU_JCYtU",
      "viewer",
      "recipient",
      "twitch"
    );
    stubSuccessfulCreation("video-1");

    var response = handler.handle(request);

    assertEquals(new AddMediaResponse("video-1", ""), response);
    verify(videoRepository).create("recipient", request.url());
    verify(metrics).mediaRequested("twitch");
    verify(prepared).makeReady("viewer", "recipient", null);
    verify(metrics).mediaAdded("twitch");
  }

  @Test
  public void returnsErrorMessageAndNoIdWhenVideoIsRejected() {
    var request = new AddMediaRequest(
      "https://youtu.be/kXYiU_JCYtU",
      "viewer",
      "recipient",
      "twitch"
    );
    when(videoRepository.create(any(), any())).thenReturn(
      CompletableFuture.failedFuture(
        Problem.builder()
          .withTitle("Incorrect media")
          .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
          .withDetail("Video is too long")
          .build()
      )
    );

    var response = handler.handle(request);

    assertNull(response.id());
    assertEquals("Video is too long", response.message());
    verify(metrics).mediaRequested("twitch");
    verify(metrics, never()).mediaAdded("twitch");
    verify(metrics).mediaFailed("twitch");
  }

  @Test
  public void returnsInternalServerErrorForUnexpectedFailures() {
    var request = new AddMediaRequest(
      "https://youtu.be/kXYiU_JCYtU",
      "viewer",
      "recipient",
      "twitch"
    );
    when(videoRepository.create(any(), any())).thenReturn(
      CompletableFuture.failedFuture(new IllegalStateException("boom"))
    );

    var response = handler.handle(request);

    assertNull(response.id());
    assertEquals("Internal server error", response.message());
    verify(metrics).mediaFailed("twitch");
  }

  @Test
  public void returnsInternalServerErrorForWrappedNonProblemFailures() {
    var request = new AddMediaRequest(
      "https://youtu.be/kXYiU_JCYtU",
      "viewer",
      "recipient",
      "twitch"
    );
    when(videoRepository.create(any(), any())).thenReturn(
      CompletableFuture.failedFuture(
        new RuntimeException(
          "wrapper",
          new IllegalArgumentException("secret internal detail")
        )
      )
    );

    var response = handler.handle(request);

    assertNull(response.id());
    assertEquals("Internal server error", response.message());
  }

  @Test
  public void extractsUrlFromFreeFormMessageWithTrailingText() {
    var request = new AddMediaRequest(
      "https://www.youtube.com/watch?v=MVRfYIpppjg    dearheRock dearheAnechka dearheRock dearheAnechka ",
      "viewer",
      "recipient",
      "twitch"
    );
    stubSuccessfulCreation("video-1");

    handler.handle(request);

    verify(videoRepository).create(
      "recipient",
      "https://www.youtube.com/watch?v=MVRfYIpppjg"
    );
  }

  @Test
  public void extractsUrlFromMessageWithSurroundingText() {
    var request = new AddMediaRequest(
      "  заказываю https://youtu.be/kXYiU_JCYtU, спасибо!  ",
      "viewer",
      "recipient",
      "twitch"
    );
    stubSuccessfulCreation("video-1");

    handler.handle(request);

    verify(videoRepository).create("recipient", "https://youtu.be/kXYiU_JCYtU");
  }

  @Test
  public void extractsVkUrlFromFreeFormMessage() {
    var request = new AddMediaRequest(
      "https://vkvideo.ru/video-165282829_456239200 dearheRock",
      "viewer",
      "recipient",
      "twitch"
    );
    stubSuccessfulCreation("video-1");

    handler.handle(request);

    verify(videoRepository).create(
      "recipient",
      "https://vkvideo.ru/video-165282829_456239200"
    );
  }

  @Test
  public void passesMessageWithoutUrlThroughUnchanged() {
    var request = new AddMediaRequest(
      "no link here",
      "viewer",
      "recipient",
      "twitch"
    );
    stubSuccessfulCreation("video-1");

    handler.handle(request);

    verify(videoRepository).create("recipient", "");
  }
}
