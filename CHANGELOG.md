# Changelog

All notable changes to HyperFactions will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

*No changes yet*

## [0.5.2] - 2026-02-03

### Fixed

**Teleport System Overhaul**
- Fixed warmup teleports not executing (countdown messages worked but teleport never happened)
  - Root cause: Teleport component must be added via `targetWorld.execute()` on the destination world's thread
  - Changed from `new Teleport()` to `Teleport.createForPlayer()` for proper player head/body rotation setup
  - Fixed in all 7 teleport locations: TerritoryTickingSystem, HomeSubCommand, FactionSettingsPage, FactionSettingsTabsPage, FactionDashboardPage, FactionMainPage, AdminFactionsPage

**Message Formatting**
- Fixed garbled chat messages showing `Ã?Â§b[HyperFactions]Ã?Â§r` instead of proper colors
  - TeleportManager was using legacy `\u00A7` color codes which `Message.raw()` doesn't parse
  - Changed to proper `Message.raw(text).color(hexColor)` pattern

**Client Crash on /f power**
- Fixed `/f power` and `/f who` commands crashing the client
  - Commands referenced non-existent UI templates (`player_info.ui`)
  - Disabled GUI mode, now falls back to text mode until templates are created

### Added

**Teleport Countdown Messages**
- Warmup teleports now show incremental countdown messages
  - High warmup (30+ seconds): announces at 30, 15, then every second from 10 down
  - Low warmup (under 10 seconds): announces every second
  - Example: "Teleporting in 30 seconds...", "Teleporting in 10 seconds...", etc.

### Changed

- Removed unused `TeleportContext.java` class (replaced by `TeleportManager.TeleportDestination`)
- Refactored `TeleportManager.PendingTeleport` from record to class to support countdown state tracking
- `/f stuck` now uses generic `scheduleTeleport()` method instead of faction-specific teleport logic

## [0.5.1] - 2026-02-02

### Fixed

**Debug Toggle Persistence**
- Fixed debug categories not staying disabled after server restart
- Root cause: `applyToLogger()` was using `enabledByDefault || category` logic which re-enabled categories on load
- Individual category settings now take direct precedence over enabledByDefault
- `enableAll()` and `disableAll()` now properly clear the enabledByDefault flag

**Debug Config Defaults**
- All debug categories now correctly default to `false` on first load
- Fixed `loadModuleSettings()` using field values as defaults instead of explicit `false`

### Added

**World Map Debug Category**
- New `worldmap` debug category separates verbose map generation logs from territory notifications
- Use `/f admin debug toggle worldmap on` to enable map tile generation logging
- Use `/f admin debug toggle territory on` for territory entry/exit notifications only
- Significantly reduces console spam when debugging territory features

### Changed

- Territory debug now only logs chunk entry/exit notifications
- World map debug logs all map generation, tile updates, and claim rendering

## [0.5.0] - 2026-02-02

### Added

**Death Power Loss System**
- Implemented ECS-based `PlayerDeathSystem` to detect player deaths via `DeathComponent`
- Power penalty now correctly applied when players die (was previously orphaned code)
- Uses Hytale's native ECS pattern (`RefChangeSystem<EntityStore, DeathComponent>`)

**Respawn Handling System**
- Implemented ECS-based `PlayerRespawnSystem` to detect respawns via `DeathComponent` removal
- Combat tag automatically cleared on respawn
- Spawn protection applied at respawn location (configurable duration)

**Claim Decay System**
- New automatic claim decay for inactive factions
- If ALL faction members are offline longer than `decayDaysInactive` (default: 30 days), all claims are removed
- Decay runs hourly via scheduled task
- Admin commands for decay management:
  - `/f admin decay` - Show decay system status
  - `/f admin decay run` - Manually trigger decay check
  - `/f admin decay check <faction>` - Check specific faction's decay status

