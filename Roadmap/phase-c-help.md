# Phase C: Help System & Documentation

> **Status**: PARTIAL - Text help implemented, GUI help started
> **Target Version**: v1.1.0 (C.1), v1.2.0 (C.2)
> **Last Updated**: January 27, 2026

**Goal**: Provide contextual, accessible help for all player types. Help is accessible from ALL three GUI systems.

**Design Decisions**:
- **Help Layout**: Full page (replaces current view, not overlay or panel)
- **Page Structure**: Two-column layout - categories on left, content on right
- **Search scope**: Commands + category names (not full text)
- **External docs**: Standalone website placeholder (future hyperfactions.example.com)
- **Command clicks**: Research first - check if Hytale supports chat pre-fill (see R.7)
- **Category structure**: By player journey (not by handler groups)
- **Locked commands**: Visible but grayed with lock icon + requirement tooltip
- **State persistence**: Remember last viewed category within session

---

## Current State (2026-01-27)

### Implemented

| Component | Status | File |
|-----------|--------|------|
| HelpFormatter | Done | `util/HelpFormatter.java` |
| CommandHelp | Done | `util/CommandHelp.java` (record) |
| Section-based help data | Done | `FactionCommand.java` (lines 146-200) |
| HelpPage (New Player GUI) | Done | `gui/page/newplayer/HelpPage.java` |

### Help System Overview

**Text-based help** (C.1 partial):
- `/f help` - Shows grouped command list via HelpFormatter
- 42 commands organized into 7 sections:
  - Core (7), Management (8), Territory (4), Relations (3)
  - Teleport (3), Information (7), Other (4)

**GUI-based help** (C.2 partial):
- Basic HelpPage for new players (static template)
- No searchable command wiki yet
- No contextual help system yet

### Not Yet Implemented

- [ ] `/f help <category>` - Category-specific chat help
- [ ] `/f help <command>` - Command-specific detail help
- [ ] HelpRegistry with categories and commands
- [ ] HelpMainPage with search
- [ ] HelpCategoryPage
- [ ] HelpCommandPage with full details
- [ ] Permission-filtered help
- [ ] Session state persistence

---

## C.0 Help Visibility Rules

**Key Principle**: Players should be able to learn about features they don't have access to yet.

| GUI Context | Documentation Visible | Highlighted Section |
|-------------|----------------------|---------------------|
| New Player GUI | All except Admin | Getting Started |
| Faction Player GUI | All except Admin | Managing Your Faction |
| Admin GUI | All categories | Server Administration |

**Rationale**:
- New players can see what faction membership offers (incentive to join)
- Faction members can review basics or look ahead to advanced features
- Admins have complete documentation access

---

## C.1 Chat Help (Current System Enhanced)

**Current**: `HelpFormatter.buildHelp()` with grouped commands.

**Enhancements**:
- Context-sensitive help based on player state
- Section-based help (`/f help <section>`)
- Subcommand-specific help (`/f help <command>`)
- Permission-filtered (only show commands player can use)
- Shorter output with "Use /f help <section> for more"

**Help Commands**:
```
/f help                - Overview + category list
/f help gui            - Open Help GUI
/f help <category>     - Commands in category
/f help <command>      - Detailed command help

Categories by player journey:
  start     - Getting Started (create, join, browse)
  basics    - Faction Basics (info, members, leave)
  territory - Territory & Claims (claim, unclaim, map, home)
  diplomacy - Diplomacy (ally, enemy, neutral, relations)
  manage    - Management (settings, invite, kick, promote)
  admin     - Server Administration (admin only)
```

**Example Output** (`/f help territory`):
```
┌──────────────────────────────────────────┐
│ HyperFactions - Territory & Claims       │
├──────────────────────────────────────────┤
│ /f claim      - Claim current chunk      │
│ /f unclaim    - Release claimed chunk    │
│ /f overclaim  - Take enemy territory     │
│ /f map        - View territory map       │
│ /f home       - Teleport to faction home │
│ /f sethome    - Set faction home         │
├──────────────────────────────────────────┤
│ Use /f help <command> for details        │
│ Use /f help gui for visual help          │
└──────────────────────────────────────────┘
```

---

## C.2 Help GUI (Quick Reference Wiki)

**Access**:
- `/f help gui` - Direct command access
- `HELP` nav button in ALL three GUIs
- `[?]` help icon in GUI headers

**Features**:
- Search commands and categories
- Player journey category structure
- Command details with examples
- Locked commands visible with requirements
- Session state persistence (remembers last category)
- External documentation links

