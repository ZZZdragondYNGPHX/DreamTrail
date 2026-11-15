package com.dreamtrail.core.engine

import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.model.*
import com.dreamtrail.core.rng.DeterministicRng
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToLong

sealed class ActionOutcome<out T> {
    data class Success<T>(val value: T) : ActionOutcome<T>()
    data class Failure(val reason: String) : ActionOutcome<Nothing>()
}

data class SalvageResult(
    val updatedState: GameStateBundle,
    val salvagedCount: Int,
    val goldGained: Long,
    val materialsGained: Map<String, Long>
)

data class MorningEchoResolution(
    val updatedBundle: GameStateBundle,
    val echoTitle: String,
    val summaryText: String
)

/**
 * Pure progression operations: stats, XP curve, camp facilities, crafting, equipment, and passives.
 */
object ProgressionEngine {

    fun computeResonanceCounts(
        equippedItems: List<EquipmentInstance>,
        equippedPassiveIds: List<String>
    ): Map<BuildArchetype, Int> {
        val counts = mutableMapOf<BuildArchetype, Int>()
        for (item in equippedItems) {
            counts[item.affix.archetype] = (counts[item.affix.archetype] ?: 0) + 1
            item.secondaryAffix?.let { sec ->
                counts[sec.archetype] = (counts[sec.archetype] ?: 0) + 1
            }
        }
        for (pid in equippedPassiveIds) {
            val arch = ContentCatalog.passiveById(pid)?.archetype ?: continue
            counts[arch] = (counts[arch] ?: 0) + 1
        }
        return counts
    }

    fun computeActiveResonances(
        equippedItems: List<EquipmentInstance>,
        equippedPassiveIds: List<String>
    ): List<ActiveResonance> {
        val counts = computeResonanceCounts(equippedItems, equippedPassiveIds)
        return BuildArchetype.entries.mapNotNull { arch ->
            val pts = counts[arch] ?: 0
            val tier = when {
                pts >= 6 -> 3
                pts >= 4 -> 2
                pts >= 2 -> 1
                else -> 0
            }
            if (tier > 0) ActiveResonance(arch, pts, tier) else null
        }
    }

    fun hasDualArchetypeSynergy(
        equippedItems: List<EquipmentInstance>,
        equippedPassiveIds: List<String>
    ): Boolean {
        val counts = computeResonanceCounts(equippedItems, equippedPassiveIds)
        return counts.values.count { it >= 2 } >= 2
    }

    fun computeActiveResonancesFromSnapshot(snapshot: ExpeditionPlayerSnapshot): List<ActiveResonance> =
        computeActiveResonances(snapshot.equippedItems, snapshot.equippedPassives)

    fun maxPassiveSlots(playerState: PlayerState): Int =
        if (playerState.level >= 25 || playerState.facilityLevel(FacilityType.INN) >= 4) 3 else 2

    fun companionAffinityRank(affinity: Int): Int = when {
        affinity >= 18 -> 5
        affinity >= 12 -> 4
        affinity >= 7 -> 3
        affinity >= 3 -> 2
        else -> 1
    }

    fun totalAstralPoints(level: Int): Int =
        (level / 4).coerceIn(0, ContentCatalog.astralNodes.size)

    fun usedAstralPoints(playerState: PlayerState): Int =
        playerState.unlockedAstralNodeIds.size

    fun xpRequiredForNextLevel(currentLevel: Int): Long {
        if (currentLevel >= PlayerState.MAX_LEVEL) return 0L
        return ceil(60.0 * 1.2.pow((currentLevel - 1).toDouble())).toLong()
    }

    fun applyXp(startLevel: Int, startXp: Long, gainedXp: Long): Pair<Int, Long> {
        var level = startLevel.coerceIn(1, PlayerState.MAX_LEVEL)
        var xp = (startXp + gainedXp.coerceAtLeast(0L)).coerceAtLeast(0L)
        while (level < PlayerState.MAX_LEVEL) {
            val need = xpRequiredForNextLevel(level)
            if (xp >= need) {
                xp -= need
                level++
            } else {
                break
            }
        }
        if (level >= PlayerState.MAX_LEVEL) {
            xp = 0L
        }
        return level to xp
    }

    fun baseStatsForLevel(level: Int): Stats {
        val lvl = level.coerceIn(1, PlayerState.MAX_LEVEL)
        return Stats(
            maxHp = 80 + (lvl - 1) * 8,
            attack = 14 + (lvl - 1) * 2,
            defense = 8 + (lvl - 1),
            speed = 10 + ((lvl - 1) / 2)
        )
    }

