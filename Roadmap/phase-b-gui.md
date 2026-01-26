# Phase B: GUI System Redesign

**Goal**: Create distinct, polished GUI experiences for each player state.

**Design Decisions**:
- **Wireframe Format**: ASCII mockups + detailed element descriptions for async review
- **New Player Focus**: Browse/Join first - encourage discovering existing factions
- **Create Wizard**: Single page with all options - fast for both new and experienced users
- **Browser Style**: Minimal cards (name only) with expand for details - clean, scalable
- **Dashboard Actions**: Home + Claim + Chat - most commonly used actions
- **Admin Switch**: Floating action button in bottom-right - always visible, mobile-inspired

---

## B.0 HyperUI Design Framework

> **Note**: This section defines the modular, generalized GUI design system that all HyperSystem plugins can share. The goal is to create reusable components, consistent visual language, and flexible page layouts.

### B.0.1 Design Principles

**1. Modularity First**
- Every UI element should be a self-contained, reusable component
- Components should work across different plugins (HyperFactions, HyperHomes, HyperWarp, etc.)
- Clear separation between layout containers, content components, and interactive elements

**2. Consistent Visual Language**
- Unified color palette across all HyperSystem plugins
- Standardized spacing, typography, and interactive feedback
- Common iconography and visual metaphors

**3. Flexible Layouts**
- Pages can exist with or without navigation
- Support for full-screen dialogs, modals, and embedded panels
- Responsive to different screen sizes (if Hytale supports this)

**4. Progressive Disclosure**
- Show essential information first
- Details available on-demand (expand/collapse, hover, click)
- Reduce cognitive load for new players

---

### B.0.2 Page Layout Types

HyperUI supports three fundamental page layouts:

#### Type A: Full Navigation Page (Standard)
The most common layout with persistent navigation bar.

```
┌─────────────────────────────────────────────────────────────────────┐
│  NAV_ITEM_1  [ACTIVE]  NAV_ITEM_3  NAV_ITEM_4           [?] Help    │  ← NavBar
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │  ← PageHeader
│   ║  PAGE TITLE                                        [ACTIONS]  ║ │
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │  ← ContentArea
│   │                                                             │   │
│   │                    MAIN CONTENT                             │   │
│   │                                                             │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ─────────────────────────────────────────────────────────────     │  ← PageFooter
│   Status bar / contextual information                               │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │  ← SystemFooter
└─────────────────────────────────────────────────────────────────────┘
```

**Use Cases**: Main plugin interfaces, dashboards, list views
**Examples**: Faction Dashboard, Browse Factions, Member List

---

#### Type B: Nav-Less Dialog Page
For focused tasks that require full attention. No navigation bar.

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  DIALOG TITLE                                      [X] Close  ║ │  ← DialogHeader
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                                                             │   │
│   │                                                             │   │
│   │                    FOCUSED CONTENT                          │   │  ← DialogContent
│   │                                                             │   │
│   │                                                             │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌───────────────────────┐  ┌───────────────────────┐              │  ← ActionBar
│   │        CANCEL         │  │        CONFIRM        │              │
│   └───────────────────────┘  └───────────────────────┘              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**Use Cases**: Confirmation dialogs, forms, wizards, critical actions
**Examples**: Transfer Leadership, Delete Faction, Kick Confirmation

---

#### Type C: Overlay Modal
Appears over existing content with backdrop dimming. Smallest footprint.

```
                    ┌───────────────────────────────┐
                    │  MODAL TITLE              [X] │  ← ModalHeader
                    ├───────────────────────────────┤
                    │                               │
                    │       COMPACT CONTENT         │  ← ModalContent
                    │                               │
                    ├───────────────────────────────┤
                    │  [CANCEL]         [CONFIRM]   │  ← ModalActions
                    └───────────────────────────────┘
```

**Use Cases**: Quick confirmations, tooltips with actions, mini-forms
**Examples**: "Kick player?", "Claim this chunk?", Invite sent confirmation

---

#### Type D: Full-Screen Immersive
No chrome at all - the entire screen is content. Used for maps, viewers.

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│                                                                     │
│                                                                     │
│                                                                     │
│                        IMMERSIVE CONTENT                            │
│                      (map, image, viewer)                           │
│                                                                     │
│                                                                     │
│                                                                     │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  [TOOL 1]  [TOOL 2]  [TOOL 3]                         [EXIT] │   │  ← FloatingToolbar
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

**Use Cases**: Territory maps, image galleries, world viewers
**Examples**: Interactive Chunk Map, Faction Territory Overview

---

### B.0.3 Component Library

#### Core Components (HyperUI Base)

| Component | Description | Props/Options |
|-----------|-------------|---------------|
| `NavBar` | Horizontal navigation with tabs | items[], activeIndex, helpEnabled |
| `PageHeader` | Title area with optional actions | title, subtitle, actions[] |
| `Button` | Standard clickable button | label, variant(primary/secondary/danger), disabled, icon |
| `IconButton` | Icon-only button | icon, tooltip, variant |
| `TextInput` | Single-line text entry | placeholder, value, validation, maxLength |
| `TextArea` | Multi-line text entry | placeholder, value, maxLength, rows |
| `Dropdown` | Select from options | options[], selected, placeholder |
| `Toggle` | On/Off switch | value, label, disabled |
| `RadioGroup` | Single selection from options | options[], selected |
| `Checkbox` | Multiple selection | checked, label, disabled |
| `Card` | Bordered content container | title, expandable, actions[] |
| `Badge` | Small label/tag | text, variant(info/success/warning/error) |
| `ScrollList` | Scrollable list container | items[], maxHeight |
| `Modal` | Overlay dialog | title, content, actions[], dismissable |
| `Tooltip` | Hover information | text, position(top/bottom/left/right) |
| `Divider` | Visual separator | orientation(horizontal/vertical) |
| `Spinner` | Loading indicator | size(small/medium/large) |
| `ProgressBar` | Progress visualization | value, max, showText |
| `StatCard` | Metric display | icon, label, value, subtext |
| `FloatingButton` | FAB for quick actions | icon, position(br/bl/tr/tl) |

#### Faction-Specific Components

| Component | Description | Base Component |
|-----------|-------------|----------------|
| `FactionCard` | Expandable faction info | Card |
| `MemberCard` | Player with role and actions | Card |
| `InviteCard` | Pending invite with actions | Card |
| `ClaimCell` | Single chunk in map grid | (custom) |
| `RelationBadge` | Ally/Enemy/Neutral indicator | Badge |
| `PowerMeter` | Power visualization | ProgressBar |
| `ActivityItem` | Log entry with timestamp | (custom) |

---

### B.0.4 Color System

**Primary Palette** (Shared across HyperSystem):

| Token | Hex | Usage |
|-------|-----|-------|
| `--hs-bg-primary` | `#1a1a2e` | Main background |
| `--hs-bg-secondary` | `#252540` | Card backgrounds, elevated surfaces |
| `--hs-bg-tertiary` | `#2d2d4a` | Hover states, nested containers |
| `--hs-border` | `#404060` | Borders, dividers |
| `--hs-text-primary` | `#ffffff` | Main text |
| `--hs-text-secondary` | `#b0b0c0` | Secondary text, labels |
| `--hs-text-muted` | `#707090` | Disabled text, hints |

**Accent Colors**:

| Token | Hex | Usage |
|-------|-----|-------|
| `--hs-accent-primary` | `#6366f1` | Primary actions, links |
| `--hs-accent-success` | `#22c55e` | Success states, confirmations |
| `--hs-accent-warning` | `#f59e0b` | Warnings, pending states |
| `--hs-accent-error` | `#ef4444` | Errors, destructive actions |
| `--hs-accent-info` | `#3b82f6` | Informational, neutral highlights |

**Faction Colors** (Minecraft color codes):

| Code | Color | Usage |
|------|-------|-------|
| `0` | Black | - |
| `1` | Dark Blue | Faction theme |
| `2` | Dark Green | Faction theme |
| `3` | Dark Aqua | Faction theme |
| `4` | Dark Red | Enemy indicator |
| `5` | Dark Purple | Faction theme |
| `6` | Gold | Faction theme |
| `7` | Gray | Neutral |
| `8` | Dark Gray | Disabled |
| `9` | Blue | Faction theme |
| `a` | Green | Ally indicator |
| `b` | Aqua | Default faction color |
| `c` | Red | Enemy indicator |
| `d` | Light Purple | Faction theme |
| `e` | Yellow | Warning, highlight |
| `f` | White | Default text |

---

### B.0.5 Typography

| Token | Size | Weight | Usage |
|-------|------|--------|-------|
| `--hs-font-title` | 24px | Bold | Page titles |
| `--hs-font-heading` | 18px | SemiBold | Section headings |
| `--hs-font-subheading` | 14px | Medium | Card titles, labels |
| `--hs-font-body` | 12px | Regular | Body text |
| `--hs-font-caption` | 10px | Regular | Captions, timestamps |

**Note**: Actual sizes depend on Hytale's UI scaling. Values are reference sizes.

