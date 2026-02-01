# HyperFactions Command Reference

Complete command reference for HyperFactions v0.3.0.

## Quick Reference

| Category | Commands |
|----------|----------|
| Core | create, disband, invite, accept, request, leave, kick |
| Management | rename, desc, color, open, close, promote, demote, transfer |
| Territory | claim, unclaim, overclaim, map |
| Relations | ally, enemy, neutral |
| Teleport | home, sethome, stuck |
| Information | info, list, members, invites, who, power, gui |
| Other | chat, admin, debug, reload |

---

## Core Commands

### `/f create <name>`
Create a new faction.
- **Permission**: `hyperfactions.create` (or `hyperfactions.use`)
- **Example**: `/f create Warriors`

### `/f disband`
Disband your faction. Requires confirmation.
- **Permission**: `hyperfactions.disband`
- **Requirement**: Must be faction leader

### `/f invite <player>`
Invite a player to join your faction.
- **Permission**: `hyperfactions.invite`
- **Requirement**: Must be officer or leader

### `/f accept [faction]`
Accept a pending faction invite.
- **Permission**: `hyperfactions.accept`
- **Example**: `/f accept` (accepts if only one invite)
- **Example**: `/f accept Warriors` (accepts specific invite)

### `/f request <faction> [message]`
Request to join a faction (for closed factions).
- **Permission**: `hyperfactions.request`
- **Example**: `/f request Warriors Looking to join!`

### `/f leave`
Leave your current faction.
- **Permission**: `hyperfactions.leave`
- **Note**: Leaders cannot leave; use `/f transfer` or `/f disband`

### `/f kick <player>`
Kick a member from your faction.
- **Permission**: `hyperfactions.kick`
- **Requirement**: Must be officer (can kick members) or leader (can kick anyone)

---

## Management Commands

### `/f rename <name>`
Rename your faction.
- **Permission**: `hyperfactions.rename`
- **Requirement**: Must be faction leader

### `/f desc <text>`
Set your faction's description.
- **Permission**: `hyperfactions.desc`
- **Example**: `/f desc We are the best faction on the server!`

### `/f color <code>`
Set your faction's display color.
- **Permission**: `hyperfactions.color`
- **Example**: `/f color c` (red), `/f color a` (green)

### `/f open`
Open your faction to allow anyone to join without invite.
- **Permission**: `hyperfactions.open`
- **Requirement**: Must be faction leader

### `/f close`
Close your faction to require invites to join.
- **Permission**: `hyperfactions.close`
- **Requirement**: Must be faction leader

### `/f promote <player>`
Promote a member to officer rank.
- **Permission**: `hyperfactions.promote`
- **Requirement**: Must be faction leader

### `/f demote <player>`
Demote an officer to member rank.
- **Permission**: `hyperfactions.demote`
- **Requirement**: Must be faction leader

### `/f transfer <player>`
Transfer faction leadership to another member.
- **Permission**: `hyperfactions.transfer`
- **Requirement**: Must be faction leader

---

## Territory Commands

### `/f claim`
Claim the current chunk for your faction.
- **Permission**: `hyperfactions.claim`
- **Requirement**: Faction must have enough power
- **Cost**: 1 power per claim

### `/f unclaim`
Unclaim the current chunk from your faction.
- **Permission**: `hyperfactions.unclaim`
- **Requirement**: Must be in your faction's territory

### `/f overclaim`
Overclaim enemy territory when they lack power.
- **Permission**: `hyperfactions.overclaim`
- **Requirement**: Target faction must have less power than claims

### `/f map`
View a visual map of nearby territory claims.
- **Permission**: `hyperfactions.map`
- **Output**: ASCII map showing claims in chat

---

## Relations Commands

### `/f ally <faction>`
Send or accept an alliance request.
- **Permission**: `hyperfactions.ally`
- **Requirement**: Must be officer or leader

### `/f enemy <faction>`
Declare another faction as an enemy.
- **Permission**: `hyperfactions.enemy`
- **Requirement**: Must be officer or leader

### `/f neutral <faction>`
Set neutral relations with another faction.
- **Permission**: `hyperfactions.neutral`
- **Requirement**: Must be officer or leader

---

## Teleport Commands

### `/f home`
Teleport to your faction's home location.
- **Permission**: `hyperfactions.home`
- **Note**: Subject to warmup/cooldown settings

### `/f sethome`
Set your faction's home at your current location.
- **Permission**: `hyperfactions.sethome`
- **Requirement**: Must be in your faction's territory

### `/f stuck`
Teleport to safety when trapped in enemy territory.
- **Permission**: `hyperfactions.stuck`
- **Note**: Longer warmup than normal teleports

---

## Information Commands

### `/f info [faction]`
View detailed information about a faction.
- **Permission**: `hyperfactions.info`
- **Example**: `/f info` (your faction)
- **Example**: `/f info Warriors` (another faction)

### `/f list`
List all factions on the server.
- **Permission**: `hyperfactions.list`
- **Output**: Paginated list sorted by power

### `/f members`
View your faction's member list.
- **Permission**: `hyperfactions.members`

### `/f invites`
View and manage pending invites and join requests.
- **Permission**: `hyperfactions.invites`

### `/f who <player>`
View information about a player.
- **Permission**: `hyperfactions.who`

### `/f power [player]`
View power level for yourself or another player.
- **Permission**: `hyperfactions.power`

### `/f gui`
Open the faction GUI menu.
- **Permission**: `hyperfactions.gui`

---

## Chat Commands

