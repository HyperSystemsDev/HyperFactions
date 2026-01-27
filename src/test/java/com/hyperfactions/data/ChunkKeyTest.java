package com.hyperfactions.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChunkKey record.
 */
@DisplayName("ChunkKey")
class ChunkKeyTest {

    private static final String WORLD = "world";

    @Nested
    @DisplayName("fromWorldCoords()")
    class FromWorldCoordsTests {

        @Test
        @DisplayName("converts positive world coordinates to chunk coordinates")
        void fromWorldCoords_convertsPositive() {
            // Block 32 is in chunk 2 (32 >> 4 = 2)
            ChunkKey key = ChunkKey.fromWorldCoords(WORLD, 32.5, 48.7);

            assertEquals(WORLD, key.world());
            assertEquals(2, key.chunkX());
            assertEquals(3, key.chunkZ());
        }

        @Test
        @DisplayName("converts negative world coordinates correctly")
        void fromWorldCoords_convertsNegative() {
            // Block -17 should be in chunk -2 (floor(-17) = -17, -17 >> 4 = -2)
            ChunkKey key = ChunkKey.fromWorldCoords(WORLD, -17.0, -33.0);

            assertEquals(-2, key.chunkX());
            assertEquals(-3, key.chunkZ());
        }

        @Test
        @DisplayName("handles zero coordinates")
        void fromWorldCoords_handlesZero() {
            ChunkKey key = ChunkKey.fromWorldCoords(WORLD, 0.0, 0.0);

            assertEquals(0, key.chunkX());
            assertEquals(0, key.chunkZ());
        }

        @Test
        @DisplayName("handles boundary coordinates (exactly on chunk edge)")
        void fromWorldCoords_handlesBoundary() {
            // Block 16 is the first block of chunk 1
            ChunkKey key = ChunkKey.fromWorldCoords(WORLD, 16.0, 0.0);

            assertEquals(1, key.chunkX());
            assertEquals(0, key.chunkZ());
        }
    }

    @Nested
    @DisplayName("fromBlockCoords()")
    class FromBlockCoordsTests {

        @Test
        @DisplayName("converts block coordinates to chunk coordinates")
        void fromBlockCoords_convertsCorrectly() {
            ChunkKey key = ChunkKey.fromBlockCoords(WORLD, 35, 65);

            assertEquals(2, key.chunkX()); // 35 >> 4 = 2
            assertEquals(4, key.chunkZ()); // 65 >> 4 = 4
        }

        @Test
        @DisplayName("handles negative block coordinates")
        void fromBlockCoords_handlesNegative() {
            ChunkKey key = ChunkKey.fromBlockCoords(WORLD, -20, -50);

            assertEquals(-2, key.chunkX()); // -20 >> 4 = -2
            assertEquals(-4, key.chunkZ()); // -50 >> 4 = -4
        }
    }

    @Nested
    @DisplayName("Block Boundary Methods")
    class BoundaryTests {

        @Test
        @DisplayName("getMinBlockX returns correct minimum X")
        void getMinBlockX_returnsCorrect() {
            ChunkKey key = new ChunkKey(WORLD, 3, 0);
            assertEquals(48, key.getMinBlockX()); // 3 << 4 = 48
        }

        @Test
        @DisplayName("getMaxBlockX returns correct maximum X")
        void getMaxBlockX_returnsCorrect() {
            ChunkKey key = new ChunkKey(WORLD, 3, 0);
            assertEquals(63, key.getMaxBlockX()); // 48 + 15 = 63
        }

        @Test
        @DisplayName("getMinBlockZ returns correct minimum Z")
        void getMinBlockZ_returnsCorrect() {
            ChunkKey key = new ChunkKey(WORLD, 0, 5);
            assertEquals(80, key.getMinBlockZ()); // 5 << 4 = 80
        }

        @Test
        @DisplayName("getMaxBlockZ returns correct maximum Z")
        void getMaxBlockZ_returnsCorrect() {
            ChunkKey key = new ChunkKey(WORLD, 0, 5);
            assertEquals(95, key.getMaxBlockZ()); // 80 + 15 = 95
        }

        @Test
        @DisplayName("handles negative chunk coordinates for boundaries")
        void boundaries_handleNegative() {
            ChunkKey key = new ChunkKey(WORLD, -2, -3);

            assertEquals(-32, key.getMinBlockX()); // -2 << 4 = -32
            assertEquals(-17, key.getMaxBlockX()); // -32 + 15 = -17
            assertEquals(-48, key.getMinBlockZ()); // -3 << 4 = -48
            assertEquals(-33, key.getMaxBlockZ()); // -48 + 15 = -33
        }
    }

    @Nested
    @DisplayName("Center Methods")
    class CenterTests {

        @Test
        @DisplayName("getCenterX returns chunk center X")
        void getCenterX_returnsCenter() {
            ChunkKey key = new ChunkKey(WORLD, 2, 0);
            assertEquals(40.0, key.getCenterX()); // (2 << 4) + 8 = 40
        }

        @Test
        @DisplayName("getCenterZ returns chunk center Z")
        void getCenterZ_returnsCenter() {
            ChunkKey key = new ChunkKey(WORLD, 0, 3);
            assertEquals(56.0, key.getCenterZ()); // (3 << 4) + 8 = 56
        }
    }

