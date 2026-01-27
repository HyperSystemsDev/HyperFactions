# B.4 Module Placeholder Pattern

For unimplemented modules (Treasury, Raids, Levels, War), use this consistent placeholder pattern.

---

## Current Modules

| Module | Icon | Color | Description | Status |
|--------|------|-------|-------------|--------|
| Treasury | Yellow | `#fbbf24` | Faction bank & economy system | Coming Soon |
| Raids | Red | `#ef4444` | Scheduled faction battles | Coming Soon |
| Levels | Purple | `#a855f7` | Faction progression & XP | Coming Soon |
| War | Teal | `#14b8a6` | Formal war declarations | Coming Soon |

---

## Implementation

### Java Pattern

```java
// In FactionModulesPage
private static final List<ModuleInfo> MODULES = List.of(
    new ModuleInfo("treasury", "Treasury", "Faction bank & economy system", "#fbbf24", false),
    new ModuleInfo("raids", "Raids", "Scheduled faction battles", "#ef4444", false),
    new ModuleInfo("levels", "Levels", "Faction progression & XP", "#a855f7", false),
    new ModuleInfo("war", "War", "Formal war declarations", "#14b8a6", false)
);

// For each module
for (int i = 0; i < MODULES.size(); i++) {
    ModuleInfo module = MODULES.get(i);
    String cardSelector = "#ModuleCard" + i;

    cmd.set(cardSelector + " #ModuleName.Text", module.name);
    cmd.set(cardSelector + " #ModuleDesc.Text", module.description);
    cmd.set(cardSelector + " #ColorBar.Background.Color", module.color);

    if (module.available) {
        cmd.set(cardSelector + " #StatusBadge.Text", "AVAILABLE");
        cmd.set(cardSelector + " #StatusBadge.Style.TextColor", "#22c55e");
    } else {
        cmd.set(cardSelector + " #StatusBadge.Text", "COMING SOON");
        cmd.set(cardSelector + " #StatusBadge.Style.TextColor", "#888888");
    }
}
```

### Template Pattern

```
// faction_modules.ui
Group #ModuleCard0 {
  Group #ColorBar { Background: (Color: #fbbf24); Anchor: (Width: 4, Height: 100%); }
  Label #ModuleName { ... }
  Label #ModuleDesc { ... }
  Label #StatusBadge { ... }
  TextButton #ModuleBtn { Visible: false; }  // Hidden until available
}
```

---

## Placeholder Card Design

Each placeholder card displays:
- **Color Bar**: Left-side accent in module theme color
- **Module Name**: Bold title (e.g., "Treasury")
- **Description**: Brief one-line description
- **Status Badge**: "COMING SOON" in muted gray

When a module becomes available:
- Badge changes to "AVAILABLE" in green
- Click handler opens module configuration page
- Optional: "NEW" badge for recently released modules

---

## Future Module Template

When implementing a new module:

1. Update `ModuleInfo` record with `available = true`
2. Create module page class (e.g., `TreasuryPage.java`)
3. Create module UI template (e.g., `treasury.ui`)
4. Add event binding for module button click
5. Register page in `FactionPageRegistry`

---

## Wireframe Reference

```
┌─────────────────────────────────────────────────────────────────────┐
│   FACTION MODULES                              [← Back to Settings] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ┌─────────────────────────────┐ ┌─────────────────────────────┐   │
│   │  ▌ TREASURY                 │ │  ▌ RAIDS                    │   │
│   │                             │ │                             │   │
│   │  Manage faction funds,      │ │  Structured faction raids   │   │
│   │  taxes, and transactions    │ │  with objectives & rewards  │   │
│   │                             │ │                             │   │
│   │  ┌───────────────────────┐  │ │  ┌───────────────────────┐  │   │
│   │  │     COMING SOON       │  │ │  │     COMING SOON       │  │   │
│   │  └───────────────────────┘  │ │  └───────────────────────┘  │   │
│   └─────────────────────────────┘ └─────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────┐ ┌─────────────────────────────┐   │
│   │  ▌ LEVELS                   │ │  ▌ WAR                      │   │
│   │                             │ │                             │   │
│   │  Faction progression with   │ │  Formal war declarations    │   │
│   │  XP, levels, and perks      │ │  with victory conditions    │   │
│   │                             │ │                             │   │
│   │  ┌───────────────────────┐  │ │  ┌───────────────────────┐  │   │
│   │  │     COMING SOON       │  │ │  │     COMING SOON       │  │   │
│   │  └───────────────────────┘  │ │  └───────────────────────┘  │   │
│   └─────────────────────────────┘ └─────────────────────────────┘   │
│                                                                     │
│   ─────────────────────────────────────────────────────────────     │
│   These modules are planned for future updates.                     │
│   Follow our roadmap for release information!                       │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                           [ESC] Back                │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Enhancement Ideas

| Priority | Enhancement | Description |
|----------|-------------|-------------|
| Low | Notify Me Toggle | Let players opt-in to module release notifications |
| Low | Module Preview | Show preview of what the module will offer |
| Low | Roadmap Link | Link to external roadmap for release timeline |
