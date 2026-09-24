package io.github.stcarolas.oda.media;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import io.github.opendonationassistant.events.history.HistoryFacade;
import io.github.opendonationassistant.integration.vk.VKApi;
import io.github.opendonationassistant.integration.youtube.YouTube;
import io.github.opendonationassistant.media.repository.VideoDataRepository;
import io.github.opendonationassistant.media.repository.VideoRepository;
import io.github.opendonationassistant.media.senders.ReadyVideoNotificationSender;
import io.github.opendonationassistant.settings.repository.MediaSettingsRepository;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

public class VideoRepositoryCreateTest {

  private VideoRepository repository() {
    return new VideoRepository(
      mock(VideoDataRepository.class),
      mock(ReadyVideoNotificationSender.class),
      mock(HistoryFacade.class),
      mock(YouTube.class),
      mock(VKApi.class),
      mock(MediaSettingsRepository.class)
    );
  }

  @Test
  public void createWithNullUrlFailsWithoutNpe() {
    assertThrows(
      CompletionException.class,
      () -> repository().create("recipient", null).join()
    );
  }

  @Test
  public void createWithBlankUrlFailsWithoutNpe() {
    assertThrows(
      CompletionException.class,
      () -> repository().create("recipient", "   ").join()
    );
  }
}
