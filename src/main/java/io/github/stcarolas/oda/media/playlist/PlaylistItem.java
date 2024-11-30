package io.github.stcarolas.oda.media.playlist;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record PlaylistItem(String title,String src){}
