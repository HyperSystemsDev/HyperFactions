# HyperFactions Storage System

This document details the data persistence system for HyperFactions.

## Overview

HyperFactions uses a JSON file-based storage system with the following characteristics:

- **Async Operations**: All storage operations return `CompletableFuture` for non-blocking I/O
- **Interface-based**: Storage interfaces allow for alternative implementations (e.g., database)
- **Pretty-printed JSON**: Human-readable files for easy debugging and manual editing
- **Auto-migration**: Supports migration from older data formats

## Data Directory Structure

```
<server>/mods/com.hyperfactions_HyperFactions/
├── config.json              # Server configuration
├── zones.json               # Admin zones (SafeZone/WarZone)
├── update_preferences.json  # Per-player update notification settings
├── factions/                # One file per faction
│   ├── {uuid}.json
│   └── ...
└── players/                 # One file per player (power data)
    ├── {uuid}.json
    └── ...
```

## Storage Interfaces

### FactionStorage

Interface for faction data persistence.

```java
public interface FactionStorage {
    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();
    CompletableFuture<Optional<Faction>> loadFaction(UUID factionId);
    CompletableFuture<Void> saveFaction(Faction faction);
    CompletableFuture<Void> deleteFaction(UUID factionId);
    CompletableFuture<Collection<Faction>> loadAllFactions();
}
```

### PlayerStorage

Interface for player power data persistence.

```java
public interface PlayerStorage {
    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();
    CompletableFuture<Optional<PlayerPower>> loadPlayerPower(UUID uuid);
    CompletableFuture<Void> savePlayerPower(PlayerPower power);
    CompletableFuture<Void> deletePlayerPower(UUID uuid);
    CompletableFuture<Collection<PlayerPower>> loadAllPlayerPower();
}
```

### ZoneStorage

Interface for admin zone data persistence.

```java
public interface ZoneStorage {
    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();
    CompletableFuture<Collection<Zone>> loadAllZones();
    CompletableFuture<Void> saveAllZones(Collection<Zone> zones);
}
```

## Data Models

### Faction

Stored in `factions/{uuid}.json`

```json
{
  "id": "uuid-string",
  "name": "FactionName",
  "description": "Optional description",
  "tag": "TAG",
  "color": "f",
  "createdAt": 1706745600000,
  "open": false,
  "home": {
    "world": "world",
    "x": 100.5,
    "y": 64.0,
    "z": 200.5,
    "yaw": 90.0,
    "pitch": 0.0,
    "setAt": 1706745600000,
    "setBy": "player-uuid"
  },
  "members": [
    {
      "uuid": "player-uuid",
      "username": "PlayerName",
      "role": "LEADER",
      "joinedAt": 1706745600000,
      "lastOnline": 1706832000000
    }
  ],
  "claims": [
    {
      "world": "world",
      "chunkX": 10,
      "chunkZ": 20,
      "claimedAt": 1706745600000,
      "claimedBy": "player-uuid"
    }
  ],
  "relations": [
    {
      "targetFactionId": "other-faction-uuid",
      "type": "ALLY",
      "since": 1706745600000
    }
  ],
  "logs": [
    {
      "type": "MEMBER_JOIN",
      "message": "PlayerName joined the faction",
      "timestamp": 1706745600000,
      "actorUuid": "player-uuid"
    }
  ],
  "permissions": {
    "outsiderBreak": false,
    "outsiderPlace": false,
    "outsiderInteract": false,
    "allyBreak": false,
    "allyPlace": false,
    "allyInteract": true,
    "memberBreak": true,
    "memberPlace": true,
    "memberInteract": true,
    "pvpEnabled": true,
    "officersCanEdit": false
  }
}
```

**Field Details**:

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique faction identifier |
| `name` | string | Display name |
| `description` | string? | Optional faction description |
| `tag` | string? | Short tag (shown in chat) |
| `color` | string | Minecraft color code (a-f, 0-9) |
| `createdAt` | long | Unix timestamp (ms) |
| `open` | bool | Public join enabled |
| `home` | object? | Faction home location |
| `members` | array | List of faction members |
| `claims` | array | List of claimed chunks |
| `relations` | array | Diplomatic relations |
| `logs` | array | Activity log (last 50 entries) |
| `permissions` | object? | Territory permissions (null = defaults) |

