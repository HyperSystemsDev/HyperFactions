# B.1 New Player GUI

> **STATUS: COMPLETE** (as of January 26, 2026)

**Target Audience**: Players not currently in a faction.

**Access**: `/f` (when not in faction), `/f menu`, `/f start`

**Nav Bar**: `BROWSE` | `CREATE` | `INVITES` | `MAP` | `HELP`

> **Note**: BROWSE is first (default landing) to encourage faction discovery.

**Implementation Files** (7 pages):
- `NewPlayerBrowsePage.java` - Browse factions with sorting/pagination
- `CreateFactionStep1Page.java` - Wizard step 1 (name, tag, color)
- `CreateFactionStep2Page.java` - Wizard step 2 (recruitment, confirm)
- `InvitesPage.java` - Pending invitations
- `NewPlayerMapPage.java` - Read-only territory map
- `HelpPage.java` - Help/tutorial page
- `NewPlayerNavBarHelper.java` + `NewPlayerPageRegistry.java` - Navigation system

---

## B.1.1 Browse Factions Page (Default Landing)

> **STATUS: IMPLEMENTED**
> - Paginated list (8 factions per page) with sort buttons (power/members/name)
> - Faction cards show: name, member count, power, claim count
> - VIEW button opens faction info in chat (placeholder for detail page)
> - Highlights viewer's own faction with "(Your Faction)" indicator
> - **Deferred enhancements**: Search input, expandable cards, JOIN/REQUEST buttons

