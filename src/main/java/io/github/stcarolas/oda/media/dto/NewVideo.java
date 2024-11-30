package io.github.stcarolas.oda.media.dto;

public class NewVideo {

  private String id;
  private String url;
  private String title;
  private String thumbnail;

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
  
}
