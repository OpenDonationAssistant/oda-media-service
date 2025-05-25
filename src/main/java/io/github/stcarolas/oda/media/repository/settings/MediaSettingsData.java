package io.github.stcarolas.oda.media.repository.settings;

import io.github.opendonationassistant.commons.StringListConverter;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import java.util.List;

@Serdeable
@MappedEntity("settings")
public record MediaSettingsData(
  @Id @MappedProperty("id") String id,
  @MappedProperty("recipient_id") String recipientId,
  @MappedProperty("song_request_cost") @Nullable Integer songRequestCost,
  @MappedProperty("max_amount") Integer maxAmount,
  @MappedProperty("request_view_amount") Integer requestViewAmount,
  @MappedProperty("requests_enabled") Boolean requestsEnabled,
  @MappedProperty("youtube_enabled") Boolean youtubeEnabled,
  @MappedProperty("vkvideo_enabled") Boolean vkvideoEnabled,
  @MappedProperty("request_tooltip") String requestTooltip,
  @MappedProperty(
    value = "wordsBlacklist",
    converter = StringListConverter.class
  )
  List<String> wordsBlacklist
) {}
