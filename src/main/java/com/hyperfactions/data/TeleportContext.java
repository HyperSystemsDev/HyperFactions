package com.hyperfactions.data;

import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.TeleportManager.StartLocation;
import com.hyperfactions.manager.TeleportManager.TaskScheduler;
import com.hyperfactions.manager.TeleportManager.TeleportExecutor;
import com.hyperfactions.manager.TeleportManager.TeleportResult;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Context object for teleportation operations.
 * Encapsulates all the callbacks and data needed for a teleport request.
 * 
 * <p>This replaces the previous 7-parameter method signature with a cleaner builder pattern.</p>
 */
public record TeleportContext(
    @NotNull UUID playerUuid,
    @NotNull StartLocation startLocation,
    @NotNull TaskScheduler scheduleTask,
    @NotNull Consumer<Integer> cancelTask,
    @NotNull TeleportExecutor doTeleport,
    @NotNull Consumer<String> sendMessage,
    @NotNull Supplier<Boolean> isTagged
) {
    /**
     * Builder for creating TeleportContext instances.
     */
    public static class Builder {
        private UUID playerUuid;
        private StartLocation startLocation;
        private TaskScheduler scheduleTask;
        private Consumer<Integer> cancelTask;
        private TeleportExecutor doTeleport;
        private Consumer<String> sendMessage;
        private Supplier<Boolean> isTagged;

        public Builder playerUuid(@NotNull UUID playerUuid) {
            this.playerUuid = playerUuid;
            return this;
        }

        public Builder startLocation(@NotNull StartLocation startLocation) {
            this.startLocation = startLocation;
            return this;
        }

        public Builder scheduleTask(@NotNull TaskScheduler scheduleTask) {
            this.scheduleTask = scheduleTask;
            return this;
        }

        public Builder cancelTask(@NotNull Consumer<Integer> cancelTask) {
            this.cancelTask = cancelTask;
            return this;
        }

        public Builder doTeleport(@NotNull TeleportExecutor doTeleport) {
            this.doTeleport = doTeleport;
            return this;
        }

        public Builder sendMessage(@NotNull Consumer<String> sendMessage) {
            this.sendMessage = sendMessage;
            return this;
        }

        public Builder isTagged(@NotNull Supplier<Boolean> isTagged) {
            this.isTagged = isTagged;
            return this;
        }

        public TeleportContext build() {
            if (playerUuid == null) throw new IllegalStateException("playerUuid is required");
            if (startLocation == null) throw new IllegalStateException("startLocation is required");
            if (scheduleTask == null) throw new IllegalStateException("scheduleTask is required");
            if (cancelTask == null) throw new IllegalStateException("cancelTask is required");
            if (doTeleport == null) throw new IllegalStateException("doTeleport is required");
            if (sendMessage == null) throw new IllegalStateException("sendMessage is required");
            if (isTagged == null) throw new IllegalStateException("isTagged is required");

            return new TeleportContext(
                playerUuid, startLocation, scheduleTask, cancelTask,
                doTeleport, sendMessage, isTagged
            );
        }
    }

    /**
     * Creates a new builder for TeleportContext.
     */
    public static Builder builder() {
        return new Builder();
    }
}
