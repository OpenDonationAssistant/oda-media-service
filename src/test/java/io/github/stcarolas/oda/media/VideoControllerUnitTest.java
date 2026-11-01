package io.github.stcarolas.oda.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.media.VideoController;
import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.github.opendonationassistant.media.video.HasVideoData;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VideoControllerUnitTest {

  private VideoRepository repository;

  private VideoController controller;

  @BeforeEach
  public void setUp() {
    repository = mock(VideoRepository.class);
    controller = new VideoController(repository);
  }

  @Test
  public void marksReadyVideoAsHandled() {
    var ready = mock(ReadyVideo.class);
    when(repository.findById("ready")).thenReturn(
      Optional.<HasVideoData>of(ready)
    );

    controller.update("ready");

    verify(ready).makeHandled();
  }

  @Test
  public void doesNotMarkPreparedVideoAsHandled() {
    var prepared = mock(PreparedVideo.class);
    when(repository.findById("prepared")).thenReturn(
      Optional.<HasVideoData>of(prepared)
    );

    controller.update("prepared");

    verifyNoInteractions(prepared);
  }

  @Test
  public void returnsOnlyReadyVideosForRequestedIds() {
    var readyData = videoData("ready");
    var ready = mock(ReadyVideo.class);
    when(ready.data()).thenReturn(readyData);
    var prepared = mock(PreparedVideo.class);
    when(repository.findById("ready")).thenReturn(
      Optional.<HasVideoData>of(ready)
    );
    when(repository.findById("prepared")).thenReturn(
      Optional.<HasVideoData>of(prepared)
    );
    when(repository.findById("missing")).thenReturn(
      Optional.<HasVideoData>empty()
    );

    var found = controller.get("ready,prepared,missing");

    assertEquals(List.of(readyData), found);
    verifyNoInteractions(prepared);
  }

  private VideoData videoData(String id) {
    return new VideoData(
      id,
      "origin",
      "youtube",
      "https://youtu.be/" + id,
      "title",
      "thumbnail",
      "ready",
      null,
      "recipient",
      null,
      null,
      "ODA"
    );
  }
}
