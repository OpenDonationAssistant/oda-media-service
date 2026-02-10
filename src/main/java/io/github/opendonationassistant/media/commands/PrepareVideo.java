package io.github.opendonationassistant.media.commands;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.integration.vk.VKApi;
import io.github.opendonationassistant.integration.youtube.ContentDetails;
import io.github.opendonationassistant.integration.youtube.Video;
import io.github.opendonationassistant.integration.youtube.Videos;
import io.github.opendonationassistant.integration.youtube.YouTube;
import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.settings.repository.MediaSettings;
import io.github.opendonationassistant.settings.repository.MediaSettingsRepository;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Put;
import io.micronaut.problem.HttpStatusType;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.zalando.problem.Problem;

@Controller
public class PrepareVideo {

  public static final String regex =
    "^((?:https?:)?\\/\\/)?((?:www|m)\\.)?((?:youtube(-nocookie)?\\.com|youtu.be))(\\/(?:[\\w\\-]+\\?v=|embed\\/|live\\/|v\\/)?)([\\w\\-]+)(\\S+)?$";

  public static final Pattern pattern = Pattern.compile(
    regex,
    Pattern.MULTILINE
  );

  private final ODALogger log = new ODALogger(this);

  private final YouTube youTube;
  private final VKApi vk;
  private final MediaSettingsRepository repository;

  @Inject
  public PrepareVideo(
    YouTube youTube,
    VKApi vk,
    MediaSettingsRepository repository
  ) {
    this.youTube = youTube;
    this.vk = vk;
    this.repository = repository;
  }

  @Put("/media/video")
  @Secured(SecurityRule.IS_ANONYMOUS)
  public CompletableFuture<HttpResponse<VideoData>> prepareVideo(
    @Body PrepareVideoCommand command
  ) {
    log.info("Preparing media", Map.of("url", command.url()));
    final MediaSettings settings = repository.getByRecipientId(
      command.recipientId()
    );
    return (
      command.url().contains("vkvideo.ru")
        ? prepareVk(settings, command.url())
        : prepareYoutube(settings, command.url())
    ).thenApply(HttpResponse::ok);
  }

  private CompletableFuture<VideoData> prepareVk(
    MediaSettings settings,
    String url
  ) {
    if (!settings.getData().vkvideoEnabled()) {
      throw Problem.builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Реквесты с VK Video выключены")
        .build();
    }
    var id = Generators.timeBasedEpochGenerator().generate().toString();
    var originId = url.replaceAll("https://vkvideo.ru/video", "");
    return vk
      .getEmbeddedInfo(Map.of("url", url))
      .thenApply(embeddedInfo -> {
        if (!settings.passWordsBlacklist(embeddedInfo.title())) {
          throw Problem.builder()
            .withTitle("Incorrect media")
            .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
            .withDetail("Видео содержит слова из черного списка")
            .build();
        }

        return new VideoData(
          id,
          originId,
          "vk",
          embeddedInfo.html(),
          embeddedInfo.title(),
          embeddedInfo.thumbnailUrl(),
          "prepared",
          null,
          null,
          null
        );
      });
  }

  private CompletableFuture<VideoData> prepareYoutube(
    MediaSettings settings,
    String url
  ) {
    if (!settings.getData().youtubeEnabled()) {
      throw Problem.builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Реквесты с YouTube выключены")
        .build();
    }
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

    Videos found = youTube.list(videoId);
    log.debug(
      "YouTube video search result",
      Map.of(
        "videoId",
        videoId,
        "amount",
        Optional.ofNullable(found.getItems()).map(it -> it.size()).orElse(0)
      )
    );
    if (found.getItems() == null || found.getItems().isEmpty()) {
      throw Problem.builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Видео не найдено")
        .build();
    }
    Video video = found.getItems().iterator().next();

    String viewStats = video.getStatistics().getViewCount();
    var viewCount = Integer.parseInt(viewStats);
    if (viewCount < settings.minViewAmount()) {
      throw Problem.builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Слишком мало просмотров у видео")
        .build();
    }
    if (
      Optional.ofNullable(video.getSnippet())
        .map(it -> it.getTitle())
        .filter(title -> settings.passWordsBlacklist(title))
        .isEmpty()
    ) {
      throw Problem.builder()
        .withTitle("Incorrect media")
        .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
        .withDetail("Видео содержит слова из черного списка")
        .build();
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
    return CompletableFuture.completedFuture(
      new VideoData(
        id,
        videoId,
        "youtube",
        "https://www.youtube.com/watch?v=%s".formatted(videoId),
        video.getSnippet().getTitle(),
        video.getSnippet().getThumbnails().get("default").getUrl(),
        "prepared",
        null,
        null,
        null
      )
    );
  }

  @Serdeable
  public static record PrepareVideoCommand(String url, String recipientId) {}
}
