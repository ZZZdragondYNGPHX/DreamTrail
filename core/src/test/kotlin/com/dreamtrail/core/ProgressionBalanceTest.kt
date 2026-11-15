package com.dreamtrail.core

import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.*
import com.dreamtrail.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProgressionBalanceTest {
    @Test fun `first month keeps regional and camp progression across sleep lengths and split sessions`() {
        for (seed in 1L..3L) for (hours in listOf(6, 8)) for (strategy in ExpeditionStrategy.entries) for (parts in listOf(1, 2)) {
            var b = GameStateBundle(PlayerState(), RewardBudgetState(), DiscoveryState(), emptyList())
            var secondRegionNight = 0
            var thirdRegionNight = 0
            for (night in 1..30) {
                val route = ContentCatalog.routes.last { it.id in b.discoveryState.unlockedRouteIds }
                repeat(parts) { part ->
                    val start = (1_700_000_000L + (night - 1) * 86400 + part * hours * 3600 / parts) * 1000
                    val s = ExpeditionSession("$night-$part", SessionStatus.RUNNING, route.id, strategy,
                        startAtUtcMs = start, sessionSeed = seed * 100 + night * 2 + part,
                        contentVersion = ContentCatalog.CONTENT_VERSION, rulesVersion = ContentCatalog.RULES_VERSION,
                        snapshot = ProgressionEngine.createDepartureSnapshot(b.playerState, b.inventory), isOnboarding = night == 1)
                    b = ExpeditionSimulator.simulateSettlement(s, start + hours * 3600_000 / parts,
                        hours * 3600L / parts, null, b).updatedBundle
                }
                for (type in FacilityType.entries) repeat(5) {
                    val upgrade = ProgressionEngine.upgradeFacility(b, type)
                    if (upgrade is ActionOutcome.Success) b = upgrade.value
                }
                for (slot in EquipmentSlot.entries) {
                    val best = b.inventory.filter { ContentCatalog.equipmentById(it.defId)!!.slot == slot }
                        .maxByOrNull { it.tier * 10 + it.level }
                    if (best != null) b = (ProgressionEngine.equipItem(b, best.instanceId) as ActionOutcome.Success).value
                }
                if (secondRegionNight == 0 && "route_tidal_shallows" in b.discoveryState.unlockedRouteIds) secondRegionNight = night + 1
                if (thirdRegionNight == 0 && "route_starlit_pass" in b.discoveryState.unlockedRouteIds) thirdRegionNight = night + 1
                if (night == 3) {
                    assertFalse("route_tidal_shallows" in b.discoveryState.unlockedRouteIds)
                    assertTrue(b.playerState.facilities.values.all { it <= 2 })
                    assertTrue(b.discoveryState.seenEventIds.size < 10)
                }
                if (night in listOf(1,7,14,30) && seed == 1L && parts == 1) {
                    println("hours=$hours strategy=$strategy night=$night level=${b.playerState.level} routes=${b.discoveryState.unlockedRouteIds.size} events=${b.discoveryState.seenEventIds.size} facilities=${b.playerState.facilities} chapters=${b.discoveryState.companionChapters}")
                }
            }
            assertTrue(secondRegionNight in 5..7, "second=$secondRegionNight hours=$hours strategy=$strategy")
            assertTrue(thirdRegionNight in 15..21, "third=$thirdRegionNight hours=$hours strategy=$strategy")
            if (hours == 6) {
                assertEquals(9, b.discoveryState.unlockedRouteIds.size)
            } else {
                assertEquals(12, b.discoveryState.unlockedRouteIds.size)
            }
            assertTrue(b.discoveryState.companionChapters.values.sum() < 6 || hours == 8,
                "6h sessions should still have final chapter progress after night 30")
        }
    }
}
