# HyperFactions Permissions

This document lists all permission nodes used by HyperFactions.

Permissions follow the Hytale best practices format: `<namespace>.<category>.<action>`

## Permission Hierarchy

```
hyperfactions.*                      All permissions (wildcard)
├── hyperfactions.use                Basic faction access
├── hyperfactions.faction.*          Faction management
│   ├── hyperfactions.faction.create
│   ├── hyperfactions.faction.disband
│   ├── hyperfactions.faction.rename
│   ├── hyperfactions.faction.description
│   ├── hyperfactions.faction.tag
│   ├── hyperfactions.faction.color
│   ├── hyperfactions.faction.open
│   └── hyperfactions.faction.close
├── hyperfactions.member.*           Membership management
│   ├── hyperfactions.member.invite
│   ├── hyperfactions.member.join
│   ├── hyperfactions.member.leave
│   ├── hyperfactions.member.kick
│   ├── hyperfactions.member.promote
│   ├── hyperfactions.member.demote
│   └── hyperfactions.member.transfer
├── hyperfactions.territory.*        Territory claims
│   ├── hyperfactions.territory.claim
│   ├── hyperfactions.territory.unclaim
│   ├── hyperfactions.territory.overclaim
│   └── hyperfactions.territory.map
├── hyperfactions.teleport.*         Teleportation
│   ├── hyperfactions.teleport.home
│   ├── hyperfactions.teleport.sethome
│   └── hyperfactions.teleport.stuck
├── hyperfactions.relation.*         Diplomatic relations
│   ├── hyperfactions.relation.ally
│   ├── hyperfactions.relation.enemy
│   ├── hyperfactions.relation.neutral
│   └── hyperfactions.relation.view
├── hyperfactions.chat.*             Communication
│   ├── hyperfactions.chat.faction
│   └── hyperfactions.chat.ally
├── hyperfactions.info.*             Information viewing
│   ├── hyperfactions.info.faction
│   ├── hyperfactions.info.list
│   ├── hyperfactions.info.player
│   ├── hyperfactions.info.power
│   ├── hyperfactions.info.members
│   ├── hyperfactions.info.logs
│   └── hyperfactions.info.help
├── hyperfactions.bypass.*           Protection bypass
│   ├── hyperfactions.bypass.build
│   ├── hyperfactions.bypass.interact
│   ├── hyperfactions.bypass.container
│   ├── hyperfactions.bypass.damage
│   ├── hyperfactions.bypass.use
│   ├── hyperfactions.bypass.warmup
│   └── hyperfactions.bypass.cooldown
├── hyperfactions.admin.*            Administration
│   ├── hyperfactions.admin.use
│   ├── hyperfactions.admin.reload
│   ├── hyperfactions.admin.debug
│   ├── hyperfactions.admin.zones
│   ├── hyperfactions.admin.disband
│   ├── hyperfactions.admin.modify
│   ├── hyperfactions.admin.bypass.limits
│   └── hyperfactions.admin.backup
└── hyperfactions.limit.*            Numeric limits
    ├── hyperfactions.limit.claims.<N>
    └── hyperfactions.limit.power.<N>
```

---

## Basic Access

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.use` | **Required** - Base permission to use `/f` command and GUI | true |

### Permission Setup

Players need `hyperfactions.use` as the base permission to access the `/f` command, plus category permissions for specific functionality.

**Recommended setup (HyperPerms):**
```
/hp group setperm default hyperfactions.use
/hp group setperm default hyperfactions.faction.*
/hp group setperm default hyperfactions.member.*
/hp group setperm default hyperfactions.territory.*
/hp group setperm default hyperfactions.teleport.*
/hp group setperm default hyperfactions.relation.*
/hp group setperm default hyperfactions.chat.*
/hp group setperm default hyperfactions.info.*
```

**Permissions requiring explicit grant:**
- `hyperfactions.admin.*` - Admin permissions
- `hyperfactions.bypass.*` - Bypass permissions
- `hyperfactions.limit.*` - Limit permissions

---

## Faction Management (`hyperfactions.faction.*`)

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.faction.create` | Create a new faction | true |
| `hyperfactions.faction.disband` | Disband your faction (leader only) | true |
| `hyperfactions.faction.rename` | Rename your faction | true |
| `hyperfactions.faction.description` | Set faction description | true |
| `hyperfactions.faction.tag` | Set faction tag | true |
| `hyperfactions.faction.color` | Set faction color | true |
| `hyperfactions.faction.open` | Make faction open (anyone can join) | true |
| `hyperfactions.faction.close` | Make faction closed (invite only) | true |

