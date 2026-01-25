package com.hyperfactions.command;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.data.*;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.integration.HyperPermsIntegration;
import com.hyperfactions.manager.*;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.TimeUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

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
            showHelp(ctx, player);
            return;
        }

        String subcommand = parts[1].toLowerCase();
        String[] subArgs = parts.length > 2 ? Arrays.copyOfRange(parts, 2, parts.length) : new String[0];

        switch (subcommand) {
            case "create" -> handleCreate(ctx, player, subArgs);
            case "disband" -> handleDisband(ctx, player);
            case "invite" -> handleInvite(ctx, player, subArgs);
            case "accept", "join" -> handleAccept(ctx, player, subArgs);
            case "leave" -> handleLeave(ctx, player);
            case "kick" -> handleKick(ctx, player, subArgs);
            case "promote" -> handlePromote(ctx, player, subArgs);
            case "demote" -> handleDemote(ctx, player, subArgs);
            case "transfer" -> handleTransfer(ctx, player, subArgs);
            case "claim" -> handleClaim(ctx, store, ref, player, currentWorld);
            case "unclaim" -> handleUnclaim(ctx, store, ref, player, currentWorld);
            case "overclaim" -> handleOverclaim(ctx, store, ref, player, currentWorld);
            case "home" -> handleHome(ctx, store, ref, player, currentWorld);
            case "sethome" -> handleSetHome(ctx, store, ref, player, currentWorld);
            case "ally" -> handleAlly(ctx, player, subArgs);
            case "enemy" -> handleEnemy(ctx, player, subArgs);
            case "neutral" -> handleNeutral(ctx, player, subArgs);
            case "info", "show" -> handleInfo(ctx, player, subArgs);
            case "list" -> handleList(ctx, player);
            case "map" -> handleMap(ctx, store, ref, player, currentWorld);
            case "who" -> handleWho(ctx, player, subArgs);
            case "power" -> handlePower(ctx, player, subArgs);
            case "chat", "c" -> handleChat(ctx, player, subArgs);
            case "help", "?" -> showHelp(ctx, player);
            case "gui", "menu" -> handleGui(ctx, store, ref, player);
            case "admin" -> handleAdmin(ctx, store, ref, player, currentWorld, subArgs);
            case "reload" -> handleReload(ctx, player);
            case "rename" -> handleRename(ctx, player, subArgs);
            case "desc", "description" -> handleDesc(ctx, player, subArgs);
            case "color" -> handleColor(ctx, player, subArgs);
            case "open" -> handleOpen(ctx, player);
            case "close" -> handleClose(ctx, player);
            case "stuck" -> handleStuck(ctx, store, ref, player, currentWorld);
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown subcommand: " + subcommand, COLOR_RED)));
                showHelp(ctx, player);
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

    private void showHelp(CommandContext ctx, PlayerRef player) {
        ctx.sendMessage(msg("=== HyperFactions Help ===", COLOR_CYAN).bold(true));
        ctx.sendMessage(msg("/f create <name>", COLOR_YELLOW).insert(msg(" - Create a faction", COLOR_GRAY)));
        ctx.sendMessage(msg("/f disband", COLOR_YELLOW).insert(msg(" - Disband your faction", COLOR_GRAY)));
        ctx.sendMessage(msg("/f invite <player>", COLOR_YELLOW).insert(msg(" - Invite a player", COLOR_GRAY)));
        ctx.sendMessage(msg("/f accept [faction]", COLOR_YELLOW).insert(msg(" - Accept an invite", COLOR_GRAY)));
        ctx.sendMessage(msg("/f leave", COLOR_YELLOW).insert(msg(" - Leave your faction", COLOR_GRAY)));
        ctx.sendMessage(msg("/f kick <player>", COLOR_YELLOW).insert(msg(" - Kick a member", COLOR_GRAY)));
        ctx.sendMessage(msg("/f claim", COLOR_YELLOW).insert(msg(" - Claim this chunk", COLOR_GRAY)));
        ctx.sendMessage(msg("/f unclaim", COLOR_YELLOW).insert(msg(" - Unclaim this chunk", COLOR_GRAY)));
        ctx.sendMessage(msg("/f home", COLOR_YELLOW).insert(msg(" - Teleport to faction home", COLOR_GRAY)));
        ctx.sendMessage(msg("/f sethome", COLOR_YELLOW).insert(msg(" - Set faction home", COLOR_GRAY)));
        ctx.sendMessage(msg("/f ally <faction>", COLOR_YELLOW).insert(msg(" - Request alliance", COLOR_GRAY)));
        ctx.sendMessage(msg("/f enemy <faction>", COLOR_YELLOW).insert(msg(" - Declare enemy", COLOR_GRAY)));
        ctx.sendMessage(msg("/f info [faction]", COLOR_YELLOW).insert(msg(" - View faction info", COLOR_GRAY)));
        ctx.sendMessage(msg("/f map", COLOR_YELLOW).insert(msg(" - View territory map", COLOR_GRAY)));
        ctx.sendMessage(msg("/f list", COLOR_YELLOW).insert(msg(" - List all factions", COLOR_GRAY)));
        ctx.sendMessage(msg("/f gui", COLOR_YELLOW).insert(msg(" - Open faction GUI", COLOR_GRAY)));
        ctx.sendMessage(msg("/f rename <name>", COLOR_YELLOW).insert(msg(" - Rename your faction", COLOR_GRAY)));
        ctx.sendMessage(msg("/f desc <text>", COLOR_YELLOW).insert(msg(" - Set faction description", COLOR_GRAY)));
        ctx.sendMessage(msg("/f color <code>", COLOR_YELLOW).insert(msg(" - Set faction color", COLOR_GRAY)));
        ctx.sendMessage(msg("/f open", COLOR_YELLOW).insert(msg(" - Allow anyone to join", COLOR_GRAY)));
        ctx.sendMessage(msg("/f close", COLOR_YELLOW).insert(msg(" - Require invite to join", COLOR_GRAY)));
        ctx.sendMessage(msg("/f stuck", COLOR_YELLOW).insert(msg(" - Escape from enemy territory", COLOR_GRAY)));
    }

    // === Create ===
    private void handleCreate(CommandContext ctx, PlayerRef player, String[] args) {
        if (!hasPermission(player, "hyperfactions.create")) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to create factions.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f create <name>", COLOR_RED)));
            return;
        }

        String name = String.join(" ", args);
        FactionManager.FactionResult result = hyperFactions.getFactionManager().createFaction(
            name, player.getUuid(), player.getUsername()
        );

        switch (result) {
            case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Faction '", COLOR_GREEN))
                .insert(msg(name, COLOR_CYAN)).insert(msg("' created!", COLOR_GREEN)));
            case ALREADY_IN_FACTION -> ctx.sendMessage(prefix().insert(msg("You are already in a faction.", COLOR_RED)));
            case NAME_TAKEN -> ctx.sendMessage(prefix().insert(msg("That faction name is already taken.", COLOR_RED)));
            case NAME_TOO_SHORT -> ctx.sendMessage(prefix().insert(msg("Faction name is too short.", COLOR_RED)));
            case NAME_TOO_LONG -> ctx.sendMessage(prefix().insert(msg("Faction name is too long.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to create faction.", COLOR_RED)));
        }
    }

    // === Disband ===
    private void handleDisband(CommandContext ctx, PlayerRef player) {
        UUID factionId = hyperFactions.getFactionManager().getPlayerFactionId(player.getUuid());
        if (factionId == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        FactionManager.FactionResult result = hyperFactions.getFactionManager().disbandFaction(
            factionId, player.getUuid()
        );

        switch (result) {
            case SUCCESS -> {
                hyperFactions.getClaimManager().unclaimAll(factionId);
                hyperFactions.getInviteManager().clearFactionInvites(factionId);
                hyperFactions.getRelationManager().clearAllRelations(factionId);
                ctx.sendMessage(prefix().insert(msg("Your faction has been disbanded.", COLOR_GREEN)));
            }
            case NOT_LEADER -> ctx.sendMessage(prefix().insert(msg("Only the faction leader can disband.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to disband faction.", COLOR_RED)));
        }
    }

    // === Invite ===
    private void handleInvite(CommandContext ctx, PlayerRef player, String[] args) {
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f invite <player>", COLOR_RED)));
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

        String targetName = args[0];
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
    private void handleAccept(CommandContext ctx, PlayerRef player, String[] args) {
        if (hyperFactions.getFactionManager().isInFaction(player.getUuid())) {
            ctx.sendMessage(prefix().insert(msg("You are already in a faction.", COLOR_RED)));
            return;
        }

        List<PendingInvite> invites = hyperFactions.getInviteManager().getPlayerInvites(player.getUuid());
        if (invites.isEmpty()) {
            ctx.sendMessage(prefix().insert(msg("You have no pending invites.", COLOR_RED)));
            return;
        }

        PendingInvite invite;
        if (args.length > 0) {
            String factionName = String.join(" ", args);
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

    // === Leave ===
    private void handleLeave(CommandContext ctx, PlayerRef player) {
        UUID factionId = hyperFactions.getFactionManager().getPlayerFactionId(player.getUuid());
        if (factionId == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        FactionManager.FactionResult result = hyperFactions.getFactionManager().removeMember(
            factionId, player.getUuid(), player.getUuid(), false
        );

        switch (result) {
            case SUCCESS -> {
                ctx.sendMessage(prefix().insert(msg("You have left your faction.", COLOR_GREEN)));
                broadcastToFaction(factionId, prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
                    .insert(msg(" has left the faction.", COLOR_RED)));
            }
            case CANNOT_KICK_LEADER -> ctx.sendMessage(prefix().insert(msg("You cannot leave as leader. Transfer leadership or disband first.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to leave faction.", COLOR_RED)));
        }
    }

    // === Kick ===
    private void handleKick(CommandContext ctx, PlayerRef player, String[] args) {
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f kick <player>", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        String targetName = args[0];
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
            }
            case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You don't have permission to kick that player.", COLOR_RED)));
            case CANNOT_KICK_LEADER -> ctx.sendMessage(prefix().insert(msg("You cannot kick the faction leader.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to kick player.", COLOR_RED)));
        }
    }

    // === Promote ===
    private void handlePromote(CommandContext ctx, PlayerRef player, String[] args) {
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f promote <player>", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        String targetName = args[0];
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
            }
            case NOT_LEADER -> ctx.sendMessage(prefix().insert(msg("Only the leader can promote members.", COLOR_RED)));
            case CANNOT_PROMOTE_LEADER -> ctx.sendMessage(prefix().insert(msg("Cannot promote further. Use /f transfer to change leader.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to promote player.", COLOR_RED)));
        }
    }

    // === Demote ===
    private void handleDemote(CommandContext ctx, PlayerRef player, String[] args) {
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f demote <player>", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        String targetName = args[0];
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
            }
            case NOT_LEADER -> ctx.sendMessage(prefix().insert(msg("Only the leader can demote members.", COLOR_RED)));
            case CANNOT_DEMOTE_MEMBER -> ctx.sendMessage(prefix().insert(msg("That player is already a Member.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to demote player.", COLOR_RED)));
        }
    }

    // === Transfer ===
    private void handleTransfer(CommandContext ctx, PlayerRef player, String[] args) {
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f transfer <player>", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        String targetName = args[0];
        FactionMember target = faction.members().values().stream()
            .filter(m -> m.username().equalsIgnoreCase(targetName))
            .findFirst().orElse(null);

        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found in your faction.", COLOR_RED)));
            return;
        }

        FactionManager.FactionResult result = hyperFactions.getFactionManager().transferLeadership(
            faction.id(), target.uuid(), player.getUuid()
        );

        switch (result) {
            case SUCCESS -> {
                ctx.sendMessage(prefix().insert(msg("Transferred leadership to ", COLOR_GREEN))
                    .insert(msg(target.username(), COLOR_YELLOW)).insert(msg("!", COLOR_GREEN)));
                broadcastToFaction(faction.id(), prefix().insert(msg(target.username(), COLOR_YELLOW))
                    .insert(msg(" is now the faction leader!", COLOR_GREEN)));
            }
            case NOT_LEADER -> ctx.sendMessage(prefix().insert(msg("Only the leader can transfer leadership.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to transfer leadership.", COLOR_RED)));
        }
    }

    // === Claim ===
    private void handleClaim(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = (int) Math.floor(pos.getX()) >> 4;
        int chunkZ = (int) Math.floor(pos.getZ()) >> 4;

        ClaimManager.ClaimResult result = hyperFactions.getClaimManager().claim(
            player.getUuid(), world.getName(), chunkX, chunkZ
        );

        switch (result) {
            case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Claimed chunk at " + chunkX + ", " + chunkZ + "!", COLOR_GREEN)));
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
    private void handleUnclaim(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = (int) Math.floor(pos.getX()) >> 4;
        int chunkZ = (int) Math.floor(pos.getZ()) >> 4;

        ClaimManager.ClaimResult result = hyperFactions.getClaimManager().unclaim(
            player.getUuid(), world.getName(), chunkX, chunkZ
        );

        switch (result) {
            case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Unclaimed chunk at " + chunkX + ", " + chunkZ + ".", COLOR_GREEN)));
            case NOT_IN_FACTION -> ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You must be an officer to unclaim land.", COLOR_RED)));
            case CHUNK_NOT_CLAIMED -> ctx.sendMessage(prefix().insert(msg("This chunk is not claimed.", COLOR_RED)));
            case NOT_YOUR_CLAIM -> ctx.sendMessage(prefix().insert(msg("Your faction doesn't own this chunk.", COLOR_RED)));
            case CANNOT_UNCLAIM_HOME -> ctx.sendMessage(prefix().insert(msg("Cannot unclaim the chunk with faction home.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to unclaim chunk.", COLOR_RED)));
        }
    }

    // === Overclaim ===
    private void handleOverclaim(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = (int) Math.floor(pos.getX()) >> 4;
        int chunkZ = (int) Math.floor(pos.getZ()) >> 4;

        ClaimManager.ClaimResult result = hyperFactions.getClaimManager().overclaim(
            player.getUuid(), world.getName(), chunkX, chunkZ
        );

        switch (result) {
            case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Overclaimed enemy territory!", COLOR_GREEN)));
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
            faction -> executeTeleport(store, ref, world, faction),
            // Message sender
            message -> ctx.sendMessage(msg(message, COLOR_YELLOW)),
            // Combat tag checker
            () -> hyperFactions.getCombatTagManager().isTagged(playerUuid)
        );

        // Handle immediate results (warmup will handle async results)
        switch (result) {
            case NOT_IN_FACTION -> ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            case NO_HOME -> ctx.sendMessage(prefix().insert(msg("Your faction has no home set.", COLOR_RED)));
            case COMBAT_TAGGED -> ctx.sendMessage(prefix().insert(msg("You cannot teleport while in combat!", COLOR_RED)));
            case ON_COOLDOWN -> {} // Message sent by TeleportManager
            case SUCCESS -> {} // Either instant teleport completed or warmup started
            default -> {}
        }
    }

    /**
     * Executes the actual teleport to faction home.
     */
    private TeleportManager.TeleportResult executeTeleport(Store<EntityStore> store, Ref<EntityStore> ref,
                                                           World currentWorld, Faction faction) {
        Faction.FactionHome home = faction.home();
        if (home == null) {
            return TeleportManager.TeleportResult.NO_HOME;
        }

        // Check if same world (cross-world teleport needs different handling)
        if (!currentWorld.getName().equals(home.world())) {
            // For now, cross-world teleport is not supported
            return TeleportManager.TeleportResult.WORLD_NOT_FOUND;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return TeleportManager.TeleportResult.WORLD_NOT_FOUND;
        }

        // Teleport the player (position only - rotation would need platform-specific handling)
        transform.setPosition(new Vector3d(home.x(), home.y(), home.z()));

        return TeleportManager.TeleportResult.SUCCESS;
    }

    // === SetHome ===
    private void handleSetHome(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = (int) Math.floor(pos.getX()) >> 4;
        int chunkZ = (int) Math.floor(pos.getZ()) >> 4;
        UUID claimOwner = hyperFactions.getClaimManager().getClaimOwner(world.getName(), chunkX, chunkZ);

        if (claimOwner == null || !claimOwner.equals(faction.id())) {
            ctx.sendMessage(prefix().insert(msg("You can only set home in your faction's territory.", COLOR_RED)));
            return;
        }

        Faction.FactionHome home = Faction.FactionHome.create(
            world.getName(), pos.getX(), pos.getY(), pos.getZ(), 0, 0, player.getUuid()
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
    private void handleAlly(CommandContext ctx, PlayerRef player, String[] args) {
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f ally <faction>", COLOR_RED)));
            return;
        }

        String factionName = String.join(" ", args);
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
    private void handleEnemy(CommandContext ctx, PlayerRef player, String[] args) {
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f enemy <faction>", COLOR_RED)));
            return;
        }

        String factionName = String.join(" ", args);
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
    private void handleNeutral(CommandContext ctx, PlayerRef player, String[] args) {
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f neutral <faction>", COLOR_RED)));
            return;
        }

        String factionName = String.join(" ", args);
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

    // === Info ===
    private void handleInfo(CommandContext ctx, PlayerRef player, String[] args) {
        Faction faction;
        if (args.length > 0) {
            String factionName = String.join(" ", args);
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
    private void handleList(CommandContext ctx, PlayerRef player) {
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
    private void handleMap(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int centerChunkX = (int) Math.floor(pos.getX()) >> 4;
        int centerChunkZ = (int) Math.floor(pos.getZ()) >> 4;

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
        if (!hasPermission(playerRef, "hyperfactions.use")) {
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

    // === Who ===
    private void handleWho(CommandContext ctx, PlayerRef player, String[] args) {
        String targetName;
        UUID targetUuid = null;

        if (args.length == 0) {
            // Show own info
            targetName = player.getUsername();
            targetUuid = player.getUuid();
        } else {
            // Look up target player
            targetName = args[0];

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
    private void handlePower(CommandContext ctx, PlayerRef player, String[] args) {
        UUID targetUuid;
        String targetName;

        if (args.length == 0) {
            // Show own power
            targetUuid = player.getUuid();
            targetName = player.getUsername();
        } else {
            // Look up target player
            targetName = args[0];
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

        PlayerPower power = hyperFactions.getPowerManager().getPlayerPower(targetUuid);
        ctx.sendMessage(msg(targetName + "'s Power:", COLOR_CYAN));
        ctx.sendMessage(msg("Current: ", COLOR_GRAY).insert(msg(String.format("%.1f/%.1f (%d%%)",
            power.power(), power.maxPower(), power.getPowerPercent()), COLOR_WHITE)));
    }

    // === Chat ===
    private void handleChat(CommandContext ctx, PlayerRef player, String[] args) {
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
        if (!hasPermission(player, "hyperfactions.admin")) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            ctx.sendMessage(msg("/f admin safezone - Create SafeZone", COLOR_YELLOW));
            ctx.sendMessage(msg("/f admin warzone - Create WarZone", COLOR_YELLOW));
            ctx.sendMessage(msg("/f admin removezone - Remove zone", COLOR_YELLOW));
            ctx.sendMessage(msg("/f admin zoneflag [flag] [true|false|clear] - Manage zone flags", COLOR_YELLOW));
            return;
        }

        String adminCmd = args[0].toLowerCase();
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = (int) Math.floor(pos.getX()) >> 4;
        int chunkZ = (int) Math.floor(pos.getZ()) >> 4;

        switch (adminCmd) {
            case "safezone" -> {
                ZoneManager.ZoneResult result = hyperFactions.getZoneManager().createZone(
                    "SafeZone", ZoneType.SAFE, world.getName(), chunkX, chunkZ, player.getUuid()
                );
                if (result == ZoneManager.ZoneResult.SUCCESS) {
                    ctx.sendMessage(prefix().insert(msg("Created SafeZone at " + chunkX + ", " + chunkZ, COLOR_GREEN)));
                } else if (result == ZoneManager.ZoneResult.CHUNK_CLAIMED) {
                    ctx.sendMessage(prefix().insert(msg("Cannot create zone: This chunk is claimed by a faction.", COLOR_RED)));
                } else if (result == ZoneManager.ZoneResult.ALREADY_EXISTS) {
                    ctx.sendMessage(prefix().insert(msg("A zone already exists at this location.", COLOR_RED)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
                }
            }
            case "warzone" -> {
                ZoneManager.ZoneResult result = hyperFactions.getZoneManager().createZone(
                    "WarZone", ZoneType.WAR, world.getName(), chunkX, chunkZ, player.getUuid()
                );
                if (result == ZoneManager.ZoneResult.SUCCESS) {
                    ctx.sendMessage(prefix().insert(msg("Created WarZone at " + chunkX + ", " + chunkZ, COLOR_GREEN)));
                } else if (result == ZoneManager.ZoneResult.CHUNK_CLAIMED) {
                    ctx.sendMessage(prefix().insert(msg("Cannot create zone: This chunk is claimed by a faction.", COLOR_RED)));
                } else if (result == ZoneManager.ZoneResult.ALREADY_EXISTS) {
                    ctx.sendMessage(prefix().insert(msg("A zone already exists at this location.", COLOR_RED)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
                }
            }
            case "removezone" -> {
                ZoneManager.ZoneResult result = hyperFactions.getZoneManager().removeZoneAt(world.getName(), chunkX, chunkZ);
                if (result == ZoneManager.ZoneResult.SUCCESS) {
                    ctx.sendMessage(prefix().insert(msg("Removed zone.", COLOR_GREEN)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("No zone found.", COLOR_RED)));
                }
            }
            case "zoneflag" -> handleZoneFlag(ctx, world.getName(), chunkX, chunkZ, Arrays.copyOfRange(args, 1, args.length));
            default -> ctx.sendMessage(prefix().insert(msg("Unknown admin command.", COLOR_RED)));
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

    // === Reload ===
    private void handleReload(CommandContext ctx, PlayerRef player) {
        if (!hasPermission(player, "hyperfactions.admin")) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        plugin.reloadConfig();
        ctx.sendMessage(prefix().insert(msg("Configuration reloaded.", COLOR_GREEN)));
    }

    // === Rename ===
    private void handleRename(CommandContext ctx, PlayerRef player, String[] args) {
        if (!hasPermission(player, "hyperfactions.rename")) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f rename <name>", COLOR_RED)));
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

        ctx.sendMessage(prefix().insert(msg("Faction renamed to ", COLOR_GREEN))
            .insert(msg(newName, COLOR_CYAN)).insert(msg("!", COLOR_GREEN)));
        broadcastToFaction(faction.id(), prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
            .insert(msg(" renamed the faction to ", COLOR_GREEN))
            .insert(msg(newName, COLOR_CYAN)));
    }

    // === Description ===
    private void handleDesc(CommandContext ctx, PlayerRef player, String[] args) {
        if (!hasPermission(player, "hyperfactions.desc")) {
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
    }

    // === Color ===
    private void handleColor(CommandContext ctx, PlayerRef player, String[] args) {
        if (!hasPermission(player, "hyperfactions.color")) {
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

        ctx.sendMessage(prefix().insert(msg("Faction color updated to ", COLOR_GREEN))
            .insert(msg("\u00A7" + colorCode + "this color", null))
            .insert(msg("!", COLOR_GREEN)));
    }

    // === Open ===
    private void handleOpen(CommandContext ctx, PlayerRef player) {
        if (!hasPermission(player, "hyperfactions.open")) {
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
    }

    // === Close ===
    private void handleClose(CommandContext ctx, PlayerRef player) {
        if (!hasPermission(player, "hyperfactions.close")) {
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
    }

    // === Stuck ===
    private void handleStuck(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        UUID playerUuid = player.getUuid();

        int chunkX = (int) Math.floor(pos.getX()) >> 4;
        int chunkZ = (int) Math.floor(pos.getZ()) >> 4;

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

            // Execute teleport
            transform.setPosition(new Vector3d(finalTargetX, finalTargetY, finalTargetZ));
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
        return HyperPermsIntegration.hasPermission(player.getUuid(), permission);
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
