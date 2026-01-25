# HyperFactions Development Roadmap

> Last Updated: January 24, 2026 (Completed 2.9 Update Checker, 2.10 Spawnkilling Prevention, 3.0 WarZone Per-Zone Configuration)
> Version: 0.3.0
> Repository: https://github.com/HyperSystemsDev/HyperFactions

---

## ⚠️ Important Note: Hytale-Specific Implementation

**AI Assistant Notice:** This is a **Hytale server plugin**, not a Minecraft plugin. Do not reference Minecraft-specific mechanics that don't exist in Hytale:

**Does NOT exist in Hytale:**
- ❌ TNT / Explosions (use "explosive devices" if needed in future)
- ❌ Pistons (use "mechanical blocks" if needed)
- ❌ Hoppers (use "item transporters" if needed)
- ❌ Redstone (use "logic circuits" or "power systems" if needed)
- ❌ Enderpearls (use "teleportation items" if needed)
- ❌ Chorus fruit (use "teleportation consumables" if needed)

**Use generic Hytale terms:**
- ✅ "Blocks" (generic)
- ✅ "Items" (generic)
- ✅ "Entities" (generic)
- ✅ "Mobs" or "Creatures"
- ✅ "Players"
- ✅ "World" / "Chunks"

When suggesting features, use Hytale API capabilities or generic placeholder terms for future game mechanics.

---

## Table of Contents

