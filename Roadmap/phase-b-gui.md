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

**Research Finding**: Hytale's CustomUI does not have built-in toast notification components. Based on analysis of UI-PATTERNS.md and AdminUI.md, the established pattern is:

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

**Design Decisions**:
- **List Loading**: Pagination (research shows `LayoutMode: TopScrolling` builds all items at once - infinite scroll would require rebuilding the page which could cause performance issues on large servers)
- **Card Display**: Faction tag (colored) + faction name. Example: `[DRG] Dragons` in cyan
- **Color Picker**: 16 standard colors + custom hex input

> **Note**: Faction tags are NOT currently implemented in the data model. See task **B.1.7** for implementation.

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│  [BROWSE]  CREATE   INVITES   HELP                    [?] Help Icon │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  FIND YOUR FACTION                                            ║ │
│   ║  ┌─────────────────────────────────┐  [Sort: Members ▼]       ║ │
│   ║  │ 🔍 Search factions...           │                          ║ │
│   ║  └─────────────────────────────────┘                          ║ │
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ▶ [DRG] Dragons                                       [+]  │   │
│   └─────────────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ▶ [PHX] Phoenix Rising                                [+]  │   │
│   └─────────────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ▶ [TIC] The Ironclad                             [OPEN]    │   │
│   └─────────────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ▼ [SHD] Shadow Collective                                  │   │
│   │  ───────────────────────────────────────────────────────────│   │
│   │    Members: 12/50  │  Power: 85  │  Claims: 23              │   │
│   │    "We strike from the darkness..."                         │   │
│   │    Leader: ShadowKing                                       │   │
│   │    ┌──────────────┐                                         │   │
│   │    │ REQUEST JOIN │  (faction is invite-only)               │   │
│   │    └──────────────┘                                         │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ─────────────────────────────────────────────────────────────     │
│   Page 1 of 5  [< Prev]  [Next >]                                   │
│   Server Stats: 24 factions │ 156 players │ 1,240 claimed chunks    │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown**:

| Element ID | Type | Description |
|------------|------|-------------|
| `#NavBar` | Container | Navigation bar with 4 tabs |
| `#SearchInput` | TextInput | Search factions by name or tag |
| `#SortDropdown` | Dropdown | Sort by: Members, Power, Name, Newest |
| `#FactionList` | ScrollContainer | List of faction cards (paginated, 10 per page) |
| `#FactionCard` | Expandable | Collapsed: [TAG] name, Expanded: full details |
| `#FactionTag` | Text | Colored faction tag (e.g., `[DRG]` in faction color) |
| `#OpenBadge` | Badge | Shows [OPEN] for factions accepting direct joins |
| `#ExpandBtn` | Button | ▶/▼ toggle to expand/collapse card |
| `#RequestJoinBtn` | Button | Request to join (invite-only factions) |
| `#JoinBtn` | Button | Direct join (open factions) |
| `#Pagination` | Container | Page controls: Prev, Next, page indicator |
| `#ServerStats` | Text | Footer with aggregate statistics |

**Behaviors**:
- Collapsed card shows only faction name + [OPEN] badge if applicable
- Click ▶ or card to expand and show details
- [OPEN] factions show "JOIN" button, others show "REQUEST JOIN"
- Search filters in real-time as user types
- Sort dropdown changes list order immediately

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

**Nav Bar**: `DASHBOARD` | `MEMBERS` | `MAP` | `RELATIONS` | `SETTINGS` | `HELP`

> **Note**: MODULES accessible from Settings page. Admin quick-switch is a floating button.

**Design Decisions**:
- **Quick Actions**: Keep 3 actions (Home, Claim, Chat) - focused and clean
- **Member Sorting**: Role first (Leader > Officer > Member), then online status within each role
- **Settings Access**: Read-only for Members (can view but not edit)
- **Territory Map**: 9x9 grid (81 chunks) for better context

---

