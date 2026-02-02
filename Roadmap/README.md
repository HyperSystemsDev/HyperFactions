# HyperFactions Development Roadmap

> Last Updated: February 1, 2026
> Current Version: 0.3.2 (released)

---

## Overview

HyperFactions is feature-complete for v1.0 release with core faction management, territory claiming, power mechanics, diplomacy, and protection systems. This roadmap outlines the next major development phases focusing on:

1. **Command System Overhaul** - Modular commands with GUI-first design
2. **User Experience by Player State** - Distinct GUIs for new players, faction members, and admins
3. **Help System & Documentation** - In-game wiki and contextual help
4. **Testing Infrastructure** - Automated and manual QA processes

---

## Roadmap Files

This roadmap is split into multiple files for easier collaboration and reduced merge conflicts:

| File | Description |
|------|-------------|
| **[README.md](README.md)** (this file) | Overview, Current State, Version Planning |
| **[phase-a-commands.md](phase-a-commands.md)** | Phase A: Command System Overhaul |
| **[phase-b-gui.md](phase-b-gui.md)** | Phase B: GUI System Redesign (HyperUI Framework, New Player, Faction Player, Admin GUIs) |
| **[phase-c-help.md](phase-c-help.md)** | Phase C: Help System & Documentation |
| **[phase-d-testing.md](phase-d-testing.md)** | Phase D: Testing Infrastructure |
| **[phase-e-modules.md](phase-e-modules.md)** | Phase E: Optional Modules (Treasury, Raids, Levels, War) |
| **[research.md](research.md)** | Research items requiring investigation |
| **[known-issues.md](known-issues.md)** | Known bugs and issues |
| **[technical-reference.md](technical-reference.md)** | Technical constraints and patterns |

---

## Current State

### Codebase Statistics (as of 2026-02-01)

| Metric | Count |
|--------|-------|
| Total Java files | 222 |
| GUI page classes | 50+ |
| GUI data classes | 40+ |
| Admin page classes | 19 |
| Admin data classes | 17 |
| UI template files | 105 |
| Manager classes | 15 |
| Subcommands | 42+ |
| Test classes | 12 |

---

## Released Versions

### v0.3.2 (Current Release)

- [x] Core faction system (create, disband, invite, join, leave, kick)
- [x] Territory claiming with power mechanics
- [x] Diplomatic relations (ally, enemy, neutral)
- [x] Combat tagging system
- [x] SafeZone/WarZone system
- [x] Faction home teleportation
- [x] GUI system with FactionPageRegistry, NavBarHelper
- [x] 42+ subcommands implemented
- [x] HyperPerms integration

### v0.3.0 - v0.3.1 Releases

- [x] **Data Loss Prevention** (v0.3.0)
  - FactionManager validates loaded data before clearing caches
  - Storage classes report failed files with SEVERE level logging
  - RuntimeException on critical I/O failures instead of returning empty
- [x] **Permission Node Overhaul** (v0.3.0)
  - Restructured permission system with category wildcards
  - `hyperfactions.use` as base permission to access `/f` command
  - 8 permission categories (faction, member, territory, teleport, relation, chat, info, admin)
- [x] **Update System** (v0.3.0)
  - `/f admin update` command to download and install plugin updates
  - Release channel config option (stable/prerelease)
- [x] **Storage Race Condition Fix** (v0.3.1)
  - Concurrent writes no longer overwrite each other's temp files
- [x] **TextField Input Fix** (v0.3.1)
  - Fixed text input fields not accepting keyboard input in GUI modals
- [x] **Logging Cleanup** (v0.3.1)
  - Removed excessive debug logging that was spamming server console

### Earlier Completed Features

- [x] Territory Map page - 29x17 interactive chunk grid with click-to-claim/unclaim
- [x] Relations page - sectioned layout (Allies, Enemies, Pending Requests)
- [x] Set Relation modal - faction search with instant ally/enemy/neutral actions
- [x] Settings page - full edit modals (name, tag, desc, color, recruitment), teleport with warmup, disband
- [x] Leader succession - automatic promotion when leader leaves (priority: role level, then tenure)
- [x] Chunk coordinate system - fixed to use Hytale 32-block chunks (was incorrectly using 16)
- [x] Data deletion on disband - verified JSON file deletion works correctly
- [x] **New Player GUI (B.1) - Complete** (6 pages)
- [x] **Faction Player GUI (B.2) - Complete** (15 pages + 16 data classes)
- [x] **Economy System (E.1) - Complete** (Phase E.1 implemented early)