---

## Membership (`hyperfactions.member.*`)

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.member.invite` | Invite players to your faction | true |
| `hyperfactions.member.join` | Accept faction invites / request to join | true |
| `hyperfactions.member.leave` | Leave your faction | true |
| `hyperfactions.member.kick` | Kick members from your faction | true |
| `hyperfactions.member.promote` | Promote faction members | true |
| `hyperfactions.member.demote` | Demote faction members | true |
| `hyperfactions.member.transfer` | Transfer faction leadership | true |

---

## Territory (`hyperfactions.territory.*`)

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.territory.claim` | Claim territory chunks | true |
| `hyperfactions.territory.unclaim` | Unclaim territory chunks | true |
| `hyperfactions.territory.overclaim` | Overclaim enemy territory | true |
| `hyperfactions.territory.map` | View faction territory map | true |

---

## Teleportation (`hyperfactions.teleport.*`)

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.teleport.home` | Teleport to faction home | true |
| `hyperfactions.teleport.sethome` | Set faction home location | true |
| `hyperfactions.teleport.stuck` | Use the /f stuck command | true |

---

## Diplomacy (`hyperfactions.relation.*`)

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.relation.ally` | Request/accept ally relations | true |
| `hyperfactions.relation.enemy` | Declare enemy relations | true |
| `hyperfactions.relation.neutral` | Set neutral relations | true |
| `hyperfactions.relation.view` | View faction relations | true |

---

## Communication (`hyperfactions.chat.*`)

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.chat.faction` | Send faction chat messages | true |
| `hyperfactions.chat.ally` | Send ally chat messages | true |

---

## Information (`hyperfactions.info.*`)

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.info.faction` | View faction info | true |
| `hyperfactions.info.list` | View faction list | true |
| `hyperfactions.info.player` | View player info | true |
| `hyperfactions.info.power` | View power info | true |
| `hyperfactions.info.members` | View faction members | true |
| `hyperfactions.info.logs` | View faction activity logs | true |
| `hyperfactions.info.help` | View help | true |

---

## Bypass Permissions (`hyperfactions.bypass.*`)

These permissions allow players to bypass faction protections.

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.bypass.*` | Bypass all protections | false |
| `hyperfactions.bypass.build` | Bypass block placement/breaking protection | false |
| `hyperfactions.bypass.interact` | Bypass interaction protection (doors, buttons) | false |
| `hyperfactions.bypass.container` | Bypass container protection (chests) | false |
| `hyperfactions.bypass.damage` | Bypass entity damage protection | false |
| `hyperfactions.bypass.use` | Bypass item use protection | false |
| `hyperfactions.bypass.warmup` | Bypass home warmup delay | false |
| `hyperfactions.bypass.cooldown` | Bypass home cooldown timer | false |

---

## Admin Permissions (`hyperfactions.admin.*`)

These permissions are for server administrators.

| Permission | Description | Default |
|------------|-------------|---------|
| `hyperfactions.admin.*` | All admin permissions | op |
| `hyperfactions.admin.use` | Base admin access (opens admin GUI) | op |
| `hyperfactions.admin.reload` | Reload configuration | op |
| `hyperfactions.admin.debug` | Debug commands | op |
| `hyperfactions.admin.zones` | Manage safezones and warzones | op |
| `hyperfactions.admin.disband` | Force disband any faction | op |
| `hyperfactions.admin.modify` | Modify any faction | op |
| `hyperfactions.admin.bypass.limits` | Bypass claim limits | op |
| `hyperfactions.admin.backup` | Manage backups (create, restore, delete) | op |

---

## Limit Permissions (`hyperfactions.limit.*`)

These permissions control per-player limits using numeric suffixes.

| Permission Pattern | Description | Example |
|-------------------|-------------|---------|
| `hyperfactions.limit.claims.<N>` | Maximum claims for player | `hyperfactions.limit.claims.50` |
| `hyperfactions.limit.power.<N>` | Maximum power for player | `hyperfactions.limit.power.100` |

### Example Usage

```yaml
# Give VIP players 50 max claims
groups:
  vip:
    permissions:
      - hyperfactions.limit.claims.50