---

### C.2.1 Help Main Page

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   ← Back to [Dashboard/Browse/Admin]                     HELP GUIDE │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │ 🔍 Search commands or categories...                         │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   YOUR STATUS: [👤 New Player]  /  [⚔️ Faction Member]  /  [⚙️ Admin] │
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  📚 PLAYER GUIDE                                              ║ │
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   ┌──────────────────────────┐ ┌──────────────────────────┐         │
│   │  🚀 GETTING STARTED      │ │  📋 FACTION BASICS       │         │
│   │                          │ │                          │         │
│   │  Create or join a        │ │  View info, manage       │         │
│   │  faction to begin        │ │  membership, basics      │         │
│   │                          │ │                          │         │
│   │  4 commands              │ │  5 commands              │         │
│   │  [OPEN →]                │ │  [OPEN →]                │         │
│   └──────────────────────────┘ └──────────────────────────┘         │
│                                                                     │
│   ┌──────────────────────────┐ ┌──────────────────────────┐         │
│   │  🗺️ TERRITORY & CLAIMS   │ │  🤝 DIPLOMACY            │         │
│   │                          │ │                          │         │
│   │  Claim land, set home,   │ │  Alliances, enemies,     │         │
│   │  view territory map      │ │  faction relations       │         │
│   │                          │ │                          │         │
│   │  6 commands              │ │  4 commands              │         │
│   │  [OPEN →]                │ │  [OPEN →]                │         │
│   └──────────────────────────┘ └──────────────────────────┘         │
│                                                                     │
│   ┌──────────────────────────┐ ┌──────────────────────────┐         │
│   │  ⚙️ MANAGEMENT           │ │  🛡️ ADMINISTRATION       │         │
│   │                          │ │                          │         │
│   │  Settings, invites,      │ │  Server admin tools,     │         │
│   │  roles, leadership       │ │  zones, config           │         │
│   │                          │ │                          │         │
│   │  8 commands              │ │  🔒 Admin only           │         │
│   │  [OPEN →]                │ │  [OPEN →]                │         │
│   └──────────────────────────┘ └──────────────────────────┘         │
│                                                                     │
│   ─────────────────────────────────────────────────────────────     │
│   📖 EXTERNAL RESOURCES                                             │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  [📚 Full Documentation]  [💬 Discord Support]              │   │
│   │  [🐛 Report a Bug]        [💡 Request Feature]              │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown**:

| Element ID | Type | Description |
|------------|------|-------------|
| `#SearchInput` | TextInput | Search commands and category names |
| `#StatusBadge` | Badge | Shows player's current state (New/Member/Admin) |
| `#CategoryGrid` | Grid | 2x3 grid of category cards |
| `#CategoryCard` | Card | Category name, description, command count, open button |
| `#AdminLock` | Icon | Lock icon on Admin category if player not admin |
| `#ExternalLinks` | Container | Links to external documentation |

**Search Behavior**:
- Searches command names, aliases, and category names
- Results appear as dropdown below search box
- Click result to go directly to command detail page
- Empty search shows all categories

---

### C.2.2 Help Category Page

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   HELP GUIDE  ›  Territory & Claims                   [🔍 Search]   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  🗺️ TERRITORY & CLAIMS                                        ║ │
│   ║  Claim land for your faction, set up a home base, and         ║ │
│   ║  manage your territory on the map.                            ║ │
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   COMMANDS IN THIS CATEGORY                                         │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ✅ /f claim                                                │   │
│   │     Claim the chunk you're standing in for your faction     │   │
│   │     Aliases: c, cl                               [DETAILS →]│   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ✅ /f unclaim                                              │   │
│   │     Release a claimed chunk back to wilderness              │   │
│   │     Aliases: uc                                  [DETAILS →]│   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  🔒 /f overclaim                                            │   │
│   │     Take territory from a raidable enemy faction            │   │
│   │     ⚠️ Requires: Officer role                    [DETAILS →]│   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ✅ /f map                                                  │   │
│   │     View territory map around your location                 │   │
│   │     Aliases: m                                   [DETAILS →]│   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  🔒 /f home                                                 │   │
│   │     Teleport to your faction's home location                │   │
│   │     ⚠️ Requires: Faction membership              [DETAILS →]│   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  🔒 /f sethome                                              │   │
│   │     Set faction home at your current location               │   │
│   │     ⚠️ Requires: Officer role                    [DETAILS →]│   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│   [← All Categories]                              [ESC] Back        │
└─────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown**:

