# B.2 Faction Player GUI

**Target Audience**: Players in a faction.

**Access**: `/f` (when in faction), `/f gui`, `/f menu`

**Nav Bar**: `DASHBOARD` | `MEMBERS` | `BROWSE` | `MAP` | `RELATIONS` | `SETTINGS`

> **Note**: MODULES accessible from Settings page. Admin quick-switch is a floating button. HELP removed from nav (accessible via command).

**Design Decisions**:
- **Quick Actions**: 5 actions (Home, Claim, F-Chat, A-Chat, Leave) - commonly used actions
- **Member Sorting**: Role first (Leader > Officer > Member), then online status within each role
- **Settings Access**: Read-only for Members (can view but not edit)
- **Territory Map**: 29x17 grid for better context
- **Browse Page**: Shows all factions with relation indicators for faction members

---

## B.2.1 Faction Dashboard (Default Landing)

> **STATUS: IMPLEMENTED** (2026-01-25)
> - Redesigned layout with faction identity banner, stat cards, quick actions, and activity feed
> - Chat toggles for faction and ally chat (placeholder, requires ChatManager)
> - Leave confirmation modal following disband_confirm pattern

### Dashboard Layout

```
+--------------------------------------------------------------------------------+
|  [DASHBOARD]   MEMBERS   BROWSE   MAP   RELATIONS   SETTINGS                   |
+--------------------------------------------------------------------------------+
|                                                                                |
|  +------------------------------------------------------------------------+   |
|  |                     DRAGONS [DRG]                                      |   |
|  |              "From the ashes we rise!"                                 |   |
|  +------------------------------------------------------------------------+   |
|                                                                                |
|  +------------------------+  +------------------------+  +------------------+ |
|  | POWER                  |  | CLAIMS                 |  | MEMBERS          | |
|  | Current: 156           |  | Current: 23            |  | Total: 8         | |
|  | Maximum: 200           |  | Maximum: 78            |  | Online: 3        | |
|  | 78%                    |  | Available: 55          |  |                  | |
|  +------------------------+  +------------------------+  +------------------+ |
|                                                                                |
|  QUICK ACTIONS                                                                 |
|  [HOME]  [CLAIM]  [F-CHAT OFF]  [A-CHAT OFF]  [LEAVE]                         |
|                                                                                |
|  RECENT ACTIVITY                                                               |
|  +------------------------------------------------------------------------+   |
|  | DragonSlayer joined the faction                        5 minutes ago   |   |
|  | FireLord promoted ShadowBlade to Officer               2 hours ago     |   |
|  | Claimed chunk at (120, 340)                            3 hours ago     |   |
|  +------------------------------------------------------------------------+   |
+--------------------------------------------------------------------------------+
```

### Dashboard Components

| Component | File | Status | Description |
|-----------|------|--------|-------------|
| **Main Page** | `faction_dashboard.ui` | **DONE** | Full dashboard layout |
| **Stat Card** | `dashboard_stat_card.ui` | **DONE** | Reusable stat card template |
| **Action Button** | `dashboard_action_btn.ui` | **DONE** | Quick action button template |
| **Activity Entry** | `activity_entry.ui` | **DONE** | Activity log entry template |
| **Leave Confirm** | `leave_confirm.ui` | **DONE** | Leave confirmation modal |

### Element Breakdown

| Element ID | Type | Description |
|------------|------|-------------|
| `#FactionName` | Text | Faction name (large, colored) |
| `#FactionTag` | Text | Tag in brackets "[DRG]" |
| `#FactionDescription` | Text | Description text |
| `#PowerCard` | Container | Power stat card |
| `#ClaimsCard` | Container | Claims stat card |
| `#MembersCard` | Container | Members stat card |
| `#HomeBtn` | Button | Teleport to faction home |
| `#ClaimBtn` | Button | Claim current chunk |
| `#LeaveBtnContainer` | Container | Leave button container |
| `#ActivityFeed` | Container | Recent activity entries |

---

## B.2.2 Members Page

> **STATUS: IMPLEMENTED** (2026-01-25)
> - Paginated member list (8 per page) sorted by role level then username
> - Each entry shows: username, role, last online time
> - Role-based action buttons: PROMOTE, DEMOTE, KICK, TRANSFER
> - Pagination with < > buttons

### Members Page Components

| Component | File | Status | Description |
|-----------|------|--------|-------------|
| **Main Page** | `faction_members.ui` | **DONE** | 8 member entry slots, pagination |
| **Member Entry** | `member_entry.ui` | **DONE** | Name, role, last online, action buttons |

