package io.github.stcarolas.oda.media.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.media.commands.LinkPayment;
import io.github.opendonationassistant.media.commands.LinkPayment.LinkPaymentCommand;
import io.github.opendonationassistant.media.commands.LinkPayment.LinkPaymentResponse;
import io.github.opendonationassistant.media.metrics.MediaMetrics;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.github.opendonationassistant.settings.repository.MediaSettings;
import io.github.opendonationassistant.settings.repository.MediaSettingsData;
import io.github.opendonationassistant.settings.repository.MediaSettingsData.TARIFICATION;
import io.github.opendonationassistant.settings.repository.MediaSettingsRepository;
import io.micronaut.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LinkPaymentTest {

  @Mock
  VideoRepository videoRepository;

  @Mock
  MediaSettingsRepository settingsRepository;

  @Mock
  MediaMetrics metrics;

  @Mock
  PreparedVideo first;

  @Mock
  PreparedVideo second;

  LinkPayment controller;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new LinkPayment(videoRepository, settingsRepository, metrics);
  }

  @Test
  public void linksMediaToPaymentAndCountsIt() {
    when(videoRepository.findPreparedVideo("media-1"))
      .thenReturn(Optional.of(first));
    when(videoRepository.findPreparedVideo("media-2"))
      .thenReturn(Optional.of(second));
    mockSettings(100);

    HttpResponse<LinkPaymentResponse> response = controller
      .linkPayment(
        new LinkPaymentCommand(
          "recipient",
          "payment-1",
          List.of("media-1", "media-2")
        )
      )
      .join();

    verify(first).linkPayment("payment-1");
    verify(second).linkPayment("payment-1");
    verify(metrics, times(2)).mediaPaymentLinked();
    assertEquals(new Amount(200, 0, "RUB"), response.body().requiredAmount());
  }

  @Test
  public void countsOnlyVideosActuallyFound() {
    when(videoRepository.findPreparedVideo("media-1"))
      .thenReturn(Optional.of(first));
    when(videoRepository.findPreparedVideo("missing"))
      .thenReturn(Optional.empty());
    mockSettings(100);

    HttpResponse<LinkPaymentResponse> response = controller
      .linkPayment(
        new LinkPaymentCommand(
          "recipient",
          "payment-1",
          List.of("media-1", "missing")
        )
      )
      .join();

    verify(first).linkPayment("payment-1");
    verify(second, never()).linkPayment("payment-1");
    verify(metrics).mediaPaymentLinked();
    assertEquals(new Amount(100, 0, "RUB"), response.body().requiredAmount());
  }

  @Test
  public void doesNotCountWhenNothingFound() {
    when(videoRepository.findPreparedVideo("missing"))
      .thenReturn(Optional.empty());
    mockSettings(100);

    HttpResponse<LinkPaymentResponse> response = controller
      .linkPayment(
        new LinkPaymentCommand("recipient", "payment-1", List.of("missing"))
      )
      .join();

    verify(metrics, never()).mediaPaymentLinked();
    assertEquals(new Amount(0, 0, "RUB"), response.body().requiredAmount());
  }

  private void mockSettings(int cost) {
    MediaSettings settings = org.mockito.Mockito.mock(MediaSettings.class);
    var data = new MediaSettingsData(
      "settings-id",
      "recipient",
      cost,
      TARIFICATION.PER_LINK,
      null,
      10,
      0,
      true,
      true,
      true,
      "",
      List.of()
    );
    when(settings.getData()).thenReturn(data);
    when(settingsRepository.getByRecipientId("recipient")).thenReturn(settings);
  }
}