- [Project Status](#project-status)
- [Hytale API Capabilities & Limitations](#hytale-api-capabilities--limitations)
- [Critical Bugs](#critical-bugs)
- [Phase 1: Quick Wins](#phase-1-quick-wins)
- [Phase 2: Core Enhancements](#phase-2-core-enhancements)
- [Phase 3: Major Features](#phase-3-major-features)
- [Phase 4: Future Vision](#phase-4-future-vision)
- [Technical Debt](#technical-debt)
- [Architecture Improvements: Modular Design](#architecture-improvements-modular-design)
- [Architecture Notes](#architecture-notes)
- [Configuration Philosophy](#configuration-philosophy)
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
- [x] GUI system (partially implemented - **Phase 2.11 will overhaul and complete**)
- [x] Chat commands (/f create, /f claim, /f ally, etc.)

### Recently Completed (Phase 1)
- [x] `/f home` teleport with warmup/cooldown support
- [x] `/f who <player>` - Full player lookup with faction info
- [x] `/f power <player>` - Now accepts target player argument
- [x] Zone claim validation - Cannot create zones on claimed chunks
- [x] Ally request logging - Both factions now get proper actor attribution

### Not Working / Incomplete
- [ ] Explosive device protection (if/when added to Hytale)
- [ ] Mechanical block protection (if/when added to Hytale)
- [ ] Item transporter protection (if/when added to Hytale)

---

## Hytale API Capabilities & Limitations

> Based on Hytale Server API analysis (as of January 2026)

### ✅ Available & Implemented

**Block Protection:**
- ✅ Block placement (`PlaceBlockEvent`)
- ✅ Block breaking (`BreakBlockEvent`)
- ✅ Block interaction (`UseBlockEvent`)
- ✅ Block damage tracking (`DamageBlockEvent`)

**Item Protection:**
- ✅ Item pickup (`InteractivelyPickupItemEvent`) - **NEEDS IMPLEMENTATION**
- ✅ Item drops (`DropItemEvent`)

**Entity & Combat:**
- ✅ Player damage events (PvP protection)
- ✅ Entity spawn/removal events

**Territory & World:**
- ✅ Chunk data access via ECS
- ✅ World/Universe management APIs
- ✅ Chunk loading/unloading

### ❌ NOT Available (Hytale Doesn't Have These)

**Game Mechanics:**
- ❌ Explosive devices (not yet implemented in Hytale)
- ❌ Mechanical block movement (pushing/pulling - not implemented)
- ❌ Automatic item transport (not implemented)
- ❌ Logic circuits / power systems (not implemented)

**Implications:**
- Phase 2 tasks 2.2, 2.3, 2.4 (explosive device/mechanical block/item transporter protection) marked as `:no_entry: NOT POSSIBLE`
- Raid system cannot use explosive mechanics
- Siege mechanics must use alternative approaches (manual block breaking only)

### 🔍 Needs Research

**Potentially Available (requires API investigation):**
- ❓ Entity spawn control for mob prevention in claims
- ❓ Crop growth rate modification
- ❓ Fire spread control
- ❓ Weather/time control per chunk
- ❓ Potion effect application in territory

**Action Item:** Test each feature when needed for implementation.

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

### Bug #3: SafeZone Item Pickup Not Protected
- **Status:** :x: **NOT FIXED**
- **Severity:** P0 - EXPLOITABLE
- **File:** `src/main/java/com/hyperfactions/listener/ProtectionListener.java`
- **Missing:** No `onItemPickup()` event handler

**Problem:**
Players can pick up consumable items placed as entities in SafeZones, despite all other protections working correctly.

**Root Cause:**
- `InteractivelyPickupItemEvent` is available in Hytale API
- Protection system defines `USE` interaction type but never uses it for item pickup
- No ECS event system registered for pickup events in `HyperFactionsPlugin`

**Fix Required:**
1. Add `onItemPickup()` method to `ProtectionListener`
2. Create `ItemPickupProtectionSystem` ECS handler in `HyperFactionsPlugin`
3. Register event system in `setup()` phase
4. Use `ProtectionChecker.canInteractChunk()` with `InteractionType.USE`

**Priority:** Should be fixed before v1.0.0 release

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

> **STATUS: COMPLETE** :white_check_mark:
> All implementable Phase 2 tasks have been completed.
> Note: Explosive device, mechanical block, and item transporter protection are NOT POSSIBLE in Hytale as these mechanics don't exist in the current API.

### 2.0 Wire Up Block Protection Events :white_check_mark:
- **Priority:** P1
- **Effort:** 0.5 day
- **Status:** :white_check_mark: **COMPLETE**

Connected the existing ProtectionListener to Hytale ECS events for block protection.

**Implementation:**
- Added `BlockPlaceProtectionSystem` - Handles PlaceBlockEvent via EntityEventSystem
- Added `BlockBreakProtectionSystem` - Handles BreakBlockEvent via EntityEventSystem
- Added `BlockUseProtectionSystem` - Handles UseBlockEvent.Pre via EntityEventSystem
- Uses `store.getExternalData().getWorld()` to get world context
- Uses `chunk.getComponent(entityIndex, PlayerRef.getComponentType())` for player info

**Files modified:**
- `HyperFactionsPlugin.java` - Added ECS event system registration + inner system classes

---

### 2.1 Faction Settings Commands :white_check_mark:
- **Priority:** P1
- **Effort:** 1 day
- **Status:** :white_check_mark: **COMPLETE**

Added commands for settings that already exist in the data model:

| Command | Description | Permission | Role Required |
|---------|-------------|------------|---------------|
| `/f rename <name>` | Change faction name | `hyperfactions.rename` | Leader |
| `/f desc <text>` | Set description | `hyperfactions.desc` | Officer+ |
| `/f color <code>` | Set faction color (0-9, a-f) | `hyperfactions.color` | Officer+ |
| `/f open` | Allow anyone to join | `hyperfactions.open` | Leader |
| `/f close` | Require invite to join | `hyperfactions.close` | Leader |

**Files modified:**
- `FactionCommand.java` - Added 5 new handler methods + switch cases + help text

---

### 2.2 Explosion Protection :no_entry:
- **Priority:** P1
- **Status:** :no_entry: **NOT POSSIBLE IN HYTALE**

Hytale does not have explosion mechanics in the current API. This feature is deferred until Hytale adds relevant events.

---

### 2.3 Mechanical Block Griefing Protection :no_entry:
- **Priority:** P1
- **Status:** :no_entry: **NOT POSSIBLE IN HYTALE**

Hytale does not have mechanical block pushing/pulling mechanics in the current API. This feature is deferred until Hytale adds relevant events.

---

### 2.4 Item Transporter Extraction Protection :no_entry:
- **Priority:** P2
- **Status:** :no_entry: **NOT POSSIBLE IN HYTALE**

Hytale does not have automatic item extraction/transport mechanics in the current API. This feature is deferred until Hytale adds relevant events.

---

### 2.5 `/f unstuck` Command :white_check_mark:
- **Priority:** P2
- **Effort:** 1 day
- **Status:** :white_check_mark: **COMPLETE**

Teleport players out of enemy territory when trapped.

**Behavior:**
- Only usable in enemy/neutral territory (not own/ally/wilderness)
- 30-second warmup (configurable via `stuck.warmupSeconds`)
- Finds nearest safe chunk via spiral search (wilderness, own claim, or ally claim)
- Cancel on movement or combat tag
- Combat check prevents use while tagged

**Config Options:**
```json
{
  "stuck": {
    "warmupSeconds": 30,
    "cooldownSeconds": 300
  }
}
```

**Files modified:**
- `FactionCommand.java` - Added `handleUnstuck()` method + `findNearestSafeChunk()` helper
- `HyperFactionsConfig.java` - Added stuck warmup/cooldown settings

---

### 2.6 Ally/Enemy Caps :white_check_mark:
- **Priority:** P2
- **Effort:** 0.5 day
- **Status:** :white_check_mark: **COMPLETE**

Limit number of allies/enemies to prevent mega-alliances.

**Config Options:**
```json
{
  "relations": {
    "maxAllies": 10,
    "maxEnemies": -1
  }
}
```
(-1 means unlimited)

**Implementation:**
- Added `ALLY_LIMIT_REACHED` and `ENEMY_LIMIT_REACHED` to `RelationResult` enum
- Added cap checks in `requestAlly()` and `setEnemy()` methods
- Added error handling in `FactionCommand` for new result types

**Files modified:**
- `HyperFactionsConfig.java` - Added `maxAllies`, `maxEnemies` fields + getters
- `RelationManager.java` - Added cap validation + new enum values
- `FactionCommand.java` - Added error messages for cap limits

---

### 2.7 API Capability Research
- **Priority:** P2
- **Effort:** 0.5 day
- **Status:** :red_circle: Not Started

Research Hytale API capabilities for advanced features.

**Items to Test:**
1. **Entity Spawn Control**
   - Can we prevent mob spawning in claimed territory?
   - Event: Look for `EntitySpawnEvent` or similar
   - Use case: SafeZones, faction claims

2. **Crop Growth Modification**
   - Can we modify crop growth rates in territory?
   - Potential for faction upgrades (faster farming)

3. **Fire Spread Control**
   - Can we prevent fire spread in claims?
   - Important for grief protection

4. **Weather/Time Control**
   - Can we set weather/time per chunk?
   - Use case: Faction upgrades, special zones

5. **Potion Effects in Territory**
   - Can we apply effects when entering faction land?
   - Use case: Faction buffs, raid debuffs

**Testing Method:**
1. Review `/home/dmehaffy/Documents/Hytale/HyperSystems/libs/Server/docs/02-event-system.md`
2. Search decompiled source for relevant event types
3. Create test plugin to verify each capability
4. Document findings in DEVELOPMENT_ROADMAP.md

**Deliverable:**
Update roadmap with ✅ (available), ❌ (not available), or 🟡 (partial) for each feature.

---

### 2.8 Warmup Damage Monitoring
- **Priority:** P0
- **Effort:** 0.5 day
- **Status:** :red_circle: Not Started

**Problem:**
Teleport warmups (e.g., `/f home`, `/f unstuck`) currently only cancel on movement, not on damage. This creates an exploit where players can start a teleport, take damage, and still escape.

**Solution:**
- Add damage event monitoring during warmup period
- Cancel warmup if player takes ANY damage from any source
- Configurable option: `teleport.cancelOnDamage` (default: true)

**Config Addition:**
```json
{
  "teleport": {
    "warmupSeconds": 5,
    "cooldownSeconds": 300,
    "cancelOnMove": true,
    "cancelOnDamage": true  // NEW
  }
}
```

**Files to Modify:**
- `TeleportManager.java` - Add damage event listener during warmup
- `HyperFactionsConfig.java` - Add `cancelOnDamage` config option
- `HyperFactionsPlugin.java` - Register damage event handler (if needed)

**Reference:** HyperHomes has similar implementation

---

### 2.9 Update Checker (GitHub Releases)
- **Priority:** P1
- **Effort:** 0.5 day
- **Status:** :white_check_mark: **COMPLETE**

**Description:**
Implement GitHub release checking similar to HyperPerms to notify server admins when new versions are available.

**Features:**
- Check GitHub API for latest release on plugin enable
- Async/non-blocking using CompletableFuture
- 5-minute cache to prevent API spam
- Semantic version comparison
- Optional changelog display
- Graceful error handling (network failures, missing releases)
- Configurable enable/disable option

**GitHub Repository:**
- URL: `https://github.com/HyperSystemsDev/HyperFactions`
- API Endpoint: `https://api.github.com/repos/HyperSystemsDev/HyperFactions/releases/latest`

**Special Note:**
⚠️ **No releases exist yet** - updater must gracefully handle 404 responses
- Add TODO comment: "Remove 404 handling after first release published"

**Config:**
```json
{
  "updates": {
    "enabled": true,
    "checkUrl": "https://api.github.com/repos/HyperSystemsDev/HyperFactions/releases/latest",
    "showChangelog": true
  }
}
```

**Files to Create:**
- New: `update/UpdateChecker.java` - Main update checking logic (copy pattern from HyperPerms)
- New: `update/UpdateInfo.java` - Record class for update information

**Files to Modify:**
- `HyperFactionsConfig.java` - Add updates config section
- `HyperFactionsPlugin.java` - Initialize UpdateChecker on enable()
- `HyperFactionsAPI.java` - Expose getUpdateChecker() method

**Implementation Pattern (from HyperPerms):**
- 284 lines of code
- Uses Apache HttpClient for GitHub API requests
- Caches result for 5 minutes (300,000ms)
- Version comparison handles semantic versioning (x.y.z)
- Downloads to temp file (.jar.tmp) then atomic rename
- All operations fully async

**Implementation Notes:**
- Created `update/UpdateChecker.java` with GitHub Releases API integration
- 5-minute response caching to prevent API spam
- Changelog support and optional auto-download
- Graceful 404 handling (no releases exist yet)
- Integrated into `HyperFactions.java` enable() method
- Config options: `updates.enabled`, `updates.checkUrl`

---

### 2.10 Spawnkilling Prevention
- **Priority:** P1
- **Effort:** 1 day
- **Status:** :white_check_mark: **COMPLETE**

**Problem:**
During raids or wars, players who die and respawn at their faction home can be repeatedly killed by enemies camping the spawn point. This creates unfair "spawn camping" scenarios.

**Solution:**
Grant temporary invulnerability after respawn when player died in PvP and respawns in own faction territory.

**Features:**
- **Duration:** 10-15 seconds (configurable)
- **Trigger Conditions:**
  - Player died to another player (PvP death)
  - Respawning in own faction territory
  - Optional: Only during active raid/war (if system exists)
- **Break Conditions:**
  - Player attacks or damages another player
  - Player moves outside faction territory
  - Duration expires
- **Visual Indicator:** Particle effects or potion effect icon

**Config:**
```json
{
  "combat": {
    "spawnProtection": {
      "enabled": true,
      "durationSeconds": 10,
      "onlyDuringRaid": false,      // Future: true when raid system implemented
      "breakOnAttack": true,
      "breakOnLeavingTerritory": true
    }
  }
}
```

**Files to Create/Modify:**
- New: `RespawnProtectionSystem.java` - ECS event handler for death/respawn events
- Modify: `CombatTagManager.java` - Track spawn protection state per player
- Modify: `ProtectionListener.java` - Check spawn protection in damage events
- Modify: `HyperFactionsConfig.java` - Add spawnProtection config section

**Implementation Notes:**
- Use Map<UUID, Instant> to track protection expiry times
- Listen to PlayerRespawnEvent and DamageEvent
- Check if respawn location is in own faction territory
- Apply temporary damage immunity
- Cancel protection if player attacks or leaves territory

**Future Enhancement:**
When raid/war system (3.4, 3.7) is implemented, make `onlyDuringRaid` default to true for more realistic combat.

**Implementation Notes:**
- Created `data/SpawnProtection.java` record to track protection state
- Added spawn protection tracking to `CombatTagManager.java`
- Added `DENIED_SPAWN_PROTECTED` to `ProtectionChecker.PvPResult` enum
- Config options added: `combat.spawnProtection.{enabled, durationSeconds, breakOnAttack, breakOnMove}`
- Protection automatically removed when player attacks or leaves own territory

---

### 2.11 GUI System Overhaul & Command Restructure
- **Priority:** P0
- **Effort:** 2-3 weeks
- **Status:** :red_circle: Not Started

**Goal:** Make HyperFactions the most user-friendly, polished faction plugin with an intuitive GUI system that provides full feature access without requiring commands.

**Current State:**
- GUI system exists but partially implemented
- Many features only accessible via commands
- `/f` requires sub-commands (e.g., `/f info`, `/f list`)
- GUI not fully functional or polished

**Target State:**
- **Command Change:** `/f` or `/hf` alone opens main GUI menu
- **Sub-commands Still Work:** `/f info`, `/f list`, etc. for compatibility and power users
- **Full Feature Parity:** Everything accessible via commands also accessible via GUI
- **Polished & Functional:** GUI must be fully tested, bug-free, and visually appealing
- **Exception:** Admin/technical features (updater, debug) remain command-only

---

#### Main GUI Menu (opened by `/f` or `/hf`)

```
┌─────────────────────────────────────────┐
│      HyperFactions - Main Menu          │
├─────────────────────────────────────────┤
│                                         │
│  [My Faction]    [Faction List]        │
│  View and manage your current faction   │
│  or create a new one                    │
│                                         │
│  [Map & Claims]  [Relations]           │
│  Territory viewer and claim management  │
│                                         │
│  [Power Status]  [Teleports]           │
│  Your power and faction home            │
│                                         │
│  [Settings]      [Help]                │
│  Configuration and documentation        │
│                                         │
└─────────────────────────────────────────┘
```

**Main Menu Sections:**
1. **My Faction** - Opens faction management GUI (if in faction) or creation screen
2. **Faction List** - Browse all factions, search, filter
3. **Map & Claims** - Interactive territory viewer with claim/unclaim
4. **Relations** - Manage allies, enemies, view relations
5. **Power Status** - Player and faction power overview
6. **Teleports** - Faction home teleport with warmup status
7. **Settings** - Personal preferences (notifications, etc.)
8. **Help** - Tutorial, command reference, feature guide

---

#### Feature-Specific GUI Screens

##### 1. Faction Management GUI (`/f` → My Faction)

**Layout:**
```
┌─────────────────────────────────────────┐
│  [Faction Name]              [Leader]   │
│  Description: ...                       │
├─────────────────────────────────────────┤
│                                         │
│  📊 Overview    👥 Members   ⚙️ Settings │
│                                         │
│  Power: ████████░░ 80/100               │
│  Claims: 15/40                          │
│  Balance: $12,345 (if economy enabled)  │
│                                         │
│  [Members] Shows member list with       │
│  - Name, role, power, online status     │
│  - Click member for actions (promote,   │
│    demote, kick, transfer leadership)   │
│                                         │
│  [Quick Actions]                        │
│  • Invite Player                        │
│  • Set Home                             │
│  • Manage Claims                        │
│  • View Relations                       │
│  • Treasury (if economy enabled)        │
│                                         │
└─────────────────────────────────────────┘
```

**Tabs:**
- **Overview:** Faction stats, description, quick actions
- **Members:** Member list with role management
- **Settings:** Rename, description, color, open/closed, permissions

##### 2. Territory/Claims GUI (`/f` → Map & Claims)

**Interactive Map View:**
```
┌─────────────────────────────────────────┐
│         Territory Map Viewer            │
├─────────────────────────────────────────┤
│                                         │
│      [9x9 chunk grid centered on you]   │
│                                         │
│  Legend:                                │
│  🟢 Your Faction   🔵 Ally              │
│  🔴 Enemy          ⚪ Wilderness        │
│  🟡 SafeZone       🟠 WarZone           │
│                                         │
│  Current Chunk:                         │
│  Owner: [Faction Name / Wilderness]     │
│  Type: [Territory / SafeZone / etc.]    │
│                                         │
│  [Claim This Chunk]  [Unclaim]          │
│  [Auto-Claim: ON/OFF]                   │
│                                         │
└─────────────────────────────────────────┘
```

**Features:**
- Real-time chunk visualization
- Click chunk to view details
- One-click claim/unclaim
- Auto-claim toggle
- Power cost preview
- Claim limits shown

##### 3. Relations GUI (`/f` → Relations)

**Layout:**
```
┌─────────────────────────────────────────┐
│           Faction Relations             │
├─────────────────────────────────────────┤
│                                         │
│  Allies (3/10)        [Requests: 2]     │
│  ├─ FactionA   [Break Alliance]         │
│  ├─ FactionB   [Break Alliance]         │
│  └─ FactionC   [Break Alliance]         │
│                                         │
│  Enemies (1/--)       [Declare Enemy]   │
│  └─ EvilCorp   [Set Neutral]            │
│                                         │
│  Ally Requests Received:                │
│  ├─ FactionD   [Accept] [Decline]       │
│  └─ FactionE   [Accept] [Decline]       │
│                                         │
│  Ally Requests Sent:                    │
│  └─ FactionF   [Cancel Request]         │
│                                         │
│  [Request Alliance]  [Declare Enemy]    │
│                                         │
└─────────────────────────────────────────┘
```

**Features:**
- Visual relationship overview
- One-click accept/decline requests
- Easy alliance breaking
- Enemy declaration with confirmation
- Request tracking

##### 4. Faction List GUI (`/f` → Faction List)

**Browse All Factions:**
```
┌─────────────────────────────────────────┐
│          All Factions (45)              │
│  [Search: ___________]  [Filter ▼]      │
├─────────────────────────────────────────┤
│                                         │
│  1. MegaCorp        Power: 250  👥 15   │
│     Leader: Player1    Claims: 40       │
│     [View Details]                      │
│                                         │
│  2. Warriors        Power: 180  👥 10   │
│     Leader: Player2    Claims: 28       │
│     [View Details]                      │
│                                         │
│  3. Builders        Power: 120  👥 8    │
│     Leader: Player3    Claims: 15       │
│     [View Details]                      │
│                                         │
│  [◀ Prev]  Page 1/5  [Next ▶]          │
│                                         │
└─────────────────────────────────────────┘
```

**Features:**
- Search by name
- Filter by: size, power, open/closed, relation
- Sort by: power, members, claims, age
- Pagination
- Click for detailed view

##### 5. Treasury/Economy GUI (`/f` → My Faction → Treasury)

**Only shown if economy module enabled:**
```
┌─────────────────────────────────────────┐
│         Faction Treasury                │
├─────────────────────────────────────────┤
│                                         │
│  Balance: $12,345                       │
│                                         │
│  📊 Income & Expenses                   │
│  ├─ Member Taxes: +$500/day            │
│  ├─ Upkeep Costs: -$200/day            │
│  └─ Net: +$300/day                     │
│                                         │
│  Next Payment: 6 hours                  │
│  Status: ✅ Funded                      │
│                                         │
│  [Deposit]  [Withdraw]  [History]       │
│                                         │
│  Recent Transactions:                   │
│  ├─ +$100 Deposit from Player1          │
│  ├─ -$200 Upkeep payment                │
│  └─ +$500 Tax collection                │
│                                         │
└─────────────────────────────────────────┘
```

**Features:**
- Visual balance overview
- Income/expense breakdown
- Deposit/withdraw buttons
- Transaction history with filtering
- Upkeep status warnings
- Tax collection status

##### 6. War & Raids GUI (`/f` → Relations → Wars/Raids Tab)

**War Management:**
```
┌─────────────────────────────────────────┐
│            Active Wars                  │
├─────────────────────────────────────────┤
│                                         │
│  War vs. EvilCorp                       │
│  Status: Active (Day 3/7)               │
│  Cost: $5,000 initial + $500/day        │
│  Total Spent: $6,500                    │
│                                         │
│  Objectives:                            │
│  ├─ Kills: 15/50   ████░░░░░░ 30%      │
│  ├─ Territory: 2/10 ██░░░░░░░░ 20%     │
│  └─ Resources: $500/$1000 ████░░░░ 50% │
│                                         │
│  Victory Spoils: ~$8,000                │
│                                         │
│  [Propose Peace] [Surrender] [Status]   │
│                                         │
└─────────────────────────────────────────┘
```

**Features:**
- Active war overview
- Objective tracking with progress bars
- Cost visualization
- Peace negotiation interface
- Surrender confirmation with penalty preview
- Raid declaration with cost calculator

##### 7. Permissions GUI (`/f` → My Faction → Permissions)

**Role-Based Permission Matrix:**
```
┌─────────────────────────────────────────┐
│        Territory Permissions            │
│  Configure what each role can do        │
├─────────────────────────────────────────┤
│                                         │
│  Permission    Member  Officer  Leader  │
│  ──────────────────────────────────────  │
│  Build         ✅      ✅       ✅      │
│  Interact      ✅      ✅       ✅      │
│  Container     ✅      ✅       ✅      │
│  Claim         ❌      ✅       ✅      │
│  Unclaim       ❌      ✅       ✅      │
│  Invite        ❌      ✅       ✅      │
│  Kick          ❌      ✅       ✅      │
│  Promote       ❌      ❌       ✅      │
│  SetHome       ❌      ✅       ✅      │
│  Withdraw      ❌      ✅       ✅      │
│                                         │
│  [Reset to Defaults]  [Save Changes]    │
│                                         │
└─────────────────────────────────────────┘
```

**Features:**
- Visual permission matrix
- Click to toggle permissions
- Role-based columns
- Color-coded (green = allowed, red = denied)
- Reset option
- Save confirmation

##### 8. Zone Management GUI (Admin)

**For SafeZone/WarZone configuration:**
```
┌─────────────────────────────────────────┐
│          Zone Management                │
├─────────────────────────────────────────┤
│                                         │
│  Zones (5):                             │
│  ├─ Spawn (SafeZone)     [Edit] [Del]  │
│  ├─ Arena (WarZone)      [Edit] [Del]  │
│  ├─ Tournament (WarZone) [Edit] [Del]  │
│  └─ ...                                 │
│                                         │
│  [Create New Zone]                      │
│                                         │
│  ─── Editing: Tournament ───            │
│  Name: Tournament                       │
│  Type: [WarZone ▼]                      │
│                                         │
│  Flags:                                 │
│  ├─ Allow PvP:        ✅               │
│  ├─ Allow Item Drop:  ❌               │
│  ├─ Allow Building:   ❌               │
│  └─ Consume Power:    ❌               │
│                                         │
│  [Save] [Cancel] [Reset to Defaults]    │
│                                         │
└─────────────────────────────────────────┘
```

---

#### Command System Changes

**New Behavior:**
- **`/f`** or **`/hf`** alone → Opens main GUI menu
- **`/f <subcommand>`** → Executes command directly (for power users)
- **`/f help`** → Shows command reference (also available in GUI)

**Examples:**
```bash
/f                    # Opens GUI
/hf                   # Opens GUI (alias)
/f info               # Shows faction info in chat (command mode)
/f create MyFaction   # Creates faction (command mode)
/f gui                # Also opens GUI (explicit)
```

**Compatibility:**
- All existing commands continue to work
- Power users can use commands for speed
- New players discover features via GUI
- Commands provide scriptability for advanced users

---

#### GUI Feature Coverage

**Must Have GUI Implementation:**

**Core Features:**
- ✅ Faction creation/deletion
- ✅ Member management (invite, kick, promote, demote, transfer)
- ✅ Faction settings (name, description, color, open/closed)
- ✅ Claim/unclaim territory (interactive map)
- ✅ Auto-claim toggle
- ✅ Faction home (set, delete, teleport)
- ✅ Relations (ally request, accept, break, enemy, neutral)
- ✅ Power viewing (self, faction, other players)
- ✅ Faction list/browse with search/filter
- ✅ Player/faction lookup (`/f who` equivalent)

**Economy Features (if module enabled):**
- ✅ Treasury balance viewing
- ✅ Deposit/withdraw
- ✅ Transaction history
- ✅ Tax status (personal and faction-wide)
- ✅ Upkeep status and warnings
- ✅ Tax payment interface

**Advanced Features:**
- ✅ Zone creation/management (admin GUI)
- ✅ Zone configuration (per-zone flags)
- ✅ War declaration with cost preview
- ✅ War status and objectives
- ✅ Raid declaration with cost calculator
- ✅ Raid status tracking
- ✅ Peace negotiations interface
- ✅ Permission matrix editing (role-based)
- ✅ Teleport with warmup/cooldown status

**Command-Only Features (No GUI):**
- ❌ Update checker (admin/technical)
- ❌ Debug commands
- ❌ Low-level configuration
- ❌ Data migration/import
- ❌ API inspection

---

#### Technical Requirements

**GUI Framework:**
- Use Hytale's native GUI system (if available)
- Fallback: Custom menu system with clickable items
- Responsive design (adapts to screen size)
- Keyboard navigation support
- Mouse support for all interactions

**Performance:**
- Lazy loading for large lists (pagination)
- Async data fetching (don't block main thread)
- Cache frequently accessed data
- Smooth animations/transitions
- No lag when opening menus

**Polish Requirements:**
- Consistent visual design across all screens
- Clear iconography and labels
- Color-coded elements (green = good, red = bad, etc.)
- Hover tooltips for all buttons/options
- Confirmation dialogs for destructive actions
- Loading indicators for async operations
- Error messages with helpful context
- Success feedback (checkmarks, animations)

**Accessibility:**
- Readable font sizes
- High contrast colors
- Clear button hit areas
- Keyboard shortcuts
- Screen reader friendly (if possible)

**Testing Requirements:**
- Every GUI screen must be tested
- All buttons must work
- Edge cases handled (empty lists, max limits, etc.)
- No broken flows (always a way to go back)
- Mobile-friendly (if Hytale supports mobile)

---

#### Implementation Phases

**Phase A: Core GUI Foundation (1 week)**
- Main menu GUI (`/f` opens menu)
- Command restructure (`/f` alone behavior)
- Navigation framework
- Basic faction management GUI
- Faction list GUI

**Phase B: Feature Parity (1 week)**
- Territory/claims interactive map
- Relations GUI (allies, enemies, requests)
- Member management GUI
- Settings GUI
- Power status GUI
- Teleport GUI with warmup indicators

**Phase C: Advanced Features (0.5 week)**
- Economy GUIs (treasury, taxes, transactions)
- War/raid GUIs (declaration, status, objectives)
- Permission matrix GUI
- Zone management GUI (admin)

**Phase D: Polish & Testing (0.5 week)**
- Visual consistency pass
- Animation and transitions
- Error handling and validation
- Edge case testing
- Performance optimization
- User testing and feedback

---

#### GUI Design Principles

1. **Discoverability:** Players should discover features by browsing the GUI
2. **Consistency:** Similar actions should look/behave similarly across all screens
3. **Clarity:** No ambiguity about what buttons do or what state things are in
4. **Feedback:** Every action should have immediate visual feedback
5. **Safety:** Destructive actions require confirmation
6. **Efficiency:** Common actions should be quick (1-2 clicks max)
7. **Flexibility:** Support both GUI and command workflows
8. **Beauty:** Visual appeal matters - make it look professional

---

#### Files to Create/Modify

**New Files:**
- New: `gui/MainMenuGUI.java` - Main faction menu
- New: `gui/FactionManagementGUI.java` - Faction overview/management
- New: `gui/ClaimsMapGUI.java` - Interactive territory map
- New: `gui/RelationsGUI.java` - Ally/enemy management
- New: `gui/FactionListGUI.java` - Browse all factions
- New: `gui/TreasuryGUI.java` - Economy/banking interface
- New: `gui/WarStatusGUI.java` - War/raid tracking
- New: `gui/PermissionsGUI.java` - Permission matrix editor
- New: `gui/ZoneManagementGUI.java` - Zone admin interface
- New: `gui/GUIManager.java` - Shared GUI utilities and framework
- New: `gui/components/` - Reusable GUI components (buttons, lists, etc.)

**Modified Files:**
- Modify: `HyperFactionsPlugin.java` - Initialize GUI manager
- Modify: `FactionCommand.java` - Change `/f` alone behavior to open GUI
- Modify: All manager classes - Add GUI update hooks (when data changes, update open GUIs)

**Priority:** P0 - Critical for user experience and adoption

---

## Phase 3: Major Features

> Estimated Time: 1+ week each

### 3.0 WarZone Per-Zone Configuration
- **Priority:** P1
- **Effort:** 1 day
- **Status:** :white_check_mark: **COMPLETE**

**Current Problem:**
All WarZones behave identically with global settings. Server admins cannot customize individual zones for different purposes (tournaments vs training grounds vs battlefields).

**Proposed Solution:**
Add per-zone configuration flags to customize behavior of each SafeZone and WarZone.

**Configuration Flags:**
- `allowPvP` - Enable/disable PvP combat
- `allowItemDrop` - Enable/disable item drops on death
- `allowBlockBreak` - Enable/disable block breaking
- `allowBlockPlace` - Enable/disable block placement
- `consumePower` - Enable/disable power loss on death

**Use Cases:**

| Use Case | PvP | Item Drop | Block Edit | Power Loss |
|----------|-----|-----------|------------|------------|
| Tournament Arena | ✅ | ❌ | ❌ | ❌ |
| Training Ground | ✅ | ✅ | ✅ | ❌ |
| Battlefield | ✅ | ✅ | ✅ | ✅ |
| Safe Spawn | ❌ | ❌ | ❌ | ❌ |

**Config Structure:**

Global defaults in `config.json`:
```json
{
  "zones": {
    "defaults": {
      "safezone": {
        "allowPvP": false,
        "allowItemDrop": false,
        "allowBlockBreak": false,
        "allowBlockPlace": false,
        "consumePower": false
      },
      "warzone": {
        "allowPvP": true,
        "allowItemDrop": true,
        "allowBlockBreak": true,
        "allowBlockPlace": true,
        "consumePower": false
      }
    }
  }
}
```

Per-zone overrides stored in zone data (zones.json):
```json
{
  "zones": [
    {
      "id": "uuid",
      "name": "Tournament",
      "type": "WARZONE",
      "flags": {
        "allowItemDrop": false,
        "allowBlockBreak": false,
        "allowBlockPlace": false
      }
    }
  ]
}
```

**Commands:**
- `/f zone config <name> <flag> <value>` - Set zone-specific flag
- `/f zone config <name> list` - View zone configuration
- `/f zone config <name> reset` - Reset to defaults

**Files to Modify:**
- `Zone.java` record - Add `Map<String, Boolean> flags` field (nullable, null = use defaults)
- `ZoneManager.java` - Add zone config methods (setFlag, getFlag, getEffectiveFlags)
- `ProtectionChecker.java` - Check zone flags when determining protection
- `FactionCommand.java` - Add zone config subcommands
- `HyperFactionsConfig.java` - Add zone defaults config section

**Priority:** P1 - High value for server event customization

**Implementation Notes:**
- Created `data/ZoneFlags.java` with 11 flag constants (ALLOW_PVP, ALLOW_ITEM_DROP, ALLOW_BLOCK_BREAK, ALLOW_BLOCK_PLACE, CONSUME_POWER, etc.)
- Added flags support to `Zone.java` record with builder pattern
- Added `setZoneFlag()`, `clearZoneFlag()`, `getEffectiveFlag()` to `ZoneManager.java`
- Updated `ProtectionChecker.java` to check zone flags for protection decisions
- Added `/f admin zoneflag <zone> <flag> <value>` command to `FactionCommand.java`
- Updated `JsonZoneStorage.java` for flags persistence with JSON serialization

---

### 3.1 Public API for Cross-Mod Integration (NEW)
- **Priority:** P1
- **Effort:** 1 week
- **Status:** :red_circle: Not Started

Expose clean APIs for economy plugins and other mods to integrate with HyperFactions.

**Current Problem:**
- `HyperFactionsAPI` exists but limited scope
- No economy integration points
- No event system for external plugins
- No manager access for advanced integrations

**API Design Pattern** (follows HyperPerms/HyperHomes patterns):

**Core API Access:**
```java
// Singleton pattern
HyperFactionsAPI.getInstance()
  .getFactionManager()      // Faction queries
  .getClaimManager()        // Territory queries
  .getPowerManager()        // Power calculations
  .getEconomyManager()      // Bank operations (when implemented)
  .getEventBus()            // Event subscriptions
```

**Economy API Extension:**
```java
public interface EconomyAPI {
    // Faction balance
    double getFactionBalance(UUID factionId);
    boolean hasFunds(UUID factionId, double amount);

    // Transactions
    CompletableFuture<TransactionResult> deposit(UUID factionId, double amount);
    CompletableFuture<TransactionResult> withdraw(UUID factionId, double amount, UUID actor);
    CompletableFuture<TransactionResult> transfer(UUID from, UUID to, double amount);

    // Transaction history
    List<Transaction> getTransactionHistory(UUID factionId, int limit);

    // Currency info
    String getCurrencyName();
    String formatCurrency(double amount);
}
```

**Event System:**
```java
public enum FactionEventType {
    // Faction lifecycle
    FACTION_CREATE,
    FACTION_DISBAND,
    FACTION_RENAME,

    // Membership
    MEMBER_JOIN,
    MEMBER_LEAVE,
    MEMBER_KICK,
    MEMBER_PROMOTE,
    MEMBER_DEMOTE,

    // Territory
    CHUNK_CLAIM,
    CHUNK_UNCLAIM,
    CHUNK_LOST,        // Due to power loss

    // Relations
    RELATION_CHANGE,   // Ally/enemy/neutral

    // Economy (future)
    BALANCE_CHANGE,
    TRANSACTION_COMPLETE,

    // Combat
    COMBAT_TAG_START,
    COMBAT_TAG_END,

    // War (future)
    WAR_DECLARE,
    WAR_END
}
```

**Usage Example (Economy Plugin):**
```java
// Economy plugin subscribing to faction events
HyperFactionsAPI.getInstance()
    .getEventBus()
    .subscribe(FactionEventType.FACTION_CREATE, event -> {
        UUID factionId = event.getFactionId();
        economyPlugin.createAccount(factionId, STARTING_BALANCE);
    });
```

**Soft Dependency Pattern:**
```java
// External plugin detecting HyperFactions
public class MyPlugin {
    private boolean hyperFactionsAvailable;

    void onEnable() {
        try {
            Class.forName("com.hyperfactions.HyperFactionsAPI");
            hyperFactionsAvailable = HyperFactionsAPI.isAvailable();

            if (hyperFactionsAvailable) {
                integrateWithFactions();
            }
        } catch (ClassNotFoundException e) {
            // HyperFactions not installed
        }
    }
}
```

**Native Hytale Feature Hooks:**

Research and document which native Hytale features we should NOT duplicate:
- ✅ Use native `/unstuck` logic where possible
- ✅ Use native world/chunk APIs (already doing)
- ✅ Use native ECS event systems (already doing)
- ❓ Research: Native economy API (if exists)
- ❓ Research: Native teleportation API
- ❓ Research: Native team/group systems

**Files to Create:**
- New: `EconomyAPI.java` interface
- New: `FactionEvent.java` record
- New: `EventBus.java` - event system (or reuse pattern from HyperPerms)
- Modify: `HyperFactionsAPI.java` - Expand public methods
- New: `docs/API-INTEGRATION.md` - Integration guide for plugin developers

**Priority:** High - enables ecosystem growth

---

### 3.2 Role-Specific Territory Permissions (NEW)
- **Priority:** P1
- **Effort:** 1.5 weeks
- **Status:** :red_circle: Not Started

Allow factions to configure what each role (and allies) can do in their territory.

**Current Problem:**
- Role permissions are hardcoded throughout command handlers
- Cannot customize what members vs officers can do
- Ally permissions are binary (all or nothing)

**Proposed Solution:**

**Config Structure:**
```json
{
  "rolePermissions": {
    "member": {
      "build": true,
      "interact": true,
      "container": true,
      "claim": false,
      "unclaim": false,
      "invite": false,
      "kick": false,
      "home": true
    },
    "officer": {
      "build": true,
      "interact": true,
      "container": true,
      "claim": true,
      "unclaim": true,
      "invite": true,
      "kick": true,
      "home": true,
      "sethome": true,
      "manage": false
    },
    "leader": {
      "_all": true
    },
    "ally": {
      "build": false,
      "interact": true,
      "container": false
    }
  }
}
```

**Permission Nodes (per-faction configurable):**
- `BUILD` - Place/break blocks
- `INTERACT` - Use doors, buttons, levers
- `CONTAINER` - Open chests, furnaces
- `CLAIM` - Claim new chunks
- `UNCLAIM` - Unclaim chunks
- `INVITE` - Invite new members
- `KICK` - Remove members
- `PROMOTE` - Promote members (officers only promote to officer)
- `DEMOTE` - Demote members
- `SETHOME` - Set faction home
- `HOME` - Use faction home
- `MANAGE` - Change faction settings (name, desc, color)
- `ECONOMY` - Withdraw from faction bank

**Commands:**
- `/f perms` - View current permission matrix
- `/f perms <role>` - View specific role permissions
- `/f perms <role> <permission> <true|false>` - Toggle permission (leader only)
- `/f perms reset` - Reset to config defaults

**Integration with HyperPerms:**

Expose faction context to HyperPerms for advanced permission scenarios:
```java
// Context calculator for HyperPerms
public class FactionContextCalculator implements ContextCalculator {
    void calculate(UUID player, ContextSet.Builder builder) {
        Faction faction = getFaction(player);
        if (faction != null) {
            builder.add("faction", faction.id());
            builder.add("faction_role", getRole(player).name());

            // Current chunk context
            Chunk chunk = getPlayerChunk(player);
            Faction chunkOwner = getChunkOwner(chunk);
            if (chunkOwner != null) {
                builder.add("territory", chunkOwner.id());
                builder.add("relation", getRelation(faction, chunkOwner).name());
            }
        }
    }
}
```

This enables HyperPerms nodes like:
- `hytale.fly` or `flight.allow` granted only in own faction territory
- `hyperfactions.build.ally` for ally territory building
- Role-specific permissions: `hyperfactions.officer.claim`

**Files to Create/Modify:**
- New: `RolePermissions.java` record - config model
- New: `FactionContextCalculator.java` - HyperPerms context integration
- Modify: `HyperFactionsConfig.java` - Add rolePermissions section
- Modify: `ProtectionChecker.java` - Check role permissions instead of hardcoded levels
- Modify: `FactionCommand.java` - Add /f perms commands
- Modify: All command handlers - Use permission checker instead of role.isAtLeast()

**Priority:** High - fundamental feature for server customization

---

### 3.3 Faction Treasury/Bank System
- **Priority:** P2
- **Effort:** 2.5 weeks (includes upkeep and taxation systems)
- **Status:** :red_circle: Not Started

Allow factions to have a shared money pool.

**Features:**
- `/f balance` - View faction balance
- `/f deposit <amount>` - Add money to faction
- `/f withdraw <amount>` - Take money (leader/officer only)
- Tax on claims (optional)
- Transaction history tracking
- Upkeep costs (see sub-feature below)

**Files to create/modify:**
- New: `FactionEconomy.java` record
- Modify: `Faction.java` - Add economy field
- Modify: `FactionManager.java` - Add economy methods
- Modify: `FactionCommand.java` - Add economy commands

**Economy Integration:**
- Hook into standard economy API (Vault-style)
- Fallback to built-in balance tracking

---

#### Sub-Feature: Faction Upkeep Costs

Factions must pay periodic upkeep from their treasury to maintain claims and operations.

**Upkeep Types:**
- **Base Upkeep:** Fixed cost per interval (e.g., 100/day)
- **Claim Upkeep:** Cost per claimed chunk (e.g., 10/chunk/day)
- **Member Upkeep:** Cost per member (e.g., 5/member/day)

**Payment Mechanics:**
- Automatic deduction every X hours (configurable, default: 24h)
- Warning notifications 24h before payment due
- Grace period if insufficient funds (default: 3 days)
- After grace period: Territory decay begins (lose claims)
- After extended period: Faction auto-disbands (optional, default: 14 days)

**Config:**
```json
{
  "economy": {
    "upkeep": {
      "enabled": true,
      "baseCost": 100,
      "costPerClaim": 10,
      "costPerMember": 5,
      "intervalHours": 24,
      "gracePeriodDays": 3,
      "autoDisbandAfterDays": 14
    }
  }
}
```

**Files to Modify (in addition to treasury files):**
- Modify: `FactionEconomy.java` - Add upkeep tracking (lastPayment, gracePeriodStart)
- Modify: `FactionManager.java` - Add calculateUpkeep() and processUpkeep() methods
- New: `UpkeepTask.java` - Scheduled task to process upkeep periodically

**Effort:** +0.5 week to treasury implementation

---

#### Sub-Feature: Faction Member Taxes

Automatic taxation of faction members to fund the faction treasury, with configurable rates based on member role.

**Tax Types:**
- **Periodic Tax:** Deducted from player balance at regular intervals (daily/weekly)
- **Activity Tax:** Tax on in-game activities (kills, mining, trading) - future consideration
- **Login Tax:** One-time tax per login session - future consideration

**Role-Based Tax Brackets:**
Allow factions to configure different tax rates per role:
- **Leader:** Configurable (default: 0% - leaders typically exempt)
- **Officer:** Configurable (default: 50 currency/day)
- **Member:** Configurable (default: 100 currency/day)

**Tax Collection Mechanics:**
- Automatic deduction at configured intervals
- Manual collection: `/f tax collect` command (officers+)
- Tax balance tracking per member (owed, paid, overdue)
- Transaction history integration

**Tax Enforcement Options:**
- Warning notifications when player balance insufficient
- Grace period if player can't pay (default: 3 days)
- Optional: Auto-kick after grace period expires
- Optional: Territory restrictions (can't build/claim) until taxes paid
- Optional: Temporary demotion or role suspension

**Config:**
```json
{
  "economy": {
    "taxes": {
      "enabled": true,
      "type": "periodic",
      "intervalHours": 24,
      "brackets": {
        "leader": 0,
        "officer": 50,
        "member": 100
      },
      "gracePeriodDays": 3,
      "enforcement": {
        "autoKickOnDefault": false,
        "restrictTerritory": true,
        "restrictClaiming": true,
        "restrictWithdraw": true
      }
    }
  }
}
```

**Commands:**
- `/f tax` - View your current tax status (owed, paid, next due date)
- `/f tax info` - View faction tax rates and brackets
- `/f tax pay` - Manually pay owed taxes
- `/f tax collect` - Force collection from all members (officers+)
- `/f tax history [player]` - View tax payment history
- `/f tax setrate <role> <amount>` - Configure tax bracket (leader only)

**Use Cases:**
- **Democratic Factions:** Equal taxes for all (members/officers same rate)
- **Hierarchical Factions:** Higher taxes for members, lower for officers (incentivize promotion)
- **Socialist Factions:** Officers pay more than members (leadership pays for privilege)
- **Meritocracy:** Activity-based taxes (future: tax players based on contribution)

**Files to Modify (in addition to treasury files):**
- Modify: `FactionEconomy.java` - Add tax tracking (taxOwed, taxPaid, lastTaxDate per member)
- Modify: `FactionManager.java` - Add calculateMemberTax(), collectTaxes() methods
- New: `TaxCollectionTask.java` - Scheduled task for automatic tax collection
- Modify: `FactionCommand.java` - Add tax management commands
- Modify: `ProtectionChecker.java` - Enforce territory restrictions for tax defaulters

**Integration Considerations:**
- Works alongside upkeep costs (dual funding model)
- Tax collection can auto-fund upkeep payments
- Excess taxes contribute to faction treasury for other purposes
- Consider tax exemptions for new members (grace period on join)

**Effort:** +0.5 week to treasury implementation

---

### 3.4 Raid System with Cooldowns
- **Priority:** P2
- **Effort:** 2 weeks
- **Status:** :red_circle: Not Started

Structured raiding with windows and cooldowns.

**Hytale API Limitation:**
Hytale does NOT have explosive mechanics currently, so raid systems must use alternative approaches:
- ✅ Block breaking (requires manual mining/digging)
- ✅ PvP combat mechanics
- ✅ Objective-based systems (capture points, resource collection)
- ❌ Explosive-based breaching (not currently possible)

**Features:**
- Raid declaration period (30 min warning)
- Raid window (2-4 hours active)
- Post-raid cooldown (can't raid same faction for 24h)
- Raid notifications to all faction members
- Optional: Raid points/objectives
- **Optional: Declaration costs** (see economic integration below)

**Config Options:**
```json
{
  "raid": {
    "enabled": true,
    "declarationMinutes": 30,
    "windowHours": 2,
    "cooldownHours": 24,
    "notifyOnDeclare": true,
    "costs": {
      "enabled": false,
      "baseCost": 1000,
      "costPerDefender": 100,
      "refundOnWin": 0.5,
      "penaltyOnLoss": 500
    }
  }
}
```

**Economic Integration (Optional):**

Add strategic weight to raid declarations through costs:

**Declaration Costs:**
- **Base Cost:** Fixed amount to declare raid (e.g., 1000 currency)
- **Scaling Cost:** Additional cost per defender faction member (e.g., 100/member)
  - Prevents small factions from being spam-raided
  - Makes raiding large factions more expensive
- **Refund on Win:** Percentage refund if attacker achieves objectives (e.g., 50%)
- **Penalty on Loss:** Additional cost deducted if attacker loses/abandons raid (e.g., 500)

**Strategic Benefits:**
- Prevents frivolous or spam raid declarations
- Adds economic risk/reward to raiding
- Small factions can't be constantly harassed without cost
- Encourages meaningful, well-planned raids
- Creates economy sink for faction treasuries

**Example Cost Calculation:**
```
Raiding faction with 10 defenders:
Base: 1000 + (10 × 100) = 2000 cost

If attacker wins: Refund 50% = 1000 returned (net cost: 1000)
If attacker loses: 2000 + 500 penalty = 2500 total cost
```

**Commands:**
- `/f raid declare <faction>` - Deducts cost, starts declaration period
- `/f raid cost <faction>` - Preview raid cost before declaring
- `/f raid status` - View active raids and costs paid

**Files to create:**
- New: `RaidManager.java`
- New: `Raid.java` record - Add `declarationCost`, `refundAmount` fields
- Modify: `FactionEconomy.java` - Handle raid cost transactions

---

### 3.5 Territory Decay for Inactive Factions
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

### 3.6 Faction Levels/Progression
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

### 3.7 War Declaration System
- **Priority:** P3
- **Effort:** 2 weeks
- **Status:** :red_circle: Not Started

Formal war declarations with objectives.

**Features:**
- `/f war declare <faction>` - Start war
- `/f war status` - View active wars
- `/f war surrender` - Give up (with optional penalty)
- War objectives (kills, territory, resources)
- War score tracking
- Peace negotiations and treaties
- **Optional: War economics** (see economic integration below)

**War States:**
1. Declaration (1h notice period)
2. Active War (unrestricted raiding)
3. Peace Negotiation
4. Ceasefire/Peace

**Config Options:**
```json
{
  "war": {
    "enabled": true,
    "declarationHours": 1,
    "objectives": {
      "killsToWin": 50,
      "territoryToCapture": 10,
      "resourcesRequired": 1000
    },
    "costs": {
      "enabled": false,
      "declarationCost": 5000,
      "dailyUpkeep": 500,
      "peaceTreatyCost": 2000,
      "surrenderPenalty": 10000,
      "victorySpoils": 0.5
    }
  }
}
```

**Economic Integration (Optional):**

Add strategic depth and consequences to war declarations through economic systems:

**War Costs:**
- **Declaration Cost:** Upfront payment to declare war (e.g., 5000 currency)
  - Prevents casual/frivolous war declarations
  - Ensures factions are committed before starting
- **Daily Upkeep:** Ongoing cost per day during active war (e.g., 500/day)
  - Simulates war economy (supplies, equipment, logistics)
  - Incentivizes swift victories or peace negotiations
  - Prevents endless stalemate wars
- **Peace Treaty Cost:** Cost to offer peace treaty (e.g., 2000 currency)
  - Adds weight to peace negotiations
  - Losing side may need to pay more
- **Surrender Penalty:** Cost to unilaterally surrender (e.g., 10000 currency)
  - Discourages easy surrender without attempting defense
  - Can be paid to enemy faction as reparations

**Victory Rewards:**
- **Spoils of War:** Winner receives percentage of loser's treasury (e.g., 50%)
- **Territory Claims:** Winner can claim X chunks from loser without power cost
- **Resource Compensation:** Winner receives configured amount from loser's coffers
- **Tribute System:** Loser pays periodic tribute for X days after war ends

**Strategic Dynamics:**
- **Wealthy factions can sustain longer wars** (daily upkeep)
- **Poor factions must win quickly** or sue for peace
- **Economic warfare:** Drain enemy treasury through prolonged conflict
- **Peace incentives:** Both sides motivated to end costly wars
- **Diplomatic options:** Pay for peace vs fight to bankruptcy

**Example War Economics:**
```
Faction A declares war on Faction B:
- Declaration cost: 5000 (paid immediately)
- War lasts 10 days: 500/day × 10 = 5000 upkeep
- Faction A wins:
  - Total cost: 10000
  - Victory spoils: 50% of Faction B treasury = 8000
  - Net result: -2000 (but gained territory/prestige)

Faction B surrenders on day 5:
- Surrender penalty: 10000
- 50% paid to Faction A as reparations
- Lost territory in peace terms
```

**Commands:**
- `/f war declare <faction>` - Deducts declaration cost, starts war
- `/f war cost <faction>` - Preview war costs before declaring
- `/f war status` - View active wars, daily upkeep, total spent
- `/f war surrender [reparations]` - Pay penalty and surrender
- `/f war peace <faction> [offer]` - Propose peace treaty with terms
- `/f war spoils` - View potential victory rewards

**Config Flexibility:**
- Server admins can disable economic system entirely (`costs.enabled: false`)
- Each cost component configurable independently
- Victory spoils can be disabled (set to 0)
- Surrender can be free (penalty: 0) or very expensive

**Files to create/modify:**
- New: `WarManager.java` - War state management
- New: `War.java` record - Add economic tracking fields (costPaid, upkeepAccrued, spoils)
- Modify: `FactionEconomy.java` - Handle war cost transactions
- New: `WarUpkeepTask.java` - Daily upkeep deduction task
- Modify: `FactionCommand.java` - Add war economic commands

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
  - Mob/creature spawning
  - Teleportation item usage
  - Special ability teleportation
  - Environmental interactions (ice formation, etc.)
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

### 4.11 Faction Shops
- **Description:** Faction-owned shops with integration to external shop plugins
- **Features:**
  - Faction members can create shops in faction territory
  - Transactions use faction treasury (optional)
  - Permission-based access (members only, allies allowed, etc.)
  - Integration with command shops, NPC shops, or sign shops
- **Commands:**
  - `/f shop create <name>` - Create faction shop
  - `/f shop delete <name>` - Remove faction shop
  - `/f shop config <name>` - Configure shop settings
  - `/f shop additem <name> <item> <buy> <sell>` - Add item to shop
  - `/f shop list` - List all faction shops
- **Config:**
  ```json
  {
    "shops": {
      "enabled": true,
      "type": "command",          // "command", "npc", "sign", "external"
      "memberOnly": true,
      "allowAllies": false,
      "useFactionTreasury": true
    }
  }
  ```
- **Dependencies:**
  - Requires economy system (3.3)
  - Requires external shop plugin integration
  - Requires mature public API (3.1)
- **Priority:** P3 - Post-v1.0 feature
- **Effort:** 2 weeks

---

## Technical Debt

### Performance Issues

| Issue | Location | Impact | Fix | Status |
|-------|----------|--------|-----|--------|
| O(n) getFactionClaims() | ClaimManager.java | High with many claims | Add reverse index Map<UUID, Set<ChunkKey>> | ✅ Complete |
| No auto-save | HyperFactions.java | Data loss on crash | Add periodic save task | ✅ Complete |
| 7 callback params | TeleportManager.java | Hard to use | Create TeleportContext object | ✅ Complete |
| No invite cleanup | InviteManager.java | Memory leak | Add scheduled cleanup task | ✅ Complete |

**Technical Debt Resolution Notes (v0.3.0):**
- **Claim Reverse Index:** Added `factionClaimsIndex` Map in ClaimManager for O(1) lookups of faction claims
- **Auto-save:** Added configurable periodic save task in HyperFactions.java (default: 5 minutes)
- **TeleportContext:** Created `data/TeleportContext.java` record with builder pattern to replace callback parameters
- **Invite Cleanup:** Integrated invite cleanup into periodic tasks in HyperFactions.java

### Code Quality

| Issue | Location | Recommendation |
|-------|----------|----------------|
| Hardcoded movement threshold | TeleportManager.java:254 | Move to config |
| Lazy cleanup race condition | CombatTagManager.java:50 | Use atomic operations |
| Missing null checks | Various | Add @NotNull annotations and checks |

---

## Architecture Improvements: Modular Design

> Goal: Features as pluggable modules that can be disabled, configured, or replaced

### Current State (Monolithic)

All features are **hardcoded as enabled** in `HyperFactionsPlugin`:
- All 8 managers initialize unconditionally
- No feature toggle system
- Protection system embedded in platform layer
- Cannot disable subsystems at runtime

### Target State (Modular)

**Module Interface Pattern:**
```java
public interface FactionModule {
    String getName();
    boolean isEnabled();
    void onEnable();
    void onDisable();
    void reload();
}
```

**Proposed Modules:**

| Module | Description | Can Disable? |
|--------|-------------|--------------|
| `CoreModule` | Faction CRUD, membership | ❌ Required |
| `GUIModule` | GUI system and interfaces | ❌ Required* |
| `ClaimsModule` | Territory claiming | ✅ Optional |
| `PowerModule` | Power system | ✅ Optional |
| `CombatModule` | Combat tagging | ✅ Optional |
| `RelationsModule` | Ally/enemy system | ✅ Optional |
| `ZonesModule` | SafeZone/WarZone | ✅ Optional |
| `TeleportModule` | Faction home teleports | ✅ Optional |
| `EconomyModule` | Treasury/bank (future) | ✅ Optional |
| `WarModule` | War declarations (future) | ✅ Optional |

*GUIModule is technically required but each module provides its own GUI screens that are only shown when the module is enabled.

**Config Structure:**
```json
{
  "modules": {
    "claims": { "enabled": true },
    "power": { "enabled": true },
    "combat": { "enabled": true },
    "relations": { "enabled": true },
    "zones": { "enabled": true },
    "teleport": { "enabled": true }
  }
}
```

**Benefits:**
- Server admins can disable unwanted features
- Alternative implementations can be swapped (e.g., different power systems)
- Reduces overhead when features not used
- Cleaner dependency management

**Implementation Priority:** P2 (Phase 3)

---

### Module Configuration Files

When implementing the modular system, each module should have its own configuration file for easier management:

**Directory Structure:**
```
HyperFactions/
├── config.json              # Main config (module toggles, core settings)
└── modules/
    ├── claims.json          # Claims module settings
    ├── power.json           # Power system settings
    ├── combat.json          # Combat tag & spawn protection
    ├── zones.json           # SafeZone/WarZone settings
    ├── economy.json         # Treasury & upkeep settings
    ├── relations.json       # Ally/enemy settings
    └── teleport.json        # Home teleport settings
```

**Main Config (`config.json`):**
```json
{
  "_readme": "HyperFactions main configuration",
  "modules": {
    "claims": { "enabled": true },
    "power": { "enabled": true },
    "combat": { "enabled": true },
    "zones": { "enabled": true },
    "economy": { "enabled": false },
    "relations": { "enabled": true },
    "teleport": { "enabled": true }
  },
  "debug": false,
  "language": "en_US",
  "updates": {
    "enabled": true,
    "checkUrl": "https://api.github.com/repos/HyperSystemsDev/HyperFactions/releases/latest",
    "showChangelog": true
  }
}
```

**Benefits:**
- Easier navigation - settings grouped by feature
- Modules can ship with default configs
- Disable module without losing its configuration
- Cleaner config management as plugin grows

**Implementation:**
- Each FactionModule implementation loads its own config file
- ConfigManager handles loading all configs
- Backward compatibility: If modules/ doesn't exist, fall back to monolithic config.json

**Priority:** P2 - Implement alongside modular architecture (Phase 3)

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

## Configuration Philosophy

> Design principles for HyperFactions configuration

### 1. Maximum Configurability, Minimal Complexity

**Principles:**
- Every game-affecting value should be configurable
- Config sections grouped by feature (power, claims, combat, etc.)
- Short variable names that are self-documenting
- Sensible defaults that work out-of-the-box

**Good Config Names:**
```json
{
  "power": {
    "max": 20,              // Not: "maximumPlayerPowerLevel"
    "start": 10,            // Not: "startingPowerAmount"
    "perClaim": 2,          // Not: "powerRequiredPerClaim"
    "deathPenalty": 1,      // Not: "powerLostOnDeath"
    "regenPerMin": 0.1      // Not: "powerRegenerationRatePerMinute"
  }
}
```

**Bad Config Names (too verbose):**
```json
{
  "powerSystemConfiguration": {
    "maximumPlayerPowerLevel": 20,
    "powerLostOnPlayerDeath": 1
  }
}
```

### 2. Feature Toggle Pattern

Every major feature should have an `enabled` flag:
```json
{
  "modules": {
    "power": { "enabled": true },
    "combat": { "enabled": true }
  }
}
```

### 3. Nested Structure for Related Settings

Group related settings under parent keys:
```json
{
  "teleport": {
    "warmup": 5,
    "cooldown": 300,
    "cancelOnMove": true,
    "cancelOnDamage": true
  }
}
```

Not flat:
```json
{
  "teleportWarmup": 5,
  "teleportCooldown": 300,
  "teleportCancelOnMove": true
}
```

### 4. Comments in Config Files

JSON doesn't support comments, so include a `_readme` field:
```json
{
  "_readme": "HyperFactions configuration. Docs: https://...",
  "power": { ... }
}
```

Or generate a separate `config-help.txt` with explanations.

### 5. Validation & Defaults

- All config values have defaults in code
- Invalid values log warnings and use defaults
- Config file regenerates with defaults if missing/corrupt

### 6. Units in Variable Names

Be explicit about units:
```json
{
  "warmupSeconds": 30,     // Not: "warmup" (ambiguous)
  "cooldownMinutes": 5,    // Not: "cooldown"
  "radiusChunks": 3,       // Not: "radius"
  "maxMembers": 50         // Count - no unit needed
}
```

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
- [x] Block place in enemy territory - verify blocked (**Implemented via ECS event system**)
- [x] Block break in enemy territory - verify blocked (**Implemented via ECS event system**)
- [x] Block interact in enemy territory - verify blocked (**Implemented via ECS event system**)
- [x] All actions in own territory - verify allowed (**Implemented**)
- [ ] ~~Place explosive devices near faction border~~ (N/A - Hytale doesn't have this mechanic yet)
- [ ] ~~Mechanical block movement across boundary~~ (N/A - Hytale doesn't have this mechanic yet)
- [ ] ~~Item transporter extraction from containers~~ (N/A - Hytale doesn't have this mechanic yet)

### Faction Settings
- [ ] `/f rename NewName` as leader - verify success
- [ ] `/f rename NewName` as member - verify denied
- [ ] `/f rename` with taken name - verify error
- [ ] `/f desc Some description` - verify updates visible in `/f info`
- [ ] `/f open` then `/f close` - verify toggles join permission
- [ ] `/f color a` - verify updates color display

### Stuck Command
- [ ] Use `/f unstuck` in own territory - verify "not stuck" message
- [ ] Use `/f unstuck` in enemy territory - verify 30s warmup starts
- [ ] Move during warmup - verify cancelled
- [ ] Wait full warmup - verify teleported to safe location
- [ ] Use while combat tagged - verify denied

### Ally/Enemy Caps
- [ ] Set `maxAllies: 2` in config, ally with 2 factions - verify success
- [ ] Try to ally with 3rd faction - verify "maximum allies" error
- [ ] Set `maxEnemies: 1`, declare 2 enemies - verify error on 2nd

### Relation System
- [x] Send ally request (existing functionality)
- [x] Accept ally request - verify both sides see correct logs (**Fixed - now stores requester UUID**)
- [ ] Break alliance - verify both sides updated
- [ ] Declare enemy - verify immediate effect
- [ ] Set neutral - verify relation cleared

---

## Changelog

### Version 0.2.0 (Current) - January 24, 2026
**New Features:**
- **Block Protection Events** - Wired up ECS event systems for PlaceBlockEvent, BreakBlockEvent, UseBlockEvent.Pre
- **Faction Settings Commands:**
  - `/f rename <name>` - Change faction name (Leader only)
  - `/f desc <text>` - Set faction description (Officer+)
  - `/f color <code>` - Set faction color using 0-9, a-f codes (Officer+)
  - `/f open` - Open faction to public joining (Leader only)
  - `/f close` - Close faction to invite-only (Leader only)
- **`/f unstuck` Command** - Escape from enemy territory with 30s warmup, finds nearest safe chunk
- **Ally/Enemy Caps** - Configurable limits on ally (default: 10) and enemy (-1 = unlimited) relations

**Configuration Additions:**
```json
{
  "relations": { "maxAllies": 10, "maxEnemies": -1 },
  "stuck": { "warmupSeconds": 30, "cooldownSeconds": 300 }
}
```

**Technical Notes:**
- Explosive device, mechanical block, and item transporter protection are NOT POSSIBLE in current Hytale API (these game mechanics don't exist yet)
- Block events use EntityEventSystem pattern (similar to entity-based event handling in other systems)

### Version 0.1.1 - January 24, 2026
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

### Version 0.3.0 (Current) - January 24, 2026
**Completed Features:**
- **Update Checker (2.9)** - GitHub Releases API integration with 5-minute caching, changelog support, and auto-download capability
- **Spawnkilling Prevention (2.10)** - Temporary invulnerability after PvP death respawn in own territory, configurable duration and break conditions
- **WarZone Per-Zone Configuration (3.0)** - 11 configurable flags per zone (PvP, item drop, block edit, power loss, etc.) with `/f admin zoneflag` command

**Still Planned:**
- **GUI System Overhaul (2-3 weeks):**
  - Complete GUI implementation for all features
  - `/f` command opens main menu (sub-commands still work)
  - Interactive territory map
  - Polished, fully functional interfaces
  - Visual permission matrix, economy management, war/raid tracking
- Warmup damage monitoring (fix teleport exploit)

**Major Features:**
- Public API for cross-mod integration
- Role-specific territory permissions (with GUI editor)
- **Comprehensive Economy System (with full GUI support):**
  - Faction treasury/bank with transaction history
  - Automated upkeep costs (base, per-claim, per-member)
  - Member taxation with role-based brackets
  - Raid declaration costs with win/loss incentives
  - War economics (declaration costs, daily upkeep, victory spoils, surrender penalties)
  - Visual treasury management, cost previews, and status tracking
- Raid system with cooldowns and economic integration (GUI for declaration/tracking)
- War declarations with objectives and economic warfare (GUI for status/negotiations)
- Territory decay for inactive factions

### Version 1.0.0 (Target)
- All Phase 1-3 critical features complete
- Full permission system with role-based territory permissions
- War declarations and raid mechanics
- Update checker operational with first release
- Stable for production use
- Comprehensive public API for economy and mod integrations

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
