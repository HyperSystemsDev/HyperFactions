# Phase E: Optional Modules

> **Last Updated**: February 1, 2026

These are larger features planned for future versions. GUI placeholders added in FactionModulesPage.

---

## E.0 Core Infrastructure (Added v0.4.0)

### Backup System - BACKEND COMPLETE

GFS (Grandfather-Father-Son) rotation backup system with hourly/daily/weekly/manual backups.

| Component | File | Description |
|-----------|------|-------------|
| BackupManager | `backup/BackupManager.java` | GFS rotation, backup creation/restoration |
| BackupMetadata | `backup/BackupMetadata.java` | Backup info with timestamps |
| BackupType | `backup/BackupType.java` | Enum: HOURLY, DAILY, WEEKLY, MANUAL |
| AdminBackupsPage | `gui/page/admin/AdminBackupsPage.java` | GUI placeholder (coming soon) |

**Commands**:
- `/f admin backup create [name]` - Create manual backup
- `/f admin backup list` - List available backups
- `/f admin backup restore <id>` - Restore from backup

**GUI**: Admin Backups page coming in v0.4.0 release.

### Importer System - COMPLETE

Full HyFactions migration support for servers switching from HyFactions to HyperFactions.

| Component | File | Description |
|-----------|------|-------------|
| HyFactionsImporter | `importer/HyFactionsImporter.java` | Complete migration with data mapping |
| ImportResult | `importer/ImportResult.java` | Import result tracking |

**Features**:
- Pre-import backup creation for safety
- Thread-safe import process
- Default generation for missing data fields
- Detailed import logging with success/failure counts

---

## E.1 Faction Treasury/Bank System

- **Status**: **COMPLETE**
- **Implemented**: January 2026

### Implementation Summary

| Component | File | Lines |
|-----------|------|-------|
| EconomyManager | `manager/EconomyManager.java` | 386 |
| EconomyAPI | `api/EconomyAPI.java` | Interface |
| FactionEconomy | `data/FactionEconomy.java` | Record |
| TransactionType | `api/EconomyAPI.TransactionType` | Enum |

### Features Implemented

- [x] Faction balance tracking with currency formatting
- [x] Deposit/withdraw operations with async CompletableFuture support
- [x] System operations (systemDeposit, systemWithdraw)
- [x] Transaction history (up to 50 per faction with FIFO)
- [x] Transaction logging to faction activity log
- [x] Permission-based access (officer+ for withdrawals)
- [x] Transfer between factions
- [x] Configurable currency name, plural, and symbol

### Transaction Types

| Type | Description |
|------|-------------|
| DEPOSIT | Player deposits money |
| WITHDRAW | Player withdraws money |
| TRANSFER_IN | Received from another faction |
| TRANSFER_OUT | Sent to another faction |
| UPKEEP | Periodic faction upkeep cost |
| TAX_COLLECTION | Tax revenue from members |
| WAR_COST | War declaration fee |
| RAID_COST | Raid initiation fee |
| SPOILS | War/raid victory rewards |
| ADMIN_ADJUSTMENT | Admin balance modification |

### Pending Items (v1.2+)

- [ ] Treasury commands (`/f money`, `/f money deposit`, `/f money withdraw`, `/f money log`)
- [ ] Treasury GUI page in Modules section
- [ ] Tax system implementation (configurable % of member earnings)
- [ ] Upkeep system implementation

---

## E.2 Role-Specific Territory Permissions

- **Status**: Not started
- **Target**: v1.3.0
- **Features**:
  - Configure per-role: build, break, interact, container access
  - Guest permissions for non-members
  - Ally permissions
  - `/f perms` command and GUI page
- **GUI**: Permissions page in Settings section

---

## E.3 Raid System

- **Status**: Not started (TransactionType.RAID_COST exists for future use)
- **Target**: v1.4.0+
- **Features**:
  - Structured raid initiation
  - Objectives (core block, flag capture, etc.)
  - 24-hour cooldown between raids on same faction
  - Raid notifications
  - Victory rewards
- **GUI**: Raids page in Modules section ("Coming Soon" placeholder displayed)

---

## E.4 Faction Levels/Progression

- **Status**: Not started
- **Target**: v1.4.0+
- **Features**:
  - XP from: claiming, PvP victories, objectives
  - Level unlocks: increased claims, power bonuses, cosmetics
  - Leaderboard
- **GUI**: Levels page in Modules section ("Coming Soon" placeholder displayed)

---

## E.5 War Declaration System

- **Status**: Not started (TransactionType.WAR_COST exists for future use)
- **Target**: v1.4.0+
- **Features**:
  - Formal war declaration
  - War objectives and victory conditions
  - War end conditions (surrender, timer, objectives)
  - War statistics
- **GUI**: War page in Modules section ("Coming Soon" placeholder displayed)

---

## E.6 Implementation Tasks

### Treasury Commands & GUI (v1.2+)

- [x] ~~E.1.1 Implement FactionEconomy data model~~ **DONE**
- [x] ~~E.1.2 Implement EconomyManager~~ **DONE**
- [ ] **E.1.3** Add treasury commands (`/f money`, `/f money deposit`, `/f money withdraw`, `/f money log`)
- [ ] **E.1.4** Create Treasury GUI page
- [ ] **E.1.5** Implement tax collection system
- [ ] **E.1.6** Implement upkeep cost system

### Role Permissions (v1.3.0)

- [ ] **E.2.1** Design permission model
- [ ] **E.2.2** Implement RolePermissionManager
- [ ] **E.2.3** Create Permissions GUI page

### Future Modules (v1.4.0+)

- [ ] *(E.3-E.5 tasks to be defined when work begins)*
