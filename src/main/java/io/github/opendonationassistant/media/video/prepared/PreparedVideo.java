package io.github.opendonationassistant.media.video.prepared;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.media.repository.VideoDataRepository;
import io.github.opendonationassistant.media.senders.ReadyVideoNotificationSender;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import java.util.Map;

public class PreparedVideo {

  private static final ODALogger log = new ODALogger(PreparedVideo.class);
  private VideoData data;
  private final VideoDataRepository repository;
  private final ReadyVideoNotificationSender notificationSender;

  public PreparedVideo(
    VideoData data,
    VideoDataRepository repository,
    ReadyVideoNotificationSender notificationSender
  ) {
    this.data = data;
    this.repository = repository;
    this.notificationSender = notificationSender;
  }

  public void linkPayment(String paymentId) {
    this.data = data.withPaymentId(paymentId);
    this.save();
  }

  public void save() {
    log.info("Saving prepared video", Map.of("video", data));
    repository.update(data);
  }

  public ReadyVideo makeReady(String owner, String recipient) {
    log.info("Make video ready", Map.of("id", data.id()));
    var updateData = data.withOwner(owner).withRecipientId(recipient);
    repository.update(updateData);
    var video = new ReadyVideo(updateData, repository);
    notificationSender.send("%smedia".formatted(recipient), video);
    return video;
  }
}
