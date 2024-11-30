package io.github.stcarolas.oda.notification;

import io.github.opendonationassistant.events.history.Attachment;
import io.github.opendonationassistant.events.history.HistoryCommand;
import io.github.opendonationassistant.events.history.HistoryCommandSender;
import io.github.opendonationassistant.events.history.HistoryItemData;
import io.github.stcarolas.oda.media.video.VideoRepository;
import io.github.stcarolas.oda.media.video.prepared.PreparedVideo;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RabbitListener
public class CompletedPaymentListener {

  private Logger log = LoggerFactory.getLogger(CompletedPaymentListener.class);

  private VideoRepository repository;
  private HistoryCommandSender historyCommandSender;

  @Inject
  public CompletedPaymentListener(
    VideoRepository repository,
    HistoryCommandSender historyCommandSender
  ) {
    this.repository = repository;
    this.historyCommandSender = historyCommandSender;
  }

  // TODO: manual ack
  @Queue("payments")
  public void listen(CompletedPaymentNotification payment) {
    log.info("Received notification: {}", payment);
    List<PreparedVideo> videos = payment
      .getAttachments()
      .stream()
      .map(repository::findById)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .map(PreparedVideo::from)
      .toList();
    videos.forEach(video ->
      video.makeReady(payment.getNickname(), payment.getRecipientId())
    );
    final List<Attachment> attachments = videos
      .stream()
      .map(video -> {
        var attach = new Attachment();
        attach.setId(video.getId());
        attach.setUrl(video.getUrl());
        attach.setTitle(video.getTitle());
        attach.setThumbnail(video.getThumbnail());
        return attach;
      })
      .toList();
    var itemData = new HistoryItemData();
    itemData.setPaymentId(payment.getId());
    itemData.setAttachments(attachments);
    var command = new HistoryCommand();
    command.setType("update");
    command.setPartial(itemData);
    historyCommandSender.send(command);
  }
}
