# HyperFactions Development Roadmap

> Last Updated: January 25, 2026
> Current Version: 1.0.0 (dev/phase1 branch)

---

## Overview

HyperFactions is feature-complete for v1.0 release with core faction management, territory claiming, power mechanics, diplomacy, and protection systems. This roadmap outlines the next major development phases focusing on:

1. **Command System Overhaul** - Modular commands with GUI-first design
2. **User Experience by Player State** - Distinct GUIs for new players, faction members, and admins
3. **Help System & Documentation** - In-game wiki and contextual help
4. **Testing Infrastructure** - Automated and manual QA processes

---

## Table of Contents (with line references)

| Section | Line | Description |
|---------|------|-------------|
| **Current State** | ~50 | Completed features and in-progress items |
| **Phase A: Command System** | ~70 | Command architecture, handlers, routing |
| ├─ A.1 Architecture | ~80 | Handler interface, package structure |
| ├─ A.2 Shared Utilities | ~180 | CommandUtils, PlayerUtils, FactionUtils |
| ├─ A.3 State-Based Routing | ~280 | Context-aware command dispatch |
| ├─ A.4 Context-Aware Defaults | ~340 | Smart behavior per command |
| ├─ A.5 Configurable Aliases | ~420 | Per-command alias system |
| ├─ A.6 Command Groups | ~510 | Handler breakdown by category |
| └─ A.7 Tasks | ~560 | Implementation checklist |
| **Phase B: GUI System** | ~610 | Three-GUI architecture |
| ├─ B.1 New Player GUI | ~620 | Browse, Create, Invites wireframes |
| ├─ B.2 Faction Player GUI | ~850 | Dashboard, Members, Map, Relations, Settings |
| ├─ B.3 Admin GUI | ~1250 | Dashboard, Factions, Zones, Players, Config, Logs |
| ├─ B.4 Module Placeholders | ~1570 | Coming Soon pattern |
| └─ B.5 Tasks | ~1590 | Implementation checklist with templates |
| **Phase C: Help System** | ~1660 | Chat help, Help GUI, visibility rules |
| **Phase D: Testing** | ~1800 | Unit tests, integration, QA scripts |
| **Phase E: Modules** | ~1970 | Treasury, Raids, Levels, War |
| **Research** | ~2070 | Future investigation items |
| **Known Issues** | ~2090 | Pitch/yaw bug, Admin back button |
| **Technical Reference** | ~2120 | CustomUI constraints, patterns |
| **Version Planning** | ~2160 | Release schedule |

> **Note**: Line numbers are approximate and will shift as content is added. Use section headers for navigation.

---

## Current State

### Completed (v1.0.0)

- [x] Core faction system (create, disband, invite, join, leave, kick)
- [x] Territory claiming with power mechanics
- [x] Diplomatic relations (ally, enemy, neutral)
- [x] Combat tagging system
- [x] SafeZone/WarZone system
- [x] Faction home teleportation
- [x] Basic GUI system with FactionPageRegistry, NavBarHelper
- [x] 27+ subcommands implemented
- [x] HyperPerms integration

### In Progress

- [ ] Pitch/yaw orientation bug on `/f home` (TABLED)
- [ ] Admin Zones page back button positioning

---

## Phase A: Command System Overhaul

**Goal**: Transform from monolithic switch-based routing to modular, GUI-first command architecture with context-aware defaults and configurable aliases.

**Design Decisions**:
- **Handler Architecture**: Hybrid approach - self-contained handlers using shared utility classes
- **GUI-First Pattern**: Context-aware defaults based on current game state
- **Aliases**: Configurable per-command with multiple alias support

---

### A.1 Command Architecture

**Current State**: Single `FactionCommand.java` (1533 lines) with switch statement routing.

**Target Package Structure**:
```
com.hyperfactions.command/
├── FactionCommand.java              # Entry point, minimal routing
├── CommandRouter.java               # State detection + handler dispatch
├── CommandContext.java              # Extended context with player state
│
├── handler/                         # Self-contained command handlers
│   ├── CommandHandler.java          # Interface
│   ├── AbstractCommandHandler.java  # Base class with common patterns
│   ├── CoreCommandHandler.java      # create, disband, invite, accept, leave, kick
│   ├── RoleCommandHandler.java      # promote, demote, transfer
│   ├── TerritoryCommandHandler.java # claim, unclaim, overclaim, map
│   ├── HomeCommandHandler.java      # home, sethome, stuck
│   ├── RelationCommandHandler.java  # ally, enemy, neutral, relations
│   ├── SettingsCommandHandler.java  # rename, desc, color, open, close
│   ├── InfoCommandHandler.java      # info, list, who, power, logs
│   ├── ChatCommandHandler.java      # chat, ally-chat
│   └── AdminCommandHandler.java     # bypass, safezone, warzone, etc.
│
├── util/                            # Shared utilities
│   ├── CommandUtils.java            # Message formatting, permission checks
│   ├── PlayerUtils.java             # Player lookup, online checks
│   ├── FactionUtils.java            # Faction/member validation
│   ├── CommandHelp.java             # Help record (existing)
│   ├── HelpFormatter.java           # Chat help formatting (existing)
│   └── CommandResult.java           # Unified result enum
│
└── alias/                           # Alias system
    ├── AliasManager.java            # Loads/resolves aliases from config
    └── AliasConfig.java             # Alias configuration model
```

**Handler Interface**:
```java
public interface CommandHandler {
    /**
     * Get the subcommands this handler responds to.
     * Example: ["claim", "unclaim", "overclaim", "map"] for TerritoryCommandHandler
     */
    List<String> getSubcommands();

    /**
     * Execute the command. Handler decides GUI vs CLI based on args and context.
     * @param ctx      Extended command context with player state
     * @param subCmd   The specific subcommand (e.g., "claim")
     * @param args     Arguments after subcommand (may be empty)
     */
    void execute(FactionCommandContext ctx, String subCmd, String[] args);

    /**
     * Get help entries for all subcommands this handler manages.
     */
    List<CommandHelp> getHelpEntries();

    /**
     * Check if player can use any command from this handler.
     * Fine-grained permission checks happen in execute().
     */
    boolean canAccess(PlayerRef player);
}
```

**Abstract Base Class**:
```java
public abstract class AbstractCommandHandler implements CommandHandler {
    protected final HyperFactions hyperFactions;
    protected final HyperFactionsPlugin plugin;

    // Shared utilities (injected)
    protected final CommandUtils cmdUtils;
    protected final PlayerUtils playerUtils;
    protected final FactionUtils factionUtils;

    // Common methods available to all handlers
    protected void sendSuccess(CommandContext ctx, String message) { ... }
    protected void sendError(CommandContext ctx, String message) { ... }
    protected void sendInfo(CommandContext ctx, String message) { ... }
    protected boolean requireFaction(FactionCommandContext ctx) { ... }
    protected boolean requireOfficer(FactionCommandContext ctx) { ... }
    protected boolean requireLeader(FactionCommandContext ctx) { ... }
    protected void openGui(FactionCommandContext ctx, String pageId) { ... }
}
```

**Extended Command Context**:
```java
public class FactionCommandContext {
    private final CommandContext baseCtx;
    private final PlayerRef player;
    private final Store<EntityStore> store;
    private final Ref<EntityStore> ref;
    private final World world;

    // Pre-computed state (cached for performance)
    private final Faction playerFaction;        // null if not in faction
    private final FactionMember memberInfo;     // null if not in faction
    private final boolean isAdmin;              // has hyperfactions.admin
    private final ChunkContext chunkContext;    // current chunk info

    // Chunk context includes:
    public record ChunkContext(
        int chunkX, int chunkZ,
        UUID ownerFactionId,          // null if wilderness
        boolean isOwnTerritory,
        boolean isAllyTerritory,
        boolean isEnemyTerritory,
        boolean isSafeZone,
        boolean isWarZone
    ) {}
}
```

---

### A.2 Shared Utility Classes

