# Phase D: Testing Infrastructure

**Goal**: Establish comprehensive automated and manual testing processes.

**Design Decisions**:
- **Mocking strategy**: Full mocks for all Hytale APIs (PlayerRef, World, Store, etc.)
- **QA detail level**: Step-by-step scripts with expected results
- **Performance testing**: Future consideration (not in v1.x scope)

---

## D.1 Mock Infrastructure

**Overview**: Create mock implementations of Hytale APIs to enable testing without a running server.

**Mock Package Structure**:
```
src/test/java/com/hyperfactions/
├── mock/
│   ├── MockPlayerRef.java          # Mock player reference
│   ├── MockWorld.java              # Mock world with chunk data
│   ├── MockStore.java              # Mock entity store
│   ├── MockCommandContext.java     # Mock command context
│   ├── MockEventBus.java           # Mock event system
│   └── MockHytaleServer.java       # Composite mock for full server
├── fixture/
│   ├── TestFixtures.java           # Common test data builders
│   ├── FactionFixtures.java        # Pre-built faction scenarios
│   └── PlayerFixtures.java         # Pre-built player scenarios
└── ...test classes
```

**MockPlayerRef**:
```java
public class MockPlayerRef {
    private final UUID uuid;
    private final String username;
    private final MockWorld world;
    private Vector3d position;
    private boolean online;

    // Builder pattern for easy test setup
    public static MockPlayerRef.Builder builder() { ... }

    // Simulate player actions
    public void moveTo(int x, int y, int z) { ... }
    public void attack(MockPlayerRef target) { ... }
    public void setOnline(boolean online) { ... }

    // Get underlying mock
    public PlayerRef asPlayerRef() { ... }
}
```

**MockWorld**:
```java
public class MockWorld {
    private final String name;
    private final Map<ChunkCoord, MockChunk> chunks = new HashMap<>();

    // Chunk management
    public MockChunk getChunk(int x, int z) { ... }
    public void setChunkOwner(int x, int z, UUID factionId) { ... }
    public void setChunkZone(int x, int z, Zone zone) { ... }

    // Block simulation (for protection tests)
    public void placeBlock(int x, int y, int z, MockPlayerRef player) { ... }
    public void breakBlock(int x, int y, int z, MockPlayerRef player) { ... }
}
```

**TestFixtures**:
```java
public class TestFixtures {
    // Quick faction creation
    public static Faction createFaction(String name, UUID leaderId) { ... }
    public static Faction createFactionWithMembers(String name, int memberCount) { ... }

    // Quick player creation
    public static MockPlayerRef createPlayer(String name) { ... }
    public static MockPlayerRef createOnlinePlayer(String name, MockWorld world) { ... }

    // Scenarios
    public static FactionScenario twoFactionsAtWar() { ... }
    public static FactionScenario allianceSetup() { ... }
}
```

---

## D.2 Unit Tests

**Framework**: JUnit 5 (already configured)

**Coverage Targets**:

| Package | Test Focus | Priority | Mock Requirements |
|---------|-----------|----------|-------------------|
| `data/` | Record equality, builders, serialization | P1 | None (pure Java) |
| `manager/` | Business logic, state transitions | P1 | MockStorage, MockPlayerRef |
| `command/handler/` | Command parsing, routing | P1 | MockCommandContext |
| `util/` | Helper methods, formatting | P2 | None (pure Java) |
| `config/` | Config parsing, defaults | P2 | File system mocks |

**Test Structure**:
```
src/test/java/com/hyperfactions/
├── mock/                           # Mock implementations
├── fixture/                        # Test data builders
├── data/
│   ├── FactionTest.java
│   ├── FactionMemberTest.java
│   ├── PlayerPowerTest.java
│   ├── ZoneTest.java
│   └── FactionClaimTest.java
├── manager/
│   ├── FactionManagerTest.java
│   ├── ClaimManagerTest.java
│   ├── PowerManagerTest.java
│   ├── RelationManagerTest.java
│   ├── InviteManagerTest.java
│   ├── ZoneManagerTest.java
│   └── CombatTagManagerTest.java
├── command/
│   ├── CommandRouterTest.java
│   ├── handler/
│   │   ├── CoreCommandHandlerTest.java
│   │   ├── TerritoryCommandHandlerTest.java
│   │   └── ...
│   └── util/
│       └── AliasManagerTest.java
└── util/
    ├── ChunkUtilTest.java
    ├── TimeUtilTest.java
    └── HelpFormatterTest.java
```