    fun computeEffectiveStats(
        playerState: PlayerState,
        inventory: List<EquipmentInstance>,
        unlockedKeepsakeIds: Set<String> = emptySet(),
        unlockedSouvenirIds: Set<String> = emptySet(),
        discoveryState: DiscoveryState? = null
    ): Stats {
        val keepsakes = unlockedKeepsakeIds + discoveryState?.unlockedKeepsakeIds.orEmpty()
        val souvenirs = unlockedSouvenirIds + discoveryState?.unlockedSouvenirIds.orEmpty()
        var stats = baseStatsForLevel(playerState.level)
        val workshopLevel = playerState.facilityLevel(FacilityType.WORKSHOP)
        val innLevel = playerState.facilityLevel(FacilityType.INN)
        val teaHearthLevel = playerState.facilityLevel(FacilityType.TEA_HEARTH)
        val conservatoryLevel = playerState.facilityLevel(FacilityType.CONSERVATORY)
        stats = stats.copy(
            maxHp = stats.maxHp + innLevel * 4 + teaHearthLevel * 3 + conservatoryLevel * 6,
            defense = stats.defense + conservatoryLevel / 2
        )

        val equippedIds = playerState.equippedInstanceIds()
        for (item in inventory) {
            if (item.instanceId in equippedIds) {
                val def = ContentCatalog.equipmentById(item.defId) ?: continue
                stats += ContentCatalog.effectiveEquipmentStats(def, item.level, workshopLevel)
            }
        }
        if ("passive_tidal_stride" in playerState.equippedPassives) {
            stats = stats.copy(speed = stats.speed + 3)
        }
        if ("passive_artisan_touch" in playerState.equippedPassives) {
            stats = stats.copy(attack = stats.attack + 3)
        }
        if ("passive_cloud_aegis" in playerState.equippedPassives) {
            stats = stats.copy(maxHp = stats.maxHp + 20)
        }
        if ("passive_thorns_oath" in playerState.equippedPassives) {
            stats = stats.copy(speed = stats.speed + 4)
        }
        if ("passive_chrono_step" in playerState.equippedPassives) {
            stats = stats.copy(speed = stats.speed + 5)
        }
        if ("keepsake_stag_lantern" in keepsakes) {
            stats = stats.copy(maxHp = stats.maxHp + 10)
        }
        if ("keepsake_tidal_chime" in keepsakes) {
            stats = stats.copy(defense = stats.defense + 3)
        }
        if ("keepsake_star_orrery" in keepsakes) {
            stats = stats.copy(attack = stats.attack + 4)
        }
        if ("keepsake_cloud_music_box" in keepsakes) {
            stats = stats.copy(speed = stats.speed + 2)
        }
        if ("keepsake_abyss_astrolabe" in keepsakes) {
            stats = stats.copy(maxHp = stats.maxHp + 24, attack = stats.attack + 6)
        }
        if ("keepsake_tea_porcelain" in keepsakes) {
            stats = stats.copy(defense = stats.defense + 4, speed = stats.speed + 2)
        }
        if ("keepsake_astral_pendulum" in keepsakes) {
            stats = stats.copy(maxHp = stats.maxHp + 30, attack = stats.attack + 8)
        }
        if ("keepsake_conservatory_bell" in keepsakes) {
            stats = stats.copy(defense = stats.defense + 6, speed = stats.speed + 3)
        }
        for (souvenirId in souvenirs) {
            val souv = ContentCatalog.souvenirById(souvenirId) ?: continue
            stats += souv.bonusStats
        }
        for (nodeId in playerState.unlockedAstralNodeIds) {
            val node = ContentCatalog.astralNodeById(nodeId) ?: continue
            stats += node.bonusStats
        }
        if (discoveryState != null) {
            for (chron in ContentCatalog.chronicles) {
                if ((discoveryState.chronicleProgress[chron.id] ?: 0) >= 3) {
                    stats += chron.rewardStats
                }
            }
            if (companionAffinityRank(discoveryState.companionAffinity["comp_airen"] ?: 0) >= 4) {
                stats = stats.copy(speed = stats.speed + 2)
            }
            if (companionAffinityRank(discoveryState.companionAffinity["comp_noah"] ?: 0) >= 4) {
                stats = stats.copy(defense = stats.defense + 4)
            }
            if (companionAffinityRank(discoveryState.companionAffinity["comp_lin"] ?: 0) >= 4) {
                stats = stats.copy(maxHp = stats.maxHp + 18)
            }
            if (companionAffinityRank(discoveryState.companionAffinity["comp_xiu"] ?: 0) >= 4) {
                stats = stats.copy(attack = stats.attack + 4)
            }
            if (companionAffinityRank(discoveryState.companionAffinity["comp_cen"] ?: 0) >= 4) {
                stats = stats.copy(maxHp = stats.maxHp + 12, defense = stats.defense + 3)
            }
            val masteredSpecies = discoveryState.enemyKillCounts.values.count { it >= 15 }
            if (masteredSpecies > 0) {
                stats = stats.copy(
                    maxHp = stats.maxHp + masteredSpecies * 2,
                    attack = stats.attack + masteredSpecies
                )
            }
        }
        return stats
    }

    fun createDepartureSnapshot(
        playerState: PlayerState,
        inventory: List<EquipmentInstance>,
        unlockedKeepsakeIds: Set<String> = emptySet(),
        discoveryState: DiscoveryState? = null
    ): ExpeditionPlayerSnapshot {
        val equippedIds = playerState.equippedInstanceIds()
        val equippedItems = inventory.filter { it.instanceId in equippedIds }
        val souvenirs = discoveryState?.unlockedSouvenirIds ?: emptySet()
        var effective = computeEffectiveStats(playerState, inventory, unlockedKeepsakeIds, souvenirs, discoveryState)

        val teaHearthLevel = playerState.facilityLevel(FacilityType.TEA_HEARTH)
        val validTea = playerState.activeTeaBlendId
            ?.let { ContentCatalog.teaBlendById(it) }
            ?.takeIf { teaHearthLevel >= it.requiredTeaHearthLevel }
        if (validTea != null) {
            effective = effective.copy(
                maxHp = effective.maxHp + validTea.bonusHp,
                attack = effective.attack + validTea.bonusAtk,
                defense = effective.defense + validTea.bonusDef
            )
        }

        playerState.activeMorningBuff?.let { buff ->
            effective = effective.copy(
                attack = effective.attack + buff.combatAtkBonus,
                defense = effective.defense + buff.combatDefBonus
            )
        }

        val unlockedCompanions = if (discoveryState != null) {
            ContentCatalog.companionStories.filter { (discoveryState.companionChapters[it.id] ?: 0) >= 1 }.map { it.id }
        } else {
            emptyList()
        }
        val activeComp = playerState.activeCompanionId?.takeIf { it in unlockedCompanions }
        val dispatchComps = unlockedCompanions.filter { it != activeComp }

        return ExpeditionPlayerSnapshot(
            level = playerState.level,
            effectiveStats = effective,
            equippedItems = equippedItems,
            equippedPassives = playerState.equippedPassives.take(maxPassiveSlots(playerState)),
            facilityLevels = playerState.facilities,
            unlockedKeepsakeIds = unlockedKeepsakeIds,
            unlockedSouvenirIds = souvenirs,
            activeCompanionId = activeComp,
            dispatchCompanionIds = dispatchComps,
            activeTeaBlendId = validTea?.id,
            dreamBeaconTier = playerState.dreamBeaconTier.coerceIn(0, 4),
            activeMorningBuff = playerState.activeMorningBuff,
            unlockedAstralNodeIds = playerState.unlockedAstralNodeIds,
            companionAffinity = discoveryState?.companionAffinity ?: emptyMap(),
            enemyKillCounts = discoveryState?.enemyKillCounts ?: emptyMap(),
            chronicleProgress = discoveryState?.chronicleProgress ?: emptyMap()
        )
    }

