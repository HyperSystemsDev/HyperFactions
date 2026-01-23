package com.hyperfactions.command;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.data.*;
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
        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        if (!faction.hasHome()) {
            ctx.sendMessage(prefix().insert(msg("Your faction has no home set.", COLOR_RED)));
            return;
        }

        if (hyperFactions.getCombatTagManager().isTagged(player.getUuid())) {
            ctx.sendMessage(prefix().insert(msg("You cannot teleport while in combat!", COLOR_RED)));
            return;
        }

        ctx.sendMessage(prefix().insert(msg("Teleporting to faction home...", COLOR_YELLOW)));
        // Actual teleport would need full implementation with warmup
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
        ctx.sendMessage(prefix().insert(msg("Player lookup not yet implemented.", COLOR_GRAY)));
    }

    // === Power ===
    private void handlePower(CommandContext ctx, PlayerRef player, String[] args) {
        UUID targetUuid = player.getUuid();
        String targetName = player.getUsername();

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
            ctx.sendMessage(msg("/f admin safezone - Create SafeZone", COLOR_RED));
            ctx.sendMessage(msg("/f admin warzone - Create WarZone", COLOR_RED));
            ctx.sendMessage(msg("/f admin removezone - Remove zone", COLOR_RED));
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
            default -> ctx.sendMessage(prefix().insert(msg("Unknown admin command.", COLOR_RED)));
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
