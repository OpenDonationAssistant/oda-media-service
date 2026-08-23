package io.github.stcarolas.oda.media.listeners.handlers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.media.listeners.handlers.AddMediaCommandHandler;
import io.github.opendonationassistant.media.listeners.handlers.AddMediaCommandHandler.AddMediaCommand;
import io.github.opendonationassistant.media.metrics.MediaMetrics;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.micronaut.serde.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AddMediaCommandHandlerTest {

  @Mock
  ObjectMapper mapper;

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
    handler = new AddMediaCommandHandler(mapper, videoRepository, metrics);
  }

  @Test
  public void countsTwitchRewardMediaAsRequestedAndAdded() throws Exception {
    var command = new AddMediaCommand(
      "https://youtu.be/kXYiU_JCYtU",
      "viewer",
      "recipient",
      "twitch"
    );
    when(videoRepository.create(any(), any())).thenReturn(
      CompletableFuture.completedFuture(prepared)
    );

    handler.handle(command);

    verify(videoRepository).create("recipient", command.url());
    verify(metrics).mediaRequested("twitch");
    verify(prepared).makeReady("viewer", "recipient", null);
    verify(metrics).mediaAdded("twitch");
  }

  @Test
  public void countsRequestedButNotAddedWhenMediaCreationFails()
    throws Exception {
    var command = new AddMediaCommand(
      "https://youtu.be/kXYiU_JCYtU",
      "viewer",
      "recipient",
      "twitch"
    );
    when(videoRepository.create(any(), any())).thenReturn(
      CompletableFuture.failedFuture(new RuntimeException("invalid video"))
    );

    handler.handle(command);

    verify(metrics).mediaRequested("twitch");
    verify(metrics, never()).mediaAdded("twitch");
  }
}

