package io.github.opendonationassistant.media.repository;

import io.github.opendonationassistant.media.senders.ReadyVideoNotificationSender;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
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
      .map(data -> new PreparedVideo(data, dataRepository, notificationSender));
  }
}
