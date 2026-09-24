package io.github.stcarolas.oda.media;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.events.history.HistoryFacade;
import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.media.repository.VideoDataRepository;
import io.github.opendonationassistant.media.senders.ReadyVideoNotificationSender;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class PreparedVideoTest {

  VideoDataRepository repository = mock(VideoDataRepository.class);

  ReadyVideoNotificationSender notificationSender = mock(
    ReadyVideoNotificationSender.class
  );

  HistoryFacade historyFacade = mock(HistoryFacade.class);

  @Test
  public void makeReadyPersistsReadyTimestamp() {
    when(historyFacade.sendEvent(any())).thenReturn(
      CompletableFuture.completedFuture(null)
    );
    when(notificationSender.send(any(), any())).thenReturn(
      CompletableFuture.completedFuture(null)
    );
    var data = new VideoData(
      "id",
      "origin",
      "youtube",
      "https://youtu.be/x",
      "title",
      "thumb",
      "prepared",
      null,
      null,
      null,
      null,
      "ODA"
    );
    var prepared = new PreparedVideo(
      data,
      repository,
      notificationSender,
      historyFacade
    );

    prepared.makeReady("owner", "recipient", null);

    var captor = ArgumentCaptor.forClass(VideoData.class);
    verify(repository).update(captor.capture());
    assertNotNull(captor.getValue().readyTimestamp());
  }
}
