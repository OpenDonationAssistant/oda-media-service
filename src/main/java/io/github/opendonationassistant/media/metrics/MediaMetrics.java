package io.github.opendonationassistant.media.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;

@Singleton
public class MediaMetrics {

  public static final String REQUESTED_METRIC_NAME = "media.requested";
  public static final String ADDED_METRIC_NAME = "media.added";
  public static final String SYSTEM_TAG = "system";
  public static final String UNKNOWN_SYSTEM = "unknown";

  private final MeterRegistry registry;

  @Inject
  public MediaMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void mediaRequested(String system) {
    counter(REQUESTED_METRIC_NAME, system).increment();
  }

  public void mediaAdded(String system) {
    counter(ADDED_METRIC_NAME, system).increment();
  }

  private Counter counter(String name, String system) {
    return registry.counter(
      name,
      SYSTEM_TAG,
      Optional.ofNullable(system).orElse(UNKNOWN_SYSTEM)
    );
  }
}