| Element ID | Type | Description |
|------------|------|-------------|
| `#Breadcrumb` | Navigation | "HELP GUIDE › Category Name" with clickable parts |
| `#CategoryHeader` | Container | Icon, name, description |
| `#CommandList` | ScrollList | List of command cards in this category |
| `#CommandCard` | Card | Available (✅) or Locked (🔒), name, brief desc, aliases |
| `#RequirementBadge` | Badge | Shows requirement for locked commands |
| `#DetailsBtn` | Button | Navigate to command detail page |
| `#BackBtn` | Button | Return to main Help page |

**Locked Command Display**:
- Grayed out card background
- 🔒 icon instead of ✅
- Yellow warning text: "⚠️ Requires: [requirement]"
- Still clickable to see full details

---

### C.2.3 Help Command Detail Page

**Wireframe**:
```
┌─────────────────────────────────────────────────────────────────────┐
│   HELP GUIDE  ›  Territory  ›  /f claim               [🔍 Search]   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ╔═══════════════════════════════════════════════════════════════╗ │
│   ║  /f claim                                              ✅     ║ │
│   ║  Claim territory for your faction                             ║ │
│   ╚═══════════════════════════════════════════════════════════════╝ │
│                                                                     │
│   USAGE                                                             │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  /f claim              - Claim current chunk (or open GUI)  │   │
│   │  /f claim here         - Claim current chunk (CLI mode)     │   │
│   │  /f claim auto         - Auto-claim as you walk             │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ALIASES: c, cl                                                    │
│                                                                     │
│   DESCRIPTION                                                       │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Claims the 16x16 block chunk you're standing in for your   │   │
│   │  faction. You need enough power to maintain claims:         │   │
│   │                                                             │   │
│   │  • Each chunk costs 2 power to hold                         │   │
│   │  • If faction power drops below needed, you become raidable │   │
│   │  • Can only claim adjacent to existing territory (or first) │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   REQUIREMENTS                                                      │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  ✅ Must be in a faction                                    │   │
│   │  ✅ Must have Officer role or higher                        │   │
│   │  ✅ Permission: hyperfactions.territory.claim               │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   BEHAVIOR                                                          │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  In wilderness:      Claims chunk immediately               │   │
│   │  In own territory:   Opens Map GUI                          │   │
│   │  In enemy territory: Opens Map GUI with overclaim option    │   │
│   │  In ally territory:  Error - cannot claim ally land         │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   RELATED COMMANDS                                                  │
│   [/f unclaim]  [/f overclaim]  [/f map]                            │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│   [← Back to Territory]                           [ESC] Back        │
└─────────────────────────────────────────────────────────────────────┘
```

**Element Breakdown**:

| Element ID | Type | Description |
|------------|------|-------------|
| `#Breadcrumb` | Navigation | Full path: "HELP › Category › Command" |
| `#CommandHeader` | Container | Command name, brief description, available badge |
| `#UsageBlock` | CodeBlock | Syntax variations with descriptions |
| `#AliasesText` | Text | Comma-separated aliases |
| `#DescriptionBlock` | TextBlock | Full description with bullet points |
| `#RequirementsList` | List | Checkmarks for met requirements, X for unmet |
| `#BehaviorBlock` | TextBlock | Context-aware behavior explanation |
| `#RelatedCommands` | ButtonGroup | Quick links to related command pages |

---

## C.3 Help Data Model

**HelpCategory Record**:
```java
public record HelpCategory(
    String id,              // "territory"
    String name,            // "Territory & Claims"
    String icon,            // "🗺️"
    String description,     // "Claim land for your faction..."
    int order,              // Display order (0 = first)
    boolean adminOnly       // Only visible to admins
) {}
```

**HelpCommand Record**:
```java
public record HelpCommand(
    String command,         // "claim"
    String categoryId,      // "territory"
    String syntax,          // "/f claim [here|auto]"
    String briefDesc,       // "Claim current chunk"
    String fullDesc,        // Multi-line description
    List<String> aliases,   // ["c", "cl"]
    List<String> examples,  // ["/f claim", "/f claim auto"]
    HelpRequirement requirement,  // What's needed to use
    Map<String, String> contextBehavior,  // Context -> behavior description
    List<String> relatedCommands  // ["unclaim", "overclaim", "map"]
) {}

public record HelpRequirement(
    boolean needsFaction,       // Must be in faction
    FactionRole minRole,        // MEMBER, OFFICER, LEADER, or null
    String permission,          // "hyperfactions.territory.claim"
    String customRequirement    // For special cases
) {}
```