**Debug Toggle Persistence**
- Implemented `/f admin debug toggle <category> [on|off]` command
- Debug category changes now persist to `config/debug.json` across server restarts
- `/f admin debug toggle` shows current status of all 6 categories
- `/f admin debug toggle all` enables/disables all categories at once
- `/f admin debug status` now shows debug logging status alongside data counts

**Zone Rename Modal**
- Admin zone rename UI accessible from AdminZonePage
- New ZoneRenameModalPage and ZoneRenameModalData classes
- Zone name input with validation and immediate save

### Changed

**Config System Restructure**
- Migrated all 31 files from deprecated `HyperFactionsConfig.get()` to `ConfigManager.get()`
- New modular config architecture with `ConfigFile`, `ModuleConfig`, and `ConfigManager`
- Added validation support with auto-correction for invalid config values
- `HyperFactionsConfig` facade retained for backward compatibility (marked deprecated)
- Config modules now support individual save/reload operations

**Command Architecture Refactor**
- Split monolithic FactionCommand.java (3500+ lines) into 40+ individual subcommand files
- New `FactionSubCommand` base class with shared functionality and permission checks
- Commands organized by category:
  - `command/admin/` - Admin subcommands (AdminSubCommand handles all /f admin *)
  - `command/faction/` - Create, Disband, Rename, Desc, Color, Open, Close
  - `command/info/` - Help, Info, List, Map, Members, Power, Who
  - `command/member/` - Accept, Demote, Invite, Kick, Leave, Promote, Transfer
  - `command/relation/` - Ally, Enemy, Neutral, Relations
  - `command/social/` - Chat, Invites, Request
  - `command/teleport/` - Home, SetHome
  - `command/territory/` - Claim, Overclaim, Stuck, Unclaim
  - `command/ui/` - Gui, Settings
  - `command/util/` - CommandUtil shared utilities
- Added `/hyperfactions` as additional command alias

### Fixed

**Power Debug Logging**
- Fixed `PlayerListener.onPlayerDeath()` using wrong logger category (`Logger.debug` → `Logger.debugPower`)

## [0.4.3] - 2026-02-02

### Fixed

**In-Game World Map Not Showing Claims**
- Fixed world map claim overlays not appearing on production servers
- Root cause: `setWorldMapProvider()` only affects future world loads, not the live WorldMapManager
- Now calling `setGenerator()` directly on WorldMapManager to properly register our claim renderer
- Added auto-recovery if another mod overwrites the generator during runtime

## [0.4.2] - 2026-02-02

### Fixed

**Admin Debug Commands**
- Fixed debug commands to be under `/f admin debug` instead of `/f debug`
- Debug subcommands: `power`, `combat`, `claim`, `zone`, `protection`

**Admin Unclaim All GUI**
- Fixed "unclaim all" in admin factions menu not updating faction's claim count
- GUI map now correctly shows 0 claims after unclaiming all territory
- Faction record is now properly updated when bulk unclaiming

### Changed

**Help GUI Overhaul**
- Added command syntax legend at top explaining `<required>` vs `[optional]` notation
- Restructured command reference with descriptions on separate indented lines
- Added new description line template for cleaner visual hierarchy
- Corrected all command syntax to match actual code:
  - `/f admin zone create <name> <safe|war>` (was `<type>`)
  - `/f admin zone radius <name> <radius> [circle|square]` (was `<r> [shape]`)
  - `/f admin zone info [name]` (was missing entirely)
- Removed non-existent `/f admin bypass` command from help
- Removed non-functional `--text` flag references from all help content

## [0.4.1] - 2026-02-02

### Fixed

**Combat Tagging Restored**
- Fixed combat tagging not working after protection system refactor to ECS-based handlers
  - PvP combat now properly tags both attacker and defender
  - PvE combat (mob damage) now properly tags the player being attacked
- Added configurable `logoutPowerLoss` setting for combat logout penalty (default: 1.0)
  - Separate from normal death penalty for finer control
  - Set to 0 to disable combat logout power loss while keeping other penalties

