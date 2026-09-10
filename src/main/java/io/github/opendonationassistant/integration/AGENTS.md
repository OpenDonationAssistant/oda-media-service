# INTEGRATION MODULE

External API clients for YouTube Data v3 and VK Video.

## STRUCTURE

```
integration/
├── youtube/                     # YouTube Data API v3 client
│   ├── YouTube.java             # High-level facade
│   ├── YouTubeLowLevelApi.java  # HTTP client interface (@Client)
│   └── *.java                   # Data model records (SearchResult, Video, etc.)
└── vk/                          # VK Video API client
    ├── VKApi.java               # HTTP client interface (@Client)
    └── VK.java                  # Empty (dead code)
```

## WHERE TO LOOK

| Task | File | Notes |
|------|------|-------|
| Add YouTube endpoint | `YouTube.java` | Facade methods, delegates to low-level API |
| Add VK endpoint | `VKApi.java` | Add method to @Client interface |
| Add data model | `youtube/` | Create record with `@Serdeable` |
| Modify HTTP client | `youtube/YouTubeLowLevelApi.java` | Micronaut @Client interface |

## CONVENTIONS

- **Two-layer pattern**: `YouTube.java` (facade) → `YouTubeLowLevelApi.java` (HTTP)
- **Data models are records**: Use Java records with `@Serdeable` for JSON binding
- **VK is minimal**: Only `getOembed` method exists
- **WireMock for tests**: Stub helpers in `src/test/java/.../youtube/YouTubeStubs.java`

## ANTI-PATTERNS

- **Dead code**: `VK.java` is empty (3 lines) - can be deleted
- **No VK facade**: VK has no high-level wrapper like YouTube does

## EXTERNAL APIS

| API | Base URL | Auth | Rate Limits |
|-----|----------|------|-------------|
| YouTube Data v3 | `https://youtube.googleapis.com` | API key (`YOUTUBE_KEY`) | 10,000 units/day |
| VK Video | `/method/video.getOembed` | None (public) | Unknown |
