# Changelog

All notable changes to HyperFactions will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

*No changes yet*

## [0.2.0] - 2026-02-01

### Added

**Update System**
- GitHub releases update checker with HTTP caching
- Login notifications for admins when updates are available
- Per-player notification preferences (opt-out support)

**Permission System**
- Unified PermissionManager with chain-of-responsibility pattern
- Support for VaultUnlocked, HyperPerms, and LuckPerms providers
- `hyperfactions.use` now grants all user-level permissions for simpler setup
- Centralized Permissions.java with all permission node definitions
- Fallback behavior: admin perms require OP, user perms allow by default

**PvP Protection**
- PvPProtectionSystem to enforce faction/ally damage rules
- Respects `allyDamage` and `factionDamage` config settings
- Denial messages sent to attacker when PvP is blocked

**Chat Formatting**
- Faction tags in public chat with relation-based coloring
- Colors: green (same faction), pink (ally), red (enemy), gray (neutral)
- Configurable chat format string with placeholders
- ChatContext for thread-safe sender tracking

**GUI Improvements**
- Configurable nav bar title via `gui.title` in config.json
- Wider nav bar title area (120px → 160px) for full "HyperFactions" display

**Build System**
- BuildInfo.java auto-generation with version, Java version, and timestamp
- Centralized version management in build.gradle

**Multi-Chunk Zone System**
- Zones can now span multiple chunks (previously limited to single chunk)
- Zone chunk claiming/unclaiming via GUI and commands
- Zone changes now trigger world map refresh for all players

**Admin Zone GUI**
- Zone list page with tab filtering (All/Safe/War)
- Interactive zone map page for visual chunk claiming/unclaiming
- Create zone wizard with optional initial chunk claim
- Zone entry display with chunk counts and edit/delete actions

**Zone Admin Commands**
- `/f admin zone create <name> <safe|war>` - create empty zone
- `/f admin zone claim <name>` - claim current chunk for zone
- `/f admin zone unclaim` - unclaim current chunk from its zone
- `/f admin zone remove <name>` - delete zone entirely
- `/f admin zone list [safe|war]` - list zones with optional filter
- `/f admin zone radius <name> <radius> [circle]` - claim chunks in radius

**Help System**
- Refactored help GUI with improved layout and organization

**Members Page Overhaul**
- Expandable member entries following AdminUI pattern (click to expand/collapse)
- Sort members by role (Leader → Officer → Member) or last online time
- Action buttons with text labels: PROMOTE, DEMOTE, KICK, MAKE LEADER
- Expanded view shows power (with color coding), joined date, and last death (relative format)
- Transfer leadership now shows confirmation modal before executing

**Faction Permissions System**
- New FactionPermissions data model with 11 boolean flags
- Territory access control: break/place/interact permissions for outsiders, allies, and members
- PvP toggle for faction territory
- Officers can edit permissions toggle (leader-only setting)
- Tabbed settings page: General | Permissions | Members tabs
- Server-side permission locks to enforce server-wide rules

**Admin Improvements**
- Admin disband now shows confirmation modal before executing
- Admin faction list properly refreshes after disbanding a faction

**Commands**
- `/f sync` - Admin command to merge disk data with in-memory faction data (timestamp-based)

### Fixed
- Nav bar selector crash when opening GUI (use element ID instead of type selector)
- Description text wiped when toggling recruitment in create faction wizard
- Ally PvP protection not enforced (PvPProtectionSystem was missing)
- Codec key mismatch for zone name input (`@Name` vs `Name`)
- Reload button showing wrong command (`/f reload` not `/f admin reload`)
- Faction GUI map not updating on claim/unclaim operations
- In-game world map not updating when zones are created/updated/deleted
- GUI pages not refreshing properly (replaced `sendUpdate()` with new page instances)
- Navigation from members page now works (FactionMembersData implements NavAwareData)
- Online member count in settings now shows actual count instead of "?"

### Changed
- Shadow plugin updated from 8.3.5 to 9.3.1 (fixes BuildInfo generation)
- Zone storage format updated to support multiple chunks per zone
- Admin zone page now uses tabbed filtering instead of separate pages

## [0.1.0] - 2026-01-30

### Added

**GUI System (Phase 2.11)**
- Main menu GUI accessible via `/f` command
- Faction dashboard with stats, quick actions, and navigation
- Interactive territory map with mouse-based chunk selection
- Faction settings page (rename, tag, description, color, recruitment)
- Relations management page with ally/enemy requests
- Member management with role changes and kick functionality
- New player flow: browse factions, create faction wizard, view invites
- Reusable modal components (color picker, input fields, confirmations)
- Navigation bar system with back button support
- Logs viewer for faction activity history

**Core Features**
- GitHub release update checker with automatic notifications (Phase 2.9)
- Spawnkill prevention with configurable invulnerability period (Phase 2.10)
- Per-zone flag configuration for WarZones/SafeZones (Phase 3.0)
- Item pickup protection in SafeZones and protected territories
- Teleport warmup damage cancellation system (Phase 2.8)
- World map overlay system with claim visualization
- Banner notifications for territory entry/exit
- Public API expansion with EconomyAPI interface
- Join request system for closed factions
- ChatManager for faction/ally chat channels

**Faction System**
- Faction creation, management, and deletion
- Territory claiming with power mechanics
- Faction roles: LEADER, OFFICER, MEMBER with granular permissions
- Diplomatic relations: ALLY, NEUTRAL, ENEMY
- Combat tagging system to prevent logout during combat
- Territory protection and safe zones
- Power-based claim limits (power regenerates over time)
- 42 commands for faction management
- HyperPerms integration for permission checks

**Testing Infrastructure**
- Unit tests for core data classes (ChunkKey, Faction, CombatTag, PlayerPower)
- Manager tests (ClaimManager, CombatTagManager, PowerManager, RelationManager)
- Protection system tests (ProtectionChecker)
- Test utilities: MockStorage, TestFactionFactory, TestPlayerFactory

**Technical Improvements**
- TeleportContext object for simplified teleport callbacks
- Auto-save system (30-minute intervals)
- Invite cleanup task for expired invitations
- Faction claim reverse index for O(1) lookups
- Zone flags system with 11 configurable flags
- Economy foundation with FactionEconomy and EconomyManager
- Territory ticking system for periodic updates

### Fixed
- Crash bug from improper UI element handling
- Help formatting standardized to match HyperPerms style
- `/f home` command now provides proper user feedback
- Promotion logic error preventing officer promotions
- Overclaim power check using wrong comparison operator
- SafeZone item pickup exploit allowing item theft
- Ally acceptance logging - both sides now get proper actor attribution
- Zone creation validation - cannot create zones on claimed chunks
- GUI navigation stability with nav bar fixes
- Chat system improvements

### Changed
- Refactored all GUIs into organized package structure (admin/, faction/, newplayer/, shared/)
- Territory map redesigned with mouse-based interaction (replaced button navigation)
- Improved HyperPerms integration reliability
- Enhanced faction relations display with visual indicators
- Improved TeleportManager API with context object pattern
- Enhanced ClaimManager with reverse index for performance