### B.2.1 Faction Dashboard (Default Landing)

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│  [DASHBOARD]  MEMBERS   MAP   RELATIONS   SETTINGS   HELP      [?] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  🏰 DRAGONS                                             [b]   ║ │
│   ║  "From the ashes we rise!"                                    ║ │
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐       │
│   │   ⚡ POWER      │ │   🗺️ CLAIMS     │ │   👥 MEMBERS    │       │
│   │                 │ │                 │ │                 │       │
│   │   156 / 200     │ │   23 / 78       │ │   8 / 50        │       │
│   │   (78%)         │ │   (can claim    │ │   (4 online)    │       │
│   │                 │ │    55 more)     │ │                 │       │
│   └─────────────────┘ └─────────────────┘ └─────────────────┘       │
│                                                                     │
│   QUICK ACTIONS                                                     │
│   ┌───────────┐ ┌───────────┐ ┌───────────┐                         │
│   │  🏠 HOME  │ │ 📍 CLAIM  │ │ 💬 CHAT   │                         │
│   │           │ │           │ │   [ON]    │                         │
│   └───────────┘ └───────────┘ └───────────┘                         │
│                                                                     │
│   RECENT ACTIVITY                                                   │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  • FireLord claimed chunk at (120, 340)           2 min ago │   │
│   │  • ShadowBlade joined the faction                 5 min ago │   │
│   │  • Alliance formed with Phoenix Rising           15 min ago │   │
│   │  • DragonSlayer set faction home                  1 hr ago  │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│                                                            ┌─────┐  │
│                                                            │ ⚙️  │  │
│                                                            │ADMIN│  │
│                                                            └─────┘  │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown**:

| Element ID | Type | Description |
|------------|------|-------------|
| `#FactionHeader` | Container | Faction name (colored), description |
| `#PowerCard` | StatCard | Current power / max power with percentage |
| `#ClaimsCard` | StatCard | Current claims / max claimable with "can claim X more" |
| `#MembersCard` | StatCard | Member count / max with online count |
| `#HomeBtn` | ActionButton | Teleport to faction home |
| `#ClaimBtn` | ActionButton | Claim current chunk (context-aware) |
| `#ChatToggle` | ToggleButton | Toggle faction chat on/off, shows current state |
| `#ActivityFeed` | ScrollList | Recent faction events, newest first |
| `#AdminFab` | FloatingButton | Bottom-right, only visible if player has admin perm |

**Quick Action Behaviors**:
- **Home**: Instant teleport if no warmup, else show warmup timer
- **Claim**: Context-aware (see Phase A.4) - claims wilderness, opens map in own territory
- **Chat**: Toggle faction chat mode, button shows [ON]/[OFF] state

**Admin FAB** (Floating Action Button):
- Only visible to players with `hyperfactions.admin` permission
- Click opens Admin GUI (B.3)
- Circular button with gear icon, bottom-right corner

---

### B.2.2 Members Page

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   DASHBOARD  [MEMBERS]  MAP   RELATIONS   SETTINGS   HELP      [?] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  FACTION MEMBERS (8/50)                    [+ INVITE PLAYER]  ║ │
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  👑 FireLord (LEADER)                              🟢 Online │   │
│   │     Power: 20/20  │  Joined: 30 days ago                    │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ⭐ DragonSlayer (OFFICER)                         🟢 Online │   │
│   │     Power: 18/20  │  Joined: 25 days ago                    │   │
│   │     [DEMOTE]  [KICK]  (Officer+ controls)                   │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ⭐ StormRider (OFFICER)                           🔴 Offline│   │
│   │     Power: 15/20  │  Joined: 20 days ago                    │   │
│   │     [DEMOTE]  [KICK]                                        │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │    ShadowBlade (MEMBER)                            🟢 Online │   │
│   │     Power: 12/20  │  Joined: 5 minutes ago                  │   │
│   │     [PROMOTE]  [KICK]                                       │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ─────────────────────────────────────────────────────────────     │
│   Your role: OFFICER  │  Total faction power: 156/200               │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown**:

