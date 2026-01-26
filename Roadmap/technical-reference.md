# Technical Reference

## CustomUI Constraints

**What Works with cmd.set()**:
```java
cmd.append("path/to/template.ui")     // Load templates
cmd.set("#ElementId.Text", "value")   // Set text content - WORKS
cmd.set("#ElementId.TextSpans", Message.raw("text"))  // Set formatted text - WORKS
events.addEventBinding()              // Bind event handlers
```

**What Does NOT Work with cmd.set()** - CRASHES THE CLIENT:
```java
cmd.set("#ElementId.Style", "...")           // CRASHES - Cannot set styles
cmd.set("#ElementId.Visible", "true")        // CRASHES - Cannot set visibility
cmd.set("#ElementId.Background.Color", "#FF0000")  // CRASHES - Cannot set background
cmd.set("#ElementId.Style.TextColor", "#FF0000")   // CRASHES - Cannot set style properties
```

**Rule**: `cmd.set()` ONLY works for `.Text` and `.TextSpans` properties. Everything else will crash.

**Workarounds**:

1. **Text Indicators** (for showing active state) - Add checkmark or bullet to button text:
   ```java
   if (faction.open()) {
       cmd.set("#OpenBtn.Text", "● OPEN");      // Active indicator
       cmd.set("#InviteOnlyBtn.Text", "INVITE ONLY");
   } else {
       cmd.set("#OpenBtn.Text", "OPEN");
       cmd.set("#InviteOnlyBtn.Text", "● INVITE ONLY");  // Active indicator
   }
   ```

2. **Conditional Appending** (for showing/hiding elements) - Only append when needed:
   ```java
   // DON'T DO THIS - Visible doesn't work:
   // cmd.set("#DangerZone.Visible", "true");

   // DO THIS - Only append when element should be visible:
   if (isLeader) {
       cmd.append("#Container", "HyperFactions/danger_zone.ui");
       events.addEventBinding(..., "#Container #DisbandBtn", ...);
   }
   ```

3. **Separate Templates** - Create multiple .ui files for different states and append the right one.

---

## Event Binding Pattern

**Correct pattern for nav buttons**:
```java
EventData.of("Button", "Nav").append("NavBar", entry.id())
```

**Key**: Must set `button` field to non-null value, otherwise handler returns early.

---

## Native Back Button

Use `$C.@BackButton {}` at end of UI template. Do NOT bind custom events - Hytale handles dismissal.

---

## Background Property Syntax

**CRITICAL**: `cmd.set()` for Background does NOT work! Creates red X pattern.

**In .ui template files** - use tuple format:
```
Background: (Color: #374151);
```

**In Java** - DO NOT use cmd.set() for Background. Instead, create elements inline:
```java
// CORRECT - bake color into appendInline
cmd.appendInline("#Container", "Group { Anchor: (Width: 32, Height: 32); Background: (Color: #22c55e); }");

// WRONG - cmd.set() for Background shows red X!
cmd.set("#Element.Background", "Solid { Color: 123456; }");  // DOESN'T WORK
```

**In style definitions** - use tuple format:
```
@InvisibleButtonStyle = TextButtonStyle(
  Default: (Background: (Color: #00000000), LabelStyle: @LabelStyle),
  ...
);
```

---

## Event Binding - Element Types

**ONLY Button/TextButton can receive click events!**

Groups CANNOT receive events - causes "Failed to apply CustomUI event bindings":
```java
// WRONG - Groups can't receive events
events.addEventBinding(CustomUIEventBindingType.Activating, "#SomeGroup", ...);

// CORRECT - Bind to Button/TextButton
events.addEventBinding(CustomUIEventBindingType.Activating, "#SomeButton", ...);
```

**Solution for clickable colored areas**: Use layered structure:
- Group #Cell for background color (set dynamically)
- TextButton #ClickBtn with @InvisibleButtonStyle for click events
- Label for text overlay