    fun upgradeFacility(
        bundle: GameStateBundle,
        facilityType: FacilityType
    ): ActionOutcome<GameStateBundle> {
        val current = bundle.playerState.facilityLevel(facilityType)
        if (current >= 5) return ActionOutcome.Failure("${facilityType.displayName}已达到最高等级 Lv.5")
        val target = current + 1
        val requiredHours = ContentCatalog.facilityRequiredExploreHours(facilityType, target)
        if (bundle.discoveryState.totalCreditedSeconds < requiredHours * 3600L) {
            return ActionOutcome.Failure("累计有效探索 ${requiredHours} 小时后可继续修葺")
        }
        val (goldCost, matPair) = ContentCatalog.facilityUpgradeCost(facilityType, target)
        val (matId, matCost) = matPair
        val res = bundle.playerState.resources
        if (res.gold < goldCost || res.materialCount(matId) < matCost) {
            return ActionOutcome.Failure("资源不足，无法升至 Lv.$target")
        }

        val nextResources = res
            .addGold(-goldCost)
            .addMaterial(matId, -matCost)
        val nextFacilities = bundle.playerState.facilities.toMutableMap().apply {
            put(facilityType, target)
        }
        val nextPlayer = bundle.playerState.copy(
            resources = nextResources,
            facilities = nextFacilities
        )
        val nextDiscovery = reconcileUnlocks(nextPlayer, bundle.discoveryState, bundle.inventory)
        return ActionOutcome.Success(
            bundle.copy(playerState = nextPlayer, discoveryState = nextDiscovery)
        )
    }

    fun adjustedCraftCost(
        def: EquipmentDef,
        playerState: PlayerState,
        inventory: List<EquipmentInstance>
    ): Pair<Long, Long> {
        var goldFactor = 1.0
        if ("passive_artisan_touch" in playerState.equippedPassives) goldFactor -= 0.10
        val equippedItems = inventory.filter { it.instanceId in playerState.equippedInstanceIds() }
        if (equippedItems.any { it.affix == AffixId.SALVAGE_INSIGHT || it.secondaryAffix == AffixId.SALVAGE_INSIGHT }) goldFactor -= 0.05
        val gold = (def.craftGoldCost * goldFactor).roundToLong().coerceAtLeast(20L)
        return gold to def.craftMaterialCost
    }

    fun adjustedUpgradeCost(
        def: EquipmentDef,
        targetLevel: Int,
        playerState: PlayerState,
        inventory: List<EquipmentInstance>
    ): Pair<Long, Long> {
        val (baseGold, matPair) = ContentCatalog.equipmentUpgradeCost(def, targetLevel)
        var goldFactor = 1.0
        if ("passive_artisan_touch" in playerState.equippedPassives) goldFactor -= 0.10
        val equippedItems = inventory.filter { it.instanceId in playerState.equippedInstanceIds() }
        if (equippedItems.any { it.affix == AffixId.SALVAGE_INSIGHT || it.secondaryAffix == AffixId.SALVAGE_INSIGHT }) goldFactor -= 0.05
        val gold = (baseGold * goldFactor).roundToLong().coerceAtLeast(15L)
        return gold to matPair.second
    }

    fun craftEquipment(
        bundle: GameStateBundle,
        defId: String,
        newInstanceId: String
    ): ActionOutcome<GameStateBundle> {
        val def = ContentCatalog.equipmentById(defId)
            ?: return ActionOutcome.Failure("未找到装备配置")
        val workshopLevel = bundle.playerState.facilityLevel(FacilityType.WORKSHOP)
        if (workshopLevel < def.requiredWorkshopLevel) {
            return ActionOutcome.Failure("需要工坊 Lv.${def.requiredWorkshopLevel} 才可打造「${def.name}」")
        }
        val (goldCost, matCost) = adjustedCraftCost(def, bundle.playerState, bundle.inventory)
        val res = bundle.playerState.resources
        if (res.gold < goldCost || res.materialCount(def.craftMaterialId) < matCost) {
            return ActionOutcome.Failure("打造所需金币或材料不足")
        }

        val newItem = EquipmentInstance(
            instanceId = newInstanceId,
            defId = def.id,
            tier = def.tier,
            level = 1,
            affix = def.defaultAffix,
            locked = false
        )
        val nextPlayer = bundle.playerState.copy(
            resources = res.addGold(-goldCost).addMaterial(def.craftMaterialId, -matCost)
        )
        val nextDiscovery = bundle.discoveryState.copy(
            discoveredEquipmentDefIds = bundle.discoveryState.discoveredEquipmentDefIds + def.id,
            discoveredAffixIds = bundle.discoveryState.discoveredAffixIds + newItem.affix
        )
        return ActionOutcome.Success(
            bundle.copy(
                playerState = nextPlayer,
                discoveryState = nextDiscovery,
                inventory = bundle.inventory + newItem
            )
        )
    }

    fun upgradeEquipment(
        bundle: GameStateBundle,
        instanceId: String
    ): ActionOutcome<GameStateBundle> {
        val item = bundle.inventory.firstOrNull { it.instanceId == instanceId }
            ?: return ActionOutcome.Failure("背包中未找到该装备")
        if (item.level >= EquipmentInstance.MAX_LEVEL) {
            return ActionOutcome.Failure("装备已达到最高强化等级 Lv.${EquipmentInstance.MAX_LEVEL}")
        }
        val def = ContentCatalog.equipmentById(item.defId)
            ?: return ActionOutcome.Failure("装备配置缺失")
        val workshopLevel = bundle.playerState.facilityLevel(FacilityType.WORKSHOP)
        val targetLevel = item.level + 1
        if (targetLevel > 5 && workshopLevel < 5) {
            return ActionOutcome.Failure("星尘精铸（Lv.6~7）需要工坊达到 Lv.5")
        }
        val (goldCost, matCost) = adjustedUpgradeCost(def, targetLevel, bundle.playerState, bundle.inventory)
        val res = bundle.playerState.resources
        if (res.gold < goldCost || res.materialCount(def.craftMaterialId) < matCost) {
            return ActionOutcome.Failure("强化所需金币或材料不足")
        }
        val nextInventory = bundle.inventory.map {
            if (it.instanceId == instanceId) it.copy(level = targetLevel) else it
        }
        val nextPlayer = bundle.playerState.copy(
            resources = res.addGold(-goldCost).addMaterial(def.craftMaterialId, -matCost)
        )
        val nextDiscovery = reconcileUnlocks(nextPlayer, bundle.discoveryState, nextInventory)
        return ActionOutcome.Success(
            bundle.copy(playerState = nextPlayer, discoveryState = nextDiscovery, inventory = nextInventory)
        )
    }

    fun reforgeCost(def: EquipmentDef, item: EquipmentInstance): Pair<Long, Long> {
        val gold = 80L * item.tier.coerceAtLeast(1)
        val mat = 4L * item.tier.coerceAtLeast(1)
        return gold to mat
    }

