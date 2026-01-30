# B.3 Admin GUI

**Target Audience**: Server admins with `hyperfactions.admin` permission.

**Access**: `/f admin`, floating action button from Faction Player GUI

**Nav Bar**: `DASHBOARD` | `FACTIONS` | `ZONES` | `PLAYERS` | `CONFIG` | `LOGS` | `HELP`

> **Note**: Player GUI switch is a floating button.

**Design Decisions**:
- **Logs Page**: Separate nav item (7th tab) for direct access
- **Config Editing**: Selected common values editable in GUI, advanced settings require file editing
- **Disband Confirmation**: Simple confirm dialog (not type-to-confirm)

**Design Principles**:
- Professional, information-dense layout
- Confirmation dialogs for destructive actions
- Audit logging for all admin actions
- All documentation accessible from Help page

---

## B.3.1 Admin Dashboard

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│  [DASHBOARD] FACTIONS  ZONES  PLAYERS  CONFIG  HELP        [?]     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  ADMIN DASHBOARD                               HyperFactions   ║ │
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   SERVER STATISTICS                                                 │
│   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐   │
│   │  FACTIONS   │ │   CLAIMS    │ │   ZONES     │ │   PLAYERS   │   │
│   │     24      │ │   1,240     │ │     5       │ │    156      │   │
│   │             │ │   chunks    │ │  (3S / 2W)  │ │  (42 online)│   │
│   └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘   │
│                                                                     │
│   QUICK ACTIONS                                                     │
│   ┌────────────────┐ ┌────────────────┐ ┌────────────────┐          │
│   │  BYPASS        │ │  VIEW LOGS     │ │  RELOAD        │          │
│   │    [OFF]       │ │                │ │    CONFIG      │          │
│   └────────────────┘ └────────────────┘ └────────────────┘          │
│                                                                     │
│   ALERTS                                                            │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  • Shadow Collective is RAIDABLE (power < claims)           │   │
│   │  • Dragons vs Phoenix Rising - recent PvP conflict          │   │
│   │  • 3 factions have been inactive for 30+ days               │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   RECENT ADMIN ACTIONS                                              │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  • Admin created SafeZone "Spawn"                 1 hr ago  │   │
│   │  • Admin adjusted power for Steve (+50)           2 hr ago  │   │
│   │  • Admin force-disbanded "Griefers Inc"           1 day ago │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│                                                            ┌─────┐  │
│                                                            │ FAB │  │
│                                                            │PLAYER│ │
│                                                            └─────┘  │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown**:

| Element ID | Type | Description |
|------------|------|-------------|
| `#StatCards` | Container | 4 stat cards with key metrics |
| `#BypassToggle` | ToggleButton | Enable/disable admin bypass mode |
| `#ViewLogsBtn` | Button | Opens AdminLogsPage |
| `#ReloadConfigBtn` | Button | Reload config from disk |
| `#AlertsList` | ScrollList | Server health warnings |
| `#RecentActions` | ScrollList | Admin audit log (last 10 actions) |
| `#PlayerFab` | FloatingButton | Switch to Player GUI (bottom-right) |

---

## B.3.2 Admin Factions Page

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   DASHBOARD [FACTIONS] ZONES  PLAYERS  CONFIG  HELP            [?] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   MANAGE FACTIONS (24 total)                                        │
│   ┌───────────────────────────────────┐  Sort: [Name ▼]             │
│   │ Search factions...                │                             │
│   └───────────────────────────────────┘                             │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ▼ Dragons                                         [MANAGE] │   │
│   │  ───────────────────────────────────────────────────────────│   │
│   │    Leader: FireLord  │  Members: 8  │  Power: 156/200       │   │
│   │    Claims: 23  │  Created: 30 days ago                      │   │
│   │                                                             │   │
│   │    Admin Actions:                                           │   │
│   │    [EDIT]  [ADJUST POWER]  [MANAGE MEMBERS]  [DISBAND]      │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ▶ Phoenix Rising                                  [MANAGE] │   │
│   └─────────────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ▶ Shadow Collective                    ⚠ RAIDABLE [MANAGE]│   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

**Admin Faction Actions**:
- **EDIT**: Override faction name, description, color, recruitment
- **ADJUST POWER**: Add/subtract power from faction total
- **MANAGE MEMBERS**: Add/remove members, change roles, transfer leadership
- **DISBAND**: Force disband with confirmation

---

## B.3.3 Admin Zones Page

