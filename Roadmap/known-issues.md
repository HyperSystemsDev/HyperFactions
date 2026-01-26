# Known Issues

## Pitch/Yaw Bug (TABLED)

- **Symptom**: `/f home` teleport has wrong camera orientation
- **Investigation Done**:
  - Tried swapping pitch/yaw order in Vector3f
  - Checked FactionHome record field mapping
- **Likely Cause**: Issue in how home orientation is saved or applied
- **Status**: Tabled for later investigation - not blocking release

---

## Admin Zones Page Back Button

- **Symptom**: Back button positioned incorrectly
- **Fix**: Update AdminZonePage template positioning
- **Priority**: P2

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
