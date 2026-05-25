package io.github.opendonationassistant.media.repository;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.history.HistoryFacade;
import io.github.opendonationassistant.integration.vk.VKApi;
import io.github.opendonationassistant.integration.youtube.ContentDetails;
import io.github.opendonationassistant.integration.youtube.Statistics;
import io.github.opendonationassistant.integration.youtube.Video;
import io.github.opendonationassistant.integration.youtube.YouTube;
import io.github.opendonationassistant.media.senders.ReadyVideoNotificationSender;
import io.github.opendonationassistant.media.video.HandledVideo;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import io.github.opendonationassistant.settings.repository.MediaSettings;
import io.github.opendonationassistant.settings.repository.MediaSettingsRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

@Singleton
public class VideoRepository {

  private static final String YOUTUBE_REGEX =
    "^((?:https?:)?\\/\\/)?((?:www|m)\\.)?((?:youtube(-nocookie)?\\.com|youtu.be))(\\/(?:[\\w\\-]+\\?v=|embed\\/|live\\/|v\\/)?)([\\w\\-]+)(\\S+)?$";

  private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
    YOUTUBE_REGEX,
    Pattern.MULTILINE
  );

  private static final Pattern SRC_PATTERN = Pattern.compile(
    ".*src=\\\"(.*?)\\\".*",
    Pattern.MULTILINE
  );

  private final ODALogger log = new ODALogger(this);
  private final VideoDataRepository dataRepository;
  private final ReadyVideoNotificationSender notificationSender;
  private final HistoryFacade historyFacade;
  private final YouTube youTube;
  private final VKApi vk;
  private final MediaSettingsRepository repository;

  @Inject
  public VideoRepository(
    VideoDataRepository dataRepository,
    ReadyVideoNotificationSender notificationSender,
    HistoryFacade historyFacade,
    YouTube youTube,
    VKApi vk,
    MediaSettingsRepository repository
  ) {
    this.dataRepository = dataRepository;
    this.notificationSender = notificationSender;
    this.historyFacade = historyFacade;
    this.youTube = youTube;
    this.vk = vk;
    this.repository = repository;
  }

  public Optional<PreparedVideo> findPreparedVideo(@Nullable String id) {
    return Optional.ofNullable(id)
      .flatMap(dataRepository::findById)
      .filter(it -> "prepared".equals(it.status()))
      .map(data ->
        new PreparedVideo(
          data,
          dataRepository,
          notificationSender,
          historyFacade
        )
      );
  }

  public CompletableFuture<List<PreparedVideo>> findPreparedVideosForPayment(
    @Nullable String paymentId
  ) {
    if (paymentId == null) {
      return CompletableFuture.completedFuture(List.of());
    }
    log.debug(
      "Getting prepared videos for payment",
      Map.of("paymentId", paymentId)
    );
    return CompletableFuture.supplyAsync(() -> {
      var videos = dataRepository.findByPaymentId(paymentId);
      log.debug(
        "Found videos for payment",
        Map.of("paymentId", paymentId, "videos", videos)
      );
      return videos
        .stream()
        .map(data -> {
          log.debug(
            "Found prepared video for payment",
            Map.of("paymentId", paymentId, "video", data)
          );
          return new PreparedVideo(
            data,
            dataRepository,
            notificationSender,
            historyFacade
          );
        })
        .toList();
    });
  }

  public Optional<ReadyVideo> findReadyVideo(@Nullable String id) {
    return Optional.ofNullable(id)
      .flatMap(dataRepository::findById)
      .filter(it -> "ready".equals(it.status()))
      .map(data -> new ReadyVideo(data, dataRepository));
  }

  public CompletableFuture<Long> countReadyVideosForRecipientId(
    String recipientId
  ) {
    return dataRepository.countByRecipientIdAndStatus(
      recipientId,
      "ready"
    );
  }

  public CompletableFuture<List<ReadyVideo>> findReadyVideosForRecipientId(
    String recipientId
  ) {
    return dataRepository
      .findByRecipientIdAndStatusOrderByReadyTimestamp(recipientId, "ready")
      .thenApply(videos ->
        videos
          .stream()
          .map(data -> new ReadyVideo(data, dataRepository))
          .toList()
      );
  }

  public CompletableFuture<Page<HandledVideo>> findHandledVideosForRecipientId(
    String recipientId,
    Pageable pageable
  ) {
    return dataRepository
      .findByRecipientIdAndStatusOrderByReadyTimestamp(
        recipientId,
        "handled",
        pageable
      )
      .thenApply(videos ->
        videos.map(data -> new HandledVideo(data, dataRepository))
      );
  }

  public CompletableFuture<VideoData> create(String recipientId, String url) {
    log.info("Creating video", Map.of("url", url, "recipientId", recipientId));
    final MediaSettings settings = repository.getByRecipientId(recipientId);
    if (url.contains("vkvideo.ru")) {
      return createFromVk(settings, url);
    }
    return createFromYoutube(settings, url);
  }

  private CompletableFuture<VideoData> createFromVk(
    MediaSettings settings,
    String url
  ) {
    if (!settings.getData().vkvideoEnabled()) {
      return CompletableFuture.failedFuture(
        new IllegalArgumentException("VK Video requests are disabled")
      );
    }
    var id = Generators.timeBasedEpochGenerator().generate().toString();
    var originId = url.replaceAll("https://vkvideo.ru/video", "");
    return vk
      .getEmbeddedInfo(Map.of("url", url, "v", "5.199"))
      .thenApply(response -> response.response())
      .thenApply(info -> {
        log.debug("Got vk video info", Map.of("info", info));
        if (!settings.passWordsBlacklist(info.title())) {
          throw new IllegalArgumentException(
            "Video contains blacklisted words"
          );
        }
        var matcher = SRC_PATTERN.matcher(info.html().replaceAll("\\n", ""));
        if (!matcher.matches()) {
          throw new IllegalArgumentException("Unable to parse video");
        }
        return new VideoData(
          id,
          originId,
          "vk",
          matcher.group(1),
          info.title(),
          info.thumbnailUrl(),
          "prepared",
          null,
          null,
          null,
          null,
          "ODA"
        );
      })
      .thenApply(dataRepository::save);
  }

  private CompletableFuture<VideoData> createFromYoutube(
    MediaSettings settings,
    String url
  ) {
    if (!settings.getData().youtubeEnabled()) {
      return CompletableFuture.failedFuture(
        new IllegalArgumentException("YouTube requests are disabled")
      );
    }
    var id = Generators.timeBasedEpochGenerator().generate().toString();

    if (url == null || url.isBlank()) {
      return CompletableFuture.failedFuture(
        new IllegalArgumentException("URL is empty")
      );
    }
    Matcher matcher = YOUTUBE_PATTERN.matcher(url);
    if (!matcher.matches()) {
      return CompletableFuture.failedFuture(
        new IllegalArgumentException("Invalid YouTube URL")
      );
    }
    String videoId = matcher.group(6);

    return youTube
      .list(videoId)
      .thenApply(found -> {
        log.debug(
          "YouTube video search result",
          Map.of("videoId", videoId)
        );
        if (found.items() == null || found.items().isEmpty()) {
          throw new IllegalArgumentException("Video not found on YouTube");
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
          throw new IllegalArgumentException("Video is too long");
        }

        var viewCount = Optional.ofNullable(video.statistics())
          .map(Statistics::viewCount)
          .map(Long::parseLong)
          .orElse(0L);
        if (viewCount < settings.minViewAmount()) {
          throw new IllegalArgumentException("Video has too few views");
        }

        if (
          Optional.ofNullable(video.snippet())
            .map(it -> it.title())
            .filter(title -> settings.passWordsBlacklist(title))
            .isEmpty()
        ) {
          throw new IllegalArgumentException(
            "Video contains blacklisted words"
          );
        }

        if (
          Optional.ofNullable(video.contentDetails())
            .map(ContentDetails::contentRating)
            .map(it -> it.get("ytRating"))
            .map(it -> "ytAgeRestricted".equals(it))
            .orElse(false)
        ) {
          throw new IllegalArgumentException(
            "Video must not be age-restricted"
          );
        }

        var snippet = video.snippet();
        if (snippet == null) {
          throw new IllegalArgumentException("Invalid video");
        }

        return new VideoData(
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
          null,
          "ODA"
        );
      })
      .thenApply(dataRepository::save);
  }
}
