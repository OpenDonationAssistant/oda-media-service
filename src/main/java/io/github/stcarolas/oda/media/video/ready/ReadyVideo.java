package io.github.stcarolas.oda.media.video.ready;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.stcarolas.oda.Beans;
import io.github.stcarolas.oda.media.video.Video;
import io.github.stcarolas.oda.media.video.VideoRepository;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class ReadyVideo extends Video {

  private Logger log = LoggerFactory.getLogger(ReadyVideo.class);

  public ReadyVideo(){
    setStatus("ready");
  }

  public static ReadyVideo from(Video origin){
    return from(origin, origin.getReadyTimestamp());
  }

  public static ReadyVideo from(Video origin, Instant readyTimestamp){
    var video = new ReadyVideo();
    video.setId(origin.getId());
    video.setUrl(origin.getUrl());
    video.setOwner(origin.getOwner());
    video.setTitle(origin.getTitle());
    video.setThumbnail(origin.getThumbnail());
    video.setRecipientId(origin.getRecipientId());
    video.setOriginId(origin.getOriginId());
    video.setReadyTimestamp(readyTimestamp);
    video.setProvider(origin.getProvider());
    return video;
  }

  public void save() {
    VideoRepository repository = Beans.get(
      VideoRepository.class
    );
    repository.update(this);
  }

  public void makeHandled(){
    log.info("Make {} handled", getId());
    setStatus("handled");
    this.save();
  }

  @Override
  public String toString() {
    return "{\"_type\"=\"ReadyVideo\",\"}";
  }
}