---

## Dynamic Grid Pattern (Territory Map)

**Working Pattern** - Create cells inline with color baked in:

Since `cmd.set()` for Background doesn't work, create each cell with its color at creation time:

```java
// Create row container
cmd.appendInline("#ChunkGrid", "Group { LayoutMode: Left; }");

// Create cell inline WITH color baked in (not set afterwards)
cmd.appendInline("#ChunkGrid[" + rowIndex + "]",
    "Group { Anchor: (Width: 16, Height: 16); Background: (Color: " + hexColor + "); }");

// Add button overlay for click events (appended as child)
cmd.append("#ChunkGrid[" + rowIndex + "][" + colIndex + "]", "HyperFactions/chunk_btn.ui");

// Bind events to the button
events.addEventBinding(CustomUIEventBindingType.Activating,
    "#ChunkGrid[" + rowIndex + "][" + colIndex + "] #Btn", eventData, false);
```

**Key Points**:
- Color must be hex string like `#22c55e` in the inline template
- Use double indexing `#Container[row][col]` to select cells
- Append button template as child for click events
- Button uses `@InvisibleButtonStyle` (transparent background)

**Nested Groups for Borders** (if needed):
```java
cmd.appendInline("#ChunkGrid[" + rowIndex + "]",
    "Group { Anchor: (Width: 16, Height: 16); " +
    "Background: (Color: " + borderColor + "); Padding: (Full: 1); " +
    "Group { Background: (Color: " + fillColor + "); } }");
```

---

## Hytale Chunk System

**Hytale uses 32-block chunks** (NOT 16-block like Minecraft):

```java
public static final int CHUNK_SIZE = 32;
private static final int CHUNK_SHIFT = 5;  // log2(32)

public static int blockToChunk(int blockCoord) {
    return blockCoord >> CHUNK_SHIFT;  // Divide by 32
}

public static int chunkToBlockMin(int chunkCoord) {
    return chunkCoord << CHUNK_SHIFT;  // Multiply by 32
}
```

**Converting world coordinates to chunk coordinates:**
- Block coordinate 46 → Chunk coordinate 1 (46 >> 5 = 1)
- Block coordinate 96 → Chunk coordinate 3 (96 >> 5 = 3)

**Important**: Using 16-block chunks (shift 4) will produce wrong chunk coordinates.

---

## Modal/Dialog Layout Patterns

**Working Modal Structure** (tested with set_relation_modal.ui):

```
$C.@PageOverlay {
  $C.@DecoratedContainer {
    Anchor: (Width: 400, Height: 450);  // Fixed size for modals

    #Title { ... }

    #Content {
      LayoutMode: Top;
      Padding: (Left: 15, Right: 15, Top: 10, Bottom: 10);

      // Search/input at top
      Group #SearchContainer { ... }

      // Results list - CRITICAL: Set fixed height to prevent overflow
      Group #ResultsList {
        LayoutMode: TopScrolling;
        ScrollbarStyle: $C.@DefaultScrollbarStyle;
        Anchor: (Height: 250);  // Fixed height for scrollable area
      }

      // Empty state - keep compact
      Label #EmptyState {
        Anchor: (Height: 40);  // Reduced from larger values
      }

      // Action buttons at bottom
      Group #ButtonContainer {
        Anchor: (Height: 50, Top: 10);
        // Buttons go here
      }
    }
  }
}
```

**Key Rules for Modal Layouts:**
1. Use fixed `Anchor: (Height: N)` for scrollable lists to prevent content overflow
2. Keep empty states compact (40-60px height)
3. Always include `Top:` margin before bottom button containers
4. Modal height should accommodate all content without overflow

---

## Section-Based Page Layouts

**Relations Page Pattern** (tested with faction_relations.ui):

When building pages with multiple sections (Allies, Enemies, Requests), use this structure:

