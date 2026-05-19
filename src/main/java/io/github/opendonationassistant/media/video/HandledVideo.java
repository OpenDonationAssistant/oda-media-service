package io.github.opendonationassistant.media.video;

import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.media.repository.VideoDataRepository;

public class HandledVideo {

  private VideoData data;
  private final VideoDataRepository repository;

  public HandledVideo(VideoData data, VideoDataRepository repository) {
    this.data = data;
    this.repository = repository;
  }

  public void save() {
    repository.update(data);
  }

  public VideoData data() {
    return this.data;
  }
}
