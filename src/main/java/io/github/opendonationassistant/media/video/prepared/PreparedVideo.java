package io.github.opendonationassistant.media.video.prepared;

import io.github.opendonationassistant.Beans;
import io.github.opendonationassistant.media.video.Video;
import io.github.opendonationassistant.media.video.VideoRepository;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import io.github.opendonationassistant.media.video.ready.ReadyVideoNotificationSender;
import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Serdeable
public class PreparedVideo extends Video {

  private Logger log = LoggerFactory.getLogger(ReadyVideo.class);

  public static PreparedVideo from(Video origin) {
    var video = new PreparedVideo();
    video.setId(origin.getId());
    video.setUrl(origin.getUrl());
    video.setOwner(origin.getOwner());
    video.setTitle(origin.getTitle());
    video.setOriginId(origin.getOriginId());
    video.setThumbnail(origin.getThumbnail());
    video.setRecipientId(origin.getRecipientId());
    video.setProvider(origin.getProvider());
    return video;
  }

  public void save() {
    Beans.get(VideoRepository.class).save(this);
  }

  public ReadyVideo makeReady(String owner, String recipient) {
    log.info("Make {} ready", getId());
    var video = ReadyVideo.from(this, Instant.now());
    video.setOwner(owner);
    video.setRecipientId(recipient);
    Beans.get(VideoRepository.class).update(video);
    ReadyVideoNotificationSender sender = Beans.get(
      ReadyVideoNotificationSender.class
    );
    sender.send("%smedia".formatted(recipient), video);
    return video;
  }
}
