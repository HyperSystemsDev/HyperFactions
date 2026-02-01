package com.hyperfactions.gui.nav;

import com.hyperfactions.config.HyperFactionsConfig;
import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.faction.FactionPageRegistry;
import com.hyperfactions.gui.shared.data.NavAwareData;
import com.hyperfactions.integration.HyperPermsIntegration;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Helper class for building and handling the shared navigation bar component.
 * Follows AdminUI pattern exactly for nav bar setup.
 */
public final class NavBarHelper {

    private NavBarHelper() {
        // Static utility class
    }

    /**
     * Sets up the navigation bar in a page.
     * Follows AdminUI pattern exactly with indexed selectors.
     *
     * @param playerRef   The player viewing the page
     * @param hasFaction  Whether the player has a faction
     * @param currentPage The ID of the current page (to highlight it)
     * @param cmd         The UI command builder
     * @param events      The UI event builder
     */
    public static void setupBar(
            @NotNull PlayerRef playerRef,
            boolean hasFaction,
            @NotNull String currentPage,
            @NotNull UICommandBuilder cmd,
            @NotNull UIEventBuilder events
    ) {
        // Get accessible nav bar entries
        List<FactionPageRegistry.Entry> entries = FactionPageRegistry.getInstance()
                .getAccessibleNavBarEntries(playerRef, hasFaction);

        if (entries.isEmpty()) {
            return;
        }

        // Set the nav bar title from config (default: "HyperFactions")
        String guiTitle = HyperFactionsConfig.get().getGuiTitle();
        cmd.set("#HyperFactionsNavBar #NavBarTitle #NavBarTitleLabel.Text", guiTitle);

        // Following AdminUI pattern exactly:
        // 1. Create #NavCards container inline inside #NavBarButtons
        cmd.appendInline("#HyperFactionsNavBar #NavBarButtons", "Group #NavCards { LayoutMode: Left; }");

        // 2. For each entry, append button and use indexed selector
        int index = 0;
        for (FactionPageRegistry.Entry entry : entries) {
            // Append navigation button template
            cmd.append("#NavCards", "HyperFactions/nav/nav_button.ui");

            // Set button text using indexed selector (AdminUI pattern)
            cmd.set("#NavBarButtons #NavCards[" + index + "] #NavActionButton.Text", entry.displayName());

            // Bind click event using indexed selector (AdminUI pattern)
            // Set both button and navBar fields so handler doesn't early-return on null button
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#NavBarButtons #NavCards[" + index + "] #NavActionButton",
                    EventData.of("Button", "Nav").append("NavBar", entry.id())
            );

            index++;
        }
    }

    /**
     * Handles navigation events from the nav bar.
     * Works with any data class implementing NavAwareData.
     *
     * @param data       The event data (must implement NavAwareData)
     * @param player     The player entity
     * @param ref        Entity reference
     * @param store      Entity store
     * @param playerRef  Player reference
     * @param faction    The player's faction (may be null)
     * @param guiManager The GUI manager
     * @return true if the event was handled, false otherwise
     */
    public static boolean handleNavEvent(
            @NotNull NavAwareData data,
            @NotNull Player player,
            @NotNull Ref<EntityStore> ref,
            @NotNull Store<EntityStore> store,
            @NotNull PlayerRef playerRef,
            @Nullable Faction faction,
            @NotNull GuiManager guiManager
    ) {
        // Check if we have navBar data (AdminUI pattern uses navBar field)
        String targetId = data.getNavBar();
        if (targetId == null || targetId.isEmpty()) {
            return false;
        }

        // Get the target entry
        FactionPageRegistry.Entry entry = FactionPageRegistry.getInstance().getEntry(targetId);
        if (entry == null) {
            return true; // Consumed but invalid target
        }

        // Check permission
        if (entry.permission() != null && !HyperPermsIntegration.hasPermission(playerRef.getUuid(), entry.permission())) {
            return true; // Consumed but no permission
        }

        // Check faction requirement
        if (entry.requiresFaction() && faction == null) {
            return true; // Consumed but needs faction
        }

        // Create and open the target page
        InteractiveCustomUIPage<?> page = entry.guiSupplier().create(
                player, ref, store, playerRef, faction, guiManager
        );

        if (page != null) {
            player.getPageManager().openCustomPage(ref, store, page);
        }

        return true;
    }

}