**Example Test: FactionManagerTest**:
```java
class FactionManagerTest {
    private FactionManager manager;
    private MockStorage storage;
    private MockPlayerRef player1;
    private MockPlayerRef player2;

    @BeforeEach
    void setup() {
        storage = new MockStorage();
        manager = new FactionManager(storage);
        player1 = MockPlayerRef.builder().name("Player1").build();
        player2 = MockPlayerRef.builder().name("Player2").build();
    }

    @Test
    void createFaction_validName_returnsFaction() {
        // Act
        Result<Faction> result = manager.createFaction("Dragons", player1.getUuid(), "b");

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("Dragons", result.getValue().getName());
        assertEquals(player1.getUuid(), result.getValue().getLeaderId());
    }

    @Test
    void createFaction_playerAlreadyInFaction_returnsError() {
        // Arrange
        manager.createFaction("Dragons", player1.getUuid(), "b");

        // Act
        Result<Faction> result = manager.createFaction("Phoenix", player1.getUuid(), "c");

        // Assert
        assertTrue(result.isError());
        assertEquals(ErrorCode.PLAYER_ALREADY_IN_FACTION, result.getError());
    }

    @Test
    void createFaction_duplicateName_returnsError() {
        // Arrange
        manager.createFaction("Dragons", player1.getUuid(), "b");

        // Act
        Result<Faction> result = manager.createFaction("Dragons", player2.getUuid(), "c");

        // Assert
        assertTrue(result.isError());
        assertEquals(ErrorCode.FACTION_NAME_TAKEN, result.getError());
    }

    @Test
    void addMember_factionFull_returnsError() {
        // Arrange
        Faction faction = createFullFaction(); // 50 members

        // Act
        Result<Void> result = manager.addMember(faction.getId(), player1.getUuid());

        // Assert
        assertTrue(result.isError());
        assertEquals(ErrorCode.FACTION_FULL, result.getError());
    }

    @Test
    void disbandFaction_removesAllClaims() {
        // Arrange
        Faction faction = manager.createFaction("Dragons", player1.getUuid(), "b").getValue();
        claimManager.claim(faction.getId(), "world", 0, 0);
        claimManager.claim(faction.getId(), "world", 1, 0);

        // Act
        manager.disband(faction.getId());

        // Assert
        assertNull(claimManager.getOwner("world", 0, 0));
        assertNull(claimManager.getOwner("world", 1, 0));
    }
}
```

**Example Test: ClaimManagerTest**:
```java
class ClaimManagerTest {
    private ClaimManager manager;
    private FactionManager factionManager;
    private PowerManager powerManager;
    private MockWorld world;

    @BeforeEach
    void setup() {
        // ... setup with mocks
    }

    @Test
    void claim_wilderness_success() {
        // Arrange
        Faction faction = TestFixtures.createFaction("Dragons", UUID.randomUUID());
        powerManager.setPower(faction.getId(), 100); // Enough power

        // Act
        Result<Void> result = manager.claim(faction.getId(), "world", 0, 0);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(faction.getId(), manager.getOwner("world", 0, 0));
    }

    @Test
    void claim_insufficientPower_returnsError() {
        // Arrange
        Faction faction = TestFixtures.createFaction("Dragons", UUID.randomUUID());
        powerManager.setPower(faction.getId(), 1); // Not enough power

        // Act
        Result<Void> result = manager.claim(faction.getId(), "world", 0, 0);

        // Assert
        assertTrue(result.isError());
        assertEquals(ErrorCode.INSUFFICIENT_POWER, result.getError());
    }

    @Test
    void claim_inSafeZone_returnsError() {
        // Arrange
        Faction faction = TestFixtures.createFaction("Dragons", UUID.randomUUID());
        zoneManager.createSafeZone("Spawn", "world", 0, 0);

        // Act
        Result<Void> result = manager.claim(faction.getId(), "world", 0, 0);

        // Assert
        assertTrue(result.isError());
        assertEquals(ErrorCode.CANNOT_CLAIM_ZONE, result.getError());
    }
}
```

