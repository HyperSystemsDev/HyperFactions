# HyperFactions Integration Breakdown

> **Version**: 0.7.0 | **Package**: `com.hyperfactions.integration`

HyperFactions integrates with external plugins through soft dependencies. All integrations use reflection-based detection and fail-open design — if a dependency is missing, the feature gracefully degrades.

---

## Table of Contents

- [Overview](#overview)
- [Permission System](#permission-system)
- [PlaceholderAPI (PAPI)](#placeholderapi-papi)
- [WiFlow PlaceholderAPI](#wiflow-placeholderapi)
- [OrbisGuard](#orbisguard)
- [OrbisGuard-Mixins](#orbisguard-mixins)
- [World Map](#world-map)
- [HyperPerms Context](#hyperperms-context)

---

## Overview

```mermaid
graph TD
    HF[HyperFactions] --> PM[PermissionManager]
    HF --> PAPI[PlaceholderAPI]
    HF --> WF[WiFlow]
    HF --> OG[OrbisGuard]
    HF --> OGM[OrbisGuard-Mixins]
    HF --> WM[World Map]
    HF --> HP[HyperPerms Context]

    PM --> VU[VaultUnlocked]
    PM --> HPP[HyperPerms]
    PM --> LP[LuckPerms]

    OGM -->|System.getProperties| Hooks[11 Hook Callbacks]

    style HF fill:#2563eb,color:#fff
    style PM fill:#7c3aed,color:#fff
    style PAPI fill:#059669,color:#fff
    style WF fill:#059669,color:#fff
    style OG fill:#d97706,color:#fff
    style OGM fill:#d97706,color:#fff
    style WM fill:#0891b2,color:#fff
    style HP fill:#7c3aed,color:#fff
```

All integrations share these design principles:

- **Reflection-based detection**: No hard dependencies in `manifest.json`
- **Fail-open**: Missing integrations never cause errors or block functionality
- **Lazy initialization**: Detection happens once at startup; results are cached
- **Logging**: All integration status is logged on startup for debugging

---

## Permission System

HyperFactions uses a chain-of-responsibility pattern to check permissions across multiple providers. Providers are tried in priority order; the first to give a definitive answer wins.

### Provider Chain

| Priority | Provider | Detection |
|----------|----------|-----------|
| 1 | VaultUnlocked | Reflection: `at.helpch.vaultunlocked.api.*` |
| 2 | HyperPerms | Reflection: `com.hyperperms.api.*` |
| 3 | LuckPerms | Reflection: `net.luckperms.api.*` |

### PermissionProvider Interface

```java
public interface PermissionProvider {
    String getName();
    boolean isAvailable();
    Optional<Boolean> hasPermission(UUID playerUuid, String permission);
    String getPrefix(UUID playerUuid, String worldName);
    String getSuffix(UUID playerUuid, String worldName);
    String getPrimaryGroup(UUID playerUuid);
}
```

Key: `hasPermission()` returns `Optional<Boolean>`:
- `Optional.of(true)` — permission explicitly granted
- `Optional.of(false)` — permission explicitly denied
- `Optional.empty()` — provider cannot determine (player not found, etc.)

### Resolution Flow

```mermaid
flowchart TD
    A[hasPermission] --> B{Try each provider\nin order}
    B -->|Provider returns true| C[GRANTED]
    B -->|Provider returns false| D{Is admin/bypass/limit?}
    D -->|Yes| E[DENIED]
    D -->|No: user-level| F{Check wildcards}
    B -->|All return empty| F

    F --> G{Category wildcard?\ne.g. hyperfactions.teleport.*}
    G -->|Granted| C
    G -->|Not found| H{Root wildcard?\nhyperfactions.*}
    H -->|Granted| C
    H -->|Not found| I{Fallback}

    I --> J{Permission type?}
    J -->|admin.*| K[Check OP status]
    J -->|bypass.*| L[DENIED]
    J -->|limit.*| L
    J -->|user-level| M{allowWithoutPermissionMod\nconfig setting}
    M -->|true| C
    M -->|false| L

    style C fill:#22c55e,color:#fff
    style E fill:#ef4444,color:#fff
    style L fill:#ef4444,color:#fff
```

### Wildcard Resolution

Permissions are checked in this order:
1. **Exact node**: `hyperfactions.teleport.home`
2. **Category wildcard**: `hyperfactions.teleport.*`
3. **Root wildcard**: `hyperfactions.*`
4. **Fallback**: Based on permission type

### Fallback Rules

When no provider can answer:

| Permission Type | Fallback Behavior |
|----------------|-------------------|
| `hyperfactions.admin.*` | Requires OP (checks Hytale PermissionsModule) |
| `hyperfactions.bypass.*` | Always denied |
| `hyperfactions.limit.*` | Always denied (config defaults used instead) |
| User-level permissions | Configurable via `allowWithoutPermissionMod` |

---

## PlaceholderAPI (PAPI)

**Package**: `com.hyperfactions.integration.papi`
**Identifier**: `factions`
**Format**: `%factions_<placeholder>%`

Registered via `PlaceholderExpansion` from the PlaceholderAPI library. Persists across reloads (`persist() = true`).

> **Full Reference**: See [placeholders.md](placeholders.md) for complete placeholder documentation with null behavior, usage examples, and configuration details.

### Placeholders (33)

#### Player Faction Info (12)

| Placeholder | Returns | Example |
|-------------|---------|---------|
| `%factions_has_faction%` | `yes` / `no` | `yes` |
| `%factions_name%` | Faction name | `Warriors` |
| `%factions_faction_id%` | Faction UUID | `a1b2c3...` |
| `%factions_tag%` | Faction tag | `WAR` |
| `%factions_display%` | Tag or name (config-dependent) | `WAR` |
| `%factions_color%` | Faction color code | `#FF5555` |
| `%factions_role%` | Player's role display name | `Officer` |
| `%factions_description%` | Faction description | `The best faction` |
| `%factions_leader%` | Leader's username | `Steve` |
| `%factions_leader_id%` | Leader's UUID | `d4e5f6...` |
| `%factions_open%` | Open status | `true` |
| `%factions_created%` | Creation date (yyyy-MM-dd) | `2025-01-15` |

#### Power (7)

| Placeholder | Returns | Example |
|-------------|---------|---------|
| `%factions_power%` | Player power (1 d.p.) | `8.5` |
| `%factions_maxpower%` | Player max power (1 d.p.) | `10.0` |
| `%factions_power_percent%` | Player power % | `85` |
| `%factions_faction_power%` | Faction total power (1 d.p.) | `42.5` |
| `%factions_faction_maxpower%` | Faction max power (1 d.p.) | `50.0` |
| `%factions_faction_power_percent%` | Faction power % | `85` |
| `%factions_raidable%` | Raidable status | `false` |

#### Territory (4)

| Placeholder | Returns | Example |
|-------------|---------|---------|
| `%factions_land%` | Claimed chunk count | `12` |
| `%factions_land_max%` | Max claimable chunks | `20` |
| `%factions_territory%` | Owner of current chunk | `Warriors` / `SafeZone` / `Wilderness` |
| `%factions_territory_type%` | Territory type at location | `Claimed` / `SafeZone` / `WarZone` / `Wilderness` |

#### Faction Home (6)

| Placeholder | Returns | Example |
|-------------|---------|---------|
| `%factions_home_world%` | Home world name | `world` |
| `%factions_home_x%` | Home X (2 d.p.) | `123.45` |
| `%factions_home_y%` | Home Y (2 d.p.) | `64.00` |
| `%factions_home_z%` | Home Z (2 d.p.) | `-456.78` |
| `%factions_home_coords%` | Home X, Y, Z (2 d.p.) | `123.45, 64.00, -456.78` |
| `%factions_home_yaw%` | Home yaw (2 d.p.) | `90.00` |
| `%factions_home_pitch%` | Home pitch (2 d.p.) | `0.00` |

#### Members & Relations (6)

| Placeholder | Returns | Example |
|-------------|---------|---------|
| `%factions_members%` | Total member count | `5` |
| `%factions_members_online%` | Online member count | `3` |
| `%factions_allies%` | Allied faction count | `2` |
| `%factions_enemies%` | Enemy faction count | `1` |
| `%factions_neutrals%` | Neutral relation count | `4` |
| `%factions_relations%` | Total relation count | `7` |

---

## WiFlow PlaceholderAPI

**Package**: `com.hyperfactions.integration.wiflow`
**Format**: `{factions_<placeholder>}`

WiFlow uses curly braces instead of percent signs but supports the same 33 placeholders as PAPI. The `WiFlowExpansion` class mirrors `HyperFactionsExpansion` exactly, using WiFlow's `PlaceholderContext` instead of PAPI's `PlayerRef`.

> **Full Reference**: See [placeholders.md](placeholders.md) for complete placeholder documentation with null behavior, usage examples, and configuration details.

---

## OrbisGuard

**Package**: `com.hyperfactions.integration.orbis`
**Purpose**: Prevents faction claims in OrbisGuard-protected regions

When OrbisGuard is installed, HyperFactions checks for protective regions before allowing territory claims. If a chunk overlaps an OrbisGuard region, the claim is denied.

### How It Works

1. On startup, HyperFactions attempts to load `com.orbisguard.api.OrbisGuardAPI` via reflection
2. If found, it caches `MethodHandle` references for region container access
3. During claim attempts, `isChunkProtected(world, chunkX, chunkZ)` checks the chunk center for regions
4. If regions are found, the claim is denied with an appropriate message

### Methods

| Method | Description |
|--------|-------------|
| `isAvailable()` | Whether OrbisGuard is installed |
| `hasProtectiveRegions(world, x, y, z)` | Check for regions at exact coordinates |
| `isChunkProtected(world, chunkX, chunkZ)` | Check chunk center (block X/Z + 8, Y=64) |

> **Note**: Checks only the chunk center for performance. Region checks are fail-open — if OrbisGuard errors, claims proceed normally.

---

## OrbisGuard-Mixins

**Package**: `com.hyperfactions.integration.orbis`
**Purpose**: Enhanced protection via Hyxin mixin hooks for events not available through normal APIs

OrbisGuard-Mixins extends protection coverage to interactions that Hytale's event system doesn't expose (F-key pickup, hammer cycling, campfire toggling, etc.). It uses `System.getProperties()` for cross-classloader communication — no direct dependency required.

### Detection

Mixins set system properties on load:

| Property | Set By |
|----------|--------|
| `orbisguard.mixins.loaded` | Pickup mixin (may load late) |
| `orbisguard.mixin.durability.loaded` | Durability mixin (loads early, targets `Player.class`) |
| `orbisguard.mixin.pickup.loaded` | Pickup mixin |
| `orbisguard.mixin.death.loaded` | Death mixin |
| `orbisguard.mixin.seating.loaded` | Seating mixin |

HyperFactions accepts any of these as evidence that OrbisGuard-Mixins is installed. A `refreshStatus()` method handles late-loading mixins.

### Hook Types (11)

All hooks are registered in a shared `ConcurrentHashMap` stored in `System.getProperties()` under `orbisguard.hook.registry`. Mixins find hooks via reflection by matching method signatures.

| Hook | Callback Interface | Protects Against |
|------|--------------------|-----------------|
| **Pickup** | `PickupCheckCallback` | F-key and auto item pickup |
| **Hammer** | `HammerCheckCallback` | Hammer block cycling |
| **Harvest** | `HarvestCheckCallback` | F-key crop harvesting |
| **Place** | `PlaceCheckCallback` | Bucket/fluid placement |
| **Use** | `UseCheckCallback` | Block interaction (campfire, lantern toggle) |
| **Seat** | `SeatCheckCallback` | Seating on blocks |
| **Explosion** | `ExplosionCheckCallback` | Explosion block damage |
| **Command** | `CommandCheckCallback` | Command blocking in zones |
| **Death** | `DeathCheckCallback` | Keep inventory on death |
| **Durability** | `DurabilityCheckCallback` | Durability loss prevention |
| **Spawn** | `SpawnCheckCallback` | Mob spawning control |

### Callback Signatures

```java
// Most hooks follow this pattern:
boolean isAllowed(UUID playerUuid, String worldName, int x, int y, int z);

// Exceptions:
// Pickup: check(UUID, String, double, double, double, String mode) -> boolean
// Harvest: check(UUID, String, int, int, int) -> String (null=allowed, non-null=denial message)
// Explosion: shouldBlockExplosion(String worldName, int x, int y, int z) -> boolean (no player)
// Spawn: shouldBlockSpawn(String worldName, int x, int y, int z) -> boolean (no player)
// Command: shouldBlockCommand(UUID, String, int, int, int, String command) -> CommandCheckResult
```

### Fail-Open Design

Every hook wrapper catches exceptions and returns the permissive default:
- `true` (allowed) for permission checks
- `false` (don't block) for block checks
- `null` (allowed) for harvest/string checks

---

## World Map

**Package**: `com.hyperfactions.worldmap`
**Purpose**: Renders faction claims as colored overlays on Hytale's built-in world map

### Components

| Class | Role |
|-------|------|
| `WorldMapService` | Registration with Hytale's world map system |
| `HyperFactionsWorldMapProvider` | Generates claim overlay images |
| `HyperFactionsWorldMap` | Custom world map with faction colors |
| `WorldMapRefreshScheduler` | Manages refresh timing and batching |

### Refresh Modes

The `WorldMapRefreshScheduler` supports 5 modes, configurable in `config/worldmap.json`:

| Mode | Behavior | Best For |
|------|----------|----------|
| **PROXIMITY** (default) | Selective cache clear + notify nearby players only | Busy servers |
| **INCREMENTAL** | Selective cache clear + notify all players | Balanced |
| **DEBOUNCED** | Full refresh after quiet period with no changes | Constant claim activity |
| **IMMEDIATE** | Full refresh on every change | Backwards compatibility |
| **MANUAL** | No auto-refresh; use `/f admin map refresh` | Full control |

Selective modes use `WorldMapManager.clearImagesInChunks()` for targeted invalidation. Full modes use `WorldMapManager.clearImages()`.

### Configuration

Key settings in `config/worldmap.json`:

- `enabled` — Enable/disable world map integration
- `refreshMode` — One of the 5 modes above
- `chunkRadius` — Radius for proximity notifications
- `batchInterval` — Milliseconds between batch updates
- `maxChunksPerBatch` — Throttle for large updates
- `showFactionTags` — Display faction names on the map

---

## HyperPerms Context

When HyperPerms is installed, HyperFactions registers context keys that enable contextual permission grants. For example, you can give members extra permissions only when they're in their own faction's territory.

### Context Keys

| Key | Value | Example |
|-----|-------|---------|
| `faction` | Player's faction name | `Warriors` |
| `faction_role` | Player's role in faction | `leader`, `officer`, `member` |
| `faction_territory` | Territory type at location | `own`, `ally`, `enemy`, `neutral`, `wilderness`, `safezone`, `warzone` |
| `relation` | Relation to territory owner | `self`, `ally`, `enemy`, `neutral` |

### Example Usage in HyperPerms

```
# Allow extra claims only in own territory
/hperms user Steve permission set hyperfactions.limit.claims.100 context faction_territory=own

# Allow ally chat only for officers
/hperms group officers permission set hyperfactions.chat.ally context faction_role=officer
```
