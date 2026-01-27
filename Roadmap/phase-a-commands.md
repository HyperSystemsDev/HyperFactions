# Phase A: Command System Overhaul

> **Status**: NOT STARTED
> **Target Version**: v1.1.0
> **Last Updated**: January 27, 2026

**Goal**: Transform from monolithic switch-based routing to modular, GUI-first command architecture with context-aware defaults and configurable aliases.

**Design Decisions**:
- **Handler Architecture**: Hybrid approach - self-contained handlers using shared utility classes
- **GUI-First Pattern**: Context-aware defaults based on current game state
- **Aliases**: Configurable per-command with multiple alias support

---

## Current State Analysis (2026-01-27)

| Metric | Value |
|--------|-------|
| FactionCommand.java | 2,678 lines |
| Subcommands | 39+ |
| Architecture | Monolithic switch statement |
| Helper Classes | FactionCommandContext (157 lines), CommandHelp, HelpFormatter |

**Current Files:**
- `command/FactionCommand.java` (2,678 lines) - All 39+ subcommands in one file
- `command/FactionCommandContext.java` (157 lines) - Flag parsing utility (`--text` / `-t`)
- `util/CommandHelp.java` (45 lines) - Help entry record
- `util/HelpFormatter.java` - Chat help formatting

**Existing Patterns:**
- GUI-first design already exists (commands default to GUI, `--text` flag for CLI)
- Handler methods organized by category but all in one file
- FactionCommandContext.shouldOpenGui() and shouldOpenGuiAfterAction() determine GUI behavior

---

## A.1 Command Architecture

**Current State**: Single `FactionCommand.java` (2,678 lines) with switch statement routing to 39+ private handler methods.

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

## A.2 Shared Utility Classes

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

## A.3 State-Based Routing

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

## A.4 Context-Aware Command Defaults

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

## A.5 Configurable Aliases

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

## A.6 Command Groups & Handlers

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

## A.7 Implementation Tasks

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
