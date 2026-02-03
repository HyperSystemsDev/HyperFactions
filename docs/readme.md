# HyperFactions Developer Documentation

Developer documentation for HyperFactions v0.3.x - a comprehensive faction management plugin for Hytale servers.

## Documentation Index

### Core Architecture

| Document | Description |
|----------|-------------|
| [architecture.md](architecture.md) | High-level architecture overview, package structure, core components |
| [managers.md](managers.md) | Manager layer - 12 managers with responsibilities and patterns |

### Systems

| Document | Description |
|----------|-------------|
| [commands.md](commands.md) | Command system architecture - dispatcher, subcommands, context |
| [permissions.md](permissions.md) | Permission framework - Permissions.java, HyperPerms integration |
| [config.md](config.md) | Config system architecture - ConfigManager, modules, validation |
| [storage.md](storage.md) | Storage layer - interfaces, JSON adapters, async patterns |
| [gui.md](gui.md) | GUI system - page lifecycle, registries, navigation |
| [protection.md](protection.md) | Protection system - ECS handlers, zone checks, PvP resolution |

## Quick Start

### Entry Points

| File | Purpose |
|------|---------|
| [`platform/HyperFactionsPlugin.java`](../src/main/java/com/hyperfactions/platform/HyperFactionsPlugin.java) | Hytale plugin lifecycle (`setup()` → `start()` → `shutdown()`) |
| [`HyperFactions.java`](../src/main/java/com/hyperfactions/HyperFactions.java) | Core singleton, manager initialization, platform callbacks |
| [`Permissions.java`](../src/main/java/com/hyperfactions/Permissions.java) | All permission node constants |

### Key Patterns

**Manager Access**:
```java
HyperFactions core = HyperFactionsPlugin.getInstance().getHyperFactions();
FactionManager factions = core.getFactionManager();
ClaimManager claims = core.getClaimManager();
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
src/main/java/com/hyperfactions/
├── HyperFactions.java          # Core singleton
├── Permissions.java            # Permission node constants
├── BuildInfo.java              # Auto-generated version info
├── platform/                   # Hytale plugin entry point
├── manager/                    # Business logic (12 managers)
├── command/                    # Command system (40+ subcommands)
├── gui/                        # CustomUI pages and navigation
├── protection/                 # Territory and zone protection
├── config/                     # Configuration management
├── storage/                    # Data persistence layer
├── data/                       # Data models (records)
├── api/                        # Public API and events
├── integration/                # HyperPerms, economy integration
├── backup/                     # Backup management
├── update/                     # Update checking
├── territory/                  # Territory notifications, world map
└── util/                       # Utilities (Logger, ChunkUtil, etc.)
```

## Tech Stack

- **Language**: Java 21 (records, pattern matching, virtual threads)
- **Build**: Gradle 8.12 with Shadow plugin
- **Platform**: Hytale Server API
- **Storage**: JSON files with async CompletableFuture
- **GUI**: Hytale CustomUI (InteractiveCustomUIPage)
- **Dependencies**: Gson 2.11.0, Caffeine (caching)

## Related Documentation

- [CHANGELOG.md](../CHANGELOG.md) - Version history
- [README.md](../README.md) - User-facing plugin documentation
