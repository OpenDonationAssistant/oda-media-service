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

---

## Feature review — micronaut-parent

**Feature:** Bump io.micronaut.platform:micronaut-parent to 5.2.5 across ODA Micronaut services.
**Change set:** `1f62d082e98cc7545bf2ea63ca98dd54a3c2ccf2` → working tree; 2 files changed (+2/−2)
**Reviewer:** independent review subagent (fresh context)
**Verdict:** PASS
**Verification:**
- `mvn -B -o -DskipTests -Dmaven.repo.local=/home/stcarolas/Coding/projects/.tmp-vbump/m2repo clean test-compile` → **BUILD SUCCESS** (33.2 s; 49 main + 15 test sources; `maven-compiler-plugin:3.16.0` in effect, i.e. the 5.2.5 BOM is active; Error Prone/NullAway warnings only, no errors).
- `mvn -B -o -Dmaven.repo.local=… -DargLine=-Duser.home=$PWD/target/fakehome test` (first attempt) → **BUILD FAILURE**, `Tests run: 40, Failures: 0, Errors: 4, Skipped: 12`; all 4 errors are `initializationError » BeanInstantiation … Error configuring data source 'default'. No URL specified` for `PrepareVideoTest`, `SaveAndLoadPlaylistTest`, `VideoControllerTest`, `YouTubeTest`. Root cause is environmental, not the code: `mn:start-testresources-service` logged `The following artifacts could not be resolved: io.micronaut.testresources:micronaut-test-resources-oauth2:jar:4.3.1 (absent) … offline mode`, so the Test Resources server never started and no DataSource was provisioned (test-resources moves 4.1.0 → 4.3.1 in 5.2.5 and the pre-warmed repo only had 4.1.0).
- `mvn -B -s ~/.m2/settings.xml -Dmaven.repo.local=… -DargLine=-Duser.home=$PWD/target/fakehome test` (online, to fetch the missing 4.3.1 artifacts) → **BUILD SUCCESS**, `Tests run: 43, Failures: 0, Errors: 0, Skipped: 15` (PostgreSQL provisioned by Test Resources via the podman socket — the first class to boot, `YouTubeTest`, spent 48.7 s there; every DB-backed class then passed).
- `mvn -B -o … -DargLine=-Duser.home=$PWD/target/fakehome test` re-run after priming → **BUILD SUCCESS**, `Tests run: 43, Failures: 0, Errors: 0, Skipped: 15` — i.e. the prescribed offline command is green once the 4.3.1 test-resources artifact is cached.
- `mvn -B -o help:evaluate -Dexpression=<prop> -DforceStdout` → `micronaut.core.version = 5.2.22`, `micronaut.data.version = 5.2.4`, `micronaut.serde.version = 3.2.6`, `micronaut.security.version = 5.4.1`, `micronaut.openapi.version = 7.3.0`, `micronaut.testresources.version = 4.3.1` — the new BOM is genuinely in effect.
- `mvn -B -o dependency:list` → resolved `micronaut-data-jdbc:5.2.4`, `micronaut-serde-*:3.2.6`, `micronaut-security-jwt:5.4.1`, `micronaut-problem-json:5.1.0`, `micronaut-sourcegen-*:2.2.4`, `micronaut-test-resources-client:4.3.1`, `micronaut-openapi-annotations:6.3.0`.
- `git status --porcelain` / `git diff --numstat 1f62d08…` → exactly `AGENTS.md` 1/1 and `pom.xml` 1/1; no untracked files, no other edits.

**Findings:**
- **Low** `pom.xml:171` — `io.micronaut.openapi:micronaut-openapi-annotations` stays pinned at `6.3.0` while the 5.2.5 platform manages OpenAPI at `7.3.0` and the annotation-processor path uses `${micronaut.openapi.version}` (`7.3.0`), so the annotations jar now sits two majors behind the processor that consumes it. **Pre-existing** (this change does not touch that line) and currently harmless — sources import only `io.swagger.v3.oas.annotations.*`, never `io.micronaut.openapi.annotation.*`, and compile + spec generation succeed — but the bump widens the skew (6.3.0 vs 7.1.3 before, 7.3.0 now). Suggested fix, separately: drop the explicit `<version>` so the BOM manages it (or align to `7.3.0`) and diff the regenerated spec.
- **Info** `target/classes/META-INF/swagger/oda-media-service-0.10.0.yml` — the OpenAPI document is *generated* by the `micronaut-openapi` processor, which moved `7.1.3 → 7.3.0` with this bump; the release pipeline republishes `@opendonationassistant/oda-media-service-client` from it. Nothing is checked in here that needs updating, but diff the published npm client after the first release in case the processor changed output shape/naming.
- **Info** `.factorypath:2` — the Eclipse/VS Code metadata (gitignored, untracked) still lists `micronaut-*-5.1.x.jar` paths. Pure local IDE state, excluded from git; regenerate the classpath locally, no action for this change.