**Wireframe**:
```
┌────────────────────────────────────────────────────────────────────────────────┐
│   DASHBOARD  [MEMBERS]   BROWSE   MAP   RELATIONS   SETTINGS                   │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                │
│   FACTION MEMBERS                                                8 members     │
│                                                                                │
│   ┌──────────────────────────────────────────────────────────────────────────┐ │
│   │  FireLord              LEADER          Online now                        │ │
│   │                                   [TRANSFER]  (leader can see)           │ │
│   └──────────────────────────────────────────────────────────────────────────┘ │
│   ┌──────────────────────────────────────────────────────────────────────────┐ │
│   │  DragonSlayer          OFFICER         2 hours ago                       │ │
│   │                                   [DEMOTE]  [KICK]  (leader sees)        │ │
│   └──────────────────────────────────────────────────────────────────────────┘ │
│   ┌──────────────────────────────────────────────────────────────────────────┐ │
│   │  ShadowBlade           MEMBER          5 mins ago                        │ │
│   │                                   [PROMOTE]  [KICK]  (officers+ see)     │ │
│   └──────────────────────────────────────────────────────────────────────────┘ │
│                                                                                │
│       [<]                         1/1                            [>]          │
│                                                                                │
└────────────────────────────────────────────────────────────────────────────────┘
```

**Permission Logic**:
- **Members**: See list only, no action buttons
- **Officers**: See PROMOTE + KICK for members (not self, not other officers)
- **Leader**: See all buttons for everyone except self

---

## B.2.3 Browse Factions Page (Faction Players)

> **STATUS: IMPLEMENTED** (2026-01-25)
> - Browse factions with relation indicators for faction members
> - Shows relation to each faction (ALLY, ENEMY, NEUTRAL)
> - No JOIN/REQUEST buttons (already in a faction)
> - VIEW button shows faction info in chat

### Key Differences from Non-Faction Browse (B.1.1)

| Feature | Non-Faction Browse (B.1.1) | Faction Browse (B.2.3) |
|---------|---------------------------|------------------------|
| Relation Indicator | None | [ALLY], [ENEMY], [NEUTRAL] badges |
| Own Faction | N/A | "(Your Faction)" indicator |
| Action Buttons | JOIN, REQUEST JOIN | VIEW only |

### Browse Components

| Component | File | Status | Description |
|-----------|------|--------|-------------|
| **Main Page** | `faction_browser.ui` | REUSE | Same template as B.1.1 |
| **Ally Indicator** | `indicator_ally.ui` | **DONE** | "[ALLY]" badge (blue text) |
| **Enemy Indicator** | `indicator_enemy.ui` | **DONE** | "[ENEMY]" badge (red text) |
| **Neutral Indicator** | `indicator_neutral.ui` | **DONE** | "[NEUTRAL]" badge (gray text) |

---

## B.2.4 Territory Map Page

> **STATUS: IMPLEMENTED** (2026-01-25)
> - 29x17 interactive chunk grid (GRID_RADIUS_X=14, GRID_RADIUS_Z=8)
> - Click-to-claim (wilderness) / right-click-to-unclaim (own territory)
> - Color-coded ownership (own, ally, enemy, neutral, safe zone, war zone, wilderness)
> - Officer+ can left-click enemy territory to attempt overclaim
> - **Technical Discovery**: Hytale uses 32-block chunks (not 16-block like Minecraft)

### Territory Map Components

| Component | File | Status | Description |
|-----------|------|--------|-------------|
| **Main Page** | `chunk_map.ui` | **DONE** | 29x17 grid container, legend, claim stats |
| **Chunk Button** | `chunk_btn.ui` | **DONE** | Invisible button overlay for click detection |
| **Player Chunk** | `chunk_btn_player.ui` | **DONE** | Special styling for player's current position |

**Map Color Legend:**

| Meaning | Hex Code | Description |
|---------|----------|-------------|
| Your Territory | `#4ade80` (bright green) | Chunks your faction owns |
| Ally Territory | `#60a5fa` (bright blue) | Allied faction's chunks |
| Enemy Territory | `#f87171` (bright red) | Enemy faction's chunks |
| Other Faction | `#fbbf24` (yellow/gold) | Neutral faction's chunks |
| Wilderness | `#1e293b` (dark slate) | Unclaimed chunks |
| Safe Zone | `#2dd4bf` (teal) | Admin-protected safe areas |
| War Zone | `#c084fc` (light purple) | Admin-designated PvP areas |
| You Are Here | `#ffffff` (white) | Player's current chunk |

