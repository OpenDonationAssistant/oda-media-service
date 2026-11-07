package io.github.opendonationassistant.media.listeners.handlers;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.media.metrics.MediaMetrics;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.github.opendonationassistant.media.video.HasVideoData;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.github.opendonationassistant.rabbit.Exchange;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.zalando.problem.Problem;

@RabbitListener
public class AddMediaCommandHandler {

  public static final String QUEUE_NAME = "media.add";
  public static final io.github.opendonationassistant.rabbit.Queue QUEUE =
    new io.github.opendonationassistant.rabbit.Queue(QUEUE_NAME);
  public static final List<Exchange> BINDINGS = List.of(
    Exchange.Exchange("rpc", Map.of("media.add", QUEUE))
  );

  private static final Pattern VIDEO_URL_PATTERN = Pattern.compile(
    "\\S*(?:youtu\\.?be|vkvideo\\.ru)\\S*",
    Pattern.CASE_INSENSITIVE
  );

  private static final Pattern TRAILING_PUNCTUATION_PATTERN = Pattern.compile(
    "[,.;:!?)\\]}>\"']+$"
  );

  private final ODALogger log = new ODALogger(this);
  private final VideoRepository videoRepository;
  private final MediaMetrics metrics;

  @Inject
  public AddMediaCommandHandler(
    VideoRepository videoRepository,
    MediaMetrics metrics
  ) {
    this.videoRepository = videoRepository;
    this.metrics = metrics;
  }

  @Queue(QUEUE_NAME)
  public AddMediaResponse handle(AddMediaRequest request) {
    metrics.mediaRequested(request.system());

    // TODO temporary
    var mediaId = Generators.timeBasedEpochGenerator().generate().toString();
    // if (request.id() == null || request.id().isBlank()) {
    //   metrics.mediaFailed(request.system());
    //   return new AddMediaResponse(null, "Media id is empty");
    // }

    final var url = extractVideoUrl(request.url());
    try {
      HasVideoData video = videoRepository
        .findById(mediaId)
        .orElseGet(() ->
          videoRepository.create(mediaId, request.recipientId(), url).join()
        );
      if (video instanceof PreparedVideo preparedVideo) {
        video = preparedVideo.makeReady(
          request.requester(),
          request.recipientId(),
          null
        );
        metrics.mediaAdded(request.system());
      }
      return new AddMediaResponse(video.data().id(), "");
    } catch (Exception e) {
      metrics.mediaFailed(request.system());
      final var errorMessage = describeError(e);
      log.context(
        Map.of(
          "id",
          request.id(),
          "url",
          Objects.toString(url, ""),
          "recipientId",
          Objects.toString(request.recipientId(), ""),
          "error",
          errorMessage
        )
      );
      log.error("Failed to add media", e);
      log.clear();
      return new AddMediaResponse(null, errorMessage);
    }
  }

  private static String extractVideoUrl(@Nullable String message) {
    if (message == null) {
      return "";
    }
    final var text = message.trim();
    final var matcher = VIDEO_URL_PATTERN.matcher(text);
    if (!matcher.find()) {
      return "";
    }
    return TRAILING_PUNCTUATION_PATTERN.matcher(matcher.group()).replaceAll("");
  }

  private static String describeError(Throwable error) {
    var current = error;
    while (current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    if (current instanceof Problem problem) {
      return Optional.ofNullable(problem.getDetail())
        .filter(detail -> !detail.isBlank())
        .or(() -> Optional.ofNullable(problem.getTitle()))
        .orElse("");
    }
    return "Internal server error";
  }

  @Serdeable
  public static record AddMediaRequest(
    String id,
    String url,
    String requester,
    String recipientId,
    String system
  ) {}

  @Serdeable
  public static record AddMediaResponse(@Nullable String id, String message) {}
}