    fun reforgeEquipmentAffix(
        bundle: GameStateBundle,
        instanceId: String
    ): ActionOutcome<GameStateBundle> {
        val item = bundle.inventory.firstOrNull { it.instanceId == instanceId }
            ?: return ActionOutcome.Failure("背包中未找到该装备")
        val def = ContentCatalog.equipmentById(item.defId)
            ?: return ActionOutcome.Failure("装备配置缺失")
        val (goldCost, matCost) = reforgeCost(def, item)
        val res = bundle.playerState.resources
        if (res.gold < goldCost || res.materialCount(def.craftMaterialId) < matCost) {
            return ActionOutcome.Failure("重铸词条需要 $goldCost 金币与 $matCost ${def.regionId.materialName}")
        }
        val candidates = AffixId.entries.filter { it != item.affix && it != item.secondaryAffix }
        val (rollSeed, nextRngState) = DeterministicRng.advanceSessionSeed(bundle.discoveryState.rngState)
        val newAffix = candidates[DeterministicRng(rollSeed).nextInt(candidates.size)]
        val nextInventory = bundle.inventory.map {
            if (it.instanceId == instanceId) it.copy(affix = newAffix) else it
        }
        val nextPlayer = bundle.playerState.copy(
            resources = res.addGold(-goldCost).addMaterial(def.craftMaterialId, -matCost)
        )
        val rawDiscovery = bundle.discoveryState.copy(
            rngState = nextRngState,
            discoveredAffixIds = bundle.discoveryState.discoveredAffixIds + newAffix
        )
        val nextDiscovery = reconcileUnlocks(nextPlayer, rawDiscovery, nextInventory)
        return ActionOutcome.Success(
            bundle.copy(
                playerState = nextPlayer,
                discoveryState = nextDiscovery,
                inventory = nextInventory
            )
        )
    }

    fun engraveCost(def: EquipmentDef, item: EquipmentInstance): Pair<Long, Long> {
        val gold = 120L * item.tier.coerceAtLeast(1)
        val mat = 5L * item.tier.coerceAtLeast(1)
        return gold to mat
    }

    fun engraveSecondaryAffix(
        bundle: GameStateBundle,
        instanceId: String
    ): ActionOutcome<GameStateBundle> {
        if (bundle.playerState.facilityLevel(FacilityType.WORKSHOP) < 2) {
            return ActionOutcome.Failure("需要工坊升至 Lv.2 方可铭刻第二词条")
        }
        val item = bundle.inventory.firstOrNull { it.instanceId == instanceId }
            ?: return ActionOutcome.Failure("背包中未找到该装备")
        if (item.level < 3) {
            return ActionOutcome.Failure("需将装备强化至 Lv.3 及以上才可铭刻副词条")
        }
        val def = ContentCatalog.equipmentById(item.defId)
            ?: return ActionOutcome.Failure("装备配置缺失")
        val (goldCost, matCost) = engraveCost(def, item)
        val res = bundle.playerState.resources
        if (res.gold < goldCost || res.materialCount(def.craftMaterialId) < matCost) {
            return ActionOutcome.Failure("铭刻副词条需要 $goldCost 金币与 $matCost ${def.regionId.materialName}")
        }
        val candidates = AffixId.entries.filter { it != item.affix && it != item.secondaryAffix }
        val (rollSeed, nextRngState) = DeterministicRng.advanceSessionSeed(bundle.discoveryState.rngState)
        val nextSecondary = candidates[DeterministicRng(rollSeed).nextInt(candidates.size)]
        val nextInventory = bundle.inventory.map {
            if (it.instanceId == instanceId) it.copy(secondaryAffix = nextSecondary) else it
        }
        val nextPlayer = bundle.playerState.copy(
            resources = res.addGold(-goldCost).addMaterial(def.craftMaterialId, -matCost)
        )
        val rawDiscovery = bundle.discoveryState.copy(
            rngState = nextRngState,
            discoveredAffixIds = bundle.discoveryState.discoveredAffixIds + nextSecondary
        )
        val nextDiscovery = reconcileUnlocks(nextPlayer, rawDiscovery, nextInventory)
        return ActionOutcome.Success(
            bundle.copy(
                playerState = nextPlayer,
                discoveryState = nextDiscovery,
                inventory = nextInventory
            )
        )
    }

    fun selectActiveCompanion(bundle: GameStateBundle, companionId: String?): ActionOutcome<GameStateBundle> {
        if (companionId != null) {
            ContentCatalog.companionById(companionId) ?: return ActionOutcome.Failure("未找到伙伴信息")
            val ch = bundle.discoveryState.companionChapters[companionId] ?: 0
            if (ch < 1) {
                return ActionOutcome.Failure("需先解锁该伙伴的第 1 章故事，才可邀请随行出征")
            }
        }
        return ActionOutcome.Success(
            bundle.copy(playerState = bundle.playerState.copy(activeCompanionId = companionId))
        )
    }

    fun selectTeaBlend(bundle: GameStateBundle, teaId: String?): ActionOutcome<GameStateBundle> {
        if (teaId != null) {
            val tea = ContentCatalog.teaBlendById(teaId) ?: return ActionOutcome.Failure("未找到茶饮配方")
            val hearthLv = bundle.playerState.facilityLevel(FacilityType.TEA_HEARTH)
            if (hearthLv < tea.requiredTeaHearthLevel) {
                return ActionOutcome.Failure("需要茶歇火塘 Lv.${tea.requiredTeaHearthLevel} 才可冲泡「${tea.name}」")
            }
        }
        return ActionOutcome.Success(
            bundle.copy(playerState = bundle.playerState.copy(activeTeaBlendId = teaId))
        )
    }

    fun selectDreamBeaconTier(bundle: GameStateBundle, tier: Int): ActionOutcome<GameStateBundle> {
        if (tier !in 0..4) return ActionOutcome.Failure("深梦信标仅支持 0~4 阶")
        val unlocked = bundle.discoveryState.unlockedRouteIds
        val ok = when (tier) {
            0 -> true
            1 -> "route_tidal_shallows" in unlocked || bundle.discoveryState.totalCreditedSeconds >= 24 * 3600L
            2 -> "route_starlit_pass" in unlocked || bundle.discoveryState.totalCreditedSeconds >= 96 * 3600L
            3 -> "route_cloud_bridge" in unlocked || bundle.discoveryState.totalCreditedSeconds >= 176 * 3600L
            else -> "route_dream_shoal" in unlocked || bundle.discoveryState.totalCreditedSeconds >= 248 * 3600L
        }
        if (!ok) {
            val hint = when (tier) {
                1 -> "踏足潮汐滩或累计探索 24 小时后可点亮 1 阶深梦信标"
                2 -> "踏足星陨山脊或累计探索 96 小时后可点亮 2 阶深梦信标"
                3 -> "踏足云栖浮岛或累计探索 176 小时后可点亮 3 阶深梦信标"
                else -> "踏足沉星梦海或累计探索 248 小时后可点亮 4 阶深梦信标"
            }
            return ActionOutcome.Failure(hint)
        }
        return ActionOutcome.Success(
            bundle.copy(playerState = bundle.playerState.copy(dreamBeaconTier = tier))
        )
    }

