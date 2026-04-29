package io.github.opendonationassistant.media.commands;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.integration.vk.VKApi;
import io.github.opendonationassistant.integration.youtube.ContentDetails;
import io.github.opendonationassistant.integration.youtube.Statistics;
import io.github.opendonationassistant.integration.youtube.Video;
import io.github.opendonationassistant.integration.youtube.YouTube;
import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.media.repository.VideoDataRepository;
import io.github.opendonationassistant.settings.repository.MediaSettings;
import io.github.opendonationassistant.settings.repository.MediaSettingsData.TARIFICATION;
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
import java.time.Duration;
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

  public static final Pattern srcPattern = Pattern.compile(
    ".*src=\\\"(.*?)\\\".*",
    Pattern.MULTILINE
  );

  private final ODALogger log = new ODALogger(this);

  private final YouTube youTube;
  private final VKApi vk;
  private final MediaSettingsRepository repository;
  private final VideoDataRepository videoRepository;

  @Inject
  public PrepareVideo(
    YouTube youTube,
    VKApi vk,
    MediaSettingsRepository repository,
    VideoDataRepository videoRepository
  ) {
    this.youTube = youTube;
    this.vk = vk;
    this.repository = repository;
    this.videoRepository = videoRepository;
  }

  @Put("/media/video")
  @Secured(SecurityRule.IS_ANONYMOUS)
  public CompletableFuture<HttpResponse<PrepareVideoResponse>> prepareVideo(
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

  @Serdeable
  public static record PrepareVideoResponse(
    String id,
    String originId,
    String provider,
    String url,
    String title,
    String thumbnail,
    Amount cost
  ) {}

  private CompletableFuture<PrepareVideoResponse> prepareVk(
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
      .getEmbeddedInfo(Map.of("url", url, "v", "5.199"))
      .thenApply(response -> response.response())
      .thenApply(embeddedInfo -> {
        log.debug("Got vk video info", Map.of("info", embeddedInfo));
        if (!settings.passWordsBlacklist(embeddedInfo.title())) {
          throw Problem.builder()
            .withTitle("Incorrect media")
            .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
            .withDetail("Видео содержит слова из черного списка")
            .build();
        }

        var matcher = srcPattern.matcher(
          embeddedInfo.html().replaceAll("\\n", "")
        );
        if (!matcher.matches()) {
          throw Problem.builder()
            .withTitle("Incorrect media")
            .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
            .withDetail("Невозможно распознать видео")
            .build();
        }
        String src = matcher.group(1);

        return new VideoData(
          id,
          originId,
          "vk",
          src,
          embeddedInfo.title(),
          embeddedInfo.thumbnailUrl(),
          "prepared",
          null,
          null,
          null,
          null
        );
      })
      .thenApply(videoRepository::save)
      .thenApply(data -> {
        return new PrepareVideoResponse(
          data.id(),
          data.originId(),
          data.provider(),
          data.url(),
          data.title(),
          data.thumbnail(),
          new Amount(
            Optional.ofNullable(settings.getData())
              .map(it -> it.songRequestCost())
              .orElse(100),
            0,
            "RUB"
          )
        );
      });
  }

  private CompletableFuture<PrepareVideoResponse> prepareYoutube(
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

    return youTube
      .list(videoId)
      .thenApply(found -> {
        log.debug(
          "YouTube video search result",
          Map.of(
            "videoId",
            videoId,
            "amount",
            Optional.ofNullable(found.items()).map(it -> it.size()).orElse(0)
          )
        );
        if (found.items() == null || found.items().isEmpty()) {
          throw Problem.builder()
            .withTitle("Incorrect media")
            .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
            .withDetail("Видео не найдено")
            .build();
        }
        Video video = found.items().iterator().next();
        final Long duration = Optional.ofNullable(video.contentDetails())
          .map(ContentDetails::duration)
          .map(Duration::parse)
          .map(Duration::toSeconds)
          .orElse(0L);
        if (
          settings.getData().maxLen() != null &&
          duration > settings.getData().maxLen()
        ) {
          throw Problem.builder()
            .withTitle("Incorrect media")
            .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
            .withDetail("Слишком длинное видео")
            .build();
        }
        var viewCount = Optional.ofNullable(video.statistics())
          .map(Statistics::viewCount)
          .map(Integer::parseInt)
          .orElse(0);
        if (viewCount < settings.minViewAmount()) {
          throw Problem.builder()
            .withTitle("Incorrect media")
            .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
            .withDetail("Слишком мало просмотров у видео")
            .build();
        }
        if (
          Optional.ofNullable(video.snippet())
            .map(it -> it.title())
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
          Optional.ofNullable(video.contentDetails())
            .map(ContentDetails::contentRating)
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
        final var snippet = video.snippet();
        if (snippet == null) {
          throw Problem.builder()
            .withTitle("Incorrect media")
            .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
            .withDetail("Некорректная ссылка")
            .build();
        }
        videoRepository.save(
          new VideoData(
            id,
            videoId,
            "youtube",
            "https://www.youtube.com/watch?v=%s".formatted(videoId),
            Optional.ofNullable(snippet.title()).orElse(""),
            Optional.ofNullable(snippet.thumbnails())
              .map(it -> it.get("default"))
              .map(it -> it.url())
              .orElse(""),
            "prepared",
            null,
            null,
            null,
            null
          )
        );
        Amount cost = settings.getData().tarification() == TARIFICATION.PER_LINK
          ? new Amount(
            Optional.ofNullable(settings.getData())
              .map(it -> it.songRequestCost())
              .orElse(100),
            0,
            "RUB"
          )
          : new Amount(
            (int) (duration *
              Optional.ofNullable(settings.getData())
                .map(it -> it.songRequestCost())
                .orElse(100) / 60),
            0,
            "RUB"
          );
        return new PrepareVideoResponse(
          id,
          videoId,
          "youtube",
          "https://www.youtube.com/watch?v=%s".formatted(videoId),
          Optional.ofNullable(snippet.title()).orElse(""),
          Optional.ofNullable(snippet.thumbnails())
            .map(it -> it.get("default"))
            .map(it -> it.url())
            .orElse(""),
          cost
        );
      });
  }

  @Serdeable
  public static record PrepareVideoCommand(String url, String recipientId) {}
}
