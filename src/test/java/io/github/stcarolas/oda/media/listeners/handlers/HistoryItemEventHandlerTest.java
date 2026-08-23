package io.github.stcarolas.oda.media.listeners.handlers;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.events.history.event.HistoryItemEvent;
import io.github.opendonationassistant.media.listeners.handlers.HistoryItemEventHandler;
import io.github.opendonationassistant.media.metrics.MediaMetrics;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.micronaut.serde.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HistoryItemEventHandlerTest {

  @Mock
  ObjectMapper mapper;

  @Mock
  VideoRepository repository;

  @Mock
  MediaMetrics metrics;

  @Mock
  PreparedVideo first;

  @Mock
  PreparedVideo second;

  HistoryItemEventHandler handler;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new HistoryItemEventHandler(mapper, repository, metrics);
  }

  @Test
  public void countsPaymentMediaRequestedAndAdded() throws Exception {
    when(repository.findPreparedVideosForPayment("payment-1"))
      .thenReturn(CompletableFuture.completedFuture(List.of(first, second)));

    handler.handle(mediaEvent("payment-1"));

    verify(first).makeReady("viewer", "recipient", "payment-1");
    verify(second).makeReady("viewer", "recipient", "payment-1");
    verify(metrics, times(2)).mediaPaymentAdded("ODA");
  }

  @Test
  public void ignoresEventsWithoutOriginId() throws Exception {
    handler.handle(mediaEvent(null));

    verifyNoInteractions(metrics);
    verifyNoInteractions(repository);
  }

  @Test
  public void countsRequestedButNotAddedWhenNoVideosFound() throws Exception {
    when(repository.findPreparedVideosForPayment("payment-1"))
      .thenReturn(CompletableFuture.completedFuture(List.of()));

    handler.handle(mediaEvent("payment-1"));

    verify(metrics, times(0)).mediaPaymentAdded("ODA");
  }

  private HistoryItemEvent mediaEvent(String originId) {
    return new HistoryItemEvent(
      "id",
      "payment",
      "recipient",
      "ODA",
      originId,
      Instant.now(),
      "viewer",
      new Amount(100, 0, "RUB"),
      "message",
      List.of(),
      List.of(),
      null
    );
  }
}
