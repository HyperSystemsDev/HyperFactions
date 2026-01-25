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

## Hytale Limitations (Cannot Implement)

- Explosion Protection - No explosive devices yet
- Mechanical Block Protection - No block movement mechanics
- Item Transporter Protection - No automated transport systems
