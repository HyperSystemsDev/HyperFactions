# HyperFactions

A comprehensive faction management mod for Hytale servers featuring territory claims, alliances, strategic PvP, power systems, and extensive customization. Part of the **HyperSystems** plugin suite.

**Version:** 0.2.0
**Game:** Hytale Early Access
**License:** GLPv3

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

### GUI Interface
- Interactive main menu with faction stats
- Chunk management with integrated minimap
- 15x15 chunk grid visualization
- Alliance management interface
- Faction browser with search
- Multi-page help wiki

---

## Commands

### Basic Faction Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/f` or `/f menu` | Open faction GUI | `hyperfactions.use` |
| `/f create <name>` | Create a new faction | `hyperfactions.create` |
| `/f invite <player>` | Invite player to faction | `hyperfactions.invite` |
| `/f accept` | Accept faction invitation | `hyperfactions.use` |
| `/f leave` | Leave your faction | `hyperfactions.use` |
| `/f kick <player>` | Remove player from faction | `hyperfactions.kick` |
| `/f disband` | Dissolve faction (Leader only) | `hyperfactions.disband` |

### Faction Settings

| Command | Description | Permission |
|---------|-------------|------------|
| `/f rename <name>` | Change faction name (Leader) | `hyperfactions.rename` |
| `/f desc <text>` | Set faction description (Officer+) | `hyperfactions.desc` |
| `/f color <code>` | Set faction color (Officer+) | `hyperfactions.color` |
| `/f open` | Allow anyone to join (Leader) | `hyperfactions.open` |
| `/f close` | Require invite to join (Leader) | `hyperfactions.close` |

### Territory Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/f claim` | Claim current chunk | `hyperfactions.claim` |
| `/f unclaim` | Unclaim current chunk | `hyperfactions.unclaim` |
| `/f map` | Open chunk management GUI | `hyperfactions.map` |
| `/f overclaim` | Capture enemy chunk (requires 0 power) | `hyperfactions.overclaim` |
| `/f stuck` | Escape from enemy territory (30s warmup) | `hyperfactions.use` |

### Home & Teleportation

| Command | Description | Permission |
|---------|-------------|------------|
| `/f home` | Teleport to faction home | `hyperfactions.home` |
| `/f sethome` | Set faction home (Leader only) | `hyperfactions.sethome` |

### Diplomacy Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/f ally <faction>` | Send alliance request | `hyperfactions.ally` |
| `/f enemy <faction>` | Declare faction as enemy | `hyperfactions.enemy` |
| `/f neutral <faction>` | Set neutral relationship | `hyperfactions.neutral` |
| `/f relations` | View all faction relationships | `hyperfactions.use` |

### Member Management

| Command | Description | Permission |
|---------|-------------|------------|
| `/f promote <player>` | Promote to Officer (Leader only) | `hyperfactions.promote` |
| `/f demote <player>` | Demote to Member (Leader only) | `hyperfactions.demote` |
| `/f transfer <player>` | Transfer leadership | `hyperfactions.transfer` |

### Communication

| Command | Description | Permission |
|---------|-------------|------------|
| `/f c <message>` | Send faction chat message | `hyperfactions.chat.faction` |
| `/f a <message>` | Send alliance chat message | `hyperfactions.chat.ally` |

### Information Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/f info [faction]` | View faction information | `hyperfactions.use` |
| `/f list` | List all factions | `hyperfactions.use` |
| `/f logs` | View faction activity logs | `hyperfactions.logs` |
| `/f help` | Open help menu | `hyperfactions.use` |

### Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/f admin` | Open admin menu | `hyperfactions.admin` |
| `/f admin claim` | Admin claim (unlimited) | `hyperfactions.admin` |
| `/f admin unclaim` | Admin unclaim (any faction) | `hyperfactions.admin` |
| `/f admin bypass` | Toggle bypass mode | `hyperfactions.admin` |
| `/f admin safezone add` | Create SafeZone | `hyperfactions.admin` |
| `/f admin safezone remove` | Remove SafeZone | `hyperfactions.admin` |
| `/f admin warzone add` | Create WarZone | `hyperfactions.admin` |
| `/f admin warzone remove` | Remove WarZone | `hyperfactions.admin` |
| `/f reload` | Reload configuration | `hyperfactions.admin` |

---

## Permissions

### Core Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.use` | Basic faction access | true |
| `hyperfactions.create` | Create factions | true |
| `hyperfactions.invite` | Invite players | true |
| `hyperfactions.claim` | Claim territory | true |
| `hyperfactions.unclaim` | Unclaim territory | true |
| `hyperfactions.home` | Use faction home | true |
| `hyperfactions.sethome` | Set faction home | true |
| `hyperfactions.kick` | Kick members | true |
| `hyperfactions.promote` | Promote members | true |
| `hyperfactions.demote` | Demote members | true |
| `hyperfactions.ally` | Manage alliances | true |
| `hyperfactions.enemy` | Declare enemies | true |
| `hyperfactions.neutral` | Set neutral relations | true |
| `hyperfactions.disband` | Dissolve faction | true |
| `hyperfactions.overclaim` | Overclaim enemy territory | true |
| `hyperfactions.chat.faction` | Use faction chat | true |
| `hyperfactions.chat.ally` | Use alliance chat | true |
| `hyperfactions.logs` | View activity logs | true |
| `hyperfactions.admin` | Full admin access | op |

### Bypass Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.bypass.warmup` | Bypass home warmup | false |
| `hyperfactions.bypass.cooldown` | Bypass home cooldown | false |
| `hyperfactions.bypass.protection` | Bypass claim protection | op |

---

## Configuration

Configuration file: `plugins/HyperFactions/config.json`

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

## HyperPerms Integration

HyperFactions automatically integrates with HyperPerms when available:

- Automatic faction prefix in chat: `[FactionName][Rank] Player: message`
- Optional rank display in chat format
- Placeholders:
  - `%faction%` - Player's faction name
  - `%faction_rank%` - Player's faction rank
  - `%faction_power%` - Player's current power

No configuration required - just install both mods.

---

## Data Storage

```
plugins/HyperFactions/
├── config.json
├── data/
│   ├── factions/
│   │   ├── {uuid}.json          # Per-faction data
│   │   └── ...
│   ├── claims.json              # Global claims registry
│   ├── safezones.json           # SafeZone definitions
│   ├── warzones.json            # WarZone definitions
│   └── players/
│       ├── {uuid}.json          # Player power & preferences
│       └── ...
```

---

## Building from Source

```bash
./gradlew build
```

The output JAR will be in `build/libs/`.

---

## Requirements

- Hytale Server (Early Access)
- Java 21+
- Optional: HyperPerms (for enhanced permission control)

---

## Support

- **Discord:** https://discord.gg/SNPjyfkYPc
- **GitHub Issues:** https://github.com/ZenithDevHQ/HyperFactions/issues

---

## Credits

Developed by **ZenithDevHQ**

Part of the **HyperSystems** plugin suite alongside:
- [HyperPerms](https://github.com/ZenithDevHQ/HyperPerms) - Advanced permissions
- [HyperHomes](https://github.com/ZenithDevHQ/HyperHomes) - Home teleportation

---

*HyperFactions - Forge Your Empire*
