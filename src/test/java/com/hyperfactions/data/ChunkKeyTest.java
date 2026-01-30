package com.hyperfactions.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChunkKey record.
 * Note: Hytale uses 32-block chunks (shift by 5), not 16-block chunks.
 */
@DisplayName("ChunkKey")
class ChunkKeyTest {

    private static final String WORLD = "world";
    private static final int CHUNK_SIZE = 32; // Hytale chunk size

    @Nested
    @DisplayName("fromWorldCoords()")
    class FromWorldCoordsTests {

        @Test
        @DisplayName("converts positive world coordinates to chunk coordinates")
        void fromWorldCoords_convertsPositive() {
            // Block 64 is in chunk 2 (64 >> 5 = 2)
            ChunkKey key = ChunkKey.fromWorldCoords(WORLD, 64.5, 96.7);

            assertEquals(WORLD, key.world());
            assertEquals(2, key.chunkX());  // 64 >> 5 = 2
            assertEquals(3, key.chunkZ());  // 96 >> 5 = 3
        }

        @Test
        @DisplayName("converts negative world coordinates correctly")
        void fromWorldCoords_convertsNegative() {
            // Block -33 should be in chunk -2 (floor(-33) = -33, -33 >> 5 = -2)
            ChunkKey key = ChunkKey.fromWorldCoords(WORLD, -33.0, -65.0);

            assertEquals(-2, key.chunkX());  // -33 >> 5 = -2
            assertEquals(-3, key.chunkZ());  // -65 >> 5 = -3
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
            // Block 32 is the first block of chunk 1
            ChunkKey key = ChunkKey.fromWorldCoords(WORLD, 32.0, 0.0);

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
            ChunkKey key = ChunkKey.fromBlockCoords(WORLD, 70, 130);

            assertEquals(2, key.chunkX()); // 70 >> 5 = 2
            assertEquals(4, key.chunkZ()); // 130 >> 5 = 4
        }

        @Test
        @DisplayName("handles negative block coordinates")
        void fromBlockCoords_handlesNegative() {
            ChunkKey key = ChunkKey.fromBlockCoords(WORLD, -40, -100);

            assertEquals(-2, key.chunkX()); // -40 >> 5 = -2
            assertEquals(-4, key.chunkZ()); // -100 >> 5 = -4
        }
    }

    @Nested
    @DisplayName("Block Boundary Methods")
    class BoundaryTests {

        @Test
        @DisplayName("getMinBlockX returns correct minimum X")
        void getMinBlockX_returnsCorrect() {
            ChunkKey key = new ChunkKey(WORLD, 3, 0);
            assertEquals(96, key.getMinBlockX()); // 3 << 5 = 96
        }

        @Test
        @DisplayName("getMaxBlockX returns correct maximum X")
        void getMaxBlockX_returnsCorrect() {
            ChunkKey key = new ChunkKey(WORLD, 3, 0);
            assertEquals(127, key.getMaxBlockX()); // 96 + 31 = 127
        }

        @Test
        @DisplayName("getMinBlockZ returns correct minimum Z")
        void getMinBlockZ_returnsCorrect() {
            ChunkKey key = new ChunkKey(WORLD, 0, 5);
            assertEquals(160, key.getMinBlockZ()); // 5 << 5 = 160
        }

        @Test
        @DisplayName("getMaxBlockZ returns correct maximum Z")
        void getMaxBlockZ_returnsCorrect() {
            ChunkKey key = new ChunkKey(WORLD, 0, 5);
            assertEquals(191, key.getMaxBlockZ()); // 160 + 31 = 191
        }

        @Test
        @DisplayName("handles negative chunk coordinates for boundaries")
        void boundaries_handleNegative() {
            ChunkKey key = new ChunkKey(WORLD, -2, -3);

            assertEquals(-64, key.getMinBlockX()); // -2 << 5 = -64
            assertEquals(-33, key.getMaxBlockX()); // -64 + 31 = -33
            assertEquals(-96, key.getMinBlockZ()); // -3 << 5 = -96
            assertEquals(-65, key.getMaxBlockZ()); // -96 + 31 = -65
        }
    }

    @Nested
    @DisplayName("Center Methods")
    class CenterTests {

        @Test
        @DisplayName("getCenterX returns chunk center X")
        void getCenterX_returnsCenter() {
            ChunkKey key = new ChunkKey(WORLD, 2, 0);
            assertEquals(80.0, key.getCenterX()); // (2 << 5) + 16 = 80
        }

        @Test
        @DisplayName("getCenterZ returns chunk center Z")
        void getCenterZ_returnsCenter() {
            ChunkKey key = new ChunkKey(WORLD, 0, 3);
            assertEquals(112.0, key.getCenterZ()); // (3 << 5) + 16 = 112
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
