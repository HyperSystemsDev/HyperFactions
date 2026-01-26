# Technical Reference

## CustomUI Constraints

**What Works**:
```java
cmd.append("path/to/template.ui")     // Load templates
cmd.set("#ElementId.Text", "value")   // Set text content
cmd.set("#ElementId.TextSpans", Message.raw("text"))  // Set formatted text
events.addEventBinding()              // Bind event handlers
```

**What Does NOT Work**:
```java
cmd.set("#ElementId.Style", "...")    // CRASHES - Cannot set styles dynamically
```

**Solution**: Create separate template files for different states.

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

## Hytale Limitations (Cannot Implement)

- Explosion Protection - No explosive devices yet
- Mechanical Block Protection - No block movement mechanics
- Item Transporter Protection - No automated transport systems
