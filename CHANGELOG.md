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

### Planned
- Faction roster command with role display
- Territory map visualization (ASCII)
- Faction logs for tracking events
- Faction banks for shared resources
- Advanced territory tools (fill, circle, borders)
- War system with objectives