> **STATUS: PARTIALLY IMPLEMENTED**
> - List and manage SafeZones and WarZones
> - Create new zones, edit properties, delete zones
> - Manage chunk assignments

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   DASHBOARD  FACTIONS [ZONES] PLAYERS  CONFIG  HELP            [?] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   PROTECTED ZONES (5)                              [+ CREATE ZONE]  │
│                                                                     │
│   SAFEZONES (3)                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Spawn                                                      │   │
│   │     Center: (0, 0)  │  Chunks: 9  │  Created: 45 days ago   │   │
│   │     Flags: PvP OFF, Build OFF, Monsters OFF                 │   │
│   │     [EDIT]  [MANAGE CHUNKS]  [DELETE]                       │   │
│   └─────────────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Market                                                     │   │
│   │     Center: (500, 200)  │  Chunks: 4  │  Created: 30 days   │   │
│   │     [EDIT]  [MANAGE CHUNKS]  [DELETE]                       │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   WARZONES (2)                                                      │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Arena                                                      │   │
│   │     Center: (1000, -500)  │  Chunks: 16  │  PvP ALWAYS ON   │   │
│   │     [EDIT]  [MANAGE CHUNKS]  [DELETE]                       │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

---

## B.3.4 Admin Players Page

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   DASHBOARD  FACTIONS  ZONES [PLAYERS] CONFIG  HELP            [?] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   PLAYER MANAGEMENT                                                 │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │ Search player by name or UUID...                              │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  PLAYER: FireLord                                   Online    ║ │
│   ╠═══════════════════════════════════════════════════════════════╣ │
│   ║  UUID: 123e4567-e89b-12d3-a456-426614174000                   ║ │
│   ║  Faction: Dragons (LEADER)                                    ║ │
│   ║  Power: 20 / 20  │  Last Online: Now                          ║ │
│   ║  Combat Tagged: No                                            ║ │
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   ADMIN ACTIONS                                                     │
│   ┌────────────────┐ ┌────────────────┐ ┌────────────────┐          │
│   │  ADJUST POWER  │ │ REMOVE FROM    │ │ CLEAR COMBAT   │          │
│   │   +/- Amount   │ │   FACTION      │ │     TAG        │          │
│   └────────────────┘ └────────────────┘ └────────────────┘          │
│                                                                     │
│   ┌────────────────┐ ┌────────────────┐                             │
│   │  VIEW FACTION  │ │  VIEW LOGS     │  (actions for this player) │
│   └────────────────┘ └────────────────┘                             │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

---

## B.3.5 Admin Config Page

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   DASHBOARD  FACTIONS  ZONES  PLAYERS [CONFIG] HELP            [?] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   SERVER CONFIGURATION                           [RELOAD CONFIG]    │
│                                                                     │
│   ⚠ Changes require reload or restart to take effect                │
│                                                                     │
│   POWER SETTINGS                                                    │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Max Power per Player:        [20      ]                    │   │
│   │  Power Regen per Hour:        [2       ]                    │   │
│   │  Power Loss on Death:         [4       ]                    │   │
│   │  Claim Cost (power/chunk):    [2       ]                    │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   FACTION SETTINGS                                                  │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Max Members per Faction:     [50      ]                    │   │
│   │  Max Allies per Faction:      [5       ]                    │   │
│   │  Invite Expiry (minutes):     [60      ]                    │   │
│   │  Combat Tag Duration (sec):   [15      ]                    │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   TELEPORTATION                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Home Warmup (seconds):       [3       ]                    │   │
│   │  Home Cooldown (seconds):     [60      ]                    │   │
│   │  Stuck Warmup (seconds):      [30      ]                    │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                    SAVE CHANGES                             │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

---

## B.3.6 Admin Logs Page

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   ADMIN LOGS                                     [← Back to Dash]   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   FILTERS                                                           │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Type: [All Actions ▼]  Faction: [All ▼]  Player: [All ▼]   │   │
│   │  Date Range: [Last 24 hours ▼]                  [APPLY]     │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   LOG ENTRIES (247 matching)                        [EXPORT CSV]    │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  2024-01-25 14:32:15 │ CLAIM     │ Dragons    │ FireLord    │   │
│   │    Claimed chunk (120, 340) in Overworld                    │   │
│   ├─────────────────────────────────────────────────────────────┤   │
│   │  2024-01-25 14:30:02 │ JOIN      │ Dragons    │ ShadowBlade │   │
│   │    Accepted invite from FireLord                            │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   Showing 1-20 of 247  │  [◀ Prev]  Page 1 of 13  [Next ▶]          │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

**Log Types**:
- `CREATE` - Faction created
- `DISBAND` - Faction disbanded
- `JOIN` - Player joined faction
- `LEAVE` - Player left faction
- `KICK` - Player kicked
- `CLAIM` - Territory claimed
- `UNCLAIM` - Territory released
- `ALLY` - Alliance formed/requested
- `ENEMY` - Enemy declared
- `ADMIN` - Admin action (bypass, zone, power adjust)

---

## Enhancement Ideas

| Priority | Enhancement | Description |
|----------|-------------|-------------|
| High | Search Across All | Global faction/player search |
| High | Bulk Actions | Mass operations on factions |
| Medium | Power Adjustment | Add/remove faction power from admin |
| Medium | Audit Log | Track admin actions separately |
| Medium | Export Logs | CSV/JSON export for external analysis |
| Low | Inactive Faction Cleanup | Auto-detect and flag inactive factions |
