# HyperFactions Development Roadmap

> Last Updated: January 24, 2026
> Current Version: 1.0.0 (dev/phase1 branch)
> Repository: https://github.com/HyperSystemsDev/HyperFactions

---

## Overview

HyperFactions is feature-complete for v1.0 release with core faction management, territory claiming, power mechanics, diplomacy, and protection systems. This roadmap outlines remaining enhancements and future features.

### Completed to Date
✅ **Phase 1**: All quick wins and critical bug fixes (100%)
✅ **Phase 2 (Partial)**: Core enhancements - 7/9 items complete (78%)
✅ **Phase 3 (Partial)**: Major features - 1/7 items complete (14%)
✅ **Technical Debt**: All P0 items resolved

See [CHANGELOG.md](CHANGELOG.md) for complete list of implemented features.

---

## Development Phases

### Phase 2: Core Enhancements (Remaining)
**Status:** 2 items remaining | **Timeline:** 3-4 weeks

#### 2.7 API Capability Research
- **Priority:** P2 (Nice to have)
- **Effort:** 1-2 days
- **Status:** Not Started

**Investigation Items:**
- Entity spawn control for mob prevention in claims
- Crop growth rate modification
- Fire spread control
- Weather/time control per chunk
- Potion effect application in territory

**Deliverable:** Technical document outlining capabilities and implementation approach for each feature.

---

#### 2.11 GUI System Overhaul & Command Restructure
- **Priority:** P0 (Critical for polish)
- **Effort:** 2-3 weeks
- **Status:** Not Started

**Goal:** Transform `/f` into a GUI-first experience while maintaining command compatibility.

**Changes:**
- `/f` alone opens main GUI menu
- All sub-commands still work (`/f info`, `/f list`, etc.)
- Full feature parity between GUI and commands
- Polished, bug-free interface

**Main GUI Sections:**
1. **My Faction** - Management, members, settings
2. **Faction List** - Browse, search, filter all factions
3. **Map & Claims** - Interactive territory viewer
4. **Relations** - Ally/enemy management
5. **Power Status** - Overview and statistics
6. **Teleports** - Faction home with warmup status
7. **Settings** - Personal preferences
8. **Help** - Tutorial and reference

**Implementation:**
- Refactor existing GUI code for modularity
- Add missing GUI screens (faction list, relations, map viewer)
- Implement main menu dispatcher
- Polish all screens with consistent styling
- Full testing across all features

**Estimated Breakdown:**
- GUI framework refactoring: 3 days
- Main menu + navigation: 2 days
- Faction management screens: 3 days
- Territory/claims interface: 4 days
- Relations GUI: 2 days
- Polish + bug fixes: 3-4 days

---

### Phase 3: Major Features
**Status:** 6 items remaining | **Timeline:** 8-12 weeks

#### 3.1 Public API for Cross-Mod Integration
- **Priority:** P1
- **Effort:** 1 week
- **Status:** Foundation Complete, Documentation Pending

**Completed:**
- Core API interfaces (`HyperFactionsAPI`, `EconomyAPI`)
- Economy foundation (`EconomyManager`, `FactionEconomy`)
- Event system for API consumers

**Remaining:**
- JavaDoc documentation for all public methods
- Usage examples and integration guide
- API versioning strategy
- Deprecation policy documentation

**Deliverable:** Published API documentation with integration examples.

---

#### 3.2 Role-Specific Territory Permissions
- **Priority:** P1
- **Effort:** 1-2 weeks
- **Status:** Not Started

**Feature:** Allow faction leaders to configure which roles can perform actions in territory.

**Configurable Permissions:**
- Build/break blocks
- Use containers (chests, furnaces, etc.)
- Use doors/buttons/levers
- Attack friendly mobs
- Invite players
- Manage claims
- Use faction home

**Implementation:**
- Extend `FactionRole` with permission sets
- Add `/f perms` command for role permission management
- Update protection system to check role permissions
- GUI integration for permission editor

**Storage:** Permissions stored per-faction in `factions.json`.

---