# Give premium players 100 max power
groups:
  premium:
    permissions:
      - hyperfactions.limit.power.100
```

---

## Wildcard Permissions

HyperFactions supports wildcard permissions at each category level:

| Wildcard | Description |
|----------|-------------|
| `hyperfactions.*` | All HyperFactions permissions |
| `hyperfactions.faction.*` | All faction management permissions |
| `hyperfactions.member.*` | All membership permissions |
| `hyperfactions.territory.*` | All territory permissions |
| `hyperfactions.teleport.*` | All teleportation permissions |
| `hyperfactions.relation.*` | All relation permissions |
| `hyperfactions.chat.*` | All chat permissions |
| `hyperfactions.info.*` | All information permissions |
| `hyperfactions.bypass.*` | All bypass permissions |
| `hyperfactions.admin.*` | All admin permissions |

---

## Fallback Behavior

When no permission plugin is installed (or the plugin doesn't have a definitive answer):

| Permission Type | Fallback Behavior |
|-----------------|-------------------|
| **User permissions** | Allowed by default (configurable) |
| **Admin permissions** (`hyperfactions.admin.*`) | Requires OP (configurable) |
| **Bypass permissions** (`hyperfactions.bypass.*`) | **Always denied** (requires explicit grant) |
| **Limit permissions** (`hyperfactions.limit.*`) | **Always denied** (uses config defaults) |

This ensures that powerful permissions like bypass are never accidentally granted.

Configuration in `config.json`:

```json
{
  "permissions": {
    "adminRequiresOp": true,
    "fallbackBehavior": "allow"
  }
}
```

**Note:** The `fallbackBehavior` setting only affects normal user permissions. Admin, bypass, and limit permissions always have secure defaults regardless of this setting.

---

## HyperPerms Integration

When HyperPerms is installed, HyperFactions permissions are automatically discovered through runtime permission discovery.

### Recommended Group Setup

```yaml
# Default group - basic faction access
groups:
  default:
    permissions:
      - hyperfactions.use
      - hyperfactions.faction.create
      - hyperfactions.member.join
      - hyperfactions.member.leave
      - hyperfactions.info.*

# Member group - full faction participation
groups:
  member:
    parents:
      - default
    permissions:
      - hyperfactions.faction.*
      - hyperfactions.member.*
      - hyperfactions.territory.*
      - hyperfactions.teleport.*
      - hyperfactions.relation.*
      - hyperfactions.chat.*

# VIP group - enhanced limits
groups:
  vip:
    parents:
      - member
    permissions:
      - hyperfactions.limit.claims.50
      - hyperfactions.limit.power.100
      - hyperfactions.bypass.warmup
      - hyperfactions.bypass.cooldown

# Admin group
groups:
  admin:
    permissions:
      - hyperfactions.admin.*
      - hyperfactions.bypass.*
