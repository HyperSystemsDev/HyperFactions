# Known Issues

> **Last Updated**: February 1, 2026

## Open Issues

### Pitch/Yaw Bug (TABLED)

- **Symptom**: `/f home` teleport has wrong camera orientation
- **Investigation Done**:
  - Tried swapping pitch/yaw order in Vector3f
  - Checked FactionHome record field mapping
- **Likely Cause**: Issue in how home orientation is saved or applied
- **Status**: Tabled for later investigation - not blocking release
- **Priority**: P3

---

## Resolved Issues (2026-02-01 - v0.3.0/v0.3.1)

### Storage Race Condition (FIXED - v0.3.1)

- **Symptom**: Checksum verification failures when saving factions rapidly
- **Root Cause**: Concurrent writes overwrote each other's temp files
- **Fix**: Each atomic write now uses a unique temp file name
- **Files Modified**: `JsonFactionStorage.java`, `JsonPlayerStorage.java`, `JsonZoneStorage.java`

### TextField Input (FIXED - v0.3.1)

- **Symptom**: Text input fields not accepting keyboard input in GUI modals
- **Affected**: Faction name input, description input, rename modal, tag modal, zone creation wizard, relation search
- **Fix**: Corrected event binding for TextField components
- **Files Modified**: Multiple GUI page classes

### Data Loss Prevention (FIXED - v0.3.0)

- **Symptom**: Faction data lost on update/reload when deserialization fails
- **Root Cause**: FactionManager cleared caches before validating loaded data
- **Fix**:
  - FactionManager validates loaded data before clearing caches
  - Storage classes report failed files with SEVERE level logging
  - RuntimeException on critical I/O failures instead of returning empty
- **Files Modified**: `FactionManager.java`, `JsonFactionStorage.java`, `JsonPlayerStorage.java`, `JsonZoneStorage.java`

---

## Resolved Issues (2026-01-25)

### Chunk Coordinate Bug (FIXED)
- **Symptom**: Territory map showed wrong chunk coordinates (e.g., displayed (93, 7) when player was at (46, 3))
- **Root Cause**: ChunkUtil was using 16-block chunks (shift 4) instead of Hytale's 32-block chunks (shift 5)
- **Fix**: Updated `ChunkUtil.java` to use `CHUNK_SIZE = 32` and `CHUNK_SHIFT = 5`
- **Files Modified**: `ChunkUtil.java`

### Relations Page Crash (FIXED)
- **Symptom**: `selected element in customui command was not found. Selector: #alliesHeader.text`
- **Root Cause**: UI template used tabbed layout with different element IDs than Java expected
- **Fix**: Replaced `faction_relations.ui` with sectioned layout matching Java selectors (`#AlliesHeader`, `#AlliesList`, etc.)
- **Files Modified**: `faction_relations.ui`, `FactionRelationsPage.java` (removed invalid Style.TextColor set)

### Teleport Button Not Working (FIXED)
- **Symptom**: Settings page "Teleport Home" button showed chat message instead of teleporting
- **Fix**: Integrated TeleportManager with warmup/combat-tag support into FactionSettingsPage
- **Files Modified**: `FactionSettingsPage.java`, `GuiManager.java`

### Set Relation Modal Button Overflow (FIXED)
- **Symptom**: Buttons rendered outside modal container frame
- **Fix**: Reduced heights (ResultsList 310→250, EmptyState 80→40) and adjusted margins
- **Files Modified**: `set_relation_modal.ui`

### Gson NoClassDefFoundError (FIXED)
- **Symptom**: `NoClassDefFoundError: com/hyperfactions/lib/gson/internal/LinkedTreeMap$EntrySet`
- **Root Cause**: `minimize()` in shadowJar removed required Gson inner classes
- **Fix**: Removed `minimize()` from build.gradle
- **Files Modified**: `build.gradle`

---

## Future Improvements

### Territory Map Visual Polish (P3)

- **Current State**: Functional 29x17 chunk grid with click-to-claim/unclaim
- **Issues to Address**:
  - Wilderness color could have better contrast with grid background
  - Player indicator (white cell) works but could be more elegant
  - Cell borders removed for flat look - could revisit with subtle borders
  - Legend "You are here" white box is small, could be more visible
- **Ideas to Try**:
  - Add subtle 1px borders in a color that complements the design
  - Use a pulsing/animated player indicator (if Hytale supports)
  - Add hover tooltips showing chunk coordinates and owner
  - Consider adding grid lines overlay for better readability
  - Make player cell stand out more (bright border + symbol combination)
- **Technical Notes**: See `technical-reference.md` for UI constraints
