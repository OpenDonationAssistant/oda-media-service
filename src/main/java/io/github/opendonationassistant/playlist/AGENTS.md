# PLAYLIST MODULE

YouTube playlist CRUD operations with per-recipient ownership.

## STRUCTURE

```
playlist/
├── Playlist.java                # Domain entity (full lifecycle)
├── commands/                    # HTTP command endpoints
│   ├── CreatePlaylist.java      # POST /playlists
│   ├── UpdatePlaylist.java      # PATCH /playlists/{id}
│   └── DeletePlaylist.java      # DELETE /playlists/{id}
├── repository/                  # Data layer
│   ├── PlaylistData.java        # JPA entity
│   ├── PlaylistDataRepository.java # Micronaut Data JDBC interface
│   └── PlaylistRepository.java  # Domain logic + persistence
└── view/                        # Query endpoints
    ├── PlaylistController.java  # GET /playlists, /playlists/{id}
    └── PlaylistDto.java         # API response DTO
```

## WHERE TO LOOK

| Task | File | Notes |
|------|------|-------|
| Add playlist field | `Playlist.java` + `PlaylistData.java` | Domain + entity + migration |
| Add command | `commands/` | New class with `@Controller` |
| Add query | `view/PlaylistController.java` | Add `@Get` method |
| Modify persistence | `repository/PlaylistRepository.java` | Domain logic here |

## CONVENTIONS

- **CQRS separation**: Commands in `commands/`, queries in `view/`
- **Repository is domain layer**: Business logic in `PlaylistRepository`, not `PlaylistData`
- **Ownership pattern**: Each playlist has `ownerId` from JWT authentication

## ANTI-PATTERNS

- **Incomplete authorization**: `UpdatePlaylist.update()` has `// todo check owner id` - any authenticated user can update any playlist

## NOTES

- **DTO confusion**: `media/dto/Playlist.java` is a simple DTO, `playlist/Playlist.java` is the full domain entity - different classes with same name
