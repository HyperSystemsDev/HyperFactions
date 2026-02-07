# HyperFactions GUI Audit: Native Hytale UI Alignment

**Date**: 2026-02-05
**Updated**: 2026-02-06 (Batch 1 complete — TextField + button styles + styles.ui cleanup)
**Status**: Batch 1 complete — Create Faction page + TextField + button migration done
**Scope**: All HyperFactions GUI pages vs. native Hytale CustomUI elements

---

## Executive Summary

HyperFactions uses TextButton workarounds across its GUI where native Hytale elements would provide better UX. The Create Faction page has been fully redesigned as the first implementation. Remaining pages still need migration.

**Completed** (since v0.6.2):
- Create Faction page: merged 2-step wizard into single 950x650 two-column page
- Native `$C.@TextField` template on Create Faction inputs (3 instances)
- Native `$C.@DefaultTextButtonStyle` on Create Faction buttons
- Inline ColorPicker with `$C.@DefaultColorPickerStyle` on Create Faction
- Territory permission toggles at creation time
- Element test page (`/f admin testgui`) confirming all native elements work
- Deleted: `CreateFactionStep1Page.java`, `CreateFactionStep2Page.java`, `create_step1.ui`, `create_step2.ui`

**Completed** (Batch 1 — 2026-02-06):
- **TextField migration**: All 9 raw TextFields across 8 files migrated to `$C.@TextField` template
- **Button style migration**: 211 non-destructive buttons → `$C.@SecondaryTextButtonStyle` (muted gray), ~45 destructive buttons → `$C.@CancelTextButton` template (red/pink cancel)
- **styles.ui cleanup**: Removed 20 obsolete style definitions (402 → 251 lines)
- **$C import fixes**: Added missing `$C = "../../Common.ui"` imports to 8 appendable template files

**Remaining work**:
1. **45 toggle buttons** across 4 pages -> **CheckBoxWithLabel** (native boolean toggle)
2. **ColorPicker on 2 settings pages** (faction settings + admin settings still use modal)
3. **Sort buttons on browse/member pages** -> **DropdownBox** (native selection)
4. **3 button-triggered searches** -> **real-time ValueChanged search** with debounce
5. **Faction color model** still single-char codes -> hex strings
6. **Slider** for zone radius (currently 5 preset buttons + TextField)
7. **ColorPickerPage + RecruitmentModalPage** still exist (used by settings pages)

### Test Page Verification Results

All native elements confirmed working in-game via `/f admin testgui` (commit `8da5387`):

| Element | Status | Notes |
|---|---|---|
| `$C.@DefaultTextButtonStyle` | **Works** | Native blue-tinted button, full states |
| `$C.@SecondaryTextButtonStyle` | **Works** | Muted gray button, full states |
| `$C.@TextField` | **Works** | Proper padding/alignment, accepts inline Style |
| `Slider` + `$C.@DefaultSliderStyle` | **Works** | Handle works, **track invisible** (no custom styling possible) |
| `$C.@CheckBoxWithLabel` | **Works** | `@Text`/`@Checked` params, child selector for `.Value`/`.Disabled` |
| `DropdownBox` (bare) | **Works** | Entries from Java, opens downward |
| `$C.@DropdownBox` + `$C.@DefaultDropdownBoxStyle` | **Works** | Styled with arrow, but opens **sideways** |
| `ColorPicker` + `$C.@DefaultColorPickerStyle` | **Works** | **Style mandatory** (crash without it), `DisplayTextField: true` works |
| `ProgressBar` | **Works** | `Bar`/`Background` PatchStyle, no Style needed |
| `TimerLabel` | **Works** | Shows 00:00 by default |
| `ItemIcon` / `ItemSlot` | **Works** | `ItemId` property |
| `Value.ref()` style application | **Works** | Apply Common.ui styles from Java |
| `$C.@CancelTextButton` | **Works** | Red/pink cancel template, `@Text` param |
| `$C.@TertiaryTextButton` | **Works** | Outlined/bordered template |
| `$C.@SecondaryTextButton` | **Works** | Secondary style template |
| `$C.@SmallSecondaryTextButton` | **Works** | Small variant |
| `$C.@SmallTertiaryTextButton` | **Works** | Small variant |
| `$C.@NumberField` | **Renders** | Default "0", but `.Value` **not settable from Java** (crashes) |
| `$C.@Container` | **Works** | With `#Title`/`#Content` structure |