    fun resolveMorningEcho(bundle: GameStateBundle, chooseOptionA: Boolean): ActionOutcome<MorningEchoResolution> {
        val pending = bundle.playerState.pendingMorningEcho
            ?: return ActionOutcome.Failure("当前没有待处理的晨间奇遇")
        val def = ContentCatalog.morningEchoById(pending.echoId)
            ?: return ActionOutcome.Failure("奇遇配置已缺失")
        val outcome = if (chooseOptionA) def.choiceA else def.choiceB

        var res = bundle.playerState.resources.addGold(outcome.rewardGold)
        if (outcome.rewardMaterialRegion != null && outcome.rewardMaterialAmount > 0L) {
            res = res.addMaterial(outcome.rewardMaterialRegion.materialId, outcome.rewardMaterialAmount)
        }
        val (nextLv, nextXp) = applyXp(bundle.playerState.level, bundle.playerState.xp, outcome.rewardXp)
        val nextPlayer = bundle.playerState.copy(
            level = nextLv,
            xp = nextXp,
            resources = res,
            pendingMorningEcho = null,
            activeMorningBuff = outcome.nextNightBuff ?: bundle.playerState.activeMorningBuff
        )
        val nextDiscovery = reconcileUnlocks(nextPlayer, bundle.discoveryState, bundle.inventory)
        return ActionOutcome.Success(
            MorningEchoResolution(
                updatedBundle = bundle.copy(playerState = nextPlayer, discoveryState = nextDiscovery),
                echoTitle = def.title,
                summaryText = outcome.resultSummary
            )
        )
    }

    fun craftKeepsake(
        bundle: GameStateBundle,
        keepsakeId: String
    ): ActionOutcome<GameStateBundle> {
        val def = ContentCatalog.keepsakeById(keepsakeId)
            ?: return ActionOutcome.Failure("未找到首领信物配置")
        if (keepsakeId in bundle.discoveryState.unlockedKeepsakeIds) {
            return ActionOutcome.Failure("已铭刻「${def.name}」")
        }
        val res = bundle.playerState.resources
        if (res.bossMarks < def.bossMarkCost || res.gold < def.goldCost || res.materialCount(def.materialId) < def.materialCost) {
            return ActionOutcome.Failure("首领印记、金币或地区材料不足，无法铭刻「${def.name}」")
        }
        val nextResources = res
            .addBossMarks(-def.bossMarkCost)
            .addGold(-def.goldCost)
            .addMaterial(def.materialId, -def.materialCost)
        val nextPlayer = bundle.playerState.copy(resources = nextResources)
        val rawDiscovery = bundle.discoveryState.copy(
            unlockedKeepsakeIds = bundle.discoveryState.unlockedKeepsakeIds + keepsakeId
        )
        val nextDiscovery = reconcileUnlocks(nextPlayer, rawDiscovery, bundle.inventory)
        return ActionOutcome.Success(
            bundle.copy(playerState = nextPlayer, discoveryState = nextDiscovery)
        )
    }

    fun equipItem(bundle: GameStateBundle, instanceId: String): ActionOutcome<GameStateBundle> {
        val item = bundle.inventory.firstOrNull { it.instanceId == instanceId }
            ?: return ActionOutcome.Failure("未找到目标装备")
        val def = ContentCatalog.equipmentById(item.defId)
            ?: return ActionOutcome.Failure("装备配置缺失")
        val p = bundle.playerState
        val next = when (def.slot) {
            EquipmentSlot.WEAPON -> p.copy(equippedWeaponId = instanceId)
            EquipmentSlot.ARMOR -> p.copy(equippedArmorId = instanceId)
            EquipmentSlot.CHARM -> p.copy(equippedCharmId = instanceId)
        }
        val nextDiscovery = reconcileUnlocks(next, bundle.discoveryState, bundle.inventory)
        return ActionOutcome.Success(bundle.copy(playerState = next, discoveryState = nextDiscovery))
    }

    fun unequipSlot(bundle: GameStateBundle, slot: EquipmentSlot): GameStateBundle {
        val p = bundle.playerState
        val next = when (slot) {
            EquipmentSlot.WEAPON -> p.copy(equippedWeaponId = null)
            EquipmentSlot.ARMOR -> p.copy(equippedArmorId = null)
            EquipmentSlot.CHARM -> p.copy(equippedCharmId = null)
        }
        return bundle.copy(playerState = next)
    }

    fun toggleLock(bundle: GameStateBundle, instanceId: String): GameStateBundle {
        val nextInv = bundle.inventory.map {
            if (it.instanceId == instanceId) it.copy(locked = !it.locked) else it
        }
        return bundle.copy(inventory = nextInv)
    }

    /**
     * Returns duplicate, unlocked, unequipped item IDs eligible for safe bulk salvage suggestion.
     */
    fun duplicateSalvageCandidates(
        playerState: PlayerState,
        inventory: List<EquipmentInstance>
    ): List<String> {
        val equipped = playerState.equippedInstanceIds()
        val seenKeys = mutableSetOf<Triple<String, AffixId, AffixId?>>()
        val candidates = mutableListOf<String>()
        val sorted = inventory.sortedWith(compareByDescending<EquipmentInstance> { it.instanceId in equipped }
            .thenByDescending { it.locked }
            .thenByDescending { it.level })
        for (item in sorted) {
            val key = Triple(item.defId, item.affix, item.secondaryAffix)
            val isFirst = seenKeys.add(key)
            if (!isFirst && !item.locked && item.instanceId !in equipped) {
                candidates.add(item.instanceId)
            }
        }
        return candidates
    }