**HyFactions Import Map Display**
- Fixed in-game world map not showing imported claims and zones after import
- Fixed GUI territory map not displaying imported faction claims
- Import now rebuilds claim index and refreshes world maps after completion

## [0.4.0] - 2026-02-01

### Added

**Admin GUI System**
- Complete admin interface accessible via `/f admin` command
- Dashboard page with server statistics overview
- Factions management: browse all factions, view details, edit settings
- Zone management: create, configure, and delete zones with visual map
- Configuration page for runtime settings adjustment
- Backups management page (placeholder for future functionality)
- Help page with command reference
- Updates page for version information
- Navigation bar for consistent page switching
- Admin faction settings now include both general settings and permissions

**Admin Mode for Modals**
- Admins can now edit faction settings (name, tag, description, color, recruitment) without being a member
- All admin actions are prefixed with `[Admin]` in chat messages

**Faction Dashboard Redesign**
- New admin-style info blocks with 6 key statistics:
  - Power (current/max with percentage)
  - Claims (used/max with available count)
  - Members (total with online count)
  - Relations (ally/enemy counts)
  - Status (Open/Invite Only indicator)
  - Invites (sent invites and join requests count)

**Leader Leave Flow**
- New leader leave confirmation page with succession information
- Shows who will become the new leader (highest officer, then most senior member)
- If no successor available, offers faction disband option
- Automatic leadership transfer on leader departure

**Browser Page Improvements**
- Both faction browsers now use expandable IndexCards pattern (matching admin pages)
- Expandable entries with faction details and action buttons
- Improved search and sort functionality

**Zone Import Improvements**
- `ZoneFlags.getDefaultFlags()` helper method for importing zones
- Zones imported from mods without flag systems now get proper defaults
- Import validation report for HyFactions importer

### Changed

**Protection System Reorganization**
- Reorganized protection code into logical subdirectories:
  - `protection/zone/` - Zone-specific protection checks
  - `protection/damage/` - Damage type handlers
  - `protection/ecs/` - ECS event systems
  - `protection/debug/` - Debug utilities
- Moved SpawnProtection and ProtectionListener into protection package

**Logging Improvements**
- Converted verbose zone lookup logs to debug level
- Converted GUI build/event logs to debug level
- Converted world map provider logs to debug level

**Nav Bar Role-Based Filtering**
- Invites button now only visible to officers and leaders
- FactionPageRegistry now supports `minimumRole` for page visibility
- NavBarHelper updated to filter buttons based on viewer's role

### Fixed

**Search Not Working**
- Fixed search functionality in faction browser and new player browser
- Codec key mismatch (`SearchQuery` vs `@SearchQuery`) now resolved
- Search input values now correctly passed to event handlers

**Sort Buttons Breaking Navigation**
- Fixed sort buttons causing nav bar to disappear
- Implemented proper `rebuildList()` pattern instead of full page rebuild

**Leader Cannot Leave**
- Leaders can now properly leave their faction
- Leadership is automatically transferred to the best successor
- Fixed `transferLeadership` parameter order bug (newLeader, actorUuid)

**CustomUI Visible Property**
- Fixed `.Visible` property using string instead of boolean
- Changed `cmd.set("#Element.Visible", "true")` to `cmd.set("#Element.Visible", true)`

**New Player Map Relation Indicators**
- New player map no longer shows ally/enemy indicators
- Players not in a faction no longer see relation-based colors

## [0.3.1] - 2026-02-01

### Fixed

**Storage Race Condition**
- Fixed checksum verification failures when saving factions rapidly
  - Concurrent writes no longer overwrite each other's temp files
  - Each atomic write now uses a unique temp file name

**TextField Input**
- Fixed text input fields not accepting keyboard input in GUI modals
  - Faction name input (create wizard step 1)
  - Description input (create wizard step 2)
  - Rename modal, tag modal, description modal
  - Zone creation wizard name input
  - Relation search input

**Logging Cleanup**
- Removed excessive debug logging that was spamming server console
  - World map generation no longer logs every chunk render
  - Territory notifications converted to debug category
  - GUI build/event logs removed or converted to debug
  - All debug categories remain disabled by default

