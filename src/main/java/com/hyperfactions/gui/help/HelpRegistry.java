package com.hyperfactions.gui.help;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Central registry of all help content.
 * Provides lookup by category, topic ID, or command name.
 */
public final class HelpRegistry {

    private static final HelpRegistry INSTANCE = new HelpRegistry();

    private final Map<HelpCategory, List<HelpTopic>> topicsByCategory = new EnumMap<>(HelpCategory.class);
    private final Map<String, HelpTopic> topicsById = new HashMap<>();
    private final Map<String, HelpCategory> categoryByCommand = new HashMap<>();

    private HelpRegistry() {
        initializeContent();
    }

    public static HelpRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Gets all topics for a category.
     */
    @NotNull
    public List<HelpTopic> getTopics(@NotNull HelpCategory category) {
        return topicsByCategory.getOrDefault(category, List.of());
    }

    /**
     * Gets a topic by its ID.
     */
    @Nullable
    public HelpTopic getTopic(@NotNull String topicId) {
        return topicsById.get(topicId);
    }

    /**
     * Finds the category associated with a command.
     * Used for deep-linking from /f <command> help.
     */
    @Nullable
    public HelpCategory getCategoryForCommand(@NotNull String command) {
        return categoryByCommand.get(command.toLowerCase());
    }

    private void register(@NotNull HelpTopic topic) {
        topicsByCategory.computeIfAbsent(topic.category(), k -> new ArrayList<>()).add(topic);
        topicsById.put(topic.id(), topic);
        for (String cmd : topic.commands()) {
            categoryByCommand.put(cmd.toLowerCase(), topic.category());
        }
    }

    private void registerCommandMapping(@NotNull String command, @NotNull HelpCategory category) {
        categoryByCommand.put(command.toLowerCase(), category);
    }

