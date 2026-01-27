# B.5 Implementation Tasks

> **Last Updated**: January 26, 2026
> **Wireframes**: See sections B.1-B.3 for ASCII mockups and element breakdowns.

---

## New Player GUI (B.1) - COMPLETE

| Task | Description | Template Files | Status |
|------|-------------|----------------|--------|
| B.1.1 | Create NewPlayerPageRegistry | `NewPlayerPageRegistry.java` | **DONE** |
| B.1.2 | Create NewPlayerNavBarHelper | `NewPlayerNavBarHelper.java` | **DONE** |
| B.1.3 | Create BrowseFactions page with pagination | `NewPlayerBrowsePage.java` | **DONE** |
| B.1.4 | Create CreateFaction wizard (2-step) | `CreateFactionStep1Page.java`, `CreateFactionStep2Page.java` | **DONE** |
| B.1.5 | Create MyInvites page | `InvitesPage.java` | **DONE** |
| B.1.6 | Integrate Help page | `HelpPage.java` | **DONE** |
| B.1.7 | Create read-only Map page | `NewPlayerMapPage.java` | **DONE** |

> **Note**: Create wizard uses 2-step flow (Step 1: name/tag/color, Step 2: recruitment/confirm) instead of single-page form.

### Deferred Enhancements (v1.2+)

| Task | Description | Status |
|------|-------------|--------|
| B.1.E1 | Add faction tag field to Faction record | Deferred |
| B.1.E2 | Add custom hex color input to color picker | Deferred |
| B.1.E3 | Search input on Browse page | Deferred |
| B.1.E4 | Expandable cards with JOIN/REQUEST buttons | Deferred |

---

## Faction Player GUI (B.2) - COMPLETE

| Task | Status | Description | Template Files |
|------|--------|-------------|----------------|
| B.2.1 | **DONE** | Redesigned Dashboard with identity, stat cards, quick actions, activity feed | `DashboardPage.java` |
| B.2.2 | **DONE** | Members page with paginated list, role-based actions | `MembersPage.java` |
| B.2.3 | **DONE** | Browse Factions for faction players with relation indicators | `BrowsePage.java` |
| B.2.4 | **DONE** | Implement interactive ChunkMapPage (29x17 grid, click to claim/unclaim) | `ChunkMapPage.java` |
| B.2.5 | **DONE** | Create Relations page (sectioned: Allies/Enemies/Requests) | `RelationsPage.java` |
| B.2.6 | **DONE** | Settings page (edit modals, teleport, recruitment, disband) | `SettingsPage.java` + 6 modals |
| B.2.7 | **DONE** | Create Modules page (placeholders) | `ModulesPage.java` |
| B.2.8 | **DONE** | LogsViewer utility component | `LogsViewer.java` |
| B.2.9 | **DONE** | ColorPicker utility component | `ColorPicker.java` |

### Deferred Enhancements (v1.2+)

| Task | Description | Status |
|------|-------------|--------|
| B.2.E1 | Activity Logs page (full viewer with filtering/pagination) | Deferred |
| B.2.E2 | Chat toggle system (F-CHAT, A-CHAT) | Deferred (requires ChatManager) |

---

## Admin GUI (B.3) - TABLED (v1.3+)

> **Status**: Basic implementation exists (AdminMainPage, AdminZonePage). Full Admin GUI deferred to v1.3+.

| Task | Description | Template Files | Status |
|------|-------------|----------------|--------|
| B.3.1 | Create AdminPageRegistry | - | Tabled |
| B.3.2 | Create AdminNavBarHelper | `admin/nav_button.ui` | Tabled |
| B.3.3 | Create Admin Dashboard with player FAB | `AdminMainPage.java` | PARTIAL |
| B.3.4 | Create Admin Factions page | `admin/factions.ui`, `admin/faction_card.ui` | Tabled |
| B.3.5 | Enhance Admin Zones page | `AdminZonePage.java` | PARTIAL |
| B.3.6 | Create Admin Players page | `admin/players.ui`, `admin/player_lookup.ui` | Tabled |
| B.3.7 | Create Admin Config page | `admin/config.ui` | Tabled |
| B.3.8 | Create Admin Logs page | `admin/logs.ui`, `admin/log_entry.ui` | Tabled |
| B.3.9 | Implement FAB quick-switch navigation | - | Tabled |

---

## Shared Components (B.4)

| Task | Description | Template Files | Status |
|------|-------------|----------------|--------|
| B.4.1 | Create shared HelpPage component | `help/main.ui`, `help/category.ui`, `help/command.ui` | TODO |
| B.4.2 | Create coming_soon_card template | `modules/coming_soon_card.ui` | DONE |
| B.4.3 | Create FAB (Floating Action Button) template | `shared/fab.ui` | TODO |
| B.4.4 | Create confirmation dialog template | `shared/confirm_dialog.ui` | PARTIAL |
| B.4.5 | Register HelpPage in all three registries | - | TODO |

---

## Template Naming Convention

```
resources/ui/HyperFactions/
├── newplayer/          # New Player GUI templates
├── faction/            # Faction Player GUI templates
├── admin/              # Admin GUI templates
├── help/               # Shared Help templates
├── modules/            # Module placeholder templates
├── shared/             # Shared components (FAB, dialogs)
└── nav/                # Existing nav bar templates
```

---

## Code Refactoring Tasks

| Task | Description | Status |
|------|-------------|--------|
| R.1 | Create NavAwareData interface | **DONE** |
| R.2 | Update data classes to implement NavAwareData | **DONE** |
| R.3 | Consolidate NavBarHelper to single handleNavEvent method | **DONE** |
| R.4 | Create GuiType enum for GUI type distinction | **DONE** |
| R.5 | Create PlaceholderPage for "Coming Soon" pages | **DONE** |
| R.6 | Create UIComponents utility class | TODO |

---

## Priority Order

### Phase 1: Code Quality (Complete)
1. ~~NavAwareData interface~~ **DONE**
2. ~~Consolidate NavBarHelper~~ **DONE**
3. ~~GuiType enum~~ **DONE**

### Phase 2: Faction Player GUI (Complete)
1. ~~Dashboard~~ **DONE**
2. ~~Members~~ **DONE**
3. ~~Browse~~ **DONE**
4. ~~Map~~ **DONE**
5. ~~Relations~~ **DONE**
6. ~~Settings~~ **DONE**
7. ~~Modules~~ **DONE**
8. ~~LogsViewer~~ **DONE**
9. ~~ColorPicker~~ **DONE**

### Phase 3: New Player GUI (Complete)
1. ~~Browse~~ **DONE**
2. ~~Create Wizard (2-step)~~ **DONE**
3. ~~Invites~~ **DONE**
4. ~~Map (read-only)~~ **DONE**
5. ~~Help~~ **DONE**
6. ~~Navigation (Registry + NavBarHelper)~~ **DONE**

### Phase 4: Admin GUI (Tabled - v1.3+)
Basic pages exist (AdminMainPage, AdminZonePage). Full implementation deferred.

---

## Next Steps (v1.1.0)

With GUI system complete, focus shifts to:
1. **Command system refactoring** (Phase A) - Modular commands, improved validation
2. **Gameplay mechanic refinement** - Combat, power, territory tuning
3. **Chat help improvements** (Phase C.1)

---

## Testing Checklist

- [ ] Build with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :HyperFactions:shadowJar`
- [ ] Deploy to test server with `./gradlew deployMods`
- [ ] Test navigation between all pages
- [ ] Verify placeholder pages display correctly
- [ ] Test permission-based visibility
- [ ] Test all modal dialogs
- [ ] Verify chat feedback messages
