package io.github.opendonationassistant.media.repository;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.history.HistoryFacade;
import io.github.opendonationassistant.media.senders.ReadyVideoNotificationSender;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Singleton
public class VideoRepository {

  private final ODALogger log = new ODALogger(this);
  private final VideoDataRepository dataRepository;
  private final ReadyVideoNotificationSender notificationSender;
  private final HistoryFacade historyFacade;

  @Inject
  public VideoRepository(
    VideoDataRepository dataRepository,
    ReadyVideoNotificationSender notificationSender,
    HistoryFacade historyFacade
  ) {
    this.dataRepository = dataRepository;
    this.notificationSender = notificationSender;
    this.historyFacade = historyFacade;
  }

  public Optional<PreparedVideo> findPreparedVideo(@Nullable String id) {
    return Optional.ofNullable(id)
      .flatMap(dataRepository::findById)
      .filter(it -> "prepared".equals(it.status()))
      .map(data ->
        new PreparedVideo(
          data,
          dataRepository,
          notificationSender,
          historyFacade
        )
      );
  }

  public Optional<ReadyVideo> findReadyVideo(@Nullable String id) {
    return Optional.ofNullable(id)
      .flatMap(dataRepository::findById)
      .filter(it -> "ready".equals(it.status()))
      .map(data -> new ReadyVideo(data, dataRepository));
  }

  public CompletableFuture<List<PreparedVideo>> findPreparedVideosForPayment(
    @Nullable String paymentId
  ) {
    if (paymentId == null) {
      return CompletableFuture.completedFuture(List.of());
    }
    log.debug(
      "Getting prepared videos for payment",
      Map.of("paymentId", paymentId)
    );
    return CompletableFuture.supplyAsync(() -> {
      var videos = dataRepository.findByPaymentId(paymentId);
      log.debug(
        "Found videos for payment",
        Map.of("paymentId", paymentId, "videos", videos)
      );
      return videos
        .stream()
        .map(data -> {
          log.debug(
            "Found prepared video for payment",
            Map.of("paymentId", paymentId, "video", data)
          );
          return new PreparedVideo(
            data,
            dataRepository,
            notificationSender,
            historyFacade
          );
        })
        .toList();
    });
  }

  public CompletableFuture<List<ReadyVideo>> findReadyVideosForRecipientId(
    String recipientId
  ) {
    return dataRepository
      .findByRecipientIdAndStatusOrderByReadyTimestamp(recipientId, "ready")
      .thenApply(videos ->
        videos
          .stream()
          .map(data -> new ReadyVideo(data, dataRepository))
          .toList()
      );
  }
}
