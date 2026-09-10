# MEDIA MODULE

Core domain: video lifecycle management with CQRS pattern.

## STRUCTURE

```
media/
├── VideoController.java         # GET/PATCH queries
├── AvailableMediaController.java # GET search (YouTube)
├── commands/                    # HTTP command endpoints
│   ├── PrepareVideo.java        # PUT /media/video
│   ├── LinkPayment.java         # POST /commands/media/linkPayment
│   └── MarkAllListened.java     # POST /media/commands/mark-all-listened
├── listeners/                   # RabbitMQ message handlers
│   ├── CommandsListener.java    # Listens to `commands` exchange
│   ├── EventsListener.java      # Listens to `events` exchange
│   └── handlers/                # Handler implementations
├── repository/                  # Data layer
│   ├── VideoData.java           # JPA entity
│   ├── VideoDataRepository.java # Micronaut Data JDBC interface
│   └── VideoRepository.java     # Domain logic + persistence
├── video/                       # State pattern domain model
│   ├── prepared/PreparedVideo.java
│   ├── ready/ReadyVideo.java
│   └── HandledVideo.java
├── senders/                     # Outbound messaging
│   └── ReadyVideoNotificationSender.java
├── metrics/                     # Prometheus counters
│   └── MediaMetrics.java
└── dto/                         # API response DTOs
    └── Playlist.java
```

## WHERE TO LOOK

| Task | File | Pattern |
|------|------|---------|
| Add video state | `video/` | Create new record implementing sealed interface |
| Add query endpoint | `VideoController.java` | `@Get` methods |
| Add command endpoint | `commands/` | New class with `@Controller` + `@Put`/`@Post` |
| Add message handler | `listeners/handlers/` | Implement handler, register in listener |
| Modify persistence | `repository/VideoRepository.java` | Domain logic here, not in entity |
| Add metric | `metrics/MediaMetrics.java` | Counter/Gauge registration |

## CONVENTIONS

- **Commands in `commands/`**, queries in root controllers
- **Handlers in `listeners/handlers/`**, wired via listener classes
- **State transitions**: Only via sealed interface methods on video records
- **Repository is domain layer**: Business logic lives in `VideoRepository`, not `VideoData`

## ANTI-PATTERNS

- **Duplicate URL parsing**: `PrepareVideo` and `VideoRepository` both parse YouTube/VK URLs
- **Stale DTO name**: `media/dto/Playlist.java` conflicts with `playlist/Playlist.java` (different classes)

## VIDEO LIFECYCLE

```
┌─────────────┐    payment     ┌─────────────┐    played    ┌─────────────┐
│ PreparedVideo│ ───────────→ │  ReadyVideo  │ ──────────→ │ HandledVideo│
└─────────────┘               └─────────────┘              └─────────────┘
```

States are Java records implementing a sealed interface. Transitions are explicit methods on each record.
