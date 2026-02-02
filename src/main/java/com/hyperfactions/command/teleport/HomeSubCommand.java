package com.hyperfactions.command.teleport;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.TeleportManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Subcommand: /f home
 * Teleports to the faction home.
 */
public class HomeSubCommand extends FactionSubCommand {

    public HomeSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("home", "Teleport to faction home", hyperFactions, plugin);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

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
            currentWorld.getName(), pos.getX(), pos.getY(), pos.getZ()
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
            targetFaction -> executeTeleport(store, ref, currentWorld, targetFaction),
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
}
