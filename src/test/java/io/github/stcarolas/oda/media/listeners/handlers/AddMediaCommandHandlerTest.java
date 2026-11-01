package io.github.stcarolas.oda.media.listeners.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
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
import io.github.opendonationassistant.media.video.HandledVideo;
import io.github.opendonationassistant.media.video.HasVideoData;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import io.micronaut.http.HttpStatus;
import io.micronaut.problem.HttpStatusType;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zalando.problem.Problem;

public class AddMediaCommandHandlerTest {

  private static final String MEDIA_ID = "media-1";

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

  private AddMediaRequest request(String url) {
    return new AddMediaRequest(MEDIA_ID, url, "viewer", "recipient", "twitch");
  }

  private VideoData videoData(String id, String status) {
    return new VideoData(
      id,
      "origin",
      "youtube",
      "https://www.youtube.com/watch?v=" + id,
      "title",
      "thumbnail",
      status,
      "viewer",
      "recipient",
      Instant.now(),
      null,
      "ODA"
    );
  }

  private ReadyVideo readyVideo(String id) {
    return new ReadyVideo(videoData(id, "ready"), mock(VideoDataRepository.class));
  }

  private void stubSuccessfulCreation() {
    when(videoRepository.create(any(), any(), any())).thenReturn(
      CompletableFuture.completedFuture(prepared)
    );
    when(prepared.makeReady(any(), any(), any())).thenReturn(
      readyVideo(MEDIA_ID)
    );
  }

  @Test
  public void returnsAddedVideoId() {
    var request = request("https://youtu.be/kXYiU_JCYtU");
    stubSuccessfulCreation();

    var response = handler.handle(request);

    assertEquals(new AddMediaResponse(MEDIA_ID, ""), response);
    verify(videoRepository).create(MEDIA_ID, "recipient", request.url());
    verify(metrics).mediaRequested("twitch");
    verify(prepared).makeReady("viewer", "recipient", null);
    verify(metrics).mediaAdded("twitch");
  }

  @Test
  public void returnsExistingMediaIdWhenCommandIsRedelivered() {
    var request = request("https://youtu.be/kXYiU_JCYtU");
    when(videoRepository.findById(MEDIA_ID)).thenReturn(
      Optional.<HasVideoData>of(readyVideo(MEDIA_ID))
    );

    var response = handler.handle(request);

    assertEquals(new AddMediaResponse(MEDIA_ID, ""), response);
    verify(videoRepository, never()).create(any(), any(), any());
    verify(metrics, never()).mediaAdded("twitch");
    verify(metrics, never()).mediaFailed("twitch");
  }

  @Test
  public void returnsExistingMediaIdForHandledVideo() {
    var request = request("https://youtu.be/kXYiU_JCYtU");
    var handled = new HandledVideo(
      videoData(MEDIA_ID, "handled"),
      mock(VideoDataRepository.class)
    );
    when(videoRepository.findById(MEDIA_ID)).thenReturn(
      Optional.<HasVideoData>of(handled)
    );

    var response = handler.handle(request);

    assertEquals(new AddMediaResponse(MEDIA_ID, ""), response);
    verify(videoRepository, never()).create(any(), any(), any());
    verify(metrics, never()).mediaAdded("twitch");
  }

  @Test
  public void completesInterruptedCommandWithoutCreatingAnotherMedia() {
    var request = request("https://youtu.be/kXYiU_JCYtU");
    when(videoRepository.findById(MEDIA_ID)).thenReturn(
      Optional.<HasVideoData>of(prepared)
    );
    when(prepared.makeReady("viewer", "recipient", null)).thenReturn(
      readyVideo(MEDIA_ID)
    );

    var response = handler.handle(request);

    assertEquals(new AddMediaResponse(MEDIA_ID, ""), response);
    verify(videoRepository, never()).create(any(), any(), any());
    verify(prepared).makeReady("viewer", "recipient", null);
    verify(metrics).mediaAdded("twitch");
  }

  @Test
  public void rejectsCommandWithoutId() {
    var request = new AddMediaRequest(
      "  ",
      "https://youtu.be/kXYiU_JCYtU",
      "viewer",
      "recipient",
      "twitch"
    );

    var response = handler.handle(request);

    assertNull(response.id());
    assertEquals("Media id is empty", response.message());
    verify(videoRepository, never()).findById(any());
    verify(videoRepository, never()).create(any(), any(), any());
    verify(metrics).mediaFailed("twitch");
  }

  @Test
  public void returnsErrorMessageAndNoIdWhenVideoIsRejected() {
    var request = request("https://youtu.be/kXYiU_JCYtU");
    when(videoRepository.create(any(), any(), any())).thenReturn(
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
    var request = request("https://youtu.be/kXYiU_JCYtU");
    when(videoRepository.create(any(), any(), any())).thenReturn(
      CompletableFuture.failedFuture(new IllegalStateException("boom"))
    );

    var response = handler.handle(request);

    assertNull(response.id());
    assertEquals("Internal server error", response.message());
    verify(metrics).mediaFailed("twitch");
  }

  @Test
  public void returnsInternalServerErrorForWrappedNonProblemFailures() {
    var request = request("https://youtu.be/kXYiU_JCYtU");
    when(videoRepository.create(any(), any(), any())).thenReturn(
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
    var request = request(
      "https://www.youtube.com/watch?v=MVRfYIpppjg    dearheRock dearheAnechka dearheRock dearheAnechka "
    );
    stubSuccessfulCreation();

    handler.handle(request);

    verify(videoRepository).create(
      MEDIA_ID,
      "recipient",
      "https://www.youtube.com/watch?v=MVRfYIpppjg"
    );
  }

  @Test
  public void extractsUrlFromMessageWithSurroundingText() {
    var request = request(
      "  заказываю https://youtu.be/kXYiU_JCYtU, спасибо!  "
    );
    stubSuccessfulCreation();

    handler.handle(request);

    verify(videoRepository).create(
      MEDIA_ID,
      "recipient",
      "https://youtu.be/kXYiU_JCYtU"
    );
  }

  @Test
  public void extractsVkUrlFromFreeFormMessage() {
    var request = request(
      "https://vkvideo.ru/video-165282829_456239200 dearheRock"
    );
    stubSuccessfulCreation();

    handler.handle(request);

    verify(videoRepository).create(
      MEDIA_ID,
      "recipient",
      "https://vkvideo.ru/video-165282829_456239200"
    );
  }

  @Test
  public void passesMessageWithoutUrlThroughUnchanged() {
    var request = request("no link here");
    stubSuccessfulCreation();

    handler.handle(request);

    verify(videoRepository).create(MEDIA_ID, "recipient", "");
  }
}
