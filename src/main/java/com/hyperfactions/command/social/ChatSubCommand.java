package com.hyperfactions.command.social;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.data.Faction;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f chat <message>
 * Sends a message to faction chat.
 * Aliases: c
 */
public class ChatSubCommand extends FactionSubCommand {

    public ChatSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("chat", "Send faction chat message", hyperFactions, plugin);
        addAliases("c");
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        if (!hasPermission(player, Permissions.CHAT_FACTION)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to use faction chat.", COLOR_RED)));
            return;
        }

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("You are not in a faction.", COLOR_RED)));
            return;
        }

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
        // parts[0] = "faction/f/hf", parts[1] = "chat/c", parts[2+] = message
        String[] args = parts.length > 2 ? java.util.Arrays.copyOfRange(parts, 2, parts.length) : new String[0];

        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f c <message>", COLOR_RED)));
            return;
        }

        String message = String.join(" ", args);
        com.hypixel.hytale.server.core.Message formatted = msg("[Faction] ", COLOR_CYAN)
            .insert(msg(player.getUsername(), COLOR_YELLOW))
            .insert(msg(": " + message, COLOR_WHITE));
        broadcastToFaction(faction.id(), formatted);
    }
}
