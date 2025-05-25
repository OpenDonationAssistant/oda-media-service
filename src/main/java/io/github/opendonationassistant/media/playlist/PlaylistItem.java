package io.github.opendonationassistant.media.playlist;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record PlaylistItem(String title,String src){}