**Known limitations discovered:**
- **Slider track**: No custom styling possible. `$C.@DefaultSliderStyle` is a built-in client asset. Track is invisible. No mod has custom slider styling.
- **CheckBox requires template**: Native `CheckBox` without Style crashes. Must use `$C.@CheckBoxWithLabel`.
- **CheckBox `.Disabled`**: Must target inner element: `#CB #CheckBox.Disabled` (not `#CB.Disabled`).
- **DropdownBox template opens sideways**: `$C.@DropdownBox` with `$C.@DefaultDropdownBoxStyle` opens sideways. Bare `DropdownBox` opens downward.
- **TabNavigation + TabButton**: Crashes (needs unknown style/setup). **Cannot use** — stick with TextButton tabs.
- **NumberField `.Value`**: Not settable from Java. Inner element selector unknown.

---

## Completed Work (Confirmed via Git)

### Create Faction Page Redesign (commit `42267d9`)

Merged 2-step wizard into single 950x650 two-column page:

**Left column** (470px): Preview card, name/tag inputs (`$C.@TextField`), description/recruitment
**Right column** (flex): ColorPicker (`$C.@DefaultColorPickerStyle`, 200px), territory permissions (10 TextButton toggles), CREATE button (`$C.@DefaultTextButtonStyle`)

**Files created:**
- `newplayer/create_faction.ui` (410 lines) — two-column layout
- `CreateFactionPage.java` (490 lines) — unified page with permission toggles

**Files deleted:**
- `CreateFactionStep1Page.java` (239 lines)
- `CreateFactionStep2Page.java` (376 lines)
- `create_step1.ui` (189 lines)
- `create_step2.ui` (187 lines)

**Net reduction**: 615 lines deleted, single-page UX

### Element Test Page (commit `8da5387`)

Research page at `/f admin testgui` testing all native elements. Added flat button styles to `styles.ui`.

---

## Remaining Improvements

### 1. CheckBoxWithLabel for Toggles (45 instances, 4 pages)

**Current**: TextButtons displaying "ON"/"OFF" text with manual `LabelStyle.TextColor` color swaps.

**Three inconsistent patterns exist:**

| Page | Toggles | Pattern |
|---|---|---|
| AdminZoneSettingsPage | 22 flags | `buildFlagToggle()`, text "ON"/"OFF"/"N/A" |
| AdminFactionSettingsPage | 11 perms | `buildToggle()`, text "ON"/"OFF"/"ON (LOCKED)", green/red/gray colors |
| FactionSettingsTabsPage | 11 perms | `buildToggle()`, text "ON"/"OFF"/"LOCKED", fresh page pattern |
| AdminDashboardPage | 1 bypass | Label + Button split, "ENABLE"/"DISABLE" |

**Proposed**: Replace with `$C.@CheckBoxWithLabel`:

```java
// New pattern (confirmed working via test page)
cmd.set("#" + elementId + " #CheckBox.Value", currentValue);
cmd.set("#" + elementId + " #CheckBox.Disabled", locked);
```

**Key learnings from test page:**
- Must use child selector: `#CB #CheckBox.Value` / `#CB #CheckBox.Disabled`
- Direct `#CB.Value` or `#CB.Disabled` crashes
- Template params: `@Text`, `@Checked`

**Impact**: Eliminates color swap logic, "ON"/"OFF" text management, and 3 divergent patterns -> 1 consistent pattern.

---

### 2. ColorPicker on Remaining Settings Pages

**Current state**: CreateFaction page has inline ColorPicker (done). But faction settings and admin settings still open `ColorPickerPage.java` (16-button modal with single-char codes).

**Files still using old color picker:**
- `shared/page/ColorPickerPage.java` (198 lines) — 16-button grid modal
- `shared/data/ColorPickerData.java` — modal data
- `shared/component/ColorPickerModal.java` — modal wrapper
- `shared/color_picker.ui` (261 lines, 16 baked style definitions)
- Referenced from: `FactionSettingsTabsPage`, `AdminFactionSettingsPage`