**Design Decisions**:
- **List Loading**: Pagination (research shows `LayoutMode: TopScrolling` builds all items at once - infinite scroll would cause page rebuild)
- **Card Display**: Simple flat cards showing core stats with VIEW button
- **Sort Method**: Three clickable text buttons (#SortPower, #SortMembers, #SortName) instead of dropdown

### Browse Page Components

| Component | File | Status | Description |
|-----------|------|--------|-------------|
| **Main Page** | `faction_browser.ui` | **DONE** | Nav bar, `#FactionCard0`-`#FactionCard7` slots, sort buttons, pagination |
| **Faction Card** | `faction_card.ui` | **DONE** | Appended into slots: name, member count, power, claims, VIEW button |

**Wireframe** (Implemented):
```
┌────────────────────────────────────────────────────────────────────────────────┐
│   DASHBOARD   MEMBERS   BROWSE   MAP   RELATIONS   SETTINGS                    │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                │
│   BROWSE FACTIONS                                            12 factions       │
│                                                                                │
│   Sort by:  [POWER]  [MEMBERS]  [NAME]                                         │
│                                                                                │
│   ┌──────────────────────────────────────────────────────────────────────────┐ │
│   │  Dragons                                                 (Your Faction)  │ │
│   │  8 members  │  156 power  │  23 claims                          [VIEW]   │ │
│   └──────────────────────────────────────────────────────────────────────────┘ │
│   ┌──────────────────────────────────────────────────────────────────────────┐ │
│   │  Phoenix Rising                                                          │ │
│   │  12 members  │  140 power  │  18 claims                         [VIEW]   │ │
│   └──────────────────────────────────────────────────────────────────────────┘ │
│   ... (up to 8 cards per page)                                                 │
│                                                                                │
│       [<]                         1/2                            [>]          │
│                                                                                │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                      [ESC] Back                │
└────────────────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown** (Implemented):

| Element ID | Type | Description |
|------------|------|-------------|
| `#FactionCount` | Text | "N factions" total count |
| `#SortPower` | Button | Sort by power descending |
| `#SortMembers` | Button | Sort by member count descending |
| `#SortName` | Button | Sort alphabetically by name |
| `#FactionCard0`-`#FactionCard7` | Container | 8 slots for faction card templates |
| `#ViewBtn` | Button | Opens faction info in chat |
| `#PrevBtn` | Button | Previous page navigation |
| `#NextBtn` | Button | Next page navigation |
| `#PageInfo` | Text | "page/total" indicator |

---

## B.1.2 Create Faction Wizard (2-Step)

> **STATUS: IMPLEMENTED** (replaced single-page form with 2-step wizard)
>
> **Step 1** (`CreateFactionStep1Page.java`): Faction name, tag, color picker
> **Step 2** (`CreateFactionStep2Page.java`): Recruitment setting, confirmation, create action
>
> The wizard validates name uniqueness before proceeding to step 2.

**Original Wireframe** (single-page design, superseded by 2-step wizard):
```
┌─────────────────────────────────────────────────────────────────────┐
│   BROWSE  [CREATE]  INVITES   HELP                    [?] Help Icon │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  CREATE YOUR FACTION                                          ║ │
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   FACTION NAME *                                                    │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │ Enter faction name (3-20 characters)                        │   │
│   └─────────────────────────────────────────────────────────────┘   │
│   ⚠ Name must be unique                                             │
│                                                                     │
│   FACTION COLOR                                                     │
│   ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐ │
│   │ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │ 8 │ 9 │ a │ b │ c │ d │ e │ f │ │
│   └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘ │
│   Selected: [b] (Cyan)   Preview: "Your Faction Name"               │
│                                                                     │
│   FACTION TAG (Optional)                                            │
│   ┌──────────────┐                                                  │
│   │ TAG          │  (2-4 characters, shown in chat)                 │
│   └──────────────┘                                                  │
│                                                                     │
│   DESCRIPTION (Optional)                                            │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │ Describe your faction...                                    │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   RECRUITMENT                                                       │
│   ○ Open (anyone can join)                                          │
│   ● Invite-only (default)                                           │
│                                                                     │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │                    CREATE FACTION                             │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown**:

| Element ID | Type | Description |
|------------|------|-------------|
| `#FactionNameInput` | TextInput | Required, 3-20 chars, validated for uniqueness |
| `#NameValidation` | Text | Shows error/success for name validation |
| `#ColorPicker` | ButtonGrid | 16 color codes (0-f), click to select |
| `#SelectedColor` | Text | Shows selected color code |
| `#ColorPreview` | Text | Live preview of faction name in selected color |
| `#FactionTagInput` | TextInput | Optional, 2-4 chars uppercase |
| `#DescriptionInput` | TextArea | Optional, max 200 chars |
| `#RecruitmentRadio` | RadioGroup | Open or Invite-only |
| `#CreateBtn` | Button | Submit form, disabled until name valid |

**Validation Rules**:
- Name: 3-20 characters, alphanumeric + spaces, unique
- Color: Default 'b' (cyan) if not selected
- Tag: 2-4 uppercase letters, optional
- Description: Max 200 characters, optional
- Submit disabled until name passes validation

**On Success**:
- Close GUI
- Show success message in chat
- Automatically open Faction Dashboard

---

## B.1.3 My Invites Page

> **STATUS: IMPLEMENTED** (`InvitesPage.java`)
> - Shows pending invitations with faction name, inviter, timestamps
> - Accept/Decline actions with confirmation
> - Empty state guidance when no invites

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   BROWSE   CREATE  [INVITES]  HELP                    [?] Help Icon │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  PENDING INVITATIONS (2)                                      ║ │
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Dragons                                                    │   │
│   │  ───────────────────────────────────────────────────────────│   │
│   │  Invited by: FireLord                                       │   │
│   │  Sent: 5 minutes ago                                        │   │
│   │  Expires: in 55 minutes                                     │   │
│   │                                                             │   │
│   │  ┌──────────┐  ┌──────────┐  ┌──────────┐                   │   │
│   │  │  ACCEPT  │  │ DECLINE  │  │   INFO   │                   │   │
│   │  └──────────┘  └──────────┘  └──────────┘                   │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ─────────────────────────────────────────────────────────────     │
│   No pending invites? Browse factions to request membership!        │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown**:

| Element ID | Type | Description |
|------------|------|-------------|
| `#InviteCount` | Badge | Number of pending invites |
| `#InviteList` | ScrollContainer | List of invite cards |
| `#InviteCard` | Card | Faction name, inviter, timestamps |
| `#AcceptBtn` | Button | Accept invite, join faction |
| `#DeclineBtn` | Button | Decline invite, remove from list |
| `#InfoBtn` | Button | Show faction details |
| `#EmptyState` | Container | Shown when no invites, links to Browse |

**Behaviors**:
- Accept: Join faction immediately, close GUI, show Dashboard
- Decline: Remove invite, show confirmation
- Info: Expand card to show full faction details
- Auto-refresh: Poll for new invites while page open

---

## B.1.4 Help Page (New Player Context)

> **STATUS: IMPLEMENTED** (`HelpPage.java`)

See **Phase C** for full Help System specification. New Player context shows:
- "Getting Started" (highlighted)
- "Faction Member Guide" (preview what's possible after joining)
- Links to external documentation

---

## B.1.5 Map Page (Read-Only)

> **STATUS: IMPLEMENTED** (`NewPlayerMapPage.java`)
> - Read-only view of territory map (no claim/unclaim actions)
> - Shows faction territories across the world
> - Helps new players understand territory distribution before joining

---

## Enhancement Ideas

| Priority | Enhancement | Description |
|----------|-------------|-------------|
| High | Search Input | Filter factions by name in Browse page |
| High | Join/Request Buttons | Direct action buttons on faction cards |
| Medium | Expandable Cards | Reveal description, leader, relations on click |
| Medium | Invite Counter Badge | Show pending count on nav item |
| Low | Server Stats Footer | Show total factions, players, claims |