## [0.3.0] - 2026-02-01

### Fixed

**CRITICAL: Data Loss Prevention**
- Fixed faction data loss on update/reload when deserialization fails
  - FactionManager now validates loaded data before clearing caches
  - If loading returns empty but data existed, keeps in-memory data safe
  - Added `.exceptionally()` handlers to catch and log exceptions without data loss
- Fixed silent exception handling in all storage classes
  - JsonFactionStorage now reports all failed files with SEVERE level logging
  - JsonPlayerStorage now reports all failed files with SEVERE level logging
  - JsonZoneStorage now reports all failed zones with SEVERE level logging
  - Storage methods now throw RuntimeException on critical I/O failures instead of returning empty
- Added comprehensive loading validation
  - Detects when 0 items load from non-empty directories (corruption indicator)
  - Logs CRITICAL warnings when data appears to be missing
  - Reports total files vs successfully loaded files for debugging
- Fixed ZoneManager and PowerManager with same safety protections
  - Both managers now validate loading before clearing caches
  - Exception handlers prevent data loss on unexpected errors

**WarZone/SafeZone Protection**
- Fixed container protection in WarZones - chests, furnaces, and workbenches are now properly blocked
  - Previously only doors were blocked; now all non-door blocks are protected
  - Uses door-only detection: only blocks with "door" state or door/gate in block ID are allowed
  - All other block interactions (containers, processing benches, etc.) are blocked
- Fixed protection denial messages showing raw color codes (e.g., `§c`)
  - Messages now use clean text without legacy formatting codes

**Help System**
- Added backup and admin commands to help GUI
  - `/f admin backup create [name]`, `/f admin backup list`, etc.
  - `/f admin zone` and `/f admin update` now listed

**HyperPerms Integration**
- Fixed faction prefix display in HyperPerms chat formatting
  - Added missing `ReflectiveHyperFactionsProvider` implementation
  - Faction names now appear correctly in chat when using HyperPerms

### Added

**Update System**
- `/f admin update` command to download and install plugin updates
- Release channel config option (`releaseChannel`: "stable" or "prerelease")
- Pre-release support in update checker (uses /releases endpoint when enabled)

**Configuration**
- Config merge behavior: missing keys are added with defaults without overwriting user values
- `configNeedsSave` flag to only write config when new keys are added

### Migration Guide (from v0.1.0)

**Permission Node Changes**

If upgrading from v0.1.0, the permission system has been restructured. Individual permission nodes (e.g., `hyperfactions.create`, `hyperfactions.invite`) are now organized under category wildcards.

**Recommended Setup (HyperPerms commands):**

Grant full faction functionality to default group:
```
/hp group setperm default hyperfactions.use
/hp group setperm default hyperfactions.faction.*
/hp group setperm default hyperfactions.member.*
/hp group setperm default hyperfactions.territory.*
/hp group setperm default hyperfactions.teleport.*
/hp group setperm default hyperfactions.relation.*
/hp group setperm default hyperfactions.chat.*
/hp group setperm default hyperfactions.info.*
```

**Permission Categories:**

| Category | Description |
|----------|-------------|
| `hyperfactions.use` | **Required** - Base permission to use `/f` command |
| `hyperfactions.faction.*` | Create, disband, rename, tag, color, open/close |
| `hyperfactions.member.*` | Invite, join, leave, kick, promote, demote, transfer |
| `hyperfactions.territory.*` | Claim, unclaim, overclaim, map |
| `hyperfactions.teleport.*` | Home, sethome, stuck |
| `hyperfactions.relation.*` | Ally, enemy, neutral, view relations |
| `hyperfactions.chat.*` | Faction chat, ally chat |
| `hyperfactions.info.*` | Info, list, who, power, members, logs, help |

**Note:** `hyperfactions.use` is required as the base permission to access the `/f` command. Category permissions control specific functionality. Admin, bypass, and limit permissions require explicit grants.

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