    fun salvageSelected(
        bundle: GameStateBundle,
        selectedIds: Set<String>
    ): ActionOutcome<SalvageResult> {
        if (selectedIds.isEmpty()) return ActionOutcome.Failure("请先选择需要拆解的装备")
        val equipped = bundle.playerState.equippedInstanceIds()
        val toSalvage = bundle.inventory.filter {
            it.instanceId in selectedIds && !it.locked && it.instanceId !in equipped
        }
        if (toSalvage.isEmpty()) {
            return ActionOutcome.Failure("选中的装备均已锁定或正在穿戴中，无法拆解")
        }

        var goldGain = 0L
        val matGain = mutableMapOf<String, Long>()
        for (item in toSalvage) {
            val def = ContentCatalog.equipmentById(item.defId) ?: continue
            val mult = 1.0 + (item.level - 1) * 0.4
            goldGain += kotlin.math.round(def.craftGoldCost * 0.35 * mult).toLong().coerceAtLeast(15L)
            val matAmt = kotlin.math.round(def.craftMaterialCost * 0.35 * mult).toLong().coerceAtLeast(2L)
            matGain[def.craftMaterialId] = (matGain[def.craftMaterialId] ?: 0L) + matAmt
        }

        val salvagedIdSet = toSalvage.map { it.instanceId }.toSet()
        val nextInventory = bundle.inventory.filterNot { it.instanceId in salvagedIdSet }
        var nextRes = bundle.playerState.resources.addGold(goldGain)
        for ((matId, amt) in matGain) {
            nextRes = nextRes.addMaterial(matId, amt)
        }
        val nextBundle = bundle.copy(
            playerState = bundle.playerState.copy(resources = nextRes),
            inventory = nextInventory
        )
        return ActionOutcome.Success(
            SalvageResult(
                updatedState = nextBundle,
                salvagedCount = toSalvage.size,
                goldGained = goldGain,
                materialsGained = matGain
            )
        )
    }

    fun togglePassive(bundle: GameStateBundle, passiveId: String): ActionOutcome<GameStateBundle> {
        if (passiveId !in bundle.discoveryState.unlockedPassiveIds) {
            return ActionOutcome.Failure("该被动尚未解锁")
        }
        val maxSlots = maxPassiveSlots(bundle.playerState)
        val current = bundle.playerState.equippedPassives.toMutableList()
        if (passiveId in current) {
            current.remove(passiveId)
        } else {
            if (current.size >= maxSlots) {
                return ActionOutcome.Failure("当前最多同时装配 $maxSlots 个被动，请先卸下一个")
            }
            current.add(passiveId)
        }
        val nextPlayer = bundle.playerState.copy(equippedPassives = current)
        val nextDiscovery = reconcileUnlocks(nextPlayer, bundle.discoveryState, bundle.inventory)
        return ActionOutcome.Success(
            bundle.copy(playerState = nextPlayer, discoveryState = nextDiscovery)
        )
    }

    fun giftCompanion(bundle: GameStateBundle, companionId: String): ActionOutcome<GameStateBundle> {
        val comp = ContentCatalog.companionById(companionId)
            ?: return ActionOutcome.Failure("未找到伙伴信息")
        if ((bundle.discoveryState.companionChapters[companionId] ?: 0) < 1) {
            return ActionOutcome.Failure("需先结识「${comp.name}」（解锁第 1 章）才可赠送心意补给")
        }
        val matId = comp.dispatchRegionId.materialId
        val goldCost = 60L
        val matCost = 4L
        val res = bundle.playerState.resources
        if (res.gold < goldCost || res.materialCount(matId) < matCost) {
            return ActionOutcome.Failure("赠礼需要 $goldCost 金币与 $matCost ${comp.dispatchRegionId.materialName}")
        }
        val currentAffinity = bundle.discoveryState.companionAffinity[companionId] ?: 0
        val nextAffinity = bundle.discoveryState.companionAffinity.toMutableMap().apply {
            put(companionId, currentAffinity + 4)
        }
        val nextPlayer = bundle.playerState.copy(
            resources = res.addGold(-goldCost).addMaterial(matId, -matCost)
        )
        val rawDiscovery = bundle.discoveryState.copy(companionAffinity = nextAffinity)
        val nextDiscovery = reconcileUnlocks(nextPlayer, rawDiscovery, bundle.inventory)
        return ActionOutcome.Success(bundle.copy(playerState = nextPlayer, discoveryState = nextDiscovery))
    }

    fun unlockAstralNode(bundle: GameStateBundle, nodeId: String): ActionOutcome<GameStateBundle> {
        val node = ContentCatalog.astralNodeById(nodeId)
            ?: return ActionOutcome.Failure("未找到星图节点配置")
        if (nodeId in bundle.playerState.unlockedAstralNodeIds) {
            return ActionOutcome.Failure("该星图节点已点亮")
        }
        val available = totalAstralPoints(bundle.playerState.level) - usedAstralPoints(bundle.playerState)
        if (available <= 0) {
            return ActionOutcome.Failure("星图点数不足（每提升 4 级获得 1 点）")
        }
        if (node.tier > 1) {
            val prevNode = ContentCatalog.astralNodes.firstOrNull {
                it.archetype == node.archetype && it.tier == node.tier - 1
            }
            if (prevNode != null && prevNode.id !in bundle.playerState.unlockedAstralNodeIds) {
                return ActionOutcome.Failure("需先点亮「${prevNode.title}」")
            }
        }
        val nextPlayer = bundle.playerState.copy(
            unlockedAstralNodeIds = bundle.playerState.unlockedAstralNodeIds + nodeId
        )
        val nextDiscovery = reconcileUnlocks(nextPlayer, bundle.discoveryState, bundle.inventory)
        return ActionOutcome.Success(bundle.copy(playerState = nextPlayer, discoveryState = nextDiscovery))
    }

    fun resetAstralNodes(bundle: GameStateBundle): ActionOutcome<GameStateBundle> {
        if (bundle.playerState.unlockedAstralNodeIds.isEmpty()) {
            return ActionOutcome.Failure("当前未点亮任何星图节点")
        }
        val resetCost = 100L
        if (bundle.playerState.resources.gold < resetCost) {
            return ActionOutcome.Failure("重塑旅人星图需要 $resetCost 金币")
        }
        val nextPlayer = bundle.playerState.copy(
            resources = bundle.playerState.resources.addGold(-resetCost),
            unlockedAstralNodeIds = emptySet()
        )
        val nextDiscovery = reconcileUnlocks(nextPlayer, bundle.discoveryState, bundle.inventory)
        return ActionOutcome.Success(bundle.copy(playerState = nextPlayer, discoveryState = nextDiscovery))
    }