**Member Roles**: `LEADER`, `OFFICER`, `MEMBER`

**Relation Types**: `ALLY`, `NEUTRAL`, `ENEMY`

**Log Types**: `FACTION_CREATE`, `MEMBER_JOIN`, `MEMBER_LEAVE`, `MEMBER_KICK`, `CLAIM`, `UNCLAIM`, `ALLY_REQUEST`, `ALLY_ACCEPT`, `ENEMY_DECLARE`, `RELATION_NEUTRAL`, `HOME_SET`, `LEADER_CHANGE`, `PROMOTE`, `DEMOTE`, `DESCRIPTION_CHANGE`, `NAME_CHANGE`, `COLOR_CHANGE`, `TAG_CHANGE`, `OPEN_TOGGLE`

### PlayerPower

Stored in `players/{uuid}.json`

```json
{
  "uuid": "player-uuid",
  "power": 15.5,
  "maxPower": 20.0,
  "lastDeath": 1706745600000,
  "lastRegen": 1706832000000
}
```

| Field | Type | Description |
|-------|------|-------------|
| `uuid` | UUID | Player's UUID |
| `power` | double | Current power level |
| `maxPower` | double | Maximum power capacity |
| `lastDeath` | long | Timestamp of last death (0 = never) |
| `lastRegen` | long | Timestamp of last power regeneration |

### Zone

All zones stored in `zones.json` as an array.

```json
[
  {
    "id": "zone-uuid",
    "name": "Spawn",
    "type": "SAFE",
    "world": "world",
    "chunks": [
      { "x": 0, "z": 0 },
      { "x": 0, "z": 1 },
      { "x": 1, "z": 0 }
    ],
    "createdAt": 1706745600000,
    "createdBy": "admin-uuid",
    "flags": {
      "pvp": false,
      "monsters": false,
      "firespread": false
    }
  }
]
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique zone identifier |
| `name` | string | Display name |
| `type` | string | `SAFE` or `WAR` |
| `world` | string | World name |
| `chunks` | array | List of chunk coordinates |
| `createdAt` | long | Unix timestamp (ms) |
| `createdBy` | UUID | Admin who created the zone |
| `flags` | object? | Custom flag overrides |

**Zone Types**:
- `SAFE`: No PvP, no monster spawning, no explosions by default
- `WAR`: PvP enabled, dangerous environment

**Zone Flags**:
| Flag | SafeZone Default | WarZone Default |
|------|-----------------|-----------------|
| `pvp` | false | true |
| `monsters` | false | true |
| `explosions` | false | true |
| `firespread` | false | true |
| `enderpearl` | true | true |
| `build` | false | true |
| `interact` | true | true |
| `itempickup` | false | true |
| `hunger` | false | true |
| `falldamage` | false | true |
| `combattag` | false | true |

### UpdateNotificationPreferences

Stored in `update_preferences.json`

```json
{
  "disabledPlayers": [
    "player-uuid-1",
    "player-uuid-2"
  ]
}
```

Players in this list won't receive update notifications on login.

## Auto-Save System

HyperFactions automatically saves data at configurable intervals:

- **Interval**: Configured via `autoSave.intervalMinutes` (default: 5)
- **Scope**: Saves all factions, player power, and zones
- **Trigger**: Also saves on graceful shutdown

## Data Migration

The storage system supports automatic migration from older formats:

### Zone Format Migration

Old single-chunk format:
```json
{
  "id": "...",
  "world": "world",
  "chunkX": 10,
  "chunkZ": 20
}
```

Automatically migrates to multi-chunk format:
```json
{
  "id": "...",
  "world": "world",
  "chunks": [{ "x": 10, "z": 20 }]
}
```

## Manual Data Editing

JSON files can be manually edited while the server is stopped. To apply changes:

1. Stop the server
2. Edit the JSON files
3. Start the server (data loads on startup)

Or use `/f admin reload` to reload config (note: faction/player data is cached in memory).

## Backup Recommendations

Important files to backup:
- `config.json` - Server configuration
- `zones.json` - Admin zones
- `factions/` - All faction data
- `players/` - All player power data

Backup frequency should match your auto-save interval at minimum.
