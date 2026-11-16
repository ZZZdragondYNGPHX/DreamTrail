package com.dreamtrail.core

import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.content.ContentValidator
import com.dreamtrail.core.engine.*
import com.dreamtrail.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContentAndProgressionTest {

    @Test
    fun `content catalog passes strict validator with expected counts`() {
        val errors = ContentValidator.validate()
        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `combat damage formula and 20-round cap work deterministically`() {
        val dmg = CombatSimulator.computeDamage(attack = 20, skillCoeff = 1.0, targetDefense = 100)
        assertEquals(10, dmg)

        val lowDmg = CombatSimulator.computeDamage(attack = 1, skillCoeff = 0.5, targetDefense = 999)
        assertEquals(1, lowDmg)

        val tankSnapshot = ExpeditionPlayerSnapshot(
            level = 1,
            effectiveStats = Stats(maxHp = 5000, attack = 1, defense = 5000, speed = 10),
            equippedItems = emptyList(),
            equippedPassives = emptyList(),
            facilityLevels = emptyMap()
        )
        val tankEnemy = EnemyDef(
            id = "test_wall", name = "测试石壁", regionId = RegionId.MISTWOOD, isBoss = false,
            stats = Stats(5000, 1, 5000, 10), skillCoeff = 1.0, openingShield = 0, description = ""
        )
        val route = ContentCatalog.routes.first()
        val result = CombatSimulator.simulate(
            tankSnapshot, route, listOf(tankEnemy),
            com.dreamtrail.core.rng.DeterministicRng(1234L)
        )
        assertEquals(CombatOutcome.DRAW_RETREAT, result.outcome)
        assertEquals(CombatSimulator.MAX_ROUNDS, result.roundsPlayed)
    }

    @Test
    fun `facility upgrade craft equip and safe bulk salvage behave as specified`() {
        val startBundle = GameStateBundle(
            playerState = PlayerState(
                resources = ResourceBag(gold = 1000L, materials = mapOf(RegionId.MISTWOOD.materialId to 100L))
            ),
            budgetState = RewardBudgetState(),
            discoveryState = DiscoveryState(),
            inventory = emptyList()
        )

        val upgRes = ProgressionEngine.upgradeFacility(startBundle, FacilityType.WORKSHOP)
        assertTrue(upgRes is ActionOutcome.Success)
        val afterWorkshop = (upgRes as ActionOutcome.Success).value
        assertEquals(1, afterWorkshop.playerState.facilityLevel(FacilityType.WORKSHOP))

        // Craft 3 copies of pine staff
        var cur = afterWorkshop
        for (i in 1..3) {
            val craft = ProgressionEngine.craftEquipment(cur, "eq_pine_staff", "inst_$i")
            assertTrue(craft is ActionOutcome.Success)
            cur = (craft as ActionOutcome.Success).value
        }
        assertEquals(3, cur.inventory.size)

        // Equip inst_1 and lock inst_2; only inst_3 should be salvage candidate
        cur = (ProgressionEngine.equipItem(cur, "inst_1") as ActionOutcome.Success).value
        cur = ProgressionEngine.toggleLock(cur, "inst_2")

        val candidates = ProgressionEngine.duplicateSalvageCandidates(cur.playerState, cur.inventory)
        assertEquals(listOf("inst_3"), candidates)

        // Trying to salvage all 3 should only salvage inst_3 (protecting equipped & locked!)
        val salvageRes = ProgressionEngine.salvageSelected(cur, setOf("inst_1", "inst_2", "inst_3"))
        assertTrue(salvageRes is ActionOutcome.Success)
        val salvaged = (salvageRes as ActionOutcome.Success).value
        assertEquals(1, salvaged.salvagedCount)
        assertEquals(listOf("inst_1", "inst_2"), salvaged.updatedState.inventory.map { it.instanceId })
    }

    @Test
    fun `xp curve scales cleanly and caps at level 65`() {
        assertEquals(60L, ProgressionEngine.xpRequiredForNextLevel(1))
        assertEquals(72L, ProgressionEngine.xpRequiredForNextLevel(2))

        val (lvl, remXp) = ProgressionEngine.applyXp(1, 0L, 60L + 72L + 10L)
        assertEquals(3, lvl)
        assertEquals(10L, remXp)

        val (maxLvl, maxRem) = ProgressionEngine.applyXp(64, 0L, 1_000_000_000L)
        assertEquals(65, maxLvl)
        assertEquals(0L, maxRem)
    }

    @Test
    fun `craft keepsake spends boss marks and grants permanent stats and opening shield`() {
        val startBundle = GameStateBundle(
            playerState = PlayerState(
                resources = ResourceBag(
                    gold = 1000L,
                    materials = mapOf(
                        RegionId.MISTWOOD.materialId to 30L,
                        RegionId.TIDAL_FLAT.materialId to 30L
                    ),
                    bossMarks = 3L
                )
            ),
            budgetState = RewardBudgetState(),
            discoveryState = DiscoveryState(),
            inventory = emptyList()
        )

        val stagRes = ProgressionEngine.craftKeepsake(startBundle, "keepsake_stag_lantern")
        assertTrue(stagRes is ActionOutcome.Success)
        val afterStag = (stagRes as ActionOutcome.Success).value
        assertEquals(2L, afterStag.playerState.resources.bossMarks)
        assertEquals(780L, afterStag.playerState.resources.gold)
        assertEquals(12L, afterStag.playerState.resources.materialCount(RegionId.MISTWOOD.materialId))
        assertTrue("keepsake_stag_lantern" in afterStag.discoveryState.unlockedKeepsakeIds)

        val baseStats = ProgressionEngine.computeEffectiveStats(afterStag.playerState, afterStag.inventory, emptySet())
        val stagStats = ProgressionEngine.computeEffectiveStats(
            afterStag.playerState,
            afterStag.inventory,
            afterStag.discoveryState.unlockedKeepsakeIds
        )
        assertEquals(10, stagStats.maxHp - baseStats.maxHp)

        val kettleRes = ProgressionEngine.craftKeepsake(afterStag, "keepsake_everwarm_kettle")
        assertTrue(kettleRes is ActionOutcome.Success)
        val afterKettle = (kettleRes as ActionOutcome.Success).value
        assertEquals(0L, afterKettle.playerState.resources.bossMarks)
        assertTrue("keepsake_everwarm_kettle" in afterKettle.discoveryState.unlockedKeepsakeIds)

        val enemy = EnemyDef(
            id = "shield_probe",
            name = "试炼木偶",
            regionId = RegionId.MISTWOOD,
            isBoss = false,
            stats = Stats(maxHp = 5000, attack = 20, defense = 5000, speed = 100),
            skillCoeff = 1.0,
            openingShield = 0,
            description = ""
        )
        val route = ContentCatalog.routes.first()
        val baseSnapshot = ExpeditionPlayerSnapshot(
            level = 1,
            effectiveStats = Stats(maxHp = 5000, attack = 1, defense = 0, speed = 1),
            equippedItems = emptyList(),
            equippedPassives = emptyList(),
            facilityLevels = emptyMap(),
            unlockedKeepsakeIds = emptySet()
        )
        val kettleSnapshot = baseSnapshot.copy(unlockedKeepsakeIds = setOf("keepsake_everwarm_kettle"))

        val baseCombat = CombatSimulator.simulate(
            baseSnapshot, route, listOf(enemy), com.dreamtrail.core.rng.DeterministicRng(99L)
        )
        val kettleCombat = CombatSimulator.simulate(
            kettleSnapshot, route, listOf(enemy), com.dreamtrail.core.rng.DeterministicRng(99L)
        )
        assertEquals(0, baseCombat.damageBlockedByShield)
        assertEquals(8, kettleCombat.damageBlockedByShield)
    }

    @Test
    fun `affix reforge updates affix deducts cost and keeps earlier region gear pool stable`() {
        val mistwoodPool = ContentCatalog.equipmentPoolForRegion(RegionId.MISTWOOD)
        assertEquals(4, mistwoodPool.size)
        assertFalse("eq_whisper_flute" in mistwoodPool.map { it.id })
        assertEquals(5, ContentCatalog.equipmentPoolForRegion(RegionId.CLOUD_SANCTUARY).size)

        val bundle = GameStateBundle(
            playerState = PlayerState(
                resources = ResourceBag(
                    gold = 500L,
                    materials = mapOf(RegionId.MISTWOOD.materialId to 50L)
                ),
                facilities = mapOf(FacilityType.WORKSHOP to 1)
            ),
            budgetState = RewardBudgetState(),
            discoveryState = DiscoveryState(),
            inventory = emptyList()
        )
        val craftRes = ProgressionEngine.craftEquipment(bundle, "eq_pine_staff", "inst_reforge_1")
        assertTrue(craftRes is ActionOutcome.Success)
        val craftedBundle = (craftRes as ActionOutcome.Success).value
        val beforeItem = craftedBundle.inventory.single()
        val goldBefore = craftedBundle.playerState.resources.gold
        val matBefore = craftedBundle.playerState.resources.materialCount(RegionId.MISTWOOD.materialId)

        val reforgeRes = ProgressionEngine.reforgeEquipmentAffix(craftedBundle, "inst_reforge_1")
        assertTrue(reforgeRes is ActionOutcome.Success)
        val reforged = (reforgeRes as ActionOutcome.Success).value
        val afterItem = reforged.inventory.single()

        assertNotEquals(beforeItem.affix, afterItem.affix)
        assertEquals(goldBefore - 80L, reforged.playerState.resources.gold)
        assertEquals(matBefore - 4L, reforged.playerState.resources.materialCount(RegionId.MISTWOOD.materialId))
        assertTrue(afterItem.affix in reforged.discoveryState.discoveredAffixIds)
        assertNotEquals(craftedBundle.discoveryState.rngState, reforged.discoveryState.rngState)
    }
}
