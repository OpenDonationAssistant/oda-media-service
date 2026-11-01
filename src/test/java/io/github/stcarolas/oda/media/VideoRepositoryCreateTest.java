package io.github.stcarolas.oda.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.events.history.HistoryFacade;
import io.github.opendonationassistant.integration.vk.VKApi;
import io.github.opendonationassistant.integration.youtube.Snippet;
import io.github.opendonationassistant.integration.youtube.Statistics;
import io.github.opendonationassistant.integration.youtube.Video;
import io.github.opendonationassistant.integration.youtube.Videos;
import io.github.opendonationassistant.integration.youtube.YouTube;
import io.github.opendonationassistant.media.repository.VideoData;
import io.github.opendonationassistant.media.repository.VideoDataRepository;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.github.opendonationassistant.media.senders.ReadyVideoNotificationSender;
import io.github.opendonationassistant.media.video.HandledVideo;
import io.github.opendonationassistant.media.video.prepared.PreparedVideo;
import io.github.opendonationassistant.media.video.ready.ReadyVideo;
import io.github.opendonationassistant.settings.repository.MediaSettings;
import io.github.opendonationassistant.settings.repository.MediaSettingsData;
import io.github.opendonationassistant.settings.repository.MediaSettingsData.TARIFICATION;
import io.github.opendonationassistant.settings.repository.MediaSettingsRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class VideoRepositoryCreateTest {

  private final VideoDataRepository dataRepository = mock(
    VideoDataRepository.class
  );
  private final YouTube youTube = mock(YouTube.class);
  private final MediaSettingsRepository settingsRepository = mock(
    MediaSettingsRepository.class
  );

  private VideoRepository repository() {
    return new VideoRepository(
      dataRepository,
      mock(ReadyVideoNotificationSender.class),
      mock(HistoryFacade.class),
      youTube,
      mock(VKApi.class),
      settingsRepository
    );
  }

  @Test
  public void createWithNullUrlFailsWithoutNpe() {
    assertThrows(
      CompletionException.class,
      () -> repository().create("media-1", "recipient", null).join()
    );
  }

  @Test
  public void createWithBlankUrlFailsWithoutNpe() {
    assertThrows(
      CompletionException.class,
      () -> repository().create("media-1", "recipient", "   ").join()
    );
  }

  @Test
  public void createUsesSuppliedIdInsteadOfGeneratingOne() {
    stubYoutubeVideo();

    var created = repository()
      .create("supplied-id", "recipient", "https://youtu.be/kXYiU_JCYtU")
      .join();

    var saved = ArgumentCaptor.forClass(VideoData.class);
    verify(dataRepository).save(saved.capture());
    assertEquals("supplied-id", saved.getValue().id());
    assertEquals("supplied-id", created.data().id());
  }

  @Test
  public void findByIdMapsStoredStatusToVideoState() {
    when(dataRepository.findById("prepared")).thenReturn(
      Optional.of(videoData("prepared", "prepared"))
    );
    when(dataRepository.findById("ready")).thenReturn(
      Optional.of(videoData("ready", "ready"))
    );
    when(dataRepository.findById("handled")).thenReturn(
      Optional.of(videoData("handled", "handled"))
    );

    var repository = repository();

    assertInstanceOf(
      PreparedVideo.class,
      repository.findById("prepared").orElseThrow()
    );
    assertInstanceOf(
      ReadyVideo.class,
      repository.findById("ready").orElseThrow()
    );
    assertInstanceOf(
      HandledVideo.class,
      repository.findById("handled").orElseThrow()
    );
  }

  @Test
  public void findByIdIsEmptyWhenVideoDoesNotExist() {
    when(dataRepository.findById("missing")).thenReturn(Optional.empty());

    assertTrue(repository().findById("missing").isEmpty());
  }

  private VideoData videoData(String id, String status) {
    return new VideoData(
      id,
      "origin",
      "youtube",
      "https://youtu.be/" + id,
      "title",
      "thumbnail",
      status,
      null,
      "recipient",
      null,
      null,
      "ODA"
    );
  }

  private void stubYoutubeVideo() {
    var data = new MediaSettingsData(
      "settings-1",
      "recipient",
      100,
      TARIFICATION.PER_LINK,
      60,
      12,
      100,
      true,
      true,
      true,
      "",
      List.of()
    );
    var settings = mock(MediaSettings.class);
    when(settings.getData()).thenReturn(data);
    when(settings.minViewAmount()).thenReturn(100);
    when(settings.passWordsBlacklist(any())).thenReturn(true);
    when(settingsRepository.getByRecipientId("recipient")).thenReturn(settings);
    when(youTube.list("kXYiU_JCYtU")).thenReturn(
      CompletableFuture.completedFuture(
        new Videos(
          List.of(
            new Video(
              "kXYiU_JCYtU",
              new Snippet(null, "title", new HashMap<>()),
              null,
              new Statistics("100")
            )
          )
        )
      )
    );
    when(dataRepository.save(any(VideoData.class))).thenAnswer(invocation ->
      invocation.getArgument(0)
    );
  }
}
