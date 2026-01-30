# HyperFactions Development Roadmap

> Last Updated: January 27, 2026
> Current Version: 1.0.0 (dev/phase1 branch)

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

### Codebase Statistics (as of 2026-01-27)

| Metric | Count |
|--------|-------|
| Total Java files | 140 |
| GUI page classes | 31 |
| GUI data classes | 27 |
| Manager classes | 13 |
| Subcommands | 39+ |
| Test classes | 12 |

### Completed (v1.0.0)

- [x] Core faction system (create, disband, invite, join, leave, kick)
- [x] Territory claiming with power mechanics
- [x] Diplomatic relations (ally, enemy, neutral)
- [x] Combat tagging system
- [x] SafeZone/WarZone system
- [x] Faction home teleportation
- [x] Basic GUI system with FactionPageRegistry, NavBarHelper
- [x] 39+ subcommands implemented (monolithic FactionCommand.java - 2,678 lines)
- [x] HyperPerms integration

### Completed (2026-01-25 Round 4)

- [x] Territory Map page - 29x17 interactive chunk grid with click-to-claim/unclaim
- [x] Relations page - sectioned layout (Allies, Enemies, Pending Requests)
- [x] Set Relation modal - faction search with instant ally/enemy/neutral actions
- [x] Settings page - full edit modals (name, tag, desc, color, recruitment), teleport with warmup, disband
- [x] Leader succession - automatic promotion when leader leaves (priority: role level, then tenure)
- [x] Chunk coordinate system - fixed to use Hytale 32-block chunks (was incorrectly using 16)
- [x] Data deletion on disband - verified JSON file deletion works correctly

**Technical Notes from Round 4:**
- cmd.set() only works for `.Text` and `.TextSpans` - crashes for `.Style.*`, `.Visible`, `.Background.Color`
- Hytale chunks are 32 blocks (shift 5), not 16 blocks (shift 4) like Minecraft
- shadowJar `minimize()` removes Gson inner classes needed at runtime - don't use
- Modal layouts need fixed heights on scrollable lists to prevent button overflow

### Completed (2026-01-26 GUI System)

- [x] **New Player GUI (B.1) - Complete** (6 pages)
  - Browse page with sorting/pagination
  - Create wizard (2-step: Step 1 name/tag/color, Step 2 recruitment/confirm)
  - Invites page for pending invitations
  - Read-only territory map
  - Help page
  - NewPlayerNavBarHelper + NewPlayerPageRegistry navigation
- [x] **Faction Player GUI (B.2) - Complete** (15 pages + 16 data classes)
  - Dashboard, Members, Browse, Map, Relations, Settings (8 main pages)
  - 6 modals (Rename, Tag, Description, Color, Recruitment, Disband/Leave confirms)
  - 2 utilities (ColorPicker, LogsViewer)
  - FactionModulesPage with "Coming Soon" placeholders
- [x] **Economy System (E.1) - Complete** (Phase E.1 implemented early)
  - EconomyManager (386 lines) with full transaction support
  - EconomyAPI interface for external access
  - FactionEconomy data model with 50-transaction history
  - 9 transaction types (DEPOSIT, WITHDRAW, TRANSFER, UPKEEP, TAX, WAR_COST, RAID_COST, SPOILS, ADMIN)
  - Currency formatting with configurable name/symbol

### Completed (Testing Infrastructure - Partial)

- [x] **Test utilities implemented:**
  - TestPlayerFactory.java - Player mocking
  - TestFactionFactory.java - Faction creation helpers
  - MockStorage.java - In-memory storage for tests
- [x] **Unit tests implemented:**
  - PlayerPowerTest.java
  - CombatTagTest.java
  - ChunkKeyTest.java
  - FactionTest.java
  - CombatTagManagerTest.java
  - RelationManagerTest.java
  - ClaimManagerTest.java
  - PowerManagerTest.java
  - ProtectionCheckerTest.java

### Completed (Help System - Partial)

- [x] **Text-based command help:**
  - HelpFormatter.java - Formatted help message builder
  - CommandHelp.java - Help entry record with section grouping
  - 42 commands across 7 sections in FactionCommand.java
- [x] **GUI help pages:**
  - HelpPage.java for new players (static template)

### Tabled

- [ ] Admin GUI (B.3) - Moved to v1.3+ (basic Main + Zone pages exist)
- [ ] Pitch/yaw orientation bug on `/f home`
- [ ] Admin Zones page back button positioning
- [ ] Territory map visual polish (wilderness contrast, player indicator elegance)

---

## Version Planning

### v1.0.0 (Current - Stabilization)
- Core faction system complete
- GUI system complete (New Player + Faction Player)
- Economy system complete (Phase E.1 done early)
- 12 unit tests passing
- Production ready pending remaining fixes

### v1.1.0 - Command Refactoring & Polish
- Phase A: Command system refactor (modular commands, improved validation)
  - Current: Monolithic FactionCommand.java (2,678 lines, 39+ subcommands)
  - Target: Modular handler architecture with CommandRegistry
- Gameplay mechanic refinement
- Phase C.1: Chat help improvements (HelpFormatter exists, needs expansion)
- *(B.1 New Player GUI and B.2 Faction Player GUI already complete)*
- *(E.1 Economy/Treasury already complete)*

### v1.2.0 - Testing & Help System
- Phase D: Testing infrastructure expansion
  - Current: 12 test files with mocks
  - Target: Integration tests, QA scripts, coverage reporting
- Phase C.2: Help GUI enhancements
  - Current: Basic HelpPage for new players
  - Target: Contextual help, command wiki, searchable help

### v1.3.0 - Admin & Modules
- Phase B.3: Admin GUI (deferred from v1.2)
  - Current: AdminMainPage + AdminZonePage (partial)
  - Target: Full AdminPageRegistry with 6 pages (Dashboard, Factions, Zones, Players, Config, Logs)
- Phase E.2: Role permissions (custom permissions per role)

### v1.4.0+ (Future)
- Phase E.3: Raid system (TransactionType.RAID_COST exists)
- Phase E.4: Faction levels (FactionModulesPage placeholder exists)
- Phase E.5: War system (TransactionType.WAR_COST exists)

---

## Manager Classes (13 Total)

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

---

## Reference Documentation

- [HytaleModding Community Docs](../resources/hytale-modding/content/docs/en/)
- [CustomUI Guide](../resources/hytale-modding/content/docs/en/guides/plugin/ui.mdx)
- [UI Patterns](../resources/docs/HYTALE-CUSTOMUI-REFERENCE.md)
- [PartyPro GUI Reference](../resources/PartyPro.md) - Advanced GUI patterns

---

## Contributing

For feature requests or bugs: https://github.com/HyperSystemsDev/HyperFactions/issues