**Interaction Flow**:
1. Player opens map - grid renders centered on player position
2. **Direct click actions** (no selection step, officers only):
   - **Left-click wilderness**: Claim chunk immediately
   - **Right-click own territory**: Unclaim chunk immediately
   - **Left-click enemy territory**: Attempt overclaim (if enemy is overclaimed)
   - **Ally/Safezone/Warzone**: No click action
3. Map refreshes after each claim/unclaim action
4. Chat messages confirm success or explain failure

---

## B.2.5 Relations Page

> **STATUS: IMPLEMENTED** (2026-01-25)
> - Three sections visible at once: Allies, Enemies, Pending Requests
> - Each section shows count in header (e.g., "ALLIES (2)")
> - Officers+ can: set neutral, set enemy, request ally, accept/decline requests
> - "+ SET RELATION" button visible only for officers+
> - Set Relation modal for searching and setting relations

### Relations Page Components

| Component | File | Status | Description |
|-----------|------|--------|-------------|
| **Main Page** | `faction_relations.ui` | **DONE** | Sectioned layout |
| **Relation Entry** | `relation_entry.ui` | **DONE** | Faction name, leader, date, buttons |
| **Empty State** | `relation_empty.ui` | **DONE** | Empty state message |
| **Button Templates** | `relation_btn_*.ui` | **DONE** | Neutral, Ally, Enemy, Accept, Decline |
| **Set Relation Modal** | `set_relation_modal.ui` | **DONE** | Search input, results, pagination |

**Permission-Based Button Logic**:
- **Allies**: NEUTRAL + ENEMY buttons (for officers+)
- **Enemies**: NEUTRAL + ALLY buttons (for officers+)
- **Pending Requests**: ACCEPT + DECLINE buttons (for officers+)
- **Members**: No buttons visible (read-only view)

---

## B.2.6 Settings Page (Officer+)

> **STATUS: IMPLEMENTED** (2026-01-25)
> - GENERAL section: Name, Tag, Description with EDIT buttons (each opens a modal)
> - APPEARANCE section: Color preview + hex code with CHANGE button
> - RECRUITMENT section: Current status (Open/Invite Only) with CHANGE button
> - HOME LOCATION section: SET HOME HERE and TELEPORT buttons
> - MODULES section: VIEW MODULES button
> - DANGER ZONE section: DISBAND button (Leader only)
> - Non-officers see error page

### Settings Page Components

| Component | File | Status | Description |
|-----------|------|--------|-------------|
| **Main Page** | `faction_settings.ui` | **DONE** | Sectioned layout |
| **Danger Zone** | `settings_danger_zone.ui` | **DONE** | Conditionally appended for leaders |
| **Error Page** | `error_page.ui` | **DONE** | Shown to non-officers |

### Settings Page Modals (All Implemented)

| Modal | File | Status | Description |
|-------|------|--------|-------------|
| **Rename Faction** | `rename_modal.ui` | **DONE** | Validates 3-32 chars, uniqueness |
| **Edit Tag** | `tag_modal.ui` | **DONE** | Validates 1-5 chars, alphanumeric |
| **Edit Description** | `description_modal.ui` | **DONE** | Max 256 chars |
| **Color Picker** | `color_picker.ui` | **DONE** | 16 color grid |
| **Recruitment Status** | `recruitment_modal.ui` | **DONE** | Open/Invite Only |
| **Disband Confirm** | `disband_confirm.ui` | **DONE** | Confirmation dialog |

**Permission Visibility**:
- **Members**: See error page "Only officers and leaders can change faction settings."
- **Officers**: See all sections except Danger Zone
- **Leader**: See all sections including Danger Zone with DISBAND button

---

## B.2.7 Modules Page

> **STATUS: IMPLEMENTED** (Placeholder)
> - 2x2 grid of module cards with "Coming Soon" badges
> - Modules: Treasury, Raids, Levels, War

See [module-placeholders.md](module-placeholders.md) for placeholder pattern.

---

## Enhancement Ideas

| Priority | Enhancement | Description |
|----------|-------------|-------------|
| High | Activity Feed Pagination | Full log with filtering on Dashboard |
| High | Member Management Modal | Actions directly from member card |
| Medium | Online Indicator | Show online members with green dot |
| Medium | Territory Statistics | Claim distribution by world |
| Medium | Invite Button | Send invites from Members page |
| Low | Power Display per Member | Show individual power contributions |
| Low | Join Date Display | When each member joined |
