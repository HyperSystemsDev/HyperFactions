package com.hyperfactions.command;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.backup.BackupMetadata;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.data.*;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.importer.HyFactionsImporter;
import com.hyperfactions.importer.ImportResult;
import com.hyperfactions.gui.help.HelpCategory;
import com.hyperfactions.gui.help.HelpRegistry;
import com.hyperfactions.integration.HyperPermsIntegration;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.manager.*;
import com.hyperfactions.manager.ConfirmationManager.ConfirmationResult;
import com.hyperfactions.manager.ConfirmationManager.ConfirmationType;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HelpFormatter;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.TimeUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Main faction command with all subcommands.
 * /faction (aliases: /f, /hf)
 */
public class FactionCommand extends AbstractPlayerCommand {

    // Colors
    private static final String COLOR_CYAN = "#55FFFF";
    private static final String COLOR_GREEN = "#55FF55";
    private static final String COLOR_RED = "#FF5555";
    private static final String COLOR_YELLOW = "#FFFF55";
    private static final String COLOR_GRAY = "#AAAAAA";
    private static final String COLOR_WHITE = "#FFFFFF";

    private final HyperFactions hyperFactions;
    private final HyperFactionsPlugin plugin;

    public FactionCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("faction", "Faction management commands");
        this.hyperFactions = hyperFactions;
        this.plugin = plugin;

        addAliases("f", "hf");
        setAllowsExtraArguments(true);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        String input = ctx.getInputString();
        String[] parts = (input != null ? input.trim() : "").split("\\s+");