    private void initializeContent() {
        // =====================================================================
        // GETTING STARTED
        // =====================================================================

        register(HelpTopic.of("gs_what", "What Are Factions?", List.of(
                "Factions are teams of players who work together",
                "to claim land, build bases, and grow stronger.",
                "",
                "When you join a faction, you get:",
                "- Protected territory only your team can build in",
                "- A home base you can teleport to anytime",
                "- Private chat with your faction members",
                "- Allies to fight alongside you",
                "",
                "Work together, claim land, and dominate!"
        ), HelpCategory.GETTING_STARTED));

        register(HelpTopic.of("gs_first", "Your First Steps", List.of(
                "New to factions? Here's what to do:",
                "",
                "1. Type /f to open the faction menu",
                "",
                "2. Choose your path:",
                "   - Browse to find a faction to join",
                "   - Create to start your own faction",
                "",
                "3. Once in a faction, explore your territory,",
                "   meet your teammates, and start building!",
                "",
                "Tip: Check the Invites tab - someone may",
                "have already invited you to join them!"
        ), HelpCategory.GETTING_STARTED));

        register(HelpTopic.of("gs_joining", "How to Join a Faction", List.of(
                "There are three ways to join a faction:",
                "",
                "BROWSE OPEN FACTIONS",
                "Type /f and click Browse to see factions",
                "accepting new members. Click Join to hop in!",
                "",
                "ACCEPT AN INVITATION",
                "If someone invited you, check the Invites",
                "tab and click Accept to join their faction.",
                "",
                "REQUEST TO JOIN",
                "Found an invite-only faction you like?",
                "Click Request and wait for them to accept."
        ), HelpCategory.GETTING_STARTED));

        register(HelpTopic.of("gs_creating", "Starting Your Own Faction", List.of(
                "Want to be the leader? Create your own!",
                "",
                "Type /f and click Create to get started.",
                "You'll pick a name, choose a color, and",
                "set up your faction's description.",
                "",
                "As the leader, you can:",
                "- Invite friends to join you",
                "- Claim land for your base",
                "- Promote trusted members to officers",
                "- Form alliances with other factions",
                "",
                "Build your empire from the ground up!"
        ), HelpCategory.GETTING_STARTED));

        register(HelpTopic.of("gs_basics", "Quick Tips", List.of(
                "CLAIMING LAND",
                "Use /f claim to protect the area you're in.",
                "Only faction members can build there.",
                "",
                "FACTION HOME",
                "Set a home with /f sethome, then use",
                "/f home to teleport back anytime.",
                "",
                "FACTION CHAT",
                "Use /f c <message> to chat privately",
                "with only your faction members.",
                "",
                "POWER",
                "Your faction needs power to hold land.",
                "Stay online to regenerate. Dying loses power!"
        ), HelpCategory.GETTING_STARTED));

        // =====================================================================
        // FACTION BASICS
        // =====================================================================

        register(HelpTopic.withCommands("fb_create", "/f create", List.of(
                "Create a new faction.",
                "",
                "/f create",
                "  Opens the faction creation wizard GUI.",
                "  Guides you through name, color, settings.",
                "",
                "/f create <name>",
                "  Creates faction immediately.",
                "  Then opens your faction dashboard.",
                "",
                "Requires: Not in a faction"
        ), List.of("create"), HelpCategory.FACTION_BASICS));

        register(HelpTopic.withCommands("fb_disband", "/f disband", List.of(
                "Permanently disband your faction.",
                "",
                "/f disband",
                "  Opens confirmation dialog GUI.",
                "  Click Confirm to proceed.",
                "",
                "Requires: Leader only",
                "",
                "WARNING: This deletes your faction!",
                "All claims released, members removed."
        ), List.of("disband"), HelpCategory.FACTION_BASICS));

        register(HelpTopic.withCommands("fb_leave", "/f leave", List.of(
                "Leave your current faction.",
                "",
                "/f leave",
                "  Opens confirmation dialog GUI.",
                "",
                "Requires: Any member",
                "",
                "If you're the leader:",
                "- Highest officer becomes new leader",
                "- If no other members, faction disbands"
        ), List.of("leave"), HelpCategory.FACTION_BASICS));

        register(HelpTopic.withCommands("fb_invite", "/f invite", List.of(
                "Invite a player to your faction.",
                "",
                "/f invite",
                "  Opens the Invites management page.",
                "  View sent invites, join requests.",
                "",
                "/f invite <player>",
                "  Invites the player immediately.",
                "",
                "Requires: Officer or Leader"
        ), List.of("invite"), HelpCategory.FACTION_BASICS));

        register(HelpTopic.withCommands("fb_accept", "/f accept", List.of(
                "Accept an invitation to join a faction.",
                "",
                "/f accept",
                "  Opens your Invites page.",
                "  Click Accept on an invitation.",
                "",
                "/f accept <faction>",
                "  Accepts that faction's invite.",
                "  (Chat confirmation, no GUI after)",
                "",
                "Aliases: /f join <faction>",
                "",
                "Requires: Have a pending invite",
                "Note: You cannot be in a faction already."
        ), List.of("accept", "join"), HelpCategory.FACTION_BASICS));

        register(HelpTopic.withCommands("fb_request", "/f request", List.of(
                "Request to join an invite-only faction.",
                "",
                "/f request",
                "  Opens the Faction Browser.",
                "  Find factions and send requests.",
                "",
                "/f request <faction>",
                "  Sends join request immediately.",
                "",
                "/f request <faction> [message]",
                "  Include a message with your request.",
                "  Example: /f request Elite Active player",
                "",
                "Requires: Not in a faction"
        ), List.of("request"), HelpCategory.FACTION_BASICS));

        register(HelpTopic.withCommands("fb_kick", "/f kick", List.of(
                "Remove a member from your faction.",
                "",
                "/f kick <player>",
                "  Kicks the player immediately.",
                "  Then opens Members page.",
                "",
                "Requires: Officer or Leader",
                "",
                "Permissions:",
                "- Officers can kick Members",
                "- Leaders can kick Officers & Members",
                "- Cannot kick the Leader"
        ), List.of("kick"), HelpCategory.FACTION_BASICS));

        register(HelpTopic.of("fb_roles", "Faction Roles", List.of(
                "LEADER (1 per faction)",
                "- Full control over faction",
                "- Disband, transfer leadership",
                "- Promote and demote members",
                "- All officer permissions",
                "",
                "OFFICER",
                "- Invite and kick members",
                "- Claim and unclaim territory",
                "- Set faction home, description",
                "- Manage diplomatic relations",
                "",
                "MEMBER",
                "- Use faction home and chat",
                "- Build in faction territory"
        ), HelpCategory.FACTION_BASICS));

        register(HelpTopic.withCommands("fb_promote", "/f promote", List.of(
                "Promote a member to officer rank.",
                "",
                "/f promote <player>",
                "  Promotes immediately.",
                "  Then opens Members page.",
                "",
                "Requires: Leader only",
                "",
                "Officers gain permissions:",
                "- Invite/kick, claim/unclaim",
                "- Set home, manage relations"
        ), List.of("promote"), HelpCategory.FACTION_BASICS));

        register(HelpTopic.withCommands("fb_demote", "/f demote", List.of(
                "Demote an officer to member rank.",
                "",
                "/f demote <player>",
                "  Demotes immediately.",
                "  Then opens Members page.",
                "",
                "Requires: Leader only",
                "",
                "The player remains in the faction",
                "but loses officer permissions."
        ), List.of("demote"), HelpCategory.FACTION_BASICS));

        register(HelpTopic.withCommands("fb_transfer", "/f transfer", List.of(
                "Transfer faction leadership.",
                "",
                "/f transfer <player>",
                "  Transfers immediately.",
                "  (Chat command, no GUI)",
                "",
                "Requires: Leader only",
                "",
                "After transfer:",
                "- Target becomes Leader",
                "- You become an Officer",
                "",
                "WARNING: This cannot be undone!",
                "Choose your successor carefully."
        ), List.of("transfer"), HelpCategory.FACTION_BASICS));

        // =====================================================================
        // TERRITORY & CLAIMS
        // =====================================================================

        register(HelpTopic.withCommands("tr_claim", "/f claim", List.of(
                "Claim the chunk you're standing in.",
                "",
                "/f claim",
                "  Claims the current chunk.",
                "  Then opens the Chunk Map.",
                "",
                "Requires: Officer or Leader",
                "",
                "Claims protect your territory:",
                "- Only members can build/break",
                "- Containers are protected",
                "- Entry alerts when others enter"
        ), List.of("claim"), HelpCategory.TERRITORY));

        register(HelpTopic.withCommands("tr_unclaim", "/f unclaim", List.of(
                "Release the current chunk.",
                "",
                "/f unclaim",
                "  Unclaims the chunk.",
                "  Then opens the Chunk Map.",
                "",
                "Requires: Officer or Leader",
                "",
                "The chunk becomes wilderness.",
                "Anyone can build there.",
                "You regain the power cost."
        ), List.of("unclaim"), HelpCategory.TERRITORY));

        register(HelpTopic.withCommands("tr_map", "/f map", List.of(
                "View the territory map.",
                "",
                "/f map",
                "  Opens interactive Chunk Map GUI.",
                "  Click chunks to claim/unclaim.",
                "",
                "Map legend:",
                "- Your faction: Your color",
                "- Allies: Blue tint",
                "- Enemies: Red tint",
                "- Neutral: Gray",
                "- Wilderness: Dark"
        ), List.of("map"), HelpCategory.TERRITORY));

        register(HelpTopic.withCommands("tr_overclaim", "/f overclaim", List.of(
                "Take territory from a weakened faction.",
                "",
                "/f overclaim",
                "  Overclaims the current chunk.",
                "  (Chat command only)",
                "",
                "Requires: Officer or Leader",
                "",
                "Overclaim conditions:",
                "- Target's claims exceed their power",
                "- You have power for this claim",
                "- Chunk is enemy or neutral",
                "",
                "This is how factions lose land!",
                "Keep your power up to stay safe."
        ), List.of("overclaim"), HelpCategory.TERRITORY));

        register(HelpTopic.withCommands("tr_home", "/f home", List.of(
                "Teleport to your faction's home.",
                "",
                "/f home",
                "  Starts teleport warmup.",
                "  Stay still during countdown.",
                "  (Chat command only)",
                "",
                "Restrictions:",
                "- Cannot use while combat tagged",
                "- Has warmup countdown",
                "- Has cooldown between uses",
                "- Faction must have home set",
                "",
                "Officers set home with /f sethome."
        ), List.of("home"), HelpCategory.TERRITORY));

        register(HelpTopic.withCommands("tr_sethome", "/f sethome", List.of(
                "Set the faction home location.",
                "",
                "/f sethome",
                "  Sets home at your location.",
                "  (Chat command only)",
                "",
                "Requires: Officer or Leader",
                "",
                "Requirements:",
                "- Must be in claimed territory",
                "- Territory must belong to your faction",
                "",
                "All members can /f home to here."
        ), List.of("sethome"), HelpCategory.TERRITORY));

        register(HelpTopic.withCommands("tr_stuck", "/f stuck", List.of(
                "Emergency escape from enemy territory.",
                "",
                "/f stuck",
                "  Teleports to nearest safe location.",
                "  (Chat command only)",
                "",
                "Features:",
                "- Extended warmup (30 seconds)",
                "- Finds nearest wilderness/own/ally",
                "- Cannot use while combat tagged",
                "",
                "Use when trapped in enemy land",
                "with no other way out."
        ), List.of("stuck"), HelpCategory.TERRITORY));

        register(HelpTopic.withCommands("tr_power", "Power System", List.of(
                "/f power [player]",
                "  View power for yourself or a player.",
                "  Opens Player Info page (GUI).",
                "",
                "How power works:",
                "- Each player has personal power",
                "- Faction power = sum of members",
                "- Power regenerates while online",
                "- Death causes power loss",
                "",
                "If claims > power, you're raidable!"
        ), List.of("power"), HelpCategory.TERRITORY));

        // =====================================================================
        // RELATIONS & DIPLOMACY
        // =====================================================================

        register(HelpTopic.of("rl_overview", "Diplomatic Relations", List.of(
                "Factions can set relations:",
                "",
                "ALLY (Mutual)",
                "- Friendly fire protection",
                "- See each other on maps",
                "- Cannot claim ally land",
                "- Requires BOTH factions to agree",
                "",
                "ENEMY (One-way)",
                "- PvP enabled in territories",
                "- Can overclaim if underpowered",
                "- Only you need to declare",
                "",
                "NEUTRAL (Default)",
                "- Standard protection rules"
        ), HelpCategory.RELATIONS));

        register(HelpTopic.withCommands("rl_ally", "/f ally", List.of(
                "Request an alliance.",
                "",
                "/f ally <faction>",
                "  Sends alliance request.",
                "  Then opens Relations page.",
                "",
                "Requires: Officer or Leader",
                "",
                "Alliances are MUTUAL - both must agree.",
                "The other faction reviews your request."
        ), List.of("ally"), HelpCategory.RELATIONS));

        register(HelpTopic.withCommands("rl_enemy", "/f enemy", List.of(
                "Declare another faction as enemy.",
                "",
                "/f enemy <faction>",
                "  Declares enemy immediately.",
                "  Then opens Relations page.",
                "",
                "Requires: Officer or Leader",
                "",
                "Enemy declaration is ONE-WAY.",
                "No agreement needed from them."
        ), List.of("enemy"), HelpCategory.RELATIONS));

        register(HelpTopic.withCommands("rl_neutral", "/f neutral", List.of(
                "Reset relation to neutral.",
                "",
                "/f neutral <faction>",
                "  Sets relation to neutral.",
                "  Then opens Relations page.",
                "",
                "Requires: Officer or Leader",
                "",
                "Ends alliance or enemy status.",
                "Returns to default relation."
        ), List.of("neutral"), HelpCategory.RELATIONS));

        register(HelpTopic.withCommands("rl_relations", "/f relations", List.of(
                "View all faction relations.",
                "",
                "/f relations",
                "  Opens Relations page (GUI).",
                "  See allies, enemies, requests.",
                "",
                "From the GUI, officers can:",
                "- Accept/decline ally requests",
                "- Set new relations",
                "- Remove existing relations"
        ), List.of("relations"), HelpCategory.RELATIONS));

        // =====================================================================
        // COMBAT & PROTECTION
        // =====================================================================

        register(HelpTopic.of("cb_tagging", "Combat Tagging", List.of(
                "When you attack or are attacked,",
                "you become 'combat tagged'.",
                "",
                "While tagged:",
                "- Cannot /f home or /f stuck",
                "- Unsafe to log out",
                "- Timer shows remaining duration",
                "",
                "Tag duration: Configured by server",
                "Resets with each combat action.",
                "",
                "Don't start fights you can't finish!"
        ), HelpCategory.COMBAT));

        register(HelpTopic.of("cb_territory", "Territory Protection", List.of(
                "Claimed land provides protection:",
                "",
                "BLOCK PROTECTION",
                "- Only members can build/break",
                "- Outsiders cannot modify terrain",
                "",
                "CONTAINER PROTECTION",
                "- Chests, barrels are secured",
                "- Only members can access",
                "",
                "ENTRY ALERTS",
                "- Notified when others enter",
                "",
                "NOTE: Claims protect BLOCKS.",
                "Enemies CAN still attack YOU!"
        ), HelpCategory.COMBAT));

        register(HelpTopic.of("cb_pvp", "PvP Rules", List.of(
                "PvP depends on location & relations:",
                "",
                "YOUR TERRITORY",
                "- Allies: No damage",
                "- Enemies: CAN attack you",
                "- Neutral: Server rules apply",
                "",
                "ENEMY TERRITORY",
                "- Full PvP enabled both ways",
                "",
                "ALLY TERRITORY",
                "- Friendly fire disabled",
                "",
                "WILDERNESS",
                "- Server PvP rules apply"
        ), HelpCategory.COMBAT));

        register(HelpTopic.of("cb_zones", "Special Zones", List.of(
                "Admin-created zones with special rules:",
                "",
                "SAFEZONE",
                "- No PvP damage",
                "- No block breaking",
                "- Spawn areas, trading posts",
                "",
                "WARZONE",
                "- PvP always enabled",
                "- No territorial protection",
                "- Designated battle areas",
                "",
                "Zone rules override faction rules."
        ), HelpCategory.COMBAT));

        register(HelpTopic.of("cb_death", "Death & Power Loss", List.of(
                "When you die, you lose power:",
                "",
                "- Personal power decreases",
                "- Faction total decreases",
                "- If claims > power: RAIDABLE",
                "",
                "Power regenerates while online.",
                "",
                "Multiple deaths can leave your",
                "faction vulnerable to overclaiming.",
                "",
                "Protect your members in combat!"
        ), HelpCategory.COMBAT));

        // =====================================================================
        // COMMANDS REFERENCE
        // =====================================================================

        // Legend and syntax guide - MUST be first
        register(HelpTopic.of("cmd_legend", "Command Syntax Guide", List.of(
                "<arg>  = Required argument",
                "[arg]  = Optional argument",
                "a|b    = Choose one option",
                "",
                "Most commands open a GUI when run",
                "without arguments."
        ), HelpCategory.COMMANDS));

        // Core commands - creating and joining factions
        register(HelpTopic.of("cmd_core", "Core Commands", List.of(
                "/f",
                "  Open faction main menu",
                "/f help",
                "  Open this help center",
                "/f create [name]",
                "  Create a new faction",
                "/f disband",
                "  Permanently delete your faction (Leader)",
                "/f leave",
                "  Leave your current faction",
                "/f invite <player>",
                "  Invite a player to join (Officer+)",
                "/f accept [faction]",
                "  Accept a faction invitation",
                "/f join [faction]",
                "  Alias for accept",
                "/f request <faction> [message]",
                "  Request to join a closed faction",
                "/f kick <player>",
                "  Remove a member (Officer+)"
        ), HelpCategory.COMMANDS));

        // Information commands
        register(HelpTopic.of("cmd_info", "Information Commands", List.of(
                "/f info [faction]",
                "  View faction details",
                "/f list",
                "  Browse all factions",
                "/f members",
                "  View your faction's roster",
                "/f invites",
                "  Manage invites and join requests",
                "/f who [player]",
                "  View player info (yourself if none)",
                "/f power [player]",
                "  Check power levels",
                "/f relations",
                "  View diplomatic relations"
        ), HelpCategory.COMMANDS));

        // Territory commands
        register(HelpTopic.of("cmd_territory", "Territory Commands", List.of(
                "/f claim",
                "  Claim the chunk you're in (Officer+)",
                "/f unclaim",
                "  Release the current chunk (Officer+)",
                "/f overclaim",
                "  Take land from a weakened faction",
                "/f map",
                "  View interactive territory map",
                "/f home",
                "  Teleport to faction home",
                "/f sethome",
                "  Set faction home location (Officer+)",
                "/f stuck",
                "  Emergency escape from enemy land"
        ), HelpCategory.COMMANDS));

        // Member management
        register(HelpTopic.of("cmd_members", "Member Management", List.of(
                "/f promote <player>",
                "  Promote member to Officer (Leader)",
                "/f demote <player>",
                "  Demote Officer to member (Leader)",
                "/f transfer <player>",
                "  Transfer leadership (Leader)"
        ), HelpCategory.COMMANDS));

        // Diplomacy commands
        register(HelpTopic.of("cmd_diplomacy", "Diplomacy Commands", List.of(
                "/f ally <faction>",
                "  Send alliance request (Officer+)",
                "/f enemy <faction>",
                "  Declare faction as enemy (Officer+)",
                "/f neutral <faction>",
                "  Reset to neutral relation (Officer+)"
        ), HelpCategory.COMMANDS));

        // Settings commands
        register(HelpTopic.of("cmd_settings", "Faction Settings", List.of(
                "/f settings",
                "  Open faction settings GUI (Officer+)",
                "/f rename <name>",
                "  Rename your faction (Leader)",
                "/f desc [text]",
                "  Set faction description (Officer+)",
                "/f color <code>",
                "  Set faction color: 0-9, a-f (Officer+)",
                "/f open",
                "  Allow anyone to join (Officer+)",
                "/f close",
                "  Require invitation to join (Officer+)"
        ), HelpCategory.COMMANDS));

        // Chat and utilities
        register(HelpTopic.of("cmd_other", "Chat & Utilities", List.of(
                "/f chat <message>",
                "  Send message to faction members",
                "/f c <message>",
                "  Shorthand for faction chat",
                "/f gui",
                "  Open faction main menu"
        ), HelpCategory.COMMANDS));

        // Admin commands - General
        register(HelpTopic.of("cmd_admin1", "Admin: General", List.of(
                "Requires hyperfactions.admin permission",
                "",
                "/f admin",
                "  Open admin control panel",
                "/f admin reload",
                "  Reload configuration from disk",
                "/f admin sync",
                "  Sync faction data from storage",
                "/f admin factions",
                "  Faction management GUI",
                "/f admin config",
                "  Configuration editor GUI",
                "/f admin update",
                "  Check for and install updates"
        ), HelpCategory.COMMANDS));

        // Admin commands - Backups
        register(HelpTopic.of("cmd_admin2", "Admin: Backups", List.of(
                "/f admin backups",
                "  Backup management GUI",
                "/f admin backup create [name]",
                "  Create a manual backup",
                "/f admin backup list",
                "  List all available backups",
                "/f admin backup restore <name>",
                "  Restore data from a backup",
                "/f admin backup delete <name>",
                "  Delete a backup file",
                "/f admin import hyfactions <path> [flags]",
                "  Import data from HyFactions mod"
        ), HelpCategory.COMMANDS));

        // Admin commands - Zones
        register(HelpTopic.of("cmd_admin3", "Admin: Zones", List.of(
                "/f admin zone",
                "  Zone management GUI",
                "/f admin zone list",
                "  List all defined zones",
                "/f admin zone info [name]",
                "  View zone details",
                "/f admin zone create <safe|war> <name>",
                "  Create a new zone (empty)",
                "/f admin zone delete <name>",
                "  Delete a zone and release chunks",
                "/f admin zone rename <old> <new>",
                "  Rename a zone",
                "/f admin zone claim <name>",
                "  Add current chunk to zone",
                "/f admin zone unclaim",
                "  Remove current chunk from zone",
                "/f admin zone radius <name> <r> [circle|square]",
                "  Claim radius of chunks for zone"
        ), HelpCategory.COMMANDS));

        // Admin commands - Zone shortcuts
        register(HelpTopic.of("cmd_admin4", "Admin: Zone Shortcuts", List.of(
                "/f admin safezone [name]",
                "  Create SafeZone + claim current chunk",
                "/f admin warzone [name]",
                "  Create WarZone + claim current chunk",
                "/f admin removezone",
                "  Unclaim current chunk from its zone",
                "/f admin zoneflag <flag> <true|false|clear>",
                "  Set a zone flag on current chunk",
                "/f admin zoneflag clearall",
                "  Reset all flags to zone defaults"
        ), HelpCategory.COMMANDS));

        // Admin commands - Debug
        register(HelpTopic.of("cmd_admin5", "Admin: Debug", List.of(
                "/f admin debug",
                "  Show debug command help",
                "/f admin debug status",
                "  Show which debug categories are on",
                "/f admin debug toggle <category> [on|off]",
                "  Toggle debug logging for a category",
                "",
                "Categories: power, claim, combat,",
                "protection, relation, territory, all",
                "",
                "/f admin debug power <player>",
                "  Inspect player/faction power",
                "/f admin debug claim [x z]",
                "  Inspect claim at location",
                "/f admin debug protection <player>",
                "  Check protection rules at location",
                "/f admin debug combat <player>",
                "  View combat tag status",
                "/f admin debug relation <faction1> <faction2>",
                "  Check relation between factions"
        ), HelpCategory.COMMANDS));

        // Additional command mappings for deep-linking
        // Getting Started
        registerCommandMapping("gui", HelpCategory.GETTING_STARTED);
        registerCommandMapping("help", HelpCategory.GETTING_STARTED);
        registerCommandMapping("menu", HelpCategory.GETTING_STARTED);

        // Faction Basics
        registerCommandMapping("info", HelpCategory.FACTION_BASICS);
        registerCommandMapping("show", HelpCategory.FACTION_BASICS);
        registerCommandMapping("list", HelpCategory.FACTION_BASICS);
        registerCommandMapping("browse", HelpCategory.FACTION_BASICS);
        registerCommandMapping("members", HelpCategory.FACTION_BASICS);
        registerCommandMapping("invites", HelpCategory.FACTION_BASICS);
        registerCommandMapping("who", HelpCategory.FACTION_BASICS);
        registerCommandMapping("rename", HelpCategory.FACTION_BASICS);
        registerCommandMapping("desc", HelpCategory.FACTION_BASICS);
        registerCommandMapping("description", HelpCategory.FACTION_BASICS);
        registerCommandMapping("color", HelpCategory.FACTION_BASICS);
        registerCommandMapping("open", HelpCategory.FACTION_BASICS);
        registerCommandMapping("close", HelpCategory.FACTION_BASICS);
        registerCommandMapping("settings", HelpCategory.FACTION_BASICS);

        // Territory
        registerCommandMapping("map", HelpCategory.TERRITORY);

        // Relations
        registerCommandMapping("ally", HelpCategory.RELATIONS);
        registerCommandMapping("enemy", HelpCategory.RELATIONS);
        registerCommandMapping("neutral", HelpCategory.RELATIONS);
        registerCommandMapping("relations", HelpCategory.RELATIONS);

        // Commands/Other
        registerCommandMapping("chat", HelpCategory.COMMANDS);
        registerCommandMapping("c", HelpCategory.COMMANDS);
        registerCommandMapping("debug", HelpCategory.COMMANDS);

        // Admin commands
        registerCommandMapping("admin", HelpCategory.COMMANDS);
        registerCommandMapping("reload", HelpCategory.COMMANDS);
        registerCommandMapping("sync", HelpCategory.COMMANDS);
        registerCommandMapping("zone", HelpCategory.COMMANDS);
        registerCommandMapping("zones", HelpCategory.COMMANDS);
        registerCommandMapping("safezone", HelpCategory.COMMANDS);
        registerCommandMapping("warzone", HelpCategory.COMMANDS);
        registerCommandMapping("removezone", HelpCategory.COMMANDS);
        registerCommandMapping("zoneflag", HelpCategory.COMMANDS);
        registerCommandMapping("backup", HelpCategory.COMMANDS);
        registerCommandMapping("backups", HelpCategory.COMMANDS);
        registerCommandMapping("config", HelpCategory.COMMANDS);
        registerCommandMapping("factions", HelpCategory.COMMANDS);
        registerCommandMapping("update", HelpCategory.COMMANDS);
        registerCommandMapping("import", HelpCategory.COMMANDS);
    }
}
