# HyperFactions Development Roadmap

> Last Updated: January 25, 2026
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

### Completed (v1.0.0)

- [x] Core faction system (create, disband, invite, join, leave, kick)
- [x] Territory claiming with power mechanics
- [x] Diplomatic relations (ally, enemy, neutral)
- [x] Combat tagging system
- [x] SafeZone/WarZone system
- [x] Faction home teleportation
- [x] Basic GUI system with FactionPageRegistry, NavBarHelper
- [x] 27+ subcommands implemented
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

### In Progress

- [ ] Pitch/yaw orientation bug on `/f home` (TABLED)
- [ ] Admin Zones page back button positioning
- [ ] Territory map visual polish (wilderness contrast, player indicator elegance)

---

## Version Planning

### v1.0.0 (Current - Stabilization)
- Core faction system complete
- Basic GUI system working
- Production ready pending remaining fixes

### v1.1.0 - Command & GUI Overhaul
- Phase A: Command system refactor
- Phase B.1: New Player GUI
- Phase B.2: Enhanced Faction Player GUI
- Phase C.1: Chat help improvements

### v1.2.0 - Admin & Testing
- Phase B.3: Admin GUI
- Phase D: Testing infrastructure
- Phase C.2: Help GUI

### v1.3.0 - Modules
- Phase E.1: Treasury system
- Phase E.2: Role permissions
- Interactive chunk map (from Phase B)

### v1.4.0+ (Future)
- Phase E.3: Raid system
- Phase E.4: Faction levels
- Phase E.5: War system

---

## Reference Documentation

- [HytaleModding Community Docs](../resources/hytale-modding/content/docs/en/)
- [CustomUI Guide](../resources/hytale-modding/content/docs/en/guides/plugin/ui.mdx)
- [UI Patterns](../resources/UI-PATTERNS.md)

---

## Contributing

For feature requests or bugs: https://github.com/HyperSystemsDev/HyperFactions/issues
