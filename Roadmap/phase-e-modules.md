# Phase E: Optional Modules

These are larger features planned for future versions. GUI placeholders will be added in Phase B.

---

## E.1 Faction Treasury/Bank System

- **Status**: Foundation complete (EconomyManager stub exists)
- **Features**:
  - Faction balance tracking
  - Deposit/withdraw commands
  - Transaction logging
  - Tax system (configurable % of member earnings)
  - Permission-based access (who can withdraw)
- **Commands**: `/f money`, `/f money deposit <amount>`, `/f money withdraw <amount>`, `/f money log`
- **GUI**: Treasury page in Modules section

---

## E.2 Role-Specific Territory Permissions

- **Status**: Not started
- **Features**:
  - Configure per-role: build, break, interact, container access
  - Guest permissions for non-members
  - Ally permissions
  - `/f perms` command and GUI page
- **GUI**: Permissions page in Settings section

---

## E.3 Raid System

- **Status**: Not started
- **Features**:
  - Structured raid initiation
  - Objectives (core block, flag capture, etc.)
  - 24-hour cooldown between raids on same faction
  - Raid notifications
  - Victory rewards
- **GUI**: Raids page in Modules section (Coming Soon placeholder)

---

## E.4 Faction Levels/Progression

- **Status**: Not started
- **Features**:
  - XP from: claiming, PvP victories, objectives
  - Level unlocks: increased claims, power bonuses, cosmetics
  - Leaderboard
- **GUI**: Levels page in Modules section (Coming Soon placeholder)

---

## E.5 War Declaration System

- **Status**: Not started
- **Features**:
  - Formal war declaration
  - War objectives and victory conditions
  - War end conditions (surrender, timer, objectives)
  - War statistics
- **GUI**: War page in Modules section (Coming Soon placeholder)

---

## E.6 Implementation Tasks (Future)

- [ ] **E.1.1** Implement FactionEconomy data model
- [ ] **E.1.2** Implement EconomyManager
- [ ] **E.1.3** Add treasury commands
- [ ] **E.1.4** Create Treasury GUI page
- [ ] **E.2.1** Design permission model
- [ ] **E.2.2** Implement RolePermissionManager
- [ ] **E.2.3** Create Permissions GUI page
- [ ] *(E.3-E.5 tasks to be defined when work begins)*
