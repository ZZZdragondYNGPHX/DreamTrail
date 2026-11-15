package com.dreamtrail.app

import com.dreamtrail.app.time.DurationResolver
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.ProgressionEngine
import com.dreamtrail.core.engine.RewardBudgetCalculator
import com.dreamtrail.core.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DurationResolverTest {

    @Test
    fun `duration resolver handles same boot reboot backward clock and 30d forward jump safely`() {
        val snapshot = ProgressionEngine.createDepartureSnapshot(PlayerState(), emptyList())
        val session = ExpeditionSession(
            sessionId = "s_clock",
            status = SessionStatus.RUNNING,
            routeId = "route_mistwood_outer",
            strategy = ExpeditionStrategy.STEADY,
            startAtUtcMs = 1_700_000_000_000L,
            startElapsedRealtimeMs = 50_000L,
            startBootId = "boot_10",
            sessionSeed = 777L,
            contentVersion = ContentCatalog.CONTENT_VERSION,
            rulesVersion = ContentCatalog.RULES_VERSION,
            snapshot = snapshot
        )

        // 1. Same boot uses elapsedRealtime even if wall clock went backwards!
        val sameBoot = DurationResolver.resolveAt(
            session = session,
            nowWallUtcMs = session.startAtUtcMs - 3600_000L,
            nowElapsedRealtimeMs = 50_000L + 4 * 3600_000L,
            currentBootId = "boot_10"
        )
        assertEquals(4 * 3600L, sameBoot.durationSeconds)
        assertNotNull(sameBoot.anomalyNote)

        // 2. After reboot with backward wall clock -> duration clamped to 0, never negative
        val rebootBackward = DurationResolver.resolveAt(
            session = session,
            nowWallUtcMs = session.startAtUtcMs - 7200_000L,
            nowElapsedRealtimeMs = 1_000L,
            currentBootId = "boot_11"
        )
        assertEquals(0L, rebootBackward.durationSeconds)
        assertEquals(session.startAtUtcMs, rebootBackward.resolvedEndAtUtcMs)
        assertNotNull(rebootBackward.anomalyNote)

        // 3. 30-day forward jump after reboot -> capped to 12h window, anchored near startAt + 12h
        val thirtyDaysAhead = DurationResolver.resolveAt(
            session = session,
            nowWallUtcMs = session.startAtUtcMs + 30L * 24L * 3600_000L,
            nowElapsedRealtimeMs = 1_000L,
            currentBootId = "boot_11"
        )
        assertEquals(RewardBudgetCalculator.MAX_SINGLE_WINDOW_SECONDS, thirtyDaysAhead.durationSeconds)
        assertEquals(
            session.startAtUtcMs + RewardBudgetCalculator.MAX_SINGLE_WINDOW_SECONDS * 1000L,
            thirtyDaysAhead.resolvedEndAtUtcMs
        )
        assertNotNull(thirtyDaysAhead.anomalyNote)
    }

}