---

## D.3 Integration Tests

**Focus**: End-to-end flows across multiple managers with mocked Hytale APIs.

**Test Scenarios**:

| Scenario | Flow | Verifications |
|----------|------|---------------|
| Faction Lifecycle | Create → Claim → Set home → Invite → Disband | All data cleaned up |
| Alliance Flow | Request ally → Accept → Mutual relation | Both factions see alliance |
| Combat Tag | Attack → Tag applied → Teleport blocked → Wait → Teleport works | Timing correct |
| Power Raid | Faction loses power → Becomes raidable → Enemy overclaims | Territory transfers |
| Protection | Claim territory → Enemy tries to break → Blocked | Block event cancelled |

**Example Integration Test**:
```java
class FactionLifecycleIntegrationTest {
    private TestHarness harness;

    @BeforeEach
    void setup() {
        harness = new TestHarness(); // Sets up all managers with mocks
    }

    @Test
    void fullFactionLifecycle() {
        // Create faction
        MockPlayerRef leader = harness.createPlayer("Leader");
        harness.executeCommand(leader, "/f create Dragons");
        assertTrue(harness.isInFaction(leader, "Dragons"));

        // Claim territory
        leader.moveTo(100, 64, 100);
        harness.executeCommand(leader, "/f claim");
        assertTrue(harness.isClaimed(100, 100, "Dragons"));

        // Set home
        harness.executeCommand(leader, "/f sethome");
        assertNotNull(harness.getFactionHome("Dragons"));

        // Invite member
        MockPlayerRef member = harness.createPlayer("Member");
        harness.executeCommand(leader, "/f invite Member");
        assertTrue(harness.hasPendingInvite(member, "Dragons"));

        // Member accepts
        harness.executeCommand(member, "/f accept Dragons");
        assertTrue(harness.isInFaction(member, "Dragons"));
        assertEquals(2, harness.getFaction("Dragons").getMemberCount());

        // Disband
        harness.executeCommand(leader, "/f disband");
        harness.executeCommand(leader, "/f disband confirm"); // Confirmation
        assertFalse(harness.factionExists("Dragons"));
        assertFalse(harness.isInFaction(leader, "Dragons"));
        assertFalse(harness.isInFaction(member, "Dragons"));
        assertFalse(harness.isClaimed(100, 100, "Dragons"));
    }
}
```

---

## D.4 Manual QA Test Plan

**Format**: Step-by-step reproducible test scripts with expected results.

> **Full QA Document**: These are summary scripts. A full QA-CHECKLIST.md document
> should be created with all test cases for release validation.

---

### QA-001: New Player GUI Flow
```
Test ID: QA-001
Category: New Player Experience
Players Required: 1
Estimated Time: 5 minutes

PRECONDITIONS:
□ Player not in any faction
□ Player has hyperfactions.use permission
□ Server has at least one existing faction

STEPS:
1. Run /f (no args)
   EXPECTED: New Player GUI opens
   EXPECTED: Browse tab is active (default landing)
   EXPECTED: At least one faction visible in list
   VERIFY: □ Pass □ Fail

2. Click on a faction name to expand
   EXPECTED: Faction details appear (members, power, description)
   VERIFY: □ Pass □ Fail

3. Click CREATE tab in nav bar
   EXPECTED: Create Faction form appears
   EXPECTED: Name field is focused
   VERIFY: □ Pass □ Fail

4. Enter "Test123" as faction name
   EXPECTED: Name validation passes (green checkmark or no error)
   VERIFY: □ Pass □ Fail

5. Select a color (click any color in picker)
   EXPECTED: Color preview updates with faction name in selected color
   VERIFY: □ Pass □ Fail

6. Click "Create Faction" button
   EXPECTED: GUI closes
   EXPECTED: Success message in chat
   EXPECTED: Player is now faction leader
   VERIFY: □ Pass □ Fail

CLEANUP:
- Run /f disband, type "confirm" when prompted

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

### QA-002: Faction Dashboard & Quick Actions
```
Test ID: QA-002
Category: Faction Player GUI
Players Required: 1
Estimated Time: 5 minutes

