package io.github.opendonationassistant.media.video.prepared;

import io.github.opendonationassistant.commons.ToString;
import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.media.repository.VideoDataRepository;
import io.github.opendonationassistant.media.senders.ReadyVideoNotificationSender;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreparedVideo {

  private Logger log = LoggerFactory.getLogger(ReadyVideo.class);
  private final VideoData data;
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

  public void save() {
    log.info("Saving prepared video: {}", ToString.asJson(this));
    repository.update(data);
  }

  public ReadyVideo makeReady(String owner, String recipient) {
    log.info("Make {} ready", data.id());
    var updateData = data.withOwner(owner).withRecipientId(recipient);
    repository.update(updateData);
    var video = new ReadyVideo(updateData, repository);
    notificationSender.send("%smedia".formatted(recipient), video);
    return video;
  }
}