---

### B.0.6 Spacing System

Using a 4px base unit:

| Token | Value | Usage |
|-------|-------|-------|
| `--hs-space-1` | 4px | Tight gaps (icon-text) |
| `--hs-space-2` | 8px | Standard gaps |
| `--hs-space-3` | 12px | Component padding |
| `--hs-space-4` | 16px | Card padding |
| `--hs-space-5` | 20px | Section spacing |
| `--hs-space-6` | 24px | Page margins |
| `--hs-space-8` | 32px | Large section gaps |

---

### B.0.7 Template File Structure

For HyperFactions, the template structure follows this pattern:

```
resources/ui/
├── common/                          # Shared across all pages
│   ├── navbar.ui                    # Navigation bar template
│   ├── modal.ui                     # Modal dialog template
│   ├── components/                  # Reusable components
│   │   ├── button.ui
│   │   ├── card.ui
│   │   ├── input.ui
│   │   └── ...
│   └── styles/                      # Common styles (if supported)
│       └── theme.ui
│
├── new-player/                      # New Player GUI pages
│   ├── browse.ui
│   ├── create.ui
│   ├── invites.ui
│   └── help.ui
│
├── faction/                         # Faction Player GUI pages
│   ├── dashboard.ui
│   ├── members.ui
│   ├── map.ui
│   ├── relations.ui
│   ├── settings.ui
│   └── modules/
│       ├── treasury.ui              # (Coming Soon placeholder)
│       ├── raids.ui
│       └── ...
│
├── admin/                           # Admin GUI pages
│   ├── dashboard.ui
│   ├── factions.ui
│   ├── zones.ui
│   ├── players.ui
│   ├── config.ui
│   └── logs.ui
│
└── dialogs/                         # Nav-less dialog pages
    ├── confirm-kick.ui
    ├── confirm-disband.ui
    ├── transfer-leader.ui
    └── ...
```

**For Other HyperSystem Plugins**:

```
# HyperHomes
resources/ui/
├── common/                          # Symlink or copy from HyperUI base
├── homes/
│   ├── list.ui                      # Home list (Type A)
│   ├── create.ui                    # Create home (Type B)
│   └── teleport.ui                  # Teleport confirmation (Type C)
└── dialogs/
    ├── delete-home.ui
    └── share-home.ui

# HyperWarp
resources/ui/
├── common/
├── warps/
│   ├── list.ui
│   ├── create.ui
│   └── categories.ui
└── tpa/
    ├── requests.ui
    └── settings.ui
```

---

### B.0.8 State Management Patterns

**Page State** (within a single page session):

```java
public class PageState<T> {
    private T state;
    private final List<Consumer<T>> listeners = new ArrayList<>();

    public void setState(T newState) {
        this.state = newState;
        notifyListeners();
    }

    public T getState() { return state; }

    public void subscribe(Consumer<T> listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (var listener : listeners) {
            listener.accept(state);
        }
    }
}
```

**Session State** (persists across page navigation):

```java
public class GUISession {
    private final UUID playerId;
    private final Map<String, Object> sessionData = new ConcurrentHashMap<>();

    public void set(String key, Object value) {
        sessionData.put(key, value);
    }

    public <T> T get(String key, Class<T> type) {
        return type.cast(sessionData.get(key));
    }

    public void clear() {
        sessionData.clear();
    }
}
```

**Example Usage** (Help page remembers last category):

```java
// In HelpPage.java
public void onOpen(GUISession session) {
    String lastCategory = session.get("help.lastCategory", String.class);
    if (lastCategory != null) {
        selectCategory(lastCategory);
    }
}

public void onCategorySelected(String categoryId) {
    session.set("help.lastCategory", categoryId);
    renderCategory(categoryId);
}
```

---

### B.0.9 Event Binding Patterns

**Standard Button Event**:
```java
events.addEventBinding()
    .setPathGlob("**/button_id")
    .setEventData(EventData.of("Button", "Action").append("action", "claim"))
    .setHandler(this::handleButtonClick);
```

**NavBar Navigation**:
```java
events.addEventBinding()
    .setPathGlob("**/NavBar/**")
    .setEventData(EventData.of("Button", "Nav").append("NavBar", "${pageId}"))
    .setHandler(this::handleNavigation);
```

**Expandable Card**:
```java
events.addEventBinding()
    .setPathGlob("**/ExpandButton")
    .setEventData(EventData.of("Button", "Expand").append("cardId", "${cardId}"))
    .setHandler(this::handleExpand);
```

**Modal Confirm**:
```java
events.addEventBinding()
    .setPathGlob("**/Modal/ConfirmBtn")
    .setEventData(EventData.of("Button", "Confirm").append("modal", "kick"))
    .setHandler(this::handleModalConfirm);
```

---

### B.0.10 Cross-Plugin Sharing (Future)

**Option 1: Shared HyperUI Library**
Create a separate `HyperUI` project that all plugins depend on:
```
HyperUI/
├── src/
│   └── com.hyperui/
│       ├── components/
│       ├── layouts/
│       └── state/
├── resources/ui/common/
└── build.gradle
```

Plugins depend on it:
```gradle
dependencies {
    implementation files('../HyperUI/build/libs/HyperUI-1.0.0.jar')
}
```

**Option 2: Copy-Paste Common Templates**
Each plugin copies the `common/` UI templates. Less elegant but works if HyperUI library is too much overhead.

**Option 3: Git Submodule**
Create HyperUI as a git submodule that each plugin includes.

**Recommendation**: Start with Option 2 (copy-paste) for speed, migrate to Option 1 (library) when patterns stabilize.

---

### B.0.11 Design Decisions (Resolved)

The following design decisions have been finalized:

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Visual Style** | Match Hytale Default | Consistency with native UI, less visual friction for players |
| **Animations** | No animations | Best performance, instant feedback, no delay between actions |
| **Loading States** | Progressive | Show available content immediately, load remaining async |
| **Error Display** | Chat + Inline (hybrid) | Based on Hytale API research (see below) |

---

#### Error Handling Implementation

**Research Finding**: Hytale's CustomUI does not have built-in toast notification components. The established pattern is:

1. **Chat-based feedback** for action results:
   ```java
   // Success
   player.sendMessage(Message.raw("✓ Action completed").color("#55FF55"));

   // Error
   player.sendMessage(Message.raw("✗ " + e.getMessage()).color("#FF5555"));
   ```

2. **Inline validation** using `.Visible` property:
   ```java
   // Show/hide validation message in template
   cmd.set("#NameError.Visible", "true");
   cmd.set("#NameError.Text", "Name already taken");
   ```

3. **Page behavior on errors**:
   - Validation errors: Keep page open for retry
   - Unexpected errors: Log error, show chat message, optionally close page

**Recommended Error Flow**:

```
┌─────────────────────────────────────────────────────────────────────┐
│  User Action (e.g., Create Faction)                                 │
└─────────────────────────────────────────────────────────────────────┘
                               ↓
                    ┌────────────────────┐
                    │  Validate Input    │
                    └────────────────────┘
                         ↓           ↓
                    [Valid]      [Invalid]
                         ↓           ↓
              ┌──────────────┐  ┌─────────────────────────┐
              │ Execute      │  │ Show inline error       │
              │ Action       │  │ #NameError.Visible=true │
              └──────────────┘  │ Keep page open          │
                    ↓           └─────────────────────────┘
         ┌──────────┴──────────┐
    [Success]              [Error]
         ↓                     ↓
┌──────────────────┐  ┌──────────────────┐
│ Chat: ✓ Success  │  │ Chat: ✗ Error    │
│ Navigate away    │  │ Keep page open   │
└──────────────────┘  └──────────────────┘
```

**Template Pattern for Inline Errors**:

```
// In create_faction.ui
Group #NameInputGroup {
  TextInput #NameInput { ... }
  Label #NameError {
    Visible: false;
    Style: (TextColor: #FF5555);
    @Text = "";
  }
}
```

```java
// In CreateFactionPage.java
private void validateName(String name) {
    if (name.length() < 3) {
        cmd.set("#NameError.Visible", "true");
        cmd.set("#NameError.Text", "Name must be at least 3 characters");
        return false;
    }
    if (factionManager.exists(name)) {
        cmd.set("#NameError.Visible", "true");
        cmd.set("#NameError.Text", "Faction name already taken");
        return false;
    }
    cmd.set("#NameError.Visible", "false");
    return true;
}
```

---

#### Pending Questions (Lower Priority)

These decisions are deferred until implementation:

| Question | Options | Notes |
|----------|---------|-------|
| **Sound Feedback** | None / Minimal / Full | Depends on Hytale audio API availability |
| **Responsive Behavior** | Fixed / Scaled / Adaptive | Depends on Hytale screen size detection |
| **Accessibility** | High contrast, larger text, keyboard nav | Depends on Hytale accessibility support |

---

## B.1 New Player GUI

**Target Audience**: Players not currently in a faction.

**Access**: `/f` (when not in faction), `/f menu`, `/f start`

