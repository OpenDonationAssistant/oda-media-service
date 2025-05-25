package io.github.opendonationassistant.media.vk;

import static io.micronaut.http.HttpHeaders.CONTENT_TYPE;

import io.micronaut.http.annotation.Header;
import io.micronaut.http.client.annotation.Client;

@Client(id = "vk")
@Header(name = CONTENT_TYPE, value = "application/json")
public interface VKApi {}
