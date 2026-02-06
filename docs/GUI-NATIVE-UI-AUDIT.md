# HyperFactions GUI Audit: Native Hytale UI Alignment

**Date**: 2026-02-05
**Status**: Research-only audit (no code modifications)
**Scope**: All HyperFactions GUI pages vs. native Hytale CustomUI elements

---

## Executive Summary

HyperFactions currently uses **~75 TextButton workarounds** across its GUI where native Hytale elements would provide better UX, clearer semantics, and less Java-side code. The biggest wins are:

1. **46 toggle buttons** (ON/OFF text with manual color swaps) -> **CheckBoxWithLabel** (native boolean toggle)
2. **16-button color grid** (pre-baked styles because `cmd.set()` crashes on TextButton styles) -> **ColorPicker** (native HSV gradient + hue strip + hex input)
3. **5 radius preset buttons + TextField** -> **Slider** (native numeric range, matches Hytale's own tool reach slider)
4. **30 sort/selection buttons** (`.Disabled = true` hack for active state) -> **DropdownBox** (native selection popup)
5. **~5 TextButton tab bars** (`.Disabled` for active, manual visibility toggling) -> **TabNavigation + MenuItem** (native tab semantics)
6. **27 custom button styles** (manual component assembly) -> **`$C.@DefaultTextButtonStyle`** / **`$C.@SecondaryTextButtonStyle`** for standard actions
7. **Button-triggered search** (TextField + SEARCH button + CLEAR button) -> **Native real-time search** (ValueChanged on TextField, matching Hytale's own pattern)

All recommended native elements are **confirmed available** in the Hytale server's decompiled source (used in PrefabEditor, EntitySpawnPage, ChunkTintCommand, LaunchPad).

### Screenshot Confirmation (Hytale Creative Mode Quick Settings)

The following native elements have been **visually confirmed** from Hytale's own inventory/creative settings screens:

| Element | Screenshot Evidence |
|---|---|
| **CheckBoxWithLabel** | "Hide Hotbar", "Hide Compass", "Hide Notifications", "Hide Chat" — native square checkboxes with label text |
| **ColorPicker** | Full HSV gradient square + vertical hue strip + hex input (`#2effd3`) + color swatch button |
| **Slider** | Tool Reach Distance, Tool Delay — diamond handle on horizontal track with numeric readout |
| **Native Button** | "RESET TO DEFAULTS" — `$C.@DefaultTextButtonStyle` with 9-slice texture background |
| **Collapsible Section** | "DISPLAY" section with chevron — client-side only, not replicable in plugins |
| **Search Bar** | Magnifying glass icon that expands to search field — **client-side only** collapse/expand animation |
| **ESC Back** | Bottom-left "ESC Back" button — `$C.@BackButton` (already used by HyperFactions) |
| **Tab Bar** | "INVENTORY TAB", "MAP M", "CREATIVE TOOLS B" — native MenuItem tabs with keyboard shortcut labels |

---

## Native Elements Confirmed Available

| Element | Server Usage Evidence | Key Properties | Key Events |
|---|---|---|---|
| **CheckBox / CheckBoxWithLabel** | LaunchPad.java (10+ instances) | `.Value` (boolean), `.Disabled`, `$C.@CheckBoxStyle` | `ValueChanged` |
| **DropdownBox** | PrefabEditorLoadSettingsPage.java (6+ instances) | `.Value` (string), `.Entries` (List\<DropdownEntryInfo\>), `AllowUnselection`, `MaxSelection` | `ValueChanged` |
| **Slider** | EntitySpawnPage.java (#ScaleSlider), SliderDensity.java | `Min`, `Max`, `Step` (all int), `.Value` (int), `$C.@DefaultSliderStyle` | `ValueChanged`, `MouseButtonReleased` |
| **ColorPicker** | ChunkTintCommand.java (#GrassTint) | `.Color` (hex string with `#` prefix) | `ValueChanged` |
| **TabNavigation + MenuItem** | Inventory screen tab bar | Container with MenuItem children, `SelectedStyle` | `SelectedTabChanged` |
| **ProgressBar** | PrefabEditorLoadSettingsPage.java, MemoriesPage.java | `.Value` (0.0-1.0), Direction, Bar/Background styling | None (visual only) |

### Confirmed `cmd.set()` Patterns (from decompiled server code)

```java
// CheckBox
commandBuilder.set("#EnableWorldTicking #CheckBox.Value", false);

// DropdownBox
List<DropdownEntryInfo> dropdown = new ObjectArrayList<>();
dropdown.add(new DropdownEntryInfo(LocalizableString.fromString("Option A"), "optionA"));
commandBuilder.set("#Dropdown #Input.Entries", dropdown);
commandBuilder.set("#Dropdown #Input.Value", "optionA");

// Slider
eventBuilder.addEventBinding(ValueChanged, "#ScaleSlider",
    new EventData().append("Type", "UpdateScale").append("@Scale", "#ScaleSlider.Value"), false);

// ColorPicker
commandBuilder.set("#GrassTint #Input.Color", "#5B9E28");
eventBuilder.addEventBinding(Activating, "#LoadButton",
    new EventData().append("@GrassTint", "#MainPage #GrassTint #Input.Color"));
```

---

## Audit by Pattern

### Pattern 1: Toggle Buttons -> CheckBoxWithLabel

**Current implementation**: TextButtons displaying "ON"/"OFF" text, with manual `LabelStyle.TextColor` color swaps (green `#55FF55` for ON, red `#FF5555` for OFF, gray `#666666` for LOCKED).

**Three inconsistent approaches exist**:

| Page | Text | Color | State Update |
|---|---|---|---|
| AdminZoneSettingsPage (22 flags) | "ON"/"OFF"/"N/A" | **None** (comment: "crashes CustomUI") | Full page rebuild |
| AdminFactionSettingsPage (11 perms) | "ON"/"OFF"/"ON (LOCKED)" | Green/Red/Gray via `LabelStyle.TextColor` | Full page rebuild |
| FactionSettingsTabsPage (11 perms) | "ON"/"OFF"/"LOCKED" | Green/Red via `LabelStyle.TextColor` | Fresh page pattern (close+reopen) |
| AdminDashboardPage (1 bypass) | Label "ON"/"OFF" + Button "ENABLE"/"DISABLE" | Green/Red via `Style.TextColor` | Partial rebuild |

**Total: 45 toggle instances across 4 pages**

#### Exact Current Code (AdminFactionSettingsPage.java:221-253)
```java
private void buildToggle(UICommandBuilder cmd, UIEventBuilder events,
                         String elementId, String permName, boolean currentValue,
                         ConfigManager config) {
    boolean locked = config.isPermissionLocked(permName);
    String selector = "#" + elementId;

    if (locked) {
        cmd.set(selector + ".Text", currentValue ? "ON (LOCKED)" : "OFF (LOCKED)");
        cmd.set(selector + ".Disabled", true);
        cmd.set(selector + ".Style.Default.LabelStyle.TextColor", "#666666");
    } else {
        cmd.set(selector + ".Text", currentValue ? "ON" : "OFF");
        cmd.set(selector + ".Disabled", false);
        String color = currentValue ? "#55FF55" : "#FF5555";
        cmd.set(selector + ".Style.Default.LabelStyle.TextColor", color);
        cmd.set(selector + ".Style.Hovered.LabelStyle.TextColor", color);
    }
}
```

#### Proposed: CheckBoxWithLabel

```java
// Proposed replacement
private void buildToggle(UICommandBuilder cmd, UIEventBuilder events,
                         String elementId, String permName, boolean currentValue,
                         ConfigManager config) {
    boolean locked = config.isPermissionLocked(permName);
    String selector = "#" + elementId;

    cmd.set(selector + ".Value", currentValue);
    cmd.set(selector + ".Disabled", locked);

    if (!locked) {
        events.addEventBinding(ValueChanged, selector,
            EventData.of("Button", "TogglePerm")
                     .append("Perm", permName)
                     .append("@Value", selector + ".Value"),
            false);
    }
}
```

**Impact**: Eliminates color swap logic, "ON"/"OFF" text management, and 3 divergent patterns -> 1 consistent pattern. CheckBox provides visual boolean state at a glance without reading text.

#### Instance Breakdown

**AdminZoneSettingsPage** (22 toggles, 7 categories):
- Build: `buildFlagToggle()` at line 127
- Template: `admin_zone_settings.ui` — 22 TextButton elements (`#Flag0Toggle` through `#Flag21Toggle`)
- Categories: Building (4), Interaction (4), Entity (3), Environment (3), Misc (3), Special (3), Extra (2)
- Special states: "N/A" for unavailable mixins, "(default)"/"(custom)"/"(mixin)" indicators

**AdminFactionSettingsPage** (11 toggles):
- Build: `buildToggle()` at line 221
- Template: `admin_faction_settings.ui` — 11 TextButton elements
- Permissions: 3 outsider (build/break/interact) + 3 ally + 3 member + PvP + officersCanEdit
- Special states: "ON (LOCKED)"/"OFF (LOCKED)" for server-locked permissions

**FactionSettingsTabsPage** (11 toggles):
- Build: `buildToggle()` at line 287
- Template: `settings_permissions_content.ui` — 11 TextButton elements
- Same permission set as admin, but user-facing with role-based edit permissions
- Special states: "LOCKED" (server-locked), read-only (insufficient role)

**AdminDashboardPage** (1 toggle):
- Build: inline at line 96
- Template: `admin_dashboard.ui` — Label `#BypassState` + TextButton `#ToggleBypassBtn`
- Unique split pattern: state display separate from action button

#### Verification Needed
- [x] `cmd.set("#checkbox.Value", true/false)` — **Confirmed**: Server uses `commandBuilder.set("#EnableWorldTicking #CheckBox.Value", false)`
- [ ] `CheckBox.Disabled` dynamic setting — Likely works (`.Disabled` works on TextButton already)
- [ ] CheckBoxWithLabel label text dynamic setting — Need to test if label portion updates via `cmd.set()`
- [ ] Visual styling with HyperFactions' dark theme — May need custom `CheckedStyle`/`UncheckedStyle` for cyan/teal aesthetic

---

### Pattern 2: Color Picker Modal -> Inline Native ColorPicker (Full Redesign)

**Decision**: Replace the color picker modal entirely with inline native ColorPicker elements. Introduce zone colors as a new feature. Reorganize/enlarge settings pages as needed to accommodate the picker without scrolling.

#### Current State

**Faction colors**: Stored as Minecraft color codes (`"0"` through `"f"`) mapped to 16 hex values. Color selection is a separate modal page (`ColorPickerPage.java`) with a 4x4 grid of 16 pre-styled buttons. Styles are baked into the template because `cmd.set()` crashes on TextButton background styles.

**Zone colors**: Zones have **no color field**. Safe zones are hardcoded green (`#55FF55`), war zones hardcoded red (`#FF5555`). The distinction is a 4px color bar + text label only.

**Two separate color picker implementations exist**:

| Page | Element Type | Color Count | Styles |
|---|---|---|---|
| ColorPickerPage (shared modal) | TextButton | 16 | 16 separate `@ColorNStyle` definitions (lines 10-127 of `color_picker.ui`) |
| CreateFactionStep1Page | Button (raw) | 16 | Inline `Background: (Color: #hex)` on each element |

**Current workarounds**:
1. Cannot dynamically highlight selected color — no visual feedback of current selection
2. Fresh page pattern — opens new page instance to update state
3. `@` prefix input capture to preserve TextField values across page transitions
4. 16 pre-baked style definitions because `cmd.set()` crashes on TextButton styles

#### Native ColorPicker (Confirmed from Screenshot)

The Hytale Creative Mode Quick Settings shows the native ColorPicker:
- Full **HSV gradient square** (~200x200px)
- Vertical **hue strip** beside it
- **Hex text input** below (`#2effd3`)
- **Color swatch button** for "Default Laser Pointer Color" (small colored square that opens the picker)
- Value format: `#RRGGBB` hex string (confirmed from server code: `#5B9E28`)

#### Design Decision: Arbitrary Hex Colors

**Chosen: Option A — Accept arbitrary hex colors.** The 16 Minecraft color code constraint was a limitation of the old button grid, not a gameplay requirement. With a native ColorPicker:
- Faction chat prefixes can use any hex color via `Message.raw(name).color(hexColor)`
- Zone indicator bars can use any hex color
- The `Faction.color` field changes from `String` (single char "0"-"f") to `String` (hex like `"#55FFFF"`)
- Migration: existing single-char codes map to their hex equivalents on first load

---

#### 2a. Faction Settings — Inline ColorPicker (Page Redesign)

**Current page**: `settings_tabs.ui` — 600×620px, 3 tabs (General / Permissions / Members), all tabs use `TopScrolling`.

**Current General tab APPEARANCE section**: 51px total — one row with Label + Preview + Hex text + "CHANGE" button that opens modal.

**Proposed**: Enlarge page to **700×720px** and restructure the General tab to fit ColorPicker inline with no scrolling needed.

```
Proposed General Tab Layout (total content: ~590px, fits in ~650px content area):

GENERAL (147px)
├─ Name row (32px)
├─ Tag row (32px)
└─ Description row (32px)
   + section header/dividers (~51px)

APPEARANCE (220px) ← expanded from 51px
├─ Section header + divider (19px)
├─ Current color row: Preview swatch (24×24) + Hex label + color name (28px)
├─ Gap (8px)
├─ ColorPicker element (160px) ← native HSV gradient + hue strip + hex input
└─ Gap (5px)

RECRUITMENT (51px)
├─ Section header + divider (19px)
└─ DropdownBox inline (32px) ← replaces modal, see Pattern 6

HOME LOCATION (99px)
├─ Section header + divider (19px)
├─ Location display (28px)
└─ Action buttons (32px)

MODULES (55px)
├─ Section header + divider (19px)
└─ View modules button (36px)

DANGER ZONE (55px) — leader only
├─ Section header + divider (19px)
└─ Disband button (36px)

Total: ~627px content (leader) / ~572px (non-leader)
Available at 720px page height: ~650px content area (720 - 40 tab bar - 1 divider - ~29 padding)
Result: Fits without scrolling for all users ✓
```

**Key changes**:
- Page: 600×620 -> **700×720**
- APPEARANCE section: 51px -> **220px** (ColorPicker inline)
- RECRUITMENT: modal button -> inline DropdownBox (saves a page transition)
- Remove `TopScrolling` from General tab content (no longer needed)

---

#### 2b. Admin Faction Settings — Inline ColorPicker (Page Redesign)

**Current page**: `admin_faction_settings.ui` — 720×560px, two-column layout, both columns `TopScrolling`.

**Proposed**: Enlarge to **800×700px** and restructure left column to fit ColorPicker inline.

```
Proposed Left Column (330px wide, ~610px content):

GENERAL (118px)
├─ Name + EDIT (32px)
├─ Tag + EDIT (32px)
└─ Description + EDIT (32px)
   + header/dividers (22px)

APPEARANCE (210px) ← expanded from ~44px
├─ Section header + divider (22px)
├─ Preview swatch + Hex label (28px)
├─ ColorPicker element (160px)

RECRUITMENT (54px)
├─ Section header + divider (22px)
└─ DropdownBox inline (32px) ← replaces modal

HOME LOCATION (82px)
├─ Section header + divider (22px)
├─ Location display (28px)
└─ Clear home button (32px)

DANGER ZONE (54px)
├─ Section header + divider (22px)
└─ Disband button (32px)

Left column total: ~518px
Available at 700px height: ~605px (700 - 50 title/header - 45 footer)
Result: Fits without scrolling ✓

Proposed Right Column (unchanged width, but shorter with CheckBoxes):
TERRITORY PERMISSIONS (~180px with CheckBoxWithLabel)
COMBAT (~58px)
ACCESS CONTROL (~58px)
Right column total: ~296px — plenty of room ✓
```

**Key changes**:
- Page: 720×560 -> **800×700**
- Left column APPEARANCE: 44px -> **210px**
- Left column RECRUITMENT: modal button -> inline DropdownBox
- Right column: TextButton toggles -> CheckBoxWithLabel (more compact)
- Remove `TopScrolling` from both columns

---

#### 2c. Zone Colors — New Feature + Zone Settings Redesign

**Current state**: Zones have no color field. `Zone.java` is a record with `id`, `name`, `type`, `world`, `chunks`, `createdAt`, `createdBy`, `flags`.

**Proposed**: Add a `color` field to zones. Default to type-based colors (safe=`#55FF55`, war=`#FF5555`). Allow admins to customize per-zone.

##### Data Model Changes

```java
// Zone.java — add color field
public record Zone(
    UUID id,
    String name,
    ZoneType type,
    String world,
    Set<ChunkPos> chunks,
    Instant createdAt,
    UUID createdBy,
    Map<String, Boolean> flags,
    String color  // NEW: hex color like "#55FF55", defaults to type color
) {
    // Default color based on type
    public String effectiveColor() {
        return color != null ? color : type.defaultColor();
    }
}
```

```java
// ZoneType.java — add default colors
public enum ZoneType {
    SAFE("SafeZone", "#55FF55", ...),
    WAR("WarZone", "#FF5555", ...);

    private final String defaultColor;
    public String defaultColor() { return defaultColor; }
}
```

##### Zone Settings Page Redesign

**Current page**: `admin_zone_settings.ui` — 720×680px, two-column layout with 23 flag toggles, **no scrolling**. This page is packed tight.

**Problem**: Adding an inline ColorPicker (~160px height) to this page requires significant reorganization. The current layout uses every pixel of both 330px columns for flag toggles.

**Proposed**: Restructure into **tabbed layout** — split zone info from zone flags.

```
Proposed Zone Settings Page (800×700px):

Title Bar: "Admin: Zone Settings"
Header: Zone name + type badge + chunk count (32px)
Divider (1px)

Tab Bar: [PROPERTIES] [FLAGS] (30px)

━━━ PROPERTIES TAB ━━━ (shown first)

Two-Column Layout:

LEFT COLUMN (380px):
  ZONE INFO (130px)
  ├─ Name + RENAME button (32px)
  ├─ Type + CHANGE TYPE button (32px)
  ├─ Chunks display (28px)
  └─ Created by/date (28px)
     + header/dividers

  ZONE COLOR (210px)
  ├─ Section header + divider (22px)
  ├─ Preview swatch + Hex label (28px)
  ├─ ColorPicker element (160px)

RIGHT COLUMN (380px):
  QUICK FLAGS SUMMARY (read-only overview)
  ├─ PvP: Enabled/Disabled
  ├─ Explosions: Enabled/Disabled
  ├─ Building: Enabled/Disabled
  └─ ... key flags at a glance
  (Links to FLAGS tab for editing)

  ACTIONS
  ├─ Reset to Defaults button
  ├─ Delete Zone button

━━━ FLAGS TAB ━━━ (current 2-column flag layout, mostly unchanged)

Two-Column Layout (same as current, with CheckBoxWithLabel):
LEFT: Combat (4) + Damage (2) + Death (1) + Building (1) + Items (4) = 12 flags
RIGHT: Interaction (6) + Mob Spawning (5) = 11 flags

Footer: BACK TO ZONES button
```

**Key changes**:
- Page: 720×680 -> **800×700**
- Add TabNavigation with 2 tabs (Properties / Flags)
- PROPERTIES tab: zone info + inline ColorPicker + actions
- FLAGS tab: existing 23-toggle layout (now with CheckBoxWithLabel)
- Both tabs fit without scrolling

**Why tabs work here**: The current page tries to show everything at once. With 23 flag toggles + zone metadata, it's already at capacity. Adding a color picker would break the layout. Splitting into tabs gives each concern dedicated space while keeping everything accessible.

##### Create Zone Wizard — Add Color Selection

**Current page**: `create_zone_wizard.ui` — 720×500px. Has zone type, name, claiming method, radius, and flags sections.

**Proposed**: Add a color section. Enlarge to **720×580px** or reorganize slightly.

```
Proposed addition to Create Zone Wizard:

Top Row (existing):
├─ Zone Type Card (SafeZone / WarZone)
├─ Zone Name Card

Middle Row (expanded):
├─ Claiming Method Card (existing)
├─ Right side (rearranged):
   ├─ Zone Color Card (NEW, 120px)
   │  ├─ "ZONE COLOR" label
   │  ├─ Preview swatch showing default type color
   │  └─ Small ColorPicker or "Use default" + "Custom..." option
   ├─ Radius Card (existing, moved/resized)
   └─ Flags Card (existing)
```

**Alternative**: For zone creation, default to the type color and let admins customize later via zone settings. This keeps the wizard simpler and puts the full ColorPicker only on the settings page.

---

#### 2d. Create Faction Step 1 — Replace Grid with ColorPicker

**Current page**: `create_step1.ui` — 500×520px. Has name, tag, 16-button color grid, preview, and next button.

**Proposed**: Replace the 16-button grid (120px section) with a native ColorPicker (~180px section). Enlarge page to **500×600px**.

```
Proposed Create Faction Step 1 (500×600px):

FACTION NAME (70px)
├─ Label + TextField

TAG (70px)
├─ Label + TextField

FACTION COLOR (200px) ← expanded from 120px
├─ Label "FACTION COLOR" (20px)
├─ ColorPicker element (160px) ← replaces 16-button grid
├─ Gap (5px)
└─ Preview: colored faction name (15px)

Spacer (FlexWeight: 1)

NEXT button (45px)
```

**Impact**: Eliminates the 16-button grid + 16 pre-baked style definitions. Users get full color freedom. Preview updates in real-time via `ValueChanged`.

---

#### ColorPicker Summary

| Location | Current | Proposed | Page Size Change |
|---|---|---|---|
| **Create Faction Step 1** | 16-button grid (120px) | Inline ColorPicker (200px) | 500×520 -> **500×600** |
| **Faction Settings General** | "CHANGE" button -> modal | Inline ColorPicker (220px section) | 600×620 -> **700×720** |
| **Admin Faction Settings** | "CHANGE" button -> modal | Inline ColorPicker (210px section) | 720×560 -> **800×700** |
| **Admin Zone Settings** | No color | New PROPERTIES tab with ColorPicker | 720×680 -> **800×700** + tabs |
| **Create Zone Wizard** | No color | Default to type color (customize later) | Unchanged |

#### Files Eliminated
- `shared/page/ColorPickerPage.java` (198 lines)
- `shared/data/ColorPickerData.java`
- `shared/component/ColorPickerModal.java`
- `shared/color_picker.ui` (261 lines, 16 baked style definitions)

#### Data Model Changes Required
- `Faction.java` — `color` field: `String` single-char -> `String` hex (`#RRGGBB`)
- `Zone.java` — Add `String color` field (nullable, defaults to type color)
- `ZoneType.java` — Add `defaultColor()` method
- `JsonFactionStorage.java` — Migration: `"a"` -> `"#55FF55"`, `"b"` -> `"#55FFFF"`, etc.
- `JsonZoneStorage.java` — Add color field serialization, default to null for existing zones

#### Verification Needed
- [x] ColorPicker is full HSV gradient — **Confirmed via screenshot**: gradient square + hue strip + hex input
- [x] Value format — **Confirmed** `#RRGGBB` hex string
- [ ] Dynamic initial value — Can `cmd.set("#picker.Color", "#00FFFF")` set the starting color?
- [ ] Minimum usable size — Screenshot shows ~200×200px gradient area. Can it work at 160px?
- [ ] ColorPicker within DecoratedContainer — Does it render correctly inside the styled panel?

---

### Pattern 3: Sort Buttons -> DropdownBox

**Current implementation**: Multiple TextButtons in a horizontal row. The active sort option has `.Disabled = true`. All buttons always visible. State tracked via `SortMode` enum in Java.

**100% consistent pattern across all 4 browse pages**:

```java
// Identical pattern in all pages:
cmd.set("#SortByPower.Disabled", sortMode == SortMode.POWER);
cmd.set("#SortByName.Disabled", sortMode == SortMode.NAME);
cmd.set("#SortByMembers.Disabled", sortMode == SortMode.MEMBERS);
```

#### Instance Breakdown

| Page | Options | Element IDs | Style |
|---|---|---|---|
| NewPlayerBrowsePage | POWER, NAME, MEMBERS (3) | `#SortByPower`, `#SortByName`, `#SortByMembers` | `@SmallCyanButtonStyle` + `@SmallButtonStyle` |
| FactionBrowserPage | POWER, NAME, MEMBERS (3) | `#SortByPower`, `#SortByName`, `#SortByMembers` | Same |
| FactionMembersPage | ROLE, LAST_ONLINE (2) | `#SortByRole`, `#SortByOnline` | Same |
| AdminFactionsPage | POWER, NAME, MEMBERS (3) | `#SortByPower`, `#SortByName`, `#SortByMembers` | Same |
| **Total** | **11 buttons** | | |

#### Additional Selection Patterns (CreateZoneWizardPage)

| Selection | Options | Current Implementation |
|---|---|---|
| Zone Type | SAFEZONE, WARZONE (2) | `#SafeZoneBtn.Disabled`, `#WarZoneBtn.Disabled` |
| Claim Method | No claims, Single, Circle, Square, Map (5) | `#MethodNone.Disabled` through `#MethodMap.Disabled` |
| Radius Presets | 3, 5, 10, 15, 20 (5) | `#Radius3.Disabled` through `#Radius20.Disabled` |
| Flag Mode | Defaults, Customize (2) | `#FlagsDefaults.Disabled`, `#FlagsCustomize.Disabled` |
| **Subtotal** | **14 buttons** | |

**Grand total: 25 selection buttons across 5 pages** (+ 5 radius buttons that would become a Slider instead)

#### Proposed: DropdownBox

```java
// Sort dropdown (replaces 3 TextButtons)
List<DropdownEntryInfo> sortEntries = List.of(
    new DropdownEntryInfo(LocalizableString.fromString("Power"), "POWER"),
    new DropdownEntryInfo(LocalizableString.fromString("Name"), "NAME"),
    new DropdownEntryInfo(LocalizableString.fromString("Members"), "MEMBERS")
);
cmd.set("#SortDropdown.Entries", sortEntries);
cmd.set("#SortDropdown.Value", sortMode.name());

events.addEventBinding(ValueChanged, "#SortDropdown",
    EventData.of("Button", "ChangeSort")
             .append("@Sort", "#SortDropdown.Value"),
    false);
```

**Impact**: Each dropdown replaces 2-5 TextButtons + their event bindings. More space-efficient. The current selection is always visible in the dropdown label.

#### Verification Needed
- [x] Dynamic entry population — **Confirmed**: Server uses `commandBuilder.set("#Dropdown #Input.Entries", entryList)`
- [x] String value binding — **Confirmed**: Server stores enum `.name()` as values
- [ ] Visual styling — Does DropdownBox inherit Common.ui dark theme automatically?
- [ ] Popup positioning — Does the dropdown popup clip to page bounds?

---

### Pattern 4: Radius Presets -> Slider

**Current implementation** (CreateZoneWizardPage.java:231-258): 5 preset TextButtons (3, 5, 10, 15, 20) with `.Disabled` for selected + TextField for custom input (1-50 range, parsed and clamped).

```java
// Current: 5 buttons + text field
int[] RADIUS_PRESETS = {3, 5, 10, 15, 20};
for (int preset : RADIUS_PRESETS) {
    cmd.set("#Radius" + preset + ".Disabled", selectedRadius == preset);
}
cmd.set("#CustomRadiusInput.Value", String.valueOf(selectedRadius));
```

#### Proposed: Slider

```
// In .ui template
Slider #RadiusSlider {
    Min: 1;
    Max: 50;
    Step: 1;
    Value: 5;
    Style: $C.@DefaultSliderStyle;
    Anchor: (Height: 30, Width: 300);
}

Label #RadiusValue {
    Text: "5 chunks (80 blocks)";
    Style: (FontSize: 12, TextColor: #AAAAAA);
    Anchor: (Height: 20);
}
```

```java
// In Java
cmd.set("#RadiusSlider.Value", selectedRadius);

events.addEventBinding(ValueChanged, "#RadiusSlider",
    EventData.of("Button", "SetRadius")
             .append("@Radius", "#RadiusSlider.Value"),
    false);
```

**Impact**: Replaces 5 preset buttons + TextField + custom apply button with a single Slider. Matches Hytale's own tool reach/delay sliders exactly. Users can select any value 1-50 with a drag, rather than choosing from 5 presets or typing.

**Note**: The label showing chunk count ("5 chunks (80 blocks)") would update dynamically on each `ValueChanged` event.

#### Verification Needed
- [x] Slider with integer Step — **Confirmed**: Server uses int Min/Max/Step
- [ ] Value label — Is numeric readout built into `@DefaultSliderStyle` or separate Label needed?
- [ ] `cmd.set("#slider.Value", intValue)` — Needs testing for dynamic initial value

---

### Pattern 5: Tab Bars -> TabNavigation

**Current implementation**: Two distinct tab patterns exist:

#### Sub-page tabs (TextButton + `.Disabled`)
Used in: FactionRelationsPage, FactionSettingsTabsPage, AdminZonesPage

```java
// FactionRelationsPage.java:85-87
cmd.set("#TabAllies.Disabled", currentTab == Tab.ALLIES);
cmd.set("#TabEnemies.Disabled", currentTab == Tab.ENEMIES);
cmd.set("#TabPending.Disabled", currentTab == Tab.PENDING);
```

Content switching via visibility toggle or container rebuild.

#### Nav bars (AdminUI indexed selector pattern)
Used in: NavBarHelper, NewPlayerNavBarHelper, AdminNavBarHelper

```java
// NavBarHelper.java:66
cmd.appendInline("#HyperFactionsNavBar #NavBarButtons",
    "Group #NavCards { LayoutMode: Left; }");
// Then indexed: #NavCards[0] #NavActionButton, etc.
```

**Known limitation**: Nav bars have **no active tab highlighting** — the `currentPage` parameter exists but is unused (NavBarHelper.java line 41 comment).

#### Tab Instance Breakdown

| Page | Tab Count | Pattern | Content Switching |
|---|---|---|---|
| FactionRelationsPage | 3 (Allies/Enemies/Pending) | TextButton + Disabled | Container rebuild |
| FactionSettingsTabsPage | 3 (General/Permissions/Members) | TextButton + Disabled | Visibility toggle |
| AdminZonesPage | 3 (All/Safe/War) | TextButton + Disabled | Container rebuild |
| NavBarHelper (faction) | ~7 | MenuItem (AdminUI pattern) | Page navigation |
| NewPlayerNavBarHelper | ~5 | MenuItem (AdminUI pattern) | Page navigation |
| AdminNavBarHelper | ~7 | MenuItem (AdminUI pattern) | Page navigation |

#### Proposed: TabNavigation

For sub-page tabs (Relations, Settings, Zones):
```
TabNavigation #RelationTabs {
    LayoutMode: Left;

    MenuItem #TabAllies {
        Text: "ALLIES";
        Style: (Default: @SmallButtonLabelStyle, Hovered: @SmallCyanLabelStyle);
        SelectedStyle: @SmallCyanLabelStyle;
    }
    MenuItem #TabEnemies {
        Text: "ENEMIES";
        Style: (Default: @SmallButtonLabelStyle, Hovered: @SmallRedLabelStyle);
        SelectedStyle: @SmallRedLabelStyle;
    }
    MenuItem #TabPending {
        Text: "PENDING";
        Style: (Default: @SmallButtonLabelStyle, Hovered: @SmallButtonLabelStyle);
        SelectedStyle: @SmallCyanLabelStyle;
    }
}
```

**Impact**: Proper tab semantics. `SelectedTabChanged` event simplifies Java-side tab switching. Native `SelectedStyle` eliminates the `.Disabled` hack for visual feedback.

#### Verification Needed
- [ ] `SelectedTabChanged` event data — Does it provide tab index/ID?
- [ ] Programmatic tab selection — Can `cmd.set("#tabs.SelectedIndex", 0)` work?
- [ ] MenuItem icon support — Can MenuItem display icons (for faction-themed tabs)?

---

### Pattern 6: Recruitment Modal -> Inline DropdownBox

**Current implementation**: Separate modal page (`RecruitmentModalPage.java`, 150 lines) with a full page overlay containing:
- Current status label
- "OPEN" button (green)
- "INVITE ONLY" button (gold)
- "CANCEL" button

The modal is opened from faction settings and admin faction settings. On selection, it saves the value and navigates back.

#### Proposed: Inline DropdownBox on Settings Page

```java
// Replace modal flow with inline dropdown
List<DropdownEntryInfo> entries = List.of(
    new DropdownEntryInfo(LocalizableString.fromString("Open"), "open"),
    new DropdownEntryInfo(LocalizableString.fromString("Invite Only"), "invite_only")
);
cmd.set("#RecruitmentDropdown.Entries", entries);
cmd.set("#RecruitmentDropdown.Value", faction.open() ? "open" : "invite_only");
```

**Impact**: Eliminates `RecruitmentModalPage.java` (150 lines), `recruitment_modal.ui` (89 lines), and the modal navigation flow. Selection happens inline — no page transition needed.

---

### Pattern 7: ProgressBar for Power Display (Optional Polish)

**Current implementation**: Labels showing "Power: 45/100" text.

#### Proposed: ProgressBar + Label

```
ProgressBar #PowerBar {
    Value: 0.45;
    Direction: LeftToRight;
    Bar: (Color: #00FFFF);
    Background: (Color: #333333);
    Anchor: (Height: 8, Width: 150);
}
Label #PowerText {
    Text: "45 / 100";
    Style: (FontSize: 11, TextColor: #AAAAAA);
}
```

**Impact**: Low — visual polish only. Power level becomes instantly readable as a bar fill rather than requiring mental division.

---

### Pattern 8: Custom Button Styles -> Native Button Styles

**Current implementation**: HyperFactions defines **27 custom TextButtonStyle variants** in `shared/styles.ui` (378 lines). Every style manually assembles four state backgrounds from Common.ui + a custom LabelStyle for text color:

```
@ButtonStyle = TextButtonStyle(
  Default: (Background: $C.@DefaultSquareButtonDefaultBackground, LabelStyle: @ButtonLabelStyle),
  Hovered: (Background: $C.@DefaultSquareButtonHoveredBackground, LabelStyle: @ButtonLabelStyle),
  Pressed: (Background: $C.@DefaultSquareButtonPressedBackground, LabelStyle: @ButtonLabelStyle),
  Disabled: (Background: $C.@DefaultSquareButtonDisabledBackground, LabelStyle: @ButtonLabelStyle),
  Sounds: $C.@ButtonSounds,
);
// Repeated 27 times with different label colors (Cyan, Green, Red, Gold, etc.)
```

This produces buttons that use native background textures but **only differentiate by text color**. The native "RESET TO DEFAULTS" button visible in Hytale's Creative Mode Quick Settings (screenshot confirmed) uses a complete pre-built style token that includes matched backgrounds + text styling as a unified whole.

#### Native Complete Style Tokens Available

Two complete native button styles exist in Common.ui:

| Token | Usage | Visual |
|---|---|---|
| `$C.@DefaultTextButtonStyle` | Primary/active buttons and tabs | Native blue-tinted background, light text, full hover/pressed/disabled states |
| `$C.@SecondaryTextButtonStyle` | Secondary/inactive buttons and tabs | Muted gray background, dimmed text, full state variants |

**Server usage confirmed** in `EntitySpawnPage.java`:
```java
private static final Value<String> TAB_STYLE_ACTIVE =
    Value.ref("Common.ui", "DefaultTextButtonStyle");
private static final Value<String> TAB_STYLE_INACTIVE =
    Value.ref("Common.ui", "SecondaryTextButtonStyle");

// Applied dynamically — this WORKS unlike TextButton background styles:
commandBuilder.set("#TabNPC.Style", this.activeTab.equals("NPC")
    ? TAB_STYLE_ACTIVE : TAB_STYLE_INACTIVE);
```

**Key discovery**: Native complete styles **CAN be set dynamically** via `cmd.set("#button.Style", styleRef)`. This is different from the known crash when setting individual style properties like `.Style.Default.Background`. The complete token reference works because it replaces the entire style object at once.

#### Proposed: Hybrid Button Style Strategy

**Replace with native tokens** (standard action buttons):
```
// Before: 6 lines of custom style definition per button
TextButton #ResetBtn { Style: $S.@ButtonStyle; }

// After: Direct native reference
TextButton #ResetBtn { Style: $C.@DefaultTextButtonStyle; }
```

Buttons that should use `$C.@DefaultTextButtonStyle`:
- "RESET TO DEFAULTS" (admin zone settings)
- "SAVE" / "APPLY" / "CONFIRM" action buttons
- "CREATE" buttons (create zone, create faction)
- Primary navigation actions
- Any generic action button currently using `@ButtonStyle`

Buttons that should use `$C.@SecondaryTextButtonStyle`:
- "CANCEL" buttons
- Secondary actions alongside a primary button
- Inactive tab states (when not using TabNavigation)

**Keep custom styles** (semantic color buttons):
- `@CyanButtonStyle` — HyperFactions brand identity, page titles
- `@GreenButtonStyle` — Success/confirm actions (accept invite, enable)
- `@RedButtonStyle` — Danger/destructive actions (disband, kick, decline)
- `@GoldButtonStyle` — Special/elevated actions (leader actions)
- `@InvisibleButtonStyle` — Click overlays for expandable entries
- `@PlayerMarkerButtonStyle` — Map markers

#### Impact by Page

| Page | Buttons Using `@ButtonStyle` | Replace With |
|---|---|---|
| All pages | "CANCEL" buttons | `$C.@SecondaryTextButtonStyle` |
| Admin Dashboard | "RELOAD CONFIG" | `$C.@DefaultTextButtonStyle` |
| Admin Zone Settings | "RESET TO DEFAULTS" | `$C.@DefaultTextButtonStyle` |
| Create Zone Wizard | "CREATE ZONE" | `$C.@DefaultTextButtonStyle` |
| Faction Settings | "SAVE DESCRIPTION" | `$C.@DefaultTextButtonStyle` |
| Browse pages | Pagination (PREV/NEXT) | `$C.@DefaultTextButtonStyle` |
| All modals | Modal action buttons | Primary: `@DefaultTextButtonStyle`, Secondary: `@SecondaryTextButtonStyle` |

#### Dynamic Style Switching for Tabs

The EntitySpawnPage pattern enables a cleaner tab implementation even without TabNavigation:

```java
// Current: .Disabled hack
cmd.set("#TabAllies.Disabled", currentTab == Tab.ALLIES);

// Proposed: Native style switching
cmd.set("#TabAllies.Style",
    currentTab == Tab.ALLIES ? TAB_STYLE_ACTIVE : TAB_STYLE_INACTIVE);
```

This gives proper visual feedback (active tab looks distinct, not "disabled") while keeping the existing TextButton elements if TabNavigation proves problematic.

#### styles.ui Cleanup

After adopting native tokens, several custom styles become unnecessary:

| Current Style | Replacement | Can Delete? |
|---|---|---|
| `@ButtonStyle` | `$C.@DefaultTextButtonStyle` | Yes — standard gray button |
| `@SmallButtonStyle` | `$C.@SecondaryTextButtonStyle` (or keep for size) | Maybe — depends on font size match |
| `@DisabledButtonStyle` | Not needed — native Disabled state handles this | Yes |
| `@TealButtonStyle` (legacy) | Already deprecated | Yes |
| `@PurpleButtonStyle` (legacy) | Already deprecated | Yes |
| `@SmallTealButtonStyle` (legacy) | Already deprecated | Yes |
| `@SmallPurpleButtonStyle` (legacy) | Already deprecated | Yes |

**Estimated reduction**: 7 style definitions removed (~70 lines from `styles.ui`)

#### Verification Needed
- [x] `cmd.set("#button.Style", Value.ref("Common.ui", "DefaultTextButtonStyle"))` — **Confirmed**: EntitySpawnPage does this for tab switching
- [ ] Font size match — Does native `DefaultTextButtonStyle` use 13px like HyperFactions? If native uses a different size, `@SmallButtonStyle` may still be needed for compact layouts
- [ ] UI template reference syntax — In `.ui` files, is it `Style: $C.@DefaultTextButtonStyle;` or does it require a different syntax for complete style tokens?
- [ ] Native disabled appearance — When using `$C.@DefaultTextButtonStyle` with `.Disabled = true`, does the disabled state match HyperFactions' expectations?

---

### Pattern 9: Button-Triggered Search -> Native Real-Time Search

**Current implementation** (FactionBrowserPage.java): TextField + explicit "SEARCH" TextButton + "X" clear button. Search only executes on button click.

```java
// Current: search triggered by button click
events.addEventBinding(Activating, "#SearchBtn",
    EventData.of("Button", "Search")
             .append("@SearchQuery", "#SearchInput.Value"),
    false);
```

```
// Current UI template (faction_browser.ui)
TextField #SearchInput { FlexWeight: 1; Height: 32; Style: $C.@DefaultInputFieldStyle; }
TextButton #SearchBtn { Width: 80; Text: "SEARCH"; Style: $S.@SmallCyanButtonStyle; }
TextButton #ClearSearchBtn { Width: 30; Text: "X"; Visible: false; Style: $S.@SmallRedButtonStyle; }
```

#### Native Hytale Search Pattern

Every Hytale server page with search (CommandListPage, WarpListPage, EntitySpawnPage, PlaySoundPage, ParticleSpawnPage, etc.) uses **real-time search via `ValueChanged`**:

```java
// Native pattern: search on every keystroke
eventBuilder.addEventBinding(
    CustomUIEventBindingType.ValueChanged,
    "#SearchInput",
    EventData.of("@SearchQuery", "#SearchInput.Value"),
    false
);
```

No search button. No clear button. The native pattern is:
1. User types in TextField
2. `ValueChanged` fires on each keystroke
3. Server filters results immediately
4. Clearing the field (backspace to empty) shows all results

Hytale's `ServerFileBrowser` also implements **fuzzy matching** via `StringCompareUtil.getFuzzyDistance()` instead of simple substring matching, with results ranked by match quality and limited to a configurable `maxResults`.

#### Collapsible Search Icon (Client-Side Only)

The inventory screen shows a collapsible search pattern: magnifying glass icon collapses to save space, expands to full search field when clicked. This behavior is **client-side only** — no server-side API exists to control the collapse/expand animation. Plugin UIs cannot replicate this exact pattern.

However, the **functional equivalent** can be achieved with a simpler approach: just use the TextField directly (always visible) with the native `ValueChanged` pattern. This matches how Hytale's own server pages (WarpListPage, CommandListPage, etc.) implement search.

#### Proposed: Real-Time Search

```java
// Replace button-triggered search with real-time filtering
events.addEventBinding(
    CustomUIEventBindingType.ValueChanged,
    "#SearchInput",
    EventData.of("Button", "Search")
             .append("@SearchQuery", "#SearchInput.Value"),
    false
);
```

```
// Simplified UI template — remove SEARCH and X buttons
TextField #SearchInput {
    PlaceholderText: "Search factions...";
    Style: $C.@DefaultInputFieldStyle;
    Anchor: (Height: 32);
    FlexWeight: 1;
}
```

#### Debounce Strategy (Required)

Real-time `ValueChanged` fires on **every keystroke**. On a server with hundreds of factions, rebuilding and transmitting the filtered list on each key would cause lag spikes. A server-side debounce is essential.

**Proposed implementation**:
```java
// Per-player debounce timer
private ScheduledFuture<?> searchDebounce;
private static final long DEBOUNCE_MS = 250; // Wait 250ms after last keystroke

private void handleSearchChanged(Player player, String query) {
    // Cancel any pending search
    if (searchDebounce != null) {
        searchDebounce.cancel(false);
    }

    // Schedule new search after debounce period
    searchDebounce = scheduler.schedule(() -> {
        searchQuery = query.trim().toLowerCase();
        currentPage = 0;
        rebuildList();
    }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
}
```

**Why 250ms**: Fast enough to feel responsive (user sees results before finishing typing), slow enough to avoid rebuilding on rapid keystrokes. Hytale's own `ServerFileBrowser` processes on every event but caps results to `maxResults` — we should do both.

#### Additional Improvements
- **Fuzzy matching**: Replace `.contains()` with fuzzy distance scoring (`StringCompareUtil.getFuzzyDistance()`) for ranked results
- **Result limiting**: Cap displayed results (e.g., 20) to prevent UI lag on large servers
- **Empty query fast-path**: If query is empty, skip matching entirely and show all results

#### Pages Affected
- `FactionBrowserPage.java` — TextField + SEARCH + X buttons -> real-time TextField + debounce
- `NewPlayerBrowsePage.java` — Same pattern
- `FactionMembersPage.java` — **Add search** (currently has no search, see Pattern 10)
- `AdminFactionsPage.java` — Same pattern (if it has search)
- `faction_browser.ui` — Remove #SearchBtn and #ClearSearchBtn
- `newplayer/browse.ui` — Same

**Impact**: Removes 2 TextButtons per search page. Provides instant feedback as users type. Matches native Hytale UX. Debounce prevents performance issues.

---

### Pattern 10: Members Page — Add Search

**Current implementation**: The Members page (`FactionMembersPage.java`) shows a member count label and sort buttons (ROLE / ONLINE) but **no search functionality**. On a faction with many members, there's no way to quickly find a specific player.

**Screenshot confirms**: The members page shows only "1 members" count + sort buttons + member list. No search field exists.

#### Proposed: Add Real-Time Search to Members Page

```
// Updated faction_members.ui header row
Group {
    Anchor: (Height: 36, Bottom: 8);
    LayoutMode: Left;

    Label #MemberCount {
        Text: "0 members";
        Style: (FontSize: 14, TextColor: #888888, VerticalAlignment: Center);
        Anchor: (Width: 100);
    }

    $C.@TextField #SearchInput {
        PlaceholderText: "Search members...";
        Anchor: (Height: 30);
        FlexWeight: 1;
    }

    Group { Anchor: (Width: 8); }

    // Sort dropdown replaces ROLE/ONLINE buttons (Pattern 3)
    DropdownBox #SortDropdown { ... }
}
```

```java
// Real-time search with debounce (same pattern as Pattern 9)
events.addEventBinding(ValueChanged, "#SearchInput",
    EventData.of("Button", "Search")
             .append("@SearchQuery", "#SearchInput.Value"),
    false);
```

**Search matches against**: player name (primary), role name (secondary).

**Impact**: Completes the search story — all list pages (Browse, Members, Admin Factions, Admin Zones) have consistent search. Essential for factions with 20+ members.

---

### Pattern 11: TextField Text Positioning Fix

**Current implementation**: All 14 TextFields across HyperFactions use raw `TextField` elements with manually applied styling:

```
// Current pattern (all 14 instances)
TextField #SearchInput {
    FlexWeight: 1;
    Anchor: (Height: 32);
    Style: $C.@DefaultInputFieldStyle;
    Background: $C.@InputBoxBackground;
}
```

**Problem**: Text appears offset outside the input box boundary (confirmed via screenshot). The text "test" renders misaligned from the visual box border.

**Root cause**: HyperFactions uses raw `TextField` elements and manually applies `Style` + `Background` separately. The Hytale CustomUI documentation shows the correct pattern is to use the **`$C.@TextField` template** which is a complete pre-built element that includes proper padding, background, and text alignment as a unified whole.

- `$C.@DefaultInputFieldStyle` = **LabelStyle only** (font size, color, alignment)
- `$C.@InputBoxBackground` = **PatchStyle only** (background texture/9-slice border)
- `$C.@TextField` = **Complete template** (includes both + padding + layout properties)

By applying only the two components but not using the template, the padding that insets text from the box border is missing.

#### Proposed: Use Native `$C.@TextField` Template

```
// Before (current, broken):
TextField #SearchInput {
    FlexWeight: 1;
    Anchor: (Height: 32);
    Style: $C.@DefaultInputFieldStyle;
    Background: $C.@InputBoxBackground;
}

// After (native template):
$C.@TextField #SearchInput {
    FlexWeight: 1;
    Anchor: (Height: 32);
}
```

The native template automatically includes:
- Proper padding (text inset from box border)
- Correct background with 9-slice texture
- Matching text style
- Proper vertical alignment

#### Affected Files (14 TextFields across 10 files)

| File | TextField Count | Element IDs |
|---|---|---|
| `newplayer/create_step1.ui` | 2 | `#NameInput`, `#TagInput` |
| `newplayer/create_step2.ui` | 1 | `#DescriptionInput` |
| `newplayer/browse.ui` | 1 | `#SearchInput` |
| `faction/faction_browser.ui` | 1 | `#SearchInput` |
| `faction/set_relation_modal.ui` | 1 | `#SearchInput` |
| `shared/rename_modal.ui` | 1 | `#NameInput` |
| `shared/tag_modal.ui` | 1 | `#TagInput` |
| `shared/description_modal.ui` | 1 | `#DescriptionInput` |
| `admin/zone_rename_modal.ui` | 1 | `#NameInput` |
| `admin/create_zone_wizard.ui` | 2 | `#NameInput`, `#CustomRadiusInput` |

**Impact**: Simple find-and-replace fix across 10 files. Immediately fixes text positioning on all input fields. Matches native Hytale look.

---

### Pattern 12: Centralized Color System + Admin Style Config

**Current state**: Colors are hardcoded throughout the codebase with no centralization:
- **~660 hex color occurrences** across 62 Java files
- **~930 hex color occurrences** across 94 .ui template files
- **~40-50 unique colors** in total
- **Partial centralization only**: `CommandUtil.java` has 6 constants, `TeleportManager.java` duplicates 5 of them, `styles.ui` has ~15 label style constants
- **Inconsistencies**: Ally color is `#55FF55` in TerritoryInfo but `#AA00AA` in ChatManager. SafeZone uses both `#55FF55` and `#00CED1` in different contexts.

#### Proposed: Three-Layer Color Architecture

```
Layer 1: config/style.json          (admin-editable, runtime loaded)
    ↓ overrides defaults in
Layer 2: FactionTheme.java          (Java constants, compile-time defaults)
    ↓ referenced by
Layer 3: styles.ui + Java code      (UI templates + message formatting)
```

##### Layer 1: `config/style.json` (Server Admin Customization)

```json
{
    "brand": {
        "primary": "#55FFFF",
        "primaryBright": "#00FFFF",
        "secondary": "#00AAAA"
    },
    "status": {
        "success": "#55FF55",
        "error": "#FF5555",
        "warning": "#FFAA00"
    },
    "text": {
        "primary": "#FFFFFF",
        "secondary": "#AAAAAA",
        "muted": "#888888",
        "disabled": "#555555"
    },
    "roles": {
        "leader": "#FFD700",
        "officer": "#87CEEB",
        "member": "#AAAAAA",
        "recruit": "#888888"
    },
    "relations": {
        "own": "#55FF55",
        "ally": "#55FF55",
        "neutral": "#FFFF55",
        "enemy": "#FF5555",
        "wilderness": "#AAAAAA"
    },
    "zones": {
        "safe": "#55FF55",
        "war": "#FF5555"
    },
    "ui": {
        "backgroundPrimary": "#1a2a3a",
        "backgroundSecondary": "#0d1520",
        "backgroundTertiary": "#111a28",
        "accent": "#334455",
        "divider": "#00FFFF",
        "buttonLabel": "#bfcdd5"
    }
}
```

Server admins can customize any color to match their server's theme (e.g., a red-themed PvP server, a green-themed survival server). Missing keys fall back to defaults. Invalid hex values are ignored with a config warning.

##### Layer 2: `FactionTheme.java` (Centralized Java Constants)

```java
public class FactionTheme {
    // Singleton, loaded from config/style.json with defaults

    // Brand
    public String primary()       { return get("brand.primary", "#55FFFF"); }
    public String primaryBright() { return get("brand.primaryBright", "#00FFFF"); }
    public String secondary()     { return get("brand.secondary", "#00AAAA"); }

    // Status
    public String success() { return get("status.success", "#55FF55"); }
    public String error()   { return get("status.error", "#FF5555"); }
    public String warning() { return get("status.warning", "#FFAA00"); }

    // Text
    public String textPrimary()   { return get("text.primary", "#FFFFFF"); }
    public String textSecondary() { return get("text.secondary", "#AAAAAA"); }
    public String textMuted()     { return get("text.muted", "#888888"); }

    // Roles, Relations, Zones, UI...
    // All with defaults matching current hardcoded values
}
```

Replaces scattered constants in `CommandUtil.java`, `TeleportManager.java`, `HelpFormatter.java`, and all direct hex literals in Java code.

##### Layer 3: `styles.ui` Refactor

The `.ui` template styles currently hardcode colors inline:

```
// Current (hardcoded):
@CyanLabelStyle = LabelStyle(
    FontSize: 13, TextColor: #00FFFF, RenderBold: true, ...
);
```

After centralization, **Java code dynamically sets colors on UI elements** using theme values:

```java
// Java-side: apply theme colors to UI
cmd.set("#FactionName.Style.TextColor", theme.primaryBright());
cmd.set("#StatusLabel.Style.TextColor", theme.success());
```

For `.ui` templates, colors that **cannot be set dynamically** (static label styles in template definitions) remain in `styles.ui` using the default values. These serve as fallbacks — the Java code overrides them at page build time with theme values.

**Alternative approach**: If `cmd.set()` on LabelStyle.TextColor proves unreliable for all elements, generate `styles.ui` at plugin startup from the theme config (write the file with substituted colors, then reference it normally).

##### Color Inventory: Semantic Groups

| Group | Colors | Current Locations | After Centralization |
|---|---|---|---|
| **Brand** (3) | `#55FFFF`, `#00FFFF`, `#00AAAA` | 62 Java files, 30+ UI templates | `theme.primary()`, `theme.primaryBright()`, `theme.secondary()` |
| **Status** (3) | `#55FF55`, `#FF5555`, `#FFAA00` | 40+ Java files, 20+ UI templates | `theme.success()`, `theme.error()`, `theme.warning()` |
| **Text** (4) | `#FFFFFF`, `#AAAAAA`, `#888888`, `#555555` | Nearly every file | `theme.textPrimary()` through `theme.textDisabled()` |
| **Roles** (4) | `#FFD700`, `#87CEEB`, `#AAAAAA`, `#888888` | Member display code, UI templates | `theme.roleLeader()` through `theme.roleRecruit()` |
| **Relations** (5) | `#55FF55`, `#55FF55`, `#FFFF55`, `#FF5555`, `#AAAAAA` | Territory, chat, map code | `theme.relationOwn()` through `theme.relationWilderness()` |
| **Zones** (2) | `#55FF55`, `#FF5555` | Zone settings, map, display | `theme.zoneSafe()`, `theme.zoneWar()` |
| **UI backgrounds** (5) | `#1a2a3a`, `#0d1520`, `#111a28`, `#334455`, `#bfcdd5` | UI templates only | `theme.bgPrimary()` through `theme.buttonLabel()` |

**Total: ~26 semantic color tokens** replacing ~40-50 inconsistent hardcoded values across ~1,590 occurrences.

##### Inconsistencies Fixed

| Issue | Current | After |
|---|---|---|
| Ally color differs between territory and chat | `#55FF55` vs `#AA00AA` | `theme.relationAlly()` everywhere |
| SafeZone uses two different greens | `#55FF55` vs `#00CED1` | `theme.zoneSafe()` everywhere |
| `CommandUtil`, `TeleportManager`, `HelpFormatter` each define their own color constants | 3 separate files, partial overlap | Single `FactionTheme` class |
| UI template colors not overridable | Hardcoded in .ui files | Theme-driven via `cmd.set()` or generated styles |

##### Implementation Scope

**New files**:
- `config/FactionTheme.java` — Theme loader + accessors with defaults
- `config/style.json` — Default style configuration (generated on first run)

**Modified files** (bulk refactor):
- **62 Java files** — Replace hardcoded hex literals with `theme.xxx()` calls
- **styles.ui** — Refactor color definitions to use default theme values, add comments mapping to `style.json` keys
- **ConfigManager.java** — Load `style.json` on startup, provide `getTheme()` accessor

**This is the largest single change in the audit** but provides long-term maintainability and server admin customization.

#### Verification Needed
- [ ] `cmd.set("#label.Style.TextColor", hexColor)` — Confirmed working on TextButtons (AdminFactionSettingsPage uses this). Need to verify it works on Labels too.
- [ ] Performance of runtime theme loading — JSON parse once at startup, cached in `FactionTheme` singleton. Negligible.
- [ ] Hot-reload support — Can theme changes apply without server restart? Would require re-opening player GUIs. Consider `/f admin reload` including style refresh.

---

## Consolidated Impact Table

| Element | Instance Count | Pages Affected | Code Eliminated | Visual Impact |
|---|---|---|---|---|
| **CheckBoxWithLabel** | 45 | 4 | ~200 lines toggle logic, 3 divergent patterns -> 1 | **Highest** |
| **Native Button Styles** | ~20+ buttons | 10+ | 7 style definitions (~70 lines from styles.ui), cleaner per-button markup | **High** — matches native Hytale look exactly |
| **ColorPicker (faction)** | 3 pages (create + settings + admin) | 3 | ColorPickerPage.java (198 lines), color_picker.ui (261 lines), 16-button grids, modal flow | **High** |
| **ColorPicker (zones)** | 1 new page (zone properties tab) | 1 | New feature — zone settings page split into tabs | **High** — enables per-zone custom colors |
| **Page enlargements** | 4 pages resized | 4 | No code eliminated — layout changes for no-scroll ColorPicker | **High** — polished, no-scroll feel |
| **Slider** | 1 | 1 | 5 preset buttons + TextField + apply logic | **High** (matches Hytale exactly) |
| **Centralized Color System** | ~1,590 color occurrences | 62 Java + 94 UI files | Eliminates inconsistencies, enables admin customization | **High** — long-term maintainability |
| **TextField Fix** | 14 TextFields | 10 UI files | Simple template swap, fixes text positioning | **Medium** — visible bug fix |
| **Real-Time Search + Debounce** | 3-4 search bars (incl. new Members search) | 3-4 | 2 TextButtons per page (SEARCH + X), button event handlers | **Medium** — instant feedback, native UX |
| **Members Page Search** | 1 new search field | 1 | New feature — enables finding members in large factions | **Medium** — usability |
| **DropdownBox** | ~10 | 6 | ~100 lines button/event code, 25 TextButton elements | **Medium** |
| **TabNavigation** | ~6 | 6 | `.Disabled` tab hack, manual visibility toggle | **Medium** |
| **DropdownBox (recruitment)** | 2 | 2 | RecruitmentModalPage.java (150 lines), recruitment_modal.ui (89 lines) | **Medium** |
| **ProgressBar** | 2-3 | 2 | None (additive) | **Low** |

**Total elements replaced**: ~75+ TextButton workarounds -> native elements
**Total code eliminated**: ~800+ lines of workaround logic
**Style definitions cleaned up**: 7 custom styles removed (~70 lines)
**TextField fixes**: 14 input fields across 10 templates
**Color occurrences centralized**: ~1,590 hardcoded hex values -> ~26 semantic theme tokens
**New features**: Zone colors, Members search, admin style customization (`style.json`)

---

## Open Questions / Risks

### Must Verify Before Implementation

1. **CheckBox `.Value` dynamic setting** — `cmd.set("#checkbox.Value", true)` — Server code confirms this works on CheckBox. **Low risk.**

2. **CheckBox disabled state** — Zone settings have "LOCKED" flags. Can `CheckBox.Disabled` be set dynamically? TextButton `.Disabled` works, so likely yes. **Low risk.**

3. **ColorPicker value constraints** — If factions require one of 16 Minecraft color codes, ColorPicker's free-form gradient may need snapping logic. **Medium risk — design decision required.**

4. **DropdownBox entry population** — `cmd.set("#dropdown.Entries", list)` is confirmed from server code. **Low risk.**

5. **TabNavigation event data** — Does `SelectedTabChanged` provide tab index/ID? **Medium risk — need to test.**

6. **Slider value readout** — The Hytale screenshot shows numeric readout next to sliders. Is this built into `@DefaultSliderStyle` or does it need a separate Label? **Low risk — can always add Label.**

7. **Dark theme compatibility** — Do native elements (`CheckBox`, `DropdownBox`, `Slider`) automatically inherit the dark theme from Common.ui, or do they need custom styling to match HyperFactions' cyan/teal aesthetic? **Medium risk — may need custom styles.**

8. **Native button style font size** — Does `$C.@DefaultTextButtonStyle` use the same 13px font size as HyperFactions' custom styles? If it uses a larger size, small/compact buttons may still need custom definitions. **Low risk — can always keep `@SmallButtonStyle`.**

9. **Complete style token syntax in .ui files** — In templates, is it `Style: $C.@DefaultTextButtonStyle;` (same as background refs), or does it need different syntax? **Low risk — confirmed working via `cmd.set()` in Java code.**

10. **Search debouncing** — `ValueChanged` fires on every keystroke. **Debounce required** (250ms recommended). See Pattern 9 for implementation. **Low risk — straightforward `ScheduledFuture` pattern.**

11. **Collapsible search icon** — The inventory's magnifying-glass-to-search-field animation is **client-side only**. Not replicable in server-side plugins. **Confirmed limitation — no workaround needed.**

12. **`$C.@TextField` template padding** — Switching to the native template should fix text positioning. Need to verify padding values match expectations. **Low risk — native template is well-tested.**

13. **Theme hot-reload** — Can `style.json` changes apply without server restart? Would need re-opening player GUIs. Consider including in `/f admin reload`. **Medium risk — optional feature.**

14. **UI template color migration** — Can `cmd.set("#label.Style.TextColor", hexColor)` work on all element types? Confirmed on TextButtons. Need to verify on Labels and other elements. If not universal, may need to generate `styles.ui` from theme at startup. **Medium risk.**

### Known Limitations

- **TextButton styles cannot be changed dynamically** — Setting individual style properties (`.Style.Default.Background`) crashes. BUT complete style token references (`.Style = Value.ref("Common.ui", "DefaultTextButtonStyle")`) DO work. This distinction is critical.
- **Collapsible search animation is client-side only** — The inventory's magnifying glass expand/collapse cannot be replicated in plugin UIs.
- **Label HorizontalAlignment: Right crashes the client** — Unrelated to this audit but important context.
- **Anchor cannot be changed dynamically** — Native elements have fixed size once rendered.
- **TextField uses `.Value` not `.Text`** — Already known, confirmed for DropdownBox and Slider too.

---

## Files That Would Be Modified

### Eliminated Files (can be deleted)
- `shared/page/ColorPickerPage.java` (198 lines) — Replaced by inline ColorPicker
- `shared/page/RecruitmentModalPage.java` (150 lines) — Replaced by inline DropdownBox
- `shared/data/RecruitmentModalData.java` — No longer needed
- `shared/data/ColorPickerData.java` — No longer needed
- `shared/component/ColorPickerModal.java` — No longer needed
- `shared/color_picker.ui` (261 lines, 16 baked styles) — Replaced by native element
- `shared/recruitment_modal.ui` (89 lines) — Replaced by native element

### Data Model Changes (new feature: zone colors + hex faction colors)
- `data/Faction.java` — `color` field: single char (`"a"`) -> hex string (`"#55FF55"`)
- `data/Zone.java` — Add `String color` field (nullable, defaults to type color)
- `data/ZoneType.java` — Add `defaultColor()` method returning hex
- `storage/JsonFactionStorage.java` — Migration: map old char codes to hex on load
- `storage/JsonZoneStorage.java` — Add color field serialization, null default for existing zones
- Any code that reads `faction.color()` and maps to Minecraft codes — update to use hex directly

### New Files (centralized color system)
- `config/FactionTheme.java` — Theme loader + accessors with defaults (~26 semantic color tokens)
- `config/style.json` — Default style configuration (generated on first run, admin-editable)

### TextField Template Fixes (10 files, simple swap)
- `newplayer/create_step1.ui` — 2 TextFields -> `$C.@TextField` template
- `newplayer/create_step2.ui` — 1 TextField
- `newplayer/browse.ui` — 1 TextField
- `faction/faction_browser.ui` — 1 TextField
- `faction/set_relation_modal.ui` — 1 TextField
- `shared/rename_modal.ui` — 1 TextField
- `shared/tag_modal.ui` — 1 TextField
- `shared/description_modal.ui` — 1 TextField
- `admin/zone_rename_modal.ui` — 1 TextField
- `admin/create_zone_wizard.ui` — 2 TextFields

### UI Templates (modify to add native elements)

**Style definitions:**
- `shared/styles.ui` — Add CheckBox, Dropdown, Slider style definitions; remove 7 obsolete button styles

**Page resizing + ColorPicker integration (no-scroll goal):**
- `faction/settings_tabs.ui` — **Resize 600×620 -> 700×720**; TabNavigation for tab bar
- `faction/settings_general_content.ui` — APPEARANCE section expanded to ~220px with inline ColorPicker; DropdownBox for recruitment; remove TopScrolling
- `admin/admin_faction_settings.ui` — **Resize 720×560 -> 800×700**; inline ColorPicker in left column; remove TopScrolling from both columns
- `admin/admin_zone_settings.ui` — **Resize 720×680 -> 800×700**; split into Properties/Flags tabs; add ColorPicker on Properties tab
- `newplayer/create_step1.ui` — **Resize 500×520 -> 500×600**; ColorPicker replacing 16-button grid

**CheckBox conversions:**
- `admin/admin_zone_settings.ui` — 22 TextButton -> CheckBoxWithLabel (on FLAGS tab)
- `admin/admin_faction_settings.ui` — 11 TextButton -> CheckBoxWithLabel
- `admin/admin_dashboard.ui` — 1 Label+Button -> CheckBoxWithLabel
- `faction/settings_permissions_content.ui` — 11 TextButton -> CheckBoxWithLabel

**DropdownBox + Slider conversions:**
- `admin/create_zone_wizard.ui` — Slider for radius, DropdownBox for type/method/flags
- `admin/admin_factions.ui` — DropdownBox for sort
- `faction/faction_members.ui` — DropdownBox for sort
- `faction/faction_browser.ui` — DropdownBox for sort; remove SEARCH + X buttons (real-time search)
- `newplayer/browse.ui` — DropdownBox for sort; remove SEARCH + X buttons

**TabNavigation conversions:**
- `admin/admin_zones.ui` — TabNavigation for filter tabs
- `faction/faction_relations.ui` — TabNavigation for tabs

**Native button style updates:**
- Multiple `.ui` files — Replace `$S.@ButtonStyle` with `$C.@DefaultTextButtonStyle` on standard action buttons
- Multiple `.ui` files — Replace `$S.@ButtonStyle` / `$S.@SmallButtonStyle` with `$C.@SecondaryTextButtonStyle` on cancel/secondary buttons

**May simplify:**
- `newplayer/create_step2.ui` — Color already selected inline on step 1

### Java Page Classes (modify event handling)
- `AdminZoneSettingsPage.java` — Major redesign: split into Properties/Flags tabs; CheckBox ValueChanged handling on Flags tab; ColorPicker on Properties tab
- `AdminFactionSettingsPage.java` — CheckBox + inline ColorPicker (replaces modal flow) + inline DropdownBox for recruitment
- `AdminDashboardPage.java` — CheckBox handling
- `AdminZonesPage.java` — TabNavigation handling; update zone color display
- `AdminFactionsPage.java` — DropdownBox handling
- `CreateZoneWizardPage.java` — Slider + DropdownBox handling
- `FactionSettingsTabsPage.java` — CheckBox + TabNavigation + inline ColorPicker + inline DropdownBox for recruitment
- `FactionRelationsPage.java` — TabNavigation handling
- `FactionMembersPage.java` — DropdownBox handling
- `FactionBrowserPage.java` — DropdownBox + real-time search with debounce (ValueChanged)
- `FactionMembersPage.java` — DropdownBox + **add search** (new feature) with debounce
- `NewPlayerBrowsePage.java` — DropdownBox + real-time search with debounce (ValueChanged)
- `CreateFactionStep1Page.java` — Native ColorPicker replacing 16-button grid
- `CreateFactionStep2Page.java` — Simplified (no color state preservation needed)

### Centralized Color Migration (bulk refactor, Batch 6)
- `config/FactionTheme.java` — New file: theme loader with ~26 semantic color tokens
- `config/style.json` — New file: admin-editable style configuration
- `ConfigManager.java` — Load style.json on startup, provide `getTheme()` accessor
- **62 Java files** — Replace hardcoded hex literals with `theme.xxx()` calls
- **94 UI template files** — Replace hardcoded TextColor/Background.Color with theme values or generated styles
- `CommandUtil.java` — Remove 6 color constants (moved to FactionTheme)
- `TeleportManager.java` — Remove 5 duplicate color constants
- `HelpFormatter.java` — Remove 4 java.awt.Color constants

### Shared/Utility
- `NavBarHelper.java` — TabNavigation if upgrading main nav bars
- `AdminNavBarHelper.java` — Same
- `NewPlayerNavBarHelper.java` — Same
- `GuiManager.java` — Remove `openColorPicker()` and `openRecruitmentModal()` methods

---

## Recommended Implementation Order

Each step is independently testable and rollback-safe:

| Order | Change | Effort | Impact | Pages |
|---|---|---|---|---|
| 1 | CheckBoxWithLabel on AdminZoneSettingsPage (22 toggles) | Medium | **Highest** — biggest single page improvement | 1 |
| 2 | CheckBoxWithLabel on FactionSettingsTabsPage (11 toggles) | Medium | High — user-facing permission controls | 1 |
| 3 | CheckBoxWithLabel on AdminFactionSettingsPage (11 toggles) | Low | High — reuse pattern from step 2 | 1 |
| 4 | Native button styles on action buttons (`@DefaultTextButtonStyle` / `@SecondaryTextButtonStyle`) | Low | **High** — immediate native look across all pages | 10+ |
| 5 | Slider on CreateZoneWizardPage (1 slider) | Low | High — quick win, matches Hytale exactly | 1 |
| 6 | Faction color model migration (char -> hex) | Low | Foundation — required before ColorPicker work | Data layer |
| 7 | ColorPicker on CreateFactionStep1Page (500×600) | Medium | High — eliminates 16-button grid + modal | 1 |
| 8 | ColorPicker on Faction Settings General tab (700×720) | Medium | High — inline picker, no-scroll layout | 1 |
| 9 | ColorPicker on Admin Faction Settings (800×700) | Medium | High — inline picker, no-scroll layout | 1 |
| 10 | Zone color data model + storage migration | Medium | Foundation — required before zone ColorPicker | Data layer |
| 11 | Zone Settings page redesign: Properties/Flags tabs (800×700) | High | **High** — zone ColorPicker + tabbed layout | 1 |
| 12 | TextField fix — swap to `$C.@TextField` template (14 fields, 10 files) | Low | Medium — fixes visible text positioning bug | 10 |
| 13 | Real-time search + debounce on browse pages | Low | Medium — instant feedback, removes buttons | 2-3 |
| 14 | Add search to Members page (new feature) | Low | Medium — usability for large factions | 1 |
| 15 | DropdownBox on browse/sort patterns (4 pages) | Medium | Medium — repeated pattern, do all at once | 4 |
| 16 | DropdownBox on CreateZoneWizardPage selections | Low | Medium — reuse pattern from step 15 | 1 |
| 17 | Inline DropdownBox replacing RecruitmentModal | Low | Medium — eliminates modal page | 2 |
| 18 | TabNavigation on Relations + Settings tabs | Medium | Medium — proper tab semantics | 2 |
| 19 | TabNavigation on AdminZones filter tabs | Low | Low-Medium — reuse from step 18 | 1 |
| 20 | CheckBoxWithLabel on AdminDashboardPage (1 toggle) | Low | Low — trivial, reuse from step 1 | 1 |
| 21 | Centralized color system — `FactionTheme.java` + `style.json` | High | **High** — foundation for all color usage | 62+ Java files |
| 22 | Migrate Java code to use `FactionTheme` color tokens | High | High — eliminates ~660 hardcoded hex values in Java | 62 files |
| 23 | Migrate UI templates to use theme-driven colors | Medium | Medium — eliminates ~930 hardcoded hex values in .ui | 94 files |
| 24 | styles.ui cleanup (remove 7 obsolete style definitions) | Low | Low — code hygiene | 1 |
| 25 | ProgressBar on Dashboard power display | Low | Low — optional polish | 1-2 |
| 26 | TabNavigation on main nav bars | Medium | Medium — biggest refactor, least urgency | 3 |

**Suggested batches**:
- **Batch 1 (steps 1-5)**: CheckBox + native button styles + Slider. Tests native form elements + styling. Low coupling.
- **Batch 2 (steps 6-9)**: Faction ColorPicker — data migration + inline pickers on all 3 faction pages. Eliminates color modal.
- **Batch 3 (steps 10-11)**: Zone ColorPicker — new zone color feature + zone settings page redesign with tabs.
- **Batch 4 (steps 12-17)**: TextField fix + search (with debounce + Members page) + DropdownBox. Bug fixes + native UX patterns.
- **Batch 5 (steps 18-20)**: TabNavigation. Proper tab semantics across remaining pages.
- **Batch 6 (steps 21-26)**: Centralized color system + cleanup + polish. Biggest scope, highest long-term impact. Best done last so all other UI changes are stable before the color migration.

---

## Appendix: Current styles.ui Analysis

`shared/styles.ui` (378 lines) defines 27 button style variants. Key observations:

- All styles reference `$C = "../../Common.ui"` for background textures
- Button styles use `Default`/`Hovered`/`Pressed`/`Disabled` states from Common.ui backgrounds
- Color variants: Button (default gray), Cyan, Green, Red, Gold, SafeZone, WarZone, Disabled, Invisible, PlayerMarker, TocLink
- Each color has regular + small variant
- Legacy aliases exist: @TealButtonStyle, @PurpleButtonStyle (deprecated)

### Styles to Remove (replaced by native tokens)

| Style | Lines | Replacement |
|---|---|---|
| `@ButtonStyle` | 114-120 | `$C.@DefaultTextButtonStyle` |
| `@DisabledButtonStyle` | 195-201 | Native `.Disabled` state on any button |
| `@TealButtonStyle` (legacy) | 290-296 | Already deprecated |
| `@PurpleButtonStyle` (legacy) | 298-304 | Already deprecated |
| `@SmallTealButtonStyle` (legacy) | 322-328 | Already deprecated |
| `@SmallPurpleButtonStyle` (legacy) | 330-336 | Already deprecated |
| `@ButtonLabelStyle` | 29-35 | Included in native `DefaultTextButtonStyle` |

### Styles to Keep (semantic color, no native equivalent)

| Style | Purpose | Why Keep |
|---|---|---|
| `@CyanButtonStyle` + Small | HyperFactions brand identity | No native cyan variant exists |
| `@GreenButtonStyle` + Small | Success/accept actions | No native green variant |
| `@RedButtonStyle` + Small | Danger/decline actions | No native red variant |
| `@GoldButtonStyle` | Leader/elevated actions | No native gold variant |
| `@SafeZoneButtonStyle` + Small | Zone type indicator | Semantic coloring |
| `@WarZoneButtonStyle` + Small | Zone type indicator | Semantic coloring |
| `@InvisibleButtonStyle` | Click overlays for expandable entries | Unique transparent pattern |
| `@PlayerMarkerButtonStyle` | Map markers | Unique white-on-transparent |
| `@TocLinkButtonStyle` | Help system links | Unique hover behavior |

### New Styles to Add

| Style | Purpose |
|---|---|
| `@CheckBoxStyle` | Cyan/teal themed checkbox (checked=cyan, unchecked=gray) |
| `@DropdownStyle` | Dark theme dropdown (or use native default) |
| `@SliderStyle` | Slider with cyan handle (or use `$C.@DefaultSliderStyle`) |

### Estimated styles.ui After Changes
- **Current**: 27 custom styles, ~378 lines
- **Removed**: 7 styles (~70 lines)
- **Added**: 3 styles (~30 lines)
- **After**: 23 styles, ~338 lines + cleaner organization
