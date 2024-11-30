package io.github.stcarolas.oda.media.youtube;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class YouTubeStubs {

  public static void successListResponse() {
    stubFor(
      get(urlPathEqualTo("/youtube/v3/videos"))
        .willReturn(
          jsonResponse(
            """
            {
              "kind": "youtube#videoListResponse",
              "etag": "Vduy691-XvbF15_8ufUJXuqm8BM",
              "items": [
                {
                  "kind": "youtube#video",
                  "etag": "UYukw5vAFQXG5YX2d2TSeyL7NAw",
                  "id": "z4FWUtsni7g",
                  "snippet": {
                    "title": "Beautiful Medieval Fantasy Tavern, Medieval Inn | Fantasy Music and Ambience Cozy",
                    "thumbnails": {
                      "default": {
                        "url": "https://i.ytimg.com/vi/z4FWUtsni7g/default.jpg",
                        "width": 120,
                        "height": 90
                      }
                    },
                    "channelTitle": "Lord of Abundance",
                    "tags": [
                      "medieval music",
                      "celtic music",
                      "fantasy music",
                      "emotional music",
                      "nordic music",
                      "traditional music",
                      "relaxing music",
                      "world music",
                      "traditional medieval music",
                      "medieval rpg music",
                      "medieval music instrumental",
                      "beautiful medieval music",
                      "celtic music instrumental",
                      "medieval instrumental music",
                      "celtic fantasy music",
                      "medieval folk music",
                      "celtic tavern music",
                      "study music",
                      "background music",
                      "medieval inn music",
                      "medieval",
                      "celtic",
                      "fantasy",
                      "celtic medieval music",
                      "folk music",
                      "tavern music"
                    ],
                    "categoryId": "10",
                    "liveBroadcastContent": "none",
                    "defaultLanguage": "en",
                    "localized": {
                      "title": "Beautiful Medieval Fantasy Tavern, Medieval Inn | Fantasy Music and Ambience Cozy"
                    },
                    "defaultAudioLanguage": "en-US"
                  },
                  "contentDetails": {
                    "duration": "PT10H33M23S",
                    "dimension": "2d",
                    "definition": "hd",
                    "caption": "false",
                    "licensedContent": true,
                    "contentRating": {},
                    "projection": "rectangular"
                  },
                  "statistics": {
                    "viewCount": "206923",
                    "likeCount": "4367",
                    "favoriteCount": "0",
                    "commentCount": "69"
                  }
                }
              ],
              "pageInfo": {
                "totalResults": 1,
                "resultsPerPage": 1
              }
            }
                          """,
            200
          )
        )
    );
  }
}
