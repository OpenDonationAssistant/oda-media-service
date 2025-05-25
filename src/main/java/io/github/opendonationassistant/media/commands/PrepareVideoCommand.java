package io.github.opendonationassistant.media.commands;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.github.opendonationassistant.media.youtube.ContentDetails;
import io.github.opendonationassistant.media.youtube.RegionRestriction;
import io.github.opendonationassistant.media.youtube.Video;
import io.github.opendonationassistant.media.youtube.Videos;
import io.github.opendonationassistant.media.youtube.YouTube;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpStatus;
import io.micronaut.problem.HttpStatusType;
import io.micronaut.serde.annotation.Serdeable;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;

@Serdeable
public class PrepareVideoCommand {

  private Logger log = LoggerFactory.getLogger(PrepareVideoCommand.class);

  public static final String regex =
    "^((?:https?:)?\\/\\/)?((?:www|m)\\.)?((?:youtube(-nocookie)?\\.com|youtu.be))(\\/(?:[\\w\\-]+\\?v=|embed\\/|live\\/|v\\/)?)([\\w\\-]+)(\\S+)?$";

  public static final Pattern pattern = Pattern.compile(
    regex,
    Pattern.MULTILINE
  );

  private String url;

  public PrepareVideoCommand(String url) {
    this.url = url;
  }

  public PreparedVideo execute(YouTube youTube) {
    log.debug("preparing media by url: {}", url);
    var id = Generators.timeBasedEpochGenerator().generate().toString();
    if (!StringUtils.hasText(url)) {
      throw Problem.builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Отсутствует ссылка")
        .build();
    }
    Matcher matcher = pattern.matcher(url);
    if (!matcher.matches()) {
      throw Problem.builder()
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
      throw Problem.builder()
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
        throw Problem.builder()
          .withTitle("Incorrect media")
          .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
          .withDetail("Слишком мало просмотров у видео")
          .build();
      }
    }
    if (
      Optional.ofNullable(video.getContentDetails())
        .map(ContentDetails::getContentRating)
        .map(it -> it.get("ytRating"))
        .map(it -> "ytAgeRestricted".equals(it))
        .orElse(false)
    ) {
      throw Problem.builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Видео не должно быть 18+")
        .build();
    }
    if (
      Optional.ofNullable(video.getContentDetails())
        .map(ContentDetails::getRegionRestriction)
        .map(RegionRestriction::getBlocked)
        .map(it -> it.contains("RU"))
        .orElse(false)
    ) {
      throw Problem.builder()
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
    return preparedVideo;
  }
}
