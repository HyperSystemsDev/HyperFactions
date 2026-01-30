# Phase B: GUI System Redesign

**Goal**: Create distinct, polished GUI experiences for each player state.

This documentation has been split into focused files for better navigation:

## Contents

| File | Section | Description |
|------|---------|-------------|
| [hyperui-framework.md](hyperui-framework.md) | B.0 | Design system: colors, typography, spacing, components, layouts |
| [new-player-gui.md](new-player-gui.md) | B.1 | GUI for players without a faction (Browse, Create, Invites, Help) |
| [faction-player-gui.md](faction-player-gui.md) | B.2 | GUI for faction members (Dashboard, Members, Map, Relations, Settings) |
| [admin-gui.md](admin-gui.md) | B.3 | Admin GUI for server administrators |
| [module-placeholders.md](module-placeholders.md) | B.4 | Pattern for "Coming Soon" module placeholders |
| [implementation-tasks.md](implementation-tasks.md) | B.5 | Implementation checklists and task tracking |

## Design Decisions Summary

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Wireframe Format** | ASCII mockups + detailed element descriptions | Async review capability |
| **New Player Focus** | Browse/Join first | Encourage discovering existing factions |
| **Create Wizard** | 2-step wizard (Step 1: name/tag/color, Step 2: recruitment/confirm) | Guided experience, validates before proceeding |
| **Browser Style** | Minimal cards with expand for details | Clean, scalable |
| **Dashboard Actions** | Home + Claim + Chat | Most commonly used actions |
| **Admin Switch** | Floating action button | Always visible, mobile-inspired |

## GUI Types

| Type | Target Audience | Access | Nav Bar |
|------|-----------------|--------|---------|
| **New Player** | Players not in a faction | `/f` (when no faction) | BROWSE \| CREATE \| INVITES \| HELP |
| **Faction Player** | Players in a faction | `/f` (when in faction) | DASHBOARD \| MEMBERS \| BROWSE \| MAP \| RELATIONS \| SETTINGS |
| **Admin** | Admins with `hyperfactions.admin` | `/f admin` | OVERVIEW \| FACTIONS \| ZONES \| PLAYERS \| CONFIG \| LOGS |

## Implementation Status

See [implementation-tasks.md](implementation-tasks.md) for detailed task tracking.

### Quick Status

| GUI | Status | Notes |
|-----|--------|-------|
| New Player GUI | **Complete** | Browse, Create (2-step wizard), Invites, Map, Help all implemented |
| Faction Player GUI | **Complete** | Dashboard, Members, Browse, Map, Relations, Settings, modals all implemented |
| Admin GUI | Tabled (v1.3+) | Basic Main + Zones pages exist; full implementation deferred |
