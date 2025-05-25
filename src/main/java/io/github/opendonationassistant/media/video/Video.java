package io.github.opendonationassistant.media.video;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
@MappedEntity("video")
public class Video {

  @Id
  private String id;

  private String originId;
  private String provider;
  private String url;
  private String title;
  private String thumbnail;
  private String owner;
  private String recipientId;
  private String status;
  private Instant readyTimestamp;

  public Instant getReadyTimestamp() {
    return readyTimestamp;
  }

  public void setReadyTimestamp(Instant readyTimestamp) {
    this.readyTimestamp = readyTimestamp;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getThumbnail() {
    return thumbnail;
  }

  public void setThumbnail(String thumbnail) {
    this.thumbnail = thumbnail;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getRecipientId() {
    return recipientId;
  }

  public void setRecipientId(String recipientId) {
    this.recipientId = recipientId;
  }

  public String getOriginId() {
    return originId;
  }

  public void setOriginId(String originId) {
    this.originId = originId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return (
      "{\"_type\"=\"Video\",\"id\"=\"" +
      id +
      "\", originId\"=\"" +
      originId +
      "\", url\"=\"" +
      url +
      "\", title\"=\"" +
      title +
      "\", thumbnail\"=\"" +
      thumbnail +
      "\", owner\"=\"" +
      owner +
      "\", recipientId\"=\"" +
      recipientId +
      "\", status\"=\"" +
      status +
      "\", readyTimestamp\"=\"" +
      readyTimestamp +
      "}"
    );
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }
}
