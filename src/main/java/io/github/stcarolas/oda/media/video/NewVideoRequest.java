package io.github.stcarolas.oda.media.video;

import com.fasterxml.uuid.Generators;
import io.github.stcarolas.oda.Beans;
import io.github.stcarolas.oda.media.video.prepared.PreparedVideo;
import io.github.stcarolas.oda.media.youtube.ContentDetails;
import io.github.stcarolas.oda.media.youtube.RegionRestriction;
import io.github.stcarolas.oda.media.youtube.Video;
import io.github.stcarolas.oda.media.youtube.Videos;
import io.github.stcarolas.oda.media.youtube.YouTube;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpStatus;
import io.micronaut.problem.HttpStatusType;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;

public class NewVideoRequest {

  private Logger log = LoggerFactory.getLogger(NewVideoRequest.class);

  public static final String regex =
    "^((?:https?:)?\\/\\/)?((?:www|m)\\.)?((?:youtube(-nocookie)?\\.com|youtu.be))(\\/(?:[\\w\\-]+\\?v=|embed\\/|live\\/|v\\/)?)([\\w\\-]+)(\\S+)?$";

  public static final Pattern pattern = Pattern.compile(
    regex,
    Pattern.MULTILINE
  );

  private String url;

  public NewVideoRequest(String url) {
    this.url = url;
  }

  public PreparedVideo prepare() {
    log.debug("preparing media by url: {}", url);
    return url.contains("vkvideo.ru") ? preparedVk() : prepareYoutube();
  }

  private PreparedVideo preparedVk() {
    var id = Generators.timeBasedEpochGenerator().generate().toString();
    var originId = url.replaceAll("https://vkvideo.ru/video", "");
    var preparedVideo = new PreparedVideo();
    preparedVideo.setId(id);
    preparedVideo.setUrl(url + "&js_api=1");
    preparedVideo.setOriginId(originId);
    preparedVideo.setProvider("vk");
    return preparedVideo;
  }

  private PreparedVideo prepareYoutube() {
    var id = Generators.timeBasedEpochGenerator().generate().toString();

    var youTube = Beans.get(YouTube.class);
    if (!StringUtils.hasText(url)) {
      throw Problem
        .builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Отсутствует ссылка")
        .build();
    }
    Matcher matcher = pattern.matcher(url);
    if (!matcher.matches()) {
      throw Problem
        .builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Некорректная ссылка")
        .build();
    }
    String videoId = matcher.group(6);
    log.info("Parsed id: {}", videoId);

    Videos found = youTube.list(videoId);
    log.debug("Found: {}", found);
    if (found.getItems() == null || found.getItems().isEmpty()) {
      throw Problem
        .builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Видео не найдено")
        .build();
    }
    Video video = found.getItems().iterator().next();
    log.info("{}", video);

    String viewStats = video.getStatistics().getViewCount();
    if (viewStats.length() <= 3) {
      var viewCount = Integer.parseInt(viewStats);
      if (viewCount < 100) {
        throw Problem
          .builder()
          .withTitle("Incorrect media")
          .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
          .withDetail("Слишком мало просмотров у видео")
          .build();
      }
    }
    if (
      Optional
        .ofNullable(video.getSnippet())
        .map(it -> it.getTitle())
        .map(title -> title.toLowerCase())
        .filter(title -> !title.contains("right version"))
        .filter(title -> !title.contains("gachi"))
        .filter(title -> !title.contains("гачи"))
        .filter(title -> !title.contains("правильная версия"))
        .isEmpty()
    ) {
      throw Problem
        .builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Видео не должно быть гачи")
        .build();
    }
    if (
      Optional
        .ofNullable(video.getContentDetails())
        .map(ContentDetails::getContentRating)
        .map(it -> it.get("ytRating"))
        .map(it -> "ytAgeRestricted".equals(it))
        .orElse(false)
    ) {
      throw Problem
        .builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Видео не должно быть 18+")
        .build();
    }
    if (
      Optional
        .ofNullable(video.getContentDetails())
        .map(ContentDetails::getRegionRestriction)
        .map(RegionRestriction::getBlocked)
        .map(it -> it.contains("RU"))
        .orElse(false)
    ) {
      throw Problem
        .builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Видео заблокировано в РФ")
        .build();
    }

    var preparedVideo = new PreparedVideo();
    preparedVideo.setId(id);
    preparedVideo.setUrl(
      "https://www.youtube.com/watch?v=%s".formatted(videoId)
    );
    preparedVideo.setOriginId(videoId);
    preparedVideo.setTitle(video.getSnippet().getTitle());
    preparedVideo.setThumbnail(
      video.getSnippet().getThumbnails().get("default").getUrl()
    );
    preparedVideo.setProvider("youtube");
    return preparedVideo;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public String toString() {
    return "{\"_type\"=\"NewVideoRequest\",\"url\"=\"" + url + "}";
  }
}
