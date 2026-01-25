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
