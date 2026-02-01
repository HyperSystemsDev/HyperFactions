package com.hyperfactions.gui.help;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents an individual help topic within a category.
 *
 * @param id           Unique identifier for this topic
 * @param title        Display title (shown as section header)
 * @param lines        Content lines to display
 * @param commands     Associated command names (for deep-linking)
 * @param category     Parent category
 */
public record HelpTopic(
        @NotNull String id,
        @NotNull String title,
        @NotNull List<String> lines,
        @NotNull List<String> commands,
        @NotNull HelpCategory category
) {
    /**
     * Creates a topic with content lines but no associated commands.
     */
    public static HelpTopic of(@NotNull String id, @NotNull String title,
                               @NotNull List<String> lines, @NotNull HelpCategory category) {
        return new HelpTopic(id, title, lines, List.of(), category);
    }

    /**
     * Creates a topic with both content and associated commands.
     */
    public static HelpTopic withCommands(@NotNull String id, @NotNull String title,
                                         @NotNull List<String> lines, @NotNull List<String> commands,
                                         @NotNull HelpCategory category) {
        return new HelpTopic(id, title, lines, commands, category);
    }
}