```
#Content {
  LayoutMode: TopScrolling;
  ScrollbarStyle: $C.@DefaultScrollbarStyle;
  Padding: (Left: 15, Right: 15, Top: 10, Bottom: 10);

  // === SECTION 1 ===
  Group #Section1Header {
    LayoutMode: Left;
    Anchor: (Height: 28, Bottom: 6);

    Label #Section1Label {
      Text: "SECTION 1 (0)";
      Style: (FontSize: 12, TextColor: #00AAFF, RenderBold: true, VerticalAlignment: Center);
    }
  }

  Group #Section1List {
    LayoutMode: Top;
    Anchor: (Bottom: 12);  // Spacing after section
  }

  // === SECTION 2 ===
  // ... same pattern
}
```

**Key Rules:**
1. Wrap Labels in Groups with fixed Height anchors
2. Add VerticalAlignment: Center to label styles inside Groups
3. Use `Anchor: (Bottom: N)` for spacing between sections
4. Keep LayoutMode: Top for list containers (items added via cmd.append())

---

## Settings Page Patterns

**Teleport Integration** (tested in FactionSettingsPage.java):

To enable actual teleportation (not just chat instructions):

```java
// In page class - add HyperFactions reference
private final HyperFactions hyperFactions;

// In TeleportHome handler:
case "TeleportHome" -> {
    if (faction.home() == null) {
        player.sendMessage(Message.raw("No faction home set.").color("#FF5555"));
        sendUpdate();
        return;
    }

    guiManager.closePage(player, ref, store);  // Close GUI first

    // Get current position for movement checking
    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    Vector3d startLoc = transform != null ? transform.getPosition() : null;

    // Use TeleportManager with warmup/combat-tag support
    TeleportManager.TeleportResult result = hyperFactions.getTeleportManager().teleportToHome(
        uuid,
        startLoc,
        (delayTicks, task) -> hyperFactions.scheduleDelayedTask(delayTicks, task),
        hyperFactions::cancelTask,
        f -> executeTeleport(store, ref, player.getWorld(), f),
        message -> player.sendMessage(Message.raw(message)),
        () -> hyperFactions.getCombatTagManager().isTagged(uuid)
    );

    // Handle result (WAITING = warmup started, SUCCESS = instant, errors)
}
```

---

## Leader Succession Pattern

**Automatic leader promotion** when leader leaves (FactionManager.removeMember):

```java
// In Faction.java:
@Nullable
public FactionMember findSuccessor() {
    return members.values().stream()
            .filter(m -> !m.isLeader())
            .max(Comparator
                    .comparingInt((FactionMember m) -> m.role().getLevel())  // Highest role first
                    .thenComparingLong(m -> -m.joinedAt()))  // Then oldest tenure
            .orElse(null);
}

// In FactionManager.removeMember():
if (target.isLeader()) {
    FactionMember successor = faction.findSuccessor();
    if (successor == null) {
        // No other members - disband faction
        return disbandFactionInternal(factionId, "Leader left with no remaining members");
    }
    // Promote successor to leader
    FactionMember promoted = successor.withRole(FactionRole.LEADER);
    // ... update faction with new leader
}
```

**Succession Priority:**
1. Highest role level (Officer > Member)
2. If same role, longest tenure (earliest joinedAt timestamp)

---

## Shadow JAR Configuration

**CRITICAL: Do NOT use minimize()** - it removes Gson inner classes:

```gradle
shadowJar {
    archiveClassifier.set('')
    relocate 'com.google.gson', 'com.hyperfactions.lib.gson'
    // Don't minimize - it removes Gson's inner classes needed at runtime
    // ERROR if minimized: NoClassDefFoundError: LinkedTreeMap$EntrySet
}
```

---

## Hytale Limitations (Cannot Implement)

- Explosion Protection - No explosive devices yet
- Mechanical Block Protection - No block movement mechanics
- Item Transporter Protection - No automated transport systems