**Notes:**
- Scope is exactly as intended and minimal: `pom.xml` parent `5.1.0 → 5.2.5` (a single `<parent>` block, no stale `<micronaut.version>` property override existed in this pom to align), and one `AGENTS.md` stack-description line. `README.md` never stated a Micronaut version (env vars and Docker only), so there was nothing to update there — the paywall-style README edit is not applicable to this repo.
- No leftover pins of the old platform anywhere that matters: `.github/workflows/maven.yml` only delegates to `oda-libraries`' reusable workflow (no version), `Dockerfile` copies a pre-built binary on `fedora:41` (no version), `aot-jar.properties`/`aot-native-image.properties` hold no Micronaut version, and there is no `.mvn/maven.config`.
- Compile gates intact: `.mvn/jvm.config` is untouched (11 lines: 9 `--add-exports` covering 8 distinct packages — `com.sun.tools.javac.util` appears twice — plus both `--add-opens`) and the `-Xplugin:ErrorProne … -XepOpt:NullAway:JSpecifyMode=true -Xep:NullAway:ERROR` args are unchanged; Error Prone `2.41.0` and NullAway `0.12.9` stay pinned. The bump produced no new Error Prone errors, only the pre-existing `FutureReturnValueIgnored`/`StringCaseLocaleUsage` warnings in `MediaSettings.java`.
- Pre-existing working-tree dirt for this repo: none (`.tmp-vbump/pre-edit-state.txt` lists no entries under `### oda-media-service`), so the reported +2/−2 is the complete change set.
- Test adequacy for a parent bump: adequate at compile level, and stronger than expected — the DB-backed integration tests (`VideoControllerTest`, `SaveAndLoadPlaylistTest`, `PlaylistAuthorizationTest`, `VideoRepositoryCreateTest`, `PrepareVideoTest`) exercise Micronaut Data JDBC 5.2.4 + Flyway + serde/jackson 3.2.6 against a real PostgreSQL, and `MediaMetricsTest` covers the micrometer 6.0.2 → 6.1.2 registry. Caveat on coverage breadth: 15 of the 43 collected tests are `@Disabled` in committed code (all 12 in `AddMediaCommandHandlerTest`, plus one each in `YouTubeTest`, `VideoControllerTest`, `PrepareVideoTest`), leaving 28 executed — that is pre-existing debt, not something this change introduced, and it does not weaken the version-bump signal.
- Minor harness detail: passing `-DargLine=` on the CLI overrides the `jacoco:prepare-agent` argLine, so the redirected run above collects no coverage. Harmless for this verification; do not read `target/jacoco.exec` from it.

## Feature review — media-queue-size-visibility (2026-11-12)