    fun transmuteMaterial(bundle: GameStateBundle, recipeId: String): ActionOutcome<GameStateBundle> {
        val recipe = ContentCatalog.transmuteRecipeById(recipeId)
            ?: return ActionOutcome.Failure("未找到转化配方")
        val conservatoryLv = bundle.playerState.facilityLevel(FacilityType.CONSERVATORY)
        if (conservatoryLv < recipe.requiredConservatoryLevel) {
            return ActionOutcome.Failure("需要星尘花房 Lv.${recipe.requiredConservatoryLevel} 方可使用此配方")
        }
        val res = bundle.playerState.resources
        if (res.gold < recipe.inputGold || res.materialCount(recipe.inputMaterialId) < recipe.inputMaterialAmount) {
            return ActionOutcome.Failure("转化所需金币或原材料不足")
        }
        var nextRes = res
            .addGold(-recipe.inputGold)
            .addMaterial(recipe.inputMaterialId, -recipe.inputMaterialAmount)
        if (recipe.outputMaterialId != null && recipe.outputMaterialAmount > 0L) {
            nextRes = nextRes.addMaterial(recipe.outputMaterialId, recipe.outputMaterialAmount)
        }
        if (recipe.outputBossMarks > 0L) {
            nextRes = nextRes.addBossMarks(recipe.outputBossMarks)
        }
        val nextPlayer = bundle.playerState.copy(resources = nextRes)
        val nextDiscovery = reconcileUnlocks(nextPlayer, bundle.discoveryState, bundle.inventory)
        return ActionOutcome.Success(bundle.copy(playerState = nextPlayer, discoveryState = nextDiscovery))
    }

    fun saveLoadout(
        bundle: GameStateBundle,
        slotIndex: Int,
        name: String = "方案 ${slotIndex + 1}",
        strategy: ExpeditionStrategy = bundle.playerState.lastStrategy
    ): ActionOutcome<GameStateBundle> {
        if (slotIndex !in 0..2) return ActionOutcome.Failure("预设栏位仅支持 1~3 号位")
        val trimmedName = name.trim().ifEmpty { "方案 ${slotIndex + 1}" }.take(12)
        val p = bundle.playerState
        val preset = LoadoutPreset(
            slotIndex = slotIndex,
            name = trimmedName,
            weaponId = p.equippedWeaponId,
            armorId = p.equippedArmorId,
            charmId = p.equippedCharmId,
            equippedPassives = p.equippedPassives,
            strategy = strategy,
            companionId = p.activeCompanionId,
            teaBlendId = p.activeTeaBlendId
        )
        val nextLoadouts = (p.loadouts.filterNot { it.slotIndex == slotIndex } + preset).sortedBy { it.slotIndex }
        return ActionOutcome.Success(bundle.copy(playerState = p.copy(loadouts = nextLoadouts)))
    }

    fun applyLoadout(bundle: GameStateBundle, slotIndex: Int): ActionOutcome<GameStateBundle> {
        val preset = bundle.playerState.loadouts.firstOrNull { it.slotIndex == slotIndex }
            ?: return ActionOutcome.Failure("预设栏位 ${slotIndex + 1} 尚未保存方案")
        val invById = bundle.inventory.associateBy { it.instanceId }
        val weaponId = preset.weaponId?.takeIf { id ->
            invById[id]?.let { ContentCatalog.equipmentById(it.defId)?.slot == EquipmentSlot.WEAPON } == true
        }
        val armorId = preset.armorId?.takeIf { id ->
            invById[id]?.let { ContentCatalog.equipmentById(it.defId)?.slot == EquipmentSlot.ARMOR } == true
        }
        val charmId = preset.charmId?.takeIf { id ->
            invById[id]?.let { ContentCatalog.equipmentById(it.defId)?.slot == EquipmentSlot.CHARM } == true
        }
        val maxSlots = maxPassiveSlots(bundle.playerState)
        val passives = preset.equippedPassives
            .filter { it in bundle.discoveryState.unlockedPassiveIds }
            .distinct()
            .take(maxSlots)
        val companionId = preset.companionId?.takeIf {
            (bundle.discoveryState.companionChapters[it] ?: 0) >= 1
        }
        val teaHearthLv = bundle.playerState.facilityLevel(FacilityType.TEA_HEARTH)
        val teaId = preset.teaBlendId?.takeIf { id ->
            (ContentCatalog.teaBlendById(id)?.requiredTeaHearthLevel ?: Int.MAX_VALUE) <= teaHearthLv
        }
        val nextPlayer = bundle.playerState.copy(
            equippedWeaponId = weaponId,
            equippedArmorId = armorId,
            equippedCharmId = charmId,
            equippedPassives = passives,
            lastStrategy = preset.strategy,
            activeCompanionId = companionId,
            activeTeaBlendId = teaId
        )
        val nextDiscovery = reconcileUnlocks(nextPlayer, bundle.discoveryState, bundle.inventory)
        return ActionOutcome.Success(bundle.copy(playerState = nextPlayer, discoveryState = nextDiscovery))
    }