**HelpRegistry**:
```java
public class HelpRegistry {
    private final Map<String, HelpCategory> categories = new LinkedHashMap<>();
    private final Map<String, HelpCommand> commands = new HashMap<>();
    private final Map<String, List<HelpCommand>> commandsByCategory = new HashMap<>();

    // Registration
    public void registerCategory(HelpCategory category) { ... }
    public void registerCommand(HelpCommand command) { ... }

    // Lookup
    public List<HelpCategory> getCategories(HelpContext context) { ... }
    public List<HelpCommand> getCommandsInCategory(String categoryId) { ... }
    public HelpCommand getCommand(String commandName) { ... }

    // Search
    public List<SearchResult> search(String query) { ... }

    // Availability check
    public boolean isAvailable(HelpCommand cmd, PlayerRef player, Faction faction) { ... }
}
```

---

## C.4 External Documentation Links

**Website Structure** (placeholder for future):
```
https://hyperfactions.example.com/
├── /                       - Landing page
├── /docs/                  - Documentation home
│   ├── /getting-started    - Quick start guide
│   ├── /power-territory    - Power & territory explained
│   ├── /diplomacy          - Alliance and enemy system
│   ├── /admin-guide        - Server administration
│   └── /api                - Developer API docs
├── /support                - Support/Discord link
└── /changelog              - Version history
```

**Link Buttons in Help GUI**:
- 📚 Full Documentation → `/docs/`
- 💬 Discord Support → Discord invite link
- 🐛 Report a Bug → GitHub issues
- 💡 Request Feature → GitHub discussions

---

## C.5 Session State Persistence

**Implementation**:
```java
public class HelpPageState {
    private String lastCategoryId;      // Last viewed category
    private String lastCommandId;       // Last viewed command
    private String searchQuery;         // Last search (optional)

    // Stored per-player in memory (not persisted to disk)
    private static final Map<UUID, HelpPageState> playerStates = new ConcurrentHashMap<>();

    public static HelpPageState get(UUID playerId) {
        return playerStates.computeIfAbsent(playerId, k -> new HelpPageState());
    }

    public static void clear(UUID playerId) {
        playerStates.remove(playerId);
    }
}
```

**Behavior**:
- On first open: Show main Help page
- On subsequent opens (same session): Return to last viewed category/command
- On GUI close (ESC or nav away): State preserved
- On disconnect: State cleared

---

## C.6 Implementation Tasks

**Chat Help (C.1)**

| Task | Description |
|------|-------------|
| C.1.1 | Implement `/f help` overview with category list |
| C.1.2 | Implement `/f help <category>` for each category |
| C.1.3 | Implement `/f help <command>` for detailed help |
| C.1.4 | Add permission filtering (hide commands player can't see) |
| C.1.5 | Add "Use /f help gui" suggestion in output |

**Help GUI Core (C.2)**

| Task | Description | Template Files |
|------|-------------|----------------|
| C.2.1 | Create HelpRegistry with categories and commands | - |
| C.2.2 | Create HelpMainPage | `help/main.ui` |
| C.2.3 | Create HelpCategoryPage | `help/category.ui`, `help/command_card.ui` |
| C.2.4 | Create HelpCommandPage | `help/command_detail.ui` |
| C.2.5 | Implement search (commands + categories) | `help/search_result.ui` |
| C.2.6 | Implement breadcrumb navigation | `help/breadcrumb.ui` |
| C.2.7 | Implement session state persistence | - |

**Context & Visibility (C.3)**

| Task | Description |
|------|-------------|
| C.3.1 | Implement HelpContext enum and detection |
| C.3.2 | Implement category visibility (hide Admin for non-admins) |
| C.3.3 | Implement command availability indicators (✅/🔒) |
| C.3.4 | Add requirement tooltips for locked commands |
| C.3.5 | Add status badge to HelpMainPage |

**External Documentation (C.4)**

| Task | Description |
|------|-------------|
| C.4.1 | Add external link buttons to Help GUI |
| C.4.2 | Create placeholder website structure |
| C.4.3 | Design docs site landing page mockup |

**Data Population (C.5)**

| Task | Description |
|------|-------------|
| C.5.1 | Define all HelpCategory entries |
| C.5.2 | Define all HelpCommand entries with full details |
| C.5.3 | Map commands to categories |
| C.5.4 | Add context behaviors for each command |
