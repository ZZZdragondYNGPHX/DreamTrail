package com.dreamtrail.core

import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.*
import com.dreamtrail.core.model.*
import com.dreamtrail.core.rng.DeterministicRng
import com.dreamtrail.core.sleep.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RegressionTest {
    private fun bundle() = GameStateBundle(PlayerState(), RewardBudgetState(), DiscoveryState(onboardingCompleted = true), emptyList())
    private fun session(b: GameStateBundle, seed: Long = 1L, start: Long = 1_700_000_000L) = ExpeditionSession(
        sessionId = "review_$start", status = SessionStatus.RUNNING, routeId = "route_mistwood_outer",
        strategy = ExpeditionStrategy.STEADY, startAtUtcMs = start * 1000, sessionSeed = seed,
        contentVersion = ContentCatalog.CONTENT_VERSION, rulesVersion = ContentCatalog.RULES_VERSION,
        snapshot = ProgressionEngine.createDepartureSnapshot(b.playerState, b.inventory))

    @Test fun `short sessions preserve fractional ordinary resources`() {
        val initial = bundle()
        val s = session(initial)
        val single = ExpeditionSimulator.simulateSettlement(s, s.startAtUtcMs + 200_000L, 200, null, initial)
        var split = initial
        repeat(10) { i ->
            val part = session(split, start = 1_700_000_000L + i * 20)
            split = ExpeditionSimulator.simulateSettlement(part, part.startAtUtcMs + 20_000, 20, null, split).updatedBundle
        }
        println("single200s gold=${single.updatedBundle.playerState.resources.gold}, split10x20s gold=${split.playerState.resources.gold}; credited=${split.discoveryState.totalCreditedSeconds}")
        assertEquals(single.updatedBundle.playerState.resources.gold, split.playerState.resources.gold)
    }

    @Test fun `preview only displays progress and does not invent events`() {
        val b = bundle()
        val s = session(b)
        for (seconds in listOf(300L, 21600L, 28800L)) {
            val p = ExpeditionSimulator.previewProgress(s, seconds, b.budgetState)
            val out = ExpeditionSimulator.simulateSettlement(s, s.startAtUtcMs + seconds * 1000, seconds, null, b)
            assertTrue(p.recentHighlights.isEmpty())
            assertEquals(out.settlement.completedNodes, p.estimatedNodes)
            assertEquals(out.settlement.effectiveCreditedSeconds, p.effectiveElapsedSeconds)
        }
    }

    @Test fun `artisan attack bonus applies only once`() {
        val p = PlayerState(equippedPassives = listOf("passive_artisan_touch"))
        val snapshot = ProgressionEngine.createDepartureSnapshot(p, emptyList())
        val route = ContentCatalog.routes.first()
        val enemy = EnemyDef("review_target", "target", RegionId.MISTWOOD, false, Stats(190, 1, 0, 1), 1.0, 0, "test")
        val expected = CombatSimulator.simulate(snapshot.copy(equippedPassives = emptyList()), route, listOf(enemy), DeterministicRng(1))
        val actual = CombatSimulator.simulate(snapshot, route, listOf(enemy), DeterministicRng(1))
        println("visibleAttack=${snapshot.effectiveStats.attack} expectedRounds=${expected.roundsPlayed} actualRounds=${actual.roundsPlayed}")
        assertEquals(expected.roundsPlayed, actual.roundsPlayed)
    }

    @Test fun `overlapping summary records from same origin are not doubled`() {
        val r = SleepRecord("hc", "mi", "a", "1", 0L, 7200_000L, asleepSeconds = 7200, observedAtUtcMs = 7200_000, consentEpoch = 1)
        val match = SleepNormalizer.normalizeForSession(0, 7200_000, listOf(r, r.copy(sourceRecordId = "b")), 1)
        println("windowSeconds=7200 normalizedAsleepSeconds=${match.matchedAsleepSeconds}")
        assertTrue(match.matchedAsleepSeconds <= 7200)
    }

    @Test fun `fractional carry belongs to the currency after applying each segment multiplier`() {
        val initial = bundle()
        var b = initial
        val routes = listOf("route_mistwood_outer", "route_tidal_shallows", "route_mistwood_outer")
        val strategies = listOf(ExpeditionStrategy.STEADY, ExpeditionStrategy.GATHER, ExpeditionStrategy.EXPLORE)
        var expectedGoldNumerator = 0L
        val expectedMaterials = mutableMapOf<String, Long>()
        for (i in routes.indices) {
            val route = ContentCatalog.routeById(routes[i])!!
            val secs = 81L
            val gp = listOf(100L,95L,92L)[i]
            val mp = listOf(100L,125L,88L)[i]
            expectedGoldNumerator += secs * route.baseGoldPerHour * gp * 100
            expectedMaterials[route.regionId.materialId] = (expectedMaterials[route.regionId.materialId] ?: 0) + secs * route.baseMaterialPerHour * mp * 100
            val s = session(b, start = 1_700_000_000L + i * 100).copy(routeId = route.id, strategy = strategies[i])
            b = ExpeditionSimulator.simulateSettlement(s, s.startAtUtcMs + secs * 1000, secs, null, b).updatedBundle
        }
        assertEquals(initial.playerState.resources.gold + expectedGoldNumerator / ResourceAccrual.RATE_DENOMINATOR, b.playerState.resources.gold)
        for ((material, numerator) in expectedMaterials) assertEquals(numerator % ResourceAccrual.RATE_DENOMINATOR, b.budgetState.resourceRemainders[material])
        val carry = ResourceAccrual(emptyMap())
        assertEquals(3L, (1..100).sumOf { carry.accrue("blessing_gold", 300, 10000) })
    }

    @Test fun `tidal speed and artisan attack apply once including combined passives`() {
        val route = ContentCatalog.routes.first() // no damp conditional modifier
        val target = EnemyDef("target", "target", RegionId.MISTWOOD, false, Stats(100, 10, 0, 14), 1.0, 0, "")
        for (passives in listOf(listOf("passive_tidal_stride"), listOf("passive_tidal_stride", "passive_artisan_touch"))) {
            val snap = ProgressionEngine.createDepartureSnapshot(PlayerState(equippedPassives = passives), emptyList())
            val expected = CombatSimulator.simulate(snap.copy(equippedPassives = emptyList()), route, listOf(target), DeterministicRng(2))
            val actual = CombatSimulator.simulate(snap, route, listOf(target), DeterministicRng(2))
            assertEquals(expected, actual)
        }
    }

    @Test fun `settlement rejects mismatched and unsupported versions`() {
        val b = bundle()
        val current = session(b)
        for (invalid in listOf(current.copy(rulesVersion = 999), current.copy(contentVersion = 1))) {
            assertThrows(IllegalArgumentException::class.java) {
                ExpeditionSimulator.simulateSettlement(invalid, current.startAtUtcMs, 0, null, b)
            }
        }
    }

    @Test fun `mixed sleep stages summaries conflicts and revisions remain bounded`() {
        val first = SleepRecord("hc", "mi", "a", "1", 0, 3600_000, asleepSeconds = 3600, observedAtUtcMs = 1, consentEpoch = 1,
            stages = listOf(SleepStageInterval(0, 3600_000, SleepStageType.DEEP)))
        val summary = first.copy(sourceRecordId = "b", startAtUtcMs = 3600_000, endAtUtcMs = 7200_000, stages = emptyList(), deepSeconds = 1800)
        val mixed = SleepNormalizer.normalizeForSession(0,7200_000,listOf(first, summary),1)
        assertEquals(7200L, mixed.matchedAsleepSeconds)
        assertEquals(5400L, mixed.matchedDeepSeconds)
        val conflict = first.copy(sourceRecordId = "c", stages = listOf(SleepStageInterval(0,3600_000,SleepStageType.REM)))
        val conflicted = SleepNormalizer.normalizeForSession(0,7200_000,listOf(first,conflict),1)
        assertEquals(3600L, conflicted.matchedAsleepSeconds)
        assertEquals(0L, conflicted.matchedDeepSeconds)
        assertEquals(0L, conflicted.matchedRemSeconds)
        val revised = first.copy(observedAtUtcMs = 2, stages = emptyList(), asleepSeconds = 1200)
        assertEquals(1200L, SleepNormalizer.normalizeForSession(0,7200_000,listOf(first,revised),1).matchedAsleepSeconds)
        val invalid = summary.copy(asleepSeconds = Long.MAX_VALUE, deepSeconds = Long.MAX_VALUE)
        val bounded = SleepNormalizer.normalizeForSession(0,7200_000,listOf(invalid),1)
        assertEquals(3600L, bounded.matchedAsleepSeconds)
        assertNull(bounded.matchedDeepSeconds)
    }

    @Test fun `save rejects unknown versions and round trips fractional balances`() {
        val payload = com.dreamtrail.core.save.SaveBackupPayload(exportedAtUtcMs = 1, contentVersion = ContentCatalog.CONTENT_VERSION, rulesVersion = ContentCatalog.RULES_VERSION,
            playerState = PlayerState(), discoveryState = DiscoveryState(), inventory = emptyList(),
            budgetState = RewardBudgetState(resourceRemainders = mapOf("gold" to 32000000L, "blessing_gold" to 9900)))
        val codec = com.dreamtrail.core.save.SaveBackupCodec
        assertEquals(payload, (codec.decodeAndValidate(codec.encode(payload)) as ActionOutcome.Success).value)
        assertTrue(codec.decodeAndValidate(codec.encode(payload.copy(rulesVersion = 999))) is ActionOutcome.Failure)
        assertTrue(codec.decodeAndValidate(codec.encode(payload.copy(budgetState = RewardBudgetState(resourceRemainders = mapOf("gold" to -1))))) is ActionOutcome.Failure)
    }
}
