# HyperFactions Configuration

This document details the complete configuration system for HyperFactions.

## Overview

HyperFactions uses a JSON-based configuration system. The config file is located at:

```
<server>/mods/com.hyperfactions_HyperFactions/config.json
```

### Behavior

- **First run**: If `config.json` doesn't exist, a default config is created with all settings.
- **Missing keys**: When the config is loaded, any missing keys are set to their default values, and the config is automatically saved with the new keys added.
- **Reload**: Use `/f admin reload` to reload the config without restarting the server.

## Configuration Sections

### faction

Basic faction settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `maxMembers` | int | `50` | Maximum members per faction |
| `maxNameLength` | int | `24` | Maximum faction name length |
| `minNameLength` | int | `3` | Minimum faction name length |
| `allowColors` | bool | `true` | Allow color codes in faction names |

```json
"faction": {
  "maxMembers": 50,
  "maxNameLength": 24,
  "minNameLength": 3,
  "allowColors": true
}
```

### power

Power mechanics control territory claiming limits.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `maxPlayerPower` | double | `20.0` | Maximum power a player can have |
| `startingPower` | double | `10.0` | Power given to new players |
| `powerPerClaim` | double | `2.0` | Power required per claimed chunk |
| `deathPenalty` | double | `1.0` | Power lost on death |
| `regenPerMinute` | double | `0.1` | Power regenerated per minute |
| `regenWhenOffline` | bool | `false` | Regenerate power while offline |

```json
"power": {
  "maxPlayerPower": 20.0,
  "startingPower": 10.0,
  "powerPerClaim": 2.0,
  "deathPenalty": 1.0,
  "regenPerMinute": 0.1,
  "regenWhenOffline": false
}
```

**Claim Limit Formula**: `max_claims = min(floor(total_faction_power / powerPerClaim), maxClaims)`

### claims

Territory claiming settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `maxClaims` | int | `100` | Hard limit on claims per faction |
| `onlyAdjacent` | bool | `false` | Require claims to be adjacent |
| `decayEnabled` | bool | `true` | Enable claim decay for inactive factions |
| `decayDaysInactive` | int | `30` | Days before claims start decaying |
| `worldWhitelist` | array | `[]` | Only allow claiming in these worlds |
| `worldBlacklist` | array | `[]` | Prevent claiming in these worlds |

```json
"claims": {
  "maxClaims": 100,
  "onlyAdjacent": false,
  "decayEnabled": true,
  "decayDaysInactive": 30,
  "worldWhitelist": [],
  "worldBlacklist": []
}
```

**World filtering logic**:
- If `worldWhitelist` is set, only those worlds allow claiming
- If `worldBlacklist` is set, those worlds are excluded
- If neither is set, all worlds allow claiming

### combat

Combat and PvP settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `tagDurationSeconds` | int | `15` | Combat tag duration |
| `allyDamage` | bool | `false` | Allow damage between allies |
| `factionDamage` | bool | `false` | Allow damage between faction members |
| `taggedLogoutPenalty` | bool | `true` | Punish logout while combat tagged |

#### combat.spawnProtection

Spawn protection (anti-spawnkill) settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | `true` | Enable spawn protection |
| `durationSeconds` | int | `5` | Protection duration after respawn |
| `breakOnAttack` | bool | `true` | Remove protection if player attacks |
| `breakOnMove` | bool | `true` | Remove protection if player moves |

```json
"combat": {
  "tagDurationSeconds": 15,
  "allyDamage": false,
  "factionDamage": false,
  "taggedLogoutPenalty": true,
  "spawnProtection": {
    "enabled": true,
    "durationSeconds": 5,
    "breakOnAttack": true,
    "breakOnMove": true
  }
}
```

### relations

Diplomatic relation limits.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `maxAllies` | int | `10` | Maximum ally factions (-1 = unlimited) |
| `maxEnemies` | int | `-1` | Maximum enemy factions (-1 = unlimited) |

