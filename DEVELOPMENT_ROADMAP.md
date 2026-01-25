# HyperFactions Development Roadmap

> Last Updated: January 25, 2026
> Current Version: 1.0.0 (dev/phase1 branch)

---

## Overview

HyperFactions is feature-complete for v1.0 release with core faction management, territory claiming, power mechanics, diplomacy, and protection systems. GUI system is being stabilized with several fixes completed.

---

## Current Focus: GUI Stabilization

### Completed

- [x] FactionPageRegistry - Central page registry
- [x] NavBarHelper - Shared navigation component with AdminUI pattern
- [x] FactionPageData - Unified event data class
- [x] Core pages refactored (Dashboard, Members, Browser)
- [x] Nav bar templates (active/inactive variants)
- [x] Nav bar navigation working (EventData pattern fix)
- [x] Duplicate dashboard buttons removed (only HOME, LEAVE/DISBAND remain)
- [x] Dashboard layout cleaned up (compact stats section)
- [x] Native Hytale back button (ESC bottom-left) on all pages
- [x] Map page crash fixed (removed invalid #BackBtn binding)
- [x] Relations page back button fixed (native ESC)
- [x] `/f` and `/f gui` consolidated to same entry point
- [x] Nav bar added to Map and Relations pages
- [x] Power values on Admin page rounded
- [x] Admin removed from main GUI nav bar (accessed via `/f admin` only)

### In Progress

**GUI Bugs:**
- [ ] Pitch/yaw orientation wrong on `/f home` - camera points wrong direction (TABLED - needs deeper investigation)
- [ ] Admin Zones page back button in wrong location

**Architecture Changes:**
- [ ] Build dedicated Admin GUI with own nav/pages
- [ ] Remove pagination buttons, implement lazy loading
- [ ] Interactive chunk map (replace text placeholder)

---

## Architecture: Separate Admin GUI

**Decision:** Remove ADMIN from main GUI nav bar. Create standalone admin GUI accessible only via `/f admin` command.

**Rationale:**
- Cleaner main GUI for regular players
- Admin tools grouped in dedicated interface
- Separate permission model
- Can expand admin features without cluttering main GUI

**Admin GUI Structure:**
- Own nav bar (FACTIONS, ZONES, CONFIG, LOGS)
- Own pages (AdminMainPage, AdminZonePage, etc.)
- Self-contained navigation flow
- Accessed via `/f admin` command only

**Main GUI Nav Bar (after changes):**
- DASHBOARD
- MEMBERS
- BROWSE
- MAP
- RELATIONS

---

## GUI Enhancements Planned

### Interactive Chunk Map
- **Priority:** P1 | **Status:** Not Started
- Replace text placeholder with actual territory map
- Visual grid showing claimed chunks around player
- Click-to-claim/unclaim functionality
- Primary interaction method (commands as fallback)
- Color coding: own faction, allies, enemies, wilderness, zones

### Lazy Loading for Lists
- **Priority:** P1 | **Status:** Not Started
- Remove prev/next pagination buttons
- Load data dynamically as user scrolls
- Affects: FactionBrowserPage, FactionMembersPage, FactionRelationsPage, Admin pages
- May revisit if performance becomes an issue

### Nav Bar on All Pages
- **Priority:** P1 | **Status:** Complete
- [x] Added nav bar to ChunkMapPage and FactionRelationsPage
- UI templates updated to DecoratedContainer pattern
- Java classes updated with NavBarHelper.setupBar() calls

---

## Known Issues

### Pitch/Yaw Bug (TABLED)
- `/f home` teleport has wrong camera orientation
- Pitch appears inverted or swapped
- Yaw may also be affected
- Already tried swapping order in Vector3f - didn't fix
- Likely issue in how home is saved or FactionHome record field mapping
- **Status:** Tabled for later investigation

---

## Completed Work

### Phase 1: Quick Wins (100%)
- All critical bug fixes resolved

### Phase 2: Core Enhancements (100%)
- GUI System Overhaul with AdminUI patterns
- NavBarHelper, FactionPageRegistry, unified FactionPageData
- Core pages refactored (Dashboard, Members, Browser)
- GUI crash fixes completed

### Technical Debt: All P0 items resolved

See [CHANGELOG.md](CHANGELOG.md) for complete list of implemented features.

---

## Phase 3: Major Features
**Status:** 6 items remaining

### 3.1 Public API for Cross-Mod Integration
- **Priority:** P1 | **Status:** Foundation Complete
- JavaDoc documentation for all public methods
- Usage examples and integration guide
- API versioning strategy

### 3.2 Role-Specific Territory Permissions
- **Priority:** P1 | **Status:** Not Started
- Configure which roles can build/break, use containers, invite players, manage claims
- Add `/f perms` command for role permission management

### 3.3 Faction Treasury/Bank System
- **Priority:** P1 | **Status:** Foundation Complete
- Transaction logging, permission-based access control
- Commands: `/f money`, `/f money deposit/withdraw`, `/f money log`

### 3.4 Raid System with Cooldowns
- **Priority:** P2 | **Status:** Not Started
- Structured raiding with objectives, cooldowns, and rewards
- 24-hour cooldown between raids on same faction

### 3.5 Territory Decay for Inactive Factions
- **Priority:** P2 | **Status:** Not Started
- Auto-unclaim territory from inactive factions (configurable)

### 3.6 Faction Levels/Progression
- **Priority:** P3 | **Status:** Not Started
- Earn XP through claiming, winning fights, completing objectives
- Unlock perks: increased claims, power bonuses, new features

### 3.7 War Declaration System
- **Priority:** P3 | **Status:** Not Started
- Formal war with objectives and victory conditions

---

## Phase 4: Future Vision

| Feature | Priority |
|---------|----------|
| Faction Shields/Offline Protection | P3 |
| Dynmap/BlueMap Integration | P3 |
| Faction Upgrades Shop | P3 |
| Custom Flags per Claim | P3 |

---

## Technical Notes

### CustomUI Constraints (from HytaleModding docs)

**What Works**:
- `cmd.append("path/to/template.ui")` - Load templates
- `cmd.set("#ElementId.Text", "value")` - Set text content
- `cmd.set("#ElementId.TextSpans", Message.raw("text"))` - Set formatted text
- `events.addEventBinding()` - Bind event handlers

**What Does NOT Work**:
- `cmd.set("#ElementId.Style", "...")` - **CRASHES** - Cannot set styles dynamically
- Setting complex properties from Java code

**Solution**: Create separate template files for different states (e.g., `nav_button.ui` vs `nav_button_active.ui`)

### Event Binding Pattern

**Correct pattern for nav buttons:**
```java
EventData.of("Button", "Nav").append("NavBar", entry.id())
```

**Key**: Must set `button` field to non-null value, otherwise handler null-check returns early before processing navBar field.

### Native Back Button

Use `$C.@BackButton {}` at end of UI template for native Hytale back button (ESC, bottom-left). Do NOT bind custom events to it - Hytale handles dismissal automatically.

### Not Possible (Hytale Limitations)
- Explosion Protection - No explosive devices in Hytale yet
- Mechanical Block Protection - No block movement mechanics
- Item Transporter Protection - No automated transport systems

---

## Version Planning

**v1.0.0** (Current - In Development)
- Core faction system complete
- GUI system stabilization in progress
- Production ready pending remaining GUI fixes

**v1.1.0** (Target: After v1.0.0 stable)
- Interactive chunk map
- Separate Admin GUI
- Lazy loading for lists
- API documentation (Phase 3.1)
- Role permissions (Phase 3.2)

**v1.2.0**
- Treasury/bank system (Phase 3.3)
- Territory decay (Phase 3.5)

**v1.3.0**
- Raid system (Phase 3.4)
- Faction levels (Phase 3.6)
- War declarations (Phase 3.7)

---

## Reference Documentation

- [HytaleModding Community Docs](../resources/hytale-modding/content/docs/en/) - Official community modding guides
- [CustomUI Guide](../resources/hytale-modding/content/docs/en/guides/plugin/ui.mdx) - Comprehensive UI documentation
- [UI Patterns](../resources/UI-PATTERNS.md) - Patterns extracted from AdminUI mod
- [AdminUI Analysis](../resources/AdminUI.md) - Reference implementation analysis

---

## Contributing

For feature requests or bugs: https://github.com/HyperSystemsDev/HyperFactions/issues
