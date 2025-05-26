package io.github.opendonationassistant.media.video;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.Beans;
import io.github.opendonationassistant.media.repository.settings.MediaSettings;
import io.github.opendonationassistant.media.repository.settings.MediaSettingsRepository;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.github.opendonationassistant.media.youtube.ContentDetails;
import io.github.opendonationassistant.media.youtube.RegionRestriction;
import io.github.opendonationassistant.media.youtube.Video;
import io.github.opendonationassistant.media.youtube.Videos;
import io.github.opendonationassistant.media.youtube.YouTube;
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
  private MediaSettings settings;

  public NewVideoRequest(String url, MediaSettings settings) {
    this.url = url;
    this.settings = settings;
  }

  public PreparedVideo prepare() {
    log.info("preparing media by url: {}", url);
    return url.contains("vkvideo.ru") ? preparedVk() : prepareYoutube();
  }

  private PreparedVideo preparedVk() {
    if (!settings.getData().vkvideoEnabled()){
      throw Problem
        .builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Реквесты с VK Video выключены")
        .build();
    }
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
    if (!settings.getData().youtubeEnabled()){
      throw Problem
        .builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Реквесты с YouTube выключены")
        .build();
    }
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
    var viewCount = Integer.parseInt(viewStats);
    if (viewCount < settings.minViewAmount()) {
      throw Problem
        .builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Слишком мало просмотров у видео")
        .build();
    }
    if (
      Optional
        .ofNullable(video.getSnippet())
        .map(it -> it.getTitle())
        .filter(title -> settings.passWordsBlacklist(title))
        .isEmpty()
    ) {
      throw Problem
        .builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Видео содержит слова из черного списка")
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
