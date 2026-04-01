package io.github.stcarolas.oda.media;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.util.Map;

public class VKStubs {

  public static void successVideoOembedResponse() {
    stubFor(
      post(urlPathEqualTo("/method/video.getOembed"))
        .withQueryParams(
          Map.of(
            "url",
            equalTo("https://vkvideo.ru/video-165282829_456239200"),
            "v",
            equalTo("5.199")
          )
        )
        .willReturn(
          jsonResponse(
            """
            {
              "response": {
                "version": "1.0",
                "type": "video",
                "html": "<iframe\\n  src=\\"https://vk.ru/video_ext.php?oid=-165282829&id=456239200&hash=12174e6476592e14&__ref=vk.web2\\"\\n  width=\\"853\\"\\n  height=\\"480\\"\\n  allow=\\"autoplay; encrypted-media; fullscreen; picture-in-picture; screen-wake-lock;\\"\\n  frameborder=\\"0\\"\\n  allowfullscreen></iframe>",
                "title": "Linux by Rebrai: Linux from scratch — 04",
                "author_name": "DevOps by REBRAIN",
                "width": 853,
                "height": 480,
                "provider_name": "VK Video",
                "provider_url": "https://vkvideo.ru/",
                "thumbnail_url": "https://sun9-40.vkuserphoto.ru/impg/L_vMRvQzn3TV1jqW7WwXSjhB1rG_vnwWL-RG9Q/slK-fO1khro.jpg?size=130x96&quality=95&keep_aspect_ratio=1&background=000000&sign=662921cba38dc8c2097317196482451f&c_uniq_tag=2_iqSFggQkl5Tvh737F0yHOPnU72qBvBENmAdOzR3ik&type=video_thumb",
                "thumbnail_width": 130,
                "thumbnail_height": 96
              }
            }
            """,
            200
          )
        )
    );
  }
}