PRECONDITIONS:
□ Player is in a faction
□ Player is Officer or Leader
□ Faction has no claimed territory

STEPS:
1. Run /f (no args)
   EXPECTED: Faction Dashboard opens
   EXPECTED: Power/Claims/Members stats visible
   EXPECTED: Quick actions (Home, Claim, Chat) visible
   VERIFY: □ Pass □ Fail

2. Click CLAIM quick action button
   EXPECTED: Current chunk is claimed (success message)
   EXPECTED: Claims stat increases by 1
   VERIFY: □ Pass □ Fail

3. Click CHAT quick action button
   EXPECTED: Toggle shows ON state
   EXPECTED: Faction chat mode message in chat
   VERIFY: □ Pass □ Fail

4. Type a message in chat
   EXPECTED: Message only visible to faction members
   EXPECTED: Message prefixed with [Faction]
   VERIFY: □ Pass □ Fail

5. Click CHAT again to toggle off
   EXPECTED: Toggle shows OFF state
   EXPECTED: Chat mode disabled message
   VERIFY: □ Pass □ Fail

6. Navigate to Settings page, click "Set Home Here"
   EXPECTED: Success message
   EXPECTED: Home location shown on Settings page
   VERIFY: □ Pass □ Fail

7. Move away, click HOME quick action on Dashboard
   EXPECTED: Warmup timer (if configured) or instant teleport
   EXPECTED: Player at faction home location
   VERIFY: □ Pass □ Fail

CLEANUP:
- Run /f unclaim to release territory

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

### QA-003: Territory Claiming & Map
```
Test ID: QA-003
Category: Territory Management
Players Required: 1
Estimated Time: 10 minutes

PRECONDITIONS:
□ Player is Officer+ in faction
□ Faction has sufficient power (> 10)
□ Player is in wilderness (unclaimed area)

STEPS:
1. Run /f map
   EXPECTED: ASCII map shows in chat
   EXPECTED: Current chunk marked with [+] or similar
   EXPECTED: Current chunk shows as wilderness (.)
   VERIFY: □ Pass □ Fail

2. Run /f claim
   EXPECTED: Success message "Claimed chunk at (X, Z)"
   VERIFY: □ Pass □ Fail

3. Run /f map again
   EXPECTED: Current chunk now shows as faction-owned (your color)
   VERIFY: □ Pass □ Fail

4. Move to adjacent chunk (not diagonal)
5. Run /f claim
   EXPECTED: Success (adjacent claiming allowed)
   VERIFY: □ Pass □ Fail

6. Move to non-adjacent chunk (2+ chunks away)
7. Run /f claim
   EXPECTED: Error "Must claim adjacent to existing territory"
   VERIFY: □ Pass □ Fail

8. Open GUI (/f gui), navigate to MAP page
   EXPECTED: Interactive map shows claimed chunks
   EXPECTED: Your chunks highlighted in faction color
   VERIFY: □ Pass □ Fail

9. Click on an owned chunk, click UNCLAIM
   EXPECTED: Success message
   EXPECTED: Chunk released on map
   VERIFY: □ Pass □ Fail

CLEANUP:
- Unclaim all test chunks

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

### QA-004: Alliance System (2 Players)
```
Test ID: QA-004
Category: Diplomacy
Players Required: 2
Estimated Time: 10 minutes