**Faction color model**: Still uses single-char codes ("0"-"f"). `CreateFactionPage.java` converts via `hexToNearestColorCode()` — snaps arbitrary ColorPicker hex to nearest of 16 codes.

#### 2a. Faction Settings General Tab — Inline ColorPicker

**Current**: 600x620, "CHANGE" button opens modal. Proposed: Enlarge to **700x720**, inline ColorPicker (220px section).

#### 2b. Admin Faction Settings — Inline ColorPicker

**Current**: 720x560, "CHANGE" button opens modal. Proposed: Enlarge to **800x700**, inline ColorPicker in left column.

#### 2c. Data Model Migration (prerequisite)

Migrate `Faction.color` from single char (`"a"`) to hex string (`"#55FFFF"`):
- `Faction.java` — change field type
- `JsonFactionStorage.java` — migration: `"a"` -> `"#55FF55"`, etc.
- Remove `hexToNearestColorCode()` from `CreateFactionPage.java`
- Remove `ColorPickerPage.java`, `ColorPickerData.java`, `ColorPickerModal.java`, `color_picker.ui`

#### 2d. Zone Colors (new feature, lower priority)

Add optional `color` field to `Zone.java`. Default to type colors (safe=`#55FF55`, war=`#FF5555`). Admin-customizable via zone settings page. Requires zone settings page redesign (tabbed layout).

**ColorPicker verified behavior:**
- `Style: $C.@DefaultColorPickerStyle;` is **mandatory** (crash without it)
- `DisplayTextField: true` shows hex input
- `.Value` returns `#RRGGBBAA` (strip to 7 chars for `#RRGGBB`)
- `ValueChanged` fires as user drags color
- Minimum 200px+ height for usable square gradient

---

### 3. TextField Template Migration — COMPLETED (Batch 1)

All 9 raw TextFields migrated to `$C.@TextField` template across 8 files. Text positioning fixed.

| File | TextFields | Element IDs |
|---|---|---|
| `newplayer/browse.ui` | 1 | `#SearchInput` |
| `faction/faction_browser.ui` | 1 | `#SearchInput` |
| `faction/set_relation_modal.ui` | 1 | `#SearchInput` |
| `shared/rename_modal.ui` | 1 | `#NameInput` |
| `shared/tag_modal.ui` | 1 | `#TagInput` |
| `shared/description_modal.ui` | 1 | `#DescInput` |
| `admin/zone_rename_modal.ui` | 1 | `#NameInput` |
| `admin/create_zone_wizard.ui` | 2 | `#NameInput`, `#CustomRadiusInput` |

---

### 4. Native Button Styles Migration — COMPLETED (Batch 1)

All custom `$S.@` button styles replaced with native equivalents:

**Non-destructive buttons (211 replacements across 66 files):**
- `$S.@ButtonStyle`, `$S.@CyanButtonStyle`, `$S.@GreenButtonStyle`, `$S.@GoldButtonStyle` → `$C.@SecondaryTextButtonStyle`
- `$S.@SmallButtonStyle`, `$S.@SmallCyanButtonStyle`, `$S.@SmallGreenButtonStyle` → `$C.@SecondaryTextButtonStyle`

**Destructive buttons (~45 conversions across 28 files):**
- `$S.@RedButtonStyle`, `$S.@SmallRedButtonStyle` → `$C.@CancelTextButton` template
- Element type changed from `TextButton` to `$C.@CancelTextButton`, text syntax from `Text:` to `@Text =`

**styles.ui cleanup (402 → 251 lines):**
- Removed 10 obsolete button styles and 10 obsolete label styles
- Kept: InvisibleButtonStyle, DisabledButtonStyle, SafeZone/WarZone, SmallTeal/SmallPurple, FlatRed/FlatDarkRed, PlayerMarkerButtonStyle
- Also kept: CyanButtonStyle, GreenButtonStyle, RedButtonStyle, GoldButtonStyle (referenced by test page)

**$C import fixes:**
- 8 appendable template files had `$C.@` references added without `$C` import: dashboard_action_btn.ui, faction_browse_entry.ui, relation_btn_accept.ui, relation_btn_ally.ui, relation_btn_neutral.ui, relation_set_btn.ui, settings_members_content.ui, newplayer_faction_entry.ui

