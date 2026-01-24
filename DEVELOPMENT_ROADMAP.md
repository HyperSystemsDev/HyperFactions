# HyperFactions Development Roadmap

> Last Updated: January 24, 2026 (Updated after Phase 1 completion)
> Version: 0.1.0-SNAPSHOT
> Repository: https://github.com/HyperSystemsDev/HyperFactions

---

## Table of Contents

- [Project Status](#project-status)
- [Critical Bugs](#critical-bugs)
- [Phase 1: Quick Wins](#phase-1-quick-wins)
- [Phase 2: Core Enhancements](#phase-2-core-enhancements)
- [Phase 3: Major Features](#phase-3-major-features)
- [Phase 4: Future Vision](#phase-4-future-vision)
- [Technical Debt](#technical-debt)
- [Architecture Notes](#architecture-notes)
- [Testing Checklist](#testing-checklist)

---

## Project Status

### Current Features (Implemented)
- [x] Faction CRUD (create, disband, rename)
- [x] Member management (invite, kick, promote, demote, transfer leadership)
- [x] Roles: Leader, Officer, Member
- [x] Chunk-based territory claiming
- [x] Power system (player power, faction power, death penalties, regeneration)
- [x] Relations: Ally, Enemy, Neutral (with ally requests)
- [x] Combat tagging
- [x] Safe Zones and War Zones
- [x] Protection checker for build/interact/PvP
- [x] GUI system (partially working)
- [x] Chat commands (/f create, /f claim, /f ally, etc.)

### Recently Completed (Phase 1)
- [x] `/f home` teleport with warmup/cooldown support
- [x] `/f who <player>` - Full player lookup with faction info
- [x] `/f power <player>` - Now accepts target player argument
- [x] Zone claim validation - Cannot create zones on claimed chunks
- [x] Ally request logging - Both factions now get proper actor attribution

### Not Working / Incomplete
- [ ] Explosion protection
- [ ] Piston protection
- [ ] Hopper protection

---

## Critical Bugs

> **These must be fixed before any release**

### Bug #1: Promotion Logic Error
- **Status:** :white_check_mark: **FIXED**
- **Severity:** P0 - GAME BREAKING
- **File:** `src/main/java/com/hyperfactions/manager/FactionManager.java`
- **Line:** 381-387

**Previous Code:**
```java
// Can't promote beyond officer (leader transfer is separate)
if (target.role() == FactionRole.OFFICER) {
    return FactionResult.CANNOT_PROMOTE_LEADER;
}
FactionRole newRole = target.role() == FactionRole.MEMBER ? FactionRole.OFFICER : FactionRole.OFFICER;
```

**Problem:** Guard checked for OFFICER (blocking promotion) and both ternary branches returned OFFICER.

**Fix Applied:**
```java
// Can't promote a leader
if (target.role() == FactionRole.LEADER) {
    return FactionResult.CANNOT_PROMOTE_LEADER;
}
FactionRole newRole = target.role() == FactionRole.MEMBER ? FactionRole.OFFICER : FactionRole.LEADER;
```

---

### Bug #2: Overclaim Power Check Reversed
- **Status:** :white_check_mark: **FIXED**
- **Severity:** P0 - GAME BREAKING
- **File:** `src/main/java/com/hyperfactions/manager/ClaimManager.java`
- **Line:** 301

**Previous Code:**
```java
if (defenderFaction.getClaimCount() <= defenderMaxClaims) {
    return ClaimResult.TARGET_HAS_POWER;
}
```

**Problem:** Used `<=` instead of `<`. Factions at exactly max claims were protected, but one over were raidable. This was backwards.

**Fix Applied:**
```java
if (defenderFaction.getClaimCount() < defenderMaxClaims) {
    return ClaimResult.TARGET_HAS_POWER;
}
```

---

## Phase 1: Quick Wins

> **STATUS: COMPLETE** :white_check_mark:
> All Phase 1 tasks have been implemented and tested.

| Task | File | Line | Effort | Status |
|------|------|------|--------|--------|
| Fix promotion bug | FactionManager.java | 381-387 | 5 min | :white_check_mark: |
| Fix overclaim check | ClaimManager.java | 301 | 5 min | :white_check_mark: |
| Fix ally acceptance logging | RelationManager.java | 216-232 | 30 min | :white_check_mark: |
| Add zone claim validation | ZoneManager.java | 207-227 | 20 min | :white_check_mark: |
| Complete /f home teleport | FactionCommand.java | 547-610 | 45 min | :white_check_mark: |
| Implement /f who | FactionCommand.java | 837-905 | 30 min | :white_check_mark: |
| Complete /f power target | FactionCommand.java | 907-951 | 20 min | :white_check_mark: |

### Details

#### Fix Ally Acceptance Logging :white_check_mark:
**File:** `src/main/java/com/hyperfactions/manager/RelationManager.java`

**Implementation:**
- Changed data structure from `Map<UUID, Set<UUID>>` to `Map<UUID, Map<UUID, UUID>>`
- Inner map stores: requesting faction ID → requester player UUID
- When alliance is accepted, both sides now get proper actor attribution
- Updated methods: `hasPendingRequest()`, `getPendingRequests()`, `requestAlly()`, `acceptAlly()`, `setEnemy()`, `clearAllRelations()`

#### Add Zone Claim Validation :white_check_mark:
**File:** `src/main/java/com/hyperfactions/manager/ZoneManager.java`

**Implementation:**
- Added ClaimManager dependency injection to constructor
- Added claim check in `createZone()` before zone creation
- `ZoneResult.CHUNK_CLAIMED` is now properly utilized
- Added user-friendly error messages in FactionCommand for zone creation failures

#### Complete /f home Teleport :white_check_mark:
**File:** `src/main/java/com/hyperfactions/command/FactionCommand.java:547-610`

**Implementation:**
- Full integration with TeleportManager for warmup/cooldown
- Combat tag checking before and during warmup
- Movement cancellation support
- Same-world teleport with TransformComponent.setPosition()
- Cross-world detection (returns WORLD_NOT_FOUND)

#### Implement /f who :white_check_mark:
**File:** `src/main/java/com/hyperfactions/command/FactionCommand.java:837-905`

**Implementation:**
- Shows: faction name, role, power, online status, join date, last seen
- Lookup order: online players first, then faction members
- Works for both online and offline players
- Uses TimeUtil.formatRelative() for timestamps

#### Complete /f power target :white_check_mark:
**File:** `src/main/java/com/hyperfactions/command/FactionCommand.java:907-951`

**Implementation:**
- Same lookup logic as /f who for consistency
- No args = show own power
- With args = look up target player

---

## Phase 2: Core Enhancements

> Estimated Time: 1-3 days each

### 2.1 Faction Settings Commands
- **Priority:** P1
- **Effort:** 1 day
- **Status:** :red_circle: Not Started

Add commands for settings that already exist in the data model:

| Command | Description | Data Model Method |
|---------|-------------|-------------------|
| `/f rename <name>` | Change faction name | `Faction.withName()` |
| `/f desc <text>` | Set description | `Faction.withDescription()` |
| `/f color <color>` | Set faction color | `Faction.withColor()` |
| `/f open` | Allow anyone to join | `Faction.withOpen(true)` |
| `/f close` | Require invite to join | `Faction.withOpen(false)` |

**Files to modify:**
- `FactionCommand.java` - Add new subcommands
- `FactionManager.java` - Add update methods if needed

---

### 2.2 Explosion Protection
- **Priority:** P1
- **Effort:** 1 day
- **Status:** :red_circle: Not Started

Protect claimed territory from:
- TNT explosions
- Creeper explosions
- Wither explosions
- End crystals
- Beds in nether/end

**Files to modify:**
- `ProtectionListener.java` - Add `onExplosion()` method
- `ProtectionChecker.java` - Add explosion check logic
- `HyperFactionsConfig.java` - Add config options

**Config Options:**
```java
boolean protectFromTNT = true;
boolean protectFromCreepers = true;
boolean protectFromWithers = true;
boolean protectFromFireballs = true;
```

---

### 2.3 Piston Griefing Protection
- **Priority:** P1
- **Effort:** 1 day
- **Status:** :red_circle: Not Started

Prevent pistons from:
- Pushing blocks into/out of claimed territory
- Pulling blocks across faction boundaries

**Files to modify:**
- `ProtectionListener.java` - Add `onPistonExtend()`, `onPistonRetract()`

---

### 2.4 Hopper Extraction Protection
- **Priority:** P2
- **Effort:** 0.5 day
- **Status:** :red_circle: Not Started

Prevent hoppers from stealing items from containers in other faction's territory.

**Files to modify:**
- `ProtectionListener.java` - Add `onHopperTransfer()`

---

### 2.5 `/f stuck` Command
- **Priority:** P2
- **Effort:** 1 day
- **Status:** :red_circle: Not Started

Teleport players out of enemy territory when trapped.

**Behavior:**
- Find nearest unclaimed/own chunk
- Long warmup (60+ seconds)
- Cancel on movement
- Only usable in enemy territory

**Files to modify:**
- `FactionCommand.java` - Add subcommand
- `TeleportManager.java` - Add stuck teleport logic

---

### 2.6 Ally/Enemy Caps
- **Priority:** P2
- **Effort:** 0.5 day
- **Status:** :red_circle: Not Started

Limit number of allies/enemies to prevent mega-alliances.

**Config Options:**
```java
int maxAllies = 10;       // -1 for unlimited
int maxEnemies = -1;      // -1 for unlimited
```

**Files to modify:**
- `HyperFactionsConfig.java` - Add limits
- `RelationManager.java` - Check limits in `requestAlly()`, `setEnemy()`

---

## Phase 3: Major Features

> Estimated Time: 1+ week each

### 3.1 Faction Treasury/Bank System
- **Priority:** P2
- **Effort:** 1 week
- **Status:** :red_circle: Not Started

Allow factions to have a shared money pool.

**Features:**
- `/f balance` - View faction balance
- `/f deposit <amount>` - Add money to faction
- `/f withdraw <amount>` - Take money (leader/officer only)
- Tax on claims (optional)
- Upkeep costs (optional)

**Files to create/modify:**
- New: `FactionEconomy.java` record
- Modify: `Faction.java` - Add economy field
- Modify: `FactionManager.java` - Add economy methods
- Modify: `FactionCommand.java` - Add economy commands

**Economy Integration:**
- Hook into standard economy API (Vault-style)
- Fallback to built-in balance tracking

---

### 3.2 Granular Permission System
- **Priority:** P2
- **Effort:** 1.5 weeks
- **Status:** :red_circle: Not Started

Per-role permission flags instead of hardcoded role checks.

**Permission Nodes:**
```java
enum FactionPermission {
    BUILD,          // Place/break blocks
    INTERACT,       // Use doors, buttons, etc.
    CONTAINER,      // Open chests, furnaces
    CLAIM,          // Claim territory
    UNCLAIM,        // Unclaim territory
    INVITE,         // Invite new members
    KICK,           // Kick members
    PROMOTE,        // Promote members
    DEMOTE,         // Demote members
    SETHOME,        // Set faction home
    HOME,           // Use faction home
    ALLY,           // Manage ally relations
    ENEMY,          // Manage enemy relations
    WITHDRAW,       // Withdraw from treasury
    DISBAND         // Disband faction
}
```

**Default Role Permissions:**
- **LEADER:** All permissions
- **OFFICER:** All except DISBAND, PROMOTE (to leader)
- **MEMBER:** BUILD, INTERACT, CONTAINER, HOME

**Commands:**
- `/f perms` - View permission matrix
- `/f perms <role> <permission> <true|false>` - Set permission

---

### 3.3 Raid System with Cooldowns
- **Priority:** P2
- **Effort:** 2 weeks
- **Status:** :red_circle: Not Started

Structured raiding with windows and cooldowns.

**Features:**
- Raid declaration period (30 min warning)
- Raid window (2-4 hours active)
- Post-raid cooldown (can't raid same faction for 24h)
- Raid notifications to all faction members
- Optional: Raid points/objectives

**Config Options:**
```java
boolean raidSystemEnabled = true;
int raidDeclarationMinutes = 30;
int raidWindowHours = 2;
int raidCooldownHours = 24;
boolean notifyOnRaidDeclare = true;
```

**Files to create:**
- New: `RaidManager.java`
- New: `Raid.java` record

---

### 3.4 Territory Decay for Inactive Factions
- **Priority:** P2
- **Effort:** 1 week
- **Status:** :red_circle: Not Started

Auto-remove claims from inactive factions.

**Behavior:**
- Track last activity per faction (any member)
- After X days inactive, start losing claims
- Lose oldest claims first (or furthest from home)
- Notify faction members on login

**Config Options:**
```java
boolean territoryDecayEnabled = true;
int decayDaysInactive = 30;
int decayClaimsPerDay = 1;
boolean notifyOnDecay = true;
```

---

### 3.5 Faction Levels/Progression
- **Priority:** P3
- **Effort:** 1 week
- **Status:** :red_circle: Not Started

Factions gain XP and levels for activities.

**XP Sources:**
- Claiming territory: +10 XP
- Member joins: +25 XP
- Kill enemy faction member: +50 XP
- Online time (per hour): +5 XP

**Level Benefits:**
| Level | Max Members | Max Claims | Power Regen |
|-------|-------------|------------|-------------|
| 1 | 10 | 20 | 1x |
| 2 | 15 | 35 | 1.1x |
| 3 | 20 | 50 | 1.2x |
| 4 | 30 | 75 | 1.3x |
| 5 | 50 | 100 | 1.5x |

---

### 3.6 War Declaration System
- **Priority:** P3
- **Effort:** 2 weeks
- **Status:** :red_circle: Not Started

Formal war declarations with objectives.

**Features:**
- `/f war declare <faction>` - Start war
- `/f war status` - View active wars
- `/f war surrender` - Give up
- War objectives (kills, territory)
- War score tracking
- Peace negotiations

**War States:**
1. Declaration (1h notice period)
2. Active War (unrestricted raiding)
3. Peace Negotiation
4. Ceasefire/Peace

---

## Phase 4: Future Vision

> Long-term features for later versions

### 4.1 Faction Shields/Offline Protection
- **Description:** Configurable protection when all members offline
- **Features:**
  - Shield activation after last member logs off (30 min delay)
  - Shield duration limit (max 16 hours)
  - Shield cooldown (can't re-shield for 4 hours after broken)
  - Visual indicator on territory

### 4.2 Dynmap/BlueMap Integration
- **Description:** Show faction territories on web map
- **Features:**
  - Color territories by faction color
  - Show faction names on hover
  - Configurable visibility (all/allies only/none)
  - Real-time updates

### 4.3 Faction Upgrades Shop
- **Description:** Purchase upgrades with faction bank
- **Upgrades:**
  - Power Regeneration Boost (+10/20/30%)
  - Max Claims Increase (+10/25/50)
  - Spawn Protection Radius (1/2/3 chunks)
  - Crop Growth Speed (10/25/50% faster)
  - Mob Spawn Rate in Territory
  - XP Boost in Territory

### 4.4 Custom Flags per Claim
- **Description:** Per-chunk or faction-wide toggles
- **Flags:**
  - Fire spread
  - Mob spawning
  - Enderpearl usage
  - Chorus fruit teleport
  - Frost walker ice
  - Leaf decay
  - Crop trampling

### 4.5 Faction Missions/Quests
- **Description:** Daily/weekly objectives with rewards
- **Mission Types:**
  - Claim X chunks
  - Kill X enemy players
  - Stay online for X hours (collective)
  - Win a raid
  - Defend against a raid
- **Rewards:** XP, Money, Power Boost

### 4.6 Alliance System
- **Description:** Multi-faction alliances with shared features
- **Features:**
  - Alliance chat channel
  - Shared territory access options
  - Alliance-wide wars
  - Alliance treasury
  - Alliance levels

### 4.7 Faction Events
- **Description:** Server-wide faction competitions
- **Event Types:**
  - King of the Hill (control point)
  - Territory Race (claim the most)
  - Faction Wars (bracket tournament)
  - Resource Collection
- **Scheduling:** Admin-triggered or automated

### 4.8 Discord Integration
- **Description:** Discord bot integration
- **Features:**
  - Faction chat to Discord channel
  - War/raid notifications
  - Member activity tracking
  - /link command for Discord-MC sync
  - Leaderboards in Discord

### 4.9 Advanced Analytics
- **Description:** Admin dashboard for server health
- **Metrics:**
  - Active factions over time
  - Claims per faction distribution
  - War frequency
  - Player retention by faction
  - Most contested territories

### 4.10 Faction Outposts
- **Description:** Secondary bases with limited features
- **Features:**
  - Separate from main territory (no adjacency)
  - Limited claim radius (3-5 chunks)
  - No home teleport (or separate cooldown)
  - Higher upkeep cost
  - Can be lost independently

---

## Technical Debt

### Performance Issues

| Issue | Location | Impact | Fix |
|-------|----------|--------|-----|
| O(n) getFactionClaims() | ClaimManager.java | High with many claims | Add reverse index Map<UUID, Set<ChunkKey>> |
| No auto-save | HyperFactions.java | Data loss on crash | Add periodic save task |
| 7 callback params | TeleportManager.java | Hard to use | Create TeleportContext object |
| No invite cleanup | InviteManager.java | Memory leak | Add scheduled cleanup task |

### Code Quality

| Issue | Location | Recommendation |
|-------|----------|----------------|
| Hardcoded movement threshold | TeleportManager.java:254 | Move to config |
| Lazy cleanup race condition | CombatTagManager.java:50 | Use atomic operations |
| Missing null checks | Various | Add @NotNull annotations and checks |

---

## Architecture Notes

### Data Flow
```
Command → FactionCommand.java
    ↓
Manager → (FactionManager, ClaimManager, PowerManager, etc.)
    ↓
Storage → JsonFactionStorage / JsonPlayerStorage
    ↓
File System → data/factions/*.json, data/players/*.json
```

### Key Design Patterns
- **Immutable Records:** All data classes use Java records with defensive copying
- **Manager Pattern:** Business logic separated from data storage
- **Async Storage:** All file I/O uses CompletableFuture
- **O(1) Lookups:** ChunkKey indexing for claim lookups

### Threading Model
- Main thread: Command execution, event handling
- Async: File I/O, periodic tasks
- ConcurrentHashMap: Thread-safe caching

---

## Testing Checklist

### Critical Bug Verification
- [x] Create faction with MEMBER
- [x] Promote MEMBER to OFFICER - verify success (**Code fix verified**)
- [x] Promote OFFICER to LEADER - verify success (**Code fix verified - previously failed**)
- [x] Test overclaim at exact power boundary (**Code fix verified**)
- [x] Test overclaim one claim over boundary (**Code fix verified**)

### Teleport Feature
- [x] Set faction home (existing functionality)
- [x] Move away from home (existing functionality)
- [x] Run `/f home` - verify warmup countdown (**Implemented**)
- [x] Wait for teleport - verify arrival (**Implemented**)
- [x] Test cancel on movement (**Implemented via TeleportManager**)
- [x] Test cancel on damage (**Implemented via TeleportManager**)
- [x] Test cancel when combat tagged (**Implemented**)

### Protection Events
- [ ] Place TNT near faction border - verify protection
- [ ] Detonate TNT in claimed territory - verify blocks protected
- [ ] Push piston across boundary - verify blocked
- [ ] Pull piston across boundary - verify blocked
- [ ] Place hopper under faction container - verify no extraction

### Relation System
- [x] Send ally request (existing functionality)
- [x] Accept ally request - verify both sides see correct logs (**Fixed - now stores requester UUID**)
- [ ] Break alliance - verify both sides updated
- [ ] Declare enemy - verify immediate effect
- [ ] Set neutral - verify relation cleared

---

## Changelog

### Version 0.1.1 (Current) - January 24, 2026
**Critical Bug Fixes:**
- Fixed promotion logic error - Officers can now be promoted to Leader
- Fixed overclaim power check - Correct boundary comparison (< instead of <=)

**Feature Completions:**
- `/f home` now fully functional with warmup/cooldown support
- `/f who <player>` implemented with full player info display
- `/f power <player>` now accepts target player argument

**Improvements:**
- Ally request logging now properly attributes both parties
- Zone creation blocked on faction-claimed chunks
- Better error messages for zone creation failures

### Version 0.1.0
- Initial implementation
- Basic faction CRUD
- Territory claiming with power system
- Ally/enemy relations
- Combat tagging
- Safe/War zones

### Version 0.2.0 (Planned)
- Add explosion/piston/hopper protection
- Add faction settings commands (`/f rename`, `/f desc`, `/f color`, `/f open`)
- Add `/f stuck` command

### Version 0.3.0 (Planned)
- Faction treasury system
- Raid cooldowns
- Territory decay

### Version 1.0.0 (Target)
- All Phase 1-3 features complete
- Full permission system
- War declarations
- Stable for production use

---

## Contributing

When adding features:
1. Update this roadmap with status
2. Follow existing code patterns (immutable records, managers)
3. Add config options for new features
4. Test thoroughly before marking complete
5. Update changelog

---

*This document is actively maintained. Check status indicators for current progress.*
