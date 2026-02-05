package com.hyperfactions.command.admin;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.backup.BackupMetadata;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.importer.HyFactionsImporter;
import com.hyperfactions.importer.ImportResult;
import com.hyperfactions.manager.ConfirmationManager;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HelpFormatter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Container subcommand: /f admin
 * Routes to admin subcommands: reload, sync, zones, backup, debug, import, update, etc.
 * Opens admin GUI when called with no arguments.
 */
public class AdminSubCommand extends FactionSubCommand {

    public AdminSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("admin", "Admin commands", hyperFactions, plugin);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        if (!hasPermission(player, Permissions.ADMIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
        // parts[0] = "faction/f/hf", parts[1] = "admin", parts[2+] = admin args
        String[] args = parts.length > 2 ? Arrays.copyOfRange(parts, 2, parts.length) : new String[0];

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
            showAdminHelp(ctx);
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

        switch (adminCmd) {
            case "reload" -> handleReload(ctx, player);
            case "sync" -> handleSync(ctx, player);
            case "factions" -> {
                Player playerEntity = store.getComponent(ref, Player.getComponentType());
                if (playerEntity != null) {
                    hyperFactions.getGuiManager().openAdminFactions(playerEntity, ref, store, player);
                }
            }
            case "zones", "zone" -> handleAdminZone(ctx, store, ref, player, currentWorld, chunkX, chunkZ, Arrays.copyOfRange(args, 1, args.length));
            case "config" -> {
                Player playerEntity = store.getComponent(ref, Player.getComponentType());
                if (playerEntity != null) {
                    hyperFactions.getGuiManager().openAdminConfig(playerEntity, ref, store, player);
                }
            }
            case "backups" -> {
                Player playerEntity = store.getComponent(ref, Player.getComponentType());
                if (playerEntity != null) {
                    hyperFactions.getGuiManager().openAdminBackups(playerEntity, ref, store, player);
                }
            }
            case "safezone" -> handleSafezone(ctx, player, currentWorld, chunkX, chunkZ, args);
            case "warzone" -> handleWarzone(ctx, player, currentWorld, chunkX, chunkZ, args);
            case "removezone" -> handleRemovezone(ctx, currentWorld, chunkX, chunkZ);
            case "zoneflag" -> handleZoneFlag(ctx, currentWorld.getName(), chunkX, chunkZ, Arrays.copyOfRange(args, 1, args.length));
            case "update" -> handleAdminUpdate(ctx, player);
            case "rollback" -> handleAdminRollback(ctx, player);
            case "backup" -> handleAdminBackup(ctx, player, Arrays.copyOfRange(args, 1, args.length));
            case "import" -> handleAdminImport(ctx, player, Arrays.copyOfRange(args, 1, args.length));
            case "debug" -> handleDebug(ctx, store, ref, player, currentWorld, Arrays.copyOfRange(args, 1, args.length));
            case "decay" -> handleAdminDecay(ctx, player, Arrays.copyOfRange(args, 1, args.length));
            default -> ctx.sendMessage(prefix().insert(msg("Unknown admin command. Use /f admin help", COLOR_RED)));
        }
    }

    private void showAdminHelp(CommandContext ctx) {
        List<CommandHelp> commands = new ArrayList<>();
        commands.add(new CommandHelp("/f admin", "Open admin dashboard GUI"));
        commands.add(new CommandHelp("/f admin factions", "Manage all factions"));
        commands.add(new CommandHelp("/f admin zone", "Zone management"));
        commands.add(new CommandHelp("/f admin config", "Server configuration"));
        commands.add(new CommandHelp("/f admin backup", "Backup management"));
        commands.add(new CommandHelp("/f admin import", "Import from other plugins"));
        commands.add(new CommandHelp("/f admin update", "Check for updates"));
        commands.add(new CommandHelp("/f admin rollback", "Rollback to previous version"));
        commands.add(new CommandHelp("/f admin reload", "Reload configuration"));
        commands.add(new CommandHelp("/f admin sync", "Sync data from disk"));
        commands.add(new CommandHelp("/f admin debug", "Debug commands"));
        commands.add(new CommandHelp("/f admin decay", "Claim decay management"));
        commands.add(new CommandHelp("/f admin safezone [name]", "Create SafeZone + claim chunk"));
        commands.add(new CommandHelp("/f admin warzone [name]", "Create WarZone + claim chunk"));
        commands.add(new CommandHelp("/f admin removezone", "Unclaim chunk from zone"));
        commands.add(new CommandHelp("/f admin zoneflag <flag> <value>", "Set zone flag"));
        ctx.sendMessage(HelpFormatter.buildHelp("Admin Commands", "Server administration", commands, null));
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
            ctx.sendMessage(prefix().insert(com.hypixel.hytale.server.core.Message.join(
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

    // === Quick zone creation shortcuts ===
    private void handleSafezone(CommandContext ctx, PlayerRef player, World world, int chunkX, int chunkZ, String[] args) {
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

    private void handleWarzone(CommandContext ctx, PlayerRef player, World world, int chunkX, int chunkZ, String[] args) {
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

    private void handleRemovezone(CommandContext ctx, World world, int chunkX, int chunkZ) {
        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().unclaimChunkAt(world.getName(), chunkX, chunkZ);
        if (result == ZoneManager.ZoneResult.SUCCESS) {
            ctx.sendMessage(prefix().insert(msg("Unclaimed chunk from zone.", COLOR_GREEN)));
        } else {
            ctx.sendMessage(prefix().insert(msg("No zone chunk found at this location.", COLOR_RED)));
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
            case "rename" -> handleZoneRename(ctx, subArgs);
            case "info" -> handleZoneInfo(ctx, world.getName(), chunkX, chunkZ, subArgs);
            case "claim" -> handleZoneClaim(ctx, world.getName(), chunkX, chunkZ, subArgs);
            case "unclaim" -> handleZoneUnclaim(ctx, world.getName(), chunkX, chunkZ);
            case "radius" -> handleZoneRadius(ctx, world.getName(), chunkX, chunkZ, subArgs);
            default -> ctx.sendMessage(prefix().insert(msg("Unknown zone command. Use /f admin help", COLOR_RED)));
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

    private void handleZoneRename(CommandContext ctx, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone rename <current-name> <new-name>", COLOR_RED)));
            return;
        }

        String currentName = args[0];
        String newName = args[1];

        Zone zone = hyperFactions.getZoneManager().getZoneByName(currentName);
        if (zone == null) {
            ctx.sendMessage(prefix().insert(msg("Zone '" + currentName + "' not found.", COLOR_RED)));
            return;
        }

        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().renameZone(zone.id(), newName);
        switch (result) {
            case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Renamed zone '" + currentName + "' to '" + newName + "'", COLOR_GREEN)));
            case NAME_TAKEN -> ctx.sendMessage(prefix().insert(msg("A zone with the name '" + newName + "' already exists.", COLOR_RED)));
            case INVALID_NAME -> ctx.sendMessage(prefix().insert(msg("Invalid zone name. Must be 1-32 characters.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to rename zone: " + result, COLOR_RED)));
        }
    }

    private void handleZoneInfo(CommandContext ctx, String worldName, int chunkX, int chunkZ, String[] args) {
        Zone zone;
        if (args.length > 0) {
            zone = hyperFactions.getZoneManager().getZoneByName(args[0]);
            if (zone == null) {
                ctx.sendMessage(prefix().insert(msg("Zone '" + args[0] + "' not found.", COLOR_RED)));
                return;
            }
        } else {
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

        if (args[0].equalsIgnoreCase("clearall") || args[0].equalsIgnoreCase("resetall")) {
            ZoneManager.ZoneResult result = hyperFactions.getZoneManager().clearAllZoneFlags(zone.id());
            if (result == ZoneManager.ZoneResult.SUCCESS) {
                ctx.sendMessage(prefix().insert(msg("Cleared all custom flags for '" + zone.name() + "' - now using zone type defaults.", COLOR_GREEN)));
            } else {
                ctx.sendMessage(prefix().insert(msg("Failed to clear flags.", COLOR_RED)));
            }
            return;
        }

        String flagName = args[0].toLowerCase();
        if (!ZoneFlags.isValidFlag(flagName)) {
            ctx.sendMessage(prefix().insert(msg("Invalid flag: " + flagName, COLOR_RED)));
            ctx.sendMessage(msg("Valid flags: " + String.join(", ", ZoneFlags.ALL_FLAGS), COLOR_GRAY));
            return;
        }

        if (args.length == 1) {
            boolean effectiveValue = zone.getEffectiveFlag(flagName);
            boolean isCustom = zone.hasFlagSet(flagName);
            ctx.sendMessage(prefix().insert(msg("Flag '" + flagName + "' = " + effectiveValue, effectiveValue ? COLOR_GREEN : COLOR_RED))
                .insert(msg(isCustom ? " (custom)" : " (default)", COLOR_GRAY)));
            return;
        }

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
        String currentVersion = updateChecker.getCurrentVersion();

        // Step 1: Create a data backup before downloading the update
        player.sendMessage(prefix().insert(msg("Creating pre-update backup...", COLOR_YELLOW)));

        hyperFactions.getBackupManager().createBackup(BackupType.MANUAL, "pre-update-" + currentVersion, player.getUuid())
            .thenCompose(backupResult -> {
                if (backupResult instanceof BackupManager.BackupResult.Success success) {
                    player.sendMessage(prefix().insert(msg("Backup created: " + success.metadata().name(), COLOR_GREEN)));
                } else if (backupResult instanceof BackupManager.BackupResult.Failure failure) {
                    player.sendMessage(prefix().insert(msg("Warning: Backup failed - " + failure.error(), COLOR_YELLOW)));
                    player.sendMessage(msg("  Continuing with update anyway...", COLOR_GRAY));
                }

                // Step 2: Download the update
                player.sendMessage(prefix().insert(msg("Downloading HyperFactions v" + info.version() + "...", COLOR_YELLOW)));
                return updateChecker.downloadUpdate(info);
            })
            .thenAccept(path -> {
                if (path == null) {
                    player.sendMessage(prefix().insert(msg("Failed to download update. Check server logs.", COLOR_RED)));
                } else {
                    player.sendMessage(prefix().insert(msg("Update downloaded successfully!", COLOR_GREEN)));
                    player.sendMessage(msg("  File: " + path.getFileName(), COLOR_GRAY));

                    // Step 3: Clean up old JAR backups (keep only the version we just upgraded from)
                    int cleaned = updateChecker.cleanupOldBackups(currentVersion);
                    if (cleaned > 0) {
                        player.sendMessage(msg("  Cleanup: Removed " + cleaned + " old backup(s)", COLOR_GRAY));
                    }
                    player.sendMessage(msg("  Kept: HyperFactions-" + currentVersion + ".jar.backup (for rollback)", COLOR_GRAY));

                    // Step 4: Create rollback marker (safe to rollback until server restarts)
                    updateChecker.createRollbackMarker(currentVersion, info.version());

                    player.sendMessage(msg("  Restart the server to apply the update.", COLOR_YELLOW));
                    player.sendMessage(msg("  Use /f admin rollback to revert before restarting.", COLOR_GRAY));

                    // Run manual backup rotation to respect retention limits
                    hyperFactions.getBackupManager().performRotation();
                }
            });
    }

    // === Admin Rollback ===
    private void handleAdminRollback(CommandContext ctx, PlayerRef player) {
        var updateChecker = hyperFactions.getUpdateChecker();
        if (updateChecker == null) {
            ctx.sendMessage(prefix().insert(msg("Update checker is not available.", COLOR_RED)));
            return;
        }

        // Check if there's a backup to rollback to
        Path latestBackup = updateChecker.findLatestBackup();
        if (latestBackup == null) {
            ctx.sendMessage(prefix().insert(msg("No backup JAR found to rollback to.", COLOR_RED)));
            return;
        }

        String backupVersion = latestBackup.getFileName().toString()
                .replace("HyperFactions-", "")
                .replace(".jar.backup", "");

        // Check if rollback is safe (server hasn't restarted since update)
        if (!updateChecker.isRollbackSafe()) {
            // Server has restarted - migrations may have run
            ctx.sendMessage(prefix().insert(msg("Cannot automatically rollback!", COLOR_RED)));
            ctx.sendMessage(msg("", COLOR_GRAY));
            ctx.sendMessage(msg("The server has been restarted since the last update.", COLOR_YELLOW));
            ctx.sendMessage(msg("Config/data migrations may have been applied.", COLOR_YELLOW));
            ctx.sendMessage(msg("", COLOR_GRAY));
            ctx.sendMessage(msg("To rollback safely, you must:", COLOR_WHITE));
            ctx.sendMessage(msg("  1. Stop the server", COLOR_GRAY));
            ctx.sendMessage(msg("  2. Restore from the pre-update backup:", COLOR_GRAY));
            ctx.sendMessage(msg("     /f admin backup restore <backup-name>", COLOR_CYAN));
            ctx.sendMessage(msg("  3. Manually replace the JAR file:", COLOR_GRAY));
            ctx.sendMessage(msg("     " + latestBackup.getFileName() + " -> HyperFactions-" + backupVersion + ".jar", COLOR_CYAN));
            ctx.sendMessage(msg("  4. Restart the server", COLOR_GRAY));
            ctx.sendMessage(msg("", COLOR_GRAY));
            ctx.sendMessage(msg("Use /f admin backup list to find the pre-update backup.", COLOR_YELLOW));
            return;
        }

        // Get rollback info
        var rollbackInfo = updateChecker.getRollbackInfo();
        if (rollbackInfo != null) {
            ctx.sendMessage(prefix().insert(msg("Rolling back update...", COLOR_YELLOW)));
            ctx.sendMessage(msg("  From: v" + rollbackInfo.toVersion() + " (new)", COLOR_GRAY));
            ctx.sendMessage(msg("  To: v" + rollbackInfo.fromVersion() + " (previous)", COLOR_GRAY));
        } else {
            ctx.sendMessage(prefix().insert(msg("Rolling back to v" + backupVersion + "...", COLOR_YELLOW)));
        }

        // Perform the rollback
        var result = updateChecker.performRollback();

        if (result.success()) {
            ctx.sendMessage(prefix().insert(msg("Rollback successful!", COLOR_GREEN)));
            ctx.sendMessage(msg("  Restored: HyperFactions-" + result.restoredVersion() + ".jar", COLOR_GRAY));
            if (result.removedVersion() != null) {
                ctx.sendMessage(msg("  Removed: HyperFactions-" + result.removedVersion() + ".jar", COLOR_GRAY));
            }
            ctx.sendMessage(msg("  Restart the server to apply the rollback.", COLOR_YELLOW));
        } else {
            ctx.sendMessage(prefix().insert(msg("Rollback failed: " + result.errorMessage(), COLOR_RED)));
        }
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
        // Find backup by name
        BackupMetadata backup = hyperFactions.getBackupManager().listBackups().stream()
            .filter(b -> b.name().equalsIgnoreCase(backupName))
            .findFirst()
            .orElse(null);
        if (backup == null) {
            ctx.sendMessage(prefix().insert(msg("Backup '" + backupName + "' not found.", COLOR_RED)));
            return;
        }

        ConfirmationManager confirmManager = hyperFactions.getConfirmationManager();
        ConfirmationManager.ConfirmationResult confirmResult = confirmManager.checkOrCreate(
            player.getUuid(), ConfirmationManager.ConfirmationType.RESTORE_BACKUP, null
        );

        switch (confirmResult) {
            case NEEDS_CONFIRMATION, EXPIRED_RECREATED -> {
                ctx.sendMessage(prefix().insert(msg("WARNING: Restoring backup will overwrite current data!", COLOR_RED)));
                ctx.sendMessage(prefix().insert(msg("Type ", COLOR_YELLOW))
                    .insert(msg("/f admin backup restore " + backupName, COLOR_WHITE))
                    .insert(msg(" again within " + confirmManager.getTimeoutSeconds() + " seconds to confirm.", COLOR_YELLOW)));
            }
            case CONFIRMED -> {
                ctx.sendMessage(prefix().insert(msg("Restoring backup...", COLOR_YELLOW)));
                hyperFactions.getBackupManager().restoreBackup(backup.name())
                    .thenAccept(result -> {
                        if (result instanceof BackupManager.RestoreResult.Success) {
                            player.sendMessage(prefix().insert(msg("Backup restored successfully! Data reloaded.", COLOR_GREEN)));
                        } else if (result instanceof BackupManager.RestoreResult.Failure failure) {
                            player.sendMessage(prefix().insert(msg("Restore failed: " + failure.error(), COLOR_RED)));
                        }
                    });
            }
            case DIFFERENT_ACTION -> {
                ctx.sendMessage(prefix().insert(msg("Previous confirmation cancelled. Type again to confirm restore.", COLOR_YELLOW)));
            }
        }
    }

    private void handleBackupDelete(CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin backup delete <name>", COLOR_RED)));
            return;
        }

        String backupName = args[0];
        // Find backup by name
        BackupMetadata backup = hyperFactions.getBackupManager().listBackups().stream()
            .filter(b -> b.name().equalsIgnoreCase(backupName))
            .findFirst()
            .orElse(null);
        if (backup == null) {
            ctx.sendMessage(prefix().insert(msg("Backup '" + backupName + "' not found.", COLOR_RED)));
            return;
        }

        hyperFactions.getBackupManager().deleteBackup(backup.name())
            .thenAccept(deleted -> {
                if (deleted) {
                    ctx.sendMessage(prefix().insert(msg("Deleted backup '" + backupName + "'", COLOR_GREEN)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Failed to delete backup.", COLOR_RED)));
                }
            });
    }

    // === Admin Import Commands ===
    private void handleAdminImport(CommandContext ctx, PlayerRef player, String[] args) {
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
        commands.add(new CommandHelp("/f admin import hyfactions <path>", "Import from HyFactions mod"));
        commands.add(new CommandHelp("  Flags: --dry-run, --no-claims, --no-relations", "Options"));
        ctx.sendMessage(HelpFormatter.buildHelp("Import Commands", "Migrate from other faction plugins", commands, null));
    }

    private void handleImportHyFactions(CommandContext ctx, PlayerRef player, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin import hyfactions <path> [flags]", COLOR_RED)));
            ctx.sendMessage(msg("  path: Path to HyFactions data folder", COLOR_GRAY));
            ctx.sendMessage(msg("  flags: --dry-run, --no-claims, --no-relations", COLOR_GRAY));
            return;
        }

        String pathStr = args[0];
        Path dataPath = Paths.get(pathStr);

        boolean dryRun = false;
        boolean skipClaims = false;
        boolean skipRelations = false;

        for (int i = 1; i < args.length; i++) {
            String flag = args[i].toLowerCase();
            switch (flag) {
                case "--dry-run", "-n" -> dryRun = true;
                case "--no-claims" -> skipClaims = true;
                case "--no-relations" -> skipRelations = true;
            }
        }

        ctx.sendMessage(prefix().insert(msg("Importing from HyFactions...", COLOR_YELLOW)));
        if (dryRun) {
            ctx.sendMessage(msg("  (Dry run - no changes will be made)", COLOR_GRAY));
        }

        // Create importer with managers
        HyFactionsImporter importer = new HyFactionsImporter(
            hyperFactions.getFactionManager(),
            hyperFactions.getClaimManager(),
            hyperFactions.getZoneManager(),
            hyperFactions.getPowerManager(),
            hyperFactions.getBackupManager()
        );

        // Configure importer
        importer.setDryRun(dryRun);
        // Note: skipClaims and skipRelations are not currently supported by HyFactionsImporter

        final boolean finalDryRun = dryRun;
        // Run import asynchronously
        CompletableFuture.supplyAsync(() -> importer.importFrom(dataPath))
            .thenAccept(result -> {
                if (!result.hasErrors()) {
                    player.sendMessage(prefix().insert(msg("Import " + (finalDryRun ? "simulation " : "") + "complete!", COLOR_GREEN)));
                    player.sendMessage(msg("  Factions: " + result.factionsImported(), COLOR_GRAY));
                    player.sendMessage(msg("  Claims: " + result.claimsImported(), COLOR_GRAY));
                    player.sendMessage(msg("  Zones: " + result.zonesCreated(), COLOR_GRAY));
                    player.sendMessage(msg("  Players with power: " + result.playersWithPower(), COLOR_GRAY));
                    if (result.hasWarnings()) {
                        player.sendMessage(msg("  Warnings: " + result.warnings().size() + " (check logs)", COLOR_YELLOW));
                    }
                } else {
                    player.sendMessage(prefix().insert(msg("Import failed with errors:", COLOR_RED)));
                    for (String error : result.errors()) {
                        player.sendMessage(msg("  - " + error, COLOR_RED));
                    }
                }
            });
    }

    // === Debug Commands ===
    private void handleDebug(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                            PlayerRef player, World world, String[] args) {
        if (!hasPermission(player, Permissions.ADMIN_DEBUG)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to use debug commands.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            showDebugHelp(ctx);
            return;
        }

        String subCmd = args[0].toLowerCase();
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (subCmd) {
            case "toggle" -> handleDebugToggle(ctx, subArgs);
            case "status" -> handleDebugStatus(ctx);
            case "power" -> handleDebugPower(ctx, subArgs);
            case "claim" -> handleDebugClaim(ctx, store, ref, world, subArgs);
            case "protection" -> handleDebugProtection(ctx, store, ref, world, subArgs);
            case "combat" -> handleDebugCombat(ctx, subArgs);
            case "relation" -> handleDebugRelation(ctx, subArgs);
            case "help", "?" -> showDebugHelp(ctx);
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown debug command: " + subCmd, COLOR_RED)));
                showDebugHelp(ctx);
            }
        }
    }

    private void showDebugHelp(CommandContext ctx) {
        List<CommandHelp> commands = new ArrayList<>();
        commands.add(new CommandHelp("/f admin debug toggle <category> [on|off]", "Toggle debug logging"));
        commands.add(new CommandHelp("/f admin debug status", "Show debug status"));
        commands.add(new CommandHelp("/f admin debug power <player>", "Show power details"));
        commands.add(new CommandHelp("/f admin debug claim [x z]", "Show claim info"));
        commands.add(new CommandHelp("/f admin debug protection <player>", "Show protection info"));
        commands.add(new CommandHelp("/f admin debug combat <player>", "Show combat tag status"));
        commands.add(new CommandHelp("/f admin debug relation <faction1> <faction2>", "Show relation info"));
        ctx.sendMessage(HelpFormatter.buildHelp("Debug Commands", "Diagnostics and troubleshooting", commands, null));
    }

    private void handleDebugToggle(CommandContext ctx, String[] args) {
        var debugConfig = ConfigManager.get().debug();

        if (args.length == 0) {
            // Show current status
            ctx.sendMessage(msg("=== Debug Logging Status ===", COLOR_CYAN).bold(true));
            ctx.sendMessage(msg("Categories:", COLOR_GRAY));
            ctx.sendMessage(msg("  power: ", COLOR_WHITE).insert(msg(debugConfig.isPower() ? "ON" : "OFF", debugConfig.isPower() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  claim: ", COLOR_WHITE).insert(msg(debugConfig.isClaim() ? "ON" : "OFF", debugConfig.isClaim() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  combat: ", COLOR_WHITE).insert(msg(debugConfig.isCombat() ? "ON" : "OFF", debugConfig.isCombat() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  protection: ", COLOR_WHITE).insert(msg(debugConfig.isProtection() ? "ON" : "OFF", debugConfig.isProtection() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  relation: ", COLOR_WHITE).insert(msg(debugConfig.isRelation() ? "ON" : "OFF", debugConfig.isRelation() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  territory: ", COLOR_WHITE).insert(msg(debugConfig.isTerritory() ? "ON" : "OFF", debugConfig.isTerritory() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  worldmap: ", COLOR_WHITE).insert(msg(debugConfig.isWorldmap() ? "ON" : "OFF", debugConfig.isWorldmap() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  interaction: ", COLOR_WHITE).insert(msg(debugConfig.isInteraction() ? "ON" : "OFF", debugConfig.isInteraction() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  mixin: ", COLOR_WHITE).insert(msg(debugConfig.isMixin() ? "ON" : "OFF", debugConfig.isMixin() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  spawning: ", COLOR_WHITE).insert(msg(debugConfig.isSpawning() ? "ON" : "OFF", debugConfig.isSpawning() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("Usage: /f admin debug toggle <category|all> [on|off]", COLOR_GRAY));
            return;
        }

        String category = args[0].toLowerCase();

        // Handle "all" category
        if (category.equals("all")) {
            boolean enable = args.length > 1 ? args[1].equalsIgnoreCase("on") : !debugConfig.isEnabledByDefault();
            if (enable) {
                debugConfig.enableAll();
                ctx.sendMessage(prefix().insert(msg("All debug categories enabled.", COLOR_GREEN)));
            } else {
                debugConfig.disableAll();
                ctx.sendMessage(prefix().insert(msg("All debug categories disabled.", COLOR_GREEN)));
            }
            debugConfig.save();
            return;
        }

        // Get current value and determine new value
        boolean currentValue;
        switch (category) {
            case "power" -> currentValue = debugConfig.isPower();
            case "claim" -> currentValue = debugConfig.isClaim();
            case "combat" -> currentValue = debugConfig.isCombat();
            case "protection" -> currentValue = debugConfig.isProtection();
            case "relation" -> currentValue = debugConfig.isRelation();
            case "territory" -> currentValue = debugConfig.isTerritory();
            case "worldmap", "map" -> currentValue = debugConfig.isWorldmap();
            case "interaction" -> currentValue = debugConfig.isInteraction();
            case "mixin" -> currentValue = debugConfig.isMixin();
            case "spawning" -> currentValue = debugConfig.isSpawning();
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown category: " + category, COLOR_RED)));
                ctx.sendMessage(msg("Valid categories: power, claim, combat, protection, relation, territory, worldmap, interaction, mixin, spawning, all", COLOR_GRAY));
                return;
            }
        }

        // Determine new value: if explicit on/off provided use that, otherwise toggle
        boolean newValue = args.length > 1
            ? args[1].equalsIgnoreCase("on")
            : !currentValue;

        // Apply the change
        switch (category) {
            case "power" -> debugConfig.setPower(newValue);
            case "claim" -> debugConfig.setClaim(newValue);
            case "combat" -> debugConfig.setCombat(newValue);
            case "protection" -> debugConfig.setProtection(newValue);
            case "relation" -> debugConfig.setRelation(newValue);
            case "territory" -> debugConfig.setTerritory(newValue);
            case "worldmap", "map" -> debugConfig.setWorldmap(newValue);
            case "interaction" -> debugConfig.setInteraction(newValue);
            case "mixin" -> debugConfig.setMixin(newValue);
            case "spawning" -> debugConfig.setSpawning(newValue);
        }

        // Save to persist the change
        debugConfig.save();

        ctx.sendMessage(prefix().insert(
            msg("Debug category '", COLOR_GREEN)
                .insert(msg(category, COLOR_CYAN))
                .insert(msg("' set to ", COLOR_GREEN))
                .insert(msg(newValue ? "ON" : "OFF", newValue ? COLOR_GREEN : COLOR_RED))
                .insert(msg(" (saved)", COLOR_GRAY))
        ));
    }

    private void handleDebugStatus(CommandContext ctx) {
        var debugConfig = ConfigManager.get().debug();

        ctx.sendMessage(msg("=== HyperFactions Debug Status ===", COLOR_CYAN).bold(true));

        // Data counts
        ctx.sendMessage(msg("Data:", COLOR_GRAY));
        ctx.sendMessage(msg("  Factions: " + hyperFactions.getFactionManager().getAllFactions().size(), COLOR_WHITE));
        ctx.sendMessage(msg("  Zones: " + hyperFactions.getZoneManager().getAllZones().size(), COLOR_WHITE));
        ctx.sendMessage(msg("  Claims: " + hyperFactions.getClaimManager().getTotalClaimCount(), COLOR_WHITE));

        // Debug logging status
        ctx.sendMessage(msg("Debug Logging:", COLOR_GRAY));
        ctx.sendMessage(msg("  power: ", COLOR_WHITE).insert(msg(debugConfig.isPower() ? "ON" : "OFF", debugConfig.isPower() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  claim: ", COLOR_WHITE).insert(msg(debugConfig.isClaim() ? "ON" : "OFF", debugConfig.isClaim() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  combat: ", COLOR_WHITE).insert(msg(debugConfig.isCombat() ? "ON" : "OFF", debugConfig.isCombat() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  protection: ", COLOR_WHITE).insert(msg(debugConfig.isProtection() ? "ON" : "OFF", debugConfig.isProtection() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  relation: ", COLOR_WHITE).insert(msg(debugConfig.isRelation() ? "ON" : "OFF", debugConfig.isRelation() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  territory: ", COLOR_WHITE).insert(msg(debugConfig.isTerritory() ? "ON" : "OFF", debugConfig.isTerritory() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  worldmap: ", COLOR_WHITE).insert(msg(debugConfig.isWorldmap() ? "ON" : "OFF", debugConfig.isWorldmap() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  interaction: ", COLOR_WHITE).insert(msg(debugConfig.isInteraction() ? "ON" : "OFF", debugConfig.isInteraction() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  mixin: ", COLOR_WHITE).insert(msg(debugConfig.isMixin() ? "ON" : "OFF", debugConfig.isMixin() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  spawning: ", COLOR_WHITE).insert(msg(debugConfig.isSpawning() ? "ON" : "OFF", debugConfig.isSpawning() ? COLOR_GREEN : COLOR_RED)));
    }

    private void handleDebugPower(CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin debug power <player>", COLOR_RED)));
            return;
        }
        ctx.sendMessage(prefix().insert(msg("Debug power info not yet implemented.", COLOR_YELLOW)));
    }

    private void handleDebugClaim(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, World world, String[] args) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

        if (args.length >= 2) {
            try {
                chunkX = Integer.parseInt(args[0]);
                chunkZ = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                ctx.sendMessage(prefix().insert(msg("Invalid chunk coordinates.", COLOR_RED)));
                return;
            }
        }

        ctx.sendMessage(msg("=== Claim Debug (" + chunkX + ", " + chunkZ + ") ===", COLOR_CYAN).bold(true));
        UUID owner = hyperFactions.getClaimManager().getClaimOwner(world.getName(), chunkX, chunkZ);
        if (owner != null) {
            var faction = hyperFactions.getFactionManager().getFaction(owner);
            ctx.sendMessage(msg("Owner: " + (faction != null ? faction.name() : owner.toString()), COLOR_WHITE));
        } else {
            ctx.sendMessage(msg("Owner: None (wilderness)", COLOR_GRAY));
        }

        Zone zone = hyperFactions.getZoneManager().getZone(world.getName(), chunkX, chunkZ);
        if (zone != null) {
            ctx.sendMessage(msg("Zone: " + zone.name() + " (" + zone.type().getDisplayName() + ")", COLOR_WHITE));
        }
    }

    private void handleDebugProtection(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, World world, String[] args) {
        ctx.sendMessage(prefix().insert(msg("Debug protection info not yet implemented.", COLOR_YELLOW)));
    }

    private void handleDebugCombat(CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin debug combat <player>", COLOR_RED)));
            return;
        }
        ctx.sendMessage(prefix().insert(msg("Debug combat info not yet implemented.", COLOR_YELLOW)));
    }

    private void handleDebugRelation(CommandContext ctx, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin debug relation <faction1> <faction2>", COLOR_RED)));
            return;
        }
        ctx.sendMessage(prefix().insert(msg("Debug relation info not yet implemented.", COLOR_YELLOW)));
    }

    // === Admin Decay Commands ===
    private void handleAdminDecay(CommandContext ctx, PlayerRef player, String[] args) {
        if (!hasPermission(player, Permissions.ADMIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            showDecayStatus(ctx);
            return;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "run", "trigger" -> handleDecayRun(ctx);
            case "check" -> handleDecayCheck(ctx, Arrays.copyOfRange(args, 1, args.length));
            case "status" -> showDecayStatus(ctx);
            case "help", "?" -> showDecayHelp(ctx);
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown decay command: " + subCmd, COLOR_RED)));
                showDecayHelp(ctx);
            }
        }
    }

    private void showDecayHelp(CommandContext ctx) {
        List<CommandHelp> commands = new ArrayList<>();
        commands.add(new CommandHelp("/f admin decay", "Show decay status"));
        commands.add(new CommandHelp("/f admin decay run", "Manually trigger claim decay"));
        commands.add(new CommandHelp("/f admin decay check <faction>", "Check faction decay status"));
        ctx.sendMessage(HelpFormatter.buildHelp("Claim Decay", "Auto-removes claims from inactive factions", commands, null));
    }

    private void showDecayStatus(CommandContext ctx) {
        ConfigManager config = ConfigManager.get();

        ctx.sendMessage(msg("=== Claim Decay Status ===", COLOR_CYAN).bold(true));
        ctx.sendMessage(msg("Enabled: ", COLOR_GRAY)
            .insert(msg(config.isDecayEnabled() ? "Yes" : "No", config.isDecayEnabled() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("Inactivity Threshold: ", COLOR_GRAY)
            .insert(msg(config.getDecayDaysInactive() + " days", COLOR_WHITE)));
        ctx.sendMessage(msg("Check Interval: ", COLOR_GRAY)
            .insert(msg("Every hour", COLOR_WHITE)));

        // Count factions at risk
        if (config.isDecayEnabled()) {
            int atRisk = 0;
            int totalWithClaims = 0;
            for (var faction : hyperFactions.getFactionManager().getAllFactions()) {
                if (faction.getClaimCount() > 0) {
                    totalWithClaims++;
                    int daysUntil = hyperFactions.getClaimManager().getDaysUntilDecay(faction.id());
                    if (daysUntil >= 0 && daysUntil <= 7) {
                        atRisk++;
                    }
                }
            }
            ctx.sendMessage(msg("Factions with claims: ", COLOR_GRAY)
                .insert(msg(String.valueOf(totalWithClaims), COLOR_WHITE)));
            ctx.sendMessage(msg("At risk (≤7 days): ", COLOR_GRAY)
                .insert(msg(String.valueOf(atRisk), atRisk > 0 ? COLOR_YELLOW : COLOR_GREEN)));
        }
    }

    private void handleDecayRun(CommandContext ctx) {
        ConfigManager config = ConfigManager.get();

        if (!config.isDecayEnabled()) {
            ctx.sendMessage(prefix().insert(msg("Claim decay is disabled in config.", COLOR_YELLOW)));
            ctx.sendMessage(msg("Set claims.decayEnabled to true to enable.", COLOR_GRAY));
            return;
        }

        ctx.sendMessage(prefix().insert(msg("Running claim decay check...", COLOR_YELLOW)));

        // Run decay on separate thread to avoid blocking
        CompletableFuture.runAsync(() -> {
            try {
                hyperFactions.getClaimManager().tickClaimDecay();
                ctx.sendMessage(prefix().insert(msg("Claim decay check complete. Check console for details.", COLOR_GREEN)));
            } catch (Exception e) {
                ctx.sendMessage(prefix().insert(msg("Error during decay: " + e.getMessage(), COLOR_RED)));
            }
        });
    }

    private void handleDecayCheck(CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin decay check <faction>", COLOR_RED)));
            return;
        }

        String factionName = args[0];
        var faction = hyperFactions.getFactionManager().getFactionByName(factionName);
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
            return;
        }

        ConfigManager config = ConfigManager.get();

        ctx.sendMessage(msg("=== Decay Check: " + faction.name() + " ===", COLOR_CYAN).bold(true));
        ctx.sendMessage(msg("Claims: ", COLOR_GRAY).insert(msg(String.valueOf(faction.getClaimCount()), COLOR_WHITE)));

        if (faction.getClaimCount() == 0) {
            ctx.sendMessage(msg("No claims to decay.", COLOR_GRAY));
            return;
        }

        if (!config.isDecayEnabled()) {
            ctx.sendMessage(msg("Decay Status: ", COLOR_GRAY).insert(msg("Disabled globally", COLOR_YELLOW)));
            return;
        }

        // Find most recent login
        long mostRecentLogin = 0;
        String mostRecentPlayer = "Unknown";
        for (var member : faction.members().values()) {
            if (member.lastOnline() > mostRecentLogin) {
                mostRecentLogin = member.lastOnline();
                mostRecentPlayer = member.username();
            }
        }

        long daysSinceActive = (System.currentTimeMillis() - mostRecentLogin) / (24L * 60 * 60 * 1000);
        int daysUntilDecay = hyperFactions.getClaimManager().getDaysUntilDecay(faction.id());
        boolean isInactive = hyperFactions.getClaimManager().isFactionInactive(faction.id());

        ctx.sendMessage(msg("Last Active: ", COLOR_GRAY)
            .insert(msg(daysSinceActive + " days ago", COLOR_WHITE))
            .insert(msg(" (" + mostRecentPlayer + ")", COLOR_GRAY)));
        ctx.sendMessage(msg("Threshold: ", COLOR_GRAY)
            .insert(msg(config.getDecayDaysInactive() + " days", COLOR_WHITE)));

        if (isInactive) {
            ctx.sendMessage(msg("Status: ", COLOR_GRAY)
                .insert(msg("INACTIVE - Claims will decay on next check!", COLOR_RED).bold(true)));
        } else if (daysUntilDecay <= 7) {
            ctx.sendMessage(msg("Status: ", COLOR_GRAY)
                .insert(msg("AT RISK - " + daysUntilDecay + " days until decay", COLOR_YELLOW)));
        } else {
            ctx.sendMessage(msg("Status: ", COLOR_GRAY)
                .insert(msg("Active - " + daysUntilDecay + " days until decay", COLOR_GREEN)));
        }
    }
}
