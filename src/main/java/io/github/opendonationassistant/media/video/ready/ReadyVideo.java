package io.github.opendonationassistant.media.video.ready;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.media.repository.VideoDataRepository;
import java.util.Map;

public class ReadyVideo {

  private final ODALogger log = new ODALogger(this);
  private VideoData data;
  private final VideoDataRepository repository;

  public ReadyVideo(VideoData data, VideoDataRepository repository) {
    this.data = data;
    this.repository = repository;
  }

  public void save() {
    repository.update(data);
  }

  public void makeHandled() {
    log.info("Make video handled", Map.of("id", data.id()));
    this.data = data.withStatus("handled");
    this.save();
  }
}