**Nav Bar**: `BROWSE` | `CREATE` | `INVITES` | `HELP`

> **Note**: BROWSE is first (default landing) to encourage faction discovery.

---

### B.1.1 Browse Factions Page (Default Landing)

> **STATUS: PARTIALLY IMPLEMENTED** (Basic version)
> - Paginated list (8 factions per page) with sort buttons (power/members/name)
> - Faction cards show: name, member count, power, claim count
> - VIEW button opens faction info in chat (placeholder for detail page)
> - Highlights viewer's own faction with "(Your Faction)" indicator
> - **NOT IMPLEMENTED**: Search input, expandable cards, JOIN/REQUEST buttons

**Design Decisions**:
- **List Loading**: Pagination (research shows `LayoutMode: TopScrolling` builds all items at once - infinite scroll would cause page rebuild)
- **Card Display**: Simple flat cards showing core stats with VIEW button
- **Sort Method**: Three clickable text buttons (#SortPower, #SortMembers, #SortName) instead of dropdown

#### Browse Page Components

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
| `#FactionName` | Text | Faction name (in card) |
| `#MemberCount` | Text | "N members" (in card) |
| `#PowerCount` | Text | "N power" (in card) |
| `#ClaimCount` | Text | "N claims" (in card) |
| `#OwnIndicator` | Text | "(Your Faction)" if viewer's faction |
| `#ViewBtn` | Button | Opens faction info in chat |
| `#PrevBtn` | Button | Previous page navigation |
| `#NextBtn` | Button | Next page navigation |
| `#PageInfo` | Text | "page/total" indicator |

**Technical Notes**:
- Uses `cmd.append("#FactionCard" + i, "HyperFactions/faction_card.ui")` pattern
- Sort buttons trigger `EventData.of("Button", "Sort").append("SortMode", mode)`
- VIEW button sends `EventData.of("Button", "ViewFaction").append("FactionId", uuid.toString())`
- Currently VIEW shows faction info in chat; future enhancement: detail modal

**Future Enhancements** (Not Yet Implemented):
- Search input with `#SearchInput` and `#SearchBtn`
- Expandable cards showing description, leader, relations
- JOIN button for open factions
- REQUEST JOIN button for invite-only factions
- Server stats footer

---

### B.1.2 Create Faction Page (Single Page Form)

**Wireframe**:
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
│   │                                                             │   │
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

### B.1.3 My Invites Page

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
│   │  📨 Dragons                                                 │   │
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
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  📨 The Ironclad                                            │   │
│   │  ───────────────────────────────────────────────────────────│   │
│   │  Invited by: IronMaster                                     │   │
│   │  Sent: 2 hours ago                                          │   │
│   │  Expires: in 22 hours                                       │   │
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
| `#InfoBtn` | Button | Show faction details (same as browser expand) |
| `#EmptyState` | Container | Shown when no invites, links to Browse |

**Behaviors**:
- Accept: Join faction immediately, close GUI, show Dashboard
- Decline: Remove invite, show confirmation
- Info: Expand card to show full faction details
- Auto-refresh: Poll for new invites while page open

---

### B.1.4 Help Page (New Player Context)

See **Phase C** for full Help System specification. New Player context shows:
- "Getting Started" (highlighted)
- "Faction Member Guide" (preview what's possible after joining)
- Links to external documentation

---

## B.2 Faction Player GUI

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

### B.2.1 Faction Dashboard (Default Landing)

> **STATUS: NOT IMPLEMENTED** (Redesigned)
> - New layout with faction identity banner, stat cards, quick actions, and activity feed
> - Chat toggles for faction and ally chat
> - Leave confirmation modal following disband_confirm pattern

#### Dashboard Layout

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

#### Dashboard Components

| Component | File | Status | Description |
|-----------|------|--------|-------------|
| **Main Page** | `faction_dashboard.ui` | TODO | Full dashboard layout with identity, stats, actions, activity |
| **Activity Entry** | `activity_entry.ui` | TODO | Activity log entry template |
| **Leave Confirm** | `leave_confirm.ui` | TODO | Leave confirmation modal |

#### Element Breakdown

| Element ID | Type | Description |
|------------|------|-------------|
| `#FactionName` | Text | Faction name (large, colored) |
| `#FactionTag` | Text | Tag in brackets "[DRG]" |
| `#FactionDescription` | Text | Description text |
| `#PowerCurrent` | Text | Current power value |
| `#PowerMax` | Text | Maximum power value |
| `#PowerPercent` | Text | Power percentage |
| `#ClaimsCurrent` | Text | Current claims count |
| `#ClaimsMax` | Text | Maximum claims allowed |
| `#ClaimsAvailable` | Text | Available claims remaining |
| `#MembersTotal` | Text | Total member count |
| `#MembersOnline` | Text | Online member count |
| `#HomeBtn` | Button | Teleport to faction home |
| `#ClaimBtn` | Button | Claim current chunk |
| `#FactionChatBtn` | Button | Toggle faction chat (shows ON/OFF state via text) |
| `#AllyChatBtn` | Button | Toggle ally chat (shows ON/OFF state via text) |
| `#LeaveBtn` | Button | Opens leave confirmation modal |
| `#ActivityList` | Container | Scrollable recent activity entries |

#### Chat Toggle Pattern

Since `cmd.set()` only works for `.Text`:
```java
if (isFactionChatEnabled) {
    cmd.set("#FactionChatBtn.Text", "F-CHAT ON");
} else {
    cmd.set("#FactionChatBtn.Text", "F-CHAT OFF");
}
```

**Chat Toggles - Future Feature:**
The F-CHAT and A-CHAT toggle buttons will be included in the wireframe but noted as requiring:
- ChatManager integration (to be designed)
- Per-player chat mode tracking
- Message routing based on chat mode

For now, buttons show static "F-CHAT OFF" / "A-CHAT OFF" text with no functionality.

#### Leave Confirmation Modal

Following `disband_confirm.ui` pattern:
```
+-----------------------------------------------+
|              LEAVE FACTION                    |
+-----------------------------------------------+
|   Are you sure you want to leave              |
|                                               |
|                Dragons                        |
|              (faction name)                   |
|                                               |
|   You will lose access to faction territory   |
|   and resources.                              |
|                                               |
|         [CANCEL]          [LEAVE]             |
+-----------------------------------------------+
```

#### Activity Log - Event Tracking System

**Data Model Addition:**
```java
public record FactionEvent(
    long timestamp,
    FactionEventType type,
    String description,
    @Nullable UUID actorUuid,
    @Nullable String actorName
) {}

public enum FactionEventType {
    MEMBER_JOIN, MEMBER_LEAVE, MEMBER_KICK,
    MEMBER_PROMOTE, MEMBER_DEMOTE,
    CHUNK_CLAIM, CHUNK_UNCLAIM,
    RELATION_ALLY, RELATION_ENEMY, RELATION_NEUTRAL,
    HOME_SET, SETTINGS_CHANGED
}
```

**Storage:** Add `List<FactionEvent> recentEvents` to Faction record (keep last ~50 events)

**Events to track:**
- Player joined/left/kicked from faction
- Player promoted/demoted
- Chunk claimed/unclaimed
- Relations changed (ally/enemy established)
- Faction home set
- Settings changed (name, tag, description, color, recruitment)

---

### B.2.2 Members Page

> **STATUS: NOT IMPLEMENTED**
> - Paginated member list (8 per page) sorted by role level then username
> - Each entry shows: username, role, last online time
> - Role-based action buttons: PROMOTE, DEMOTE, KICK, TRANSFER
> - Pagination with < > buttons

#### Members Page Components

| Component | File | Status | Description |
|-----------|------|--------|-------------|
| **Main Page** | `faction_members.ui` | **DONE** | `#MemberEntry0`-`#MemberEntry7` slots, pagination controls |
| **Member Entry** | `member_entry.ui` | **DONE** | Name, role, last online, action button areas |

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
│   ... (up to 8 per page)                                                       │
│                                                                                │
│       [<]                         1/1                            [>]          │
│                                                                                │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                     [ESC] Back                 │
└────────────────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown** (Implemented):

| Element ID | Type | Description |
|------------|------|-------------|
| `#MemberCount` | Text | "N members" total count |
| `#MemberEntry0`-`#MemberEntry7` | Container | 8 slots for member entry templates |
| `#MemberName` | Text | Member username (in entry) |
| `#MemberRole` | Text | Role name: LEADER, OFFICER, MEMBER |
| `#LastOnline` | Text | "Online now" or "X ago" (uses TimeUtil.formatDuration) |
| `#PromoteBtn` | Button | Promote member to officer |
| `#DemoteBtn` | Button | Demote officer to member |
| `#KickBtn` | Button | Remove from faction |
| `#TransferBtn` | Button | Transfer leadership |
| `#PrevBtn` | Button | Previous page |
| `#NextBtn` | Button | Next page |
| `#PageInfo` | Text | "page/total" |

**Permission Logic** (Implemented):
```java
boolean canManageThis = canManage && !isSelf &&
    viewerRole.getLevel() > member.role().getLevel();
```

- **Members**: See list only, no action buttons
- **Officers**: See PROMOTE + KICK for members (not self, not other officers)
- **Leader**: See all buttons for everyone except self

**Button Behaviors** (Implemented):
- **PROMOTE**: `factionManager.promoteMember()` -> refreshes page
- **DEMOTE**: `factionManager.demoteMember()` -> refreshes page
- **KICK**: `factionManager.removeMember()` -> refreshes page
- **TRANSFER**: Closes GUI, shows `/f transfer {name}` instruction in chat

**NOT IMPLEMENTED** (Future enhancements):
- INVITE button with player search modal
- Online status indicators (green/red dots)
- Power display per member
- Join date display

---

### B.2.3 Browse Factions Page (Faction Players)

> **STATUS: NOT IMPLEMENTED**
> - Browse factions with relation indicators for faction members
> - Shows relation to each faction (ALLY, ENEMY, NEUTRAL)
> - No JOIN/REQUEST buttons (already in a faction)
> - VIEW button shows faction info in chat

#### Browse Layout (Faction Players)

```
+--------------------------------------------------------------------------------+
|   DASHBOARD   MEMBERS  [BROWSE]   MAP   RELATIONS   SETTINGS                   |
+--------------------------------------------------------------------------------+
|                                                                                |
|   BROWSE FACTIONS                                          12 factions         |
|                                                                                |
|   Sort by:  [POWER]  [MEMBERS]  [NAME]                                         |
|                                                                                |
|   +------------------------------------------------------------------------+  |
|   |  Phoenix Rising                                      [ALLY]            |  |
|   |  12 members  |  140 power  |  18 claims                       [VIEW]   |  |
|   +------------------------------------------------------------------------+  |
|   +------------------------------------------------------------------------+  |
|   |  Shadow Collective                                   [ENEMY]           |  |
|   |  6 members  |  80 power  |  12 claims                         [VIEW]   |  |
|   +------------------------------------------------------------------------+  |
|   +------------------------------------------------------------------------+  |
|   |  Dragons                                        (Your Faction)         |  |
|   |  8 members  |  156 power  |  23 claims                        [VIEW]   |  |
|   +------------------------------------------------------------------------+  |
|   +------------------------------------------------------------------------+  |
|   |  Iron Legion                                         [NEUTRAL]         |  |
|   |  10 members  |  100 power  |  15 claims                       [VIEW]   |  |
|   +------------------------------------------------------------------------+  |
|   ... (8 per page)                                                            |
|                                                                                |
|       [<]                         1/2                           [>]           |
+--------------------------------------------------------------------------------+
```

#### Key Differences from Non-Faction Browse (B.1.1)

| Feature | Non-Faction Browse (B.1.1) | Faction Browse (B.2.3) |
|---------|---------------------------|------------------------|
| Relation Indicator | None | [ALLY], [ENEMY], [NEUTRAL] badges |
| Own Faction | N/A | "(Your Faction)" indicator |
| Action Buttons | JOIN, REQUEST JOIN | VIEW only |
| Purpose | Find faction to join | View server factions, see relations |

#### Browse Components

| Component | File | Status | Description |
|-----------|------|--------|-------------|
| **Main Page** | `faction_browser.ui` | REUSE | Same template as B.1.1, with indicator slots |
| **Faction Card** | `faction_card.ui` | MODIFY | Add `#IndicatorSlot` container for relation badges |
| **Ally Indicator** | `indicator_ally.ui` | TODO | "[ALLY]" badge (blue text) |
| **Enemy Indicator** | `indicator_enemy.ui` | TODO | "[ENEMY]" badge (red text) |
| **Neutral Indicator** | `indicator_neutral.ui` | TODO | "[NEUTRAL]" badge (gray text) |

#### Element Breakdown

| Element ID | Type | Description |
|------------|------|-------------|
| `#FactionCount` | Text | "N factions" total count |
| `#SortPower` | Button | Sort by power descending |
| `#SortMembers` | Button | Sort by member count descending |
| `#SortName` | Button | Sort alphabetically by name |
| `#FactionCard0`-`#FactionCard7` | Container | 8 slots for faction card templates |
| `#FactionName` | Text | Faction name (in card) |
| `#MemberCount` | Text | "N members" (in card) |
| `#PowerCount` | Text | "N power" (in card) |
| `#ClaimCount` | Text | "N claims" (in card) |
| `#IndicatorSlot` | Container | Container for relation indicator template |
| `#OwnIndicator` | Text | "(Your Faction)" if viewer's faction |
| `#ViewBtn` | Button | Opens faction info in chat |
| `#PrevBtn` | Button | Previous page navigation |
| `#NextBtn` | Button | Next page navigation |
| `#PageInfo` | Text | "page/total" indicator |

#### Relation Indicator Implementation

Use conditional template appending (since colors can't be set dynamically):
```java
String prefix = "#FactionCard" + i;
Faction targetFaction = factions.get(i);

if (viewerFaction.isAlly(targetFaction)) {
    cmd.append(prefix + "#IndicatorSlot", "HyperFactions/indicator_ally.ui");
} else if (viewerFaction.isEnemy(targetFaction)) {
    cmd.append(prefix + "#IndicatorSlot", "HyperFactions/indicator_enemy.ui");
} else if (targetFaction.equals(viewerFaction)) {
    // Own faction - show "(Your Faction)" via #OwnIndicator
    cmd.set(prefix + "#OwnIndicator.Visible", "true");
} else {
    cmd.append(prefix + "#IndicatorSlot", "HyperFactions/indicator_neutral.ui");
}
```

#### Technical Notes

- Reuses most of the `faction_browser.ui` template from B.1.1
- `faction_card.ui` needs modification to add `#IndicatorSlot` container
- Indicator templates contain styled text with preset colors (workaround for dynamic color limitation)
- VIEW button behavior same as B.1.1 - shows faction info in chat

---

### B.2.4 Territory Map Page

> **STATUS: IMPLEMENTED** (2026-01-25)
> - 29x17 interactive chunk grid (GRID_RADIUS_X=14, GRID_RADIUS_Z=8) with click-to-claim (wilderness) / right-click-to-unclaim (own territory)
> - Color-coded ownership (own, ally, enemy, neutral, safe zone, war zone, wilderness)
> - Dynamic claim stats showing "Claims: X/Y (Z Available)" with power status
> - Overclaim warning when power < claims
> - Officer+ can left-click enemy territory to attempt overclaim
> - **Note**: "Set Faction Home" button removed - use Settings page instead
> - **Technical Discovery**: Hytale uses 32-block chunks (not 16-block like Minecraft)

#### Territory Map Components

| Component | File | Status | Description |
|-----------|------|--------|-------------|
| **Main Page** | `chunk_map.ui` | **DONE** | 29x17 grid container, position info, legend (7 colors), action hints, claim/power stats |
| **Chunk Button** | `chunk_btn.ui` | **DONE** | Invisible button overlay for click detection on each chunk cell |
| **Player Chunk** | `chunk_btn_player.ui` | **DONE** | Special styling for player's current position (white cell) |

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

**Technical Implementation:**
- Grid built dynamically via `cmd.appendInline()` with colors baked into template
- Uses `ChunkUtil.java` for coordinate conversion (32-block chunks, shift 5)
- Event bindings on `#Btn` elements within each cell for click detection
- No modals - direct click actions (claim/unclaim/overclaim) with chat feedback
- Cell size: 16 pixels per cell

**Architecture** (Implemented):
```java
public class ChunkMapPage extends InteractiveCustomUIPage<ChunkMapData> {
    private static final int GRID_RADIUS_X = 14; // 29 columns (-14 to +14)
    private static final int GRID_RADIUS_Z = 8;  // 17 rows (-8 to +8)
    private static final int CELL_SIZE = 16;     // pixels per cell

    // Colors
    private static final String COLOR_OWN = "#4ade80";
    private static final String COLOR_ALLY = "#60a5fa";
    private static final String COLOR_ENEMY = "#f87171";
    private static final String COLOR_OTHER = "#fbbf24";
    private static final String COLOR_WILDERNESS = "#1e293b";
    private static final String COLOR_SAFEZONE = "#2dd4bf";
    private static final String COLOR_WARZONE = "#c084fc";
    private static final String COLOR_PLAYER_POS = "#ffffff";
}
```

**Wireframe** (Actual 29x17 grid - simplified view):
```
┌──────────────────────────────────────────────────────────────────────────────┐
│   DASHBOARD   MEMBERS   BROWSE  [MAP]  RELATIONS   SETTINGS                  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Your Position: Chunk (120, 340)                                            │
│                                                                              │
│   ┌────────────────────────────────────────────────────────────────────────┐ │
│   │ . . . . . . . . . . . . . . . . . . . . . . . . . . . . .              │ │
│   │ . . . . . . . . . A A . . . . . . . . . . . . . . . . . .              │ │
│   │ . . . . . . . . A ■ ■ . . . . . . . . . . . . . . . . . .              │ │
│   │ . . . . . . E E ■ ■ ■ ■ . . . . . . . . . . . . . . . . .              │ │
│   │ . . . . . . E E ■[⬜]■ ■ . . . . . . . . . . . . . . . . .  LEGEND:    │ │
│   │ . . . . . . . . ■ ■ ■ . . . . . . . . . . . . . . . . . .  ■ = Own    │ │
│   │ . . . . . . . . . ■ . . . . . . . . . . . . . . . . . . .  A = Ally   │ │
│   │ . . . . . . . . . . . . . . . . . . . . . . . . . . . . .  E = Enemy  │ │
│   │ . . . . . . . . . . . . . . . . . . . . . . . . . . . . .  . = Wild   │ │
│   │                                                            S = Safe   │ │
│   │                                                            W = War    │ │
│   └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│   Claims: 23/78 (55 Available)                Power: 120/200                 │
│   -- or if overclaimed: --                                                   │
│   Claims: 23/78 (55 Available)                OVERCLAIMED by 5!              │
│                                                                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                [ESC] Back                    │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Interaction Flow** (Implemented):
1. Player opens map - grid renders centered on player position
2. **Direct click actions** (no selection step, officers only):
   - **Left-click wilderness**: Claim chunk immediately
   - **Right-click own territory**: Unclaim chunk immediately
   - **Left-click enemy territory**: Attempt overclaim (if enemy is overclaimed)
   - **Ally/Safezone/Warzone**: No click action
3. Map refreshes after each claim/unclaim action
4. Chat messages confirm success or explain failure

**Event Binding** (Implemented):
```java
switch (info.type) {
    case WILDERNESS:
        // Left-click wilderness to claim
        events.addEventBinding(CustomUIEventBindingType.Activating, cellSelector,
            EventData.of("Button", "Claim").append("ChunkX", ...).append("ChunkZ", ...), false);
        break;
    case OWN:
        // Right-click own territory to unclaim
        events.addEventBinding(CustomUIEventBindingType.RightClicking, cellSelector,
            EventData.of("Button", "Unclaim").append("ChunkX", ...).append("ChunkZ", ...), false);
        break;
    case ENEMY:
        // Left-click enemy territory to attempt overclaim
        events.addEventBinding(CustomUIEventBindingType.Activating, cellSelector,
            EventData.of("Button", "Overclaim").append("ChunkX", ...).append("ChunkZ", ...), false);
        break;
}
```

**Element Breakdown** (Implemented):

| Element ID | Type | Description |
|------------|------|-------------|
| `#ChunkGrid` | Container | Container for chunk rows (built dynamically) |
| `#PositionInfo` | Text | Player's current chunk coordinates |
| `#ClaimStats` | Text | "Claims: X/Y (Z Available)" or "Join a faction to claim" |
| `#PowerStatus` | Text | "Power: X/Y" or "OVERCLAIMED by N!" |

**Template Files**:
- `chunk_map.ui` - Main map page layout with grid container, stats areas
- `chunk_btn.ui` - Invisible button overlay for each cell
| WarZone | `#a855f7` | 11031031 | (custom) |
| Wilderness | `#374151` | 3621201 | `--hs-bg-tertiary` |
| Selected | `#ffffff` | - | White border |
| Player Position | `#22c55e80` | - | Semi-transparent green |

**Dynamic Grid Generation**:
```java
private void buildChunkGrid(UICommandBuilder cmd, UIEventBuilder events) {
    int radius = 4;  // 9x9 grid

    for (int z = 0; z <= radius * 2; z++) {
        // Create row container
        cmd.appendInline("#ChunkCards", "Group { LayoutMode: Left; }");

        for (int x = 0; x <= radius * 2; x++) {
            int chunkX = centerX - radius + x;
            int chunkZ = centerZ - radius + z;

            // Append cell template
            cmd.append("#ChunkCards[" + z + "]", "faction/chunk_cell.ui");

            // Set color based on ownership
            ChunkOwnership ownership = getChunkOwnership(chunkX, chunkZ);
            cmd.set("#ChunkCards[" + z + "][" + x + "].Background",
                    "Solid { Color: " + ownership.color() + "; }");

            String selector = "#ChunkCards[" + z + "][" + x + "]";

            // Bind click events based on ownership
            if (ownership.isWilderness()) {
                // Left-click wilderness to claim
                events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector,
                    EventData.of("Action", "Claim:" + chunkX + ":" + chunkZ),
                    false
                );
            } else if (ownership.isOwnFaction()) {
                // Right-click own territory to unclaim
                events.addEventBinding(
                    CustomUIEventBindingType.RightClicking,
                    selector,
                    EventData.of("Action", "Unclaim:" + chunkX + ":" + chunkZ),
                    false
                );
            }
            // Ally/enemy/zone chunks have no click actions

            // Setup tooltip with action hints
            setupChunkTooltip(cmd, z, x, chunkX, chunkZ, ownership);
        }
    }
}
```

**Event Handling**:
```java
@Override
public void handleDataEvent(Ref ref, Store store, MapData data) {
    String[] parts = data.action.split(":");
    int chunkX = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
    int chunkZ = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

    switch (parts[0]) {
        case "Claim" -> {
            claimChunk(chunkX, chunkZ);
            rebuildAndSend(ref, store);
        }
        case "Unclaim" -> {
            unclaimChunk(chunkX, chunkZ);
            rebuildAndSend(ref, store);
        }
        case "SetHome" -> {
            setFactionHome();
            player.sendMessage(Message.raw("Faction home set!").color("#22c55e"));
        }
    }
}
```

**Element Breakdown**:

| Element ID | Type | Description |
|------------|------|-------------|
| `#MapGrid` | Container | Container for chunk grid |
| `#ChunkCards` | Dynamic | Rows and cells generated via `appendInline()` |
| `#CurrentCoords` | Text | Player's current chunk coordinates |
| `#ActionHint` | Text | "Left-click: Claim / Right-click: Unclaim" |
| `#SetHomeBtn` | Button | Set faction home at current location |
| `#Legend` | Container | Color legend |
| `#ClaimStats` | Text | Current claims / max with available count |

**Template Files**:
- `faction/map.ui` - Main map page layout
- `faction/chunk_cell.ui` - Single chunk cell (32x32)

**Interaction Flow**:
1. Player opens map - grid renders centered on player position
2. Hover over chunk - tooltip shows ownership and available action
3. **Direct click actions** (no selection step):
   - **Left-click wilderness**: Claim chunk immediately
   - **Right-click own territory**: Unclaim chunk immediately
   - **Enemy/Ally/Zone**: No click action (tooltip shows info only)
4. Set Home button sets faction home at player's current chunk
5. Map always centers on player position (reopening updates view)

**Tooltip Content**:
- Wilderness: "Wilderness - Left-click to claim"
- Own territory: "[Faction Name] - Right-click to unclaim"
- Ally: "[Faction Name] (Ally) - Protected"
- Enemy: "[Faction Name] (Enemy) - Protected" (or "Raidable!" if overclaim possible)
- SafeZone/WarZone: "[Zone Name] - Protected"

---

### B.2.5 Relations Page

> **STATUS: IMPLEMENTED** (2026-01-25)
> - Three sections visible at once: Allies, Enemies, Pending Requests (no tabs)
> - Each section shows count in header (e.g., "ALLIES (2)")
> - Relation entries show faction name, leader, date established, type badge, action buttons
> - Officers+ can: set neutral, set enemy, request ally, accept/decline requests
> - "+ SET RELATION" button visible only for officers+
> - Set Relation modal for searching and setting relations with other factions
> - **Technical Note**: Uses sectioned layout with dynamic `cmd.append()` for entries

#### Relations Page Components

| Component | File | Status | Description |
|-----------|------|--------|-------------|
| **Main Page** | `faction_relations.ui` | **DONE** | Sectioned layout with `#AlliesList`, `#EnemiesList`, `#RequestsList` containers |
| **Relation Entry** | `relation_entry.ui` | **DONE** | Faction name, leader name, date established, type badge, `#ButtonsContainer` |
| **Empty State** | `relation_empty.ui` | **DONE** | `#EmptyText` message template |
| **Neutral Button** | `relation_btn_neutral.ui` | **DONE** | `#NeutralBtn` - sets relation to neutral |
| **Ally Button** | `relation_btn_ally.ui` | **DONE** | `#AllyBtn` - requests alliance |
| **Enemy Button** | `relation_btn_enemy.ui` | **DONE** | `#EnemyBtn` - declares enemy |
| **Accept Button** | `relation_btn_accept.ui` | **DONE** | `#AcceptBtn` - accepts ally request |
| **Decline Button** | `relation_btn_decline.ui` | **DONE** | `#DeclineBtn` - declines ally request |
| **Set Relation Button** | `relation_set_btn.ui` | **DONE** | `#SetRelationBtn` in `#ActionBtnContainer` (officers+ only) |

#### Set Relation Modal

| Modal | File | Status | Description |
|-------|------|--------|-------------|
| **Set Relation** | `set_relation_modal.ui` | **DONE** | Search input with `#SearchBtn`, `#ResultsList` container, `#PrevBtn`/`#NextBtn` pagination, `#PageInfo` |
| **Faction Card** | `set_relation_card.ui` | **DONE** | Faction name, leader, power, member count, `#AllyBtn`, `#EnemyBtn`, `#ViewBtn` |

**Set Relation Modal Features:**
- Search by faction name or tag (4 factions per page)
- Results show: faction name, leader, power, member count
- Each result has: ALLY (request), ENEMY (declare), VIEW (info to chat) buttons
- Pagination with < > buttons and "page/total" indicator
- Empty state: "Search for a faction to set relation" or "No factions found matching 'query'"
- Results sorted by power (highest first)

**Wireframe** (Implemented):
```
┌───────────────────────────────────────────────────────────────────────────────┐
│   DASHBOARD   MEMBERS   BROWSE   MAP  [RELATIONS]  SETTINGS                   │
├───────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│   DIPLOMATIC RELATIONS                                  [+ SET RELATION]      │
│                                                        (officers+ only)       │
│                                                                               │
│   ═══════════════════════════════════════════════════════════════════════════ │
│   ALLIES (2)                                                                  │
│   ═══════════════════════════════════════════════════════════════════════════ │
│   ┌─────────────────────────────────────────────────────────────────────────┐ │
│   │  Phoenix Rising              [ALLY]                                     │ │
│   │  Leader: PhoenixKing                                                    │ │
│   │  Since: 15 days ago                            [NEUTRAL]  [ENEMY]       │ │
│   └─────────────────────────────────────────────────────────────────────────┘ │
│   ┌─────────────────────────────────────────────────────────────────────────┐ │
│   │  The Ironclad                [ALLY]                                     │ │
│   │  Leader: IronMaster                                                     │ │
│   │  Since: 3 days ago                             [NEUTRAL]  [ENEMY]       │ │
│   └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                               │
│   ═══════════════════════════════════════════════════════════════════════════ │
│   ENEMIES (1)                                                                 │
│   ═══════════════════════════════════════════════════════════════════════════ │
│   ┌─────────────────────────────────────────────────────────────────────────┐ │
│   │  Shadow Collective           [ENEMY]                                    │ │
│   │  Leader: ShadowKing                                                     │ │
│   │  Since: 20 days ago                            [NEUTRAL]  [ALLY]        │ │
│   └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                               │
│   ═══════════════════════════════════════════════════════════════════════════ │
│   PENDING REQUESTS (1)                                                        │
│   ═══════════════════════════════════════════════════════════════════════════ │
│   ┌─────────────────────────────────────────────────────────────────────────┐ │
│   │  Storm Legion                [PENDING]                                  │ │
│   │  Leader: StormKing                                                      │ │
│   │  Requested recently                            [ACCEPT]  [DECLINE]      │ │
│   └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                               │
├───────────────────────────────────────────────────────────────────────────────┤
│                                                    [ESC] Back                 │
└───────────────────────────────────────────────────────────────────────────────┘
```

**Set Relation Modal Wireframe**:
```
┌───────────────────────────────────────────────────────────────────────┐
│                        SET RELATION                                   │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│   ┌─────────────────────────────────────────┐  ┌──────────┐           │
│   │  Search factions...                     │  │  SEARCH  │           │
│   └─────────────────────────────────────────┘  └──────────┘           │
│                                                                       │
│   ┌─────────────────────────────────────────────────────────────────┐ │
│   │  Phoenix Empire                                                 │ │
│   │  Leader: PhoenixKing  │  150 power  │  12 members               │ │
│   │  [ALLY]  [ENEMY]  [VIEW]                                        │ │
│   └─────────────────────────────────────────────────────────────────┘ │
│   ┌─────────────────────────────────────────────────────────────────┐ │
│   │  Iron Legion                                                    │ │
│   │  Leader: IronMaster  │  120 power  │  8 members                 │ │
│   │  [ALLY]  [ENEMY]  [VIEW]                                        │ │
│   └─────────────────────────────────────────────────────────────────┘ │
│   ┌─────────────────────────────────────────────────────────────────┐ │
│   │  Storm Raiders                                                  │ │
│   │  Leader: StormChief  │  80 power  │  5 members                  │ │
│   │  [ALLY]  [ENEMY]  [VIEW]                                        │ │
│   └─────────────────────────────────────────────────────────────────┘ │
│   ┌─────────────────────────────────────────────────────────────────┐ │
│   │  Shadow Cult                                                    │ │
│   │  Leader: ShadowLord  │  60 power  │  4 members                  │ │
│   │  [ALLY]  [ENEMY]  [VIEW]                                        │ │
│   └─────────────────────────────────────────────────────────────────┘ │
│                                                                       │
│       [<]                    1/3                           [>]        │
│                                                                       │
│                         [CANCEL]                                      │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown** (Implemented):

| Element ID | Type | Description |
|------------|------|-------------|
| `#AlliesHeader` | Text | "ALLIES (N)" section header |
| `#AlliesList` | Container | List of allied factions (entries appended dynamically) |
| `#EnemiesHeader` | Text | "ENEMIES (N)" section header |
| `#EnemiesList` | Container | List of enemy factions |
| `#RequestsHeader` | Text | "PENDING REQUESTS (N)" section header |
| `#RequestsList` | Container | Incoming ally requests |
| `#ActionBtnContainer` | Container | Container for "+ SET RELATION" button (officers+ only) |
| `#FactionName` | Text | Faction name in entry |
| `#LeaderName` | Text | "Leader: {name}" in entry |
| `#DateEstablished` | Text | "Since: X days ago" or "Requested recently" |
| `#RelationType` | Text | Type badge ("ALLY", "ENEMY", "PENDING") |
| `#ButtonsContainer` | Container | Action buttons (dynamically appended based on entry type) |

**Permission-Based Button Logic**:
- **Allies**: NEUTRAL + ENEMY buttons (for officers+)
- **Enemies**: NEUTRAL + ALLY buttons (for officers+)
- **Pending Requests**: ACCEPT + DECLINE buttons (for officers+)
- **Members**: No buttons visible (read-only view)

---

### B.2.6 Settings Page (Officer+)

> **STATUS: IMPLEMENTED** (2026-01-25)
> - GENERAL section: Name, Tag, Description with EDIT buttons (each opens a modal)
> - APPEARANCE section: Color preview + hex code with CHANGE button (opens color picker)
> - RECRUITMENT section: Current status (Open/Invite Only) with CHANGE button (opens modal)
> - HOME LOCATION section: SET HOME HERE and TELEPORT buttons
>   - Teleport has warmup/cooldown and combat-tag support via TeleportManager
>   - Set home requires being in faction territory
> - MODULES section: VIEW MODULES button (opens FactionModulesPage)
> - DANGER ZONE section: DISBAND button (Leader only, conditionally appended via `settings_danger_zone.ui`)
> - Non-officers see error page: "Only officers and leaders can change faction settings."

#### Settings Page Components

| Component | File | Status | Description |
|-----------|------|--------|-------------|
| **Main Page** | `faction_settings.ui` | **DONE** | Sectioned layout with General, Appearance, Recruitment, Home, Modules sections |
| **Danger Zone** | `settings_danger_zone.ui` | **DONE** | Conditionally appended to `#DangerZoneContainer` for leaders only |
| **Error Page** | `error_page.ui` | **DONE** | Shown to non-officers with `#ErrorMessage` text |

#### Settings Page Modals (All Implemented)

| Modal | File | Status | Description |
|-------|------|--------|-------------|
| **Rename Faction** | `rename_modal.ui` | **DONE** | `#CurrentName` display, `#NameInput` text field, `#CancelBtn`/`#SaveBtn`. Validates 3-32 chars, uniqueness. |
| **Edit Tag** | `tag_modal.ui` | **DONE** | `#CurrentTag` display (shows "[TAG]"), `#TagInput` text field, `#CancelBtn`/`#SaveBtn`. Validates 1-5 chars, alphanumeric, uniqueness. |
| **Edit Description** | `description_modal.ui` | **DONE** | `#CurrentDesc` display (truncated to 100 chars), `#DescInput` text area, `#CancelBtn`/`#ClearBtn`/`#SaveBtn`. Max 256 chars. |
| **Color Picker** | `color_picker.ui` | **DONE** | `#CurrentColorPreview` (background color set dynamically), `#CurrentColorName` text, 16 color buttons `#Color0`-`#Color15` with preset TextButtonStyle, `#CancelBtn`. |
| **Recruitment Status** | `recruitment_modal.ui` | **DONE** | `#CurrentStatus` display, `#OpenBtn`, `#InviteOnlyBtn`, `#CancelBtn`. |
| **Disband Confirm** | `disband_confirm.ui` | **DONE** | `#FactionName` display (set dynamically), warning text, `#CancelBtn`/`#ConfirmBtn`. |

**Color Picker Details:**
- 16 Minecraft colors (codes 0-f) in 4x4 grid
- Each button has preset background color via TextButtonStyle (workaround for cmd.set() limitation)
- Colors: Black, Dark Blue, Dark Green, Dark Aqua, Dark Red, Dark Purple, Gold, Gray, Dark Gray, Blue, Green, Aqua, Red, Light Purple, Yellow, White
- On select: Updates faction color, shows chat message with color name

**Wireframe** (Implemented):
```
┌────────────────────────────────────────────────────────────────────────────────┐
│   DASHBOARD   MEMBERS   BROWSE   MAP   RELATIONS  [SETTINGS]                   │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                │
│   FACTION SETTINGS                                                             │
│                                                                                │
│   ═══════════════════════════════════════════════════════════════════════════  │
│   GENERAL                                                                      │
│   ═══════════════════════════════════════════════════════════════════════════  │
│                                                                                │
│   Name:        Dragons                                           [EDIT]        │
│   Description: From the ashes we rise!                           [EDIT]        │
│   Tag:         [DRG]                                             [EDIT]        │
│                                                                                │
│   ═══════════════════════════════════════════════════════════════════════════  │
│   APPEARANCE                                                                   │
│   ═══════════════════════════════════════════════════════════════════════════  │
│                                                                                │
│   Color:   [██]  #55FFFF                                        [CHANGE]       │
│                                                                                │
│   ═══════════════════════════════════════════════════════════════════════════  │
│   RECRUITMENT                                                                  │
│   ═══════════════════════════════════════════════════════════════════════════  │
│                                                                                │
│   Status:  Invite Only                                          [CHANGE]       │
│                                                                                │
│   ═══════════════════════════════════════════════════════════════════════════  │
│   HOME LOCATION                                                                │
│   ═══════════════════════════════════════════════════════════════════════════  │
│                                                                                │
│   Current:  world (120, 64, 340)                                               │
│                                                                                │
│   [SET HOME HERE]              [TELEPORT]                                      │
│                                                                                │
│   ═══════════════════════════════════════════════════════════════════════════  │
│   MODULES                                                                      │
│   ═══════════════════════════════════════════════════════════════════════════  │
│                                                                                │
│   [VIEW MODULES]                                                               │
│                                                                                │
│   ═══════════════════════════════════════════════════════════════════════════  │
│   DANGER ZONE  (Leader only - conditionally shown)                             │
│   ═══════════════════════════════════════════════════════════════════════════  │
│                                                                                │
│   [DISBAND FACTION]  (red, destructive)                                        │
│                                                                                │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                     [ESC] Back                 │
└────────────────────────────────────────────────────────────────────────────────┘
```

**Modal Wireframes:**

**Rename Modal:**
```
┌─────────────────────────────────────────────────────────────────┐
│                    RENAME FACTION                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Current Name:  Dragons                                        │
│                                                                 │
│   New Name:                                                     │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   (3-32 characters, must be unique)                             │
│                                                                 │
│               [CANCEL]              [SAVE]                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Color Picker Modal:**
```
┌─────────────────────────────────────────────────────────────────┐
│                    FACTION COLOR                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Current:  [██]  Aqua (#55FFFF)                                │
│                                                                 │
│   ┌────┬────┬────┬────┐                                         │
│   │ 0  │ 1  │ 2  │ 3  │   0=Black   1=DkBlue  2=DkGreen 3=DkAqua│
│   ├────┼────┼────┼────┤                                         │
│   │ 4  │ 5  │ 6  │ 7  │   4=DkRed   5=DkPurp  6=Gold    7=Gray  │
│   ├────┼────┼────┼────┤                                         │
│   │ 8  │ 9  │ a  │ b  │   8=DkGray  9=Blue    a=Green   b=Aqua  │
│   ├────┼────┼────┼────┤                                         │
│   │ c  │ d  │ e  │ f  │   c=Red     d=LtPurp  e=Yellow  f=White │
│   └────┴────┴────┴────┘                                         │
│                                                                 │
│                       [CANCEL]                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Disband Confirmation Modal:**
```
┌─────────────────────────────────────────────────────────────────┐
│                    DISBAND FACTION                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Are you sure you want to disband                              │
│                                                                 │
│                      Dragons                                    │
│                   (shown in red)                                │
│                                                                 │
│   This action cannot be undone!                                 │
│   All claims, members, and data will be lost.                   │
│                                                                 │
│             [CANCEL]              [DISBAND]                     │
│                                  (red button)                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Element Breakdown** (Implemented):

| Element ID | Type | Description |
|------------|------|-------------|
| `#NameValue` | Text | Current faction name |
| `#NameEditBtn` | Button | Opens rename modal |
| `#DescValue` | Text | Current description (or "(None)") |
| `#DescEditBtn` | Button | Opens description modal |
| `#TagValue` | Text | Current tag as "[TAG]" (or "(None)") |
| `#TagEditBtn` | Button | Opens tag modal |
| `#ColorPreview` | Group | Background color set to current color hex |
| `#ColorValue` | Text | Current color hex code |
| `#ColorBtn` | Button | Opens color picker |
| `#RecruitmentStatus` | Text | "Open" or "Invite Only" |
| `#RecruitmentBtn` | Button | Opens recruitment modal |
| `#HomeLocation` | Text | "world (X, Y, Z)" or "Not set" |
| `#SetHomeBtn` | Button | Sets home at current location (requires faction territory) |
| `#TeleportHomeBtn` | Button | Teleports to faction home (warmup/combat-tag) |
| `#ModulesBtn` | Button | Opens modules page |
| `#DangerZoneContainer` | Container | Danger zone appended here for leaders only |
| `#DisbandBtn` | Button | Opens disband confirmation modal (in danger_zone.ui) |

**Permission Visibility**:
- **Members**: See error page "Only officers and leaders can change faction settings."
- **Officers**: See all sections except Danger Zone
- **Leader**: See all sections including Danger Zone with DISBAND button

**Teleport Implementation**:
- Uses `TeleportManager` for warmup, cooldown, and combat-tag checking
- Closes GUI before initiating teleport
- Cross-world teleportation supported via `Universe.get().getWorld()`
- Uses `Teleport` component for actual player movement

---

### B.2.7 Modules Page (Coming Soon)

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   FACTION MODULES                              [← Back to Settings] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ┌─────────────────────────────┐ ┌─────────────────────────────┐   │
│   │  💰 TREASURY                │ │  ⚔️ RAIDS                   │   │
│   │                             │ │                             │   │
│   │  Manage faction funds,      │ │  Structured faction raids   │   │
│   │  taxes, and transactions    │ │  with objectives & rewards  │   │
│   │                             │ │                             │   │
│   │  ┌───────────────────────┐  │ │  ┌───────────────────────┐  │   │
│   │  │     COMING SOON       │  │ │  │     COMING SOON       │  │   │
│   │  └───────────────────────┘  │ │  └───────────────────────┘  │   │
│   └─────────────────────────────┘ └─────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────┐ ┌─────────────────────────────┐   │
│   │  📈 LEVELS                  │ │  🏴 WAR                     │   │
│   │                             │ │                             │   │
│   │  Faction progression with   │ │  Formal war declarations    │   │
│   │  XP, levels, and perks      │ │  with victory conditions    │   │
│   │                             │ │                             │   │
│   │  ┌───────────────────────┐  │ │  ┌───────────────────────┐  │   │
│   │  │     COMING SOON       │  │ │  │     COMING SOON       │  │   │
│   │  └───────────────────────┘  │ │  └───────────────────────┘  │   │
│   └─────────────────────────────┘ └─────────────────────────────┘   │
│                                                                     │
│   ─────────────────────────────────────────────────────────────     │
│   These modules are planned for future updates.                     │
│   Follow our roadmap for release information!                       │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

**Module Card Pattern**:
Each module shows:
- Icon + Name
- Brief description (1-2 lines)
- "COMING SOON" badge (grayed out, non-interactive)

When implemented, badge becomes [OPEN] button to access module page.

---

## B.3 Admin GUI

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

### B.3.1 Admin Dashboard

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│  [DASHBOARD] FACTIONS  ZONES  PLAYERS  CONFIG  HELP        [?]     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  ⚙️ ADMIN DASHBOARD                          HyperFactions    ║ │
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
│   │  🛡️ BYPASS     │ │  📋 VIEW LOGS  │ │  🔄 RELOAD     │          │
│   │    [OFF]       │ │                │ │    CONFIG      │          │
│   └────────────────┘ └────────────────┘ └────────────────┘          │
│                                                                     │
│   ⚠️ ALERTS                                                         │
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
│                                                            │ 👤  │  │
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

### B.3.2 Admin Factions Page

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   DASHBOARD [FACTIONS] ZONES  PLAYERS  CONFIG  HELP            [?] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   MANAGE FACTIONS (24 total)                                        │
│   ┌───────────────────────────────────┐  Sort: [Name ▼]             │
│   │ 🔍 Search factions...             │                             │
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
│   │  ▶ Shadow Collective                    ⚠️ RAIDABLE [MANAGE]│   │
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
- **DISBAND**: Force disband with confirmation ("Type faction name to confirm")

---

### B.3.3 Admin Zones Page

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
│   │  🛡️ Spawn                                                   │   │
│   │     Center: (0, 0)  │  Chunks: 9  │  Created: 45 days ago   │   │
│   │     Flags: PvP OFF, Build OFF, Monsters OFF                 │   │
│   │     [EDIT]  [MANAGE CHUNKS]  [DELETE]                       │   │
│   └─────────────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  🛡️ Market                                                  │   │
│   │     Center: (500, 200)  │  Chunks: 4  │  Created: 30 days   │   │
│   │     [EDIT]  [MANAGE CHUNKS]  [DELETE]                       │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   WARZONES (2)                                                      │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ⚔️ Arena                                                   │   │
│   │     Center: (1000, -500)  │  Chunks: 16  │  PvP ALWAYS ON   │   │
│   │     [EDIT]  [MANAGE CHUNKS]  [DELETE]                       │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

**Create Zone Dialog**:
```
┌─────────────────────────────────────────┐
│  CREATE ZONE                            │
│                                         │
│  Zone Type:                             │
│  ● SafeZone    ○ WarZone                │
│                                         │
│  Zone Name:                             │
│  ┌─────────────────────────────────┐    │
│  │ Enter zone name...              │    │
│  └─────────────────────────────────┘    │
│                                         │
│  Starting Position:                     │
│  ○ Current location                     │
│  ○ Custom coordinates: X___ Z___        │
│                                         │
│  ┌──────────┐  ┌──────────┐             │
│  │  CREATE  │  │  CANCEL  │             │
│  └──────────┘  └──────────┘             │
└─────────────────────────────────────────┘
```

---

### B.3.4 Admin Players Page

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   DASHBOARD  FACTIONS  ZONES [PLAYERS] CONFIG  HELP            [?] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   PLAYER MANAGEMENT                                                 │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │ 🔍 Search player by name or UUID...                           │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  PLAYER: FireLord                                    🟢 Online ║ │
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

### B.3.5 Admin Config Page

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   DASHBOARD  FACTIONS  ZONES  PLAYERS [CONFIG] HELP            [?] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   SERVER CONFIGURATION                           [🔄 RELOAD CONFIG] │
│                                                                     │
│   ⚠️ Changes require reload or restart to take effect               │
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
│   │                    💾 SAVE CHANGES                          │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

---

### B.3.6 Admin Logs Page

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
│   LOG ENTRIES (247 matching)                        [📥 EXPORT CSV] │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  2024-01-25 14:32:15 │ CLAIM     │ Dragons    │ FireLord    │   │
│   │    Claimed chunk (120, 340) in Overworld                    │   │
│   ├─────────────────────────────────────────────────────────────┤   │
│   │  2024-01-25 14:30:02 │ JOIN      │ Dragons    │ ShadowBlade │   │
│   │    Accepted invite from FireLord                            │   │
│   ├─────────────────────────────────────────────────────────────┤   │
│   │  2024-01-25 14:28:45 │ INVITE    │ Dragons    │ FireLord    │   │
│   │    Invited ShadowBlade to faction                           │   │
│   ├─────────────────────────────────────────────────────────────┤   │
│   │  2024-01-25 14:15:00 │ ADMIN     │ -          │ Admin       │   │
│   │    Created SafeZone "Spawn"                                 │   │
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

## B.4 Module Placeholder Pattern

For unimplemented modules (Treasury, Raids, Levels, War):

**Implementation**:
```java
// In FactionModulesPage
if (!moduleManager.isEnabled("treasury")) {
    // Show "Coming Soon" card
    cmd.append("modules/coming_soon_card.ui");
    cmd.set("#ModuleName.Text", "Faction Treasury");
    cmd.set("#ModuleDesc.Text", "Manage faction funds, taxes, and transactions");
    cmd.set("#ModuleIcon.Src", "treasury_icon.png");
}
```

**Placeholder Card Design**:
- Grayed-out icon
- Module name and brief description
- "Coming Soon" badge
- Optional: "Notify me" toggle for future

---

## B.5 Implementation Tasks

> **Wireframes**: See sections B.1-B.3 above for ASCII mockups and element breakdowns.
> **Review**: Wireframes are ready for async feedback before implementation.

**New Player GUI (B.1)**

| Task | Description | Template Files |
|------|-------------|----------------|
| B.1.1 | Create NewPlayerPageRegistry | - |
| B.1.2 | Create NewPlayerNavBarHelper | `newplayer/nav_button.ui` |
| B.1.3 | Create BrowseFactions page with pagination | `newplayer/browse.ui`, `newplayer/faction_card.ui` |
| B.1.4 | Create CreateFaction page (single form) | `newplayer/create.ui`, `newplayer/color_picker.ui` |
| B.1.5 | Create MyInvites page | `newplayer/invites.ui`, `newplayer/invite_card.ui` |
| B.1.6 | Integrate Help page (shared) | Uses shared `help/*.ui` |
| B.1.7 | **Add faction tag field to Faction record** | Update `Faction.java`, storage, commands |
| B.1.8 | Add custom hex color input to color picker | `newplayer/hex_color_input.ui` |

**B.1.7 Details (Faction Tags)**:
> Currently NOT implemented. The Faction record needs a new `tag` field.
> - Add `@Nullable String tag` to Faction record (2-4 uppercase chars)
> - Add `withTag()` method for immutable updates
> - Update JSON serialization (storage)
> - Update `/f create` to accept optional tag
> - Update `/f tag <tag>` command for changing tag (or add to settings GUI)
> - Display as `[TAG]` in faction color in Browse page and chat

**Faction Player GUI (B.2)**

| Task | Status | Description | Template Files |
|------|--------|-------------|----------------|
| B.2.1 | TODO | Redesigned Dashboard with identity, stat cards, quick actions, activity feed | `faction_dashboard.ui`, `activity_entry.ui`, `leave_confirm.ui` |
| B.2.2 | TODO | Members page with paginated list, role-based actions | `faction_members.ui`, `member_entry.ui` |
| B.2.3 | TODO | Browse Factions for faction players with relation indicators | `faction_browser.ui` (reuse), `indicator_*.ui` |
| B.2.4 | **DONE** | Implement interactive ChunkMapPage (29x17 grid, click to claim/unclaim) | `chunk_map.ui`, `chunk_btn.ui` |
| B.2.5 | **DONE** | Create Relations page (sectioned: Allies/Enemies/Requests) | `faction_relations.ui`, `relation_*.ui` |
| B.2.6 | **DONE** | Settings page (edit modals, teleport, recruitment, disband) | `faction_settings.ui`, `*_modal.ui` |
| B.2.7 | TODO | Create Modules page (placeholders) | `faction/modules.ui`, `modules/coming_soon_card.ui` |
| B.2.8 | TODO | Activity log event tracking system | `FactionEvent.java`, `FactionEventType.java`, storage updates |
| B.2.9 | TODO | Chat toggle system (F-CHAT, A-CHAT) | ChatManager integration (future) |

**Admin GUI (B.3)**

| Task | Description | Template Files |
|------|-------------|----------------|
| B.3.1 | Create AdminPageRegistry | - |
| B.3.2 | Create AdminNavBarHelper | `admin/nav_button.ui` |
| B.3.3 | Create Admin Dashboard with player FAB | `admin/dashboard.ui`, `admin/player_fab.ui` |
| B.3.4 | Create Admin Factions page | `admin/factions.ui`, `admin/faction_card.ui` |
| B.3.5 | Enhance Admin Zones page | `admin/zones.ui`, `admin/zone_card.ui`, `admin/create_zone.ui` |
| B.3.6 | Create Admin Players page | `admin/players.ui`, `admin/player_lookup.ui` |
| B.3.7 | Create Admin Config page | `admin/config.ui` |
| B.3.8 | Create Admin Logs page | `admin/logs.ui`, `admin/log_entry.ui` |
| B.3.9 | Implement FAB quick-switch navigation | - |

**Shared Components (B.4)**

| Task | Description | Template Files |
|------|-------------|----------------|
| B.4.1 | Create shared HelpPage component | `help/main.ui`, `help/category.ui`, `help/command.ui` |
| B.4.2 | Create coming_soon_card template | `modules/coming_soon_card.ui` |
| B.4.3 | Create FAB (Floating Action Button) template | `shared/fab.ui` |
| B.4.4 | Create confirmation dialog template | `shared/confirm_dialog.ui` |
| B.4.5 | Register HelpPage in all three registries | - |

**Template Naming Convention**:
```
resources/ui/HyperFactions/
├── newplayer/          # New Player GUI templates
├── faction/            # Faction Player GUI templates
├── admin/              # Admin GUI templates
├── help/               # Shared Help templates
├── modules/            # Module placeholder templates
├── shared/             # Shared components (FAB, dialogs)
└── nav/                # Existing nav bar templates
```