    /**
     * Reconciles route, passive, companion chapter, and milestone unlocks from current player, discovery, and inventory state.
     */
    fun reconcileUnlocks(
        playerState: PlayerState,
        discoveryState: DiscoveryState,
        inventory: List<EquipmentInstance> = emptyList()
    ): DiscoveryState {
        val workshopLv = playerState.facilityLevel(FacilityType.WORKSHOP)
        val innLv = playerState.facilityLevel(FacilityType.INN)
        val teaHearthLv = playerState.facilityLevel(FacilityType.TEA_HEARTH)
        val conservatoryLv = playerState.facilityLevel(FacilityType.CONSERVATORY)

        val mistInv = discoveryState.regionInvestigation[RegionId.MISTWOOD.id] ?: 0
        val tidalInv = discoveryState.regionInvestigation[RegionId.TIDAL_FLAT.id] ?: 0
        val starInv = discoveryState.regionInvestigation[RegionId.STARLIT_RIDGE.id] ?: 0
        val cloudInv = discoveryState.regionInvestigation[RegionId.CLOUD_SANCTUARY.id] ?: 0
        val dreamInv = discoveryState.regionInvestigation[RegionId.DREAM_SEA.id] ?: 0
        val astralInv = discoveryState.regionInvestigation[RegionId.ASTRAL_ARCH.id] ?: 0

        val routes = discoveryState.unlockedRouteIds.toMutableSet()
        val investigations = listOf(
            true,
            mistInv >= 3,
            mistInv >= 7,
            mistInv >= 6,
            tidalInv >= 4,
            tidalInv >= 8,
            tidalInv >= 7,
            starInv >= 5,
            starInv >= 9,
            starInv >= 9,
            cloudInv >= 2,
            cloudInv >= 4,
            cloudInv >= 4,
            dreamInv >= 3,
            dreamInv >= 5,
            dreamInv >= 5,
            astralInv >= 3,
            astralInv >= 5
        )
        ContentCatalog.routes.forEachIndexed { index, route ->
            if (investigations[index] && discoveryState.totalCreditedSeconds >= route.requiredExploreHours * 3600L) {
                routes += route.id
            }
        }

        val chapters = discoveryState.companionChapters.toMutableMap()
        for (story in ContentCatalog.companionStories) {
            var unlockedCh = chapters[story.id] ?: 0
            for (ch in story.chapters) {
                if (ch.chapterIndex == unlockedCh + 1) {
                    val regInv = discoveryState.regionInvestigation[ch.regionId.id] ?: 0
                    if (innLv >= ch.requiredInnLevel && regInv >= ch.requiredInvestigation &&
                        discoveryState.totalCreditedSeconds >= ch.requiredExploreHours * 3600L) {
                        unlockedCh = ch.chapterIndex
                    }
                }
            }
            if (unlockedCh > 0) chapters[story.id] = unlockedCh
        }

        val passives = discoveryState.unlockedPassiveIds.toMutableSet()
        passives += "passive_forest_ward"
        if (innLv >= 1 || "route_tidal_shallows" in routes) passives += "passive_tidal_stride"
        if (routes.size >= 2 || (chapters["comp_airen"] ?: 0) >= 1) passives += "passive_keen_eye"
        if (workshopLv >= 2) passives += "passive_artisan_touch"
        if (innLv >= 2 || (chapters["comp_noah"] ?: 0) >= 1) passives += "passive_ember_resolve"
        if ("route_starlit_pass" in routes || (chapters["comp_noah"] ?: 0) >= 2) passives += "passive_star_whisper"
        if ("route_cloud_bridge" in routes || (chapters["comp_lin"] ?: 0) >= 1) passives += "passive_dream_weaver"
        if (innLv >= 3 || (chapters["comp_lin"] ?: 0) >= 2) passives += "passive_cloud_aegis"
        if (workshopLv >= 3 || teaHearthLv >= 2) passives += "passive_thorns_oath"
        if ("route_dream_shoal" in routes || (chapters["comp_xiu"] ?: 0) >= 1) passives += "passive_astral_brush"
        if ("route_astral_arcade" in routes || conservatoryLv >= 2) passives += "passive_amber_anchor"
        if (playerState.level >= 25 || workshopLv >= 4) passives += "passive_chrono_step"
        if (conservatoryLv >= 3 || (chapters["comp_cen"] ?: 0) >= 1) passives += "passive_herbarium_bloom"
        if (discoveryState.discoveredEnemyIds.size >= 24 || (chapters["comp_cen"] ?: 0) >= 2) passives += "passive_bestiary_scholar"

        val milestones = discoveryState.unlockedMilestoneIds.toMutableSet()
        if (discoveryState.totalJourneysCompleted >= 1) milestones += "ms_first_step"
        if (ContentCatalog.routes.filter { it.regionId == RegionId.MISTWOOD }.all { it.id in routes }) milestones += "ms_mistwood_explorer"
        if (ContentCatalog.routes.filter { it.regionId == RegionId.TIDAL_FLAT }.all { it.id in routes }) milestones += "ms_tidal_listener"
        if (ContentCatalog.routes.filter { it.regionId == RegionId.STARLIT_RIDGE }.all { it.id in routes }) milestones += "ms_starlit_watcher"
        if (ContentCatalog.routes.filter {
                it.regionId in setOf(RegionId.MISTWOOD, RegionId.TIDAL_FLAT, RegionId.STARLIT_RIDGE, RegionId.CLOUD_SANCTUARY)
            }.all { it.id in routes }) {
            milestones += "ms_cloud_walker"
        }
        if (playerState.resources.bossMarks > 0L || discoveryState.unlockedKeepsakeIds.isNotEmpty()) milestones += "ms_boss_slayer"
        if (inventory.any { it.level >= 5 }) milestones += "ms_master_artisan"
        if (playerState.facilities.values.any { it >= 4 }) milestones += "ms_camp_builder"
        if (discoveryState.seenEventIds.size >= 30 || discoveryState.discoveredEnemyIds.size >= 18) milestones += "ms_lore_collector"
        if (discoveryState.totalCreditedSeconds >= 160 * 3600L && ContentCatalog.companionStories.count { (chapters[it.id] ?: 0) >= 1 } >= 3) {
            milestones += "ms_night_guardian"
        }
        if (ContentCatalog.routes.filter { it.regionId != RegionId.ASTRAL_ARCH }.all { it.id in routes }) {
            milestones += "ms_dream_sea_voyager"
        }
        if (discoveryState.unlockedSouvenirIds.size >= 8) milestones += "ms_souvenir_curator"
        val equippedItems = inventory.filter { it.instanceId in playerState.equippedInstanceIds() }
        val activeResonances = computeActiveResonances(equippedItems, playerState.equippedPassives)
        if (activeResonances.any { it.tier >= 2 }) {
            milestones += "ms_resonance_master"
        }
        if (discoveryState.dreamBeaconClears.any { (tier, count) -> tier >= 1 && count > 0 }) {
            milestones += "ms_beacon_conqueror"
        }
        if (ContentCatalog.routes.all { it.id in routes }) milestones += "ms_astral_arch_pioneer"
        if (playerState.equippedPassives.size >= 3) milestones += "ms_triple_passive"
        if (activeResonances.any { it.tier >= 3 }) milestones += "ms_resonance_tier3"
        if (inventory.any { it.level >= 7 }) milestones += "ms_masterwork_artisan"
        if (ContentCatalog.chronicles.count { (discoveryState.chronicleProgress[it.id] ?: 0) >= 3 } >= 2) {
            milestones += "ms_chronicle_archivist"
        }
        if (discoveryState.enemyKillCounts.values.count { it >= 15 } >= 12) {
            milestones += "ms_bestiary_expert"
        }

        return discoveryState.copy(
            unlockedRouteIds = routes,
            unlockedPassiveIds = passives,
            companionChapters = chapters,
            unlockedMilestoneIds = milestones
        )
    }
}