    @Nested
    @DisplayName("isAdjacentTo()")
    class AdjacencyTests {

        @Test
        @DisplayName("returns true for neighbor to the north")
        void isAdjacentTo_trueForNorth() {
            ChunkKey key = new ChunkKey(WORLD, 5, 5);
            ChunkKey north = new ChunkKey(WORLD, 5, 4);

            assertTrue(key.isAdjacentTo(north));
        }

        @Test
        @DisplayName("returns true for neighbor to the south")
        void isAdjacentTo_trueForSouth() {
            ChunkKey key = new ChunkKey(WORLD, 5, 5);
            ChunkKey south = new ChunkKey(WORLD, 5, 6);

            assertTrue(key.isAdjacentTo(south));
        }

        @Test
        @DisplayName("returns true for neighbor to the east")
        void isAdjacentTo_trueForEast() {
            ChunkKey key = new ChunkKey(WORLD, 5, 5);
            ChunkKey east = new ChunkKey(WORLD, 6, 5);

            assertTrue(key.isAdjacentTo(east));
        }

        @Test
        @DisplayName("returns true for neighbor to the west")
        void isAdjacentTo_trueForWest() {
            ChunkKey key = new ChunkKey(WORLD, 5, 5);
            ChunkKey west = new ChunkKey(WORLD, 4, 5);

            assertTrue(key.isAdjacentTo(west));
        }

        @Test
        @DisplayName("returns false for diagonal neighbor")
        void isAdjacentTo_falseForDiagonal() {
            ChunkKey key = new ChunkKey(WORLD, 5, 5);
            ChunkKey diagonal = new ChunkKey(WORLD, 6, 6);

            assertFalse(key.isAdjacentTo(diagonal));
        }

        @Test
        @DisplayName("returns false for distant chunk")
        void isAdjacentTo_falseForDistant() {
            ChunkKey key = new ChunkKey(WORLD, 5, 5);
            ChunkKey distant = new ChunkKey(WORLD, 10, 10);

            assertFalse(key.isAdjacentTo(distant));
        }

        @Test
        @DisplayName("returns false for same chunk")
        void isAdjacentTo_falseForSame() {
            ChunkKey key = new ChunkKey(WORLD, 5, 5);
            ChunkKey same = new ChunkKey(WORLD, 5, 5);

            assertFalse(key.isAdjacentTo(same));
        }

        @Test
        @DisplayName("returns false for different world")
        void isAdjacentTo_falseForDifferentWorld() {
            ChunkKey key = new ChunkKey("world", 5, 5);
            ChunkKey other = new ChunkKey("nether", 5, 4);

            assertFalse(key.isAdjacentTo(other));
        }
    }

    @Nested
    @DisplayName("Directional Methods")
    class DirectionalTests {

        private final ChunkKey center = new ChunkKey(WORLD, 5, 5);

        @Test
        @DisplayName("north() returns chunk with Z-1")
        void north_returnsCorrect() {
            ChunkKey north = center.north();

            assertEquals(WORLD, north.world());
            assertEquals(5, north.chunkX());
            assertEquals(4, north.chunkZ());
        }

        @Test
        @DisplayName("south() returns chunk with Z+1")
        void south_returnsCorrect() {
            ChunkKey south = center.south();

            assertEquals(WORLD, south.world());
            assertEquals(5, south.chunkX());
            assertEquals(6, south.chunkZ());
        }

        @Test
        @DisplayName("east() returns chunk with X+1")
        void east_returnsCorrect() {
            ChunkKey east = center.east();

            assertEquals(WORLD, east.world());
            assertEquals(6, east.chunkX());
            assertEquals(5, east.chunkZ());
        }

        @Test
        @DisplayName("west() returns chunk with X-1")
        void west_returnsCorrect() {
            ChunkKey west = center.west();

            assertEquals(WORLD, west.world());
            assertEquals(4, west.chunkX());
            assertEquals(5, west.chunkZ());
        }

        @Test
        @DisplayName("directional methods are consistent with isAdjacentTo")
        void directionals_consistentWithAdjacency() {
            assertTrue(center.isAdjacentTo(center.north()));
            assertTrue(center.isAdjacentTo(center.south()));
            assertTrue(center.isAdjacentTo(center.east()));
            assertTrue(center.isAdjacentTo(center.west()));
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualityTests {

        @Test
        @DisplayName("equal chunks are equal")
        void equals_sameValues() {
            ChunkKey key1 = new ChunkKey(WORLD, 5, 10);
            ChunkKey key2 = new ChunkKey(WORLD, 5, 10);

            assertEquals(key1, key2);
            assertEquals(key1.hashCode(), key2.hashCode());
        }

        @Test
        @DisplayName("different chunks are not equal")
        void equals_differentValues() {
            ChunkKey key1 = new ChunkKey(WORLD, 5, 10);
            ChunkKey key2 = new ChunkKey(WORLD, 5, 11);

            assertNotEquals(key1, key2);
        }

        @Test
        @DisplayName("different worlds are not equal")
        void equals_differentWorlds() {
            ChunkKey key1 = new ChunkKey("world", 5, 10);
            ChunkKey key2 = new ChunkKey("nether", 5, 10);

            assertNotEquals(key1, key2);
        }
    }
}