### `/f chat [mode]`
Toggle or set faction chat mode.
- **Permission**: `hyperfactions.chat`
- **Modes**: `faction` (f), `ally` (a), `public` (p)
- **Example**: `/f c f` (faction chat)
- **Example**: `/f c` (toggle)

---

## Admin Commands

All admin commands require `hyperfactions.admin` or `hyperfactions.admin.*` permission.

### `/f admin`
Open the admin GUI.

### Zone Management

#### `/f admin zone`
Open zone management GUI.

#### `/f admin zone list`
List all zones.

#### `/f admin zone create <type> <name>`
Create a new zone.
- **Types**: `safezone`, `warzone`
- **Example**: `/f admin zone create safezone Spawn`

#### `/f admin zone delete <name>`
Delete a zone by name.

#### `/f admin zone claim <name>`
Claim current chunk for an existing zone.

#### `/f admin zone unclaim`
Unclaim current chunk from its zone.

#### `/f admin zone radius <name> <radius> [shape]`
Claim multiple chunks in a radius.
- **Shapes**: `square` (default), `circle`
- **Example**: `/f admin zone radius Spawn 5 circle`

#### `/f admin safezone [name]`
Quick create SafeZone and claim current chunk.
- **Example**: `/f admin safezone` (auto-names)
- **Example**: `/f admin safezone SpawnArea`

#### `/f admin warzone [name]`
Quick create WarZone and claim current chunk.

#### `/f admin removezone`
Unclaim current chunk from its zone (alias for zone unclaim).

### Zone Flags

#### `/f admin zoneflag`
View all flags for the zone at your location.

#### `/f admin zoneflag <flag>`
View specific flag value.

#### `/f admin zoneflag <flag> <true|false>`
Set a flag value.
- **Example**: `/f admin zoneflag pvp_enabled true`

#### `/f admin zoneflag <flag> clear`
Reset a flag to its zone type default.

#### `/f admin zoneflag clearall`
Reset ALL flags to zone type defaults.

**Available Flags:**

| Flag | Description | SafeZone Default | WarZone Default |
|------|-------------|------------------|-----------------|
| `pvp_enabled` | Allow PvP combat | false | true |
| `friendly_fire` | Allow same-faction damage | false | false |
| `build_allowed` | Allow block place/break | false | false |
| `container_access` | Allow chest access | false | false |
| `interact_allowed` | Allow doors, buttons | true | true |
| `item_drop` | Allow dropping items | true | true |
| `item_pickup` | Allow picking up items | true | true |
| `mob_spawning` | Allow mob spawning | false | false |
| `mob_damage` | Allow mob damage | false | true |
| `hunger_loss` | Allow hunger drain | false | true |
| `fall_damage` | Allow fall damage | false | true |

### Other Admin Commands

#### `/f admin bypass`
Toggle admin bypass mode (ignore protection).
- **Permission**: `hyperfactions.admin.bypass`

#### `/f admin update`
Check for and install plugin updates.
- **Permission**: `hyperfactions.admin.update`

### Backup Commands

#### `/f admin backup`
Show backup help.
- **Permission**: `hyperfactions.admin.backup`

#### `/f admin backup create [name]`
Create a manual backup.
- **Example**: `/f admin backup create before-reset`

#### `/f admin backup list`
List all backups grouped by type (hourly, daily, weekly, manual).

#### `/f admin backup restore <name>`
Restore from a backup. Requires confirmation.
- **Warning**: This will overwrite current data!

#### `/f admin backup delete <name>`
Delete a backup.

---

## Debug Commands

All debug commands require `hyperfactions.admin` permission.

### `/f debug toggle <category|all>`
Toggle debug logging for a category.
- **Categories**: `protection`, `power`, `combat`, `claims`, `all`

### `/f debug status`
Show current debug logging status.

### `/f debug power <player>`
Show detailed power information for a player.

### `/f debug claim [x z]`
Show claim information at location.

### `/f debug protection <player>`
Show protection check results at location.

### `/f debug combat <player>`
Show combat tag status for a player.

### `/f debug relation <faction1> <faction2>`
Show relation details between two factions.

---

## Config Command

### `/f reload`
Reload plugin configuration.
- **Permission**: `hyperfactions.admin.reload`

---

## Permission Summary

### Universal Permission
- `hyperfactions.use` - Grants all basic user permissions (not admin/bypass/limit)

### User Permissions
All prefixed with `hyperfactions.`:
- `create`, `disband`, `invite`, `accept`, `request`, `leave`, `kick`
- `rename`, `desc`, `color`, `open`, `close`, `promote`, `demote`, `transfer`
- `claim`, `unclaim`, `overclaim`, `map`
- `ally`, `enemy`, `neutral`
- `home`, `sethome`, `stuck`
- `info`, `list`, `members`, `invites`, `who`, `power`, `gui`
- `chat`

### Admin Permissions
- `hyperfactions.admin` - Access admin commands/GUI
- `hyperfactions.admin.*` - All admin permissions
- `hyperfactions.admin.bypass` - Admin bypass mode
- `hyperfactions.admin.reload` - Reload config
- `hyperfactions.admin.update` - Plugin updates
- `hyperfactions.admin.backup` - Backup management

### Bypass Permissions
- `hyperfactions.bypass.*` - Bypass all protections
- `hyperfactions.bypass.build` - Bypass build protection
- `hyperfactions.bypass.container` - Bypass container protection
- `hyperfactions.bypass.damage` - Bypass PvP protection

### Limit Permissions
- `hyperfactions.limit.<N>` - Set max claims to N
- `hyperfactions.limit.unlimited` - Unlimited claims
