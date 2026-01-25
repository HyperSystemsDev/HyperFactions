# Changelog

All notable changes to HyperFactions will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-01-24

### Added
- Initial release with comprehensive faction system
- Faction creation, management, and deletion
- Territory claiming with power mechanics
- Faction roles: LEADER, OFFICER, MEMBER with granular permissions
- Diplomatic relations: ALLY, NEUTRAL, ENEMY
- Combat tagging system to prevent logout during combat
- Territory protection and safe zones
- Power-based claim limits (power regenerates over time)
- 42 commands for faction management
- HyperPerms integration for permission checks
- Comprehensive API for integration

### Features
- **Faction Management**: Create factions, invite members, manage roles
- **Territory System**: Claim chunks, manage borders, protect builds
- **Power Mechanics**: Power-based claiming system with regeneration
- **Diplomacy**: Form alliances, declare enemies, manage relationships
- **Combat System**: Combat tagging, territory-based PvP rules
- **Roles & Permissions**: Hierarchical role system with customizable permissions

### Territory
- Chunk-based claiming system
- Power requirements for claims (1 power per chunk)
- Automatic unclaim when faction loses power
- Territory visualization and borders
- SafeZone and WarZone support

### Power System
- Players generate power (default: 10 max per player)
- Faction power = sum of member power
- Power regenerates over time
- Death causes power loss
- Claims protected as long as faction has sufficient power

### Diplomacy
- Ally relations (mutual agreement required)
- Enemy relations (hostile, allows territory conflict)
- Neutral default stance
- Relation-based access control

## [Unreleased]

### Added
- Item pickup protection in SafeZones and protected territories (Bug #3)
- Teleport warmup damage cancellation system (Phase 2.8)
- GitHub release update checker with automatic notifications (Phase 2.9)
- Spawnkilling prevention with temporary invulnerability (Phase 2.10)
- Per-zone flag configuration for WarZones and SafeZones (Phase 3.0)
- Public API expansion with EconomyAPI interface
- TeleportContext object to simplify teleport callbacks
- Auto-save system for data persistence (30-minute intervals)
- Invite cleanup task to remove expired invitations
- Faction claim reverse index for O(1) lookups

### Fixed
- Promotion logic error preventing officer promotions (Bug #1)
- Overclaim power check using wrong comparison operator (Bug #2)
- SafeZone item pickup exploit allowing item theft (Bug #3)
- Ally acceptance logging - both sides now get proper actor attribution
- Zone creation validation - cannot create zones on claimed chunks

### Changed
- Improved TeleportManager API with context object pattern
- Enhanced ClaimManager with reverse index for performance
- Updated periodic task comments for clarity

### Technical Improvements
- Repeating task scheduler for auto-save and periodic cleanup
- Spawn protection tracking in CombatTagManager
- Zone flags system with 11 configurable flags
- Economy foundation with FactionEconomy and EconomyManager

## [1.0.0] - 2026-01-24

### Phase 1 Completed
- `/f home` teleport with warmup/cooldown support
- `/f who <player>` - Full player lookup with faction info
- `/f power <player>` - Target player argument support
- Zone claim validation system
- Ally request proper actor attribution
- All critical bugs fixed (Bug #1, #2, #3)

### Phase 2 Completed Items
- 2.0: Block protection events wired up
- 2.1: Faction settings commands (`/f desc`, `/f open`, etc.)
- 2.5: `/f unstuck` command with SafeZone teleport
- 2.6: Ally/enemy caps configuration
- 2.8: Warmup damage monitoring
- 2.9: Update checker (GitHub releases)
- 2.10: Spawnkilling prevention

### Phase 3 Completed Items
- 3.0: WarZone per-zone configuration with flags

### Planned for Next Release
- GUI system overhaul (Phase 2.11)
- Public API documentation (Phase 3.1)
- Role-specific territory permissions (Phase 3.2)
- Faction treasury/bank system (Phase 3.3)