| Element ID | Type | Description |
|------------|------|-------------|
| `#MemberCount` | Text | "FACTION MEMBERS (X/Y)" |
| `#InviteBtn` | Button | Opens invite dialog (Officer+ only) |
| `#MemberList` | ScrollList | All members sorted by role then online status |
| `#MemberCard` | Card | Player name, role badge, online status, power, join date |
| `#RoleBadge` | Icon | 👑 Leader, ⭐ Officer, (none) Member |
| `#OnlineStatus` | Indicator | 🟢 Online, 🔴 Offline |
| `#PromoteBtn` | Button | Promote to Officer (Leader only, for Members) |
| `#DemoteBtn` | Button | Demote to Member (Leader only, for Officers) |
| `#KickBtn` | Button | Remove from faction (Officer+ for Members, Leader for Officers) |
| `#YourRole` | Text | Shows current player's role |

**Role-Based Visibility**:
- **Members**: See list only, no action buttons
- **Officers**: See Kick button for Members only
- **Leader**: See all buttons, can Promote/Demote/Kick anyone except self

**Invite Dialog** (opens as modal):
```
┌─────────────────────────────────────────┐
│  INVITE PLAYER                          │
│  ┌─────────────────────────────────┐    │
│  │ Enter player name...            │    │
│  └─────────────────────────────────┘    │
│                                         │
│  Online players:                        │
│  [Steve] [Alex] [Notch] [Herobrine]     │
│                                         │
│  ┌──────────┐  ┌──────────┐             │
│  │  INVITE  │  │  CANCEL  │             │
│  └──────────┘  └──────────┘             │
└─────────────────────────────────────────┘
```

---

### B.2.3 Territory Map Page

> **Reference Implementation**: See [ElbaphFactions Analysis](../../../resources/ElbaphFactions.md) for detailed patterns on interactive map rendering using `InteractiveCustomUIPage<T>`.

**Implementation Approach** (based on ElbaphFactions patterns):

The territory map uses `InteractiveCustomUIPage<MapData>` to render a dynamic chunk grid with click interactions.

**Architecture**:
```java
public class TerritoryMapPage extends InteractiveCustomUIPage<MapData> {
    private int centerX, centerZ;  // Map center (player position at open)
    private int selectedX, selectedZ;  // Currently selected chunk (-1 if none)

    public TerritoryMapPage(PlayerRef player) {
        super(player, CustomPageLifetime.CanDismiss, MapData.CODEC);
        this.centerX = player.getChunkX();
        this.centerZ = player.getChunkZ();
        this.selectedX = this.selectedZ = -1;
    }
}
```

**Grid Configuration**:
- **Default Size**: 9x9 (81 chunks) for cleaner UI
- **Cell Size**: 32x32 pixels per chunk
- **Grid Dimensions**: 288x288 pixels (9 * 32)

