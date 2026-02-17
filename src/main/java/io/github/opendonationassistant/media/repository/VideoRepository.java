package io.github.opendonationassistant.media.repository;

import io.github.opendonationassistant.media.senders.ReadyVideoNotificationSender;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Singleton
public class VideoRepository {

  private final VideoDataRepository dataRepository;
  private final ReadyVideoNotificationSender notificationSender;

  @Inject
  public VideoRepository(
    VideoDataRepository dataRepository,
    ReadyVideoNotificationSender notificationSender
  ) {
    this.dataRepository = dataRepository;
    this.notificationSender = notificationSender;
  }

  public Optional<PreparedVideo> findPreparedVideo(@Nullable String id) {
    return Optional.ofNullable(id)
      .flatMap(dataRepository::findById)
      .filter(it -> "prepared".equals(it.status()))
      .map(data -> new PreparedVideo(data, dataRepository, notificationSender));
  }

  public Optional<ReadyVideo> findReadyVideo(@Nullable String id) {
    return Optional.ofNullable(id)
      .flatMap(dataRepository::findById)
      .filter(it -> "ready".equals(it.status()))
      .map(data -> new ReadyVideo(data, dataRepository));
  }

  public CompletableFuture<List<PreparedVideo>> findPreparedVideosForPayment(
    String paymentId
  ) {
    return dataRepository
      .findByPaymentId(paymentId)
      .thenApply(videos ->
        videos
          .stream()
          .map(data ->
            new PreparedVideo(data, dataRepository, notificationSender)
          )
          .toList()
      );
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
