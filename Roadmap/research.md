# Research & Future Investigation

Items requiring investigation before implementation. These may reveal Hytale limitations or API requirements.

---

## R.1 Mob Spawning Control in Zones

**Question**: Can we control or prevent mob spawning in specific regions (SafeZones, WarZones)?

**Investigation Needed**:
- [ ] Research Hytale mob spawning system in decompiled sources
- [ ] Check for spawn events or hooks in EventRegistry
- [ ] Look for world/region-based spawn control APIs
- [ ] Investigate if chunk-level spawn rules are possible
- [ ] Check AdminUI or other mods for spawn control patterns

**Use Cases**:
- SafeZones: No hostile mob spawning (peaceful areas)
- WarZones: Potentially increased spawning or specific mob types
- Claimed territory: Configurable spawn rules per faction setting

**Relevant Files to Check**:
- `HytaleServerDocs/decompiled/` - Search for spawn-related classes
- `HytaleServerDocs/docs/reference/events.md` - Check for spawn events
- `resources/hytale-modding/` - Community spawn documentation

**Status**: Not started

---

## R.2 Block Protection Events

**Question**: What events are available for protecting blocks in claimed territory?

**Investigation Needed**:
- [ ] Document all block-related events (break, place, interact)
- [ ] Check for container access events
- [ ] Investigate explosion damage events (when explosives are added)
- [ ] Research entity-block interaction events

**Status**: Partially complete (basic block break/place working)

---

## R.3 Custom Context Providers for HyperPerms

**Question**: How can HyperFactions register custom context providers for HyperPerms?

**Investigation Needed**:
- [ ] Design API for faction context (faction name, role, territory type)
- [ ] Document integration pattern for other plugins
- [ ] Test performance impact of context lookups

**Planned Contexts**:
- `faction`: Player's faction name
- `faction_role`: LEADER, OFFICER, MEMBER
- `faction_territory`: own, ally, enemy, wilderness, zone

**Status**: Not started

---

## R.4 Interactive Map Rendering

**Question**: How can we render an interactive chunk map in the Hytale UI?

**Investigation Needed**:
- [ ] Research CustomUI capabilities for dynamic grid rendering
- [ ] Check if image generation is possible (chunk map image)
- [ ] Investigate click coordinates for grid interactions
- [ ] Look at minimap mods for patterns

**Constraints**:
- UI elements must be defined in templates
- Dynamic style changes crash (per Technical Reference)
- May need pre-generated cell templates

**Status**: Not started

---

## R.5 Clipboard / Copy Command Functionality

**Question**: Can we copy text (commands) to the player's clipboard from the Hytale UI?

**Investigation Needed**:
- [ ] Research Hytale client clipboard access
- [ ] Check if any existing mods implement copy functionality
- [ ] Investigate CustomUI text selection capabilities
- [ ] Look for chat input pre-fill alternatives (if clipboard not possible)

**Use Case**:
- Help GUI "Copy command" button
- Copy faction names, player names
- Copy coordinates

**Alternative if Not Possible**:
- "Click to insert in chat" - pre-fills chat input with command
- Just display command clearly for manual typing

**Status**: Not started

---

## R.6 Caching System (Performance)

**Question**: How can we implement efficient caching for faction data similar to HyperPerms?

**Context**: HyperFactions will store significant state information across features (factions, members, claims, power, relations, combat tags, zones, invites). Reading from storage on every access will cause performance issues.

**Investigation Needed**:
- [ ] Review HyperPerms caching implementation in `com.hyperperms.cache/`
- [ ] Analyze HyperPerms cache configuration options
- [ ] Document cache invalidation (cache busting) patterns used
- [ ] Identify which HyperFactions data benefits most from caching
- [ ] Design cache hierarchy (hot data vs. cold data)

**Data to Cache (Priority Order)**:
1. **Hot (frequent access)**:
   - Faction memberships (player → faction lookup)
   - Chunk claims (chunk → faction lookup)
   - Combat tags (active tags)
   - Power values (for claim calculations)
2. **Warm (moderate access)**:
   - Faction data (name, description, home, settings)
   - Relations (ally/enemy status)
   - Player profiles (last seen, deaths, kills)
3. **Cold (infrequent access)**:
   - Zone definitions (rarely change)
   - Audit logs (write-heavy, read-infrequent)

**Cache Invalidation Triggers**:
- Player joins/leaves faction
- Faction created/disbanded
- Territory claimed/unclaimed
- Relation changed
- Power updated
- Config reload

**HyperPerms Patterns to Review**:
- `PermissionCache` - Main caching class
- `CacheConfiguration` - TTL, max size, eviction policies
- `CacheInvalidator` - Event-based invalidation
- Caffeine library integration

**Configuration Goals**:
```yaml
cache:
  enabled: true
  faction-membership:
    ttl: 300  # seconds
    max-size: 1000
  chunk-claims:
    ttl: 60
    max-size: 10000
  power:
    ttl: 30
    max-size: 500
```

**Status**: Not started

---

## R.7 Chat Input Pre-Fill (Click-to-Suggest)

**Question**: Can we pre-fill the chat input with command syntax when a player clicks a command in the Help GUI?

**Investigation Needed**:
- [ ] Research Hytale client chat input APIs in decompiled sources
- [ ] Check for chat suggestion events or hooks in EventRegistry
- [ ] Look at how other mods handle command suggestions
- [ ] Test if closing a CustomUI page and sending a chat message works
- [ ] Investigate if there's a "suggest command" packet/API

**Use Cases**:
- Help GUI: Click `/f claim` to pre-fill in chat
- Territory Map: Click "Claim" button, command appears in chat
- Quick action buttons that suggest commands instead of executing

**Alternative Approaches if Not Possible**:
1. Copy to clipboard (requires R.5 research)
2. Execute command directly (current approach)
3. Display command prominently for manual typing

**Related to**: R.5 (Clipboard functionality)

**Status**: Not started