**CommandUtils** - Message formatting and common operations:
```java
public class CommandUtils {
    // Colors (constants)
    public static final String CYAN = "#55FFFF";
    public static final String GREEN = "#55FF55";
    public static final String RED = "#FF5555";
    public static final String YELLOW = "#FFFF55";
    public static final String GRAY = "#AAAAAA";
    public static final String WHITE = "#FFFFFF";

    // Prefix
    public Message prefix() {
        return Message.raw("[").color(GRAY)
            .insert(Message.raw("HyperFactions").color(CYAN))
            .insert(Message.raw("] ").color(GRAY));
    }

    // Message helpers
    public Message success(String text) { return prefix().insert(msg(text, GREEN)); }
    public Message error(String text) { return prefix().insert(msg(text, RED)); }
    public Message info(String text) { return prefix().insert(msg(text, YELLOW)); }
    public Message msg(String text, String color) { return Message.raw(text).color(color); }

    // Permission check with HyperPerms integration
    public boolean hasPermission(PlayerRef player, String permission) {
        return HyperPermsIntegration.hasPermission(player.getUuid(), permission);
    }

    // Usage formatting
    public Message usage(String command, String description) {
        return msg("Usage: ", GRAY).insert(msg(command, WHITE))
            .insert(msg(" - " + description, GRAY));
    }
}
```

**PlayerUtils** - Player lookup and validation:
```java
public class PlayerUtils {
    private final HyperFactionsPlugin plugin;
    private final FactionManager factionManager;

    // Find online player by name (case-insensitive)
    public Optional<PlayerRef> findOnlinePlayer(String name) { ... }

    // Find player UUID by name (online or in faction data)
    public Optional<UUID> findPlayerUuid(String name) { ... }

    // Get player's display name with faction color
    public Message getDisplayName(UUID playerUuid) { ... }

    // Check if player is online
    public boolean isOnline(UUID playerUuid) { ... }
}
```

**FactionUtils** - Faction validation and lookups:
```java
public class FactionUtils {
    private final FactionManager factionManager;
    private final ClaimManager claimManager;
    private final RelationManager relationManager;

    // Get faction by name (case-insensitive fuzzy match)
    public Optional<Faction> findFaction(String name) { ... }

    // Validate faction name
    public ValidationResult validateFactionName(String name) { ... }

    // Get relation between player's faction and target
    public RelationType getRelation(UUID playerUuid, UUID targetFactionId) { ... }

    // Check if player can perform action in chunk
    public boolean canActInChunk(UUID playerUuid, String world, int chunkX, int chunkZ, ActionType action) { ... }

    public enum ActionType { BUILD, BREAK, INTERACT, CLAIM, UNCLAIM }
    public record ValidationResult(boolean valid, String errorMessage) {}
}
```

---

### A.3 State-Based Routing

**Behavior**: Bare `/f` command routes based on player state:

| Player State | `/f` Opens | Rationale |
|--------------|------------|-----------|
| Not in faction | New Player GUI | Help them get started |
| In faction | Faction Dashboard | Most common use case |
| Admin | Faction Dashboard + admin quick-switch | Admins are usually players too |

**CommandRouter Implementation**:
```java
public class CommandRouter {
    private final Map<String, CommandHandler> handlers = new HashMap<>();
    private final AliasManager aliasManager;
    private final GuiManager guiManager;

    public void route(FactionCommandContext ctx, String subcommand, String[] args) {
        // Handle bare /f command
        if (subcommand == null || subcommand.isEmpty()) {
            openDefaultGui(ctx);
            return;
        }

        // Resolve aliases
        String resolvedCmd = aliasManager.resolve(subcommand);

        // Find handler for subcommand
        CommandHandler handler = findHandler(resolvedCmd);
        if (handler == null) {
            ctx.sendError("Unknown command: " + subcommand);
            ctx.sendInfo("Use /f help for available commands");
            return;
        }

        // Check basic access
        if (!handler.canAccess(ctx.getPlayer())) {
            ctx.sendError("You don't have permission for this command.");
            return;
        }

        // Delegate to handler
        handler.execute(ctx, resolvedCmd, args);
    }

    private void openDefaultGui(FactionCommandContext ctx) {
        if (ctx.getPlayerFaction() != null) {
            // Player is in a faction - open Dashboard
            guiManager.openFactionMain(ctx, ctx.isAdmin());
        } else {
            // Player not in faction - open New Player GUI
            guiManager.openNewPlayerGui(ctx);
        }
    }
}
```

---

### A.4 Context-Aware Command Defaults

**Pattern**: Commands behave intelligently based on current game state.

**Claim Command Context Logic**:
```java
// In TerritoryCommandHandler.executeClaim()
public void executeClaim(FactionCommandContext ctx, String[] args) {
    ChunkContext chunk = ctx.getChunkContext();

    // If args provided, use CLI mode (explicit claim)
    if (args.length > 0) {
        handleCliClaim(ctx, args);
        return;
    }

    // Context-aware defaults (no args)
    if (chunk.ownerFactionId() == null) {
        // WILDERNESS: Claim directly
        claimCurrentChunk(ctx);

    } else if (chunk.isOwnTerritory()) {
        // OWN TERRITORY: Open Map GUI (nothing to claim here)
        openMapGui(ctx);

    } else if (chunk.isEnemyTerritory()) {
        // ENEMY TERRITORY: Open Map GUI with overclaim option highlighted
        openMapGuiWithOverclaimPrompt(ctx, chunk.ownerFactionId());

    } else if (chunk.isAllyTerritory()) {
        // ALLY TERRITORY: Cannot claim, show message
        ctx.sendError("This chunk belongs to your ally. Cannot claim ally territory.");

    } else if (chunk.isSafeZone() || chunk.isWarZone()) {
        // ZONE: Cannot claim
        ctx.sendError("This is a protected zone. Cannot claim zone territory.");
    }
}
```

**Context-Aware Behavior Table**:

| Command | Wilderness | Own Territory | Enemy Territory | Ally Territory |
|---------|-----------|---------------|-----------------|----------------|
| `/f claim` | Claim chunk | Open Map GUI | Map + Overclaim prompt | Error message |
| `/f unclaim` | Error (not claimed) | Unclaim chunk | Error (not yours) | Error (not yours) |
| `/f home` | Teleport | Teleport | Teleport | Teleport |
| `/f sethome` | Error (not in territory) | Set home | Error (not yours) | Error (not yours) |
| `/f info` | Own faction info | Own faction info | Own faction info | Own faction info |

**Other Context-Aware Commands**:

```java
// /f invite (no args) - Opens member management with invite button
// /f invite <player> - Direct invite via CLI

// /f settings (no args) - Opens Settings GUI page
// /f desc <text> - Set description via CLI
// /f color <code> - Set color via CLI

// /f create (no args) - Opens Create Faction wizard
// /f create <name> [color] - Create via CLI with optional color

// /f info (no args) - Shows own faction info in chat OR opens Dashboard
// /f info <faction> - Shows target faction info in chat
```

---

### A.5 Configurable Aliases

**Config Structure** (in `config.json`):
```json
{
  "commands": {
    "claim": {
      "aliases": ["c", "cl"],
      "description": "Claim territory"
    },
    "unclaim": {
      "aliases": ["uc", "uncl"],
      "description": "Release territory"
    },
    "home": {
      "aliases": ["h", "tp"],
      "description": "Teleport to faction home"
    },
    "sethome": {
      "aliases": ["sh", "setspawn"],
      "description": "Set faction home location"
    },
    "create": {
      "aliases": ["new", "make"],
      "description": "Create a faction"
    },
    "disband": {
      "aliases": ["delete", "del"],
      "description": "Disband your faction"
    },
    "invite": {
      "aliases": ["inv", "i"],
      "description": "Invite a player"
    },
    "accept": {
      "aliases": ["join", "acc", "j"],
      "description": "Accept an invite"
    },
    "map": {
      "aliases": ["m", "territory"],
      "description": "View territory map"
    },
    "info": {
      "aliases": ["show", "f"],
      "description": "View faction info"
    },
    "admin": {
      "aliases": ["a", "adm"],
      "description": "Admin commands"
    }
  },
  "baseCommandAliases": ["f", "faction", "fac", "hf"]
}
```

