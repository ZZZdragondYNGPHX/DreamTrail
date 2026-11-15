package com.dreamtrail.core

import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.*
import com.dreamtrail.core.model.*
import com.dreamtrail.core.rng.DeterministicRng
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DeterminismAndBudgetTest {

    private fun newSession(
        id: String = "sess_1",
        startSec: Long = 1_700_000_000L,
        routeId: String = "route_mistwood_outer",
        strategy: ExpeditionStrategy = ExpeditionStrategy.STEADY,
        seed: Long = 987654321L,
        bundle: GameStateBundle
    ): ExpeditionSession {
        return ExpeditionSession(
            sessionId = id,
            status = SessionStatus.RUNNING,
            routeId = routeId,
            strategy = strategy,
            startAtUtcMs = startSec * 1000L,
            sessionSeed = seed,
            contentVersion = ContentCatalog.CONTENT_VERSION,
            rulesVersion = ContentCatalog.RULES_VERSION,
            snapshot = ProgressionEngine.createDepartureSnapshot(bundle.playerState, bundle.inventory)
        )
    }

    @Test
    fun `same snapshot seed and end time produce identical settlement across repeated runs`() {
        val bundle = GameStateBundle(PlayerState(), RewardBudgetState(), DiscoveryState(), emptyList())
        val session = newSession(bundle = bundle)

        val first = ExpeditionSimulator.simulateSettlement(
            session = session,
            resolvedEndAtUtcMs = session.startAtUtcMs + 8 * 3600_000L,
            resolvedDurationSeconds = 8 * 3600L,
            clockAnomalyNote = null,
            bundle = bundle
        )

        repeat(10) {
            val again = ExpeditionSimulator.simulateSettlement(
                session = session,
                resolvedEndAtUtcMs = session.startAtUtcMs + 8 * 3600_000L,
                resolvedDurationSeconds = 8 * 3600L,
                clockAnomalyNote = null,
                bundle = bundle
            )
            assertEquals(first.settlement, again.settlement)
            assertEquals(first.updatedBundle, again.updatedBundle)
            assertEquals(first.ledgerEntries, again.ledgerEntries)
        }
    }

    @Test
    fun `previewing ten times during the night produces identical final result to staying closed`() {
        val bundle = GameStateBundle(PlayerState(), RewardBudgetState(), DiscoveryState(), emptyList())
        val session = newSession(bundle = bundle)

        // Preview 10 times at intermediate timestamps
        for (hour in 1..7) {
            val preview = ExpeditionSimulator.previewProgress(session, hour * 3600L, bundle.budgetState)
            assertEquals(hour * 12, preview.estimatedNodes)
        }
        repeat(3) {
            ExpeditionSimulator.previewProgress(session, 7 * 3600L + 1800L, bundle.budgetState)
        }

        // Neither preview modified bundle; compare against clean settlement
        val settled = ExpeditionSimulator.simulateSettlement(
            session = session,
            resolvedEndAtUtcMs = session.startAtUtcMs + 8 * 3600_000L,
            resolvedDurationSeconds = 8 * 3600L,
            clockAnomalyNote = null,
            bundle = bundle
        )
        assertEquals(96, settled.settlement.completedNodes)
        assertEquals(4, settled.settlement.newEquipment.size) // 8h / 2h = 4 equipment rolls
    }

    @Test
    fun `rolling 24h budget matches brute-force second oracle and enforces 8h cap across split sessions`() {
        val cap = RewardBudgetCalculator.ROLLING_CREDIT_CAP_SECONDS
        val twoFour = RewardBudgetCalculator.ROLLING_WINDOW_SECONDS

        // Two 4h sessions vs one 8h session yield identical credited seconds (28800s) and nodes (96)
        var budget = RewardBudgetState()
        val alloc1 = RewardBudgetCalculator.allocate(budget, sessionStartUtcSec = 0L, rawDurationSeconds = 4 * 3600L)
        budget = budget.copy(creditedIntervals = alloc1.updatedIntervals, remainderSeconds = alloc1.newRemainderSeconds)

        val alloc2 = RewardBudgetCalculator.allocate(budget, sessionStartUtcSec = 4 * 3600L, rawDurationSeconds = 4 * 3600L)
        budget = budget.copy(creditedIntervals = alloc2.updatedIntervals, remainderSeconds = alloc2.newRemainderSeconds)

        assertEquals(14_400L, alloc1.creditedSeconds)
        assertEquals(14_400L, alloc2.creditedSeconds)
        assertEquals(48 + 48, alloc1.completedNodes + alloc2.completedNodes)

        // Third 4h session inside same 24h window gets 0 credit
        val alloc3 = RewardBudgetCalculator.allocate(budget, sessionStartUtcSec = 10 * 3600L, rawDurationSeconds = 4 * 3600L)
        assertEquals(0L, alloc3.creditedSeconds)
        assertNotNull(alloc3.budgetNote)

        // Next night starting at 23h and lasting 8h (23h..31h): 23h..24h gets 0, 24h..31h releases 7h from [0..28800)
        val alloc4 = RewardBudgetCalculator.allocate(budget, sessionStartUtcSec = 23 * 3600L, rawDurationSeconds = 8 * 3600L)
        assertEquals(7 * 3600L, alloc4.creditedSeconds)
        assertEquals(cap - 3600L, alloc4.creditedSeconds)
        assertTrue(twoFour == 24 * 3600L)
    }

    @Test
    fun `30 days offline computes rapidly and stays strictly bounded to single window and 8h cap`() {
        val bundle = GameStateBundle(PlayerState(), RewardBudgetState(), DiscoveryState(), emptyList())
        val session = newSession(bundle = bundle)
        val thirtyDaysSec = 30L * 24L * 3600L

        val startMs = System.nanoTime()
        val output = ExpeditionSimulator.simulateSettlement(
            session = session,
            resolvedEndAtUtcMs = session.startAtUtcMs + thirtyDaysSec * 1000L,
            resolvedDurationSeconds = thirtyDaysSec,
            clockAnomalyNote = "已按单次最长 12 小时窗口结算",
            bundle = bundle
        )
        val elapsedMs = (System.nanoTime() - startMs) / 1_000_000L

        assertEquals(12 * 3600L, output.settlement.windowCappedSeconds)
        assertEquals(8 * 3600L, output.settlement.effectiveCreditedSeconds)
        assertEquals(96, output.settlement.completedNodes)
        assertTrue(elapsedMs < 500, "Simulation took ${elapsedMs}ms, expected < 500ms")
    }

    @Test
    fun `short sub-5-min sessions conserve remainder seconds and do not multiply rewards`() {
        var bundle = GameStateBundle(PlayerState(), RewardBudgetState(), DiscoveryState(), emptyList())
        // Run 4 sessions of 90 seconds each = 360s -> 1 node total, 60s remainder
        for (i in 0 until 4) {
            val s = newSession(id = "short_$i", startSec = 1000L + i * 200L, seed = 100L + i, bundle = bundle)
            val out = ExpeditionSimulator.simulateSettlement(s, s.startAtUtcMs + 90_000L, 90L, null, bundle)
            bundle = out.updatedBundle
        }
        assertEquals(60, bundle.budgetState.remainderSeconds)
        assertEquals(360L, bundle.discoveryState.totalCreditedSeconds)
    }

    @Test
    fun `cancelling departure advances rngState without rolling back`() {
        val (seed1, next1) = DeterministicRng.advanceSessionSeed(0x5DEECE66DL)
        val (seed2, next2) = DeterministicRng.advanceSessionSeed(next1)
        assertNotEquals(seed1, seed2)
        assertNotEquals(next1, next2)
    }

    @Test
    fun `three danger failures trigger safe gather fallback without losing prior loot`() {
        // Level 1 naked traveler on Tier-3 hardest route should lose 3 encounters and switch to safe gather
        val bundle = GameStateBundle(PlayerState(), RewardBudgetState(), DiscoveryState(), emptyList())
        val session = newSession(
            routeId = "route_starlit_observatory",
            strategy = ExpeditionStrategy.EXPLORE,
            seed = 42L,
            bundle = bundle
        )
        val out = ExpeditionSimulator.simulateSettlement(
            session = session,
            resolvedEndAtUtcMs = session.startAtUtcMs + 8 * 3600_000L,
            resolvedDurationSeconds = 8 * 3600L,
            clockAnomalyNote = null,
            bundle = bundle
        )
        assertTrue(out.settlement.switchedToSafeGather)
        assertEquals(3, out.settlement.dangerFailures)
        assertTrue(out.settlement.baseGold > 0L)
        assertTrue(out.settlement.baseXp > 0L)
    }

    @Test
    fun `first onboarding journey grants starter milestone once and allows camp upgrade`() {
        val bundle = GameStateBundle(PlayerState(), RewardBudgetState(), DiscoveryState(), emptyList())
        val firstSession = newSession(id = "onboard_1", bundle = bundle).copy(isOnboarding = true)
        val firstOut = ExpeditionSimulator.simulateSettlement(
            session = firstSession,
            resolvedEndAtUtcMs = firstSession.startAtUtcMs + 60_000L,
            resolvedDurationSeconds = 60L,
            clockAnomalyNote = null,
            bundle = bundle
        )
        assertTrue(firstOut.updatedBundle.discoveryState.onboardingCompleted)
        assertEquals(1, firstOut.settlement.newEquipment.size)
        assertTrue(firstOut.settlement.keyEvents.isNotEmpty())
        assertTrue(firstOut.updatedBundle.playerState.resources.gold >= 100L)
        assertTrue(firstOut.updatedBundle.playerState.resources.materialCount(RegionId.MISTWOOD.materialId) >= 10L)

        // Second short journey must NOT re-trigger onboarding handouts
        val secondSession = newSession(id = "normal_2", startSec = 1_700_001_000L, bundle = firstOut.updatedBundle).copy(isOnboarding = true)
        val secondOut = ExpeditionSimulator.simulateSettlement(
            session = secondSession,
            resolvedEndAtUtcMs = secondSession.startAtUtcMs + 60_000L,
            resolvedDurationSeconds = 60L,
            clockAnomalyNote = null,
            bundle = firstOut.updatedBundle
        )
        assertTrue(secondOut.settlement.newEquipment.isEmpty())
        assertTrue(secondOut.settlement.baseGold < 10L)
    }
}