**Feature:** `oda-media-service` forwards the media widget's `showQueueSizeOnPage` flag to the payment page config as `PutKeyValue(recipientId, "paymentpage", "media.requests.showQueueSize", <Boolean>)`, alongside the five existing `media.requests.*` keys.
**Change set:** `1f62d082e98cc7545bf2ea63ca98dd54a3c2ccf2` → working tree; 2 files changed (+129/−0) — `settings/repository/MediaSettings.java` +11/−0 (tracked) and `src/test/java/io/github/stcarolas/oda/media/settings/MediaSettingsTest.java` +118/−0 (new, untracked). The other dirty entries — `AGENTS.md` +1/−1, `pom.xml` +1/−1 (micronaut-parent 5.1.0 → 5.2.5) and `CODE_REVIEW.md` +30 (the earlier parent-bump review) — carry 2026-11-10 mtimes and are not part of this change.
**Reviewer:** independent review subagent (fresh context)
**Verdict:** PASS WITH ISSUES
**Verification:**
- `mvn -B -Dmaven.repo.local=/home/stcarolas/Coding/projects/.feature-tools/m2repo -DskipTests compile` → **BUILD SUCCESS**, exit 0, 1.8 s — but the plugin reported `Nothing to compile - all classes are up to date`, so that run alone does not re-prove the source. Confirmed the cached output is current instead: `target/classes/io/github/opendonationassistant/settings/repository/MediaSettings.class` (mtime 02:14:15) is newer than the source (02:06:11) and contains both new literals `showQueueSizeOnPage` and `media.requests.showQueueSize`.
- `mvn -B -Dmaven.repo.local=… -DskipTests clean compile` **on a pristine copy of the tree under `/tmp`** (so the reviewed `target/`, containing the rung-1 jar, was left intact) → `Compiling 49 source files with javac [debug parameters release 25]`, **BUILD SUCCESS**, exit 0, 17 warnings, **no Error Prone / NullAway errors**. Warnings are pre-existing only: 6× `FutureReturnValueIgnored` at `MediaSettings.java:109,117,125,133,141,149` (line 149 is the new send, joining the five that already existed) and 2× `StringCaseLocaleUsage` at :171/:176 (untouched code). The `(Boolean)` cast at :78 raises no NullAway diagnostic.
- `mvn -B -Dmaven.repo.local=… test -Dtest=MediaSettingsTest -DfailIfNoTests=false -DargLine="-javaagent:…/net/bytebuddy/byte-buddy-agent/1.14.15/byte-buddy-agent-1.14.15.jar"` → **BUILD SUCCESS**, exit 0, `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` (1.5 s). The run log shows the `repository.save(...)` branch being taken, i.e. the deliberately unstubbed `findById` returned `Optional.empty()` and `save()` did not NPE.
- `git diff --numstat`, `git status --porcelain`, `git diff --check` → the numbers above; no whitespace errors; nothing under `.mvn/` modified, so the `--add-exports` and `-Xep:NullAway:ERROR` gates are intact.
- Contract signatures read from the resolved `oda-rabbit-conf-0.12.245` jar with `javap`: `WidgetProperty(String name, String displayName, String type, @Nullable Object value)`, `WidgetConfig(List<WidgetProperty>)`, `PutKeyValue(String ownerId, String name, String key, Object value)` — both the production call and the test fixture match positionally.
- Rung 1 (not re-executed — the cluster is restored): corroborated from artefacts. Live `deployment/media-service` runs `ghcr.io/opendonationassistant/oda-media-service:83`, exactly the `original_image` recorded in `.feature-tools/oda-jar-state/default/media-service.meta`; the live spec has no `command` override and no `volumes`, `readyReplicas 1/1 updated=1`, and pod `media-service-6f894ff847-g4594` started 2026-11-11T23:15:51Z — ~85 s after the local-jar patch timestamp 23:14:26Z. The deployed `target/oda-media-service-0.10.0.jar` (built 02:14:19+03:00) embeds `MediaSettings.class` of 11536 B — byte-size identical to the compile output — containing `showQueueSizeOnPage`, `paymentpage` and `media.requests.showQueueSize`. The jar that answered requests was therefore the working-tree code.
**Findings:**
- **Low** `src/test/java/io/github/stcarolas/oda/media/settings/MediaSettingsTest.java:79-90` — `capturedCommand()` verifies with `atLeastOnce()` and then takes `getAllValues().stream().filter(key).findFirst()`, so the tests prove that *a* command with the expected key/value exists but not that exactly one is sent: a duplicated `showQueueSize` send, or a second send for the same key carrying the other value, would still pass. Suggested fix: assert the per-key count (`assertEquals(1, commands.stream().filter(c -> SHOW_QUEUE_SIZE_KEY.equals(c.key())).count())`) and optionally pin the total command count (6) so a silently dropped sibling key is caught too.
- **Low** `src/main/java/io/github/opendonationassistant/settings/repository/MediaSettings.java:77-79` — the pass-through has no memory: `apply()` recomputes everything from the event, so any `changes.widgets` / routing-key `media` payload that omits `showQueueSizeOnPage` re-sends `true` and overwrites a stored `false`. That is reachable via the public `PATCH /widgets/{id}` (the rung-1 harness itself patched only `requestsEnabled` + `showQueueSizeOnPage`, i.e. exactly such a partial payload) and from any older client build. Not a regression introduced here — the five sibling keys behave identically and are reset the same way (`media.requests.amount` snapping back to 12 is the harsher instance) — and the console always ships the full property list (`AbstractWidgetSettings.prepareConfig()` maps every section property), so production impact is small. If the flag should survive partial payloads, send the key only when the property is present, or persist it the way `requestsEnabled` is.
- **Info** `src/main/java/io/github/opendonationassistant/settings/repository/MediaSettings.java:78` — `(Boolean) property.value()` cannot NPE: casting `null` to `Boolean` yields `null`, and `Optional.map` collapses a `null` result to `Optional.empty()`, which is precisely why the single `orElse(true)` covers both "absent" and "present but null" (the new test demonstrates this empirically rather than by comment). It does throw `ClassCastException` if the value ever arrives as a non-Boolean (e.g. the string `"false"`). That is the established pattern for `requestsEnabled`/`youtubeEnabled`/`vkvideoEnabled`, the console's `BooleanProperty` sends a real JS boolean, and the rung-1 round trip preserved the JSON boolean type (the harness compares `is True` / `== False`, which a string could not satisfy) — so consistent rather than defective; worth knowing only because the listener has no exception handling, so such a CCE would fail the `config.media` message outright.
- **Info** `src/main/java/io/github/opendonationassistant/settings/repository/MediaSettings.java:149` — the returned `CompletableFuture` is dropped, like the five sends above it, so a failed publish is silent and the toggle would simply never reach the payment page. Pre-existing pattern, extended by one (the sixth `FutureReturnValueIgnored` warning). Ordering is not a concern here: every command targets a distinct key, so the asynchronous publishes are order-independent.
- **Info** `AGENTS.md:39` (not part of this change) — the guidance "Add settings field → `settings/repository/` MediaSettings entity + repository" points a future contributor at a Flyway migration, which is the wrong shape for a payment-page-only display flag. One clarifying line — config-only flags are forwarded through `ConfigCommandSender` in `MediaSettings.apply()` and need no column — would prevent a pointless migration.
**Notes:**
- The scope decision (no DB column, no Flyway migration, no `MediaSettingsData` / `MediaSettingsRepository` change) is **correct**. This service never reads the flag: its only consumer is `oda-payment-page` (`PaymentPageConfig.ts:53`, `json.value["media.requests.showQueueSize"] ?? true`), the value is persisted by `oda-config-service`, and `PaymentPageConfigValue.defaultValues` already seeds `media.requests.showQueueSize = true`, so the key is readable before any widget is ever saved. A column here would be dead state plus a migration with no reader. The only consequence is the no-memory behaviour recorded as the second Low finding.
- Defaults agree across all four repos, so there is nothing to reconcile: console `BooleanProperty` default `true` (`MediaWidgetSettings.tsx:55`), media-service `.orElse(true)`, config-service `defaultValues` `true`, payment-page `?? true`.
- Placement is consistent with the existing sends: a sixth sequential `configCommandSender.send(...)` immediately after `media.requests.enabled`, before `this.data = updated; this.save();`, same `data.recipientId()` owner, same fire-and-forget style. `apply()` is the only producer of `media.requests.*` in the whole service (verified by grep), so there is no second code path that could omit the new key.
- Ownership is sound and unchanged: the recipient comes from `data.recipientId()`, i.e. the settings row resolved in `WidgetConfigListener` from `changes.widget().ownerId()`; `WidgetConfig` carries no owner field at all, so the target cannot be influenced by the property list in the payload. No new authorization surface, no secrets, nothing sensitive logged.
- No shared-contract impact. `ConfigCommand.PutKeyValue` is consumed as-is from `oda-rabbit-conf` `0.12.245`, which the committed `pom.xml` already pins; the pom's only diff is the pre-existing micronaut-parent bump. No `oda-libraries` release, no `${oda.version}` bump, and no new queue/exchange binding for siblings to follow.
- Test style matches the repo: plain `mock()` fields plus `ArgumentCaptor`, as in `PreparedVideoTest` and `VideoRepositoryCreateTest`, in the documented test package `io.github.stcarolas.oda.media.*` rather than the production package; fresh mock per test method; no `MockitoExtension`, consistent with the rest of the suite. Formatting is 2-space/80-col in spirit — the single 83-char line (`MediaSettingsTest.java:23`, the key constant) matches existing practice, and the repo has no formatter or checkstyle gate (other test files already exceed 80 columns). No TODOs, debug output or unrelated edits.
- Environment limitations: `~/.m2` is read-only, so every command used `-Dmaven.repo.local=/home/stcarolas/Coding/projects/.feature-tools/m2repo`; Mockito's inline mock maker needs the explicit byte-buddy `-javaagent` (no pom workaround was added); containers/Testcontainers are unavailable, so the repository's DB-backed integration tests were not run — out of scope for this change regardless.
- Not verified here: the rung-1 harness's own `9/9 checks passed` output was not preserved as an artefact and the cluster is restored, so it could not be replayed. What I could verify independently is the mechanism (the captured log line's shape matches `ConfigCommandSender.send`'s `ODALogger` context exactly), the deployed artefact (jar embeds the changed class), the timing (patch 23:14:26Z → restore pod 23:15:51Z) and the restoration itself (live image equals `original_image`, no leftover patch fields). No Playwright run was made against `oda-payment-page`; that belongs to the sibling reviews.
- Local side effects of the rung-1 run, recorded for the next session: `testuser`'s paymentpage config in the local cluster now holds `media.requests.*` values written during the replay (including `showQueueSize = true`), and both `media-service` and `config-service` were temporarily repointed to locally built jars (config-service to `:69`). Both deployments are back on their original images; nothing to undo.

