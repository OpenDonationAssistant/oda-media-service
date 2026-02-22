package io.github.opendonationassistant.media.commands;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.github.opendonationassistant.settings.repository.MediaSettingsRepository;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
public class LinkPayment {

  private final ODALogger log = new ODALogger(this);

  private final VideoRepository videoRepository;
  private final MediaSettingsRepository settingsRepository;

  @Inject
  public LinkPayment(
    VideoRepository videoRepository,
    MediaSettingsRepository settingsRepository
  ) {
    this.videoRepository = videoRepository;
    this.settingsRepository = settingsRepository;
  }

  @Transactional
  @Post("/commands/media/linkPayment")
  public CompletableFuture<HttpResponse<LinkPaymentResponse>> linkPayment(
    @Body LinkPaymentCommand command
  ) {
    return CompletableFuture.supplyAsync(() -> {
      log.info("Linking media to payment", Map.of("command", command));
      final List<PreparedVideo> found = command
        .mediaIds()
        .stream()
        .flatMap(id -> videoRepository.findPreparedVideo(id).stream())
        .toList();
      found.forEach(video -> video.linkPayment(command.paymentId()));
      Integer songCost = settingsRepository
        .getByRecipientId(command.recipientId())
        .getData()
        .songRequestCost();
      return HttpResponse.ok(
        new LinkPaymentResponse(new Amount(songCost * found.size(), 0, "RUB"))
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