```json
"relations": {
  "maxAllies": 10,
  "maxEnemies": -1
}
```

### invites

Invitation and join request settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `inviteExpirationMinutes` | int | `5` | How long faction invites last |
| `joinRequestExpirationHours` | int | `24` | How long join requests last |

```json
"invites": {
  "inviteExpirationMinutes": 5,
  "joinRequestExpirationHours": 24
}
```

### stuck

Settings for `/f stuck` command (teleport out of enemy territory).

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `warmupSeconds` | int | `30` | Warmup before teleport |
| `cooldownSeconds` | int | `300` | Cooldown between uses (5 min) |

```json
"stuck": {
  "warmupSeconds": 30,
  "cooldownSeconds": 300
}
```

### teleport

Settings for `/f home` teleportation.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `warmupSeconds` | int | `5` | Warmup before teleport |
| `cooldownSeconds` | int | `300` | Cooldown between teleports |
| `cancelOnMove` | bool | `true` | Cancel teleport if player moves |
| `cancelOnDamage` | bool | `true` | Cancel teleport if player takes damage |

```json
"teleport": {
  "warmupSeconds": 5,
  "cooldownSeconds": 300,
  "cancelOnMove": true,
  "cancelOnDamage": true
}
```

### updates

Automatic update checking settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | `true` | Enable update checking |
| `url` | string | GitHub API URL | Update check endpoint |
| `releaseChannel` | string | `"stable"` | `"stable"` or `"prerelease"` |

```json
"updates": {
  "enabled": true,
  "url": "https://api.github.com/repos/ZenithDevHQ/HyperFactions/releases/latest",
  "releaseChannel": "stable"
}
```

**Release Channels**:
- `stable`: Only checks for stable releases (recommended for production)
- `prerelease`: Includes pre-release versions (for testing new features)

### autoSave

Auto-save settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | `true` | Enable automatic saving |
| `intervalMinutes` | int | `5` | Save interval in minutes |

```json
"autoSave": {
  "enabled": true,
  "intervalMinutes": 5
}
```

### economy

Economy integration settings (for future use).

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | `true` | Enable economy features |
| `currencyName` | string | `"dollar"` | Singular currency name |
| `currencyNamePlural` | string | `"dollars"` | Plural currency name |
| `currencySymbol` | string | `"$"` | Currency symbol |
| `startingBalance` | double | `0.0` | Starting balance for new factions |

```json
"economy": {
  "enabled": true,
  "currencyName": "dollar",
  "currencyNamePlural": "dollars",
  "currencySymbol": "$",
  "startingBalance": 0.0
}
```

### messages

Message formatting settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `prefix` | string | `"[HyperFactions] "` | Message prefix (supports color codes) |
| `primaryColor` | string | `"#00FFFF"` | Primary accent color |

```json
"messages": {
  "prefix": "\u00A7b[HyperFactions]\u00A7r ",
  "primaryColor": "#00FFFF"
}
```

### gui

GUI settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `title` | string | `"HyperFactions"` | Title shown in navigation bar |

```json
"gui": {
  "title": "HyperFactions"
}
```

### territoryNotifications

Territory entry/exit notifications.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | `true` | Enable territory notifications |

```json
"territoryNotifications": {
  "enabled": true
}
```

### worldMap

World map marker settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | `true` | Enable world map claim markers |

```json
"worldMap": {
  "enabled": true
}
```

### debug