**Note:** `settings_permissions_content.ui` has 11 permission toggle buttons converted to `$C.@CancelTextButton`. These are DENY/ALLOW toggles — may need reverting to `$C.@SecondaryTextButtonStyle` if Java code dynamically swaps their style.

---

### 5. DropdownBox for Sort (8 buttons → 4 dropdowns, 4 pages)

**Current**: TextButtons with `.Disabled = true` for active sort option. Consistent pattern across browse/list pages.

| Page | Sort Options | Current Pattern |
|---|---|---|
| FactionBrowserPage | POWER, NAME, MEMBERS (3) | TextButton + `.Disabled` |
| NewPlayerBrowsePage | POWER, NAME, MEMBERS (3) | TextButton + `.Disabled` |
| FactionMembersPage | ROLE, LAST_ONLINE (2) | TextButton + `.Disabled` |
| AdminFactionsPage | POWER, NAME, MEMBERS (3) | TextButton + `.Disabled` |

**Proposed**: Replace sort buttons with bare `DropdownBox` (not `$C.@DropdownBox` — opens sideways):

```
// .ui file:
DropdownBox #SortDropdown {
    Anchor: (Height: 30, Width: 150);
}
```

```java
// Java:
List<DropdownEntryInfo> sortEntries = List.of(
    new DropdownEntryInfo(LocalizableString.fromString("Power"), "POWER"),
    new DropdownEntryInfo(LocalizableString.fromString("Name"), "NAME"),
    new DropdownEntryInfo(LocalizableString.fromString("Members"), "MEMBERS")
);
cmd.set("#SortDropdown.Entries", sortEntries);
cmd.set("#SortDropdown.Value", sortMode.name());
```

**Note**: Use bare `DropdownBox` (opens downward) — NOT `$C.@DropdownBox` template (opens sideways).

**Not included**: CreateZoneWizardPage's Type/Method/Radius/Flags buttons stay as TextButtons — they have visual descriptions alongside each option that work better as button groups.

---

### 6. Real-Time Search with Debounce (3 pages + 1 new)

**Current**: All 3 browse pages use button-triggered search (TextField + SEARCH button + X clear button):

| Page | Current Pattern |
|---|---|
| FactionBrowserPage | `Activating` on `#SearchBtn` |
| NewPlayerBrowsePage | `Activating` on `#SearchBtn` |
| SetRelationModalPage | `Activating` on `#SearchBtn` |
| FactionMembersPage | **No search at all** |