**AliasManager Implementation**:
```java
public class AliasManager {
    private final Map<String, String> aliasToCommand = new HashMap<>();
    private final Map<String, List<String>> commandToAliases = new HashMap<>();

    public void loadFromConfig(JsonObject commandsConfig) {
        for (String command : commandsConfig.keySet()) {
            JsonObject cmdConfig = commandsConfig.getAsJsonObject(command);
            JsonArray aliases = cmdConfig.getAsJsonArray("aliases");

            List<String> aliasList = new ArrayList<>();
            for (JsonElement alias : aliases) {
                String aliasStr = alias.getAsString().toLowerCase();
                aliasToCommand.put(aliasStr, command);
                aliasList.add(aliasStr);
            }
            commandToAliases.put(command, aliasList);
        }
    }

    /** Resolve alias to canonical command name */
    public String resolve(String input) {
        String lower = input.toLowerCase();
        return aliasToCommand.getOrDefault(lower, lower);
    }

    /** Get all aliases for a command (for help display) */
    public List<String> getAliases(String command) {
        return commandToAliases.getOrDefault(command, Collections.emptyList());
    }

    /** Check if input is a known alias or command */
    public boolean isKnownCommand(String input) {
        String lower = input.toLowerCase();
        return aliasToCommand.containsKey(lower) || commandToAliases.containsKey(lower);
    }
}
```

**Help Display with Aliases**:
```
/f claim (aliases: c, cl)     - Claim territory
/f home (aliases: h, tp)      - Teleport to faction home
```

---

### A.6 Command Groups & Handlers

| Handler | Subcommands | Permission Base | Notes |
|---------|-------------|-----------------|-------|
| **CoreCommandHandler** | create, disband, invite, accept, leave, kick | `hyperfactions.use` | Faction lifecycle |
| **RoleCommandHandler** | promote, demote, transfer | `hyperfactions.manage` | Leadership actions |
| **TerritoryCommandHandler** | claim, unclaim, overclaim, map | `hyperfactions.territory` | Land management |
| **HomeCommandHandler** | home, sethome, stuck | `hyperfactions.home` | Teleportation |
| **RelationCommandHandler** | ally, enemy, neutral, relations | `hyperfactions.relations` | Diplomacy |
| **SettingsCommandHandler** | rename, desc, color, open, close | `hyperfactions.settings` | Faction config |
| **InfoCommandHandler** | info, list, who, power, logs | `hyperfactions.info` | Read-only info |
| **ChatCommandHandler** | chat, c, ally-chat, a | `hyperfactions.chat` | Communication |
| **AdminCommandHandler** | admin, bypass, safezone, warzone, zoneflag, reload | `hyperfactions.admin` | Admin tools |

**Handler Registration**:
```java
// In CommandRouter constructor
public CommandRouter(HyperFactions hf, HyperFactionsPlugin plugin) {
    CommandUtils cmdUtils = new CommandUtils();
    PlayerUtils playerUtils = new PlayerUtils(plugin, hf.getFactionManager());
    FactionUtils factionUtils = new FactionUtils(hf.getFactionManager(), hf.getClaimManager(), hf.getRelationManager());

    registerHandler(new CoreCommandHandler(hf, plugin, cmdUtils, playerUtils, factionUtils));
    registerHandler(new RoleCommandHandler(hf, plugin, cmdUtils, playerUtils, factionUtils));
    registerHandler(new TerritoryCommandHandler(hf, plugin, cmdUtils, playerUtils, factionUtils));
    registerHandler(new HomeCommandHandler(hf, plugin, cmdUtils, playerUtils, factionUtils));
    registerHandler(new RelationCommandHandler(hf, plugin, cmdUtils, playerUtils, factionUtils));
    registerHandler(new SettingsCommandHandler(hf, plugin, cmdUtils, playerUtils, factionUtils));
    registerHandler(new InfoCommandHandler(hf, plugin, cmdUtils, playerUtils, factionUtils));
    registerHandler(new ChatCommandHandler(hf, plugin, cmdUtils, playerUtils, factionUtils));
    registerHandler(new AdminCommandHandler(hf, plugin, cmdUtils, playerUtils, factionUtils));
}

private void registerHandler(CommandHandler handler) {
    for (String subCmd : handler.getSubcommands()) {
        handlers.put(subCmd.toLowerCase(), handler);
    }
}
```

---

### A.7 Implementation Tasks

**Foundation (A.1)**
- [ ] **A.1.1** Create `CommandHandler` interface
- [ ] **A.1.2** Create `AbstractCommandHandler` base class
- [ ] **A.1.3** Create `FactionCommandContext` with pre-computed state
- [ ] **A.1.4** Create `CommandRouter` with handler registration

**Shared Utilities (A.2)**
- [ ] **A.2.1** Extract `CommandUtils` from FactionCommand
- [ ] **A.2.2** Create `PlayerUtils` with lookup methods
- [ ] **A.2.3** Create `FactionUtils` with validation methods
- [ ] **A.2.4** Create `CommandResult` enum for unified return values

**Handler Extraction (A.3)**
- [ ] **A.3.1** Extract `CoreCommandHandler` (create, disband, invite, accept, leave, kick)
- [ ] **A.3.2** Extract `RoleCommandHandler` (promote, demote, transfer)
- [ ] **A.3.3** Extract `TerritoryCommandHandler` (claim, unclaim, overclaim, map)
- [ ] **A.3.4** Extract `HomeCommandHandler` (home, sethome, stuck)
- [ ] **A.3.5** Extract `RelationCommandHandler` (ally, enemy, neutral, relations)
- [ ] **A.3.6** Extract `SettingsCommandHandler` (rename, desc, color, open, close)
- [ ] **A.3.7** Extract `InfoCommandHandler` (info, list, who, power, logs)
- [ ] **A.3.8** Extract `ChatCommandHandler` (chat, ally-chat)
- [ ] **A.3.9** Extract `AdminCommandHandler` (admin commands)

**Context-Aware Defaults (A.4)**
- [ ] **A.4.1** Implement context-aware `/f claim` behavior
- [ ] **A.4.2** Implement context-aware `/f create` behavior
- [ ] **A.4.3** Implement context-aware `/f invite` behavior
- [ ] **A.4.4** Implement context-aware `/f settings` behavior
- [ ] **A.4.5** Implement context-aware `/f info` behavior

**Alias System (A.5)**
- [ ] **A.5.1** Create `AliasManager` class
- [ ] **A.5.2** Create `AliasConfig` model
- [ ] **A.5.3** Add alias config to `config.json`
- [ ] **A.5.4** Integrate alias resolution into CommandRouter
- [ ] **A.5.5** Update help display to show aliases

**Integration (A.6)**
- [ ] **A.6.1** Update FactionCommand to use CommandRouter
- [ ] **A.6.2** Update help system to aggregate from handlers
- [ ] **A.6.3** Add GUI-mode triggers to relevant handlers
- [ ] **A.6.4** Write migration tests (old commands still work)

---

## Phase B: GUI System Redesign

**Goal**: Create distinct, polished GUI experiences for each player state.

**Design Decisions**:
- **Wireframe Format**: ASCII mockups + detailed element descriptions for async review
- **New Player Focus**: Browse/Join first - encourage discovering existing factions
- **Create Wizard**: Single page with all options - fast for both new and experienced users
- **Browser Style**: Minimal cards (name only) with expand for details - clean, scalable
- **Dashboard Actions**: Home + Claim + Chat - most commonly used actions
- **Admin Switch**: Floating action button in bottom-right - always visible, mobile-inspired

---

### B.0 HyperUI Design Framework

> **Note**: This section defines the modular, generalized GUI design system that all HyperSystem plugins can share. The goal is to create reusable components, consistent visual language, and flexible page layouts.

#### B.0.1 Design Principles

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

#### B.0.2 Page Layout Types

