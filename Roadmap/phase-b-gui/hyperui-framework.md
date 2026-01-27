# B.0 HyperUI Design Framework

> **Note**: This section defines the modular, generalized GUI design system that all HyperSystem plugins can share. The goal is to create reusable components, consistent visual language, and flexible page layouts.

---

## B.0.1 Design Principles

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

## B.0.2 Page Layout Types

HyperUI supports three fundamental page layouts:

### Type A: Full Navigation Page (Standard)
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

### Type B: Nav-Less Dialog Page
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

### Type C: Overlay Modal
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

### Type D: Full-Screen Immersive
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

## B.0.3 Component Library

### Core Components (HyperUI Base)

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

### Faction-Specific Components

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

## B.0.4 Color System

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

## B.0.5 Typography

| Token | Size | Weight | Usage |
|-------|------|--------|-------|
| `--hs-font-title` | 24px | Bold | Page titles |
| `--hs-font-heading` | 18px | SemiBold | Section headings |
| `--hs-font-subheading` | 14px | Medium | Card titles, labels |
| `--hs-font-body` | 12px | Regular | Body text |
| `--hs-font-caption` | 10px | Regular | Captions, timestamps |

**Note**: Actual sizes depend on Hytale's UI scaling. Values are reference sizes.

---

## B.0.6 Spacing System

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

## B.0.7 Template File Structure

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

---

## B.0.8 State Management Patterns

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

---

## B.0.9 Event Binding Patterns

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

---

## B.0.10 Cross-Plugin Sharing (Future)

**Option 1: Shared HyperUI Library**
Create a separate `HyperUI` project that all plugins depend on.

**Option 2: Copy-Paste Common Templates**
Each plugin copies the `common/` UI templates. Less elegant but works if HyperUI library is too much overhead.

**Option 3: Git Submodule**
Create HyperUI as a git submodule that each plugin includes.

**Recommendation**: Start with Option 2 (copy-paste) for speed, migrate to Option 1 (library) when patterns stabilize.

---

## B.0.11 Design Decisions (Resolved)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Visual Style** | Match Hytale Default | Consistency with native UI |
| **Animations** | No animations | Best performance, instant feedback |
| **Loading States** | Progressive | Show available content immediately |
| **Error Display** | Chat + Inline (hybrid) | Based on Hytale API research |

### Error Handling Implementation

**Research Finding**: Hytale's CustomUI does not have built-in toast notification components. The established pattern is:

1. **Chat-based feedback** for action results
2. **Inline validation** using `.Visible` property
3. **Page behavior on errors**: Validation errors keep page open; unexpected errors show chat message

---

## Enhancement Ideas

| Priority | Enhancement | Description |
|----------|-------------|-------------|
| Medium | HyperUI Library | Extract shared components to standalone library |
| Medium | Theme System | Support for light/dark themes |
| Low | Animation Support | Subtle transitions when Hytale supports them |
| Low | Sound Feedback | Action sounds when Hytale audio API is available |