**Proposed**: Replace with `ValueChanged` on TextField (matching Hytale's native pattern):

```java
events.addEventBinding(
    CustomUIEventBindingType.ValueChanged,
    "#SearchInput",
    EventData.of("Button", "Search")
             .append("@SearchQuery", "#SearchInput.Value"),
    false);
```

**Debounce required** (250ms) — `ValueChanged` fires every keystroke.

**New feature**: Add search to FactionMembersPage for finding players in large factions.

**Impact**: Removes 2 TextButtons per page (SEARCH + X), provides instant feedback.

---

### 7. Slider for Zone Radius (1 instance)

**Current**: 5 preset TextButtons (3, 5, 10, 15, 20) + TextField for custom input in CreateZoneWizardPage.

**Proposed**: Replace with native Slider:

```
Slider #RadiusSlider {
    Min: 1; Max: 50; Step: 1; Value: 5;
    Style: $C.@DefaultSliderStyle;
    Anchor: (Height: 30);
}
```

**Known limitation**: Slider track is invisible (confirmed via test page). Handle works fine. This is acceptable for a radius selector — the numeric label beside it shows the value.

---

### 8. Recruitment Modal -> Inline DropdownBox

**Current**: `RecruitmentModalPage.java` (150 lines) opens as separate page with Open/Invite Only buttons.

**Proposed**: Replace with inline DropdownBox on settings pages:

```java
List<DropdownEntryInfo> entries = List.of(
    new DropdownEntryInfo(LocalizableString.fromString("Open"), "open"),
    new DropdownEntryInfo(LocalizableString.fromString("Invite Only"), "invite_only")
);
cmd.set("#RecruitmentDropdown.Entries", entries);
```

**Note**: CreateFactionPage already handles recruitment inline with TextButtons. This change affects `FactionSettingsTabsPage` and `AdminFactionSettingsPage` only.

**Files eliminated**: `RecruitmentModalPage.java`, `RecruitmentModalData.java`, `recruitment_modal.ui`

---

### 9. Tab Bars (stick with TextButton pattern)

**Original proposal**: Use native `TabNavigation + MenuItem`.

**Updated**: TabNavigation + TabButton **crashes** (confirmed via test page). Stick with the current TextButton + `.Disabled` tab pattern. Can improve with `Value.ref()` style switching instead of `.Disabled` hack:

```java
// Better tab visual (confirmed working via test page)
cmd.set("#TabAllies.Style", Value.ref("Common.ui",
    currentTab == Tab.ALLIES ? "DefaultTextButtonStyle" : "SecondaryTextButtonStyle"));
```

This gives active/inactive tab visual feedback without the "disabled" appearance.

---

### 10. ProgressBar for Power Display (optional polish)

**Current**: Labels showing "Power: 45/100" text.

**Proposed**: Add ProgressBar alongside label:

```
ProgressBar #PowerBar {
    Value: 0.45;
    Bar: (Color: #00FFFF);
    Background: (Color: #333333);
    Anchor: (Height: 8);
}
```

**Impact**: Low — visual polish only. Confirmed working via test page.

---

### 11. Centralized Color System (long-term)

**Current**: ~660 hex color occurrences across 62 Java files, ~930 across 94 UI files. Partial centralization in `CommandUtil.java` (6 constants) and `TeleportManager.java` (5 duplicates). Inconsistencies exist (ally color differs between territory and chat).

**Proposed**: `FactionTheme.java` + `config/style.json` with ~26 semantic color tokens. Server admins can customize colors to match their server theme.

**This is the largest change** — best done last after all other UI improvements are stable.

---

## Verified Element Behavior (from test page)

### What works

| Element | Template/Style | Settable from Java |
|---|---|---|
| TextButton | `$C.@DefaultTextButtonStyle`, `$C.@SecondaryTextButtonStyle` | `.Text`, `.Disabled`, `.Style` (via `Value.ref()`) |
| TextButton | `$C.@CancelTextButton`, `$C.@TertiaryTextButton`, etc. | `.Text` via `@Text` param |
| TextField | `$C.@TextField` | `.Value` (NOT `.Text`) |
| Slider | `$C.@DefaultSliderStyle` | `.Value`, `Min`, `Max`, `Step` |
| CheckBox | `$C.@CheckBoxWithLabel` | `#id #CheckBox.Value`, `#id #CheckBox.Disabled` |
| DropdownBox | bare (no template) | `.Entries`, `.Value` |
| ColorPicker | `$C.@DefaultColorPickerStyle` (**mandatory**) | `.Value` (set from Java, NOT in .ui) |
| ProgressBar | no Style needed | `.Value` (0.0-1.0), `Bar`, `Background` |
| Group | n/a | `Background: (Color: #hex)` |
| Label | inline Style | `.Text`, `.TextSpans`, `.Style.TextColor` |

### What crashes

| Element | Issue |
|---|---|
| `TabNavigation + TabButton` | NullReferenceException — needs unknown style/setup |
| `CheckBox` (native, no template) | NullReferenceException — must use `$C.@CheckBoxWithLabel` |
| `ColorPicker` without Style | NullReferenceException — `$C.@DefaultColorPickerStyle` mandatory |
| `#CB.Disabled` on CheckBoxWithLabel | "selector doesn't match" — use `#CB #CheckBox.Disabled` |
| `#NumberField.Value` | "couldn't set value" — inner selector unknown |
| `HorizontalAlignment` in inline Style | Client crash — only works in named LabelStyle definitions |
| `$` in .ui text strings | Parser crash — resolves as variable reference |

### What renders but has limitations

| Element | Limitation |
|---|---|
| Slider track | Invisible — `$C.@DefaultSliderStyle` renders opaque background covering overlays. Handle works. |
| `$C.@DropdownBox` template | Opens sideways (arrow right) — use bare `DropdownBox` instead |
| `$C.@NumberField` | Renders with "0" but `.Value` not settable from Java |
| `CircularProgressBar` | Invisible without `MaskTexturePath` texture |
| `Button` (plain) | Invisible — always use `TextButton` instead |

---

## Files to Modify/Eliminate

### Files to Delete (after ColorPicker migration)

| File | Lines | Replacement |
|---|---|---|
| `shared/page/ColorPickerPage.java` | 198 | Inline ColorPicker on settings pages |
| `shared/data/ColorPickerData.java` | ~30 | Not needed |
| `shared/component/ColorPickerModal.java` | ~50 | Not needed |
| `shared/color_picker.ui` | 261 | Native ColorPicker element |
| `shared/page/RecruitmentModalPage.java` | 150 | Inline DropdownBox |
| `shared/data/RecruitmentModalData.java` | ~30 | Not needed |
| `shared/recruitment_modal.ui` | 89 | Inline DropdownBox |

### Already Deleted

| File | Commit |
|---|---|
| `CreateFactionStep1Page.java` (239 lines) | `42267d9` |
| `CreateFactionStep2Page.java` (376 lines) | `42267d9` |
| `create_step1.ui` (189 lines) | `42267d9` |
| `create_step2.ui` (187 lines) | `42267d9` |

### TextField Migration — COMPLETED (Batch 1)

All 9 TextFields across 8 files migrated to `$C.@TextField`.

### Button Style Migration — COMPLETED (Batch 1)

211 non-destructive replacements (66 files) → `$C.@SecondaryTextButtonStyle`.
~45 destructive conversions (28 files) → `$C.@CancelTextButton`.
styles.ui cleaned: 402 → 251 lines (20 obsolete definitions removed).

---

## Recommended Implementation Order

Each step is independently testable and rollback-safe.

### Completed

| Order | Change | Status |
|---|---|---|
| ~~1~~ | TextField fix — swap 9 instances to `$C.@TextField` | **DONE** (Batch 1) |
| ~~2~~ | Native button styles on all pages (211 non-destructive + ~45 destructive) | **DONE** (Batch 1) |
| ~~19~~ | styles.ui cleanup (402 → 251 lines, 20 definitions removed) | **DONE** (Batch 1) |

### Remaining

| Order | Change | Effort | Impact | Pages |
|---|---|---|---|---|
| 3 | CheckBoxWithLabel on AdminFactionSettingsPage (11) | Medium | High — admin permission controls | 1 |
| 4 | CheckBoxWithLabel on FactionSettingsTabsPage (11) | Medium | High — user-facing controls | 1 |
| 5 | CheckBoxWithLabel on AdminZoneSettingsPage (22) | Medium | **Highest** — biggest single page | 1 |
| 6 | CheckBoxWithLabel on AdminDashboardPage (1) | Low | Low — trivial reuse | 1 |
| 7 | Faction color model migration (char -> hex) | Low | Foundation for ColorPicker work | Data |
| 8 | ColorPicker on Faction Settings General tab | Medium | High — inline picker | 1 |
| 9 | ColorPicker on Admin Faction Settings | Medium | High — inline picker | 1 |
| 10 | Delete ColorPickerPage + modal files | Low | Cleanup — removes 539 lines | 4 files |
| 11 | Slider on CreateZoneWizardPage | Low | Medium — replaces 5 preset buttons | 1 |
| 12 | DropdownBox for sort on browse/list pages | Medium | Medium — replaces disabled-button sort pattern | 4 |
| 13 | Inline DropdownBox replacing RecruitmentModal | Low | Medium — eliminates modal page | 2 |
| 14 | Real-time search + debounce on browse pages | Low | Medium — instant feedback | 3 |
| 15 | Add search to Members page (new feature) | Low | Medium — usability | 1 |
| 16 | Tab bar style switching (Value.ref) | Low | Low — better active/inactive visual | 3 |
| 17 | ProgressBar on Dashboard power display | Low | Low — optional polish | 1 |
| 18 | Centralized color system (FactionTheme + style.json) | **High** | **High** — long-term | 62+ |

**Suggested batches:**
- **Batch 1 (steps 1-2, 19)**: COMPLETED — TextField fix + native button styles + styles.ui cleanup.
- **Batch 2 (steps 3-6)**: CheckBox migration across all 4 toggle pages.
- **Batch 3 (steps 7-10)**: ColorPicker on remaining settings pages + data model migration + cleanup.
- **Batch 4 (steps 11-15)**: Slider + DropdownBox sort + real-time search. Native input elements.
- **Batch 5 (steps 16-18)**: Polish + centralized color system.