### Post-review follow-up — media-queue-size-visibility (2026-11-12, Lead)

- Low (per-key command count not pinned) **addressed**: `MediaSettingsTest.capturedCommand()` now asserts exactly one command per key instead of `atLeastOnce()` + `findFirst()` (test file 117 lines; change set 2 files, +128/−0).
- Re-verified: `mvn -B -Dmaven.repo.local=<writable repo copy> test -Dtest=MediaSettingsTest -DfailIfNoTests=false -DargLine="-javaagent:<byte-buddy-agent jar>"` → **Tests run: 3, Failures: 0, Errors: 0**.
- Low ("no memory" pass-through) accepted deliberately: keeping the new key consistent with the five sibling `media.requests.*` sends; see the cumulative feature report §6.3.
- Info (AGENTS.md "Add settings field" hint) not changed: that file carries pre-existing uncommitted edits.

## Feature review — shared-error-responses (2026-11-14)

**Feature:** `oda-media-service` adopts the platform's shared RFC 7807 error handling from `oda-libraries` `oda-commons` by moving all three `oda-*` pins to `0.12.251` through a single `<oda.version>` property.
**Change set:** `1f62d08` -> working tree; feature change = `pom.xml` +4/−3 (one `<oda.version>0.12.251</oda.version>` property at `:16`, `oda-rabbit-conf` `:44` and `oda-test-utils` `:49` repointed from the literal `0.12.245` to `${oda.version}`, plus the tightly coupled `micronaut-parent` 5.1.0 -> 5.2.6 line at `:12`). Whole working tree = 4 tracked files (+83/−4) and 1 untracked file (`src/test/java/io/github/stcarolas/oda/media/settings/MediaSettingsTest.java`, 117 lines); the non-`pom.xml` entries belong to other sessions and are not reviewed here.
**Reviewer:** independent review subagent (fresh context)
**Verdict:** PASS WITH ISSUES
**Verification:**
- `mvn -B -o -DskipTests -Dmaven.repo.local=/home/stcarolas/Coding/projects/.m2-repo compile` -> **BUILD SUCCESS**, exit 0, 24.6 s. The log shows `Recompiling the module because of changed dependency` followed by `Compiling 49 source files with javac [debug parameters release 25]`, i.e. the 49 main sources were genuinely recompiled against the bumped artefacts, and OpenAPI was regenerated (`Writing OpenAPI file to destination … oda-media-service-0.10.0.yml`). Error Prone / NullAway emitted warnings only — `FutureReturnValueIgnored` ×6 and `StringCaseLocaleUsage` ×2 in `MediaSettings.java`, `UnnecessaryAssignment` in `YouTube.java` — no errors.
- `mvn -B -o ... dependency:tree -Dscope=compile` -> `oda-rabbit-conf:jar:0.12.251`, `io.github.opendonationassistant:oda-commons:jar:0.12.251` (transitive through rabbit-conf), `oda-test-utils:jar:0.12.251`, `micronaut-problem-json:jar:5.1.0`; BUILD SUCCESS. The pins resolve to the intended version on the compile classpath.
- `mvn -B -o ... help:evaluate -Dexpression=<prop>` -> `micronaut.version=5.2.6`, `micronaut.core.version=5.2.23`, `micronaut.data.version=5.2.5`, `micronaut.security.version=5.4.2`, `micronaut.serialization.version=3.2.6`, `micronaut.testresources.version=4.3.1`.
- `mvn -B -o ... -DskipTests test-compile` -> **BUILD FAILURE**, environment only: `httpclient5:5.6.4`, `jakarta.websocket-api:2.1.1`, `jakarta.websocket-client-api:2.1.1` are absent from the offline cache (wiremock test scope), so no test source was compiled. Retried once without `-o` -> **BUILD SUCCESS**, `Compiling 16 source files … to target/test-classes`, i.e. the `oda-test-utils` `0.12.251` bump does not break the test sources either.
- `git diff --numstat`, `git diff --check`, `git status --porcelain -uall` -> the numbers above, no whitespace errors, nothing under `.mvn/` touched (`--add-exports` and the `-Xplugin:ErrorProne … -Xep:NullAway:ERROR` args unchanged).
- Comment audit over the added lines of `*.java`/`*.ts`/`*.tsx`/`*.js` -> no `//`, `/* */` or Javadoc line added.
- Artefact inspection of the resolved jars: `oda-commons-0.12.251.jar` contains `io/github/opendonationassistant/commons/problem/ApiProblem.class`, `GlobalExceptionHandler.class`, `AbstractExceptionHandler.class`, `problem/UpstreamCallExceptionHandler.class` plus their `META-INF/micronaut/...BeanDefinitionReference` entries, so the shared handlers really are what this pin delivers; `javap` shows `GlobalExceptionHandler(ThrowableProblemHandler, UpstreamCallExceptionHandler)` and `UpstreamCallExceptionHandler(ThrowableProblemHandler)`, whose only external bean requirement `io.micronaut.problem.ThrowableProblemHandler` is satisfied by this pom's existing `micronaut-problem-json` dependency (`5.1.0`, the version `0.12.251` was built against).
**Findings:**
- **Medium** `pom.xml:16` (effective at `:44`, `:49`) — the bump points at `oda-libraries` `0.12.251`, and in the local Maven cache that version exists only as a *locally installed* artefact: `.m2-repo/io/github/opendonationassistant/oda-commons/0.12.251/_remote.repositories` records `oda-commons-0.12.251.jar>=` (empty repository id = `mvn install`, and the same for `oda-rabbit-conf`/`oda-test-utils`), whereas the previously pinned `0.12.245` and the intermediate `0.12.249`/`0.12.250` all record `>github=` (downloaded from GitHub Packages), and only `maven-metadata-local.xml` names `0.12.251` as release. Every gate above therefore ran against a locally built copy of the library, and this pom will not resolve in CI until `oda-libraries` `0.12.251` is published. This is expected mid-feature, but it fixes the merge ordering: publish `oda-libraries` `0.12.251` (aggregate + `commons` + `rabbit-conf` + `test-utils`) before committing this bump, and re-resolve once online so the cache marker proves the published artefact rather than a local one. No code fix needed in this repo.
- **Low** `AGENTS.md:25` — the structure block still says `Maven build (Micronaut parent 5.2.5)` while `pom.xml:12` is now `5.2.6`. The parent bump is not incidental: `oda-commons` `0.12.245`, the pin this change replaces, was built against Micronaut netty `5.1.10` / data `5.1.1` / security `5.3.1` / problem-json `5.0.0` (the old 5.1.0 BOM), `0.12.250` against netty `5.2.22` / data `5.2.4` / security `5.4.1`, and `0.12.251` against netty `5.2.23` / data `5.2.5` / security `5.4.2` / problem-json `5.1.0` — exactly what `micronaut-parent` `5.2.6` manages and what this build now resolves. So the parent line belongs to this change set even though the feature brief listed only the `oda.version` edit, and the repo rule "if you change public behaviour and `AGENTS.md` describes the old one, update it in the same change" applies. Suggested fix: `5.2.5` -> `5.2.6` in that one line (and say so in the commit message, since the brief described a narrower change).
- **Info** `pom.xml:41-50` — there is no direct `oda-commons` declaration; it reaches the build only transitively (`oda-rabbit-conf:0.12.251` -> `oda-commons:0.12.251`, verified). That is sufficient today and nothing needs repointing, but it means the service's RFC 7807 behaviour is load-bearing on an edge owned by `rabbit-conf`: if a future `oda-rabbit-conf` drops its `commons` dependency, this service silently loses `GlobalExceptionHandler` without any pom change. Declaring `io.github.opendonationassistant:oda-commons` explicitly at `${oda.version}` would make the contract visible and is the pattern the platform documentation describes ("shared libs pinned per service via `${oda.version}`").
- **Info** `src/main/java/io/github/opendonationassistant/media/commands/PrepareVideo.java:77` — with this pin the service gains the shared `GlobalExceptionHandler` (`RuntimeException` -> `500 internal_error`, `CompletableFuture` cause-chain unwrapping) and `UpstreamCallExceptionHandler` (`401 upstream_unauthorized` / `500 upstream_failure`, opt-out `oda.problems.upstream=false`), and it has no local exception handler that could shadow them (grep for `ExceptionHandler`/`@ControllerAdvice`/`ErrorResponseProcessor` under `src/` finds none). It does **not** adopt `ApiProblem`: all 26 `Problem.builder()` sites (`PrepareVideo.java` ×13, `VideoRepository.java` ×13) still build the problem by hand with `.withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))` and no `.withType(...)` anywhere in `src/main`, so those 400s still carry no stable `type`/`code`. They do at least always carry a status, so the specific defect `ApiProblem` exists to prevent (status-less `Problem` answered as `400` with `"status":null`) is not present here. Migrating the 26 builders to `ApiProblem.of(...)` is follow-up work for this service, not part of this bump.
**Notes:**
- Scope check against the brief: exactly one `<oda.version>0.12.251</oda.version>` (`pom.xml:16`, `grep -c '<oda.version>'` = 1) and both `oda-*` dependencies resolve to `${oda.version}` (`:44`, `:49`); no `oda-commons` literal to convert. `AGENTS.md` carries no `oda.version` line at all (grep for `oda.version|0.12.` matches nothing there), so the "update the one-line `oda.version` reference in `AGENTS.md`" part of the brief does not apply to this repo — the only `AGENTS.md` edit in the tree is the parent-version line covered above. A repo-wide grep for `oda.version|0.12.251` finds nothing outside `pom.xml` (`CODE_REVIEW.md:292` mentions `0.12.245` but that is earlier review prose, not a pin). No other file belongs to this feature.
- Pre-existing uncommitted changes, out of scope and not reviewed: `src/main/java/io/github/opendonationassistant/settings/repository/MediaSettings.java` (+11/−0, the `showQueueSizeOnPage` -> `media.requests.showQueueSize` forwarding), the untracked `src/test/java/io/github/stcarolas/oda/media/settings/MediaSettingsTest.java` (+117, its test), `CODE_REVIEW.md` (+67, the two earlier review sections), and the `AGENTS.md` 5.1.0 -> 5.2.5 line, all with 2026-11-10/11-12 mtimes from the parent-bump and queue-size sessions. They were left untouched; only `pom.xml` was rebuilt for this review.
- Rung 1 (live jar + `requests/`) was not executed: this review's remit is the compile gate, and the cluster state was not touched. It would be worth running before merge for one specific reason — this pin changes the service's answer for *unhandled* exceptions from Micronaut's default error body to a `500 internal_error` problem, and no `.http` collection asserts that path, so the behaviour change is currently unobserved either way. Rung 2 (Playwright) is not applicable: no HTTP contract shape this repo's frontends compile against changed, and the harness was not part of this task.
- Environment limits: `~/.m2` is read-only, so every command used `-Dmaven.repo.local=/home/stcarolas/Coding/projects/.m2-repo`; the offline test-compile failed only on uncached wiremock test-scope artefacts and the one permitted online retry downloaded `httpclient5:5.6.4` and the two `jakarta.websocket` APIs into that shared cache (normal cache warm, no repo files changed). GitHub Packages publication of `0.12.251` could not be verified from here (the repository needs credentials and the gates ran offline), which is why the Medium finding is phrased as a merge-ordering action rather than a defect.
