# PROJECT KNOWLEDGE BASE

**Generated:** 2026-09-10
**Commit:** 26ed40f
**Branch:** master

## OVERVIEW

Micronaut 5 microservice for OpenDonationAssistant platform managing YouTube/VK video song requests. CQRS-like architecture with HTTP endpoints + RabbitMQ async processing. Java 25, PostgreSQL (Flyway), GraalVM native-image deployment.

## STRUCTURE

```
oda-media-service/
├── src/main/java/io/github/opendonationassistant/
│   ├── Application.java          # Entry point + RabbitMQ channel factory
│   ├── media/                    # Core domain: video lifecycle (prepared→ready→handled)
│   ├── playlist/                 # YouTube playlist CRUD
│   ├── settings/                 # Per-recipient media configuration
│   └── integration/              # External APIs (YouTube Data v3, VK Video)
├── src/main/resources/
│   ├── application*.yml          # Micronaut profiles (standalone, allinone)
│   └── db/migration/V1..V9       # Flyway SQL migrations
├── src/test/java/                # Tests (NOTE: different package: io.github.stcarolas.oda.media)
├── pom.xml                       # Maven build (Micronaut parent 5.1.0)
├── Dockerfile                    # Native binary on fedora:41
└── .github/workflows/maven.yml   # CI stub → delegates to oda-libraries
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Add new video state | `media/video/` | State pattern: PreparedVideo → ReadyVideo → HandledVideo |
| Add HTTP endpoint | `media/commands/` or `media/` | Commands in `commands/`, queries in root |
| Add RabbitMQ listener | `media/listeners/` | Implement handler in `listeners/handlers/` |
| External API integration | `integration/youtube/` or `integration/vk/` | WireMock stubs in tests |
| Database changes | `src/main/resources/db/migration/` | Flyway naming: V{N}__description.sql |
| Add settings field | `settings/repository/` | MediaSettings entity + repository |
| Playlist operations | `playlist/` | Commands, repository, controller pattern |

## CODE MAP

| Symbol | Type | Location | Role |
|--------|------|----------|------|
| `Application` | Class | `Application.java` | Entry point + RabbitMQ factory |
| `VideoController` | Class | `media/VideoController.java` | GET/PATCH `/media/video` |
| `PrepareVideo` | Class | `media/commands/PrepareVideo.java` | PUT `/media/video` |
| `VideoRepository` | Class | `media/repository/VideoRepository.java` | Domain logic + persistence |
| `YouTube` | Class | `integration/youtube/YouTube.java` | YouTube Data API client |
| `PlaylistController` | Class | `playlist/view/PlaylistController.java` | Playlist endpoints |
| `MediaSettingsRepository` | Class | `settings/repository/` | Settings persistence |

## CONVENTIONS

- **Null-safety mandatory**: NullAway JSpecify mode enforced at compile (`-Xep:NullAway:ERROR`)
- **Test environment**: `@MicronautTest(environments = "allinone")` for integration tests
- **WireMock for external APIs**: `@WireMockTest` + stub helpers in `*Stubs.java`
- **Package mismatch**: Production `io.github.opendonationassistant` ≠ Tests `io.github.stcarolas.oda.media`
- **State pattern**: Video lifecycle uses sealed interface + implementing records
- **CQRS separation**: Commands (HTTP PUT/POST) vs Queries (HTTP GET) in separate classes

## ANTI-PATTERNS (THIS PROJECT)

- **Duplicate video creation logic**: `PrepareVideo` and `VideoRepository` both contain YouTube/VK URL parsing
- **Incomplete owner check**: `UpdatePlaylist.update()` has `// todo check owner id` - authorization gap
- **Dead code**: `integration/vk/VK.java` is empty (3 lines, no content)
- **Stale config**: Logger levels reference old package `io.github.stcarolas` not `io.github.opendonationassistant`

## UNIQUE STYLES

- **Native-image packaging**: Dockerfile expects pre-built binary, not JAR
- **Cross-repo CI**: `maven.yml` delegates to `OpenDonationAssistant/oda-libraries` shared workflow
- **Version duality**: pom.xml `0.10.0` vs CI release tag = GitHub RUN_NUMBER
- **No Maven wrapper**: `.mvn/wrapper` gitignored; builds require installed Maven

## COMMANDS

```bash
# Build (requires JDK 25 + Maven)
mvn clean package -DskipTests

# Build native image
mvn clean package -Dpackaging=native-image -DskipTests

# Run tests
mvn test

# Run locally (standalone profile)
mvn clean package -DskipTests && java -jar target/oda-media-service-0.10.0.jar

# Docker build (requires native binary first)
docker build -t oda-media-service .
```

## NOTES

- **Environment variables**: `JDBC_URL`, `JDBC_USER`, `JDBC_PASSWORD`, `RABBITMQ_HOST`, `YOUTUBE_KEY`
- **Auth**: Cookie JWT via Keycloak (`MICRONAUT_SECURITY_TOKEN_JWT_SIGNATURES_JWKS_KEYCLOAK_URL`)
- **Micronaut profiles**: `standalone` (default), `allinone` (tests)
- **SonarCloud**: Project key `OpenDonationAssistant_oda-media-service`
- **WireMock beta**: Tests use `4.0.0-beta.30` (may have API instability)
- **Ignored**: `target/`, `.gradle`, `prepared-video-storage`, IDE files
