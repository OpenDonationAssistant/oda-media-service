package io.github.opendonationassistant.media.video.prepared;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.history.HistoryFacade;
import io.github.opendonationassistant.events.history.event.MediaHistoryEvent;
import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.media.repository.VideoDataRepository;
import io.github.opendonationassistant.media.senders.ReadyVideoNotificationSender;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class PreparedVideo {

  private static final ODALogger log = new ODALogger(PreparedVideo.class);
  private VideoData data;
  private final VideoDataRepository repository;
  private final ReadyVideoNotificationSender notificationSender;
  private final HistoryFacade historyFacade;

  public PreparedVideo(
    VideoData data,
    VideoDataRepository repository,
    ReadyVideoNotificationSender notificationSender,
    HistoryFacade historyFacade
  ) {
    this.data = data;
    this.repository = repository;
    this.notificationSender = notificationSender;
    this.historyFacade = historyFacade;
  }

  public void linkPayment(String paymentId) {
    this.data = data.withPaymentId(paymentId);
    this.save();
  }

  public void save() {
    log.info("Saving prepared video", Map.of("video", data));
    repository.update(data);
  }

  public ReadyVideo makeReady(
    String owner,
    String recipient,
    @Nullable String paymentId
  ) {
    log.info("Make video ready", Map.of("id", data.id()));
    var video = new ReadyVideo(
      data.withOwner(owner).withRecipientId(recipient).withStatus("ready"),
      repository
    );
    video.save();
    historyFacade
      .sendEvent(
        new MediaHistoryEvent(
          "payment",
          paymentId,
          recipient,
          video.data().id(),
          video.data().url(),
          video.data().title(),
          video.data().thumbnail()
        )
      )
      .thenComposeAsync(it -> {
        log.info(
          "Send notification to recipient",
          Map.of("recipient", recipient, "video", video.data())
        );
        return notificationSender
          .send("/topic/%smedia".formatted(recipient), video.data())
          .thenAccept(ignore ->
            log.info("Notification sent", Map.of("recipient", recipient))
          );
      })
      .join();
    return video;
  }
}
