package io.github.stcarolas.oda.media.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.opendonationassistant.media.metrics.MediaMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

public class MediaMetricsTest {

  @Test
  public void countsRequestedMediaPerSystem() {
    var registry = new SimpleMeterRegistry();
    var metrics = new MediaMetrics(registry);

    metrics.mediaRequested("twitch");
    metrics.mediaRequested("twitch");
    metrics.mediaRequested("kick");
    metrics.mediaRequested(null);

    assertEquals(
      2,
      registry
        .counter(
          MediaMetrics.REQUESTED_METRIC_NAME,
          MediaMetrics.SYSTEM_TAG,
          "twitch"
        )
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(MediaMetrics.REQUESTED_METRIC_NAME, MediaMetrics.SYSTEM_TAG, "kick")
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(
          MediaMetrics.REQUESTED_METRIC_NAME,
          MediaMetrics.SYSTEM_TAG,
          MediaMetrics.UNKNOWN_SYSTEM
        )
        .count()
    );
  }

  @Test
  public void countsAddedMediaPerSystem() {
    var registry = new SimpleMeterRegistry();
    var metrics = new MediaMetrics(registry);

    metrics.mediaAdded("twitch");
    metrics.mediaAdded("twitch");
    metrics.mediaAdded("vk");

    assertEquals(
      2,
      registry
        .counter(MediaMetrics.ADDED_METRIC_NAME, MediaMetrics.SYSTEM_TAG, "twitch")
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(MediaMetrics.ADDED_METRIC_NAME, MediaMetrics.SYSTEM_TAG, "vk")
        .count()
    );
  }


  @Test
  public void countsPaymentAddedMediaPerSystem() {
    var registry = new SimpleMeterRegistry();
    var metrics = new MediaMetrics(registry);

    metrics.mediaPaymentAdded("ODA");
    metrics.mediaPaymentAdded("ODA");
    metrics.mediaPaymentAdded("StreamElements");

    assertEquals(
      2,
      registry
        .counter(
          MediaMetrics.PAYMENT_ADDED_METRIC_NAME,
          MediaMetrics.SYSTEM_TAG,
          "ODA"
        )
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(
          MediaMetrics.PAYMENT_ADDED_METRIC_NAME,
          MediaMetrics.SYSTEM_TAG,
          "StreamElements"
        )
        .count()
    );
  }

  @Test
  public void countsLinkedPaymentMedia() {
    var registry = new SimpleMeterRegistry();
    var metrics = new MediaMetrics(registry);

    metrics.mediaPaymentLinked();
    metrics.mediaPaymentLinked();

    assertEquals(
      2,
      registry.counter(MediaMetrics.PAYMENT_LINKED_METRIC_NAME).count()
    );
  }
}