```

---

## Command Permission Reference

| Command | Permission Required |
|---------|---------------------|
| `/f create <name>` | `hyperfactions.faction.create` |
| `/f disband` | `hyperfactions.faction.disband` |
| `/f rename <name>` | `hyperfactions.faction.rename` |
| `/f desc <description>` | `hyperfactions.faction.description` |
| `/f tag <tag>` | `hyperfactions.faction.tag` |
| `/f color <code>` | `hyperfactions.faction.color` |
| `/f open` | `hyperfactions.faction.open` |
| `/f close` | `hyperfactions.faction.close` |
| `/f invite <player>` | `hyperfactions.member.invite` |
| `/f accept <faction>` | `hyperfactions.member.join` |
| `/f request <faction>` | `hyperfactions.member.join` |
| `/f leave` | `hyperfactions.member.leave` |
| `/f kick <player>` | `hyperfactions.member.kick` |
| `/f promote <player>` | `hyperfactions.member.promote` |
| `/f demote <player>` | `hyperfactions.member.demote` |
| `/f transfer <player>` | `hyperfactions.member.transfer` |
| `/f claim` | `hyperfactions.territory.claim` |
| `/f unclaim` | `hyperfactions.territory.unclaim` |
| `/f overclaim` | `hyperfactions.territory.overclaim` |
| `/f map` | `hyperfactions.territory.map` |
| `/f home` | `hyperfactions.teleport.home` |
| `/f sethome` | `hyperfactions.teleport.sethome` |
| `/f stuck` | `hyperfactions.teleport.stuck` |
| `/f ally <faction>` | `hyperfactions.relation.ally` |
| `/f enemy <faction>` | `hyperfactions.relation.enemy` |
| `/f neutral <faction>` | `hyperfactions.relation.neutral` |
| `/f relations` | `hyperfactions.relation.view` |
| `/f chat <message>` | `hyperfactions.chat.faction` |
| `/f info [faction]` | `hyperfactions.info.faction` |
| `/f list` | `hyperfactions.info.list` |
| `/f who [player]` | `hyperfactions.info.player` |
| `/f power [player]` | `hyperfactions.info.power` |
| `/f members` | `hyperfactions.info.members` |
| `/f logs` | `hyperfactions.info.logs` |
| `/f help` | `hyperfactions.info.help` |
| `/f gui` | `hyperfactions.use` |
| `/f admin` | `hyperfactions.admin.use` |
| `/f admin reload` | `hyperfactions.admin.reload` |
| `/f admin debug` | `hyperfactions.admin.debug` |
| `/f admin zone` | `hyperfactions.admin.zones` |
| `/f admin backup` | `hyperfactions.admin.backup` |
| `/f admin backup create [name]` | `hyperfactions.admin.backup` |
| `/f admin backup list` | `hyperfactions.admin.backup` |
| `/f admin backup restore <name>` | `hyperfactions.admin.backup` |
| `/f admin backup delete <name>` | `hyperfactions.admin.backup` |

---

## Security: Manager-Level Permission Checks

HyperFactions implements permission checks at the manager level to ensure security regardless of entry point (command, GUI, or API). This prevents GUI pages from bypassing permission checks.

### Managers with Permission Checks

| Manager | Method | Permission |
|---------|--------|------------|
| `ClaimManager` | `claim()` | `hyperfactions.territory.claim` |
| `ClaimManager` | `unclaim()` | `hyperfactions.territory.unclaim` |
| `ClaimManager` | `overclaim()` | `hyperfactions.territory.overclaim` |
| `FactionManager` | `createFaction()` | `hyperfactions.faction.create` |
| `FactionManager` | `disbandFaction()` | `hyperfactions.faction.disband` |
| `FactionManager` | `promoteMember()` | `hyperfactions.member.promote` |
| `FactionManager` | `demoteMember()` | `hyperfactions.member.demote` |
| `FactionManager` | `transferLeadership()` | `hyperfactions.member.transfer` |
| `FactionManager` | `setHome()` | `hyperfactions.teleport.sethome` |
| `RelationManager` | `requestAlly()` | `hyperfactions.relation.ally` |
| `RelationManager` | `setEnemy()` | `hyperfactions.relation.enemy` |
| `RelationManager` | `setNeutral()` | `hyperfactions.relation.neutral` |
| `TeleportManager` | `teleportToHome()` | `hyperfactions.teleport.home` |
| `InviteManager` | `createInviteChecked()` | `hyperfactions.member.invite` |
| `JoinRequestManager` | `createRequestChecked()` | `hyperfactions.member.join` |
| `ChatManager` | `toggleFactionChatChecked()` | `hyperfactions.chat.faction` |
| `ChatManager` | `toggleAllyChatChecked()` | `hyperfactions.chat.ally` |

### Result Enums

Each manager returns a result enum that includes `NO_PERMISSION` for permission failures:

- `ClaimManager.ClaimResult.NO_PERMISSION`
- `FactionManager.FactionResult.NO_PERMISSION`
- `RelationManager.RelationResult.NO_PERMISSION`
- `TeleportManager.TeleportResult.NO_PERMISSION`
- `InviteManager.InviteResult.NO_PERMISSION`
- `JoinRequestManager.RequestResult.NO_PERMISSION`
- `ChatManager.ChatResult.NO_PERMISSION`