PRECONDITIONS:
□ Player1 is Officer+ in FactionA
□ Player2 is Officer+ in FactionB
□ Factions have no existing relation

STEPS:
1. Player1: Run /f ally FactionB
   EXPECTED: "Alliance request sent to FactionB"
   VERIFY: □ Pass □ Fail

2. Player2: Should receive notification
   EXPECTED: Message about incoming ally request from FactionA
   VERIFY: □ Pass □ Fail

3. Player2: Run /f ally FactionA
   EXPECTED: "You are now allies with FactionA"
   VERIFY: □ Pass □ Fail

4. Player1: Should receive notification
   EXPECTED: Message about alliance formed with FactionB
   VERIFY: □ Pass □ Fail

5. Both players: Run /f relations
   EXPECTED: Other faction listed as ALLY (green text)
   VERIFY: □ Pass □ Fail

6. Both players: Open GUI, navigate to Relations page
   EXPECTED: Alliance visible with correct details
   VERIFY: □ Pass □ Fail

7. Player1: Change relation to NEUTRAL
   EXPECTED: Alliance broken notification to both
   EXPECTED: Relations page updated
   VERIFY: □ Pass □ Fail

CLEANUP:
- Reset relations to neutral if not done

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

### QA-005: Combat Tag System (2 Players)
```
Test ID: QA-005
Category: PvP Protection
Players Required: 2
Estimated Time: 5 minutes

PRECONDITIONS:
□ Player1 in FactionA
□ Player2 in FactionB (or no faction)
□ FactionA has a home set
□ Combat tag duration is 15 seconds (default)

STEPS:
1. Player1: Attack Player2 (deal damage)
   EXPECTED: Both receive "You are now in combat" message
   EXPECTED: Combat tag indicator (if HUD implemented)
   VERIFY: □ Pass □ Fail

2. Player1: Immediately run /f home
   EXPECTED: Error "Cannot teleport while in combat"
   VERIFY: □ Pass □ Fail

3. Wait 15 seconds (do not attack again)
   EXPECTED: "Combat tag expired" message (optional)
   VERIFY: □ Pass □ Fail

4. Player1: Run /f home
   EXPECTED: Teleport begins (warmup or instant)
   EXPECTED: Player arrives at faction home
   VERIFY: □ Pass □ Fail

CLEANUP:
- None required

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

### QA-006: Admin Functions
```
Test ID: QA-006
Category: Administration
Players Required: 1 admin + 1 regular player
Estimated Time: 10 minutes

PRECONDITIONS:
□ AdminPlayer has hyperfactions.admin permission
□ TestPlayer is in a faction
□ No SafeZone exists at test location

STEPS:
1. AdminPlayer: Run /f admin
   EXPECTED: Admin GUI opens
   EXPECTED: Dashboard shows server statistics
   VERIFY: □ Pass □ Fail

2. Navigate to Zones page
   EXPECTED: Zone list visible (may be empty)
   VERIFY: □ Pass □ Fail

3. Create SafeZone named "TestZone" at current location
   EXPECTED: "SafeZone 'TestZone' created"
   EXPECTED: Zone appears in list
   VERIFY: □ Pass □ Fail

4. TestPlayer: Try to claim chunk in SafeZone
   EXPECTED: Error "Cannot claim protected zone territory"
   VERIFY: □ Pass □ Fail

5. AdminPlayer: Run /f admin bypass
   EXPECTED: "Admin bypass enabled"
   VERIFY: □ Pass □ Fail

6. AdminPlayer: Try to break block in TestPlayer's claimed territory
   EXPECTED: Block breaks successfully (bypass active)
   VERIFY: □ Pass □ Fail

7. AdminPlayer: Run /f admin bypass again
   EXPECTED: "Admin bypass disabled"
   VERIFY: □ Pass □ Fail

8. Delete the test SafeZone
   EXPECTED: Zone removed from list
   VERIFY: □ Pass □ Fail

CLEANUP:
- Ensure bypass is disabled
- Ensure test zone is deleted

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

