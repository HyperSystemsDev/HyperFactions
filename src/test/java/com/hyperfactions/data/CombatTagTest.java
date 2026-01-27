package com.hyperfactions.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CombatTag record.
 */
@DisplayName("CombatTag")
class CombatTagTest {

    private static final UUID TEST_UUID = UUID.randomUUID();

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("creates tag with correct player and duration")
        void create_setsValues() {
            CombatTag tag = CombatTag.create(TEST_UUID, 15);

            assertEquals(TEST_UUID, tag.playerUuid());
            assertEquals(15, tag.durationSeconds());
            assertTrue(tag.taggedAt() > 0);
        }
    }

    @Nested
    @DisplayName("getExpiresAt()")
    class ExpiresAtTests {

        @Test
        @DisplayName("calculates expiration time correctly")
        void getExpiresAt_calculatesCorrectly() {
            long before = System.currentTimeMillis();
            CombatTag tag = CombatTag.create(TEST_UUID, 15);
            long after = System.currentTimeMillis();

            long expectedMin = before + 15_000;
            long expectedMax = after + 15_000;

            assertTrue(tag.getExpiresAt() >= expectedMin);
            assertTrue(tag.getExpiresAt() <= expectedMax);
        }
    }

    @Nested
    @DisplayName("isExpired()")
    class IsExpiredTests {

        @Test
        @DisplayName("returns false when freshly created")
        void isExpired_falseWhenFresh() {
            CombatTag tag = CombatTag.create(TEST_UUID, 15);
            assertFalse(tag.isExpired());
        }

        @Test
        @DisplayName("returns true when past expiry time")
        void isExpired_trueWhenPastExpiry() {
            // Create a tag with timestamp in the past
            long pastTime = System.currentTimeMillis() - 20_000; // 20 seconds ago
            CombatTag tag = new CombatTag(TEST_UUID, pastTime, 15);

            assertTrue(tag.isExpired());
        }

        @Test
        @DisplayName("returns false just before expiry")
        void isExpired_falseJustBeforeExpiry() {
            long recentTime = System.currentTimeMillis() - 14_000; // 14 seconds ago
            CombatTag tag = new CombatTag(TEST_UUID, recentTime, 15);

            assertFalse(tag.isExpired());
        }

        @Test
        @DisplayName("handles zero duration tag")
        void isExpired_trueWithZeroDuration() {
            CombatTag tag = CombatTag.create(TEST_UUID, 0);
            assertTrue(tag.isExpired());
        }
    }

    @Nested
    @DisplayName("getRemainingSeconds()")
    class RemainingSecondsTests {

        @Test
        @DisplayName("calculates remaining time correctly")
        void getRemainingSeconds_calculatesCorrectly() {
            CombatTag tag = CombatTag.create(TEST_UUID, 15);
            int remaining = tag.getRemainingSeconds();

            // Should be approximately 15 seconds (allow for test execution time)
            assertTrue(remaining >= 14 && remaining <= 15,
                    "Expected 14-15 seconds, got " + remaining);
        }

        @Test
        @DisplayName("returns zero when expired")
        void getRemainingSeconds_zeroWhenExpired() {
            long pastTime = System.currentTimeMillis() - 20_000;
            CombatTag tag = new CombatTag(TEST_UUID, pastTime, 15);

            assertEquals(0, tag.getRemainingSeconds());
        }

        @Test
        @DisplayName("rounds up remaining milliseconds")
        void getRemainingSeconds_roundsUp() {
            // Create tag that has ~500ms remaining in the current second
            long taggedAt = System.currentTimeMillis() - 14_500; // 14.5 seconds ago
            CombatTag tag = new CombatTag(TEST_UUID, taggedAt, 15);

            // Should round up to 1 second
            assertEquals(1, tag.getRemainingSeconds());
        }
    }

    @Nested
    @DisplayName("refresh()")
    class RefreshTests {

        @Test
        @DisplayName("resets timer with same duration")
        void refresh_resetsTimer() throws InterruptedException {
            CombatTag tag = CombatTag.create(TEST_UUID, 15);
            long originalTaggedAt = tag.taggedAt();

            // Small delay to ensure different timestamp
            Thread.sleep(5);

            CombatTag refreshed = tag.refresh();

            assertTrue(refreshed.taggedAt() > originalTaggedAt);
            assertEquals(tag.durationSeconds(), refreshed.durationSeconds());
            assertEquals(tag.playerUuid(), refreshed.playerUuid());
        }

        @Test
        @DisplayName("keeps same duration when using no-arg refresh")
        void refresh_keepsDuration() {
            CombatTag tag = CombatTag.create(TEST_UUID, 15);
            CombatTag refreshed = tag.refresh();

            assertEquals(15, refreshed.durationSeconds());
        }
    }

    @Nested
    @DisplayName("refresh(newDuration)")
    class RefreshWithDurationTests {

        @Test
        @DisplayName("resets timer with new duration")
        void refreshWithDuration_setsNewDuration() {
            CombatTag tag = CombatTag.create(TEST_UUID, 15);
            CombatTag refreshed = tag.refresh(30);

            assertEquals(30, refreshed.durationSeconds());
            assertTrue(refreshed.taggedAt() >= tag.taggedAt());
        }

        @Test
        @DisplayName("preserves player UUID")
        void refreshWithDuration_preservesPlayer() {
            CombatTag tag = CombatTag.create(TEST_UUID, 15);
            CombatTag refreshed = tag.refresh(20);

            assertEquals(TEST_UUID, refreshed.playerUuid());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("handles exactly expired tag")
        void exactlyExpired() {
            long exactExpiry = System.currentTimeMillis() - 15_000;
            CombatTag tag = new CombatTag(TEST_UUID, exactExpiry, 15);

            assertTrue(tag.isExpired());
            assertEquals(0, tag.getRemainingSeconds());
        }

        @Test
        @DisplayName("handles large duration values")
        void largeDuration() {
            CombatTag tag = CombatTag.create(TEST_UUID, 3600); // 1 hour

            assertFalse(tag.isExpired());
            assertTrue(tag.getRemainingSeconds() > 3500);
        }
    }
}
