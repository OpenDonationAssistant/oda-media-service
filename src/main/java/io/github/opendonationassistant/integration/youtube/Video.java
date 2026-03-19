package io.github.opendonationassistant.integration.youtube;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Serdeable
@Schema(description = "YouTube video data")
public record Video(
  @Nullable @Schema(description = "Video ID") String id,
  @Nullable @Schema(description = "Video snippet with title, description, thumbnails") Snippet snippet,
  @Nullable @Schema(description = "Content details like duration") ContentDetails contentDetails,
  @Nullable @Schema(description = "Video statistics (views, likes, etc.)") Statistics statistics
) {}