### Completed (Testing Infrastructure - Partial)

- [x] **Test utilities implemented:**
  - TestPlayerFactory.java - Player mocking
  - TestFactionFactory.java - Faction creation helpers
  - MockStorage.java - In-memory storage for tests
- [x] **Unit tests implemented:**
  - PlayerPowerTest.java, CombatTagTest.java, ChunkKeyTest.java, FactionTest.java
  - CombatTagManagerTest.java, RelationManagerTest.java, ClaimManagerTest.java
  - PowerManagerTest.java, ProtectionCheckerTest.java

### Completed (Help System - Partial)

- [x] **Text-based command help:**
  - HelpFormatter.java - Formatted help message builder
  - CommandHelp.java - Help entry record with section grouping
  - 42 commands across 7 sections in FactionCommand.java
- [x] **GUI help pages:**
  - HelpPage.java for new players (static template)

---

## v0.4.0 (In Development - Unreleased)

### Admin GUI System (B.3) - PARTIAL

Complete admin interface accessible via `/f admin` command with 19 page classes and 17 data classes.

**Implemented Pages:**
- [x] **Dashboard** - Server statistics overview, quick actions, alerts, recent admin actions
- [x] **Factions** - Browse all factions, expandable details, admin actions (edit, adjust power, manage members, disband)
- [x] **Faction Info** - Detailed faction view with settings, members, relations tabs
- [x] **Faction Settings** - Edit name, tag, description, color, recruitment (with admin mode prefix)
- [x] **Faction Members** - Manage faction members with promote/demote/kick/transfer actions
- [x] **Faction Relations** - View and manage faction diplomatic relations
- [x] **Zones** - List and manage SafeZones and WarZones with tab filtering
- [x] **Zone Map** - Visual chunk claiming/unclaiming interface
- [x] **Zone Settings** - Edit zone properties (name, flags)
- [x] **Create Zone Wizard** - Multi-step zone creation with radius claiming

**Coming Soon Pages (placeholders):**
- [ ] **Config** - Server configuration editing via GUI
- [ ] **Backups** - Backup management UI (backend complete)
- [ ] **Help** - Admin documentation and command reference
- [ ] **Updates** - Version info display
- [ ] **Logs** - Audit log viewing with filters

**Navigation System:**
- AdminNavBarHelper for consistent navigation
- AdminPageRegistry for page management
- Nav bar: `DASHBOARD | FACTIONS | ZONES | CONFIG | BACKUPS | HELP | UPDATES`

### Backup System - BACKEND COMPLETE

GFS (Grandfather-Father-Son) rotation backup system with hourly/daily/weekly/manual backups.

| Component | Description |
|-----------|-------------|
| `BackupManager.java` | GFS rotation logic, backup creation/restoration |
| `BackupMetadata.java` | Backup info with timestamps, type, description |
| `BackupType.java` | Enum: HOURLY, DAILY, WEEKLY, MANUAL |
| `AdminBackupsPage.java` | GUI placeholder for backup management |

**Commands:**
- `/f admin backup create [name]` - Create manual backup
- `/f admin backup list` - List available backups
- `/f admin backup restore <id>` - Restore from backup

### Importer System - COMPLETE

Full HyFactions migration support for servers switching from HyFactions to HyperFactions.

| Component | Description |
|-----------|-------------|
| `HyFactionsImporter.java` | Complete migration with 12 data model mappings |
| `ImportResult.java` | Import result tracking |

**Features:**
- Pre-import backup creation
- Thread-safe import process
- Default generation for missing data
- Detailed import logging

### Protection System Reorganization - COMPLETE

Reorganized protection code into logical subdirectories (21 protection-related files):

