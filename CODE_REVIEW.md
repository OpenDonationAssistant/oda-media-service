# Code Review: oda-media-service

**Date:** 2026-10-29  
**Reviewer:** Automated Code Review  
**Service:** Micronaut 5 microservice (Java 25) for YouTube/VK video song requests  
**Scope:** Full service review covering architecture, domain model, controllers, repositories, integrations, configuration, tests

---

## Executive Summary

The service follows a reasonable CQRS-like structure with Micronaut, RabbitMQ, and PostgreSQL/Flyway. The state pattern for videos is conceptually sound. The highest-priority remaining issues are:

1. **Code duplication** - URL parsing logic duplicated between `PrepareVideo` and `VideoRepository`
2. **Dead code** - `integration/vk/VK.java` is empty

---

## Architecture & Structure

### Strengths
- **Clear module organization**: `media/`, `playlist/`, `settings/`, `integration/` with well-defined responsibilities
- **CQRS separation**: Commands in `commands/` packages, queries in controller root/view packages
- **State pattern**: Video lifecycle (prepared → ready → handled) modeled as separate classes
- **Micronaut best practices**: Proper use of DI, `@Factory`, `@Controller`, security annotations
- **Database migrations**: Well-versioned Flyway migrations (V1-V9)
- **OpenAPI documentation**: Annotations present on endpoints
- **NullAway enforcement**: Compile-time null-safety checking

### Concerns
- **Package mismatch**: Production code in `io.github.opendonationassistant.*`, tests in `io.github.stcarolas.oda.media.*` (and some in `io.github.opendonationassistant.*`) - inconsistent and confusing
- **DTO name collision**: `media/dto/Playlist.java` (simple DTO) vs `playlist/Playlist.java` (full domain entity) - same name, different purposes
- **Heavy constructor coupling**: `PreparedVideo` takes many dependencies (notificationSender, historyFacade, repository) making it hard to test
- **Repository as domain layer**: `VideoRepository` contains significant business logic (validation, parsing, API calls) - mixes concerns

---

## High Priority Issues

### 1. Duplicate URL Parsing [MAINTAINABILITY]
**Locations:** `media/commands/PrepareVideo.java:38-44, 198-206`, `media/repository/VideoRepository.java`

Both `PrepareVideo` (HTTP endpoint) and `VideoRepository.create()` contain:
- YouTube regex/pattern for URL parsing
- VK URL detection and scraping logic
- Similar validation logic

The media module AGENTS.md explicitly calls this out as an anti-pattern. This creates maintenance burden and risk of inconsistent behavior.

**Recommendation:** Consolidate URL parsing into a shared utility/service. Either:
- Move all parsing to `VideoRepository` and have `PrepareVideo` delegate
- Create a `VideoUrlParser` utility class
- Have `PrepareVideo` call `VideoRepository.create()` for consistency

### 2. Dead Code
**Location:** `integration/vk/VK.java` - completely empty class (3 lines). Should be deleted per integration AGENTS.md.

### 3. NPE Risks in VK Parsing
**Locations:** `media/commands/PrepareVideo.java:131-143`, `media/repository/VideoRepository.java`

```java
var matcher = srcPattern.matcher(
  embeddedInfo.html().replaceAll("\\n", "")  // html() could be null
);
...
String src = matcher.group(1);
// ...
if (!settings.passWordsBlacklist(embeddedInfo.title())) { ... } // title() could be null
```

No null checks on `embeddedInfo.html()` or `embeddedInfo.title()`. Also `srcPattern.matcher()` called without null-safety.

**Fix:** Add null checks and defensive programming.

### 4. NPE in PlaylistController.get()
**Location:** `playlist/view/PlaylistController.java:150-158, 165-167`

```java
while (Objects.nonNull(nextPage)) {
  page = youTube.playlistItems(playlistId, nextPage);
  nextPage = page.nextPageToken();
  videos.addAll(page.items()); // page.items() could be null -> NPE
}
...
String videoId = Optional.ofNullable(snippet.resourceId())
  .map(resourceId -> resourceId.videoId())
  .orElseThrow(); // No message, unclear exception
```

**Fix:** Add null checks:
```java
if (page.items() != null) {
  videos.addAll(page.items());
}
```

And provide a meaningful exception message.

### 5. Error Handling Issues

#### HistoryItemEventHandler batch failure
**Location:** `media/listeners/handlers/HistoryItemEventHandler.java`

When processing payment events, if any `video.makeReady()` fails (throws exception), the entire batch fails because `join()` propagates the exception and there's no try/catch around individual videos. This could block processing all videos for a payment if one is invalid.

**Recommendation:** Wrap each video processing in try/catch and log failures individually.

#### AddMediaCommandHandler exception handling
Catches all exceptions, logs context, increments metric, rethrows as IOException. The `log.clear()` call is suspicious - might be clearing thread-local context. Also loses specific exception types.

