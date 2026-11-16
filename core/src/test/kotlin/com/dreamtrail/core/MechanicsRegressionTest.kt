package com.dreamtrail.core

import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.*
import com.dreamtrail.core.model.*
import com.dreamtrail.core.rng.DeterministicRng
import com.dreamtrail.core.save.SaveBackupCodec
import com.dreamtrail.core.save.SaveBackupPayload
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MechanicsRegressionTest {
    private val route = ContentCatalog.routes.first()
    private fun snapshot(stats: Stats, vararg affixes: AffixId) = ExpeditionPlayerSnapshot(
        level = 1, effectiveStats = stats,
        equippedItems = affixes.mapIndexed { i, affix -> EquipmentInstance("item_$i", listOf("eq_pine_staff", "eq_moss_cloak", "eq_dew_charm")[i], 1, affix = affix) },
        equippedPassives = emptyList(), facilityLevels = emptyMap()
    )
    private fun combat(snapshot: ExpeditionPlayerSnapshot, hp: Int, attack: Int, speed: Int = 1) = CombatSimulator.simulate(
        snapshot, route, listOf(EnemyDef("target", "Target", route.regionId, false, Stats(hp, attack, 0, speed), 1.0, 0, "")),
        DeterministicRng(42)
    )

    @Test fun `dream mending heals ten percent and adds shield after every third round`() {
        val result = combat(snapshot(Stats(80, 1, 0, 10), AffixId.DREAM_MENDING), hp = 4, attack = 10)
        assertEquals(CombatOutcome.VICTORY, result.outcome)
        assertEquals(4, result.roundsPlayed)
        assertEquals(58, result.remainingHp)
    }

    @Test fun `thorn reflects only a shielded hit and grants six opening shield`() {
        val result = combat(snapshot(Stats(1000, 1, 100, 10), AffixId.THORN_BASTION), hp = 57, attack = 50)
        assertEquals(CombatOutcome.DRAW_RETREAT, result.outcome)
        assertEquals(6, result.damageBlockedByShield)
        assertEquals(506, result.remainingHp)
    }

    @Test fun `gale combo requires speed advantage and an even round`() {
        val fast = combat(snapshot(Stats(1000, 10, 0, 10), AffixId.GALE_COMBO), hp = 14, attack = 1)
        assertEquals(2, fast.roundsPlayed)
        val slow = combat(snapshot(Stats(1000, 10, 0, 0), AffixId.GALE_COMBO), hp = 24, attack = 1)
        assertEquals(3, slow.roundsPlayed)
    }

    @Test fun `shield strike rearms when sustain grants a new shield`() {
        val result = combat(snapshot(Stats(1000, 10, 0, 10), AffixId.DREAM_MENDING, AffixId.SHIELD_STRIKE), hp = 148, attack = 1)
        assertEquals(14, result.roundsPlayed)
    }

    @Test fun `temporary speed buffs also enable speed based combo effects`() {
        val result = combat(snapshot(Stats(1000, 10, 0, 0), AffixId.GALE_COMBO, AffixId.CHRONO_STRIKE), hp = 44, attack = 1, speed = 4)
        assertEquals(4, result.roundsPlayed)
    }

    @Test fun `affinity rank three strengthens Airen speed assistance`() {
        val base = snapshot(Stats(10, 10, 0, 0)).copy(activeCompanionId = "comp_airen")
        val before = combat(base, hp = 20, attack = 4, speed = 4)
        val after = combat(base.copy(companionAffinity = mapOf("comp_airen" to 7)), hp = 20, attack = 4, speed = 4)
        assertEquals(2, before.remainingHp)
        assertEquals(6, after.remainingHp)
    }

    @Test fun `secondary salvage insight discounts crafting and strengthening once`() {
        val def = ContentCatalog.equipmentDefs.first()
        val item = EquipmentInstance("discount", def.id, def.tier, affix = AffixId.STEADY_RECOVERY, secondaryAffix = AffixId.SALVAGE_INSIGHT)
        val player = PlayerState(equippedWeaponId = item.instanceId)
        val before = ProgressionEngine.adjustedCraftCost(def, player, emptyList())
        val after = ProgressionEngine.adjustedCraftCost(def, player, listOf(item))
        assertTrue(after.first < before.first)
        assertEquals(after, ProgressionEngine.adjustedCraftCost(def, player, listOf(item.copy(affix = AffixId.SALVAGE_INSIGHT))))
        assertTrue(ProgressionEngine.adjustedUpgradeCost(def, 2, player, listOf(item)).first <
            ProgressionEngine.adjustedUpgradeCost(def, 2, player, emptyList()).first)
    }

    @Test fun `character stats include the same collection bonuses as the departure snapshot`() {
        val discovery = DiscoveryState(unlockedKeepsakeIds = setOf("keepsake_stag_lantern"),
            unlockedSouvenirIds = setOf(ContentCatalog.souvenirs.first().id))
        val player = PlayerState()
        val stats = ProgressionEngine.computeEffectiveStats(player, emptyList(), discoveryState = discovery)
        assertEquals(stats, ProgressionEngine.createDepartureSnapshot(player, emptyList(), discovery.unlockedKeepsakeIds, discovery).effectiveStats)
        assertTrue(stats.maxHp > ProgressionEngine.computeEffectiveStats(player, emptyList()).maxHp)
    }

    @Test fun `backup rejects impossible astral branches passive duplicates and swapped gear slots`() {
        val base = SaveBackupPayload(exportedAtUtcMs = 1, contentVersion = ContentCatalog.CONTENT_VERSION, rulesVersion = ContentCatalog.RULES_VERSION,
            playerState = PlayerState(level = 20), budgetState = RewardBudgetState(), discoveryState = DiscoveryState(), inventory = emptyList())
        fun invalid(player: PlayerState, inventory: List<EquipmentInstance> = emptyList()) {
            assertTrue(SaveBackupCodec.decodeAndValidate(SaveBackupCodec.encode(base.copy(playerState = player, inventory = inventory))) is ActionOutcome.Failure)
        }
        invalid(base.playerState.copy(unlockedAstralNodeIds = setOf("astral_gale_2")))
        invalid(base.playerState.copy(level = 1, unlockedAstralNodeIds = setOf("astral_gale_1")))
        invalid(base.playerState.copy(equippedPassives = listOf("passive_forest_ward", "passive_forest_ward")))
        invalid(base.playerState.copy(equippedArmorId = "weapon"), listOf(EquipmentInstance("weapon", "eq_pine_staff", 1, affix = AffixId.BOUNTIFUL_GATHER)))
        assertTrue(SaveBackupCodec.decodeAndValidate(SaveBackupCodec.encode(base)) is ActionOutcome.Success)
    }
}