#### 3.3 Faction Treasury/Bank System
- **Priority:** P1
- **Effort:** 2 weeks
- **Status:** Foundation Complete

**Completed:**
- `EconomyManager` and `FactionEconomy` models
- Basic deposit/withdrawal structure
- Economy API interface

**Remaining:**
- Transaction logging system
- Permission-based access control (who can deposit/withdraw)
- Tax system for upkeep (optional daily/weekly costs)
- War costs (declaring war, claiming enemy land)
- Economy-based upgrades (claim limit increases, power bonuses)

**Commands:**
- `/f money` - View faction balance
- `/f money deposit <amount>` - Deposit from personal balance
- `/f money withdraw <amount>` - Withdraw (officers/leader only)
- `/f money log` - View recent transactions

**Integration:** Requires Vault or economy plugin (soft dependency).

---

#### 3.4 Raid System with Cooldowns
- **Priority:** P2
- **Effort:** 2-3 weeks
- **Status:** Not Started

**Feature:** Structured raiding system with objectives, cooldowns, and rewards.

**Mechanics:**
- **Raid Declaration:** `/f raid <faction>` (requires power advantage)
- **Raid Duration:** 2-hour window, cooldown prevents spam
- **Objectives:**
  - Breach walls (break blocks in enemy territory)
  - Steal resources (if treasury implemented)
  - Destroy faction home
- **Rewards:** Power boost, economy rewards, claim access
- **Cooldowns:** 24-hour cooldown between raids on same faction

**Protection:**
- Defenders notified when raid starts
- Raid window limited to specific hours (configurable)
- Can't raid offline factions (requires min % members online)

**Integration:** Works with Phase 3.3 (economy) for resource theft.

---

#### 3.5 Territory Decay for Inactive Factions
- **Priority:** P2
- **Effort:** 3-4 days
- **Status:** Not Started

**Feature:** Automatically unclaim territory from inactive factions to free up land.

**Mechanics:**
- Faction marked inactive after X days with no member logins (configurable, default: 30 days)
- After grace period, claims begin decaying (1 chunk per day)
- Faction power frozen during inactivity
- Full faction deletion after additional period (configurable, default: 60 days)

**Notifications:**
- Warning messages to faction members before decay starts
- Server announcements when major factions decay

**Configuration:**
```json
{
  "decay": {
    "enabled": true,
    "inactivityDays": 30,
    "gracePeriodDays": 7,
    "decayRatePerDay": 1,
    "deletionDays": 60
  }
}
```

---

#### 3.6 Faction Levels/Progression
- **Priority:** P3 (Future)
- **Effort:** 1-2 weeks
- **Status:** Not Started

**Feature:** Faction leveling system with unlockable perks.

**Level Progression:**
- Earn XP through: claiming land, winning fights, completing objectives
- Levels unlock: increased claim limits, power bonuses, new features

**Example Perks:**
- Level 5: +10 max claims
- Level 10: Faction banner/emblem
- Level 15: Teleport to ally territories
- Level 20: War immunity period
- Level 25: Custom zone creation

**Commands:**
- `/f level` - View current level and progress
- `/f perks` - View unlocked perks

---

#### 3.7 War Declaration System
- **Priority:** P3 (Future)
- **Effort:** 2 weeks
- **Status:** Not Started

**Feature:** Formal war system with objectives and victory conditions.

**Mechanics:**
- **Declaration:** `/f war declare <faction>` (requires mutual agreement or council vote)
- **Duration:** Wars last fixed period (e.g., 1 week)
- **Objectives:**
  - Control territory: Capture % of enemy land
  - Defeat enemies: Kill enemy members
  - Siege: Hold enemy faction home for duration
- **Victory Rewards:** Power boost, territory transfer, economy rewards
- **Peace Treaties:** Negotiate terms to end war early

**Wartime Rules:**
- PvP always enabled between warring factions (even in SafeZones)
- Claim costs reduced against war enemies
- Death power penalties increased
- Allies can join wars as coalition

---

### Phase 4: Future Vision
**Status:** Concept Phase | **Timeline:** TBD