---

## Medium Priority Issues

### 6. Inconsistent Null Handling
- `PreparedVideo.makeReady()` uses `@Nullable` annotations inconsistently
- Some code uses `Optional`, others does manual null checks
- `VideoRepository.createFromYoutube()` defaults duration/viewCount to 0 on parse errors - could allow invalid videos through
- `MediaSettings.get()` returns `Optional` but usage patterns vary

### 7. Logging Issues
- `PreparedVideo.save()` logs full data at INFO level: `log.info("Saved prepared video {}", video.data());` - could leak sensitive info
- Excessive debug logging in playlist fetching
- `log.clear()` in exception handlers is unusual

### 8. Blocking in Async Chains
**Location:** `media/video/prepared/PreparedVideo.java`

```java
notificationSender.send(...).join();
```

Calling `.join()` on a CompletableFuture blocks the thread. In an async context, this defeats the purpose and could cause thread pool exhaustion. Should be handled asynchronously or use `thenAccept`/`thenRun`.

### 9. Type Safety Issues
- Video status stored as raw String ("prepared", "ready", "handled") instead of enum
- `TARIFICATION` is an enum (good) but status isn't
- Magic strings throughout codebase

### 10. Missing Validation
- No guards in state transitions (e.g., can't make ready an already-ready video)
- `VideoRepository.findPreparedVideo()` filters by status, but direct state object manipulation bypasses checks
- URL format validation could be more robust

---

## Low Priority / Code Quality

### 11. Code Style
- Inconsistent use of `var` (some places use it, others don't)
- Some methods are very long (e.g., `PrepareVideo.prepareYoutube()` is ~160 lines)
- Missing JavaDoc on public APIs
- `VKApi` only has `getOembed` method - minimal implementation

### 12. Tests
**Location:** `src/test/java/io/github/stcarolas/oda/media/`

- `PrepareVideoTest.testPrepareVideoWithVkUrl()` is `@Disabled`
- Test package structure different from main package (confusing)
- WireMock 4.0.0-beta.30 used - noted as potentially unstable in AGENTS.md
- Some tests use `@MicronautTest(environments = "allinone")` correctly
- Good use of WireMock stubs

### 13. Configuration
- `logback.xml` has references to old package `io.github.stcarolas` (noted in root AGENTS anti-patterns) - should be updated to `io.github.opendonationassistant`
- Dockerfile expects pre-built native binary (correct for native-image packaging)

---

## Specific File Reviews

### Application.java
**Status:** Good
- Clean entry point
- Proper RabbitMQ binding setup
- Correct default environment configuration
- OpenAPI setup present

### VideoController.java
**Status:** Mostly good
- Proper security annotations
- Good use of CompletableFuture for async operations
- Owner check in authenticated endpoints
- `@Patch("{id}")` marks video as handled without auth check - intentional? Comment says "anonymous" allowed, but marking videos as handled should probably be restricted or have some protection
- `get()` by comma-separated IDs allows anonymous access and doesn't check ownership - could leak video data

### PlaylistController.java
**Status:** Issues found
- NPE risk in pagination
- Anonymous access to YouTube playlist fetching is reasonable for public playlists

### PrepareVideo.java
**Status:** Functional but has issues
- Duplicates parsing logic from VideoRepository
- Good validation (blacklist, duration, views, age restriction)
- Proper error responses with Problem Details
- Tarification logic (per-link vs per-minute) is reasonable
- Could be refactored for maintainability

### MediaSettings.java
**Status:** Mostly good
- Widget config application logic is complex but seems reasonable
- Properly sends config commands to payment page

### UpdatePlaylist.java
**Status:** Good
- Correctly checks ownership (line 40)
- Returns 401 on unauthorized/missing
- Note: playlist AGENTS says "UpdatePlaylist.update() has `// todo check owner id`" but it's actually implemented now

---

## Recommendations

### Short-term (High)
1. **Remove or fix dead code** - delete `integration/vk/VK.java`
2. **Eliminate URL parsing duplication** - refactor to single source of truth
3. **Add null checks** in VK parsing and playlist pagination
4. **Fix batch error handling** in `HistoryItemEventHandler`
5. **Update logback.xml** to use correct package name

### Medium-term
6. **Refactor VideoRepository** - move business logic to appropriate places, reduce duplication
7. **Improve type safety** - use enum for video status
8. **Make async properly** - remove `.join()` blocking calls
9. **Add guards to state transitions**
10. **Fix test package structure** for consistency
11. **Re-enable or fix disabled tests**

---

## Conclusion

The service is functional and follows many good practices (Micronaut patterns, state machine concept, proper DI, migrations). No critical issues remain; the outstanding work is high- and medium-priority technical debt.

The codebase is generally well-structured, but technical debt exists around duplication and some architectural choices (repository containing business logic, type-unsafe status strings). With the recommended fixes, this would be a solid, maintainable service.