Debug logging settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabledByDefault` | bool | `false` | Enable all debug categories |
| `logToConsole` | bool | `true` | Output debug to console |

#### debug.categories

Per-category debug toggles.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `power` | bool | `false` | Power system debug |
| `claim` | bool | `false` | Claim system debug |
| `combat` | bool | `false` | Combat system debug |
| `protection` | bool | `false` | Protection system debug |
| `relation` | bool | `false` | Relation system debug |
| `territory` | bool | `false` | Territory system debug |

```json
"debug": {
  "enabledByDefault": false,
  "logToConsole": true,
  "categories": {
    "power": false,
    "claim": false,
    "combat": false,
    "protection": false,
    "relation": false,
    "territory": false
  }
}
```

### chat

Chat formatting settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | `true` | Enable faction chat formatting |
| `format` | string | `"{faction_tag}{prefix}{player}{suffix}: {message}"` | Chat format template |
| `tagDisplay` | string | `"tag"` | `"tag"`, `"name"`, or `"none"` |
| `tagFormat` | string | `"[{tag}] "` | Format for faction tag |
| `noFactionTag` | string | `""` | Tag for non-faction players |
| `priority` | string | `"LATE"` | Event priority (after LuckPerms) |

#### chat.relationColors

Colors for faction tags based on relation to viewer.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `own` | string | `"#00FF00"` | Same faction (green) |
| `ally` | string | `"#FF69B4"` | Allied faction (pink) |
| `neutral` | string | `"#AAAAAA"` | Neutral faction (gray) |
| `enemy` | string | `"#FF0000"` | Enemy faction (red) |

```json
"chat": {
  "enabled": true,
  "format": "{faction_tag}{prefix}{player}{suffix}: {message}",
  "tagDisplay": "tag",
  "tagFormat": "[{tag}] ",
  "noFactionTag": "",
  "priority": "LATE",
  "relationColors": {
    "own": "#00FF00",
    "ally": "#FF69B4",
    "neutral": "#AAAAAA",
    "enemy": "#FF0000"
  }
}
```

### permissions

Server-level permission settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `adminRequiresOp` | bool | `true` | Admin commands require OP |
| `fallbackBehavior` | string | `"allow"` | `"allow"` or `"deny"` when no perm provider |

```json
"permissions": {
  "adminRequiresOp": true,
  "fallbackBehavior": "allow"
}
```

### backup

GFS (Grandfather-Father-Son) backup system settings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | `true` | Enable automatic backups |
| `hourlyRetention` | int | `24` | Number of hourly backups to keep |
| `dailyRetention` | int | `7` | Number of daily backups to keep |
| `weeklyRetention` | int | `4` | Number of weekly backups to keep |
| `onShutdown` | bool | `true` | Create backup on server shutdown |

```json
"backup": {
  "enabled": true,
  "hourlyRetention": 24,
  "dailyRetention": 7,
  "weeklyRetention": 4,
  "onShutdown": true
}
```

**Backup Types:**
- **Hourly (Son)**: Created every hour, keeps last N hours
- **Daily (Father)**: Created at midnight, keeps last N days
- **Weekly (Grandfather)**: Created Sunday at midnight, keeps last N weeks
- **Manual**: Created via `/f admin backup create`, never auto-deleted

**Backup Contents:**
- `data/factions/` - All faction data files
- `data/players/` - All player power data
- `data/zones.json` - Zone definitions
- `config.json` - Configuration file

**Commands:** See [Permissions](permissions.md) for `/f admin backup` commands.

### factionPermissions

Faction territory permission settings. Controls what actions outsiders, allies, and members can perform in faction territory.

#### factionPermissions.defaults

Default permissions for newly created factions.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `outsiderBreak` | bool | `false` | Non-members can break blocks |
| `outsiderPlace` | bool | `false` | Non-members can place blocks |
| `outsiderInteract` | bool | `false` | Non-members can interact |
| `allyBreak` | bool | `false` | Allies can break blocks |
| `allyPlace` | bool | `false` | Allies can place blocks |
| `allyInteract` | bool | `true` | Allies can interact |
| `memberBreak` | bool | `true` | Members can break blocks |
| `memberPlace` | bool | `true` | Members can place blocks |
| `memberInteract` | bool | `true` | Members can interact |
| `pvpEnabled` | bool | `true` | PvP allowed in territory |
| `officersCanEdit` | bool | `false` | Officers can edit permissions |

#### factionPermissions.locks

When `true`, factions cannot change this setting (server enforced).

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `outsiderBreak` | bool | `false` | Lock outsider break setting |
| `outsiderPlace` | bool | `false` | Lock outsider place setting |
| ... | ... | ... | Same keys as defaults |

#### factionPermissions.forced

When a setting is locked, use this value instead of the faction's setting.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `outsiderBreak` | bool | `false` | Forced outsider break value |
| `outsiderPlace` | bool | `false` | Forced outsider place value |
| ... | ... | ... | Same keys as defaults |

```json
"factionPermissions": {
  "defaults": {
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
  },
  "locks": {
    "outsiderBreak": false,
    "outsiderPlace": false,
    "outsiderInteract": false,
    "allyBreak": false,
    "allyPlace": false,
    "allyInteract": false,
    "memberBreak": false,
    "memberPlace": false,
    "memberInteract": false,
    "pvpEnabled": false,
    "officersCanEdit": false
  },
  "forced": {
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

**Example: Force PvP off in all faction territory**:
```json
"factionPermissions": {
  "locks": { "pvpEnabled": true },
  "forced": { "pvpEnabled": false }
}
```

---

## Zone Flags

Zones (SafeZones and WarZones) use flags to control behavior. These are hardcoded defaults that cannot be changed via config, but individual zones can override them via `/f admin zone setflag`.

### Available Flags

| Flag | Description |
|------|-------------|
| `pvp_enabled` | Whether PvP is allowed |
| `friendly_fire` | Whether same-faction damage is allowed |
| `build_allowed` | Whether players can place/break blocks |
| `container_access` | Whether players can access containers (chests) |
| `interact_allowed` | Whether players can interact (doors, buttons) |
| `item_drop` | Whether players can drop items |
| `item_pickup` | Whether players can pick up items |
| `mob_spawning` | Whether mobs can spawn |
| `mob_damage` | Whether mobs can damage players |
| `hunger_loss` | Whether players lose hunger |
| `fall_damage` | Whether players take fall damage |

### SafeZone Defaults

SafeZones are protected areas where PvP and building are disabled.

| Flag | Default | Description |
|------|---------|-------------|
| `pvp_enabled` | `false` | No PvP |
| `friendly_fire` | `false` | No friendly fire |
| `build_allowed` | `false` | No building |
| `container_access` | `false` | No container access |
| `interact_allowed` | `true` | Can use doors/buttons |
| `item_drop` | `true` | Can drop items |
| `item_pickup` | `true` | Can pick up items |
| `mob_spawning` | `false` | No mob spawning |
| `mob_damage` | `false` | No mob damage |
| `hunger_loss` | `false` | No hunger loss |
| `fall_damage` | `false` | No fall damage |

### WarZone Defaults

WarZones are PvP-enabled areas where building is blocked to prevent griefing.

| Flag | Default | Description |
|------|---------|-------------|
| `pvp_enabled` | `true` | PvP enabled (main purpose) |
| `friendly_fire` | `false` | No friendly fire |
| `build_allowed` | `false` | No building (prevents griefing) |
| `container_access` | `false` | No container access |
| `interact_allowed` | `true` | Can use doors/buttons |
| `item_drop` | `true` | Can drop items |
| `item_pickup` | `true` | Can pick up items |
| `mob_spawning` | `false` | No mob spawning |
| `mob_damage` | `true` | Mobs can damage |
| `hunger_loss` | `true` | Hunger loss enabled |
| `fall_damage` | `true` | Fall damage enabled |

### Customizing Zone Flags

To override a flag for a specific zone:
```
/f admin zone setflag <zone_name> <flag> <true|false>
```

**Example**: Allow building in a specific WarZone:
```
/f admin zone setflag pvp_arena build_allowed true
```

---

## Complete Default Config

See the auto-generated `config.json` in your data directory for a complete example with all current defaults.