HyperUI supports three fundamental page layouts:

##### Type A: Full Navigation Page (Standard)
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

##### Type B: Nav-Less Dialog Page
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

##### Type C: Overlay Modal
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

##### Type D: Full-Screen Immersive
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

#### B.0.3 Component Library

##### Core Components (HyperUI Base)

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

##### Faction-Specific Components

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

#### B.0.4 Color System

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

#### B.0.5 Typography

| Token | Size | Weight | Usage |
|-------|------|--------|-------|
| `--hs-font-title` | 24px | Bold | Page titles |
| `--hs-font-heading` | 18px | SemiBold | Section headings |
| `--hs-font-subheading` | 14px | Medium | Card titles, labels |
| `--hs-font-body` | 12px | Regular | Body text |
| `--hs-font-caption` | 10px | Regular | Captions, timestamps |

**Note**: Actual sizes depend on Hytale's UI scaling. Values are reference sizes.

---

#### B.0.6 Spacing System

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

#### B.0.7 Template File Structure

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

#### B.0.8 State Management Patterns

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

#### B.0.9 Event Binding Patterns

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

#### B.0.10 Cross-Plugin Sharing (Future)

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

#### B.0.11 Design Decisions (Resolved)

The following design decisions have been finalized:

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Visual Style** | Match Hytale Default | Consistency with native UI, less visual friction for players |
| **Animations** | No animations | Best performance, instant feedback, no delay between actions |
| **Loading States** | Progressive | Show available content immediately, load remaining async |
| **Error Display** | Chat + Inline (hybrid) | Based on Hytale API research (see below) |

---

##### Error Handling Implementation

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

##### Pending Questions (Lower Priority)

These decisions are deferred until implementation:

| Question | Options | Notes |
|----------|---------|-------|
| **Sound Feedback** | None / Minimal / Full | Depends on Hytale audio API availability |
| **Responsive Behavior** | Fixed / Scaled / Adaptive | Depends on Hytale screen size detection |
| **Accessibility** | High contrast, larger text, keyboard nav | Depends on Hytale accessibility support |

---

### B.1 New Player GUI

**Target Audience**: Players not currently in a faction.

**Access**: `/f` (when not in faction), `/f menu`, `/f start`

**Nav Bar**: `BROWSE` | `CREATE` | `INVITES` | `HELP`

> **Note**: BROWSE is first (default landing) to encourage faction discovery.

---

#### B.1.1 Browse Factions Page (Default Landing)

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

#### B.1.2 Create Faction Page (Single Page Form)

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

#### B.1.3 My Invites Page

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

#### B.1.4 Help Page (New Player Context)

