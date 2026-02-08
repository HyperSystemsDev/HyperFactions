# HyperFactions Developer Documentation

> **Version**: 0.7.0 | **302 classes** | **50 packages** | **14 managers** | **43 commands** | **47+ permissions**

Developer documentation for HyperFactions - a comprehensive faction management plugin for Hytale servers.

## Documentation Index

### Core Architecture

| Document | Description |
|----------|-------------|
| [architecture.md](architecture.md) | High-level architecture overview, 9-layer design, package structure |
| [managers.md](managers.md) | Manager layer - 14 managers with responsibilities and dependency graph |

### Systems

| Document | Description |
|----------|-------------|
| [commands.md](commands.md) | Command system - 43 subcommands across 9 categories |
| [permissions.md](permissions.md) | Permission framework - 47+ nodes, chain-based resolution |
| [config.md](config.md) | Config system - ConfigManager, 7 modules, config v4 migration |
| [storage.md](storage.md) | Storage layer - interfaces, JSON adapters, backup system |
| [gui.md](gui.md) | GUI system - 40+ pages, 3 registries, navigation flows |
| [protection.md](protection.md) | Protection system - ECS handlers, OrbisGuard-Mixins hooks |

### API & Integrations

| Document | Description |
|----------|-------------|
| [api.md](api.md) | Developer API reference - HyperFactionsAPI, EconomyAPI, EventBus |
| [integrations.md](integrations.md) | Integration breakdown - permissions, PAPI, WiFlow, OrbisGuard, world map |

### Feature Documentation

| Document | Description |
|----------|-------------|
| [announcements.md](announcements.md) | Announcement system - 7 event types, config, admin exclusions |
| [data-import.md](data-import.md) | Data import & migration - ElbaphFactions/HyFactions importers, config v1→v4 |

## Quick Start

### Entry Points

| File | Purpose |
|------|---------|
| [`platform/HyperFactionsPlugin.java`](../src/main/java/com/hyperfactions/platform/HyperFactionsPlugin.java) | Hytale plugin lifecycle (`setup()` → `start()` → `shutdown()`) |
| [`HyperFactions.java`](../src/main/java/com/hyperfactions/HyperFactions.java) | Core singleton, manager initialization, platform callbacks |
| [`Permissions.java`](../src/main/java/com/hyperfactions/Permissions.java) | All 47+ permission node constants |
| [`api/HyperFactionsAPI.java`](../src/main/java/com/hyperfactions/api/HyperFactionsAPI.java) | Public API for third-party mods |

### Key Patterns

**Manager Access**:
```java
HyperFactions core = HyperFactionsPlugin.getInstance().getHyperFactions();
FactionManager factions = core.getFactionManager();
ClaimManager claims = core.getClaimManager();
```

**Public API** (for third-party mods):
```java
if (HyperFactionsAPI.isAvailable()) {
    Faction faction = HyperFactionsAPI.getPlayerFaction(playerUuid);
    EventBus.register(FactionCreateEvent.class, event -> { ... });
}
```

**Config Access**:
```java
ConfigManager config = ConfigManager.get();
int maxMembers = config.getMaxMembers();
boolean pvpEnabled = config.isFactionDamage();
```

**Permission Check**:
```java
PermissionManager.get().hasPermission(playerUuid, Permissions.CLAIM);
```

## Package Overview

```
src/main/java/com/hyperfactions/         (302 classes, 50 packages)
├── HyperFactions.java          # Core singleton
├── Permissions.java            # 47+ permission node constants
├── BuildInfo.java              # Auto-generated version info
├── platform/                   # Hytale plugin entry point
├── manager/                    # Business logic (14 managers)
├── command/                    # Command system (43 subcommands)
├── gui/                        # CustomUI pages (40+ pages)
├── protection/                 # Territory/zone protection + ECS handlers
├── config/                     # Configuration (7 module configs)
├── storage/                    # Data persistence layer
├── data/                       # Data models (records)
├── api/                        # Public API, EventBus, EconomyAPI
├── integration/                # Permissions, PAPI, WiFlow, OrbisGuard
├── backup/                     # GFS backup management
├── migration/                  # Config migration (v1→v2→v3→v4)
├── importer/                   # ElbaphFactions + HyFactions importers
├── worldmap/                   # World map integration (5 refresh modes)
├── territory/                  # Territory notifications
├── update/                     # Update checking
├── chat/                       # Chat formatting
├── listener/                   # Event listeners
├── debug/                      # Debug utilities
└── util/                       # Utilities (Logger, ChunkUtil, etc.)
```

## Tech Stack

- **Language**: Java 25 (records, pattern matching)
- **Build**: Gradle 9.3.0 with Shadow 9.3.1
- **Platform**: Hytale Server API
- **Storage**: JSON files with async CompletableFuture
- **GUI**: Hytale CustomUI (InteractiveCustomUIPage)
- **Dependencies**: Gson 2.11.0, JetBrains Annotations

## Related Documentation

- [CHANGELOG.md](../CHANGELOG.md) - Version history
- [README.md](../README.md) - User-facing plugin documentation