| Directory | Purpose |
|-----------|---------|
| `protection/zone/` | Zone-specific protection checks (ZoneDamageProtection, ZoneInteractionProtection) |
| `protection/damage/` | Damage type handlers (Fall, Environmental, Mob, Projectile, PvP) |
| `protection/ecs/` | ECS event systems (Block place/break/use, Item drop/pickup, Damage, PvP) |
| `protection/debug/` | Debug utilities (ProtectionTrace, PvPTrace) |

### Other v0.4.0 Changes

- [x] **Admin Mode for Modals** - Admins can edit faction settings without being a member
- [x] **Zone flag defaults fixes** - Proper default flag handling
- [x] **Logging improvements** - Converted verbose logs to debug level

---

## Tabled Issues

- [ ] Pitch/yaw orientation bug on `/f home`
- [ ] Territory map visual polish (wilderness contrast, player indicator elegance)

---

## Version Planning

### v0.3.2 (Current Release)
- Core faction system complete
- GUI system complete (New Player + Faction Player)
- Economy backend complete (Phase E.1 done early)
- Data safety improvements (v0.3.0-v0.3.1)
- 12 unit tests passing

### v0.4.0 (In Development)
- Phase B.3: Admin GUI (PARTIAL - see above)
  - Dashboard, Factions, Zones pages implemented
  - Config, Backups, Help, Updates, Logs coming soon
- Backup system (backend complete, GUI coming)
- Importer system (HyFactions migration complete)
- Protection system reorganization (complete)

### v0.5.0 - Command Refactoring
- Phase A: Command system refactor (modular commands, improved validation)
  - Current: Monolithic FactionCommand.java (2,678 lines, 42+ subcommands)
  - Target: Modular handler architecture with CommandRegistry
- Gameplay mechanic refinement
- Phase C.1: Chat help improvements (HelpFormatter exists, needs expansion)

### v0.6.0 - Testing & Help System
- Phase D: Testing infrastructure expansion
  - Current: 12 test files with mocks
  - Target: Integration tests, QA scripts, coverage reporting
- Phase C.2: Help GUI enhancements
  - Current: Basic HelpPage for new players
  - Target: Contextual help, command wiki, searchable help

### v1.0.0 - Production Release
- All major features complete and tested
- Admin GUI fully implemented
- Documentation complete
- Performance optimization
- Phase E.2: Role permissions (custom permissions per role)

### v1.1.0+ (Future)
- Phase E.3: Raid system (TransactionType.RAID_COST exists)
- Phase E.4: Faction levels (FactionModulesPage placeholder exists)
- Phase E.5: War system (TransactionType.WAR_COST exists)

---

## Manager Classes (15 Total)

| Manager | Purpose | Status |
|---------|---------|--------|
| FactionManager | Faction CRUD, membership | Complete |
| ClaimManager | Territory claims | Complete |
| PowerManager | Power mechanics | Complete |
| RelationManager | Diplomacy | Complete |
| CombatTagManager | Combat tagging | Complete |
| ZoneManager | SafeZone/WarZone | Complete |
| TeleportManager | Home teleportation | Complete |
| InviteManager | Faction invites | Complete |
| JoinRequestManager | Join requests | Complete |
| ChatManager | Faction chat | Complete |
| ConfirmationManager | Command confirmations | Complete |
| EconomyManager | Treasury system | Complete |
| GuiManager | UI coordination | Complete |
| BackupManager | GFS backup rotation | Complete |
| PermissionManager | Permission provider chain | Complete |

---

## Reference Documentation

### Internal Documentation
- [technical-reference.md](technical-reference.md) - CustomUI constraints, patterns, workarounds

### External Resources
- [HytaleModding Community Docs](../resources/hytale-modding/content/docs/en/)
- [CustomUI Guide](../resources/hytale-modding/content/docs/en/guides/plugin/ui.mdx)

### UI Resources
- `src/main/resources/HyperFactions/shared/styles.ui` - Centralized style definitions (colors, typography, components)
- [HYTALE-CUSTOMUI-REFERENCE.md](../resources/docs/HYTALE-CUSTOMUI-REFERENCE.md) - Complete CustomUI syntax reference
- [AdminUI/patterns.md](../resources/AdminUI.md) - Page registry patterns from reference mod

---

## Contributing

For feature requests or bugs: https://github.com/HyperSystemsDev/HyperFactions/issues