### QA-007: Help System
```
Test ID: QA-007
Category: Documentation
Players Required: 1
Estimated Time: 5 minutes

PRECONDITIONS:
□ Player can be in or out of faction (test both)

STEPS:
1. Run /f help
   EXPECTED: Overview with category list
   EXPECTED: "Use /f help <category> for more" hint
   VERIFY: □ Pass □ Fail

2. Run /f help territory
   EXPECTED: List of territory commands with descriptions
   VERIFY: □ Pass □ Fail

3. Run /f help claim
   EXPECTED: Detailed help for /f claim command
   EXPECTED: Usage, aliases, requirements shown
   VERIFY: □ Pass □ Fail

4. Run /f help gui
   EXPECTED: Help GUI opens
   EXPECTED: Categories visible
   VERIFY: □ Pass □ Fail

5. Search for "claim" in Help GUI
   EXPECTED: Search results include /f claim and related commands
   VERIFY: □ Pass □ Fail

6. Click on a category card
   EXPECTED: Category page opens with command list
   VERIFY: □ Pass □ Fail

7. Click on a command
   EXPECTED: Command detail page with full information
   VERIFY: □ Pass □ Fail

CLEANUP:
- None required

RESULT: □ PASS □ FAIL
NOTES: _______________________________________________
```

---

## D.5 Automated Test Commands

```bash
# Run all unit tests
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :HyperFactions:test

# Run specific test class
./gradlew :HyperFactions:test --tests "com.hyperfactions.manager.FactionManagerTest"

# Run tests matching pattern
./gradlew :HyperFactions:test --tests "*ClaimManager*"

# Run with verbose output
./gradlew :HyperFactions:test --info

# Generate test report
# Location: build/reports/tests/test/index.html

# Run tests with coverage (if JaCoCo configured)
./gradlew :HyperFactions:jacocoTestReport
# Coverage report: build/reports/jacoco/test/html/index.html
```

---

## D.6 Implementation Tasks

**Mock Infrastructure (D.1)**

| Task | Description |
|------|-------------|
| D.1.1 | Create mock/ package structure |
| D.1.2 | Implement MockPlayerRef with builder |
| D.1.3 | Implement MockWorld with chunk management |
| D.1.4 | Implement MockStore |
| D.1.5 | Implement MockCommandContext |
| D.1.6 | Create TestFixtures utility class |

**Unit Tests (D.2)**

| Task | Description |
|------|-------------|
| D.2.1 | Write FactionTest (data model) |
| D.2.2 | Write FactionMemberTest |
| D.2.3 | Write FactionManagerTest |
| D.2.4 | Write ClaimManagerTest |
| D.2.5 | Write PowerManagerTest |
| D.2.6 | Write RelationManagerTest |
| D.2.7 | Write InviteManagerTest |
| D.2.8 | Write ZoneManagerTest |
| D.2.9 | Write CombatTagManagerTest |
| D.2.10 | Write CommandRouterTest |
| D.2.11 | Write AliasManagerTest |
| D.2.12 | Write utility class tests |

**Integration Tests (D.3)**

| Task | Description |
|------|-------------|
| D.3.1 | Create TestHarness class |
| D.3.2 | Write FactionLifecycleIntegrationTest |
| D.3.3 | Write AllianceFlowIntegrationTest |
| D.3.4 | Write CombatTagIntegrationTest |
| D.3.5 | Write ProtectionIntegrationTest |

**Manual QA (D.4)**

| Task | Description |
|------|-------------|
| D.4.1 | Create QA-CHECKLIST.md document |
| D.4.2 | Document QA-001 through QA-007 (core tests) |
| D.4.3 | Document QA-008 through QA-015 (advanced tests) |
| D.4.4 | Create QA test data setup guide |

**CI/CD (D.5)**

| Task | Description |
|------|-------------|
| D.5.1 | Configure Gradle test task |
| D.5.2 | Add JaCoCo for coverage reports |
| D.5.3 | Create GitHub Actions workflow (if using GitHub) |
