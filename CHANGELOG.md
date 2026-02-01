# Changelog

All notable changes to HyperFactions will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

## [Unreleased]

### Added

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

### Fixed
- Codec key mismatch for zone name input (`@Name` vs `Name`)
- Reload button showing wrong command (`/f reload` not `/f admin reload`)
- Faction GUI map not updating on claim/unclaim operations
- In-game world map not updating when zones are created/updated/deleted
- GUI pages not refreshing properly (replaced `sendUpdate()` with new page instances)

### Changed
- Zone storage format updated to support multiple chunks per zone
- Admin zone page now uses tabbed filtering instead of separate pages

### Planned for Next Release
- Public API documentation (Phase 3.1)
- Role-specific territory permissions (Phase 3.2)
- Faction treasury/bank system (Phase 3.3)