**Wireframe** (9x9 grid):
```
┌─────────────────────────────────────────────────────────────────────────────┐
│   DASHBOARD   MEMBERS  [MAP]  RELATIONS   SETTINGS   HELP              [?] │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   TERRITORY MAP                                      Current: (120, 340)    │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │     -4   -3   -2   -1    0   +1   +2   +3   +4                      │   │
│   │  +4  .    .    .    .    .    .    .    .    .                      │   │
│   │  +3  .    .    .    .    .    .    .    .    .                      │   │
│   │  +2  .    .    .    A    A    .    .    .    .                      │   │
│   │  +1  .    .    .    A    ■    ■    .    .    .                      │   │
│   │   0  .    .    E    E   [■]   ■    ■    .    .   ■ = Your faction   │   │
│   │  -1  .    .    E    E    ■    ■    .    .    .   A = Ally           │   │
│   │  -2  .    .    .    .    ■    .    .    .    .   E = Enemy          │   │
│   │  -3  .    .    .    .    .    .    .    .    .   . = Wilderness     │   │
│   │  -4  .    .    .    .    .    .    .    .    .   S = SafeZone       │   │
│   │                                                  W = WarZone        │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│   SELECTED: (120, 340) - Your territory                                     │
│   ┌────────────┐ ┌────────────┐                                             │
│   │  UNCLAIM   │ │  SET HOME  │   (actions based on selected chunk)         │
│   └────────────┘ └────────────┘                                             │
│                                                                             │
│   Navigation: [◀ W] [▲ N] [▼ S] [▶ E]  │  [CENTER ON ME]                    │
│                                                                             │
│   Claims: 23/78 (55 available)  │  Power needed to hold: 46                 │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                               [ESC] Back                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Color Coding** (HyperUI palette):

| Relation/Type | Color (Hex) | Color (Decimal) | HyperUI Token |
|---------------|-------------|-----------------|---------------|
| Your Faction | `#22c55e` | 2278750 | `--hs-accent-success` |
| Ally | `#3b82f6` | 3899126 | `--hs-accent-info` |
| Enemy | `#ef4444` | 15684676 | `--hs-accent-error` |
| Other Faction | `#f59e0b` | 16096779 | `--hs-accent-warning` |
| SafeZone | `#22d3d8` | 2282456 | (custom) |
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
            int color = getColorForChunk(chunkX, chunkZ);
            cmd.set("#ChunkCards[" + z + "][" + x + "].Background",
                    "Solid { Color: " + color + "; }");

            // Setup click event (select chunk)
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ChunkCards[" + z + "][" + x + "]",
                EventData.of("Action", "Select:" + chunkX + ":" + chunkZ),
                false
            );

            // Setup tooltip
            setupChunkTooltip(cmd, z, x, chunkX, chunkZ);
        }
    }
}
```

**Event Handling**:
```java
@Override
public void handleDataEvent(Ref ref, Store store, MapData data) {
    String[] parts = data.action.split(":");

    switch (parts[0]) {
        case "Select" -> {
            selectedX = Integer.parseInt(parts[1]);
            selectedZ = Integer.parseInt(parts[2]);
            rebuildAndSend(ref, store);  // Update to show selection
        }
        case "Claim" -> claimSelectedChunk();
        case "Unclaim" -> unclaimSelectedChunk();
        case "SetHome" -> setHomeAtSelected();
        case "Pan" -> {
            centerX += Integer.parseInt(parts[1]);
            centerZ += Integer.parseInt(parts[2]);
            rebuildAndSend(ref, store);
        }
        case "Center" -> {
            centerX = player.getChunkX();
            centerZ = player.getChunkZ();
            rebuildAndSend(ref, store);
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
| `#SelectedInfo` | Text | Details about selected chunk |
| `#ActionButtons` | Container | Contextual action buttons for selected chunk |
| `#ClaimBtn` | Button | Claim selected wilderness chunk |
| `#UnclaimBtn` | Button | Unclaim selected owned chunk |
| `#SetHomeBtn` | Button | Set faction home at selected owned chunk |
| `#NavButtons` | ButtonGroup | Pan map N/S/E/W |
| `#CenterBtn` | Button | Recenter map on player position |
| `#Legend` | Container | Color legend |
| `#ClaimStats` | Text | Current claims / max with available count |

**Template Files**:
- `faction/map.ui` - Main map page layout
- `faction/chunk_cell.ui` - Single chunk cell (32x32)

**Interaction Flow**:
1. Player opens map - grid renders centered on player position
2. Click chunk to select - shows info panel with available actions
3. Actions change based on chunk ownership:
   - **Wilderness**: [CLAIM]
   - **Own territory**: [UNCLAIM] [SET HOME]
   - **Enemy territory**: [OVERCLAIM] (if raidable)
   - **Ally/Zone**: No actions (info only)
4. Navigation buttons pan the view
5. Center button re-centers on player

---

### B.2.4 Relations Page

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   DASHBOARD   MEMBERS   MAP  [RELATIONS]  SETTINGS   HELP      [?] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  DIPLOMATIC RELATIONS                       [+ SET RELATION]  ║ │
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   ALLIES (2)                                                        │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  🤝 Phoenix Rising                                          │   │
│   │     Since: 15 days ago  │  Leader: PhoenixKing              │   │
│   │     [NEUTRAL]  [ENEMY]  (change relation)                   │   │
│   └─────────────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  🤝 The Ironclad                                            │   │
│   │     Since: 3 days ago  │  Leader: IronMaster                │   │
│   │     [NEUTRAL]  [ENEMY]                                      │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ENEMIES (1)                                                       │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ⚔️ Shadow Collective                                       │   │
│   │     Since: 20 days ago  │  Leader: ShadowKing               │   │
│   │     [NEUTRAL]  [ALLY]                                       │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   PENDING REQUESTS (1)                                              │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  📨 Storm Legion wants to ally                              │   │
│   │     Requested: 2 hours ago                                  │   │
│   │     [ACCEPT]  [DECLINE]                                     │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown**:

| Element ID | Type | Description |
|------------|------|-------------|
| `#SetRelationBtn` | Button | Opens faction picker to set new relation |
| `#AlliesSection` | Container | List of allied factions |
| `#EnemiesSection` | Container | List of enemy factions |
| `#PendingSection` | Container | Incoming ally requests |
| `#RelationCard` | Card | Faction name, relation date, leader |
| `#NeutralBtn` | Button | Set relation to neutral |
| `#AllyBtn` | Button | Request/set ally relation |
| `#EnemyBtn` | Button | Set enemy relation |
| `#AcceptBtn` | Button | Accept pending ally request |
| `#DeclineBtn` | Button | Decline pending ally request |

---

### B.2.5 Settings Page (Officer+)

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   DASHBOARD   MEMBERS   MAP   RELATIONS  [SETTINGS]  HELP      [?] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  FACTION SETTINGS                                             ║ │
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   BASIC INFORMATION                                                 │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Faction Name:  [Dragons                              ] [✓] │   │
│   │  Description:   [From the ashes we rise!              ] [✓] │   │
│   │  Color:         [b] Cyan  [CHANGE]                          │   │
│   │  Tag:           [DRG] (shown in chat)                  [✓]  │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   RECRUITMENT                                                       │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ○ Open - Anyone can join                                   │   │
│   │  ● Invite Only - Players must be invited                    │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   HOME LOCATION                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Current: Overworld (120, 64, 340)                          │   │
│   │  Set: 5 days ago by FireLord                                │   │
│   │  ┌───────────────────┐ ┌───────────────────┐                │   │
│   │  │ SET HOME HERE     │ │ TELEPORT TO HOME  │                │   │
│   │  └───────────────────┘ └───────────────────┘                │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   MODULES    [▶ Open Modules Page]                                  │
│                                                                     │
│   DANGER ZONE (Leader Only)                                         │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ┌───────────────────────────────────────────────────────┐  │   │
│   │  │                 ☠️ DISBAND FACTION                    │  │   │
│   │  └───────────────────────────────────────────────────────┘  │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown**:

| Element ID | Type | Description |
|------------|------|-------------|
| `#NameInput` | TextInput | Editable faction name with save button |
| `#DescInput` | TextInput | Editable description with save button |
| `#ColorDisplay` | Text | Current color code with change button |
| `#TagInput` | TextInput | Editable tag (2-4 chars) |
| `#RecruitmentRadio` | RadioGroup | Open / Invite Only |
| `#HomeInfo` | Container | Current home location and who set it |
| `#SetHomeBtn` | Button | Set home at current location |
| `#TeleportBtn` | Button | Teleport to faction home |
| `#ModulesLink` | Button | Navigate to Modules page |
| `#DisbandBtn` | Button | Disband faction (Leader only, requires confirmation) |

**Permission Visibility**:
- **Members**: Cannot access Settings page (nav hidden or disabled)
- **Officers**: Can edit name, desc, color, tag, recruitment, home
- **Leader**: All above + Disband button

---

### B.2.6 Modules Page (Coming Soon)

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

| Task | Description | Template Files |
|------|-------------|----------------|
| B.2.1 | Enhance Dashboard with admin FAB | `faction/dashboard.ui`, `faction/admin_fab.ui` |
| B.2.2 | Add quick actions (Home, Claim, Chat) | Existing `faction/dashboard.ui` |
| B.2.3 | Enhance Members page with invite dialog | `faction/members.ui`, `faction/invite_dialog.ui` |
| B.2.4 | Implement interactive ChunkMapPage | `faction/map.ui`, `faction/chunk_cell.ui` |
| B.2.5 | Create Relations page | `faction/relations.ui`, `faction/relation_card.ui` |
| B.2.6 | Enhance Settings page | `faction/settings.ui` |
| B.2.7 | Create Modules page (placeholders) | `faction/modules.ui`, `modules/coming_soon_card.ui` |
| B.2.8 | Update FactionPageRegistry with HELP | - |

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