See **Phase C** for full Help System specification. New Player context shows:
- "Getting Started" (highlighted)
- "Faction Member Guide" (preview what's possible after joining)
- Links to external documentation

### B.2 Faction Player GUI

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

#### B.2.1 Faction Dashboard (Default Landing)

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

#### B.2.2 Members Page

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

#### B.2.3 Territory Map Page

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

**Element Breakdown**:

| Element ID | Type | Description |
|------------|------|-------------|
| `#MapGrid` | ChunkGrid | 9x9 interactive chunk map (81 chunks visible) |
| `#CurrentCoords` | Text | Player's current chunk coordinates |
| `#ChunkCell` | Clickable | Each cell shows owner symbol, click to select |
| `#SelectedInfo` | Text | Details about selected chunk |
| `#ClaimBtn` | Button | Claim selected wilderness chunk |
| `#UnclaimBtn` | Button | Unclaim selected owned chunk |
| `#SetHomeBtn` | Button | Set faction home at selected owned chunk |
| `#NavButtons` | ButtonGroup | Pan map N/S/E/W |
| `#CenterBtn` | Button | Recenter map on player position |
| `#Legend` | Container | Color/symbol legend |
| `#ClaimStats` | Text | Current claims / max with available count |

**Interaction**:
- Click chunk to select, shows info and available actions
- Actions change based on chunk ownership:
  - **Wilderness**: [CLAIM]
  - **Own territory**: [UNCLAIM] [SET HOME]
  - **Enemy territory**: [OVERCLAIM] (if raidable)
  - **Ally/Zone**: No actions

---

#### B.2.4 Relations Page

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

#### B.2.5 Settings Page (Officer+)

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

#### B.2.6 Modules Page (Coming Soon)

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

### B.3 Admin GUI

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

#### B.3.1 Admin Dashboard

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

#### B.3.2 Admin Factions Page

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

#### B.3.3 Admin Zones Page

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

#### B.3.4 Admin Players Page

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

#### B.3.5 Admin Config Page

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

#### B.3.6 Admin Logs Page

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

### B.4 Module Placeholder Pattern

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

### B.5 Implementation Tasks

> **Wireframes**: See sections B.1-B.3 above for ASCII mockups and element breakdowns.
> **Review**: Wireframes are ready for async feedback before implementation.

**New Player GUI (B.1)** - see wireframes at ~line 620

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

**Faction Player GUI (B.2)** - see wireframes at ~line 850

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

**Admin GUI (B.3)** - see wireframes at ~line 1250

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

---

## Phase C: Help System & Documentation

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

### C.0 Help Visibility Rules

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

### C.1 Chat Help (Current System Enhanced)

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

### C.2 Help GUI (Quick Reference Wiki)

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

#### C.2.1 Help Main Page

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

#### C.2.2 Help Category Page

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

#### C.2.3 Help Command Detail Page

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

### C.3 Help Data Model

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

### C.4 External Documentation Links

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

### C.5 Session State Persistence

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

### C.6 Implementation Tasks

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

---

## Phase D: Testing Infrastructure

**Goal**: Establish comprehensive automated and manual testing processes.

**Design Decisions**:
- **Mocking strategy**: Full mocks for all Hytale APIs (PlayerRef, World, Store, etc.)
- **QA detail level**: Step-by-step scripts with expected results
- **Performance testing**: Future consideration (not in v1.x scope)

---

### D.1 Mock Infrastructure

**Overview**: Create mock implementations of Hytale APIs to enable testing without a running server.

**Mock Package Structure**:
```
src/test/java/com/hyperfactions/
├── mock/
│   ├── MockPlayerRef.java          # Mock player reference
│   ├── MockWorld.java              # Mock world with chunk data
│   ├── MockStore.java              # Mock entity store
│   ├── MockCommandContext.java     # Mock command context
│   ├── MockEventBus.java           # Mock event system
│   └── MockHytaleServer.java       # Composite mock for full server
├── fixture/
│   ├── TestFixtures.java           # Common test data builders
│   ├── FactionFixtures.java        # Pre-built faction scenarios
│   └── PlayerFixtures.java         # Pre-built player scenarios
└── ...test classes
```

**MockPlayerRef**:
```java
public class MockPlayerRef {
    private final UUID uuid;
    private final String username;
    private final MockWorld world;
    private Vector3d position;
    private boolean online;

    // Builder pattern for easy test setup
    public static MockPlayerRef.Builder builder() { ... }

    // Simulate player actions
    public void moveTo(int x, int y, int z) { ... }
    public void attack(MockPlayerRef target) { ... }
    public void setOnline(boolean online) { ... }

    // Get underlying mock
    public PlayerRef asPlayerRef() { ... }
}
```

**MockWorld**:
```java
public class MockWorld {
    private final String name;
    private final Map<ChunkCoord, MockChunk> chunks = new HashMap<>();

    // Chunk management
    public MockChunk getChunk(int x, int z) { ... }
    public void setChunkOwner(int x, int z, UUID factionId) { ... }
    public void setChunkZone(int x, int z, Zone zone) { ... }

    // Block simulation (for protection tests)
    public void placeBlock(int x, int y, int z, MockPlayerRef player) { ... }
    public void breakBlock(int x, int y, int z, MockPlayerRef player) { ... }
}
```

**TestFixtures**:
```java
public class TestFixtures {
    // Quick faction creation
    public static Faction createFaction(String name, UUID leaderId) { ... }
    public static Faction createFactionWithMembers(String name, int memberCount) { ... }

    // Quick player creation
    public static MockPlayerRef createPlayer(String name) { ... }
    public static MockPlayerRef createOnlinePlayer(String name, MockWorld world) { ... }

    // Scenarios
    public static FactionScenario twoFactionsAtWar() { ... }
    public static FactionScenario allianceSetup() { ... }
}
```

---

### D.2 Unit Tests

**Framework**: JUnit 5 (already configured)

**Coverage Targets**:

| Package | Test Focus | Priority | Mock Requirements |
|---------|-----------|----------|-------------------|
| `data/` | Record equality, builders, serialization | P1 | None (pure Java) |
| `manager/` | Business logic, state transitions | P1 | MockStorage, MockPlayerRef |
| `command/handler/` | Command parsing, routing | P1 | MockCommandContext |
| `util/` | Helper methods, formatting | P2 | None (pure Java) |
| `config/` | Config parsing, defaults | P2 | File system mocks |

**Test Structure**:
```
src/test/java/com/hyperfactions/
├── mock/                           # Mock implementations
├── fixture/                        # Test data builders
├── data/
│   ├── FactionTest.java
│   ├── FactionMemberTest.java
│   ├── PlayerPowerTest.java
│   ├── ZoneTest.java
│   └── FactionClaimTest.java
├── manager/
│   ├── FactionManagerTest.java
│   ├── ClaimManagerTest.java
│   ├── PowerManagerTest.java
│   ├── RelationManagerTest.java
│   ├── InviteManagerTest.java
│   ├── ZoneManagerTest.java
│   └── CombatTagManagerTest.java
├── command/
│   ├── CommandRouterTest.java
│   ├── handler/
│   │   ├── CoreCommandHandlerTest.java
│   │   ├── TerritoryCommandHandlerTest.java
│   │   └── ...
│   └── util/
│       └── AliasManagerTest.java
└── util/
    ├── ChunkUtilTest.java
    ├── TimeUtilTest.java
    └── HelpFormatterTest.java
```

**Example Test: FactionManagerTest**:
```java
class FactionManagerTest {
    private FactionManager manager;
    private MockStorage storage;
    private MockPlayerRef player1;
    private MockPlayerRef player2;

    @BeforeEach
    void setup() {
        storage = new MockStorage();
        manager = new FactionManager(storage);
        player1 = MockPlayerRef.builder().name("Player1").build();
        player2 = MockPlayerRef.builder().name("Player2").build();
    }

    @Test
    void createFaction_validName_returnsFaction() {
        // Act
        Result<Faction> result = manager.createFaction("Dragons", player1.getUuid(), "b");

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("Dragons", result.getValue().getName());
        assertEquals(player1.getUuid(), result.getValue().getLeaderId());
    }

    @Test
    void createFaction_playerAlreadyInFaction_returnsError() {
        // Arrange
        manager.createFaction("Dragons", player1.getUuid(), "b");

        // Act
        Result<Faction> result = manager.createFaction("Phoenix", player1.getUuid(), "c");

        // Assert
        assertTrue(result.isError());
        assertEquals(ErrorCode.PLAYER_ALREADY_IN_FACTION, result.getError());
    }

    @Test
    void createFaction_duplicateName_returnsError() {
        // Arrange
        manager.createFaction("Dragons", player1.getUuid(), "b");

        // Act
        Result<Faction> result = manager.createFaction("Dragons", player2.getUuid(), "c");

        // Assert
        assertTrue(result.isError());
        assertEquals(ErrorCode.FACTION_NAME_TAKEN, result.getError());
    }

    @Test
    void addMember_factionFull_returnsError() {
        // Arrange
        Faction faction = createFullFaction(); // 50 members

        // Act
        Result<Void> result = manager.addMember(faction.getId(), player1.getUuid());

        // Assert
        assertTrue(result.isError());
        assertEquals(ErrorCode.FACTION_FULL, result.getError());
    }

    @Test
    void disbandFaction_removesAllClaims() {
        // Arrange
        Faction faction = manager.createFaction("Dragons", player1.getUuid(), "b").getValue();
        claimManager.claim(faction.getId(), "world", 0, 0);
        claimManager.claim(faction.getId(), "world", 1, 0);

        // Act
        manager.disband(faction.getId());

        // Assert
        assertNull(claimManager.getOwner("world", 0, 0));
        assertNull(claimManager.getOwner("world", 1, 0));
    }
}
```

**Example Test: ClaimManagerTest**:
```java
class ClaimManagerTest {
    private ClaimManager manager;
    private FactionManager factionManager;
    private PowerManager powerManager;
    private MockWorld world;

    @BeforeEach
    void setup() {
        // ... setup with mocks
    }

    @Test
    void claim_wilderness_success() {
        // Arrange
        Faction faction = TestFixtures.createFaction("Dragons", UUID.randomUUID());
        powerManager.setPower(faction.getId(), 100); // Enough power

        // Act
        Result<Void> result = manager.claim(faction.getId(), "world", 0, 0);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(faction.getId(), manager.getOwner("world", 0, 0));
    }

    @Test
    void claim_insufficientPower_returnsError() {
        // Arrange
        Faction faction = TestFixtures.createFaction("Dragons", UUID.randomUUID());
        powerManager.setPower(faction.getId(), 1); // Not enough power

        // Act
        Result<Void> result = manager.claim(faction.getId(), "world", 0, 0);

        // Assert
        assertTrue(result.isError());
        assertEquals(ErrorCode.INSUFFICIENT_POWER, result.getError());
    }

    @Test
    void claim_inSafeZone_returnsError() {
        // Arrange
        Faction faction = TestFixtures.createFaction("Dragons", UUID.randomUUID());
        zoneManager.createSafeZone("Spawn", "world", 0, 0);

        // Act
        Result<Void> result = manager.claim(faction.getId(), "world", 0, 0);

        // Assert
        assertTrue(result.isError());
        assertEquals(ErrorCode.CANNOT_CLAIM_ZONE, result.getError());
    }
}
```

---

### D.3 Integration Tests

**Focus**: End-to-end flows across multiple managers with mocked Hytale APIs.

**Test Scenarios**:

| Scenario | Flow | Verifications |
|----------|------|---------------|
| Faction Lifecycle | Create → Claim → Set home → Invite → Disband | All data cleaned up |
| Alliance Flow | Request ally → Accept → Mutual relation | Both factions see alliance |
| Combat Tag | Attack → Tag applied → Teleport blocked → Wait → Teleport works | Timing correct |
| Power Raid | Faction loses power → Becomes raidable → Enemy overclaims | Territory transfers |
| Protection | Claim territory → Enemy tries to break → Blocked | Block event cancelled |

**Example Integration Test**:
```java
class FactionLifecycleIntegrationTest {
    private TestHarness harness;

    @BeforeEach
    void setup() {
        harness = new TestHarness(); // Sets up all managers with mocks
    }

    @Test
    void fullFactionLifecycle() {
        // Create faction
        MockPlayerRef leader = harness.createPlayer("Leader");
        harness.executeCommand(leader, "/f create Dragons");
        assertTrue(harness.isInFaction(leader, "Dragons"));

        // Claim territory
        leader.moveTo(100, 64, 100);
        harness.executeCommand(leader, "/f claim");
        assertTrue(harness.isClaimed(100, 100, "Dragons"));

        // Set home
        harness.executeCommand(leader, "/f sethome");
        assertNotNull(harness.getFactionHome("Dragons"));

        // Invite member
        MockPlayerRef member = harness.createPlayer("Member");
        harness.executeCommand(leader, "/f invite Member");
        assertTrue(harness.hasPendingInvite(member, "Dragons"));

        // Member accepts
        harness.executeCommand(member, "/f accept Dragons");
        assertTrue(harness.isInFaction(member, "Dragons"));
        assertEquals(2, harness.getFaction("Dragons").getMemberCount());

        // Disband
        harness.executeCommand(leader, "/f disband");
        harness.executeCommand(leader, "/f disband confirm"); // Confirmation
        assertFalse(harness.factionExists("Dragons"));
        assertFalse(harness.isInFaction(leader, "Dragons"));
        assertFalse(harness.isInFaction(member, "Dragons"));
        assertFalse(harness.isClaimed(100, 100, "Dragons"));
    }
}
```

### D.4 Manual QA Test Plan

**Format**: Step-by-step reproducible test scripts with expected results.

> **Full QA Document**: These are summary scripts. A full QA-CHECKLIST.md document
> should be created with all test cases for release validation.

---

#### QA-001: New Player GUI Flow
```
Test ID: QA-001
Category: New Player Experience
Players Required: 1
Estimated Time: 5 minutes

PRECONDITIONS:
□ Player not in any faction
□ Player has hyperfactions.use permission
□ Server has at least one existing faction

STEPS:
1. Run /f (no args)
   EXPECTED: New Player GUI opens
   EXPECTED: Browse tab is active (default landing)
   EXPECTED: At least one faction visible in list
   VERIFY: □ Pass □ Fail

2. Click on a faction name to expand
   EXPECTED: Faction details appear (members, power, description)
   VERIFY: □ Pass □ Fail

3. Click CREATE tab in nav bar
   EXPECTED: Create Faction form appears
   EXPECTED: Name field is focused
   VERIFY: □ Pass □ Fail

4. Enter "Test123" as faction name
   EXPECTED: Name validation passes (green checkmark or no error)
   VERIFY: □ Pass □ Fail

5. Select a color (click any color in picker)
   EXPECTED: Color preview updates with faction name in selected color
   VERIFY: □ Pass □ Fail

6. Click "Create Faction" button
   EXPECTED: GUI closes
   EXPECTED: Success message in chat
   EXPECTED: Player is now faction leader
   VERIFY: □ Pass □ Fail

CLEANUP:
- Run /f disband, type "confirm" when prompted

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

#### QA-002: Faction Dashboard & Quick Actions
```
Test ID: QA-002
Category: Faction Player GUI
Players Required: 1
Estimated Time: 5 minutes

PRECONDITIONS:
□ Player is in a faction
□ Player is Officer or Leader
□ Faction has no claimed territory

STEPS:
1. Run /f (no args)
   EXPECTED: Faction Dashboard opens
   EXPECTED: Power/Claims/Members stats visible
   EXPECTED: Quick actions (Home, Claim, Chat) visible
   VERIFY: □ Pass □ Fail

2. Click CLAIM quick action button
   EXPECTED: Current chunk is claimed (success message)
   EXPECTED: Claims stat increases by 1
   VERIFY: □ Pass □ Fail

3. Click CHAT quick action button
   EXPECTED: Toggle shows ON state
   EXPECTED: Faction chat mode message in chat
   VERIFY: □ Pass □ Fail

4. Type a message in chat
   EXPECTED: Message only visible to faction members
   EXPECTED: Message prefixed with [Faction]
   VERIFY: □ Pass □ Fail

5. Click CHAT again to toggle off
   EXPECTED: Toggle shows OFF state
   EXPECTED: Chat mode disabled message
   VERIFY: □ Pass □ Fail

6. Navigate to Settings page, click "Set Home Here"
   EXPECTED: Success message
   EXPECTED: Home location shown on Settings page
   VERIFY: □ Pass □ Fail

7. Move away, click HOME quick action on Dashboard
   EXPECTED: Warmup timer (if configured) or instant teleport
   EXPECTED: Player at faction home location
   VERIFY: □ Pass □ Fail

CLEANUP:
- Run /f unclaim to release territory

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

#### QA-003: Territory Claiming & Map
```
Test ID: QA-003
Category: Territory Management
Players Required: 1
Estimated Time: 10 minutes

PRECONDITIONS:
□ Player is Officer+ in faction
□ Faction has sufficient power (> 10)
□ Player is in wilderness (unclaimed area)

STEPS:
1. Run /f map
   EXPECTED: ASCII map shows in chat
   EXPECTED: Current chunk marked with [+] or similar
   EXPECTED: Current chunk shows as wilderness (.)
   VERIFY: □ Pass □ Fail

2. Run /f claim
   EXPECTED: Success message "Claimed chunk at (X, Z)"
   VERIFY: □ Pass □ Fail

3. Run /f map again
   EXPECTED: Current chunk now shows as faction-owned (your color)
   VERIFY: □ Pass □ Fail

4. Move to adjacent chunk (not diagonal)
5. Run /f claim
   EXPECTED: Success (adjacent claiming allowed)
   VERIFY: □ Pass □ Fail

6. Move to non-adjacent chunk (2+ chunks away)
7. Run /f claim
   EXPECTED: Error "Must claim adjacent to existing territory"
   VERIFY: □ Pass □ Fail

8. Open GUI (/f gui), navigate to MAP page
   EXPECTED: Interactive map shows claimed chunks
   EXPECTED: Your chunks highlighted in faction color
   VERIFY: □ Pass □ Fail

9. Click on an owned chunk, click UNCLAIM
   EXPECTED: Success message
   EXPECTED: Chunk released on map
   VERIFY: □ Pass □ Fail

CLEANUP:
- Unclaim all test chunks

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

#### QA-004: Alliance System (2 Players)
```
Test ID: QA-004
Category: Diplomacy
Players Required: 2
Estimated Time: 10 minutes

PRECONDITIONS:
□ Player1 is Officer+ in FactionA
□ Player2 is Officer+ in FactionB
□ Factions have no existing relation

STEPS:
1. Player1: Run /f ally FactionB
   EXPECTED: "Alliance request sent to FactionB"
   VERIFY: □ Pass □ Fail

2. Player2: Should receive notification
   EXPECTED: Message about incoming ally request from FactionA
   VERIFY: □ Pass □ Fail

3. Player2: Run /f ally FactionA
   EXPECTED: "You are now allies with FactionA"
   VERIFY: □ Pass □ Fail

4. Player1: Should receive notification
   EXPECTED: Message about alliance formed with FactionB
   VERIFY: □ Pass □ Fail

5. Both players: Run /f relations
   EXPECTED: Other faction listed as ALLY (green text)
   VERIFY: □ Pass □ Fail

6. Both players: Open GUI, navigate to Relations page
   EXPECTED: Alliance visible with correct details
   VERIFY: □ Pass □ Fail

7. Player1: Change relation to NEUTRAL
   EXPECTED: Alliance broken notification to both
   EXPECTED: Relations page updated
   VERIFY: □ Pass □ Fail

CLEANUP:
- Reset relations to neutral if not done

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

#### QA-005: Combat Tag System (2 Players)
```
Test ID: QA-005
Category: PvP Protection
Players Required: 2
Estimated Time: 5 minutes

PRECONDITIONS:
□ Player1 in FactionA
□ Player2 in FactionB (or no faction)
□ FactionA has a home set
□ Combat tag duration is 15 seconds (default)

STEPS:
1. Player1: Attack Player2 (deal damage)
   EXPECTED: Both receive "You are now in combat" message
   EXPECTED: Combat tag indicator (if HUD implemented)
   VERIFY: □ Pass □ Fail

2. Player1: Immediately run /f home
   EXPECTED: Error "Cannot teleport while in combat"
   VERIFY: □ Pass □ Fail

3. Wait 15 seconds (do not attack again)
   EXPECTED: "Combat tag expired" message (optional)
   VERIFY: □ Pass □ Fail

4. Player1: Run /f home
   EXPECTED: Teleport begins (warmup or instant)
   EXPECTED: Player arrives at faction home
   VERIFY: □ Pass □ Fail

CLEANUP:
- None required

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

#### QA-006: Admin Functions
```
Test ID: QA-006
Category: Administration
Players Required: 1 admin + 1 regular player
Estimated Time: 10 minutes

PRECONDITIONS:
□ AdminPlayer has hyperfactions.admin permission
□ TestPlayer is in a faction
□ No SafeZone exists at test location

STEPS:
1. AdminPlayer: Run /f admin
   EXPECTED: Admin GUI opens
   EXPECTED: Dashboard shows server statistics
   VERIFY: □ Pass □ Fail

2. Navigate to Zones page
   EXPECTED: Zone list visible (may be empty)
   VERIFY: □ Pass □ Fail

3. Create SafeZone named "TestZone" at current location
   EXPECTED: "SafeZone 'TestZone' created"
   EXPECTED: Zone appears in list
   VERIFY: □ Pass □ Fail

4. TestPlayer: Try to claim chunk in SafeZone
   EXPECTED: Error "Cannot claim protected zone territory"
   VERIFY: □ Pass □ Fail

5. AdminPlayer: Run /f admin bypass
   EXPECTED: "Admin bypass enabled"
   VERIFY: □ Pass □ Fail

6. AdminPlayer: Try to break block in TestPlayer's claimed territory
   EXPECTED: Block breaks successfully (bypass active)
   VERIFY: □ Pass □ Fail

7. AdminPlayer: Run /f admin bypass again
   EXPECTED: "Admin bypass disabled"
   VERIFY: □ Pass □ Fail

8. Delete the test SafeZone
   EXPECTED: Zone removed from list
   VERIFY: □ Pass □ Fail

CLEANUP:
- Ensure bypass is disabled
- Ensure test zone is deleted

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

#### QA-007: Help System
```
Test ID: QA-007
Category: Documentation
Players Required: 1
Estimated Time: 5 minutes

PRECONDITIONS:
□ Player can be in or out of faction (test both)

STEPS:
1. Run /f help
   EXPECTED: Overview with category list
   EXPECTED: "Use /f help <category> for more" hint
   VERIFY: □ Pass □ Fail

2. Run /f help territory
   EXPECTED: List of territory commands with descriptions
   VERIFY: □ Pass □ Fail

3. Run /f help claim
   EXPECTED: Detailed help for /f claim command
   EXPECTED: Usage, aliases, requirements shown
   VERIFY: □ Pass □ Fail

4. Run /f help gui
   EXPECTED: Help GUI opens
   EXPECTED: Categories visible
   VERIFY: □ Pass □ Fail

5. Search for "claim" in Help GUI
   EXPECTED: Search results include /f claim and related commands
   VERIFY: □ Pass □ Fail

6. Click on a category card
   EXPECTED: Category page opens with command list
   VERIFY: □ Pass □ Fail

7. Click on a command
   EXPECTED: Command detail page with full information
   VERIFY: □ Pass □ Fail

CLEANUP:
- None required

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

### D.5 Automated Test Commands

```bash
# Run all unit tests
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :HyperFactions:test

# Run specific test class
./gradlew :HyperFactions:test --tests "com.hyperfactions.manager.FactionManagerTest"

# Run tests matching pattern
./gradlew :HyperFactions:test --tests "*ClaimManager*"

# Run with verbose output
./gradlew :HyperFactions:test --info

# Generate test report
# Location: build/reports/tests/test/index.html

# Run tests with coverage (if JaCoCo configured)
./gradlew :HyperFactions:jacocoTestReport
# Coverage report: build/reports/jacoco/test/html/index.html
```

---

### D.6 Implementation Tasks

**Mock Infrastructure (D.1)**

| Task | Description |
|------|-------------|
| D.1.1 | Create mock/ package structure |
| D.1.2 | Implement MockPlayerRef with builder |
| D.1.3 | Implement MockWorld with chunk management |
| D.1.4 | Implement MockStore |
| D.1.5 | Implement MockCommandContext |
| D.1.6 | Create TestFixtures utility class |

**Unit Tests (D.2)**

| Task | Description |
|------|-------------|
| D.2.1 | Write FactionTest (data model) |
| D.2.2 | Write FactionMemberTest |
| D.2.3 | Write FactionManagerTest |
| D.2.4 | Write ClaimManagerTest |
| D.2.5 | Write PowerManagerTest |
| D.2.6 | Write RelationManagerTest |
| D.2.7 | Write InviteManagerTest |
| D.2.8 | Write ZoneManagerTest |
| D.2.9 | Write CombatTagManagerTest |
| D.2.10 | Write CommandRouterTest |
| D.2.11 | Write AliasManagerTest |
| D.2.12 | Write utility class tests |

**Integration Tests (D.3)**

| Task | Description |
|------|-------------|
| D.3.1 | Create TestHarness class |
| D.3.2 | Write FactionLifecycleIntegrationTest |
| D.3.3 | Write AllianceFlowIntegrationTest |
| D.3.4 | Write CombatTagIntegrationTest |
| D.3.5 | Write ProtectionIntegrationTest |

**Manual QA (D.4)**

| Task | Description |
|------|-------------|
| D.4.1 | Create QA-CHECKLIST.md document |
| D.4.2 | Document QA-001 through QA-007 (core tests) |
| D.4.3 | Document QA-008 through QA-015 (advanced tests) |
| D.4.4 | Create QA test data setup guide |

**CI/CD (D.5)**

| Task | Description |
|------|-------------|
| D.5.1 | Configure Gradle test task |
| D.5.2 | Add JaCoCo for coverage reports |
| D.5.3 | Create GitHub Actions workflow (if using GitHub) |

---

## Phase E: Optional Modules

These are larger features planned for future versions. GUI placeholders will be added in Phase B.

### E.1 Faction Treasury/Bank System
- **Status**: Foundation complete (EconomyManager stub exists)
- **Features**:
  - Faction balance tracking
  - Deposit/withdraw commands
  - Transaction logging
  - Tax system (configurable % of member earnings)
  - Permission-based access (who can withdraw)
- **Commands**: `/f money`, `/f money deposit <amount>`, `/f money withdraw <amount>`, `/f money log`
- **GUI**: Treasury page in Modules section

### E.2 Role-Specific Territory Permissions
- **Status**: Not started
- **Features**:
  - Configure per-role: build, break, interact, container access
  - Guest permissions for non-members
  - Ally permissions
  - `/f perms` command and GUI page
- **GUI**: Permissions page in Settings section

### E.3 Raid System
- **Status**: Not started
- **Features**:
  - Structured raid initiation
  - Objectives (core block, flag capture, etc.)
  - 24-hour cooldown between raids on same faction
  - Raid notifications
  - Victory rewards
- **GUI**: Raids page in Modules section (Coming Soon placeholder)

### E.4 Faction Levels/Progression
- **Status**: Not started
- **Features**:
  - XP from: claiming, PvP victories, objectives
  - Level unlocks: increased claims, power bonuses, cosmetics
  - Leaderboard
- **GUI**: Levels page in Modules section (Coming Soon placeholder)

### E.5 War Declaration System
- **Status**: Not started
- **Features**:
  - Formal war declaration
  - War objectives and victory conditions
  - War end conditions (surrender, timer, objectives)
  - War statistics
- **GUI**: War page in Modules section (Coming Soon placeholder)

### E.6 Tasks (Future)

- [ ] **E.1.1** Implement FactionEconomy data model
- [ ] **E.1.2** Implement EconomyManager
- [ ] **E.1.3** Add treasury commands
- [ ] **E.1.4** Create Treasury GUI page
- [ ] **E.2.1** Design permission model
- [ ] **E.2.2** Implement RolePermissionManager
- [ ] **E.2.3** Create Permissions GUI page
- [ ] *(E.3-E.5 tasks to be defined when work begins)*

---

## Research & Future Investigation

Items requiring investigation before implementation. These may reveal Hytale limitations or API requirements.

### R.1 Mob Spawning Control in Zones

**Question**: Can we control or prevent mob spawning in specific regions (SafeZones, WarZones)?

**Investigation Needed**:
- [ ] Research Hytale mob spawning system in decompiled sources
- [ ] Check for spawn events or hooks in EventRegistry
- [ ] Look for world/region-based spawn control APIs
- [ ] Investigate if chunk-level spawn rules are possible
- [ ] Check AdminUI or other mods for spawn control patterns

**Use Cases**:
- SafeZones: No hostile mob spawning (peaceful areas)
- WarZones: Potentially increased spawning or specific mob types
- Claimed territory: Configurable spawn rules per faction setting

**Relevant Files to Check**:
- `HytaleServerDocs/decompiled/` - Search for spawn-related classes
- `HytaleServerDocs/docs/reference/events.md` - Check for spawn events
- `resources/hytale-modding/` - Community spawn documentation

**Status**: Not started

---

### R.2 Block Protection Events

**Question**: What events are available for protecting blocks in claimed territory?

**Investigation Needed**:
- [ ] Document all block-related events (break, place, interact)
- [ ] Check for container access events
- [ ] Investigate explosion damage events (when explosives are added)
- [ ] Research entity-block interaction events

**Status**: Partially complete (basic block break/place working)

---

### R.3 Custom Context Providers for HyperPerms

**Question**: How can HyperFactions register custom context providers for HyperPerms?

**Investigation Needed**:
- [ ] Design API for faction context (faction name, role, territory type)
- [ ] Document integration pattern for other plugins
- [ ] Test performance impact of context lookups

**Planned Contexts**:
- `faction`: Player's faction name
- `faction_role`: LEADER, OFFICER, MEMBER
- `faction_territory`: own, ally, enemy, wilderness, zone

**Status**: Not started

---

### R.4 Interactive Map Rendering

**Question**: How can we render an interactive chunk map in the Hytale UI?

**Investigation Needed**:
- [ ] Research CustomUI capabilities for dynamic grid rendering
- [ ] Check if image generation is possible (chunk map image)
- [ ] Investigate click coordinates for grid interactions
- [ ] Look at minimap mods for patterns

**Constraints**:
- UI elements must be defined in templates
- Dynamic style changes crash (per Technical Reference)
- May need pre-generated cell templates

**Status**: Not started

---

### R.5 Clipboard / Copy Command Functionality

**Question**: Can we copy text (commands) to the player's clipboard from the Hytale UI?

**Investigation Needed**:
- [ ] Research Hytale client clipboard access
- [ ] Check if any existing mods implement copy functionality
- [ ] Investigate CustomUI text selection capabilities
- [ ] Look for chat input pre-fill alternatives (if clipboard not possible)

**Use Case**:
- Help GUI "Copy command" button
- Copy faction names, player names
- Copy coordinates

**Alternative if Not Possible**:
- "Click to insert in chat" - pre-fills chat input with command
- Just display command clearly for manual typing

**Status**: Not started

---

### R.6 Caching System (Performance)

**Question**: How can we implement efficient caching for faction data similar to HyperPerms?

**Context**: HyperFactions will store significant state information across features (factions, members, claims, power, relations, combat tags, zones, invites). Reading from storage on every access will cause performance issues.

**Investigation Needed**:
- [ ] Review HyperPerms caching implementation in `com.hyperperms.cache/`
- [ ] Analyze HyperPerms cache configuration options
- [ ] Document cache invalidation (cache busting) patterns used
- [ ] Identify which HyperFactions data benefits most from caching
- [ ] Design cache hierarchy (hot data vs. cold data)

**Data to Cache (Priority Order)**:
1. **Hot (frequent access)**:
   - Faction memberships (player → faction lookup)
   - Chunk claims (chunk → faction lookup)
   - Combat tags (active tags)
   - Power values (for claim calculations)
2. **Warm (moderate access)**:
   - Faction data (name, description, home, settings)
   - Relations (ally/enemy status)
   - Player profiles (last seen, deaths, kills)
3. **Cold (infrequent access)**:
   - Zone definitions (rarely change)
   - Audit logs (write-heavy, read-infrequent)

**Cache Invalidation Triggers**:
- Player joins/leaves faction
- Faction created/disbanded
- Territory claimed/unclaimed
- Relation changed
- Power updated
- Config reload

**HyperPerms Patterns to Review**:
- `PermissionCache` - Main caching class
- `CacheConfiguration` - TTL, max size, eviction policies
- `CacheInvalidator` - Event-based invalidation
- Caffeine library integration

**Configuration Goals**:
```yaml
cache:
  enabled: true
  faction-membership:
    ttl: 300  # seconds
    max-size: 1000
  chunk-claims:
    ttl: 60
    max-size: 10000
  power:
    ttl: 30
    max-size: 500
```

**Status**: Not started

---

### R.7 Chat Input Pre-Fill (Click-to-Suggest)

**Question**: Can we pre-fill the chat input with command syntax when a player clicks a command in the Help GUI?

**Investigation Needed**:
- [ ] Research Hytale client chat input APIs in decompiled sources
- [ ] Check for chat suggestion events or hooks in EventRegistry
- [ ] Look at how other mods handle command suggestions
- [ ] Test if closing a CustomUI page and sending a chat message works
- [ ] Investigate if there's a "suggest command" packet/API

**Use Cases**:
- Help GUI: Click `/f claim` to pre-fill in chat
- Territory Map: Click "Claim" button, command appears in chat
- Quick action buttons that suggest commands instead of executing

**Alternative Approaches if Not Possible**:
1. Copy to clipboard (requires R.5 research)
2. Execute command directly (current approach)
3. Display command prominently for manual typing

**Related to**: R.5 (Clipboard functionality)

**Status**: Not started

---

## Known Issues

### Pitch/Yaw Bug (TABLED)
- **Symptom**: `/f home` teleport has wrong camera orientation
- **Investigation Done**:
  - Tried swapping pitch/yaw order in Vector3f
  - Checked FactionHome record field mapping
- **Likely Cause**: Issue in how home orientation is saved or applied
- **Status**: Tabled for later investigation - not blocking release

### Admin Zones Page Back Button
- **Symptom**: Back button positioned incorrectly
- **Fix**: Update AdminZonePage template positioning
- **Priority**: P2

---

## Technical Reference

### CustomUI Constraints

**What Works**:
```java
cmd.append("path/to/template.ui")     // Load templates
cmd.set("#ElementId.Text", "value")   // Set text content
cmd.set("#ElementId.TextSpans", Message.raw("text"))  // Set formatted text
events.addEventBinding()              // Bind event handlers
```

**What Does NOT Work**:
```java
cmd.set("#ElementId.Style", "...")    // CRASHES - Cannot set styles dynamically
```

**Solution**: Create separate template files for different states.

### Event Binding Pattern

**Correct pattern for nav buttons**:
```java
EventData.of("Button", "Nav").append("NavBar", entry.id())
```

**Key**: Must set `button` field to non-null value, otherwise handler returns early.

### Native Back Button

Use `$C.@BackButton {}` at end of UI template. Do NOT bind custom events - Hytale handles dismissal.

### Hytale Limitations (Cannot Implement)

- Explosion Protection - No explosive devices yet
- Mechanical Block Protection - No block movement mechanics
- Item Transporter Protection - No automated transport systems

---

## Version Planning

### v1.0.0 (Current - Stabilization)
- Core faction system complete
- Basic GUI system working
- Production ready pending remaining fixes

### v1.1.0 - Command & GUI Overhaul
- Phase A: Command system refactor
- Phase B.1: New Player GUI
- Phase B.2: Enhanced Faction Player GUI
- Phase C.1: Chat help improvements

### v1.2.0 - Admin & Testing
- Phase B.3: Admin GUI
- Phase D: Testing infrastructure
- Phase C.2: Help GUI

### v1.3.0 - Modules
- Phase E.1: Treasury system
- Phase E.2: Role permissions
- Interactive chunk map (from Phase B)

### v1.4.0+ (Future)
- Phase E.3: Raid system
- Phase E.4: Faction levels
- Phase E.5: War system

---

## Reference Documentation

- [HytaleModding Community Docs](../resources/hytale-modding/content/docs/en/)
- [CustomUI Guide](../resources/hytale-modding/content/docs/en/guides/plugin/ui.mdx)
- [UI Patterns](../resources/UI-PATTERNS.md)
- [AdminUI Analysis](../resources/AdminUI.md)

---

## Contributing

For feature requests or bugs: https://github.com/HyperSystemsDev/HyperFactions/issues