        // parts[0] is "faction", "f", or "hf"
        if (parts.length <= 1) {
            // Check permission before opening GUI
            if (!hasPermission(player, Permissions.USE)) {
                ctx.sendMessage(prefix().insert(msg("You don't have permission to use factions.", COLOR_RED)));
                return;
            }
            // Open faction main dashboard GUI
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionMain(playerEntity, ref, store, player);
            } else {
                // Fallback to text help if Player component not available (shouldn't happen)
                showHelpText(ctx, player);
            }
            return;
        }

        String subcommand = parts[1].toLowerCase();
        String[] rawSubArgs = parts.length > 2 ? Arrays.copyOfRange(parts, 2, parts.length) : new String[0];

        // Parse flags (--text, -t) from arguments
        FactionCommandContext fctx = FactionCommandContext.parse(rawSubArgs);

        switch (subcommand) {
            case "create" -> handleCreate(ctx, store, ref, player, fctx);
            case "disband" -> handleDisband(ctx, store, ref, player, fctx);
            case "invite" -> handleInvite(ctx, store, ref, player, fctx);
            case "accept", "join" -> handleAccept(ctx, store, ref, player, fctx);
            case "leave" -> handleLeave(ctx, store, ref, player, fctx);
            case "kick" -> handleKick(ctx, store, ref, player, fctx);
            case "promote" -> handlePromote(ctx, store, ref, player, fctx);
            case "demote" -> handleDemote(ctx, store, ref, player, fctx);
            case "transfer" -> handleTransfer(ctx, store, ref, player, fctx);
            case "claim" -> handleClaim(ctx, store, ref, player, currentWorld, fctx);
            case "unclaim" -> handleUnclaim(ctx, store, ref, player, currentWorld, fctx);
            case "overclaim" -> handleOverclaim(ctx, store, ref, player, currentWorld, fctx);
            case "home" -> handleHome(ctx, store, ref, player, currentWorld);
            case "sethome" -> handleSetHome(ctx, store, ref, player, currentWorld);
            case "ally" -> handleAlly(ctx, store, ref, player, fctx);
            case "enemy" -> handleEnemy(ctx, store, ref, player, fctx);
            case "neutral" -> handleNeutral(ctx, store, ref, player, fctx);
            case "relations" -> handleRelations(ctx, store, ref, player, fctx);
            case "info", "show" -> handleInfo(ctx, store, ref, player, fctx);
            case "list" -> handleList(ctx, store, ref, player, fctx);
            case "map" -> handleMap(ctx, store, ref, player, currentWorld, fctx);
            case "members" -> handleMembers(ctx, store, ref, player, fctx);
            case "invites" -> handleInvites(ctx, store, ref, player, fctx);
            case "who" -> handleWho(ctx, store, ref, player, fctx);
            case "power" -> handlePower(ctx, store, ref, player, fctx);
            case "chat", "c" -> handleChat(ctx, player, fctx.getArgs());
            case "help", "?" -> handleHelp(ctx, store, ref, player, fctx);
            case "gui", "menu" -> handleGui(ctx, store, ref, player);
            case "admin" -> handleAdmin(ctx, store, ref, player, currentWorld, fctx.getArgs());
            case "debug" -> handleDebug(ctx, store, ref, player, currentWorld, fctx.getArgs());
            case "reload" -> handleReload(ctx, player);
            case "sync" -> handleSync(ctx, player);
            case "rename" -> handleRename(ctx, store, ref, player, fctx);
            case "desc", "description" -> handleDesc(ctx, store, ref, player, fctx);
            case "color" -> handleColor(ctx, store, ref, player, fctx);
            case "open" -> handleOpen(ctx, store, ref, player, fctx);
            case "close" -> handleClose(ctx, store, ref, player, fctx);
            case "request" -> handleRequest(ctx, store, ref, player, fctx);
            case "stuck" -> handleStuck(ctx, store, ref, player, currentWorld);
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown subcommand: " + subcommand, COLOR_RED)));
                showHelpText(ctx, player);
            }
        }
    }

    private Message prefix() {
        return Message.raw("[").color(COLOR_GRAY)
            .insert(Message.raw("HyperFactions").color(COLOR_CYAN))
            .insert(Message.raw("] ").color(COLOR_GRAY));
    }

    private Message msg(String text, String color) {
        return Message.raw(text).color(color);
    }

    /**
     * Handles the /f help command.
     * Opens the Help GUI unless --text flag is provided.
     */
    private void handleHelp(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                            PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.HELP)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to view help.", COLOR_RED)));
            return;
        }

        // Text mode: show chat-based help
        if (fctx.isTextMode()) {
            showHelpText(ctx, player);
            return;
        }

        // GUI mode: open help page
        Player playerEntity = store.getComponent(ref, Player.getComponentType());
        if (playerEntity != null) {
            hyperFactions.getGuiManager().openHelpPage(playerEntity, ref, store, player);
        } else {
            showHelpText(ctx, player);
        }
    }

    /**
     * Opens the Help GUI to a specific category based on a command.
     * Used for deep-linking from /f &lt;command&gt; help.
     *
     * @param command The command name to find the relevant help category for
     */
    private void openHelpForCommand(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                                    PlayerRef player, String command) {
        Player playerEntity = store.getComponent(ref, Player.getComponentType());
        if (playerEntity == null) {
            showHelpText(ctx, player);
            return;
        }

        // Look up the category for this command
        HelpCategory category = HelpRegistry.getInstance().getCategoryForCommand(command);
        if (category == null) {
            category = HelpCategory.COMMANDS; // Default to commands reference
        }

        hyperFactions.getGuiManager().openHelp(playerEntity, ref, store, player, category);
    }

    /**
     * Shows text-based help in chat (fallback for --text mode or when GUI unavailable).
     */
    private void showHelpText(CommandContext ctx, PlayerRef player) {
        List<CommandHelp> commands = new ArrayList<>();

        // Core - Basic faction management
        commands.add(new CommandHelp("/f create <name>", "Create a faction", "Core"));
        commands.add(new CommandHelp("/f disband", "Disband your faction", "Core"));
        commands.add(new CommandHelp("/f invite <player>", "Invite a player", "Core"));
        commands.add(new CommandHelp("/f accept [faction]", "Accept an invite", "Core"));
        commands.add(new CommandHelp("/f request <faction> [msg]", "Request to join a faction", "Core"));
        commands.add(new CommandHelp("/f leave", "Leave your faction", "Core"));
        commands.add(new CommandHelp("/f kick <player>", "Kick a member", "Core"));

        // Management - Faction settings
        commands.add(new CommandHelp("/f rename <name>", "Rename your faction", "Management"));
        commands.add(new CommandHelp("/f desc <text>", "Set faction description", "Management"));
        commands.add(new CommandHelp("/f color <code>", "Set faction color", "Management"));
        commands.add(new CommandHelp("/f open", "Allow anyone to join", "Management"));
        commands.add(new CommandHelp("/f close", "Require invite to join", "Management"));
        commands.add(new CommandHelp("/f promote <player>", "Promote to officer", "Management"));
        commands.add(new CommandHelp("/f demote <player>", "Demote to member", "Management"));
        commands.add(new CommandHelp("/f transfer <player>", "Transfer leadership", "Management"));

        // Territory - Land claims
        commands.add(new CommandHelp("/f claim", "Claim this chunk", "Territory"));
        commands.add(new CommandHelp("/f unclaim", "Unclaim this chunk", "Territory"));
        commands.add(new CommandHelp("/f overclaim", "Overclaim enemy territory", "Territory"));
        commands.add(new CommandHelp("/f map", "View territory map", "Territory"));

        // Relations - Diplomatic relations
        commands.add(new CommandHelp("/f ally <faction>", "Request alliance", "Relations"));
        commands.add(new CommandHelp("/f enemy <faction>", "Declare enemy", "Relations"));
        commands.add(new CommandHelp("/f neutral <faction>", "Set neutral relation", "Relations"));

        // Teleport - Home teleportation
        commands.add(new CommandHelp("/f home", "Teleport to faction home", "Teleport"));
        commands.add(new CommandHelp("/f sethome", "Set faction home", "Teleport"));
        commands.add(new CommandHelp("/f stuck", "Escape from enemy territory", "Teleport"));

        // Information - Viewing faction data
        commands.add(new CommandHelp("/f info [faction]", "View faction info", "Information"));
        commands.add(new CommandHelp("/f list", "List all factions", "Information"));
        commands.add(new CommandHelp("/f members", "View faction members", "Information"));
        commands.add(new CommandHelp("/f invites", "Manage invites/requests", "Information"));
        commands.add(new CommandHelp("/f who <player>", "View player info", "Information"));
        commands.add(new CommandHelp("/f power [player]", "View power level", "Information"));
        commands.add(new CommandHelp("/f gui", "Open faction GUI", "Information"));

        // Other
        commands.add(new CommandHelp("/f chat [mode]", "Toggle faction chat", "Other"));
        commands.add(new CommandHelp("/f reload", "Reload config", "Other"));

        // Admin
        commands.add(new CommandHelp("/f admin zone", "Manage admin zones", "Admin"));
        commands.add(new CommandHelp("/f admin backup create [name]", "Create manual backup", "Admin"));
        commands.add(new CommandHelp("/f admin backup list", "List all backups", "Admin"));
        commands.add(new CommandHelp("/f admin backup restore <name>", "Restore from backup", "Admin"));
        commands.add(new CommandHelp("/f admin backup delete <name>", "Delete a backup", "Admin"));
        commands.add(new CommandHelp("/f admin update", "Check for updates", "Admin"));
        commands.add(new CommandHelp("/f debug", "Debug commands", "Admin"));

        ctx.sendMessage(HelpFormatter.buildHelp("HyperFactions", "Faction management and territory control", commands, "Use /f <command> for more details"));
    }

    // === Create ===
    private void handleCreate(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                              PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.CREATE)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to create factions.", COLOR_RED)));
            return;
        }

        // GUI mode: open CreateFactionStep1Page when no name provided
        if (!fctx.hasArgs() && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openCreateFactionWizard(playerEntity, ref, store, player);
                return;
            }
        }

        // Text mode or with args: create directly
        if (!fctx.hasArgs()) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f create <name>", COLOR_RED)));
            return;
        }

        String name = fctx.joinArgs();
        FactionManager.FactionResult result = hyperFactions.getFactionManager().createFaction(
            name, player.getUuid(), player.getUsername()
        );

        switch (result) {
            case SUCCESS -> {
                ctx.sendMessage(prefix().insert(msg("Faction '", COLOR_GREEN))
                    .insert(msg(name, COLOR_CYAN)).insert(msg("' created!", COLOR_GREEN)));
                // Open dashboard after creation (if not text mode)
                if (!fctx.isTextMode()) {
                    Player playerEntity = store.getComponent(ref, Player.getComponentType());
                    Faction newFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
                    if (playerEntity != null && newFaction != null) {
                        hyperFactions.getGuiManager().openFactionDashboard(playerEntity, ref, store, player, newFaction);
                    }
                }
            }
            case ALREADY_IN_FACTION -> {
                Faction existingFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
                if (existingFaction != null) {
                    ctx.sendMessage(prefix().insert(msg("You are already in ", COLOR_RED))
                        .insert(msg(existingFaction.name(), COLOR_CYAN))
                        .insert(msg(".", COLOR_RED)));
                    ctx.sendMessage(prefix().insert(msg("Use /f leave first if you want to create a new faction.", COLOR_YELLOW)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("You are already in a faction.", COLOR_RED)));
                }
            }
            case NAME_TAKEN -> ctx.sendMessage(prefix().insert(msg("That faction name is already taken.", COLOR_RED)));
            case NAME_TOO_SHORT -> ctx.sendMessage(prefix().insert(msg("Faction name is too short.", COLOR_RED)));
            case NAME_TOO_LONG -> ctx.sendMessage(prefix().insert(msg("Faction name is too long.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to create faction.", COLOR_RED)));
        }
    }

    // === Disband ===
    private void handleDisband(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.DISBAND)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to disband factions.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        // Check if leader
        FactionMember member = faction.getMember(player.getUuid());
        if (member == null || !member.isLeader()) {
            ctx.sendMessage(prefix().insert(msg("Only the faction leader can disband.", COLOR_RED)));
            return;
        }

        // GUI mode: open DisbandConfirmPage
        if (!fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openDisbandConfirm(playerEntity, ref, store, player, faction);
                return;
            }
        }

        // Text mode: require confirmation
        ConfirmationManager confirmManager = hyperFactions.getConfirmationManager();
        ConfirmationResult confirmResult = confirmManager.checkOrCreate(
            player.getUuid(), ConfirmationType.DISBAND, null
        );

        switch (confirmResult) {
            case NEEDS_CONFIRMATION, EXPIRED_RECREATED -> {
                ctx.sendMessage(prefix().insert(msg("Are you sure you want to disband your faction?", COLOR_YELLOW)));
                ctx.sendMessage(prefix().insert(msg("Type ", COLOR_YELLOW))
                    .insert(msg("/f disband --text", COLOR_WHITE))
                    .insert(msg(" again within " + confirmManager.getTimeoutSeconds() + " seconds to confirm.", COLOR_YELLOW)));
            }
            case CONFIRMED -> {
                UUID factionId = faction.id();
                FactionManager.FactionResult result = hyperFactions.getFactionManager().disbandFaction(
                    factionId, player.getUuid()
                );
                if (result == FactionManager.FactionResult.SUCCESS) {
                    hyperFactions.getClaimManager().unclaimAll(factionId);
                    hyperFactions.getInviteManager().clearFactionInvites(factionId);
                    hyperFactions.getJoinRequestManager().clearFactionRequests(factionId);
                    hyperFactions.getRelationManager().clearAllRelations(factionId);
                    ctx.sendMessage(prefix().insert(msg("Your faction has been disbanded.", COLOR_GREEN)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Failed to disband faction.", COLOR_RED)));
                }
            }
            case DIFFERENT_ACTION -> {
                ctx.sendMessage(prefix().insert(msg("Previous confirmation cancelled. Type again to confirm disband.", COLOR_YELLOW)));
            }
        }
    }

    // === Invite ===
    private void handleInvite(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                              PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.INVITE)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to invite players.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        FactionMember member = faction.getMember(player.getUuid());
        if (member == null || !member.isOfficerOrHigher()) {
            ctx.sendMessage(prefix().insert(msg("You must be an officer to invite players.", COLOR_RED)));
            return;
        }

        // GUI mode: open FactionInvitesPage when no player specified
        if (!fctx.hasArgs() && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionInvites(playerEntity, ref, store, player, faction);
                return;
            }
        }

        if (!fctx.hasArgs()) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f invite <player>", COLOR_RED)));
            return;
        }

        String targetName = fctx.getArg(0);
        PlayerRef target = findOnlinePlayer(targetName);
        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player '" + targetName + "' not found or offline.", COLOR_RED)));
            return;
        }

        if (hyperFactions.getFactionManager().isInFaction(target.getUuid())) {
            ctx.sendMessage(prefix().insert(msg("That player is already in a faction.", COLOR_RED)));
            return;
        }

        hyperFactions.getInviteManager().createInvite(faction.id(), target.getUuid(), player.getUuid());

        ctx.sendMessage(prefix().insert(msg("Invited ", COLOR_GREEN))
            .insert(msg(target.getUsername(), COLOR_YELLOW)).insert(msg(" to your faction.", COLOR_GREEN)));
        target.sendMessage(prefix().insert(msg("You have been invited to join ", COLOR_YELLOW))
            .insert(msg(faction.name(), COLOR_CYAN)).insert(msg("!", COLOR_YELLOW)));
        target.sendMessage(prefix().insert(msg("Type ", COLOR_YELLOW))
            .insert(msg("/f accept " + faction.name(), COLOR_GREEN)).insert(msg(" to join.", COLOR_YELLOW)));
    }

    // === Accept ===
    private void handleAccept(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                              PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.JOIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to join factions.", COLOR_RED)));
            return;
        }

        if (hyperFactions.getFactionManager().isInFaction(player.getUuid())) {
            Faction existingFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
            if (existingFaction != null) {
                ctx.sendMessage(prefix().insert(msg("You are already in ", COLOR_RED))
                    .insert(msg(existingFaction.name(), COLOR_CYAN))
                    .insert(msg(".", COLOR_RED)));
                ctx.sendMessage(prefix().insert(msg("Use /f leave first if you want to join another faction.", COLOR_YELLOW)));
            } else {
                ctx.sendMessage(prefix().insert(msg("You are already in a faction.", COLOR_RED)));
            }
            return;
        }

        List<PendingInvite> invites = hyperFactions.getInviteManager().getPlayerInvites(player.getUuid());

        // GUI mode: open InvitesPage when no faction specified
        if (!fctx.hasArgs() && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openInvitesPage(playerEntity, ref, store, player);
                return;
            }
        }

        if (invites.isEmpty()) {
            ctx.sendMessage(prefix().insert(msg("You have no pending invites.", COLOR_RED)));
            return;
        }

        PendingInvite invite;
        if (fctx.hasArgs()) {
            String factionName = fctx.joinArgs();
            Faction targetFaction = hyperFactions.getFactionManager().getFactionByName(factionName);
            if (targetFaction == null) {
                ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
                return;
            }
            invite = hyperFactions.getInviteManager().getInvite(targetFaction.id(), player.getUuid());
            if (invite == null) {
                ctx.sendMessage(prefix().insert(msg("You have no invite from that faction.", COLOR_RED)));
                return;
            }
        } else {
            invite = invites.get(0);
        }

        Faction faction = hyperFactions.getFactionManager().getFaction(invite.factionId());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("That faction no longer exists.", COLOR_RED)));
            hyperFactions.getInviteManager().removeInvite(invite.factionId(), player.getUuid());
            return;
        }

        FactionManager.FactionResult result = hyperFactions.getFactionManager().addMember(
            faction.id(), player.getUuid(), player.getUsername()
        );

        if (result == FactionManager.FactionResult.SUCCESS) {
            hyperFactions.getInviteManager().clearPlayerInvites(player.getUuid());
            hyperFactions.getJoinRequestManager().clearPlayerRequests(player.getUuid());
            ctx.sendMessage(prefix().insert(msg("You have joined ", COLOR_GREEN))
                .insert(msg(faction.name(), COLOR_CYAN)).insert(msg("!", COLOR_GREEN)));
            broadcastToFaction(faction.id(), prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
                .insert(msg(" has joined the faction!", COLOR_GREEN)));
        } else if (result == FactionManager.FactionResult.FACTION_FULL) {
            ctx.sendMessage(prefix().insert(msg("That faction is full.", COLOR_RED)));
        } else {
            ctx.sendMessage(prefix().insert(msg("Failed to join faction.", COLOR_RED)));
        }
    }

    // === Request ===
    private void handleRequest(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                                PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.JOIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to request faction membership.", COLOR_RED)));
            return;
        }

        // Check if player is already in a faction
        if (hyperFactions.getFactionManager().isInFaction(player.getUuid())) {
            Faction existingFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
            if (existingFaction != null) {
                ctx.sendMessage(prefix().insert(msg("You are already in ", COLOR_RED))
                    .insert(msg(existingFaction.name(), COLOR_CYAN))
                    .insert(msg(".", COLOR_RED)));
                ctx.sendMessage(prefix().insert(msg("Use /f leave first if you want to join another faction.", COLOR_YELLOW)));
            } else {
                ctx.sendMessage(prefix().insert(msg("You are already in a faction.", COLOR_RED)));
            }
            return;
        }

        String[] args = fctx.getArgs();

        // GUI mode: Open faction browser if no args and not text mode
        if (!fctx.hasArgs() && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionBrowser(playerEntity, ref, store, player);
            }
            return;
        }

        // Text mode requires faction name
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f request <faction> [message]", COLOR_RED)));
            return;
        }

        // Find the target faction
        String factionName = args[0];
        Faction faction = hyperFactions.getFactionManager().getFactionByName(factionName);
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
            return;
        }

        // Check if faction is open (if open, just join directly)
        if (faction.open()) {
            ctx.sendMessage(prefix().insert(msg("That faction is open! Use ", COLOR_YELLOW))
                .insert(msg("/f accept " + faction.name(), COLOR_GREEN))
                .insert(msg(" to join directly.", COLOR_YELLOW)));
            return;
        }

        // Check if player already has a pending request
        JoinRequestManager requestManager = hyperFactions.getJoinRequestManager();
        if (requestManager.hasRequest(faction.id(), player.getUuid())) {
            ctx.sendMessage(prefix().insert(msg("You already have a pending request to that faction.", COLOR_RED)));
            return;
        }

        // Check if player has an invite to this faction (they should accept it instead)
        InviteManager inviteManager = hyperFactions.getInviteManager();
        if (inviteManager.hasInvite(faction.id(), player.getUuid())) {
            ctx.sendMessage(prefix().insert(msg("You have been invited to that faction! Use ", COLOR_YELLOW))
                .insert(msg("/f accept " + faction.name(), COLOR_GREEN))
                .insert(msg(" to join.", COLOR_YELLOW)));
            return;
        }

        // Build the optional message (rest of args)
        String message = null;
        if (args.length > 1) {
            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            if (message.length() > 200) {
                message = message.substring(0, 200); // Truncate if too long
            }
        }

        // Create the join request
        requestManager.createRequest(faction.id(), player.getUuid(), player.getUsername(), message);

        ctx.sendMessage(prefix().insert(msg("Sent join request to ", COLOR_GREEN))
            .insert(msg(faction.name(), COLOR_CYAN)).insert(msg("!", COLOR_GREEN)));
        if (message != null) {
            ctx.sendMessage(prefix().insert(msg("Your message: \"" + message + "\"", COLOR_GRAY)));
        }
        ctx.sendMessage(prefix().insert(msg("An officer will review your request.", COLOR_YELLOW)));

        // Notify online officers
        for (UUID memberUuid : faction.members().keySet()) {
            FactionMember member = faction.getMember(memberUuid);
            if (member != null && member.isOfficerOrHigher()) {
                PlayerRef officer = plugin.getTrackedPlayer(memberUuid);
                if (officer != null) {
                    officer.sendMessage(prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
                        .insert(msg(" has requested to join your faction!", COLOR_GREEN)));
                    officer.sendMessage(prefix().insert(msg("Use ", COLOR_YELLOW))
                        .insert(msg("/f gui", COLOR_GREEN))
                        .insert(msg(" > Invites to review.", COLOR_YELLOW)));
                }
            }
        }
    }

    // === Leave ===
    private void handleLeave(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                             PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.LEAVE)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to leave factions.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        // GUI mode: open LeaveConfirmPage
        if (!fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openLeaveConfirm(playerEntity, ref, store, player, faction);
                return;
            }
        }

        // Text mode: require confirmation
        ConfirmationManager confirmManager = hyperFactions.getConfirmationManager();
        ConfirmationResult confirmResult = confirmManager.checkOrCreate(
            player.getUuid(), ConfirmationType.LEAVE, null
        );

        switch (confirmResult) {
            case NEEDS_CONFIRMATION, EXPIRED_RECREATED -> {
                ctx.sendMessage(prefix().insert(msg("Are you sure you want to leave your faction?", COLOR_YELLOW)));
                ctx.sendMessage(prefix().insert(msg("Type ", COLOR_YELLOW))
                    .insert(msg("/f leave --text", COLOR_WHITE))
                    .insert(msg(" again within " + confirmManager.getTimeoutSeconds() + " seconds to confirm.", COLOR_YELLOW)));
            }
            case CONFIRMED -> {
                UUID factionId = faction.id();
                FactionManager.FactionResult result = hyperFactions.getFactionManager().removeMember(
                    factionId, player.getUuid(), player.getUuid(), false
                );
                if (result == FactionManager.FactionResult.SUCCESS) {
                    ctx.sendMessage(prefix().insert(msg("You have left your faction.", COLOR_GREEN)));
                    broadcastToFaction(factionId, prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
                        .insert(msg(" has left the faction.", COLOR_RED)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Failed to leave faction.", COLOR_RED)));
                }
            }
            case DIFFERENT_ACTION -> {
                ctx.sendMessage(prefix().insert(msg("Previous confirmation cancelled. Type again to confirm leave.", COLOR_YELLOW)));
            }
        }
    }

    // === Kick ===
    private void handleKick(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                            PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.KICK)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to kick members.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        if (!fctx.hasArgs()) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f kick <player>", COLOR_RED)));
            return;
        }

        String targetName = fctx.getArg(0);
        FactionMember target = faction.members().values().stream()
            .filter(m -> m.username().equalsIgnoreCase(targetName))
            .findFirst().orElse(null);

        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player '" + targetName + "' is not in your faction.", COLOR_RED)));
            return;
        }

        FactionManager.FactionResult result = hyperFactions.getFactionManager().removeMember(
            faction.id(), target.uuid(), player.getUuid(), true
        );

        switch (result) {
            case SUCCESS -> {
                ctx.sendMessage(prefix().insert(msg("Kicked ", COLOR_GREEN))
                    .insert(msg(target.username(), COLOR_YELLOW)).insert(msg(" from the faction.", COLOR_GREEN)));
                broadcastToFaction(faction.id(), prefix().insert(msg(target.username(), COLOR_YELLOW))
                    .insert(msg(" was kicked from the faction.", COLOR_RED)));
                PlayerRef targetPlayer = plugin.getTrackedPlayer(target.uuid());
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(prefix().insert(msg("You have been kicked from the faction.", COLOR_RED)));
                }
                // Show members page after action (if not text mode)
                if (!fctx.isTextMode()) {
                    Player playerEntity = store.getComponent(ref, Player.getComponentType());
                    if (playerEntity != null) {
                        hyperFactions.getGuiManager().openFactionMembers(playerEntity, ref, store, player, faction);
                    }
                }
            }
            case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You don't have permission to kick that player.", COLOR_RED)));
            case CANNOT_KICK_LEADER -> ctx.sendMessage(prefix().insert(msg("You cannot kick the faction leader.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to kick player.", COLOR_RED)));
        }
    }

    // === Promote ===
    private void handlePromote(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.PROMOTE)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to promote members.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        if (!fctx.hasArgs()) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f promote <player>", COLOR_RED)));
            return;
        }

        String targetName = fctx.getArg(0);
        FactionMember target = faction.members().values().stream()
            .filter(m -> m.username().equalsIgnoreCase(targetName))
            .findFirst().orElse(null);

        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found in your faction.", COLOR_RED)));
            return;
        }

        FactionManager.FactionResult result = hyperFactions.getFactionManager().promoteMember(
            faction.id(), target.uuid(), player.getUuid()
        );

        switch (result) {
            case SUCCESS -> {
                ctx.sendMessage(prefix().insert(msg("Promoted ", COLOR_GREEN))
                    .insert(msg(target.username(), COLOR_YELLOW)).insert(msg(" to Officer!", COLOR_GREEN)));
                broadcastToFaction(faction.id(), prefix().insert(msg(target.username(), COLOR_YELLOW))
                    .insert(msg(" was promoted to Officer!", COLOR_GREEN)));
                // Show members page after action (if not text mode)
                if (!fctx.isTextMode()) {
                    Player playerEntity = store.getComponent(ref, Player.getComponentType());
                    if (playerEntity != null) {
                        hyperFactions.getGuiManager().openFactionMembers(playerEntity, ref, store, player, faction);
                    }
                }
            }
            case NOT_LEADER -> ctx.sendMessage(prefix().insert(msg("Only the leader can promote members.", COLOR_RED)));
            case CANNOT_PROMOTE_LEADER -> ctx.sendMessage(prefix().insert(msg("Cannot promote further. Use /f transfer to change leader.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to promote player.", COLOR_RED)));
        }
    }

    // === Demote ===
    private void handleDemote(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                              PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.DEMOTE)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to demote members.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        if (!fctx.hasArgs()) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f demote <player>", COLOR_RED)));
            return;
        }

        String targetName = fctx.getArg(0);
        FactionMember target = faction.members().values().stream()
            .filter(m -> m.username().equalsIgnoreCase(targetName))
            .findFirst().orElse(null);

        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found in your faction.", COLOR_RED)));
            return;
        }

        FactionManager.FactionResult result = hyperFactions.getFactionManager().demoteMember(
            faction.id(), target.uuid(), player.getUuid()
        );

        switch (result) {
            case SUCCESS -> {
                ctx.sendMessage(prefix().insert(msg("Demoted ", COLOR_GREEN))
                    .insert(msg(target.username(), COLOR_YELLOW)).insert(msg(" to Member.", COLOR_GREEN)));
                broadcastToFaction(faction.id(), prefix().insert(msg(target.username(), COLOR_YELLOW))
                    .insert(msg(" was demoted to Member.", COLOR_RED)));
                // Show members page after action (if not text mode)
                if (!fctx.isTextMode()) {
                    Player playerEntity = store.getComponent(ref, Player.getComponentType());
                    if (playerEntity != null) {
                        hyperFactions.getGuiManager().openFactionMembers(playerEntity, ref, store, player, faction);
                    }
                }
            }
            case NOT_LEADER -> ctx.sendMessage(prefix().insert(msg("Only the leader can demote members.", COLOR_RED)));
            case CANNOT_DEMOTE_MEMBER -> ctx.sendMessage(prefix().insert(msg("That player is already a Member.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to demote player.", COLOR_RED)));
        }
    }

    // === Transfer ===
    private void handleTransfer(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                                PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.TRANSFER)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to transfer leadership.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        // Check if leader
        FactionMember member = faction.getMember(player.getUuid());
        if (member == null || !member.isLeader()) {
            ctx.sendMessage(prefix().insert(msg("Only the leader can transfer leadership.", COLOR_RED)));
            return;
        }

        if (!fctx.hasArgs()) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f transfer <player>", COLOR_RED)));
            return;
        }

        String targetName = fctx.getArg(0);
        FactionMember target = faction.members().values().stream()
            .filter(m -> m.username().equalsIgnoreCase(targetName))
            .findFirst().orElse(null);

        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found in your faction.", COLOR_RED)));
            return;
        }

        // GUI mode: open TransferConfirmPage
        if (!fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openTransferConfirm(playerEntity, ref, store, player, faction,
                        target.uuid(), target.username());
                return;
            }
        }

        // Text mode: require confirmation
        ConfirmationManager confirmManager = hyperFactions.getConfirmationManager();
        ConfirmationResult confirmResult = confirmManager.checkOrCreate(
            player.getUuid(), ConfirmationType.TRANSFER, target.uuid()
        );

        switch (confirmResult) {
            case NEEDS_CONFIRMATION, EXPIRED_RECREATED -> {
                ctx.sendMessage(prefix().insert(msg("Are you sure you want to transfer leadership to ", COLOR_YELLOW))
                    .insert(msg(target.username(), COLOR_WHITE)).insert(msg("?", COLOR_YELLOW)));
                ctx.sendMessage(prefix().insert(msg("Type ", COLOR_YELLOW))
                    .insert(msg("/f transfer " + target.username() + " --text", COLOR_WHITE))
                    .insert(msg(" again within " + confirmManager.getTimeoutSeconds() + " seconds to confirm.", COLOR_YELLOW)));
            }
            case CONFIRMED -> {
                FactionManager.FactionResult result = hyperFactions.getFactionManager().transferLeadership(
                    faction.id(), target.uuid(), player.getUuid()
                );
                if (result == FactionManager.FactionResult.SUCCESS) {
                    ctx.sendMessage(prefix().insert(msg("Transferred leadership to ", COLOR_GREEN))
                        .insert(msg(target.username(), COLOR_YELLOW)).insert(msg("!", COLOR_GREEN)));
                    broadcastToFaction(faction.id(), prefix().insert(msg(target.username(), COLOR_YELLOW))
                        .insert(msg(" is now the faction leader!", COLOR_GREEN)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Failed to transfer leadership.", COLOR_RED)));
                }
            }
            case DIFFERENT_ACTION -> {
                ctx.sendMessage(prefix().insert(msg("Previous confirmation cancelled. Type again to confirm transfer.", COLOR_YELLOW)));
            }
        }
    }

    // === Claim ===
    private void handleClaim(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                             PlayerRef player, World world, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.CLAIM)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to claim territory.", COLOR_RED)));
            return;
        }

        // Upfront faction check - consistent error handling
        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

        // Context-aware behavior - check current chunk status
        UUID playerFactionId = faction.id();
        UUID chunkOwner = hyperFactions.getClaimManager().getClaimOwner(world.getName(), chunkX, chunkZ);

        // If own territory and not text mode, just show map
        if (playerFactionId != null && playerFactionId.equals(chunkOwner) && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                ctx.sendMessage(prefix().insert(msg("Your faction already owns this chunk.", COLOR_GRAY)));
                hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
                return;
            }
        }

        // If enemy territory and not text mode, suggest overclaim via map
        if (chunkOwner != null && !chunkOwner.equals(playerFactionId) && !fctx.isTextMode()) {
            boolean isAlly = playerFactionId != null && hyperFactions.getRelationManager().areAllies(playerFactionId, chunkOwner);
            if (isAlly) {
                ctx.sendMessage(prefix().insert(msg("You cannot claim ally territory.", COLOR_RED)));
            } else {
                ctx.sendMessage(prefix().insert(msg("This chunk is claimed. Use ", COLOR_RED))
                    .insert(msg("/f overclaim", COLOR_WHITE))
                    .insert(msg(" if they are raidable.", COLOR_RED)));
            }
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
            }
            return;
        }

        // Execute claim
        ClaimManager.ClaimResult result = hyperFactions.getClaimManager().claim(
            player.getUuid(), world.getName(), chunkX, chunkZ
        );

        switch (result) {
            case SUCCESS -> {
                ctx.sendMessage(prefix().insert(msg("Claimed chunk at " + chunkX + ", " + chunkZ + "!", COLOR_GREEN)));
                // Show map after claiming (if not text mode)
                if (!fctx.isTextMode()) {
                    Player playerEntity = store.getComponent(ref, Player.getComponentType());
                    if (playerEntity != null) {
                        hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
                    }
                }
            }
            case NOT_IN_FACTION -> ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You must be an officer to claim land.", COLOR_RED)));
            case ALREADY_CLAIMED_SELF -> ctx.sendMessage(prefix().insert(msg("Your faction already owns this chunk.", COLOR_RED)));
            case ALREADY_CLAIMED_OTHER -> ctx.sendMessage(prefix().insert(msg("This chunk is already claimed.", COLOR_RED)));
            case MAX_CLAIMS_REACHED -> ctx.sendMessage(prefix().insert(msg("Your faction has reached max claims. Get more power!", COLOR_RED)));
            case NOT_ADJACENT -> ctx.sendMessage(prefix().insert(msg("You must claim adjacent to existing territory.", COLOR_RED)));
            case WORLD_NOT_ALLOWED -> ctx.sendMessage(prefix().insert(msg("Claiming is not allowed in this world.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to claim chunk.", COLOR_RED)));
        }
    }

    // === Unclaim ===
    private void handleUnclaim(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef player, World world, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.UNCLAIM)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to unclaim territory.", COLOR_RED)));
            return;
        }

        // Upfront faction check - consistent error handling
        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

        ClaimManager.ClaimResult result = hyperFactions.getClaimManager().unclaim(
            player.getUuid(), world.getName(), chunkX, chunkZ
        );

        switch (result) {
            case SUCCESS -> {
                ctx.sendMessage(prefix().insert(msg("Unclaimed chunk at " + chunkX + ", " + chunkZ + ".", COLOR_GREEN)));
                // Show map after unclaiming (if not text mode)
                if (!fctx.isTextMode()) {
                    Player playerEntity = store.getComponent(ref, Player.getComponentType());
                    if (playerEntity != null) {
                        hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
                    }
                }
            }
            case NOT_IN_FACTION -> ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You must be an officer to unclaim land.", COLOR_RED)));
            case CHUNK_NOT_CLAIMED -> ctx.sendMessage(prefix().insert(msg("This chunk is not claimed.", COLOR_RED)));
            case NOT_YOUR_CLAIM -> ctx.sendMessage(prefix().insert(msg("Your faction doesn't own this chunk.", COLOR_RED)));
            case CANNOT_UNCLAIM_HOME -> ctx.sendMessage(prefix().insert(msg("Cannot unclaim the chunk with faction home.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to unclaim chunk.", COLOR_RED)));
        }
    }

    // === Overclaim ===
    private void handleOverclaim(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                                 PlayerRef player, World world, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.OVERCLAIM)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to overclaim territory.", COLOR_RED)));
            return;
        }

        // Upfront faction check - consistent error handling
        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

        ClaimManager.ClaimResult result = hyperFactions.getClaimManager().overclaim(
            player.getUuid(), world.getName(), chunkX, chunkZ
        );

        switch (result) {
            case SUCCESS -> {
                ctx.sendMessage(prefix().insert(msg("Overclaimed enemy territory!", COLOR_GREEN)));
                // Show map after overclaiming (if not text mode)
                if (!fctx.isTextMode()) {
                    Player playerEntity = store.getComponent(ref, Player.getComponentType());
                    if (playerEntity != null) {
                        hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
                    }
                }
            }
            case NOT_IN_FACTION -> ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You must be an officer to overclaim.", COLOR_RED)));
            case CHUNK_NOT_CLAIMED -> ctx.sendMessage(prefix().insert(msg("This chunk is not claimed. Use /f claim.", COLOR_RED)));
            case ALREADY_CLAIMED_SELF -> ctx.sendMessage(prefix().insert(msg("Your faction already owns this chunk.", COLOR_RED)));
            case ALREADY_CLAIMED_ALLY -> ctx.sendMessage(prefix().insert(msg("You cannot overclaim ally territory.", COLOR_RED)));
            case TARGET_HAS_POWER -> ctx.sendMessage(prefix().insert(msg("This faction still has enough power.", COLOR_RED)));
            case MAX_CLAIMS_REACHED -> ctx.sendMessage(prefix().insert(msg("Your faction has reached max claims.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to overclaim.", COLOR_RED)));
        }
    }

    // === Home ===
    private void handleHome(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
        if (!hasPermission(player, Permissions.HOME)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to teleport to faction home.", COLOR_RED)));
            return;
        }

        // Upfront faction check - consistent error handling
        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        UUID playerUuid = player.getUuid();

        // Create start location for movement checking
        TeleportManager.StartLocation startLoc = new TeleportManager.StartLocation(
            world.getName(), pos.getX(), pos.getY(), pos.getZ()
        );

        // Call TeleportManager with all required callbacks
        TeleportManager.TeleportResult result = hyperFactions.getTeleportManager().teleportToHome(
            playerUuid,
            startLoc,
            // Task scheduler
            (delayTicks, task) -> hyperFactions.scheduleDelayedTask(delayTicks, task),
            // Task canceller
            hyperFactions::cancelTask,
            // Teleport executor
            targetFaction -> executeTeleport(store, ref, world, targetFaction),
            // Message sender - TeleportManager formats its own messages
            message -> ctx.sendMessage(Message.raw(message)),
            // Combat tag checker
            () -> hyperFactions.getCombatTagManager().isTagged(playerUuid)
        );

        // Handle immediate results (warmup will handle async results)
        switch (result) {
            case NOT_IN_FACTION -> ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            case NO_HOME -> ctx.sendMessage(prefix().insert(msg("Your faction has no home set.", COLOR_RED)));
            case COMBAT_TAGGED -> ctx.sendMessage(prefix().insert(msg("You cannot teleport while in combat!", COLOR_RED)));
            case ON_COOLDOWN -> {} // Message sent by TeleportManager
            case SUCCESS_INSTANT -> ctx.sendMessage(prefix().insert(msg("Teleported to faction home!", COLOR_GREEN)));
            case SUCCESS_WARMUP -> {} // Message sent by TeleportManager
            default -> {}
        }
    }

    /**
     * Executes the actual teleport to faction home using the proper Teleport component.
     */
    private TeleportManager.TeleportResult executeTeleport(Store<EntityStore> store, Ref<EntityStore> ref,
                                                           World currentWorld, Faction faction) {
        Faction.FactionHome home = faction.home();
        if (home == null) {
            return TeleportManager.TeleportResult.NO_HOME;
        }

        // Get target world (supports cross-world teleportation)
        World targetWorld;
        if (currentWorld.getName().equals(home.world())) {
            targetWorld = currentWorld;
        } else {
            targetWorld = Universe.get().getWorld(home.world());
            if (targetWorld == null) {
                return TeleportManager.TeleportResult.WORLD_NOT_FOUND;
            }
        }

        // Create and apply teleport using the proper Teleport component
        Vector3d position = new Vector3d(home.x(), home.y(), home.z());
        Vector3f rotation = new Vector3f(home.pitch(), home.yaw(), 0);
        Teleport teleport = new Teleport(targetWorld, position, rotation);
        store.addComponent(ref, Teleport.getComponentType(), teleport);

        return TeleportManager.TeleportResult.SUCCESS_INSTANT;
    }

    // === SetHome ===
    private void handleSetHome(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
        if (!hasPermission(player, Permissions.SETHOME)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to set faction home.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        Vector3f rot = transform.getRotation();
        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());
        UUID claimOwner = hyperFactions.getClaimManager().getClaimOwner(world.getName(), chunkX, chunkZ);

        if (claimOwner == null || !claimOwner.equals(faction.id())) {
            ctx.sendMessage(prefix().insert(msg("You can only set home in your faction's territory.", COLOR_RED)));
            return;
        }

        // Capture player's look direction (yaw and pitch)
        Faction.FactionHome home = Faction.FactionHome.create(
            world.getName(), pos.getX(), pos.getY(), pos.getZ(), rot.getYaw(), rot.getPitch(), player.getUuid()
        );

        FactionManager.FactionResult result = hyperFactions.getFactionManager().setHome(faction.id(), home, player.getUuid());

        if (result == FactionManager.FactionResult.SUCCESS) {
            ctx.sendMessage(prefix().insert(msg("Faction home set!", COLOR_GREEN)));
            broadcastToFaction(faction.id(), prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
                .insert(msg(" set the faction home.", COLOR_GREEN)));
        } else if (result == FactionManager.FactionResult.NOT_OFFICER) {
            ctx.sendMessage(prefix().insert(msg("You must be an officer to set the home.", COLOR_RED)));
        } else {
            ctx.sendMessage(prefix().insert(msg("Failed to set home.", COLOR_RED)));
        }
    }

    // === Ally ===
    private void handleAlly(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                            PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.ALLY)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to manage alliances.", COLOR_RED)));
            return;
        }

        Faction myFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (myFaction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        // GUI mode: open SetRelationModal when no faction specified
        if (!fctx.hasArgs() && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openSetRelationModal(playerEntity, ref, store, player, myFaction);
                return;
            }
        }

        if (!fctx.hasArgs()) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f ally <faction>", COLOR_RED)));
            return;
        }

        String factionName = fctx.joinArgs();
        Faction targetFaction = hyperFactions.getFactionManager().getFactionByName(factionName);
        if (targetFaction == null) {
            ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
            return;
        }

        RelationManager.RelationResult result = hyperFactions.getRelationManager().requestAlly(player.getUuid(), targetFaction.id());

        switch (result) {
            case REQUEST_SENT -> {
                ctx.sendMessage(prefix().insert(msg("Ally request sent to ", COLOR_GREEN))
                    .insert(msg(targetFaction.name(), COLOR_CYAN)).insert(msg("!", COLOR_GREEN)));
            }
            case REQUEST_ACCEPTED -> {
                ctx.sendMessage(prefix().insert(msg("You are now allies with ", COLOR_GREEN))
                    .insert(msg(targetFaction.name(), COLOR_CYAN)).insert(msg("!", COLOR_GREEN)));
            }
            case NOT_IN_FACTION -> ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You must be an officer to manage relations.", COLOR_RED)));
            case CANNOT_RELATE_SELF -> ctx.sendMessage(prefix().insert(msg("You cannot ally with yourself.", COLOR_RED)));
            case ALREADY_ALLY -> ctx.sendMessage(prefix().insert(msg("You are already allied with that faction.", COLOR_RED)));
            case ALLY_LIMIT_REACHED -> ctx.sendMessage(prefix().insert(msg("You have reached the maximum number of allies.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to send ally request.", COLOR_RED)));
        }
    }

    // === Enemy ===
    private void handleEnemy(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                             PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.ENEMY)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to declare enemies.", COLOR_RED)));
            return;
        }

        Faction myFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (myFaction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        // GUI mode: open SetRelationModal when no faction specified
        if (!fctx.hasArgs() && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openSetRelationModal(playerEntity, ref, store, player, myFaction);
                return;
            }
        }

        if (!fctx.hasArgs()) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f enemy <faction>", COLOR_RED)));
            return;
        }

        String factionName = fctx.joinArgs();
        Faction targetFaction = hyperFactions.getFactionManager().getFactionByName(factionName);
        if (targetFaction == null) {
            ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
            return;
        }

        RelationManager.RelationResult result = hyperFactions.getRelationManager().setEnemy(player.getUuid(), targetFaction.id());

        switch (result) {
            case SUCCESS -> ctx.sendMessage(prefix().insert(msg(targetFaction.name(), COLOR_RED))
                .insert(msg(" is now your enemy!", COLOR_RED)));
            case NOT_IN_FACTION -> ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You must be an officer to manage relations.", COLOR_RED)));
            case ALREADY_ENEMY -> ctx.sendMessage(prefix().insert(msg("You are already enemies with that faction.", COLOR_RED)));
            case ENEMY_LIMIT_REACHED -> ctx.sendMessage(prefix().insert(msg("You have reached the maximum number of enemies.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to set enemy.", COLOR_RED)));
        }
    }

    // === Neutral ===
    private void handleNeutral(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.NEUTRAL)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to set neutral relations.", COLOR_RED)));
            return;
        }

        Faction myFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (myFaction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        // GUI mode: open SetRelationModal when no faction specified
        if (!fctx.hasArgs() && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openSetRelationModal(playerEntity, ref, store, player, myFaction);
                return;
            }
        }

        if (!fctx.hasArgs()) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f neutral <faction>", COLOR_RED)));
            return;
        }

        String factionName = fctx.joinArgs();
        Faction targetFaction = hyperFactions.getFactionManager().getFactionByName(factionName);
        if (targetFaction == null) {
            ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
            return;
        }

        RelationManager.RelationResult result = hyperFactions.getRelationManager().setNeutral(player.getUuid(), targetFaction.id());

        switch (result) {
            case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Your faction is now neutral with " + targetFaction.name() + ".", COLOR_GRAY)));
            case NOT_IN_FACTION -> ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You must be an officer to manage relations.", COLOR_RED)));
            case ALREADY_NEUTRAL -> ctx.sendMessage(prefix().insert(msg("You are already neutral with that faction.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to set neutral.", COLOR_RED)));
        }
    }

    // === Relations ===
    private void handleRelations(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                                 PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.RELATIONS)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to view relations.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        // GUI mode: open FactionRelationsPage
        if (fctx.shouldOpenGui()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionRelations(playerEntity, ref, store, player, faction);
                return;
            }
        }

        // Text mode: list relations
        List<UUID> allies = hyperFactions.getRelationManager().getAllies(faction.id());
        List<UUID> enemies = hyperFactions.getRelationManager().getEnemies(faction.id());

        ctx.sendMessage(msg("=== Faction Relations ===", COLOR_CYAN).bold(true));

        ctx.sendMessage(msg("Allies (" + allies.size() + "):", COLOR_GREEN));
        if (allies.isEmpty()) {
            ctx.sendMessage(msg("  (none)", COLOR_GRAY));
        } else {
            for (UUID allyId : allies) {
                Faction ally = hyperFactions.getFactionManager().getFaction(allyId);
                if (ally != null) {
                    ctx.sendMessage(msg("  - ", COLOR_GRAY).insert(msg(ally.name(), COLOR_GREEN)));
                }
            }
        }

        ctx.sendMessage(msg("Enemies (" + enemies.size() + "):", COLOR_RED));
        if (enemies.isEmpty()) {
            ctx.sendMessage(msg("  (none)", COLOR_GRAY));
        } else {
            for (UUID enemyId : enemies) {
                Faction enemy = hyperFactions.getFactionManager().getFaction(enemyId);
                if (enemy != null) {
                    ctx.sendMessage(msg("  - ", COLOR_GRAY).insert(msg(enemy.name(), COLOR_RED)));
                }
            }
        }
    }

    // === Info ===
    private void handleInfo(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                            PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.INFO)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to view faction info.", COLOR_RED)));
            return;
        }

        Faction faction;
        if (fctx.hasArgs()) {
            String factionName = fctx.joinArgs();
            faction = hyperFactions.getFactionManager().getFactionByName(factionName);
            if (faction == null) {
                ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
                return;
            }
        } else {
            faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
            if (faction == null) {
                ctx.sendMessage(prefix().insert(msg("You are not in a faction. Use /f info <faction>", COLOR_RED)));
                return;
            }
        }

        // GUI mode: open FactionInfoPage (default when no args and no --text flag)
        if (!fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionInfo(playerEntity, ref, store, player, faction);
                return;
            }
        }

        // Text mode: output to chat
        PowerManager.FactionPowerStats stats = hyperFactions.getPowerManager().getFactionPowerStats(faction.id());
        FactionMember leader = faction.getLeader();

        ctx.sendMessage(msg("=== " + faction.name() + " ===", COLOR_CYAN).bold(true));
        ctx.sendMessage(msg("Leader: ", COLOR_GRAY).insert(msg(leader != null ? leader.username() : "None", COLOR_YELLOW)));
        ctx.sendMessage(msg("Members: ", COLOR_GRAY).insert(msg(faction.getMemberCount() + "/" + HyperFactionsConfig.get().getMaxMembers(), COLOR_WHITE)));
        ctx.sendMessage(msg("Power: ", COLOR_GRAY).insert(msg(String.format("%.1f/%.1f", stats.currentPower(), stats.maxPower()), COLOR_WHITE)));
        ctx.sendMessage(msg("Claims: ", COLOR_GRAY).insert(msg(stats.currentClaims() + "/" + stats.maxClaims(), COLOR_WHITE)));
        if (stats.isRaidable()) {
            ctx.sendMessage(msg("RAIDABLE!", COLOR_RED).bold(true));
        }
    }

    // === List ===
    private void handleList(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                            PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.LIST)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to view faction list.", COLOR_RED)));
            return;
        }

        // GUI mode: open FactionBrowserPage
        if (fctx.shouldOpenGui()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionBrowser(playerEntity, ref, store, player);
                return;
            }
        }

        // Text mode: output to chat
        Collection<Faction> factions = hyperFactions.getFactionManager().getAllFactions();
        if (factions.isEmpty()) {
            ctx.sendMessage(prefix().insert(msg("There are no factions.", COLOR_GRAY)));
            return;
        }

        ctx.sendMessage(msg("=== Factions (" + factions.size() + ") ===", COLOR_CYAN).bold(true));
        for (Faction faction : factions) {
            PowerManager.FactionPowerStats stats = hyperFactions.getPowerManager().getFactionPowerStats(faction.id());
            String raidable = stats.isRaidable() ? " [RAIDABLE]" : "";
            ctx.sendMessage(msg(faction.name(), COLOR_YELLOW)
                .insert(msg(" - " + faction.getMemberCount() + " members, " + String.format("%.0f", stats.currentPower()) + " power" + raidable, COLOR_GRAY)));
        }
    }

    // === Map ===
    private void handleMap(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                           PlayerRef player, World world, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.MAP)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to view the map.", COLOR_RED)));
            return;
        }

        // GUI mode: open ChunkMapPage
        if (fctx.shouldOpenGui()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
                return;
            }
        }

        // Text mode: ASCII map
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int centerChunkX = ChunkUtil.toChunkCoord(pos.getX());
        int centerChunkZ = ChunkUtil.toChunkCoord(pos.getZ());

        UUID playerFactionId = hyperFactions.getFactionManager().getPlayerFactionId(player.getUuid());

        ctx.sendMessage(msg("=== Territory Map ===", COLOR_CYAN).bold(true));

        for (int dz = -3; dz <= 3; dz++) {
            StringBuilder row = new StringBuilder();
            for (int dx = -3; dx <= 3; dx++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                boolean isCenter = (dx == 0 && dz == 0);

                UUID owner = hyperFactions.getClaimManager().getClaimOwner(world.getName(), chunkX, chunkZ);
                boolean isOwned = playerFactionId != null && playerFactionId.equals(owner);
                boolean isAlly = playerFactionId != null && owner != null &&
                    hyperFactions.getRelationManager().areAllies(playerFactionId, owner);
                boolean isEnemy = playerFactionId != null && owner != null &&
                    hyperFactions.getRelationManager().areEnemies(playerFactionId, owner);
                boolean isSafeZone = hyperFactions.getZoneManager().isInSafeZone(world.getName(), chunkX, chunkZ);
                boolean isWarZone = hyperFactions.getZoneManager().isInWarZone(world.getName(), chunkX, chunkZ);

                row.append(ChunkUtil.getMapChar(isOwned, isAlly, isEnemy, owner != null, isCenter, isSafeZone, isWarZone));
            }
            ctx.sendMessage(Message.raw(row.toString()));
        }
        ctx.sendMessage(msg("Legend: +You /Own /Ally /Enemy -Wild", COLOR_GRAY));
        ctx.sendMessage(msg("Use /f gui for interactive map", COLOR_GRAY));
    }

    // === GUI ===
    private void handleGui(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
        if (!hasPermission(playerRef, Permissions.USE)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            ctx.sendMessage(prefix().insert(msg("Could not find player entity.", COLOR_RED)));
            return;
        }

        hyperFactions.getGuiManager().openFactionMain(player, ref, store, playerRef);
    }

    // === Members ===
    private void handleMembers(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.MEMBERS)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to view faction members.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        // GUI mode: open FactionMembersPage
        if (fctx.shouldOpenGui()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionMembers(playerEntity, ref, store, player, faction);
                return;
            }
        }

        // Text mode: output member list to chat
        List<FactionMember> members = faction.getMembersSorted();
        ctx.sendMessage(msg("=== " + faction.name() + " Members (" + members.size() + ") ===", COLOR_CYAN).bold(true));

        for (FactionMember member : members) {
            String roleColor = switch (member.role()) {
                case LEADER -> COLOR_YELLOW;
                case OFFICER -> COLOR_GREEN;
                default -> COLOR_GRAY;
            };
            boolean isOnline = plugin.getTrackedPlayer(member.uuid()) != null;
            String status = isOnline ? " [Online]" : "";
            ctx.sendMessage(msg(member.role().getDisplayName() + " ", roleColor)
                .insert(msg(member.username(), COLOR_WHITE))
                .insert(msg(status, isOnline ? COLOR_GREEN : COLOR_GRAY)));
        }
    }

    // === Invites ===
    private void handleInvites(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef player, FactionCommandContext fctx) {
        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());

        // Player has a faction - show FactionInvitesPage (outgoing invites, incoming requests)
        if (faction != null) {
            FactionMember member = faction.getMember(player.getUuid());
            if (member == null || !member.isOfficerOrHigher()) {
                ctx.sendMessage(prefix().insert(msg("You must be an officer to manage invites.", COLOR_RED)));
                return;
            }

            // GUI mode: open FactionInvitesPage
            if (fctx.shouldOpenGui()) {
                Player playerEntity = store.getComponent(ref, Player.getComponentType());
                if (playerEntity != null) {
                    hyperFactions.getGuiManager().openFactionInvites(playerEntity, ref, store, player, faction);
                    return;
                }
            }

            // Text mode: show outgoing invites and incoming requests
            List<PendingInvite> invites = hyperFactions.getInviteManager().getFactionInvitesList(faction.id());
            List<JoinRequest> requests = hyperFactions.getJoinRequestManager().getFactionRequests(faction.id());

            ctx.sendMessage(msg("=== Faction Invites ===", COLOR_CYAN).bold(true));

            if (invites.isEmpty() && requests.isEmpty()) {
                ctx.sendMessage(msg("No pending invites or requests.", COLOR_GRAY));
                return;
            }

            if (!invites.isEmpty()) {
                ctx.sendMessage(msg("Outgoing Invites:", COLOR_YELLOW));
                for (PendingInvite invite : invites) {
                    String inviterName = plugin.getTrackedPlayer(invite.invitedBy()) != null
                        ? plugin.getTrackedPlayer(invite.invitedBy()).getUsername()
                        : "Unknown";
                    ctx.sendMessage(msg("  - ", COLOR_GRAY)
                        .insert(msg(invite.playerUuid().toString().substring(0, 8), COLOR_WHITE))
                        .insert(msg(" (invited by " + inviterName + ")", COLOR_GRAY)));
                }
            }

            if (!requests.isEmpty()) {
                ctx.sendMessage(msg("Join Requests:", COLOR_GREEN));
                for (JoinRequest request : requests) {
                    String message = request.message() != null ? " \"" + request.message() + "\"" : "";
                    ctx.sendMessage(msg("  - ", COLOR_GRAY)
                        .insert(msg(request.playerName(), COLOR_WHITE))
                        .insert(msg(message, COLOR_GRAY)));
                }
            }
        } else {
            // Player has no faction - show InvitesPage (incoming invites)
            // GUI mode: open InvitesPage
            if (fctx.shouldOpenGui()) {
                Player playerEntity = store.getComponent(ref, Player.getComponentType());
                if (playerEntity != null) {
                    hyperFactions.getGuiManager().openInvitesPage(playerEntity, ref, store, player);
                    return;
                }
            }

            // Text mode: show incoming invites
            List<PendingInvite> invites = hyperFactions.getInviteManager().getPlayerInvites(player.getUuid());

            ctx.sendMessage(msg("=== Your Invites ===", COLOR_CYAN).bold(true));

            if (invites.isEmpty()) {
                ctx.sendMessage(msg("You have no pending invites.", COLOR_GRAY));
                return;
            }

            for (PendingInvite invite : invites) {
                Faction invitingFaction = hyperFactions.getFactionManager().getFaction(invite.factionId());
                if (invitingFaction != null) {
                    ctx.sendMessage(msg("  - ", COLOR_GRAY)
                        .insert(msg(invitingFaction.name(), COLOR_YELLOW))
                        .insert(msg(" - Use /f accept " + invitingFaction.name(), COLOR_GRAY)));
                }
            }
        }
    }

    // === Who ===
    private void handleWho(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                           PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.WHO)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to view player info.", COLOR_RED)));
            return;
        }

        String targetName;
        UUID targetUuid = null;

        if (!fctx.hasArgs()) {
            // Show own info
            targetName = player.getUsername();
            targetUuid = player.getUuid();
        } else {
            // Look up target player
            targetName = fctx.getArg(0);

            // First check online players
            for (PlayerRef online : plugin.getTrackedPlayers().values()) {
                if (online.getUsername().equalsIgnoreCase(targetName)) {
                    targetUuid = online.getUuid();
                    targetName = online.getUsername(); // Use correct case
                    break;
                }
            }

            // If not online, search faction members
            if (targetUuid == null) {
                for (Faction faction : hyperFactions.getFactionManager().getAllFactions()) {
                    for (FactionMember member : faction.getMembersSorted()) {
                        if (member.username().equalsIgnoreCase(targetName)) {
                            targetUuid = member.uuid();
                            targetName = member.username(); // Use correct case
                            break;
                        }
                    }
                    if (targetUuid != null) break;
                }
            }
        }

        if (targetUuid == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found.", COLOR_RED)));
            return;
        }

        // GUI mode: open PlayerInfoPage
        if (!fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openPlayerInfo(playerEntity, ref, store, player, targetUuid, targetName);
                return;
            }
        }

        // Text mode: output to chat
        // Get faction info
        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(targetUuid);
        FactionMember member = faction != null ? faction.getMember(targetUuid) : null;

        // Get power info
        PlayerPower power = hyperFactions.getPowerManager().getPlayerPower(targetUuid);

        // Check if online
        boolean isOnline = plugin.getTrackedPlayer(targetUuid) != null;

        // Display info
        ctx.sendMessage(msg("=== " + targetName + " ===", COLOR_CYAN));

        if (faction != null && member != null) {
            ctx.sendMessage(msg("Faction: ", COLOR_GRAY).insert(msg(faction.name(), COLOR_WHITE)));
            ctx.sendMessage(msg("Role: ", COLOR_GRAY).insert(msg(member.role().getDisplayName(), COLOR_WHITE)));
            ctx.sendMessage(msg("Joined: ", COLOR_GRAY).insert(msg(TimeUtil.formatRelative(member.joinedAt()), COLOR_WHITE)));
        } else {
            ctx.sendMessage(msg("Faction: ", COLOR_GRAY).insert(msg("None", COLOR_WHITE)));
        }

        ctx.sendMessage(msg("Power: ", COLOR_GRAY).insert(msg(String.format("%.1f/%.1f", power.power(), power.maxPower()), COLOR_WHITE)));
        ctx.sendMessage(msg("Status: ", COLOR_GRAY).insert(msg(isOnline ? "Online" : "Offline", isOnline ? COLOR_GREEN : COLOR_RED)));

        if (!isOnline && member != null) {
            ctx.sendMessage(msg("Last seen: ", COLOR_GRAY).insert(msg(TimeUtil.formatRelative(member.lastOnline()), COLOR_WHITE)));
        }
    }

    // === Power ===
    private void handlePower(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                             PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.POWER)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to view power info.", COLOR_RED)));
            return;
        }

        UUID targetUuid;
        String targetName;

        if (!fctx.hasArgs()) {
            // Show own power
            targetUuid = player.getUuid();
            targetName = player.getUsername();
        } else {
            // Look up target player
            targetName = fctx.getArg(0);
            targetUuid = null;

            // First check online players
            for (PlayerRef online : plugin.getTrackedPlayers().values()) {
                if (online.getUsername().equalsIgnoreCase(targetName)) {
                    targetUuid = online.getUuid();
                    targetName = online.getUsername();
                    break;
                }
            }

            // If not online, search faction members
            if (targetUuid == null) {
                for (Faction faction : hyperFactions.getFactionManager().getAllFactions()) {
                    for (FactionMember member : faction.getMembersSorted()) {
                        if (member.username().equalsIgnoreCase(targetName)) {
                            targetUuid = member.uuid();
                            targetName = member.username();
                            break;
                        }
                    }
                    if (targetUuid != null) break;
                }
            }

            if (targetUuid == null) {
                ctx.sendMessage(prefix().insert(msg("Player not found.", COLOR_RED)));
                return;
            }
        }

        // GUI mode: open PlayerInfoPage (same as /f who, but focused on power)
        if (!fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openPlayerInfo(playerEntity, ref, store, player, targetUuid, targetName);
                return;
            }
        }

        // Text mode: output power to chat
        PlayerPower power = hyperFactions.getPowerManager().getPlayerPower(targetUuid);
        ctx.sendMessage(msg(targetName + "'s Power:", COLOR_CYAN));
        ctx.sendMessage(msg("Current: ", COLOR_GRAY).insert(msg(String.format("%.1f/%.1f (%d%%)",
            power.power(), power.maxPower(), power.getPowerPercent()), COLOR_WHITE)));
    }

    // === Chat ===
    private void handleChat(CommandContext ctx, PlayerRef player, String[] args) {
        if (!hasPermission(player, Permissions.CHAT_FACTION)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to use faction chat.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f c <message>", COLOR_RED)));
            return;
        }

        String message = String.join(" ", args);
        Message formatted = msg("[Faction] ", COLOR_CYAN)
            .insert(msg(player.getUsername(), COLOR_YELLOW))
            .insert(msg(": " + message, COLOR_WHITE));
        broadcastToFaction(faction.id(), formatted);
    }

    // === Admin ===
    private void handleAdmin(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world, String[] args) {
        if (!hasPermission(player, Permissions.ADMIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        // No args - open admin GUI
        if (args.length == 0) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity == null) {
                ctx.sendMessage(prefix().insert(msg("Could not find player entity.", COLOR_RED)));
                return;
            }
            hyperFactions.getGuiManager().openAdminMain(playerEntity, ref, store, player);
            return;
        }

        String adminCmd = args[0].toLowerCase();

        // Show help for admin commands
        if (adminCmd.equals("help") || adminCmd.equals("?")) {
            List<CommandHelp> adminCommands = new ArrayList<>();
            adminCommands.add(new CommandHelp("/f admin", "Open admin GUI"));
            adminCommands.add(new CommandHelp("/f admin zone", "Open zone management GUI"));
            adminCommands.add(new CommandHelp("/f admin zone list", "List all zones"));
            adminCommands.add(new CommandHelp("/f admin zone create <type> <name>", "Create a new zone"));
            adminCommands.add(new CommandHelp("/f admin zone delete <name>", "Delete a zone"));
            adminCommands.add(new CommandHelp("/f admin zone claim <name>", "Claim current chunk for zone"));
            adminCommands.add(new CommandHelp("/f admin zone unclaim", "Unclaim current chunk from zone"));
            adminCommands.add(new CommandHelp("/f admin zone radius <name> <r> [shape]", "Radius claim for zone"));
            adminCommands.add(new CommandHelp("/f admin safezone [name]", "Quick create SafeZone + claim"));
            adminCommands.add(new CommandHelp("/f admin warzone [name]", "Quick create WarZone + claim"));
            adminCommands.add(new CommandHelp("/f admin removezone", "Unclaim current chunk from zone"));
            adminCommands.add(new CommandHelp("/f admin zoneflag [flag] [value]", "View/set zone flags (clearall to reset)"));
            adminCommands.add(new CommandHelp("/f admin bypass", "Toggle admin bypass mode"));
            adminCommands.add(new CommandHelp("/f admin update", "Download and install plugin update"));
            adminCommands.add(new CommandHelp("/f admin backup", "Backup management"));
            adminCommands.add(new CommandHelp("/f admin backup create [name]", "Create manual backup"));
            adminCommands.add(new CommandHelp("/f admin backup list", "List all backups"));
            adminCommands.add(new CommandHelp("/f admin backup restore <name>", "Restore from backup"));
            adminCommands.add(new CommandHelp("/f admin backup delete <name>", "Delete a backup"));
            adminCommands.add(new CommandHelp("/f admin import hyfactions <path>", "Import from HyFactions mod"));
            ctx.sendMessage(HelpFormatter.buildHelp("HyperFactions Admin", null, adminCommands, null));
            return;
        }
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

        switch (adminCmd) {
            case "zone" -> handleAdminZone(ctx, store, ref, player, world, chunkX, chunkZ, Arrays.copyOfRange(args, 1, args.length));
            case "safezone" -> {
                // Quick shortcut: create SafeZone + claim current chunk
                String zoneName = args.length > 1 ? args[1] : "SafeZone-" + chunkX + "_" + chunkZ;
                ZoneManager.ZoneResult result = hyperFactions.getZoneManager().createZone(
                    zoneName, ZoneType.SAFE, world.getName(), chunkX, chunkZ, player.getUuid()
                );
                if (result == ZoneManager.ZoneResult.SUCCESS) {
                    ctx.sendMessage(prefix().insert(msg("Created SafeZone '" + zoneName + "' at " + chunkX + ", " + chunkZ, COLOR_GREEN)));
                } else if (result == ZoneManager.ZoneResult.CHUNK_CLAIMED) {
                    ctx.sendMessage(prefix().insert(msg("Cannot create zone: This chunk is claimed by a faction.", COLOR_RED)));
                } else if (result == ZoneManager.ZoneResult.ALREADY_EXISTS) {
                    ctx.sendMessage(prefix().insert(msg("A zone already exists at this location.", COLOR_RED)));
                } else if (result == ZoneManager.ZoneResult.NAME_TAKEN) {
                    ctx.sendMessage(prefix().insert(msg("A zone with that name already exists.", COLOR_RED)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
                }
            }
            case "warzone" -> {
                // Quick shortcut: create WarZone + claim current chunk
                String zoneName = args.length > 1 ? args[1] : "WarZone-" + chunkX + "_" + chunkZ;
                ZoneManager.ZoneResult result = hyperFactions.getZoneManager().createZone(
                    zoneName, ZoneType.WAR, world.getName(), chunkX, chunkZ, player.getUuid()
                );
                if (result == ZoneManager.ZoneResult.SUCCESS) {
                    ctx.sendMessage(prefix().insert(msg("Created WarZone '" + zoneName + "' at " + chunkX + ", " + chunkZ, COLOR_GREEN)));
                } else if (result == ZoneManager.ZoneResult.CHUNK_CLAIMED) {
                    ctx.sendMessage(prefix().insert(msg("Cannot create zone: This chunk is claimed by a faction.", COLOR_RED)));
                } else if (result == ZoneManager.ZoneResult.ALREADY_EXISTS) {
                    ctx.sendMessage(prefix().insert(msg("A zone already exists at this location.", COLOR_RED)));
                } else if (result == ZoneManager.ZoneResult.NAME_TAKEN) {
                    ctx.sendMessage(prefix().insert(msg("A zone with that name already exists.", COLOR_RED)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
                }
            }
            case "removezone" -> {
                // Quick shortcut: unclaim current chunk from its zone
                ZoneManager.ZoneResult result = hyperFactions.getZoneManager().unclaimChunkAt(world.getName(), chunkX, chunkZ);
                if (result == ZoneManager.ZoneResult.SUCCESS) {
                    ctx.sendMessage(prefix().insert(msg("Unclaimed chunk from zone.", COLOR_GREEN)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("No zone chunk found at this location.", COLOR_RED)));
                }
            }
            case "zoneflag" -> handleZoneFlag(ctx, world.getName(), chunkX, chunkZ, Arrays.copyOfRange(args, 1, args.length));
            case "update" -> handleAdminUpdate(ctx, player);
            case "backup" -> handleAdminBackup(ctx, player, Arrays.copyOfRange(args, 1, args.length));
            case "import" -> handleAdminImport(ctx, player, Arrays.copyOfRange(args, 1, args.length));
            default -> ctx.sendMessage(prefix().insert(msg("Unknown admin command. Use /f admin help", COLOR_RED)));
        }
    }

    // === Admin Zone Subcommands ===
    private void handleAdminZone(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                                 PlayerRef player, World world, int chunkX, int chunkZ, String[] args) {
        // No args - open zone GUI
        if (args.length == 0) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openAdminZone(playerEntity, ref, store, player);
            }
            return;
        }

        String subCmd = args[0].toLowerCase();
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (subCmd) {
            case "list" -> handleZoneList(ctx);
            case "create" -> handleZoneCreate(ctx, world.getName(), player.getUuid(), subArgs);
            case "delete" -> handleZoneDelete(ctx, subArgs);
            case "info" -> handleZoneInfo(ctx, world.getName(), chunkX, chunkZ, subArgs);
            case "claim" -> handleZoneClaim(ctx, world.getName(), chunkX, chunkZ, subArgs);
            case "unclaim" -> handleZoneUnclaim(ctx, world.getName(), chunkX, chunkZ);
            case "radius" -> handleZoneRadius(ctx, world.getName(), chunkX, chunkZ, subArgs);
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown zone command. Use /f admin help", COLOR_RED)));
            }
        }
    }

    private void handleZoneList(CommandContext ctx) {
        var zones = hyperFactions.getZoneManager().getAllZones();
        if (zones.isEmpty()) {
            ctx.sendMessage(prefix().insert(msg("No zones defined.", COLOR_GRAY)));
            return;
        }

        ctx.sendMessage(msg("=== Zones (" + zones.size() + ") ===", COLOR_CYAN).bold(true));
        for (Zone zone : zones) {
            String typeColor = zone.isSafeZone() ? "#2dd4bf" : "#c084fc";
            ctx.sendMessage(msg("  " + zone.name(), typeColor)
                .insert(msg(" (" + zone.type().name() + ", " + zone.getChunkCount() + " chunks)", COLOR_GRAY)));
        }
    }

    private void handleZoneCreate(CommandContext ctx, String worldName, UUID createdBy, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone create <safe|war> <name>", COLOR_RED)));
            return;
        }

        String typeStr = args[0].toLowerCase();
        String name = args[1];

        ZoneType type;
        if (typeStr.equals("safe") || typeStr.equals("safezone")) {
            type = ZoneType.SAFE;
        } else if (typeStr.equals("war") || typeStr.equals("warzone")) {
            type = ZoneType.WAR;
        } else {
            ctx.sendMessage(prefix().insert(msg("Invalid zone type. Use 'safe' or 'war'", COLOR_RED)));
            return;
        }

        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().createZone(name, type, worldName, createdBy);
        switch (result) {
            case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Created " + type.getDisplayName() + " '" + name + "' (empty, use claim to add chunks)", COLOR_GREEN)));
            case NAME_TAKEN -> ctx.sendMessage(prefix().insert(msg("A zone with that name already exists.", COLOR_RED)));
            case INVALID_NAME -> ctx.sendMessage(prefix().insert(msg("Invalid zone name. Must be 1-32 characters.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
        }
    }

    private void handleZoneDelete(CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone delete <name>", COLOR_RED)));
            return;
        }

        String name = args[0];
        Zone zone = hyperFactions.getZoneManager().getZoneByName(name);
        if (zone == null) {
            ctx.sendMessage(prefix().insert(msg("Zone '" + name + "' not found.", COLOR_RED)));
            return;
        }

        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().removeZone(zone.id());
        if (result == ZoneManager.ZoneResult.SUCCESS) {
            ctx.sendMessage(prefix().insert(msg("Deleted zone '" + name + "' (" + zone.getChunkCount() + " chunks released)", COLOR_GREEN)));
        } else {
            ctx.sendMessage(prefix().insert(msg("Failed to delete zone: " + result, COLOR_RED)));
        }
    }

    private void handleZoneInfo(CommandContext ctx, String worldName, int chunkX, int chunkZ, String[] args) {
        Zone zone;
        if (args.length > 0) {
            // Zone specified by name
            zone = hyperFactions.getZoneManager().getZoneByName(args[0]);
            if (zone == null) {
                ctx.sendMessage(prefix().insert(msg("Zone '" + args[0] + "' not found.", COLOR_RED)));
                return;
            }
        } else {
            // Zone at current location
            zone = hyperFactions.getZoneManager().getZone(worldName, chunkX, chunkZ);
            if (zone == null) {
                ctx.sendMessage(prefix().insert(msg("No zone at your location.", COLOR_RED)));
                return;
            }
        }

        String typeColor = zone.isSafeZone() ? "#2dd4bf" : "#c084fc";
        ctx.sendMessage(msg("=== Zone: " + zone.name() + " ===", typeColor).bold(true));
        ctx.sendMessage(msg("Type: ", COLOR_GRAY).insert(msg(zone.type().getDisplayName(), typeColor)));
        ctx.sendMessage(msg("World: ", COLOR_GRAY).insert(msg(zone.world(), COLOR_WHITE)));
        ctx.sendMessage(msg("Chunks: ", COLOR_GRAY).insert(msg(String.valueOf(zone.getChunkCount()), COLOR_WHITE)));

        // Show custom flags if any
        if (!zone.getFlags().isEmpty()) {
            ctx.sendMessage(msg("Custom Flags:", COLOR_GRAY));
            for (var entry : zone.getFlags().entrySet()) {
                ctx.sendMessage(msg("  " + entry.getKey() + ": " + entry.getValue(), COLOR_YELLOW));
            }
        }
    }

    private void handleZoneClaim(CommandContext ctx, String worldName, int chunkX, int chunkZ, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone claim <name>", COLOR_RED)));
            return;
        }

        String name = args[0];
        Zone zone = hyperFactions.getZoneManager().getZoneByName(name);
        if (zone == null) {
            ctx.sendMessage(prefix().insert(msg("Zone '" + name + "' not found.", COLOR_RED)));
            return;
        }

        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().claimChunk(zone.id(), worldName, chunkX, chunkZ);
        switch (result) {
            case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Claimed chunk (" + chunkX + ", " + chunkZ + ") for zone '" + name + "'", COLOR_GREEN)));
            case CHUNK_HAS_ZONE -> ctx.sendMessage(prefix().insert(msg("This chunk already belongs to another zone.", COLOR_RED)));
            case CHUNK_HAS_FACTION -> ctx.sendMessage(prefix().insert(msg("This chunk is claimed by a faction.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
        }
    }

    private void handleZoneUnclaim(CommandContext ctx, String worldName, int chunkX, int chunkZ) {
        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().unclaimChunkAt(worldName, chunkX, chunkZ);
        if (result == ZoneManager.ZoneResult.SUCCESS) {
            ctx.sendMessage(prefix().insert(msg("Unclaimed chunk (" + chunkX + ", " + chunkZ + ") from zone.", COLOR_GREEN)));
        } else {
            ctx.sendMessage(prefix().insert(msg("No zone chunk found at this location.", COLOR_RED)));
        }
    }

    private void handleZoneRadius(CommandContext ctx, String worldName, int chunkX, int chunkZ, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone radius <name> <radius> [circle|square]", COLOR_RED)));
            ctx.sendMessage(msg("  radius: 1-20 chunks", COLOR_GRAY));
            ctx.sendMessage(msg("  shape: circle (default) or square", COLOR_GRAY));
            return;
        }

        String name = args[0];
        Zone zone = hyperFactions.getZoneManager().getZoneByName(name);
        if (zone == null) {
            ctx.sendMessage(prefix().insert(msg("Zone '" + name + "' not found.", COLOR_RED)));
            return;
        }

        int radius;
        try {
            radius = Integer.parseInt(args[1]);
            if (radius < 1 || radius > 20) {
                ctx.sendMessage(prefix().insert(msg("Radius must be between 1 and 20.", COLOR_RED)));
                return;
            }
        } catch (NumberFormatException e) {
            ctx.sendMessage(prefix().insert(msg("Invalid radius number.", COLOR_RED)));
            return;
        }

        boolean circle = true;
        if (args.length > 2) {
            String shape = args[2].toLowerCase();
            if (shape.equals("square")) {
                circle = false;
            } else if (!shape.equals("circle")) {
                ctx.sendMessage(prefix().insert(msg("Invalid shape. Use 'circle' or 'square'.", COLOR_RED)));
                return;
            }
        }

        int claimed = hyperFactions.getZoneManager().claimRadius(zone.id(), worldName, chunkX, chunkZ, radius, circle);
        if (claimed > 0) {
            ctx.sendMessage(prefix().insert(msg("Claimed " + claimed + " chunks for zone '" + name + "'", COLOR_GREEN)));
        } else {
            ctx.sendMessage(prefix().insert(msg("No chunks could be claimed (all occupied or already in zone).", COLOR_YELLOW)));
        }
    }

    // === Zone Flag Management ===
    private void handleZoneFlag(CommandContext ctx, String worldName, int chunkX, int chunkZ, String[] args) {
        Zone zone = hyperFactions.getZoneManager().getZone(worldName, chunkX, chunkZ);
        if (zone == null) {
            ctx.sendMessage(prefix().insert(msg("No zone at your location. Stand in a zone to manage flags.", COLOR_RED)));
            return;
        }

        // No args - show current flags
        if (args.length == 0) {
            ctx.sendMessage(msg("=== Zone Flags: " + zone.name() + " ===", COLOR_CYAN).bold(true));
            ctx.sendMessage(msg("Zone Type: " + zone.type().getDisplayName(), COLOR_GRAY));
            ctx.sendMessage(msg("", COLOR_GRAY));

            for (String flag : ZoneFlags.ALL_FLAGS) {
                boolean effectiveValue = zone.getEffectiveFlag(flag);
                boolean isCustom = zone.hasFlagSet(flag);
                String valueStr = effectiveValue ? "true" : "false";
                String customStr = isCustom ? " (custom)" : " (default)";
                String color = effectiveValue ? COLOR_GREEN : COLOR_RED;
                ctx.sendMessage(msg("  " + flag + ": ", COLOR_GRAY).insert(msg(valueStr, color)).insert(msg(customStr, COLOR_GRAY)));
            }
            ctx.sendMessage(msg("", COLOR_GRAY));
            ctx.sendMessage(msg("Usage: /f admin zoneflag <flag> <true|false|clear>", COLOR_YELLOW));
            ctx.sendMessage(msg("Usage: /f admin zoneflag clearall (reset all to defaults)", COLOR_YELLOW));
            return;
        }

        // Handle "clearall" - reset all flags to defaults
        if (args[0].equalsIgnoreCase("clearall") || args[0].equalsIgnoreCase("resetall")) {
            ZoneManager.ZoneResult result = hyperFactions.getZoneManager().clearAllZoneFlags(zone.id());
            if (result == ZoneManager.ZoneResult.SUCCESS) {
                ctx.sendMessage(prefix().insert(msg("Cleared all custom flags for '" + zone.name() + "' - now using zone type defaults.", COLOR_GREEN)));
            } else {
                ctx.sendMessage(prefix().insert(msg("Failed to clear flags.", COLOR_RED)));
            }
            return;
        }

        // Get flag name
        String flagName = args[0].toLowerCase();
        if (!ZoneFlags.isValidFlag(flagName)) {
            ctx.sendMessage(prefix().insert(msg("Invalid flag: " + flagName, COLOR_RED)));
            ctx.sendMessage(msg("Valid flags: " + String.join(", ", ZoneFlags.ALL_FLAGS), COLOR_GRAY));
            return;
        }

        // Show specific flag value
        if (args.length == 1) {
            boolean effectiveValue = zone.getEffectiveFlag(flagName);
            boolean isCustom = zone.hasFlagSet(flagName);
            ctx.sendMessage(prefix().insert(msg("Flag '" + flagName + "' = " + effectiveValue, effectiveValue ? COLOR_GREEN : COLOR_RED))
                .insert(msg(isCustom ? " (custom)" : " (default)", COLOR_GRAY)));
            return;
        }

        // Set or clear flag
        String action = args[1].toLowerCase();
        ZoneManager.ZoneResult result;

        if (action.equals("clear") || action.equals("default") || action.equals("reset")) {
            result = hyperFactions.getZoneManager().clearZoneFlag(zone.id(), flagName);
            if (result == ZoneManager.ZoneResult.SUCCESS) {
                boolean defaultValue = zone.isSafeZone() ? ZoneFlags.getSafeZoneDefault(flagName) : ZoneFlags.getWarZoneDefault(flagName);
                ctx.sendMessage(prefix().insert(msg("Cleared flag '" + flagName + "' (now using default: " + defaultValue + ")", COLOR_GREEN)));
            } else {
                ctx.sendMessage(prefix().insert(msg("Failed to clear flag.", COLOR_RED)));
            }
        } else if (action.equals("true") || action.equals("false")) {
            boolean value = action.equals("true");
            result = hyperFactions.getZoneManager().setZoneFlag(zone.id(), flagName, value);
            if (result == ZoneManager.ZoneResult.SUCCESS) {
                ctx.sendMessage(prefix().insert(msg("Set flag '" + flagName + "' to " + value, COLOR_GREEN)));
            } else {
                ctx.sendMessage(prefix().insert(msg("Failed to set flag.", COLOR_RED)));
            }
        } else {
            ctx.sendMessage(prefix().insert(msg("Invalid value. Use: true, false, or clear", COLOR_RED)));
        }
    }

    // === Admin Update ===
    private void handleAdminUpdate(CommandContext ctx, PlayerRef player) {
        var updateChecker = hyperFactions.getUpdateChecker();
        if (updateChecker == null) {
            ctx.sendMessage(prefix().insert(msg("Update checker is not available.", COLOR_RED)));
            return;
        }

        // Check if there's an update available
        if (!updateChecker.hasUpdateAvailable()) {
            ctx.sendMessage(prefix().insert(msg("Checking for updates...", COLOR_YELLOW)));
            updateChecker.checkForUpdates(true).thenAccept(info -> {
                if (info == null) {
                    player.sendMessage(prefix().insert(msg("Plugin is already up-to-date (v" + updateChecker.getCurrentVersion() + ")", COLOR_GREEN)));
                } else {
                    player.sendMessage(prefix().insert(msg("Update available: v" + info.version(), COLOR_GREEN)));
                    startDownload(player, updateChecker, info);
                }
            });
            return;
        }

        var info = updateChecker.getCachedUpdate();
        if (info == null) {
            ctx.sendMessage(prefix().insert(msg("No update information available.", COLOR_RED)));
            return;
        }

        startDownload(player, updateChecker, info);
    }

    private void startDownload(PlayerRef player, com.hyperfactions.update.UpdateChecker updateChecker,
                               com.hyperfactions.update.UpdateChecker.UpdateInfo info) {
        player.sendMessage(prefix().insert(msg("Downloading HyperFactions v" + info.version() + "...", COLOR_YELLOW)));

        updateChecker.downloadUpdate(info).thenAccept(path -> {
            if (path == null) {
                player.sendMessage(prefix().insert(msg("Failed to download update. Check server logs.", COLOR_RED)));
            } else {
                player.sendMessage(prefix().insert(msg("Update downloaded successfully!", COLOR_GREEN)));
                player.sendMessage(msg("  File: " + path.getFileName(), COLOR_GRAY));
                player.sendMessage(msg("  Restart the server to apply the update.", COLOR_YELLOW));
            }
        });
    }

    // === Admin Backup Commands ===
    private void handleAdminBackup(CommandContext ctx, PlayerRef player, String[] args) {
        if (!hasPermission(player, Permissions.ADMIN_BACKUP)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to manage backups.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            showBackupHelp(ctx);
            return;
        }

        String subCmd = args[0].toLowerCase();
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (subCmd) {
            case "create" -> handleBackupCreate(ctx, player, subArgs);
            case "list" -> handleBackupList(ctx);
            case "restore" -> handleBackupRestore(ctx, player, subArgs);
            case "delete" -> handleBackupDelete(ctx, subArgs);
            case "help", "?" -> showBackupHelp(ctx);
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown backup command: " + subCmd, COLOR_RED)));
                showBackupHelp(ctx);
            }
        }
    }

    private void showBackupHelp(CommandContext ctx) {
        List<CommandHelp> commands = new ArrayList<>();
        commands.add(new CommandHelp("/f admin backup create [name]", "Create manual backup"));
        commands.add(new CommandHelp("/f admin backup list", "List all backups grouped by type"));
        commands.add(new CommandHelp("/f admin backup restore <name>", "Restore from backup (requires confirmation)"));
        commands.add(new CommandHelp("/f admin backup delete <name>", "Delete a backup"));
        ctx.sendMessage(HelpFormatter.buildHelp("Backup Management", "GFS rotation scheme", commands, null));
    }

    private void handleBackupCreate(CommandContext ctx, PlayerRef player, String[] args) {
        String customName = args.length > 0 ? String.join("_", args) : null;

        ctx.sendMessage(prefix().insert(msg("Creating backup...", COLOR_YELLOW)));

        hyperFactions.getBackupManager().createBackup(BackupType.MANUAL, customName, player.getUuid())
            .thenAccept(result -> {
                if (result instanceof BackupManager.BackupResult.Success success) {
                    player.sendMessage(prefix().insert(msg("Backup created successfully!", COLOR_GREEN)));
                    player.sendMessage(msg("  Name: " + success.metadata().name(), COLOR_GRAY));
                    player.sendMessage(msg("  Size: " + success.metadata().getFormattedSize(), COLOR_GRAY));
                } else if (result instanceof BackupManager.BackupResult.Failure failure) {
                    player.sendMessage(prefix().insert(msg("Backup failed: " + failure.error(), COLOR_RED)));
                }
            });
    }

    private void handleBackupList(CommandContext ctx) {
        Map<BackupType, List<BackupMetadata>> grouped = hyperFactions.getBackupManager().getBackupsGroupedByType();

        if (grouped.isEmpty()) {
            ctx.sendMessage(prefix().insert(msg("No backups found.", COLOR_GRAY)));
            return;
        }

        ctx.sendMessage(msg("=== Backups ===", COLOR_CYAN).bold(true));

        for (BackupType type : BackupType.values()) {
            List<BackupMetadata> backups = grouped.getOrDefault(type, List.of());
            if (backups.isEmpty()) continue;

            ctx.sendMessage(msg(type.getDisplayName() + " (" + backups.size() + "):", COLOR_YELLOW));
            for (BackupMetadata backup : backups) {
                ctx.sendMessage(msg("  " + backup.name(), COLOR_WHITE)
                    .insert(msg(" - " + backup.getFormattedTimestamp() + " (" + backup.getFormattedSize() + ")", COLOR_GRAY)));
            }
        }
    }

    private void handleBackupRestore(CommandContext ctx, PlayerRef player, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin backup restore <name>", COLOR_RED)));
            return;
        }

        String backupName = args[0];

        // Require confirmation for restore
        ConfirmationResult confirmResult = hyperFactions.getConfirmationManager().checkOrCreate(
            player.getUuid(), ConfirmationType.RESTORE_BACKUP, null
        );

        switch (confirmResult) {
            case NEEDS_CONFIRMATION, EXPIRED_RECREATED, DIFFERENT_ACTION -> {
                ctx.sendMessage(prefix().insert(msg("WARNING: Restoring will OVERWRITE current data!", COLOR_RED)));
                ctx.sendMessage(msg("Run the command again within 30 seconds to confirm.", COLOR_YELLOW));
            }
            case CONFIRMED -> {
                ctx.sendMessage(prefix().insert(msg("Restoring from backup '" + backupName + "'...", COLOR_YELLOW)));

                hyperFactions.getBackupManager().restoreBackup(backupName).thenAccept(result -> {
                    if (result instanceof BackupManager.RestoreResult.Success success) {
                        player.sendMessage(prefix().insert(msg("Restore completed! " + success.filesRestored() + " files restored.", COLOR_GREEN)));
                        player.sendMessage(msg("You should reload the plugin or restart the server.", COLOR_YELLOW));
                    } else if (result instanceof BackupManager.RestoreResult.Failure failure) {
                        player.sendMessage(prefix().insert(msg("Restore failed: " + failure.error(), COLOR_RED)));
                    }
                });
            }
        }
    }

    private void handleBackupDelete(CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin backup delete <name>", COLOR_RED)));
            return;
        }

        String backupName = args[0];

        hyperFactions.getBackupManager().deleteBackup(backupName).thenAccept(success -> {
            if (success) {
                ctx.sendMessage(prefix().insert(msg("Backup '" + backupName + "' deleted.", COLOR_GREEN)));
            } else {
                ctx.sendMessage(prefix().insert(msg("Backup not found: " + backupName, COLOR_RED)));
            }
        });
    }

    // === Admin Import Commands ===
    private void handleAdminImport(CommandContext ctx, PlayerRef player, String[] args) {
        if (!hasPermission(player, Permissions.ADMIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            showImportHelp(ctx);
            return;
        }

        String subCmd = args[0].toLowerCase();
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (subCmd) {
            case "hyfactions" -> handleImportHyFactions(ctx, player, subArgs);
            case "help", "?" -> showImportHelp(ctx);
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown import source: " + subCmd, COLOR_RED)));
                showImportHelp(ctx);
            }
        }
    }

    private void showImportHelp(CommandContext ctx) {
        List<CommandHelp> commands = new ArrayList<>();
        commands.add(new CommandHelp("/f admin import hyfactions <path>", "Import from HyFactions mod data"));
        commands.add(new CommandHelp("  Flags:", ""));
        commands.add(new CommandHelp("  dryrun", "Preview import without making changes (default)"));
        commands.add(new CommandHelp("  overwrite", "Replace existing factions with same ID"));
        commands.add(new CommandHelp("  skipzones", "Don't import SafeZones/WarZones"));
        commands.add(new CommandHelp("  skippower", "Don't import player power data"));
        ctx.sendMessage(HelpFormatter.buildHelp("Import Data", "Import factions from other mods", commands,
            "Example: /f admin import hyfactions mods/Kaws_Hyfaction overwrite"));
    }

    private void handleImportHyFactions(CommandContext ctx, PlayerRef player, String[] args) {
        // Parse arguments and flags
        // Flags use simple words (no -- prefix) to avoid Hytale command parser interception
        String path = null;
        boolean dryRun = true;
        boolean overwrite = false;
        boolean skipZones = false;
        boolean skipPower = false;

        for (String arg : args) {
            switch (arg.toLowerCase()) {
                case "dryrun", "dry-run", "dry" -> dryRun = true;
                case "overwrite", "force" -> {
                    overwrite = true;
                    dryRun = false;
                }
                case "skipzones", "skip-zones", "nozones" -> skipZones = true;
                case "skippower", "skip-power", "nopower" -> skipPower = true;
                default -> {
                    // If it doesn't match a flag, treat as path
                    if (path == null) {
                        path = arg;
                    } else {
                        ctx.sendMessage(prefix().insert(msg("Unknown flag: " + arg, COLOR_RED)));
                        return;
                    }
                }
            }
        }

        if (path == null) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin import hyfactions <path> [flags]", COLOR_RED)));
            ctx.sendMessage(msg("Example: /f admin import hyfactions mods/Kaws_Hyfaction", COLOR_GRAY));
            return;
        }

        // Resolve path relative to server directory
        Path sourcePath = Paths.get(path);
        if (!sourcePath.isAbsolute()) {
            // Relative to server working directory
            sourcePath = Paths.get(System.getProperty("user.dir"), path);
        }

        final Path finalPath = sourcePath;
        final boolean finalDryRun = dryRun;
        final boolean finalOverwrite = overwrite;
        final boolean finalSkipZones = skipZones;
        final boolean finalSkipPower = skipPower;

        // Show mode
        if (finalDryRun) {
            ctx.sendMessage(prefix().insert(msg("Running in DRY RUN mode - no changes will be made", COLOR_YELLOW)));
        } else if (finalOverwrite) {
            ctx.sendMessage(prefix().insert(msg("Running with OVERWRITE - existing factions will be replaced", COLOR_YELLOW)));
        }

        ctx.sendMessage(prefix().insert(msg("Starting HyFactions import from: " + finalPath, COLOR_CYAN)));

        // Check if import is already in progress
        if (HyFactionsImporter.isImportInProgress()) {
            ctx.sendMessage(prefix().insert(msg("An import is already in progress. Please wait for it to complete.", COLOR_RED)));
            return;
        }

        // Run import
        HyFactionsImporter importer = new HyFactionsImporter(
            hyperFactions.getFactionManager(),
            hyperFactions.getZoneManager(),
            hyperFactions.getPowerManager(),
            hyperFactions.getBackupManager()
        );

        importer.setDryRun(finalDryRun)
            .setOverwrite(finalOverwrite)
            .setSkipZones(finalSkipZones)
            .setSkipPower(finalSkipPower)
            .setCreateBackup(!finalDryRun) // Only create backup if not dry run
            .setProgressCallback(msg -> player.sendMessage(prefix().insert(msg(msg, COLOR_GRAY))));

        ImportResult result = importer.importFrom(finalPath);

        // Display results
        ctx.sendMessage(msg("", COLOR_WHITE));
        ctx.sendMessage(msg("=== Import " + (result.dryRun() ? "Preview" : "Complete") + " ===", COLOR_CYAN).bold(true));

        if (result.hasErrors()) {
            ctx.sendMessage(msg("Errors:", COLOR_RED));
            for (String error : result.errors()) {
                ctx.sendMessage(msg("  - " + error, COLOR_RED));
            }
        }

        if (result.hasWarnings()) {
            ctx.sendMessage(msg("Warnings:", COLOR_YELLOW));
            for (String warning : result.warnings()) {
                ctx.sendMessage(msg("  - " + warning, COLOR_YELLOW));
            }
        }

        ctx.sendMessage(msg("Factions: " + result.factionsImported() + " imported, " +
            result.factionsSkipped() + " skipped", COLOR_WHITE));
        ctx.sendMessage(msg("Claims: " + result.claimsImported() + " imported", COLOR_WHITE));
        ctx.sendMessage(msg("Zones: " + result.zonesCreated() + " created", COLOR_WHITE));
        ctx.sendMessage(msg("Players with power: " + result.playersWithPower(), COLOR_WHITE));

        if (result.dryRun() && !result.hasErrors()) {
            ctx.sendMessage(msg("", COLOR_WHITE));
            ctx.sendMessage(msg("To apply changes, run with --overwrite flag", COLOR_YELLOW));
        }
    }

    // === Debug ===
    private void handleDebug(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                              PlayerRef player, World world, String[] args) {
        if (!hasPermission(player, Permissions.ADMIN_DEBUG)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            showDebugHelp(ctx);
            return;
        }

        String debugCmd = args[0].toLowerCase();
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (debugCmd) {
            case "help", "?" -> showDebugHelp(ctx);
            case "toggle" -> handleDebugToggle(ctx, subArgs);
            case "status" -> handleDebugStatus(ctx);
            case "power" -> handleDebugPower(ctx, subArgs);
            case "claim" -> handleDebugClaim(ctx, store, ref, world, subArgs);
            case "protection" -> handleDebugProtection(ctx, store, ref, world, subArgs);
            case "combat" -> handleDebugCombat(ctx, subArgs);
            case "relation" -> handleDebugRelation(ctx, subArgs);
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown debug command: " + debugCmd, COLOR_RED)));
                showDebugHelp(ctx);
            }
        }
    }

    private void showDebugHelp(CommandContext ctx) {
        List<CommandHelp> debugCommands = new ArrayList<>();
        debugCommands.add(new CommandHelp("/f debug toggle <category|all>", "Toggle debug logging"));
        debugCommands.add(new CommandHelp("/f debug status", "Show debug status"));
        debugCommands.add(new CommandHelp("/f debug power <player>", "Show player/faction power details"));
        debugCommands.add(new CommandHelp("/f debug claim [x z]", "Show claim info at location"));
        debugCommands.add(new CommandHelp("/f debug protection <player>", "Show protection at location"));
        debugCommands.add(new CommandHelp("/f debug combat <player>", "Show combat tag status"));
        debugCommands.add(new CommandHelp("/f debug relation <f1> <f2>", "Show relation between factions"));
        ctx.sendMessage(HelpFormatter.buildHelp("HyperFactions Debug", "Server diagnostics", debugCommands, null));
    }

    private void handleDebugToggle(CommandContext ctx, String[] args) {
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f debug toggle <category|all>", COLOR_YELLOW)));
            ctx.sendMessage(msg("Categories: power, claim, combat, protection, relation, all", COLOR_GRAY));
            return;
        }

        String category = args[0].toLowerCase();

        if (category.equals("all")) {
            // Toggle all - if any are enabled, disable all. Otherwise enable all.
            boolean anyEnabled = false;
            for (Logger.DebugCategory cat : Logger.DebugCategory.values()) {
                if (Logger.isDebugEnabled(cat)) {
                    anyEnabled = true;
                    break;
                }
            }

            if (anyEnabled) {
                Logger.disableAll();
                ctx.sendMessage(prefix().insert(msg("Disabled all debug categories.", COLOR_RED)));
            } else {
                Logger.enableAll();
                ctx.sendMessage(prefix().insert(msg("Enabled all debug categories.", COLOR_GREEN)));
            }
        } else {
            // Toggle specific category
            try {
                Logger.DebugCategory cat = Logger.DebugCategory.valueOf(category.toUpperCase());
                boolean newState = !Logger.isDebugEnabled(cat);
                Logger.setDebugEnabled(cat, newState);
                ctx.sendMessage(prefix().insert(msg((newState ? "Enabled" : "Disabled") + " debug for: " + cat.name(),
                    newState ? COLOR_GREEN : COLOR_RED)));
            } catch (IllegalArgumentException e) {
                ctx.sendMessage(prefix().insert(msg("Unknown category: " + category, COLOR_RED)));
                ctx.sendMessage(msg("Valid categories: power, claim, combat, protection, relation", COLOR_GRAY));
            }
        }
    }

    private void handleDebugStatus(CommandContext ctx) {
        ctx.sendMessage(msg("=== Debug Status ===", COLOR_CYAN).bold(true));

        for (Logger.DebugCategory cat : Logger.DebugCategory.values()) {
            boolean enabled = Logger.isDebugEnabled(cat);
            String status = enabled ? "ENABLED" : "disabled";
            String color = enabled ? COLOR_GREEN : COLOR_GRAY;
            ctx.sendMessage(msg("  " + cat.name() + ": ", COLOR_WHITE).insert(msg(status, color)));
        }

        ctx.sendMessage(msg("", COLOR_GRAY));
        ctx.sendMessage(msg("Use /f debug toggle <category> to toggle", COLOR_GRAY));
    }

    private void handleDebugPower(CommandContext ctx, String[] args) {
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f debug power <player>", COLOR_YELLOW)));
            return;
        }

        String playerName = args[0];
        PlayerRef targetRef = findOnlinePlayer(playerName);

        if (targetRef == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found or offline: " + playerName, COLOR_RED)));
            return;
        }

        UUID playerUuid = targetRef.getUuid();
        PlayerPower power = hyperFactions.getPowerManager().getPlayerPower(playerUuid);

        ctx.sendMessage(msg("=== Power Debug: " + playerName + " ===", COLOR_CYAN).bold(true));
        ctx.sendMessage(msg("  UUID: ", COLOR_GRAY).insert(msg(playerUuid.toString(), COLOR_WHITE)));
        ctx.sendMessage(msg("  Power: ", COLOR_GRAY).insert(msg(String.format("%.2f / %.2f", power.power(), power.maxPower()), COLOR_GREEN)));
        ctx.sendMessage(msg("  Percent: ", COLOR_GRAY).insert(msg(String.format("%.1f%%", power.getPowerPercent() * 100), COLOR_YELLOW)));

        // Faction info
        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(playerUuid);
        if (faction != null) {
            ctx.sendMessage(msg("", COLOR_GRAY));
            ctx.sendMessage(msg("  Faction: ", COLOR_GRAY).insert(msg(faction.name(), COLOR_CYAN)));
            double factionPower = hyperFactions.getPowerManager().getFactionPower(faction.id());
            double factionMaxPower = hyperFactions.getPowerManager().getFactionMaxPower(faction.id());
            int claimCap = hyperFactions.getPowerManager().getFactionClaimCapacity(faction.id());
            int claimCount = faction.claims().size();
            boolean raidable = hyperFactions.getPowerManager().isFactionRaidable(faction.id());

            ctx.sendMessage(msg("  Faction Power: ", COLOR_GRAY).insert(msg(String.format("%.2f / %.2f", factionPower, factionMaxPower), COLOR_GREEN)));
            ctx.sendMessage(msg("  Claims: ", COLOR_GRAY).insert(msg(claimCount + " / " + claimCap, claimCount > claimCap ? COLOR_RED : COLOR_GREEN)));
            ctx.sendMessage(msg("  Raidable: ", COLOR_GRAY).insert(msg(raidable ? "YES" : "NO", raidable ? COLOR_RED : COLOR_GREEN)));
        } else {
            ctx.sendMessage(msg("  Faction: ", COLOR_GRAY).insert(msg("None", COLOR_GRAY)));
        }
    }

    private void handleDebugClaim(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, World world, String[] args) {
        int chunkX, chunkZ;

        if (args.length >= 2) {
            try {
                chunkX = Integer.parseInt(args[0]);
                chunkZ = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                ctx.sendMessage(prefix().insert(msg("Invalid coordinates.", COLOR_RED)));
                return;
            }
        } else {
            // Use player's current location
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                ctx.sendMessage(prefix().insert(msg("Cannot determine location.", COLOR_RED)));
                return;
            }
            Vector3d pos = transform.getPosition();
            chunkX = ChunkUtil.toChunkCoord(pos.getX());
            chunkZ = ChunkUtil.toChunkCoord(pos.getZ());
        }

        String worldName = world.getName();
        ctx.sendMessage(msg("=== Claim Debug: " + worldName + " @ " + chunkX + ", " + chunkZ + " ===", COLOR_CYAN).bold(true));

        // Check zone
        Zone zone = hyperFactions.getZoneManager().getZone(worldName, chunkX, chunkZ);
        if (zone != null) {
            ctx.sendMessage(msg("  Zone: ", COLOR_GRAY).insert(msg(zone.name() + " (" + zone.type().getDisplayName() + ")", COLOR_YELLOW)));
            ctx.sendMessage(msg("  Zone ID: ", COLOR_GRAY).insert(msg(zone.id().toString(), COLOR_WHITE)));
            return;
        }

        // Check claim
        UUID claimOwner = hyperFactions.getClaimManager().getClaimOwner(worldName, chunkX, chunkZ);
        if (claimOwner == null) {
            ctx.sendMessage(msg("  Status: ", COLOR_GRAY).insert(msg("Wilderness (unclaimed)", COLOR_GREEN)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getFaction(claimOwner);
        if (faction == null) {
            ctx.sendMessage(msg("  Status: ", COLOR_GRAY).insert(msg("ERROR - Unknown faction: " + claimOwner, COLOR_RED)));
            return;
        }

        ctx.sendMessage(msg("  Owner: ", COLOR_GRAY).insert(msg(faction.name(), COLOR_CYAN)));
        ctx.sendMessage(msg("  Faction ID: ", COLOR_GRAY).insert(msg(claimOwner.toString(), COLOR_WHITE)));

        // Find claim details
        for (FactionClaim claim : faction.claims()) {
            if (claim.world().equals(worldName) && claim.chunkX() == chunkX && claim.chunkZ() == chunkZ) {
                ctx.sendMessage(msg("  Claimed at: ", COLOR_GRAY).insert(msg(TimeUtil.formatDateTime(claim.claimedAt()), COLOR_WHITE)));
                PlayerRef claimerRef = plugin.getTrackedPlayer(claim.claimedBy());
                String claimerName = claimerRef != null ? claimerRef.getUsername() : claim.claimedBy().toString();
                ctx.sendMessage(msg("  Claimed by: ", COLOR_GRAY).insert(msg(claimerName, COLOR_WHITE)));
                break;
            }
        }

        // Check if raidable
        boolean raidable = hyperFactions.getPowerManager().isFactionRaidable(claimOwner);
        ctx.sendMessage(msg("  Raidable: ", COLOR_GRAY).insert(msg(raidable ? "YES - Can be overclaimed" : "NO - Protected", raidable ? COLOR_RED : COLOR_GREEN)));
    }

    private void handleDebugProtection(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, World world, String[] args) {
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f debug protection <player>", COLOR_YELLOW)));
            return;
        }

        String playerName = args[0];
        PlayerRef targetRef = findOnlinePlayer(playerName);

        if (targetRef == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found or offline: " + playerName, COLOR_RED)));
            return;
        }

        UUID playerUuid = targetRef.getUuid();

        // Get player's current location (from executor's perspective for now)
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            ctx.sendMessage(prefix().insert(msg("Cannot determine location.", COLOR_RED)));
            return;
        }
        Vector3d pos = transform.getPosition();
        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());
        String worldName = world.getName();

        ctx.sendMessage(msg("=== Protection Debug: " + playerName + " ===", COLOR_CYAN).bold(true));
        ctx.sendMessage(msg("  Location: ", COLOR_GRAY).insert(msg(worldName + " @ " + chunkX + ", " + chunkZ, COLOR_WHITE)));

        // Test different interaction types
        for (var type : com.hyperfactions.protection.ProtectionChecker.InteractionType.values()) {
            var result = hyperFactions.getProtectionChecker().canInteractChunk(playerUuid, worldName, chunkX, chunkZ, type);
            boolean allowed = hyperFactions.getProtectionChecker().isAllowed(result);
            ctx.sendMessage(msg("  " + type.name() + ": ", COLOR_GRAY)
                .insert(msg(result.name(), allowed ? COLOR_GREEN : COLOR_RED)));
        }

        // Player's faction context
        Faction playerFaction = hyperFactions.getFactionManager().getPlayerFaction(playerUuid);
        ctx.sendMessage(msg("", COLOR_GRAY));
        ctx.sendMessage(msg("  Player Faction: ", COLOR_GRAY).insert(msg(playerFaction != null ? playerFaction.name() : "None", COLOR_CYAN)));

        // Claim owner context
        UUID claimOwner = hyperFactions.getClaimManager().getClaimOwner(worldName, chunkX, chunkZ);
        if (claimOwner != null && playerFaction != null) {
            Faction ownerFaction = hyperFactions.getFactionManager().getFaction(claimOwner);
            RelationType relation = hyperFactions.getRelationManager().getRelation(playerFaction.id(), claimOwner);
            ctx.sendMessage(msg("  Claim Owner: ", COLOR_GRAY).insert(msg(ownerFaction != null ? ownerFaction.name() : "Unknown", COLOR_CYAN)));
            ctx.sendMessage(msg("  Relation: ", COLOR_GRAY).insert(msg(relation.name(), COLOR_YELLOW)));
        }
    }

    private void handleDebugCombat(CommandContext ctx, String[] args) {
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f debug combat <player>", COLOR_YELLOW)));
            return;
        }

        String playerName = args[0];
        PlayerRef targetRef = findOnlinePlayer(playerName);

        if (targetRef == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found or offline: " + playerName, COLOR_RED)));
            return;
        }

        UUID playerUuid = targetRef.getUuid();
        CombatTagManager ctm = hyperFactions.getCombatTagManager();

        ctx.sendMessage(msg("=== Combat Debug: " + playerName + " ===", COLOR_CYAN).bold(true));
        ctx.sendMessage(msg("  UUID: ", COLOR_GRAY).insert(msg(playerUuid.toString(), COLOR_WHITE)));

        // Combat tag status
        boolean isTagged = ctm.isTagged(playerUuid);
        ctx.sendMessage(msg("  Combat Tagged: ", COLOR_GRAY).insert(msg(isTagged ? "YES" : "NO", isTagged ? COLOR_RED : COLOR_GREEN)));
        if (isTagged) {
            int remaining = ctm.getRemainingSeconds(playerUuid);
            ctx.sendMessage(msg("  Tag Expires In: ", COLOR_GRAY).insert(msg(remaining + " seconds", COLOR_YELLOW)));
        }

        // Spawn protection status
        boolean hasSpawnProt = ctm.hasSpawnProtection(playerUuid);
        ctx.sendMessage(msg("  Spawn Protected: ", COLOR_GRAY).insert(msg(hasSpawnProt ? "YES" : "NO", hasSpawnProt ? COLOR_GREEN : COLOR_GRAY)));
        if (hasSpawnProt) {
            int remaining = ctm.getSpawnProtectionRemainingSeconds(playerUuid);
            ctx.sendMessage(msg("  Protection Expires In: ", COLOR_GRAY).insert(msg(remaining + " seconds", COLOR_YELLOW)));
        }
    }

    private void handleDebugRelation(CommandContext ctx, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f debug relation <faction1> <faction2>", COLOR_YELLOW)));
            return;
        }

        String name1 = args[0];
        String name2 = args[1];

        Faction faction1 = hyperFactions.getFactionManager().getFactionByName(name1);
        Faction faction2 = hyperFactions.getFactionManager().getFactionByName(name2);

        if (faction1 == null) {
            ctx.sendMessage(prefix().insert(msg("Faction not found: " + name1, COLOR_RED)));
            return;
        }
        if (faction2 == null) {
            ctx.sendMessage(prefix().insert(msg("Faction not found: " + name2, COLOR_RED)));
            return;
        }

        RelationType relation1to2 = hyperFactions.getRelationManager().getRelation(faction1.id(), faction2.id());
        RelationType relation2to1 = hyperFactions.getRelationManager().getRelation(faction2.id(), faction1.id());

        ctx.sendMessage(msg("=== Relation Debug ===", COLOR_CYAN).bold(true));
        ctx.sendMessage(msg("  " + faction1.name() + " -> " + faction2.name() + ": ", COLOR_GRAY)
            .insert(msg(relation1to2.name(), getRelationColor(relation1to2))));
        ctx.sendMessage(msg("  " + faction2.name() + " -> " + faction1.name() + ": ", COLOR_GRAY)
            .insert(msg(relation2to1.name(), getRelationColor(relation2to1))));

        // Check for pending ally requests
        boolean pending1to2 = hyperFactions.getRelationManager().hasPendingRequest(faction1.id(), faction2.id());
        boolean pending2to1 = hyperFactions.getRelationManager().hasPendingRequest(faction2.id(), faction1.id());

        if (pending1to2 || pending2to1) {
            ctx.sendMessage(msg("", COLOR_GRAY));
            ctx.sendMessage(msg("  Pending Requests:", COLOR_YELLOW));
            if (pending1to2) {
                ctx.sendMessage(msg("    " + faction1.name() + " sent ally request to " + faction2.name(), COLOR_GRAY));
            }
            if (pending2to1) {
                ctx.sendMessage(msg("    " + faction2.name() + " sent ally request to " + faction1.name(), COLOR_GRAY));
            }
        }
    }

    private String getRelationColor(RelationType relation) {
        return switch (relation) {
            case OWN -> COLOR_CYAN;
            case ALLY -> COLOR_GREEN;
            case ENEMY -> COLOR_RED;
            case NEUTRAL -> COLOR_GRAY;
        };
    }

    // === Reload ===
    private void handleReload(CommandContext ctx, PlayerRef player) {
        if (!hasPermission(player, Permissions.ADMIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        plugin.reloadConfig();
        ctx.sendMessage(prefix().insert(msg("Configuration reloaded.", COLOR_GREEN)));
    }

    // === Sync ===
    private void handleSync(CommandContext ctx, PlayerRef player) {
        if (!hasPermission(player, Permissions.ADMIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        ctx.sendMessage(prefix().insert(msg("Syncing faction data from disk...", COLOR_CYAN)));

        hyperFactions.getFactionManager().syncFromDisk().thenAccept(result -> {
            ctx.sendMessage(prefix().insert(Message.join(
                msg("Sync complete: ", COLOR_GREEN),
                msg(result.factionsUpdated() + " factions updated, ", COLOR_GRAY),
                msg(result.membersAdded() + " members added, ", COLOR_GRAY),
                msg(result.membersUpdated() + " members updated.", COLOR_GRAY)
            )));
        }).exceptionally(e -> {
            ctx.sendMessage(prefix().insert(msg("Sync failed: " + e.getMessage(), COLOR_RED)));
            return null;
        });
    }

    // === Rename ===
    private void handleRename(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.RENAME)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        FactionMember member = faction.getMember(player.getUuid());
        if (member == null || !member.isLeader()) {
            ctx.sendMessage(prefix().insert(msg("Only the leader can rename the faction.", COLOR_RED)));
            return;
        }

        String[] args = fctx.getArgs();

        // GUI mode: Open settings page if no args and not text mode
        if (!fctx.hasArgs() && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, faction);
            }
            return;
        }

        // Text mode requires args
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f rename <name>", COLOR_RED)));
            return;
        }

        String newName = String.join(" ", args);
        HyperFactionsConfig config = HyperFactionsConfig.get();

        if (newName.length() < config.getMinNameLength()) {
            ctx.sendMessage(prefix().insert(msg("Name is too short (min " + config.getMinNameLength() + " chars).", COLOR_RED)));
            return;
        }
        if (newName.length() > config.getMaxNameLength()) {
            ctx.sendMessage(prefix().insert(msg("Name is too long (max " + config.getMaxNameLength() + " chars).", COLOR_RED)));
            return;
        }
        if (hyperFactions.getFactionManager().isNameTaken(newName) && !newName.equalsIgnoreCase(faction.name())) {
            ctx.sendMessage(prefix().insert(msg("That name is already taken.", COLOR_RED)));
            return;
        }

        String oldName = faction.name();
        Faction updated = faction.withName(newName)
            .withLog(FactionLog.create(FactionLog.LogType.SETTINGS_CHANGE,
                "Renamed from '" + oldName + "' to '" + newName + "'", player.getUuid()));

        hyperFactions.getFactionManager().updateFaction(updated);

        // Refresh world maps to show new faction name
        if (hyperFactions.getWorldMapService() != null) {
            hyperFactions.getWorldMapService().refreshAllWorldMaps();
        }

        ctx.sendMessage(prefix().insert(msg("Faction renamed to ", COLOR_GREEN))
            .insert(msg(newName, COLOR_CYAN)).insert(msg("!", COLOR_GREEN)));
        broadcastToFaction(faction.id(), prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
            .insert(msg(" renamed the faction to ", COLOR_GREEN))
            .insert(msg(newName, COLOR_CYAN)));

        // After action, open settings page if not text mode
        if (fctx.shouldOpenGuiAfterAction()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                // Refresh faction to get updated data
                Faction refreshed = hyperFactions.getFactionManager().getFaction(faction.id());
                if (refreshed != null) {
                    hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, refreshed);
                }
            }
        }
    }

    // === Description ===
    private void handleDesc(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                            PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.DESC)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        FactionMember member = faction.getMember(player.getUuid());
        if (member == null || !member.isOfficerOrHigher()) {
            ctx.sendMessage(prefix().insert(msg("You must be an officer to set the description.", COLOR_RED)));
            return;
        }

        String[] args = fctx.getArgs();

        // GUI mode: Open settings page if no args and not text mode
        if (!fctx.hasArgs() && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, faction);
            }
            return;
        }

        // Text mode without args clears description
        String description = args.length > 0 ? String.join(" ", args) : null;

        Faction updated = faction.withDescription(description)
            .withLog(FactionLog.create(FactionLog.LogType.SETTINGS_CHANGE,
                description != null ? "Description set" : "Description cleared", player.getUuid()));

        hyperFactions.getFactionManager().updateFaction(updated);

        if (description != null) {
            ctx.sendMessage(prefix().insert(msg("Faction description set!", COLOR_GREEN)));
        } else {
            ctx.sendMessage(prefix().insert(msg("Faction description cleared.", COLOR_GREEN)));
        }

        // After action, open settings page if not text mode
        if (fctx.shouldOpenGuiAfterAction()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                Faction refreshed = hyperFactions.getFactionManager().getFaction(faction.id());
                if (refreshed != null) {
                    hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, refreshed);
                }
            }
        }
    }

    // === Color ===
    private void handleColor(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                             PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.COLOR)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        FactionMember member = faction.getMember(player.getUuid());
        if (member == null || !member.isOfficerOrHigher()) {
            ctx.sendMessage(prefix().insert(msg("You must be an officer to change the color.", COLOR_RED)));
            return;
        }

        if (!HyperFactionsConfig.get().isAllowColors()) {
            ctx.sendMessage(prefix().insert(msg("Faction colors are disabled.", COLOR_RED)));
            return;
        }

        String[] args = fctx.getArgs();

        // GUI mode: Open settings page if no args and not text mode
        if (!fctx.hasArgs() && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, faction);
            }
            return;
        }

        // Text mode requires args
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f color <code>", COLOR_RED)));
            ctx.sendMessage(msg("Valid codes: 0-9, a-f", COLOR_GRAY));
            return;
        }

        String colorCode = args[0].toLowerCase();
        if (colorCode.length() != 1 || !colorCode.matches("[0-9a-f]")) {
            ctx.sendMessage(prefix().insert(msg("Invalid color code. Use 0-9 or a-f.", COLOR_RED)));
            return;
        }

        Faction updated = faction.withColor(colorCode)
            .withLog(FactionLog.create(FactionLog.LogType.SETTINGS_CHANGE,
                "Color changed to '" + colorCode + "'", player.getUuid()));

        hyperFactions.getFactionManager().updateFaction(updated);

        // Refresh world maps to show new faction color
        hyperFactions.getWorldMapService().refreshAllWorldMaps();

        ctx.sendMessage(prefix().insert(msg("Faction color updated to ", COLOR_GREEN))
            .insert(msg("\u00A7" + colorCode + "this color", null))
            .insert(msg("!", COLOR_GREEN)));

        // After action, open settings page if not text mode
        if (fctx.shouldOpenGuiAfterAction()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                Faction refreshed = hyperFactions.getFactionManager().getFaction(faction.id());
                if (refreshed != null) {
                    hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, refreshed);
                }
            }
        }
    }

    // === Open ===
    private void handleOpen(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                            PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.OPEN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        FactionMember member = faction.getMember(player.getUuid());
        if (member == null || !member.isLeader()) {
            ctx.sendMessage(prefix().insert(msg("Only the leader can change this setting.", COLOR_RED)));
            return;
        }

        if (faction.open()) {
            ctx.sendMessage(prefix().insert(msg("Your faction is already open.", COLOR_YELLOW)));
            return;
        }

        Faction updated = faction.withOpen(true)
            .withLog(FactionLog.create(FactionLog.LogType.SETTINGS_CHANGE,
                "Faction set to open", player.getUuid()));

        hyperFactions.getFactionManager().updateFaction(updated);

        ctx.sendMessage(prefix().insert(msg("Your faction is now open! Anyone can join with /f join.", COLOR_GREEN)));
        broadcastToFaction(faction.id(), prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
            .insert(msg(" opened the faction to public joining.", COLOR_GREEN)));

        // After action, open settings page if not text mode
        if (!fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                Faction refreshed = hyperFactions.getFactionManager().getFaction(faction.id());
                if (refreshed != null) {
                    hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, refreshed);
                }
            }
        }
    }

    // === Close ===
    private void handleClose(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                             PlayerRef player, FactionCommandContext fctx) {
        if (!hasPermission(player, Permissions.CLOSE)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        FactionMember member = faction.getMember(player.getUuid());
        if (member == null || !member.isLeader()) {
            ctx.sendMessage(prefix().insert(msg("Only the leader can change this setting.", COLOR_RED)));
            return;
        }

        if (!faction.open()) {
            ctx.sendMessage(prefix().insert(msg("Your faction is already closed.", COLOR_YELLOW)));
            return;
        }

        Faction updated = faction.withOpen(false)
            .withLog(FactionLog.create(FactionLog.LogType.SETTINGS_CHANGE,
                "Faction set to invite-only", player.getUuid()));

        hyperFactions.getFactionManager().updateFaction(updated);

        ctx.sendMessage(prefix().insert(msg("Your faction is now invite-only.", COLOR_GREEN)));
        broadcastToFaction(faction.id(), prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
            .insert(msg(" closed the faction to invite-only.", COLOR_GREEN)));

        // After action, open settings page if not text mode
        if (!fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                Faction refreshed = hyperFactions.getFactionManager().getFaction(faction.id());
                if (refreshed != null) {
                    hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, refreshed);
                }
            }
        }
    }

    // === Stuck ===
    private void handleStuck(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
        if (!hasPermission(player, Permissions.STUCK)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to use /f stuck.", COLOR_RED)));
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        UUID playerUuid = player.getUuid();

        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

        // Check if in enemy/neutral territory
        UUID claimOwner = hyperFactions.getClaimManager().getClaimOwner(world.getName(), chunkX, chunkZ);
        Faction playerFaction = hyperFactions.getFactionManager().getPlayerFaction(playerUuid);

        if (claimOwner == null) {
            ctx.sendMessage(prefix().insert(msg("You're not stuck - this is wilderness.", COLOR_RED)));
            return;
        }

        if (playerFaction != null && claimOwner.equals(playerFaction.id())) {
            ctx.sendMessage(prefix().insert(msg("You're not stuck - this is your territory.", COLOR_RED)));
            return;
        }

        if (playerFaction != null && playerFaction.isAlly(claimOwner)) {
            ctx.sendMessage(prefix().insert(msg("You're not stuck - this is ally territory.", COLOR_RED)));
            return;
        }

        // Combat check
        if (hyperFactions.getCombatTagManager().isTagged(playerUuid)) {
            ctx.sendMessage(prefix().insert(msg("You cannot use /f stuck while in combat!", COLOR_RED)));
            return;
        }

        // Find nearest safe chunk
        int[] safeChunk = findNearestSafeChunk(world.getName(), chunkX, chunkZ, playerFaction);
        if (safeChunk == null) {
            ctx.sendMessage(prefix().insert(msg("Could not find a safe location.", COLOR_RED)));
            return;
        }

        // Create teleport location (center of safe chunk)
        double targetX = (safeChunk[0] << 4) + 8;
        double targetZ = (safeChunk[1] << 4) + 8;
        double targetY = pos.getY();

        // Use extended warmup for stuck (30 seconds by default)
        int warmupSeconds = HyperFactionsConfig.get().getStuckWarmupSeconds();

        TeleportManager.StartLocation startLoc = new TeleportManager.StartLocation(
            world.getName(), pos.getX(), pos.getY(), pos.getZ()
        );

        ctx.sendMessage(prefix().insert(msg("Teleporting to safety in " + warmupSeconds + " seconds. Don't move!", COLOR_YELLOW)));

        // Schedule teleport with extended warmup
        final double finalTargetX = targetX;
        final double finalTargetZ = targetZ;
        final double finalTargetY = targetY;

        int taskId = hyperFactions.scheduleDelayedTask(warmupSeconds * 20, () -> {
            // Recheck combat tag
            if (hyperFactions.getCombatTagManager().isTagged(playerUuid)) {
                player.sendMessage(prefix().insert(msg("Teleportation cancelled - you are in combat!", COLOR_RED)));
                return;
            }

            // Recheck if still pending
            TeleportManager.PendingTeleport pending = hyperFactions.getTeleportManager().getPending(playerUuid);
            if (pending == null) {
                return;
            }
            hyperFactions.getTeleportManager().cancelPending(playerUuid, hyperFactions::cancelTask);

            // Execute teleport using proper Teleport component
            Vector3d position = new Vector3d(finalTargetX, finalTargetY, finalTargetZ);
            Vector3f rotation = new Vector3f(0, 0, 0);
            Teleport teleport = new Teleport(world, position, rotation);
            store.addComponent(ref, Teleport.getComponentType(), teleport);

            player.sendMessage(prefix().insert(msg("Teleported to safety!", COLOR_GREEN)));
        });

        // Store as pending teleport
        TeleportManager.PendingTeleport pending = new TeleportManager.PendingTeleport(
            playerUuid, null, startLoc,
            System.currentTimeMillis(), warmupSeconds, taskId, null
        );
        // Note: We're directly tracking this since TeleportManager.teleportToHome doesn't fit this use case
    }

    /**
     * Finds the nearest safe chunk (wilderness, own claim, or ally claim).
     */
    private int[] findNearestSafeChunk(String world, int startX, int startZ, Faction playerFaction) {
        int maxRadius = 10;

        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;

                    int checkX = startX + dx;
                    int checkZ = startZ + dz;
                    UUID owner = hyperFactions.getClaimManager().getClaimOwner(world, checkX, checkZ);

                    // Safe if: wilderness, own claim, or ally claim
                    if (owner == null) return new int[]{checkX, checkZ};
                    if (playerFaction != null) {
                        if (owner.equals(playerFaction.id())) return new int[]{checkX, checkZ};
                        if (playerFaction.isAlly(owner)) return new int[]{checkX, checkZ};
                    }
                }
            }
        }
        return null;
    }

    // === Helpers ===

    private boolean hasPermission(PlayerRef player, String permission) {
        return PermissionManager.get().hasPermission(player.getUuid(), permission);
    }

    private PlayerRef findOnlinePlayer(String name) {
        for (PlayerRef player : plugin.getTrackedPlayers().values()) {
            if (player.getUsername().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    private void broadcastToFaction(UUID factionId, Message message) {
        Faction faction = hyperFactions.getFactionManager().getFaction(factionId);
        if (faction == null) return;

        for (UUID memberUuid : faction.members().keySet()) {
            PlayerRef member = plugin.getTrackedPlayer(memberUuid);
            if (member != null) {
                member.sendMessage(message);
            }
        }
    }
}
