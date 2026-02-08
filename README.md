# HyperFactions

[![Discord](https://img.shields.io/badge/Discord-Join%20Us-7289DA?logo=discord&logoColor=white)](https://discord.gg/SNPjyfkYPc)
[![GitHub](https://img.shields.io/github/stars/HyperSystemsDev/HyperFactions?style=social)](https://github.com/HyperSystemsDev/HyperFactions)

A comprehensive faction management mod for Hytale servers featuring territory claims, alliances, strategic PvP, power systems, and extensive customization. Part of the **HyperSystems** plugin suite.

**Version:** 0.7.0
**Game:** Hytale Early Access
**License:** GPLv3

---

## Overview

HyperFactions transforms your Hytale server into a dynamic faction-based environment where players create factions, claim territories, forge alliances, and engage in strategic PvP combat. Built on the proven HyperSystems architecture with native HyperPerms integration.

**Main Commands:** `/faction` | `/f` | `/hf`

---

## Key Features

### Faction Management
- Create & manage factions with unique names and customizable colors
- Three-tier role hierarchy: **Leader**, **Officer**, **Member**
- Smart invitation system with configurable expiration
- Activity logging with configurable history size
- Per-faction JSON storage for data isolation
- Configurable member limits (default: 50)
- Disband confirmation to prevent accidents

### Territory System
- Chunk-based claiming with visual feedback
- Integrated world map display with faction colors
- Power-based claim limits for balanced expansion
- Adjacent claims mode (optional) for connected territories
- Anti-fragmentation protection when unclaiming
- Overclaiming system for strategic warfare
- Inactive faction decay (configurable)
- Multi-world/dimension support

### Power System
- Personal power per player (configurable cap)
- Faction power = sum of member contributions
- Automatic regeneration at configurable intervals
- Power loss on death (configurable)
- Power cost per claim (configurable)
- No power loss in WarZones
- Real-time recalculation on member changes

### Diplomatic Relations
- **Allies** - Mutual protection, shared land access, no friendly fire
- **Enemies** - Full PvP enabled, overclaiming available
- **Neutral** - Default state, PvP with penalties
- Alliance request/accept/reject workflow
- Color-coded displays in GUI and chat

### Home & Teleportation
- Faction home with exact position and rotation preservation
- Cross-dimension teleportation support
- Configurable warmup and cooldown
- Permission-based access control

### Combat System
- Combat tagging to prevent logout exploitation
- Relationship-based PvP rules
- Death detection via ECS components
- Combat logging with timestamps
- Configurable tag duration

### Zone System
- **SafeZones** (Green) - Complete PvP protection, no building
- **WarZones** (Red) - PvP enabled without power penalties
- Visual indicators on map and in-game
- Admin-only zone management

### Communication
- **Faction Chat** (`/f c`) - Private faction messaging
- **Alliance Chat** (`/f a`) - Coordinate with allied factions
- Distinct formatting with faction colors
- Permission-controlled access

### Protection System
- Block placement/breaking protection
- Container and mechanism interaction protection
- Fluid placement protection
- Item pickup protection in claimed areas
- Allied access (configurable)
- Admin bypass mode
- OrbisGuard-Mixins integration (11 hook types)
- Mob spawn suppression in claims/zones

### GUI Interface
- Interactive main menu with faction stats
- Chunk management with integrated minimap
- 15x15 chunk grid visualization
- Alliance management interface
- Faction browser with search
- Multi-page help wiki (40+ pages across 3 registries)

### Economy System
- Faction treasury with balance tracking
- Deposit, withdraw, and inter-faction transfers
- Transaction history (max 50 per faction)
- Configurable currency formatting

### Server Announcements
- Server-wide broadcasts for faction events
- 7 event types with individual toggles
- Admin actions excluded from announcements

### Data Import & Migration
- Import from ElbaphFactions and HyFactions
- Automatic config migration (v1→v2→v3→v4) with rollback
- Pre-import backup creation

### World Map Integration
- Faction claim overlays on Hytale's world map
- 5 refresh modes for performance tuning
- Faction tag display on map

### Placeholder Support
- 33 placeholders via PlaceholderAPI (`%factions_xxx%`)
- 33 placeholders via WiFlow (`{factions_xxx}`)

### Public API
- Full API for third-party mod developers
- EventBus with 4 faction events
- EconomyAPI interface

---

## Commands

> 43 subcommands across 9 categories. Permission nodes follow `hyperfactions.<category>.<action>` hierarchy.

### Faction Management

| Command | Description | Permission |
|---------|-------------|------------|
| `/f` or `/f menu` | Open faction GUI | `hyperfactions.use` |
| `/f create <name>` | Create a new faction | `hyperfactions.faction.create` |
| `/f disband` | Dissolve faction (Leader only) | `hyperfactions.faction.disband` |
| `/f rename <name>` | Change faction name (Leader) | `hyperfactions.faction.rename` |
| `/f desc <text>` | Set faction description (Officer+) | `hyperfactions.faction.description` |
| `/f tag <tag>` | Set faction tag (Officer+) | `hyperfactions.faction.tag` |
| `/f color <code>` | Set faction color (Officer+) | `hyperfactions.faction.color` |
| `/f open` | Allow anyone to join (Leader) | `hyperfactions.faction.open` |
| `/f close` | Require invite to join (Leader) | `hyperfactions.faction.close` |

### Membership

| Command | Description | Permission |
|---------|-------------|------------|
| `/f invite <player>` | Invite player to faction | `hyperfactions.member.invite` |
| `/f accept` | Accept faction invitation | `hyperfactions.member.join` |
| `/f leave` | Leave your faction | `hyperfactions.member.leave` |
| `/f kick <player>` | Remove player from faction | `hyperfactions.member.kick` |
| `/f promote <player>` | Promote to Officer (Leader only) | `hyperfactions.member.promote` |
| `/f demote <player>` | Demote to Member (Leader only) | `hyperfactions.member.demote` |
| `/f transfer <player>` | Transfer leadership | `hyperfactions.member.transfer` |

### Territory

| Command | Description | Permission |
|---------|-------------|------------|
| `/f claim` | Claim current chunk | `hyperfactions.territory.claim` |
| `/f unclaim` | Unclaim current chunk | `hyperfactions.territory.unclaim` |
| `/f map` | Open chunk management GUI | `hyperfactions.territory.map` |
| `/f overclaim` | Capture enemy chunk (requires 0 power) | `hyperfactions.territory.overclaim` |

### Teleportation

| Command | Description | Permission |
|---------|-------------|------------|
| `/f home` | Teleport to faction home | `hyperfactions.teleport.home` |
| `/f sethome` | Set faction home (Leader only) | `hyperfactions.teleport.sethome` |
| `/f delhome` | Delete faction home (Leader only) | `hyperfactions.teleport.delhome` |
| `/f stuck` | Escape from enemy territory (30s warmup) | `hyperfactions.teleport.stuck` |

### Diplomacy

| Command | Description | Permission |
|---------|-------------|------------|
| `/f ally <faction>` | Send alliance request | `hyperfactions.relation.ally` |
| `/f enemy <faction>` | Declare faction as enemy | `hyperfactions.relation.enemy` |
| `/f neutral <faction>` | Set neutral relationship | `hyperfactions.relation.neutral` |
| `/f relations` | View all faction relationships | `hyperfactions.relation.view` |

### Communication

| Command | Description | Permission |
|---------|-------------|------------|
| `/f c <message>` | Send faction chat message | `hyperfactions.chat.faction` |
| `/f a <message>` | Send alliance chat message | `hyperfactions.chat.ally` |

### Information

| Command | Description | Permission |
|---------|-------------|------------|
| `/f info [faction]` | View faction information | `hyperfactions.info.faction` |
| `/f list` | List all factions | `hyperfactions.info.list` |
| `/f who <player>` | View player info | `hyperfactions.info.player` |
| `/f power` | View power info | `hyperfactions.info.power` |
| `/f members` | View faction members | `hyperfactions.info.members` |
| `/f logs` | View faction activity logs | `hyperfactions.info.logs` |
| `/f help` | Open help menu | `hyperfactions.info.help` |

### Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/f admin` | Open admin menu | `hyperfactions.admin.use` |
| `/f admin claim` | Admin claim (unlimited) | `hyperfactions.admin.bypass.limits` |
| `/f admin unclaim` | Admin unclaim (any faction) | `hyperfactions.admin.modify` |
| `/f admin bypass` | Toggle bypass mode | `hyperfactions.admin.use` |
| `/f admin safezone add` | Create SafeZone | `hyperfactions.admin.zones` |
| `/f admin safezone remove` | Remove SafeZone | `hyperfactions.admin.zones` |
| `/f admin warzone add` | Create WarZone | `hyperfactions.admin.zones` |
| `/f admin warzone remove` | Remove WarZone | `hyperfactions.admin.zones` |
| `/f admin disband <faction>` | Force disband any faction | `hyperfactions.admin.disband` |
| `/f admin backup` | Manage backups | `hyperfactions.admin.backup` |
| `/f reload` | Reload configuration | `hyperfactions.admin.reload` |

---

## Permissions

> 47+ permission nodes across 10 categories. Supports wildcards (e.g., `hyperfactions.faction.*`).

### Faction Management

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.use` | Basic faction access (GUI) | true |
| `hyperfactions.faction.create` | Create factions | true |
| `hyperfactions.faction.disband` | Disband your faction | true |
| `hyperfactions.faction.rename` | Rename your faction | true |
| `hyperfactions.faction.description` | Set faction description | true |
| `hyperfactions.faction.tag` | Set faction tag | true |
| `hyperfactions.faction.color` | Set faction color | true |
| `hyperfactions.faction.open` | Make faction open | true |
| `hyperfactions.faction.close` | Make faction closed | true |
| `hyperfactions.faction.permissions` | Edit territory permissions | true |

### Membership

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.member.invite` | Invite players | true |
| `hyperfactions.member.join` | Accept invites / join | true |
| `hyperfactions.member.leave` | Leave faction | true |
| `hyperfactions.member.kick` | Kick members | true |
| `hyperfactions.member.promote` | Promote members | true |
| `hyperfactions.member.demote` | Demote members | true |
| `hyperfactions.member.transfer` | Transfer leadership | true |

### Territory

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.territory.claim` | Claim territory | true |
| `hyperfactions.territory.unclaim` | Unclaim territory | true |
| `hyperfactions.territory.overclaim` | Overclaim enemy territory | true |
| `hyperfactions.territory.map` | View territory map | true |

### Teleportation

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.teleport.home` | Use faction home | true |
| `hyperfactions.teleport.sethome` | Set faction home | true |
| `hyperfactions.teleport.delhome` | Delete faction home | true |
| `hyperfactions.teleport.stuck` | Use /f stuck | true |

### Diplomacy & Communication

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.relation.ally` | Manage alliances | true |
| `hyperfactions.relation.enemy` | Declare enemies | true |
| `hyperfactions.relation.neutral` | Set neutral relations | true |
| `hyperfactions.relation.view` | View relations | true |
| `hyperfactions.chat.faction` | Use faction chat | true |
| `hyperfactions.chat.ally` | Use alliance chat | true |

### Information

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.info.faction` | View faction info | true |
| `hyperfactions.info.list` | View faction list | true |
| `hyperfactions.info.player` | View player info | true |
| `hyperfactions.info.power` | View power info | true |
| `hyperfactions.info.members` | View members | true |
| `hyperfactions.info.logs` | View activity logs | true |
| `hyperfactions.info.help` | View help | true |

### Bypass Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.bypass.build` | Bypass block protection | op |
| `hyperfactions.bypass.interact` | Bypass interaction protection | op |
| `hyperfactions.bypass.container` | Bypass container protection | op |
| `hyperfactions.bypass.damage` | Bypass entity damage protection | op |
| `hyperfactions.bypass.use` | Bypass item use protection | op |
| `hyperfactions.bypass.warmup` | Bypass home warmup | false |
| `hyperfactions.bypass.cooldown` | Bypass home cooldown | false |

### Admin Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.admin.use` | Base admin access | op |
| `hyperfactions.admin.reload` | Reload configuration | op |
| `hyperfactions.admin.debug` | Debug commands | op |
| `hyperfactions.admin.zones` | Manage safe/war zones | op |
| `hyperfactions.admin.disband` | Force disband factions | op |
| `hyperfactions.admin.modify` | Modify any faction | op |
| `hyperfactions.admin.bypass.limits` | Bypass claim limits | op |
| `hyperfactions.admin.backup` | Manage backups | op |

### Limit Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.limit.claims.<N>` | Override max claims | — |
| `hyperfactions.limit.power.<N>` | Override max power | — |

---

## Configuration

Configuration file: `mods/com.hyperfactions_HyperFactions/config.json`

```json
{
  "faction": {
    "maxMembers": 50,
    "maxNameLength": 24,
    "minNameLength": 3,
    "allowedNamePattern": "^[a-zA-Z0-9_]+$"
  },
  "power": {
    "maxPlayerPower": 20,
    "startingPower": 10,
    "powerPerClaim": 2,
    "regenerationMinutes": 60,
    "regenerationAmount": 1,
    "deathPenalty": 1,
    "neutralKillPenalty": 1,
    "warzonePowerLoss": false
  },
  "claims": {
    "maxClaims": 100,
    "onlyAdjacent": false,
    "decayEnabled": true,
    "decayDelayMinutes": 10,
    "inactiveDecayDays": 7,
    "showOnMap": true
  },
  "overclaim": {
    "enabled": true,
    "requireEnemyRelation": true,
    "powerThreshold": 0
  },
  "combat": {
    "tagDurationSeconds": 15,
    "allyDamage": false,
    "factionDamage": false
  },
  "teleport": {
    "warmupSeconds": 5,
    "cooldownMinutes": 5,
    "cancelOnMove": true,
    "cancelOnDamage": true,
    "crossWorld": true
  },
  "integration": {
    "hyperpermsEnabled": true,
    "showFactionInChat": true,
    "showFactionOnMap": true
  },
  "messages": {
    "prefix": "[HyperFactions] ",
    "primaryColor": "#00FFFF",
    "secondaryColor": "#AAAAAA",
    "errorColor": "#FF5555",
    "successColor": "#55FF55"
  }
}
```

---

## Role Hierarchy

### Leader
- Full control over faction
- Set faction home
- Promote/demote members
- Transfer leadership
- Disband faction
- Manage all relations
- Kick any member

### Officer
- Invite new players
- Kick Members (not Officers)
- Manage diplomatic relations
- Claim/unclaim territory
- Use overclaim

### Member
- Claim territory (with permission)
- Use faction home
- View faction info
- Participate in chat

---

## Integrations

### HyperPerms

HyperFactions automatically integrates with HyperPerms when available:

- Chain-of-responsibility permission resolution: VaultUnlocked → HyperPerms → LuckPerms
- Automatic faction prefix in chat: `[FactionName] Player: message`
- Context keys: `faction`, `faction_role`, `faction_territory`, `relation`
- No configuration required — just install both mods

### PlaceholderAPI / WiFlow

33 placeholders available under the `factions` identifier:

| Placeholder | Description |
|-------------|-------------|
| `factions_faction_name` | Player's faction name |
| `factions_faction_role` | Player's role (Leader/Officer/Member) |
| `factions_faction_power` | Faction total power |
| `factions_player_power` | Player's personal power |
| `factions_faction_claims` | Number of claimed chunks |
| `factions_faction_members` | Member count |
| `factions_faction_color` | Faction color hex code |
| `factions_territory_owner` | Faction owning current chunk |

Access via PlaceholderAPI (`%factions_xxx%`) or WiFlow (`{factions_xxx}`). See [developer docs](docs/integrations.md) for the full list.

### OrbisGuard-Mixins

11 hook types for comprehensive territory protection. See [Protection System](docs/protection.md).

---

## Data Storage

```
mods/com.hyperfactions_HyperFactions/
├── config.json                    # Main configuration
├── announcements.json             # Announcement toggles
├── worldmap.json                  # World map settings
├── data/
│   ├── factions/
│   │   ├── {uuid}.json            # Per-faction data
│   │   └── ...
│   ├── claims.json                # Global claims registry
│   ├── safezones.json             # SafeZone definitions
│   ├── warzones.json              # WarZone definitions
│   └── players/
│       ├── {uuid}.json            # Player power & preferences
│       └── ...
├── backups/
│   ├── hourly/                    # Hourly auto-backups
│   ├── daily/                     # Daily backups
│   ├── weekly/                    # Weekly backups
│   ├── manual/                    # Admin-triggered backups
│   └── migration/                 # Pre-migration backups
└── imports/                       # Data import staging
```

---

## Building from Source

### Requirements

- Java 25 (build and runtime)
- Gradle 9.3.0+
- Hytale Server (Early Access)
- Optional: HyperPerms (for enhanced permission control)

```bash
# From HyperSystems root (multi-project build)
./gradlew :HyperFactions:shadowJar
```

The output JAR will be in `build/libs/`.

---

## Support

- **Discord:** https://discord.gg/SNPjyfkYPc
- **GitHub Issues:** https://github.com/HyperSystemsDev/HyperFactions/issues

---

## Credits

Developed by **HyperSystemsDev**

Part of the **HyperSystems** plugin suite:
- [HyperPerms](https://github.com/HyperSystemsDev/HyperPerms) - Advanced permissions
- [HyperHomes](https://github.com/HyperSystemsDev/HyperHomes) - Home teleportation
- [HyperFactions](https://github.com/HyperSystemsDev/HyperFactions) - Faction management
- [HyperWarp](https://github.com/HyperSystemsDev/HyperWarp) - Warps, spawns, TPA

---

*HyperFactions - Forge Your Empire*
