package io.github.opendonationassistant.media.commands;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.media.metrics.MediaMetrics;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.github.opendonationassistant.settings.repository.MediaSettingsRepository;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Controller
public class LinkPayment {

  private final ODALogger log = new ODALogger(this);

  private final VideoRepository videoRepository;
  private final MediaSettingsRepository settingsRepository;
  private final MediaMetrics metrics;

  @Inject
  public LinkPayment(
    VideoRepository videoRepository,
    MediaSettingsRepository settingsRepository,
    MediaMetrics metrics
  ) {
    this.videoRepository = videoRepository;
    this.settingsRepository = settingsRepository;
    this.metrics = metrics;
  }

  @Transactional
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Post("/commands/media/linkPayment")
  public CompletableFuture<HttpResponse<LinkPaymentResponse>> linkPayment(
    @Body LinkPaymentCommand command
  ) {
    return CompletableFuture.supplyAsync(() -> {
      log.info("Linking media to payment", Map.of("command", command));
      Integer songCost = Optional.ofNullable(
        settingsRepository
          .getByRecipientId(command.recipientId())
          .getData()
          .songRequestCost()
      ).orElse(100);
      var totalCost = command
        .mediaIds()
        .stream()
        .flatMap(id -> videoRepository.findById(id).stream())
        .reduce(
          Integer.valueOf(0),
          (sum, video) -> {
            if (video instanceof PreparedVideo preparedVideo) {
              preparedVideo.linkPayment(command.paymentId());
              metrics.mediaPaymentLinked();
              return sum + songCost;
            }
            return sum;
          },
          Integer::sum
        );
      return HttpResponse.ok(
        new LinkPaymentResponse(new Amount(totalCost, 0, "RUB"))
      );
    });
  }

  @Serdeable
  public static record LinkPaymentCommand(
    String recipientId,
    String paymentId,
    List<String> mediaIds
  ) {}

  @Serdeable
  public static record LinkPaymentResponse(Amount requiredAmount) {}
}