#### 4.1 Faction Shields/Offline Protection
- **Priority:** P3
- **Effort:** 1 week

**Feature:** Temporary invulnerability shields for offline protection.

**Mechanics:**
- Shield activates when all members offline
- Blocks enemy claiming/raiding while active
- Limited duration or uses per week
- Can be bypassed during declared wars

---

#### 4.2 Dynmap/BlueMap Integration
- **Priority:** P3
- **Effort:** 3-4 days

**Feature:** Territory visualization on web-based maps.

**Integration:**
- Faction claims shown with borders
- Color-coded by faction/relation
- Click claims for faction info
- Real-time updates

---

#### 4.3 Faction Upgrades Shop
- **Priority:** P3
- **Effort:** 1-2 weeks

**Feature:** Purchasable upgrades using faction treasury.

**Example Upgrades:**
- Claim limit increases
- Power regeneration boost
- Teleport cooldown reduction
- Combat tag duration reduction
- Spawn protection duration increase

---

#### 4.4 Custom Flags per Claim
- **Priority:** P3
- **Effort:** 3-4 days

**Feature:** Per-chunk flag customization (extension of 3.0).

**Examples:**
- Disable mob spawning in specific claims
- Allow friendly fire in training grounds
- Create no-build zones for decoration
- Designate farming/resource claims

---

## Technical Notes

### Not Possible (Hytale Limitations)

The following features cannot be implemented due to Hytale API limitations:

❌ **2.2 Explosion Protection** - No explosive devices in Hytale yet
❌ **2.3 Mechanical Block Protection** - No block movement mechanics
❌ **2.4 Item Transporter Protection** - No automated transport systems

These may become possible if Hytale adds these mechanics in future updates.

---

## Timeline Summary

### Immediate Priorities (Next 4-6 weeks)
1. **Phase 2.11**: GUI System Overhaul (2-3 weeks) - P0
2. **Phase 3.1**: API Documentation (1 week) - P1
3. **Phase 3.2**: Role Permissions (1-2 weeks) - P1

### Short Term (2-3 months)
4. **Phase 3.3**: Treasury/Bank System (2 weeks) - P1
5. **Phase 2.7**: API Research (1-2 days) - P2
6. **Phase 3.5**: Territory Decay (3-4 days) - P2

### Medium Term (3-6 months)
7. **Phase 3.4**: Raid System (2-3 weeks) - P2
8. **Phase 3.6**: Faction Levels (1-2 weeks) - P3
9. **Phase 3.7**: War System (2 weeks) - P3

### Long Term (6+ months)
10. **Phase 4.x**: Future vision features as needed

---

## Configuration Philosophy

All new features follow these principles:

1. **Configurable by Default** - Every feature has enable/disable toggle
2. **Sane Defaults** - Works out of box without configuration
3. **Modular** - Features can be independently enabled/disabled
4. **Backwards Compatible** - Config changes don't break existing setups
5. **Hot-Reload** - Changes via `/f admin reload` when possible

---

## Testing Requirements

Each feature must include:

✅ Unit tests for core logic
✅ Integration tests for cross-system interactions
✅ Manual test checklist
✅ Performance benchmarks (if applicable)
✅ Migration path from previous versions

---

## Version Planning

**v1.0.0** (Current - dev/phase1)
- Core faction system complete
- All critical bugs fixed
- Production ready

**v1.1.0** (Target: 4-6 weeks)
- GUI system overhaul (Phase 2.11)
- API documentation complete (Phase 3.1)
- Role permissions (Phase 3.2)

**v1.2.0** (Target: 2-3 months)
- Treasury/bank system (Phase 3.3)
- Territory decay (Phase 3.5)

**v1.3.0** (Target: 3-6 months)
- Raid system (Phase 3.4)
- Faction levels (Phase 3.6)
- War declarations (Phase 3.7)

**v2.0.0** (TBD)
- Major architectural changes (if needed)
- Breaking API changes
- Phase 4 features

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines and how to submit features.

For feature requests or bugs, open an issue at: https://github.com/HyperSystemsDev/HyperFactions/issues
