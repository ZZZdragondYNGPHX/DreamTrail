package com.dreamtrail.core.engine

import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.model.*
import com.dreamtrail.core.rng.DeterministicRng
import com.dreamtrail.core.sleep.SleepBuffPolicy
import kotlin.math.roundToLong

data class SimulationOutput(
    val settlement: SettlementResult,
    val updatedBundle: GameStateBundle,
    val ledgerEntries: List<RewardLedgerEntry>
)

/**
 * Pure deterministic expedition simulator.
 * Computation is strictly bounded by at most 96 five-minute nodes per expedition regardless of offline days.
 */
object ExpeditionSimulator {

    fun previewProgress(
        session: ExpeditionSession,
        resolvedDurationSeconds: Long,
        budgetState: RewardBudgetState
    ): ExpeditionPreview {
        val alloc = RewardBudgetCalculator.allocate(budgetState, session.startAtUtcMs / 1000L, resolvedDurationSeconds)
        return ExpeditionPreview(
            sessionId = session.sessionId,
            elapsedSeconds = resolvedDurationSeconds.coerceAtLeast(0L),
            effectiveElapsedSeconds = alloc.creditedSeconds,
            estimatedNodes = alloc.completedNodes,
            recentHighlights = emptyList(),
            budgetNotice = alloc.budgetNote
        )
    }

    fun simulateSettlement(
        session: ExpeditionSession,
        resolvedEndAtUtcMs: Long,
        resolvedDurationSeconds: Long,
        clockAnomalyNote: String?,
        bundle: GameStateBundle
    ): SimulationOutput {
        require(session.contentVersion == ContentCatalog.CONTENT_VERSION && session.rulesVersion == ContentCatalog.RULES_VERSION) {
            "不支持的旅程规则版本，请更新应用后重试"
        }
        val route = requireNotNull(ContentCatalog.routeById(session.routeId)) { "旅程路线不存在" }
        val allocation = RewardBudgetCalculator.allocate(
            budgetState = bundle.budgetState,
            sessionStartUtcSec = session.startAtUtcMs / 1000L,
            rawDurationSeconds = resolvedDurationSeconds
        )

        val creditedSeconds = allocation.creditedSeconds
        val completedNodes = allocation.completedNodes.coerceAtMost(96) // upper safety bound

        val strategyGoldPercent = when (session.strategy) {
            ExpeditionStrategy.STEADY -> 100L
            ExpeditionStrategy.EXPLORE -> 92L
            ExpeditionStrategy.GATHER -> 95L
        }
        val strategyXpPercent = when (session.strategy) {
            ExpeditionStrategy.STEADY -> 100L
            ExpeditionStrategy.EXPLORE -> 105L
            ExpeditionStrategy.GATHER -> 92L
        }
        val strategyMatPercent = when (session.strategy) {
            ExpeditionStrategy.STEADY -> 100L
            ExpeditionStrategy.EXPLORE -> 88L
            ExpeditionStrategy.GATHER -> 125L
        }
        val affixes = session.snapshot.equippedItems.flatMap { listOfNotNull(it.affix, it.secondaryAffix) }.toSet()
        val passives = session.snapshot.equippedPassives.toSet()
        val keepsakes = session.snapshot.unlockedKeepsakeIds
        val souvenirs = session.snapshot.unlockedSouvenirIds
        val tea = session.snapshot.activeTeaBlendId
            ?.let { ContentCatalog.teaBlendById(it) }
            ?.takeIf { (session.snapshot.facilityLevels[FacilityType.TEA_HEARTH] ?: 0) >= it.requiredTeaHearthLevel }
        val phenom = session.nightPhenomenon
        val phenomMatches = phenom != null &&
            (phenom.favoredRegionId == route.regionId || phenom.favoredEnvironment == route.environmentTag)
        val beaconTier = session.snapshot.dreamBeaconTier.coerceIn(0, 4)
        val conservatoryLv = session.snapshot.facilityLevels[FacilityType.CONSERVATORY] ?: 0

        var goldBonusPercent = 100L
        var xpPercent = if ("passive_ember_resolve" in passives) 108L else 100L
        if ("passive_dream_weaver" in passives) xpPercent += 6L
        if ("passive_astral_brush" in passives && route.regionId == RegionId.DREAM_SEA) goldBonusPercent += 8L
        if (AffixId.STARLIGHT_INSIGHT in affixes) {
            goldBonusPercent += 6L
            xpPercent += 6L
        }
        var matPercent = if (AffixId.SALVAGE_INSIGHT in affixes) 110L else 100L
        if ("passive_herbarium_bloom" in passives) matPercent += 8L
        if (conservatoryLv > 0) matPercent += conservatoryLv * 2L
        if ("keepsake_dream_catcher" in keepsakes) {
            goldBonusPercent += 5L
            xpPercent += 5L
        }
        if ("keepsake_stag_lantern" in keepsakes && route.regionId == RegionId.MISTWOOD) {
            xpPercent += 5L
        }
        if ("keepsake_tidal_chime" in keepsakes && route.regionId == RegionId.TIDAL_FLAT) {
            matPercent += 8L
        }
        if ("keepsake_star_orrery" in keepsakes && route.regionId == RegionId.STARLIT_RIDGE) {
            goldBonusPercent += 8L
        }
        if ("keepsake_abyss_astrolabe" in keepsakes && route.regionId == RegionId.DREAM_SEA) {
            goldBonusPercent += 8L
            matPercent += 8L
        }
        if ("keepsake_astral_pendulum" in keepsakes) {
            xpPercent += 6L
            if (route.regionId == RegionId.ASTRAL_ARCH) matPercent += 8L
        }
        for (nodeId in session.snapshot.unlockedAstralNodeIds) {
            val node = ContentCatalog.astralNodeById(nodeId) ?: continue
            goldBonusPercent += node.goldBonusPercent
            xpPercent += node.xpBonusPercent
            matPercent += node.materialBonusPercent
        }
        for (chron in ContentCatalog.chronicles) {
            if ((session.snapshot.chronicleProgress[chron.id] ?: 0) >= 3) {
                goldBonusPercent += chron.rewardGoldBonusPercent
                xpPercent += chron.rewardXpBonusPercent
                matPercent += chron.rewardMaterialBonusPercent
            }
        }
        if (tea != null) {
            goldBonusPercent += (tea.goldMultiplierBonus * 100.0).roundToLong()
            xpPercent += (tea.xpMultiplierBonus * 100.0).roundToLong()
        }
        if (phenom != null &&
            (phenom.favoredRegionId == route.regionId || phenom.favoredEnvironment == route.environmentTag)
        ) {
            goldBonusPercent += phenom.goldBonusPercent
            xpPercent += phenom.xpBonusPercent
            matPercent += phenom.materialBonusPercent
        }
        session.snapshot.activeMorningBuff?.let { buff ->
            goldBonusPercent += buff.goldBonusPercent
            xpPercent += buff.xpBonusPercent
            matPercent += buff.materialBonusPercent
        }
        for (souvId in souvenirs) {
            val souv = ContentCatalog.souvenirById(souvId) ?: continue
            goldBonusPercent += souv.goldBonusPercent
            xpPercent += souv.xpBonusPercent
            matPercent += souv.materialBonusPercent
        }
        if (beaconTier > 0) {
            goldBonusPercent += beaconTier * 15L
            xpPercent += beaconTier * 15L
            matPercent += beaconTier * 12L
        }

        val fractions = ResourceAccrual(bundle.budgetState.resourceRemainders)
        var baseGold = fractions.accrue("gold", creditedSeconds * route.baseGoldPerHour * strategyGoldPercent * goldBonusPercent)
        var baseXp = fractions.accrue("xp", creditedSeconds * route.baseXpPerHour * strategyXpPercent * xpPercent)
        var baseMat = fractions.accrue(route.regionId.materialId, creditedSeconds * route.baseMaterialPerHour * strategyMatPercent * matPercent)

        // Track mutable discovery & budget progression inside this deterministic run
        val seenEvents = bundle.discoveryState.seenEventIds.toMutableSet()
        val cooldowns = bundle.budgetState.eventCooldownUntilSeconds.toMutableMap()
        val priorRegionSeconds = bundle.discoveryState.regionCreditedSeconds[route.regionId.id] ?: 0L
        val investigation = bundle.discoveryState.regionInvestigation.toMutableMap()
        val discoveredEnemies = bundle.discoveryState.discoveredEnemyIds.toMutableSet()
        val discoveredEquipDefs = bundle.discoveryState.discoveredEquipmentDefIds.toMutableSet()
        val discoveredAffixes = bundle.discoveryState.discoveredAffixIds.toMutableSet()

        var dangerFailures = 0
        var switchedToSafeGather = false
        var bossMarksGained = 0L
        val failureLimit = 3 + (tea?.extraFailureTolerance ?: 0)

        var gatherCount = 0
        var victoryCount = 0
        var restCount = 0
        var clueCount = 0

        val candidateKeyEvents = mutableListOf<Pair<Int, KeyEventSummary>>() // priority to summary
        val regionEvents = ContentCatalog.eventsForRegion(route.regionId)
        val dangerRate = effectiveDangerRate(route, session.strategy)

        var bossProgress = bundle.budgetState.bossProgressSecondsByRegion[route.regionId.id] ?: 0
        val currentRegionInv = investigation[route.regionId.id] ?: 0
        var bossTriggeredThisRun = false
        val killCounts = bundle.discoveryState.enemyKillCounts.toMutableMap()
        var eliteVictories = 0

        for (nodeIndex in 0 until completedNodes) {
            val creditedAtNode = ((nodeIndex + 1) * 300L - bundle.budgetState.remainderSeconds).coerceIn(0, creditedSeconds)
            val regionSeconds = priorRegionSeconds + creditedAtNode
            bossProgress += if (nodeIndex == 0) creditedAtNode.toInt() else 300

            val nodeRng = DeterministicRng.forNode(session.sessionSeed, nodeIndex, streamTag = 0)
            val combatRng = DeterministicRng.forNode(session.sessionSeed, nodeIndex, streamTag = 1)

            val canTriggerBoss = !bossTriggeredThisRun &&
                !switchedToSafeGather &&
                (investigation[route.regionId.id] ?: currentRegionInv) >= route.bossInvestigationRequired &&
                bossProgress >= 21_600 &&
                regionSeconds >= 24 * 3600L

            if (canTriggerBoss) {
                bossTriggeredThisRun = true
                bossProgress = 0 // Do not stockpile boss attempts before the region becomes accessible.
                val bossDef = ContentCatalog.enemyById(route.bossId)
                if (bossDef != null) {
                    discoveredEnemies += bossDef.id
                    val combat = CombatSimulator.simulate(session.snapshot, route, listOf(bossDef), combatRng, phenom)
                    if (combat.outcome == CombatOutcome.VICTORY) {
                        killCounts[bossDef.id] = (killCounts[bossDef.id] ?: 0) + 1
                        val beaconBonusMarks = when {
                            beaconTier >= 3 -> 2L
                            beaconTier >= 1 -> 1L
                            else -> 0L
                        }
                        val marksWon = 1L + beaconBonusMarks
                        bossMarksGained += marksWon
                        baseGold += 45L + beaconTier * 20L
                        baseXp += 55L + beaconTier * 25L
                        candidateKeyEvents += 100 to KeyEventSummary(
                            nodeIndex = nodeIndex,
                            title = "首领捷报 · ${bossDef.name}",
                            detail = "历经 ${combat.roundsPlayed} 回合交锋后击败${bossDef.name}，获得 $marksWon 枚首领印记。",
                            isFirstDiscovery = false,
                            highlightReason = combat.buildHighlight
                        )
                    } else {
                        dangerFailures++
                        if (dangerFailures >= failureLimit) switchedToSafeGather = true
                        baseXp += 15L
                        candidateKeyEvents += 95 to KeyEventSummary(
                            nodeIndex = nodeIndex,
                            title = "试探首领 · ${bossDef.name}",
                            detail = combat.failureReason ?: "暂未击败${bossDef.name}，伙伴携带既有收获安全撤离。",
                            isFirstDiscovery = false,
                            highlightReason = "保留已有收益，无战利品损失"
                        )
                    }
                    continue
                }
            }

            if (!switchedToSafeGather && nodeRng.nextDouble() < dangerRate) {
                val enemyCount = if (route.recommendedPowerTier >= 2 && nodeRng.nextDouble() < 0.35) 2 else 1
                val pickedEnemies = (0 until enemyCount).mapNotNull {
                    val eid = route.enemyIds[nodeRng.nextInt(route.enemyIds.size)]
                    ContentCatalog.enemyById(eid)
                }
                for (e in pickedEnemies) discoveredEnemies += e.id

                val eliteTrait = if (pickedEnemies.isNotEmpty() &&
                    (beaconTier >= 1 || route.regionId == RegionId.ASTRAL_ARCH)
                ) {
                    val eliteRng = DeterministicRng.forNode(session.sessionSeed, nodeIndex, streamTag = 6)
                    val eliteChance = (0.10 + beaconTier * 0.05).coerceAtMost(0.32)
                    if (eliteRng.nextDouble() < eliteChance) {
                        EliteTrait.entries[eliteRng.nextInt(EliteTrait.entries.size)]
                    } else null
                } else {
                    null
                }

                val combat = CombatSimulator.simulate(session.snapshot, route, pickedEnemies, combatRng, phenom, eliteTrait)
                if (combat.outcome == CombatOutcome.VICTORY) {
                    victoryCount++
                    for (e in pickedEnemies) killCounts[e.id] = (killCounts[e.id] ?: 0) + 1
                    baseGold += 2L + route.recommendedPowerTier
                    baseXp += 3L + route.recommendedPowerTier
                    if (eliteTrait != null) {
                        eliteVictories++
                        baseGold += 6L + route.recommendedPowerTier * 3L
                        baseXp += 8L + route.recommendedPowerTier * 4L
                    }
                    if (nodeRng.nextDouble() < 0.15) baseMat += 1L
                    if (combat.buildHighlight != null) {
                        val prefix = if (eliteTrait != null) "迎战精英「${eliteTrait.title}」" else "迎战"
                        candidateKeyEvents += (if (eliteTrait != null) 78 else 60) to KeyEventSummary(
                            nodeIndex = nodeIndex,
                            title = "$prefix${pickedEnemies.joinToString("、") { it.name }}",
                            detail = "以剩余 ${combat.remainingHp}/${combat.maxHp} 生命取胜，带回额外战利品。",
                            isFirstDiscovery = false,
                            highlightReason = combat.buildHighlight
                        )
                    }
                } else {
                    dangerFailures++
                    baseXp += 5L
                    if (dangerFailures >= failureLimit) {
                        switchedToSafeGather = true
                        candidateKeyEvents += 85 to KeyEventSummary(
                            nodeIndex = nodeIndex,
                            title = "稳妥转为安全搜集",
                            detail = (combat.failureReason ?: "前方强敌密集") + "；伙伴累计 $failureLimit 次受挫后已主动转向安全路段搜集，保障后续收益。",
                            isFirstDiscovery = false,
                            highlightReason = "失败保护机制生效"
                        )
                    } else {
                        candidateKeyEvents += 55 to KeyEventSummary(
                            nodeIndex = nodeIndex,
                            title = "遭遇强敌后暂避",
                            detail = combat.failureReason ?: "避开危险路段并保留全部既有收获。",
                            isFirstDiscovery = false,
                            highlightReason = "已保留前面路段所得"
                        )
                    }
                }
            } else {
                val curInv = investigation[route.regionId.id] ?: 0
                val eligible = regionEvents.filter { ev ->
                    ev.minInvestigation <= curInv &&
                        regionSeconds >= ev.minRegionExploreHours * 3600L &&
                        (cooldowns[ev.id] ?: 0L) <= regionSeconds &&
                        (ev.requiredStrategy == null || ev.requiredStrategy == session.strategy || switchedToSafeGather)
                }.ifEmpty { regionEvents.filter { it.kind == EventKind.GATHER && it.minRegionExploreHours == 0 && it.requiredStrategy == null } }

                val picked = nodeRng.pickWeighted(eligible) { ev ->
                    var w = ev.weight.coerceAtLeast(1)
                    if (ev.id !in seenEvents) w = (w * 1.6).toInt()
                    if (ev.kind == EventKind.CLUE) {
                        if (session.strategy == ExpeditionStrategy.EXPLORE) w = (w * 1.7).toInt()
                        if (AffixId.CLUE_SEEKER in affixes) w = (w * 1.35).toInt()
                        if (AffixId.ASTRAL_HARMONY in affixes) w = (w * 1.15).toInt()
                        if ("passive_keen_eye" in passives) w = (w * 1.25).toInt()
                        if ("passive_bestiary_scholar" in passives) w = (w * 1.18).toInt()
                        w = (w * (1.0 + (session.snapshot.facilityLevels[FacilityType.MAP_TABLE] ?: 0) * 0.04)).toInt()
                    }
                    if (ev.kind == EventKind.GATHER && session.strategy == ExpeditionStrategy.GATHER) {
                        w = (w * 1.6).toInt()
                    }
                    if (switchedToSafeGather && ev.kind == EventKind.GATHER) {
                        w = (w * 1.8).toInt()
                    }
                    w.coerceAtLeast(1)
                } ?: eligible.first()

                val isFirst = seenEvents.add(picked.id)
                if (picked.cooldownJourneys > 0) {
                    cooldowns[picked.id] = regionSeconds + picked.cooldownJourneys * 8 * 3600L
                }

                val scale = if (isFirst) 0.25 else 0.08
                var evGold = (picked.bonusGold * scale).roundToLong()
                var evXp = (picked.bonusXp * scale).roundToLong()
                var evMat = (picked.bonusMaterial * scale).roundToLong()

                if (picked.kind == EventKind.GATHER && AffixId.BOUNTIFUL_GATHER in affixes) {
                    evGold += 2L
                }
                if (picked.kind == EventKind.GATHER && "passive_forest_ward" in passives && route.regionId == RegionId.MISTWOOD) {
                    if (nodeRng.nextDouble() < 0.15) evMat += 1L
                }
                if (picked.kind == EventKind.REST) {
                    if (AffixId.DREAM_RESONANCE in affixes) {
                        evGold = (evGold * 1.35).roundToLong()
                        evXp = (evXp * 1.35).roundToLong()
                    }
                    if (AffixId.LUCID_PULSE in affixes) {
                        evGold = (evGold * 1.15).roundToLong()
                        evXp = (evXp * 1.15).roundToLong()
                    }
                    if (conservatoryLv > 0) {
                        evGold += conservatoryLv * 2L
                        evXp += conservatoryLv * 3L
                    }
                    if ("passive_dream_weaver" in passives) {
                        evGold += 4L
                        evXp += 5L
                    }
                    if ("passive_astral_brush" in passives) {
                        evGold += 3L
                        evXp += 4L
                    }
                }

                baseGold += evGold
                baseXp += evXp
                baseMat += evMat

                val invGain = if (isFirst) picked.investigationGain else (if (picked.kind == EventKind.CLUE) 1 else 0)
                if (invGain > 0) {
                    investigation[route.regionId.id] = (investigation[route.regionId.id] ?: 0) + invGain
                }

                when (picked.kind) {
                    EventKind.GATHER -> gatherCount++
                    EventKind.CLUE -> clueCount++
                    EventKind.REST -> restCount++
                    EventKind.ENCOUNTER -> victoryCount++
                }

                val priority = when {
                    isFirst && picked.companionId != null -> 90
                    isFirst && picked.kind == EventKind.CLUE -> 80
                    isFirst -> 70
                    picked.kind == EventKind.CLUE -> 45
                    else -> 20
                }
                val highlightReason = when {
                    picked.kind == EventKind.CLUE && AffixId.CLUE_SEEKER in affixes -> "「秘闻寻踪」词条提高了线索发现几率"
                    picked.kind == EventKind.GATHER && AffixId.BOUNTIFUL_GATHER in affixes -> "「丰饶采集」额外带来金币收入"
                    picked.kind == EventKind.REST && AffixId.DREAM_RESONANCE in affixes -> "「梦境共鸣」深化了休整恢复与旅途收获"
                    picked.kind == EventKind.REST && "passive_dream_weaver" in passives -> "「织梦呼吸」提升了安稳休整的收益"
                    isFirst -> "首次发现"
                    else -> null
                }
                candidateKeyEvents += priority to KeyEventSummary(
                    nodeIndex = nodeIndex,
                    title = picked.title,
                    detail = if (isFirst) picked.firstDiscoveryText else picked.repeatText,
                    isFirstDiscovery = isFirst,
                    highlightReason = highlightReason
                )
            }
        }

        val nodeCreditedSeconds = if (completedNodes == 0) 0L else
            (completedNodes * 300L - bundle.budgetState.remainderSeconds).coerceIn(0, creditedSeconds)
        bossProgress += (creditedSeconds - nodeCreditedSeconds).toInt()

        // Equipment rolls: 1 guaranteed roll per 2 cumulative credited hours (7200s)
        var equipProgressPct = 100L
        if (AffixId.TREASURE_SENSE in affixes) equipProgressPct += 15L
        if (AffixId.ASTRAL_HARMONY in affixes) equipProgressPct += 15L
        if ("keepsake_cloud_music_box" in keepsakes) equipProgressPct += 10L
        if ("passive_astral_brush" in passives) equipProgressPct += 8L
        if (tea != null) equipProgressPct += (tea.dropRateBonus * 100.0).roundToLong()
        if (phenom != null &&
            (phenom.favoredRegionId == route.regionId || phenom.favoredEnvironment == route.environmentTag)
        ) {
            equipProgressPct += phenom.dropBonusPercent
        }
        val equipNumerator = creditedSeconds * equipProgressPct + bundle.budgetState.equipmentProgressRemainder
        val addedEquipSeconds = (equipNumerator / 100L).toInt()
        var equipProgress = bundle.budgetState.equipmentProgressSeconds + addedEquipSeconds
        val equipRolls = equipProgress / 7200
        equipProgress %= 7200

        val regionPool = ContentCatalog.equipmentPoolForRegion(route.regionId)
        val rarePityMap = bundle.budgetState.rarePityByRegion.toMutableMap()
        var regionPity = rarePityMap[route.regionId.id] ?: 0

        val updatedInventory = bundle.inventory.toMutableList()
        val newEquipmentList = mutableListOf<EquipmentInstance>()
        var duplicateConvertedGold = 0L

        for (rollIdx in 0 until equipRolls) {
            val dropRng = DeterministicRng.forNode(session.sessionSeed, 10_000 + rollIdx, streamTag = 2)
            val unowned = regionPool.filter { it.id !in discoveredEquipDefs }
            val pickedDef = if (regionPity >= 5 && unowned.isNotEmpty()) {
                unowned[dropRng.nextInt(unowned.size)]
            } else {
                regionPool[dropRng.nextInt(regionPool.size)]
            }

            if (pickedDef.id !in discoveredEquipDefs) {
                regionPity = 0
            } else {
                regionPity++
            }

            val affix = pickedDef.defaultAffix
            discoveredEquipDefs += pickedDef.id
            discoveredAffixes += affix

            val sameInBag = updatedInventory.count { it.defId == pickedDef.id && it.affix == affix }
            if (sameInBag >= 3) {
                duplicateConvertedGold += (pickedDef.craftGoldCost * 0.4).roundToLong().coerceAtLeast(25L)
            } else {
                val instance = EquipmentInstance(
                    instanceId = "${session.sessionId}_eq_$rollIdx",
                    defId = pickedDef.id,
                    tier = pickedDef.tier,
                    level = 1,
                    affix = affix,
                    locked = false
                )
                updatedInventory += instance
                newEquipmentList += instance
            }
        }
        rarePityMap[route.regionId.id] = regionPity

        baseGold += duplicateConvertedGold

        // First onboarding journey milestone (only once per save, requires >= 5s credited travel)
        var completedOnboardingNow = bundle.discoveryState.onboardingCompleted
        if (session.isOnboarding && !bundle.discoveryState.onboardingCompleted && creditedSeconds >= 5L) {
            completedOnboardingNow = true
            if (candidateKeyEvents.isEmpty()) {
                val introEvent = ContentCatalog.eventTemplates.firstOrNull { it.id == "ev_mw_tidal_tablet" }
                    ?: regionEvents.firstOrNull()
                if (introEvent != null) {
                    val isFirstIntro = seenEvents.add(introEvent.id)
                    investigation[route.regionId.id] = (investigation[route.regionId.id] ?: 0) + introEvent.investigationGain.coerceAtLeast(1)
                    candidateKeyEvents += 88 to KeyEventSummary(
                        nodeIndex = 0,
                        title = introEvent.title,
                        detail = if (isFirstIntro) introEvent.firstDiscoveryText else introEvent.repeatText,
                        isFirstDiscovery = isFirstIntro,
                        highlightReason = "初次踏入林道的特别发现"
                    )
                }
            }
            if (newEquipmentList.isEmpty() && updatedInventory.isEmpty()) {
                val starterDef = ContentCatalog.equipmentById("eq_pine_staff")
                if (starterDef != null) {
                    val starterItem = EquipmentInstance(
                        instanceId = "${session.sessionId}_eq_onboarding",
                        defId = starterDef.id,
                        tier = starterDef.tier,
                        level = 1,
                        affix = starterDef.defaultAffix,
                        locked = false
                    )
                    updatedInventory += starterItem
                    newEquipmentList += starterItem
                    discoveredEquipDefs += starterDef.id
                    discoveredAffixes += starterDef.defaultAffix
                }
            }
            if (baseGold < 60L) baseGold = 60L
            if (baseXp < 60L) baseXp = 60L
            if (baseMat < 6L) baseMat = 6L
        }

        // Souvenir drop check (max 1 new region souvenir per expedition >= 1h)
        val unlockedSouvenirs = bundle.discoveryState.unlockedSouvenirIds.toMutableSet()
        val newSouvenirIds = mutableListOf<String>()
        if (completedNodes >= 12) {
            val missingSouvenirs = ContentCatalog.souvenirsForRegion(route.regionId).filter { it.id !in unlockedSouvenirs }
            if (missingSouvenirs.isNotEmpty()) {
                val souvRng = DeterministicRng.forNode(session.sessionSeed, 20_000, streamTag = 3)
                val chance = (0.30 + (creditedSeconds / 3600.0) * 0.08 +
                    (if (session.snapshot.activeCompanionId == "comp_xiu") 0.12 else 0.0) +
                    (tea?.dropRateBonus ?: 0.0)).coerceIn(0.25, 0.92)
                if (souvRng.nextDouble() < chance) {
                    val pickedSouv = missingSouvenirs[souvRng.nextInt(missingSouvenirs.size)]
                    unlockedSouvenirs += pickedSouv.id
                    newSouvenirIds += pickedSouv.id
                    candidateKeyEvents += 92 to KeyEventSummary(
                        nodeIndex = (completedNodes - 1).coerceAtLeast(0),
                        title = "风物志收录 · ${pickedSouv.name}",
                        detail = "在${route.regionId.displayName}带回独特纪念物「${pickedSouv.name}」：${pickedSouv.description}",
                        isFirstDiscovery = true,
                        highlightReason = "旅途风物志永久收藏"
                    )
                }
            }
        }

        // Camp Companion Dispatch yields (credit automatically per 2h for unlocked idle companions)
        val materialsGainedMap = mutableMapOf<String, Long>()
        if (baseMat > 0L) {
            materialsGainedMap[route.regionId.materialId] = baseMat
        }
        val dispatchSummaries = mutableListOf<String>()
        val dispatchCycles = (creditedSeconds / 7200L).toInt().coerceAtMost(6)
        val bellScale = if ("keepsake_conservatory_bell" in keepsakes) 1.15 else 1.0
        if (dispatchCycles > 0 && session.snapshot.dispatchCompanionIds.isNotEmpty()) {
            for (compId in session.snapshot.dispatchCompanionIds) {
                val affRank = ProgressionEngine.companionAffinityRank(session.snapshot.companionAffinity[compId] ?: 0)
                val compScale = bellScale * (if (affRank >= 2) 1.25 else 1.0)
                when (compId) {
                    "comp_airen" -> {
                        val extraGold = (18L * dispatchCycles * compScale).roundToLong()
                        val extraMat = (1L * dispatchCycles * compScale).roundToLong().coerceAtLeast(1L)
                        baseGold += extraGold
                        materialsGainedMap[RegionId.MISTWOOD.materialId] =
                            (materialsGainedMap[RegionId.MISTWOOD.materialId] ?: 0L) + extraMat
                        dispatchSummaries += "艾琳跑腿递送：+$extraGold 金币，+$extraMat ${RegionId.MISTWOOD.materialName}"
                    }
                    "comp_noah" -> {
                        val extraXp = (24L * dispatchCycles * compScale).roundToLong()
                        val extraGold = (10L * dispatchCycles * compScale).roundToLong()
                        baseXp += extraXp
                        baseGold += extraGold
                        dispatchSummaries += "诺亚整理星图：+$extraXp 经验，+$extraGold 金币"
                    }
                    "comp_lin" -> {
                        val extraGold = (14L * dispatchCycles * compScale).roundToLong()
                        val extraMat = (2L * dispatchCycles * compScale).roundToLong().coerceAtLeast(1L)
                        baseGold += extraGold
                        materialsGainedMap[RegionId.CLOUD_SANCTUARY.materialId] =
                            (materialsGainedMap[RegionId.CLOUD_SANCTUARY.materialId] ?: 0L) + extraMat
                        dispatchSummaries += "琳照料云圃：+$extraMat ${RegionId.CLOUD_SANCTUARY.materialName}，+$extraGold 金币"
                    }
                    "comp_xiu" -> {
                        val extraGold = (24L * dispatchCycles * compScale).roundToLong()
                        val matRegion = if ("route_dream_shoal" in bundle.discoveryState.unlockedRouteIds) {
                            RegionId.DREAM_SEA
                        } else {
                            RegionId.STARLIT_RIDGE
                        }
                        val extraMat = (1L * dispatchCycles * compScale).roundToLong().coerceAtLeast(1L)
                        baseGold += extraGold
                        materialsGainedMap[matRegion.materialId] =
                            (materialsGainedMap[matRegion.materialId] ?: 0L) + extraMat
                        dispatchSummaries += "汐绘稿寻珍：+$extraGold 金币，+$extraMat ${matRegion.materialName}"
                    }
                    "comp_cen" -> {
                        val extraXp = (28L * dispatchCycles * compScale).roundToLong()
                        val extraGold = (12L * dispatchCycles * compScale).roundToLong()
                        val matRegion = if ("route_astral_arcade" in bundle.discoveryState.unlockedRouteIds) {
                            RegionId.ASTRAL_ARCH
                        } else {
                            RegionId.DREAM_SEA
                        }
                        val extraMat = (1L * dispatchCycles * compScale).roundToLong().coerceAtLeast(1L)
                        baseXp += extraXp
                        baseGold += extraGold
                        materialsGainedMap[matRegion.materialId] =
                            (materialsGainedMap[matRegion.materialId] ?: 0L) + extraMat
                        dispatchSummaries += "岑巡护钟廊：+$extraXp 经验，+$extraGold 金币，+$extraMat ${matRegion.materialName}"
                    }
                }
            }
        }

        val nextAffinity = bundle.discoveryState.companionAffinity.toMutableMap()
        if (completedNodes >= 6) {
            session.snapshot.activeCompanionId?.let { cid ->
                nextAffinity[cid] = (nextAffinity[cid] ?: 0) + 2
            }
            for (cid in session.snapshot.dispatchCompanionIds) {
                nextAffinity[cid] = (nextAffinity[cid] ?: 0) + 1
            }
        }

        // Camp Commissions (auto-completed once when >= requiredMinNodes in matching region)
        val nextCompletedCommissions = bundle.discoveryState.completedCommissionIds.toMutableSet()
        val completedCommissionTitles = mutableListOf<String>()
        if (!switchedToSafeGather) {
            for (comm in ContentCatalog.commissions) {
                if (comm.regionId == route.regionId &&
                    comm.id !in nextCompletedCommissions &&
                    completedNodes >= comm.requiredMinNodes
                ) {
                    nextCompletedCommissions += comm.id
                    completedCommissionTitles += comm.title
                    baseGold += comm.rewardGold
                    baseXp += comm.rewardXp
                    comm.rewardMaterialRegion?.let { matReg ->
                        if (comm.rewardMaterialAmount > 0L) {
                            materialsGainedMap[matReg.materialId] =
                                (materialsGainedMap[matReg.materialId] ?: 0L) + comm.rewardMaterialAmount
                        }
                    }
                    if (comm.rewardBossMarks > 0L) {
                        bossMarksGained += comm.rewardBossMarks
                    }
                    val affTarget = session.snapshot.activeCompanionId
                        ?: ContentCatalog.companionStories.firstOrNull {
                            (bundle.discoveryState.companionChapters[it.id] ?: 0) >= 1
                        }?.id
                    if (affTarget != null && comm.rewardAffinity > 0) {
                        nextAffinity[affTarget] = (nextAffinity[affTarget] ?: 0) + comm.rewardAffinity
                    }
                    candidateKeyEvents += 93 to KeyEventSummary(
                        nodeIndex = (completedNodes - 1).coerceAtLeast(0),
                        title = "委托完成 · ${comm.title}",
                        detail = "顺利达成${route.regionId.displayName}营地委托，获赠 ${comm.rewardGold} 金币与 ${comm.rewardXp} 经验。",
                        isFirstDiscovery = true,
                        highlightReason = "营地委托赏格"
                    )
                }
            }
        }

        // Chronicle Quest progression (>= 6 nodes, checks region investigation & region explore hours)
        val nextChronicleProgress = bundle.discoveryState.chronicleProgress.toMutableMap()
        var chronicleUpdateText: String? = null
        val totalRegionSecondsAfter = priorRegionSeconds + creditedSeconds
        if (completedNodes >= 6) {
            val chron = ContentCatalog.chronicleForRegion(route.regionId)
            if (chron != null) {
                var stageIdx = nextChronicleProgress[chron.id] ?: 0
                val curRegionInv = investigation[route.regionId.id] ?: 0
                for (stage in chron.stages) {
                    if (stage.stageIndex == stageIdx + 1 &&
                        curRegionInv >= stage.requiredInvestigation &&
                        totalRegionSecondsAfter >= stage.requiredExploreHours * 3600L
                    ) {
                        stageIdx = stage.stageIndex
                        chronicleUpdateText = if (stageIdx >= 3) chron.completionTitle else "「${chron.title}」推进至：${stage.title}"
                        candidateKeyEvents += 94 to KeyEventSummary(
                            nodeIndex = (completedNodes - 1).coerceAtLeast(0),
                            title = "秘闻卷宗 · ${stage.title}",
                            detail = stage.summary,
                            isFirstDiscovery = true,
                            highlightReason = if (stageIdx >= 3) "秘闻终章永久加成" else "区域秘闻推进"
                        )
                    }
                }
                if (stageIdx > 0) {
                    nextChronicleProgress[chron.id] = stageIdx
                }
            }
        }

        // Base +3% resting blessing (Stage-1 always credits 3%; wearable data later tops up delta via SleepBuffPolicy)
        val goldCarry = fractions.remainders["blessing_gold"] ?: 0L
        val xpCarry = fractions.remainders["blessing_xp"] ?: 0L
        val baseBuff = SleepBuffPolicy.computeBuff(baseGold, baseXp, sleepMatch = null,
            goldRemainder = goldCarry, xpRemainder = xpCarry)
        fractions.accrue("blessing_gold", baseGold * 300, 10000)
        fractions.accrue("blessing_xp", baseXp * 300, 10000)
        val totalGoldGain = baseGold + baseBuff.targetBlessingGold
        val totalXpGain = baseXp + baseBuff.targetBlessingXp

        val (nextLevel, nextXp) = ProgressionEngine.applyXp(
            startLevel = bundle.playerState.level,
            startXp = bundle.playerState.xp,
            gainedXp = totalXpGain
        )

        val materialsMap = materialsGainedMap.toMap()

        var nextRes = bundle.playerState.resources
            .addGold(totalGoldGain)
            .addBossMarks(bossMarksGained)
        for ((matId, amt) in materialsMap) {
            if (amt > 0L) {
                nextRes = nextRes.addMaterial(matId, amt)
            }
        }

        // Morning echo generation for sessions with night phenomenon & >= 30min (6 nodes)
        val pendingEcho = if (phenom != null && completedNodes >= 6) {
            val echoes = ContentCatalog.morningEchoesForRegion(route.regionId)
            val echoRng = DeterministicRng.forNode(session.sessionSeed, 30_000, streamTag = 4)
            echoes[echoRng.nextInt(echoes.size)].let { chosen ->
                PendingMorningEcho(echoId = chosen.id, regionId = route.regionId, sessionId = session.sessionId)
            }
        } else {
            bundle.playerState.pendingMorningEcho
        }

        val nextPlayer = bundle.playerState.copy(
            level = nextLevel,
            xp = nextXp,
            resources = nextRes,
            pendingMorningEcho = pendingEcho,
            activeMorningBuff = if (creditedSeconds > 0L) null else bundle.playerState.activeMorningBuff
        )

        val nextBudget = bundle.budgetState.copy(
            creditedIntervals = allocation.updatedIntervals,
            remainderSeconds = allocation.newRemainderSeconds,
            equipmentProgressSeconds = equipProgress,
            equipmentProgressRemainder = (equipNumerator % 100L).toInt(),
            rarePityByRegion = rarePityMap,
            resourceRemainders = fractions.remainders.toMap(),
            eventCooldownUntilSeconds = cooldowns,
            bossProgressSecondsByRegion = bundle.budgetState.bossProgressSecondsByRegion.toMutableMap().apply {
                put(route.regionId.id, bossProgress)
            }
        )

        val nextBeaconClears = bundle.discoveryState.dreamBeaconClears.toMutableMap()
        if (beaconTier > 0 && completedNodes >= 12 && !switchedToSafeGather) {
            nextBeaconClears[beaconTier] = (nextBeaconClears[beaconTier] ?: 0) + 1
        }

        val rawDiscovery = bundle.discoveryState.copy(
            seenEventIds = seenEvents,
            eventCooldowns = emptyMap(),
            regionInvestigation = investigation,
            discoveredEnemyIds = discoveredEnemies,
            discoveredEquipmentDefIds = discoveredEquipDefs,
            discoveredAffixIds = discoveredAffixes,
            totalJourneysCompleted = bundle.discoveryState.totalJourneysCompleted + 1,
            totalCreditedSeconds = bundle.discoveryState.totalCreditedSeconds + creditedSeconds,
            onboardingCompleted = completedOnboardingNow,
            regionCreditedSeconds = bundle.discoveryState.regionCreditedSeconds + (route.regionId.id to totalRegionSecondsAfter),
            unlockedSouvenirIds = unlockedSouvenirs,
            companionAffinity = nextAffinity,
            dreamBeaconClears = nextBeaconClears,
            chronicleProgress = nextChronicleProgress,
            enemyKillCounts = killCounts,
            completedCommissionIds = nextCompletedCommissions
        )
        val reconciledDiscovery = ProgressionEngine.reconcileUnlocks(nextPlayer, rawDiscovery, updatedInventory)

        val updatedBundle = bundle.copy(
            playerState = nextPlayer,
            budgetState = nextBudget,
            discoveryState = reconciledDiscovery,
            inventory = updatedInventory
        )

        // Pick top <= 3 key events, sorted chronologically among top picks
        val topKeyEvents = candidateKeyEvents
            .sortedWith(compareByDescending<Pair<Int, KeyEventSummary>> { it.first }.thenBy { it.second.nodeIndex })
            .distinctBy { it.second.title }
            .take(3)
            .map { it.second }
            .sortedBy { it.nodeIndex }

        val mergedActivities = buildList {
            if (gatherCount > 0) add(MergedActivitySummary("采集了 $gatherCount 次${route.regionId.materialName}", gatherCount))
            if (victoryCount > 0) add(MergedActivitySummary("稳妥通过 $victoryCount 次沿途遭遇", victoryCount))
            if (clueCount > 0) add(MergedActivitySummary("记录了 $clueCount 处路途线索", clueCount))
            if (restCount > 0) add(MergedActivitySummary("沿途安心休整 $restCount 次", restCount))
        }

        val newlyUnlockedRoutes = reconciledDiscovery.unlockedRouteIds - bundle.discoveryState.unlockedRouteIds
        val nextStepSuggestion = buildNextStepSuggestion(
            updatedBundle = updatedBundle,
            newEquipment = newEquipmentList,
            newlyUnlockedRoutes = newlyUnlockedRoutes
        )

        val pendingEchoTitle = if (phenom != null && completedNodes >= 6) {
            pendingEcho?.let { ContentCatalog.morningEchoById(it.echoId)?.title }
        } else {
            null
        }

        val settlement = SettlementResult(
            sessionId = session.sessionId,
            routeId = route.id,
            strategy = session.strategy,
            startAtUtcMs = session.startAtUtcMs,
            endAtUtcMs = resolvedEndAtUtcMs,
            rawDurationSeconds = resolvedDurationSeconds,
            windowCappedSeconds = allocation.windowCappedSeconds,
            effectiveCreditedSeconds = creditedSeconds,
            completedNodes = completedNodes,
            clockAnomalyNote = clockAnomalyNote,
            budgetNote = allocation.budgetNote,
            baseGold = baseGold,
            baseXp = baseXp,
            blessingGold = baseBuff.targetBlessingGold,
            blessingXp = baseBuff.targetBlessingXp,
            sleepBonusPercentTenths = baseBuff.totalBasisPoints / 10,
            blessingGoldRemainder = goldCarry,
            blessingXpRemainder = xpCarry,
            materialsGained = materialsMap,
            bossMarksGained = bossMarksGained,
            newEquipment = newEquipmentList,
            duplicateConvertedGold = duplicateConvertedGold,
            keyEvents = topKeyEvents,
            mergedActivities = mergedActivities,
            nextStepSuggestion = nextStepSuggestion,
            dangerFailures = dangerFailures,
            switchedToSafeGather = switchedToSafeGather,
            leveledUpFrom = bundle.playerState.level,
            leveledUpTo = nextLevel,
            rulesVersion = session.rulesVersion,
            contentVersion = session.contentVersion,
            nightPhenomenonName = phenom?.name,
            activeCompanionName = session.snapshot.activeCompanionId?.let { ContentCatalog.companionById(it)?.name },
            teaBlendName = tea?.name,
            dreamBeaconTier = beaconTier,
            newSouvenirIds = newSouvenirIds,
            dispatchSummaries = dispatchSummaries,
            pendingMorningEchoTitle = pendingEchoTitle,
            chronicleUpdateText = chronicleUpdateText,
            completedCommissionTitles = completedCommissionTitles,
            eliteVictories = eliteVictories
        )

        val matKind = materialRewardKind(route.regionId)
        val extraMatEntries = RegionId.entries.mapNotNull { reg ->
            if (reg == route.regionId) return@mapNotNull null
            val amt = materialsMap[reg.materialId] ?: 0L
            if (amt > 0L) {
                RewardLedgerEntry(session.sessionId, materialRewardKind(reg), amt, SleepBuffPolicy.POLICY_VERSION)
            } else {
                null
            }
        }

        val ledgerEntries = buildList {
            add(RewardLedgerEntry(session.sessionId, RewardKind.BASE_GOLD, baseGold, SleepBuffPolicy.POLICY_VERSION))
            add(RewardLedgerEntry(session.sessionId, RewardKind.BASE_XP, baseXp, SleepBuffPolicy.POLICY_VERSION))
            add(RewardLedgerEntry(session.sessionId, matKind, materialsMap[route.regionId.materialId] ?: baseMat, SleepBuffPolicy.POLICY_VERSION))
            addAll(extraMatEntries)
            if (bossMarksGained > 0L) {
                add(RewardLedgerEntry(session.sessionId, RewardKind.BASE_BOSS_MARK, bossMarksGained, SleepBuffPolicy.POLICY_VERSION))
            }
            add(RewardLedgerEntry(session.sessionId, RewardKind.BLESSING_GOLD, baseBuff.targetBlessingGold, SleepBuffPolicy.POLICY_VERSION))
            add(RewardLedgerEntry(session.sessionId, RewardKind.BLESSING_XP, baseBuff.targetBlessingXp, SleepBuffPolicy.POLICY_VERSION))
        }

        return SimulationOutput(
            settlement = settlement,
            updatedBundle = updatedBundle,
            ledgerEntries = ledgerEntries
        )
    }

    private fun materialRewardKind(regionId: RegionId): RewardKind = when (regionId) {
        RegionId.MISTWOOD -> RewardKind.BASE_MAT_MISTWOOD
        RegionId.TIDAL_FLAT -> RewardKind.BASE_MAT_TIDAL
        RegionId.STARLIT_RIDGE -> RewardKind.BASE_MAT_STARLIT
        RegionId.CLOUD_SANCTUARY -> RewardKind.BASE_MAT_CLOUD
        RegionId.DREAM_SEA -> RewardKind.BASE_MAT_DREAM_SEA
        RegionId.ASTRAL_ARCH -> RewardKind.BASE_MAT_ASTRAL_ARCH
    }

    private fun effectiveDangerRate(route: RouteDef, strategy: ExpeditionStrategy): Double {
        val factor = when (strategy) {
            ExpeditionStrategy.STEADY -> 0.65
            ExpeditionStrategy.EXPLORE -> 1.05
            ExpeditionStrategy.GATHER -> 0.85
        }
        return (route.dangerRate * factor).coerceIn(0.05, 0.75)
    }

    private fun buildNextStepSuggestion(
        updatedBundle: GameStateBundle,
        newEquipment: List<EquipmentInstance>,
        newlyUnlockedRoutes: Set<String>
    ): String {
        val player = updatedBundle.playerState
        val firstNew = newEquipment.firstOrNull()
        if (firstNew != null) {
            val def = ContentCatalog.equipmentById(firstNew.defId)
            if (def != null) {
                return "获得了「${def.name}」，可前往角色页装备或比较词条搭配。"
            }
        }
        val firstNewRoute = newlyUnlockedRoutes.firstOrNull()?.let { ContentCatalog.routeById(it) }
        if (firstNewRoute != null) {
            return "已解锁新路线「${firstNewRoute.name}」，下次出发可以直接前往。"
        }
        val workshopNext = player.facilityLevel(FacilityType.WORKSHOP) + 1
        if (workshopNext <= 5) {
            val (gold, mat) = ContentCatalog.facilityUpgradeCost(FacilityType.WORKSHOP, workshopNext)
            if (updatedBundle.discoveryState.totalCreditedSeconds >= ContentCatalog.facilityRequiredExploreHours(workshopNext) * 3600L &&
                player.resources.gold >= gold && player.resources.materialCount(mat.first) >= mat.second) {
                return "营地工坊已凑齐升至 Lv.$workshopNext 的材料，可前往营地建设。"
            }
        }
        val mapNext = player.facilityLevel(FacilityType.MAP_TABLE) + 1
        if (mapNext <= 5) {
            val (gold, mat) = ContentCatalog.facilityUpgradeCost(FacilityType.MAP_TABLE, mapNext)
            if (updatedBundle.discoveryState.totalCreditedSeconds >= ContentCatalog.facilityRequiredExploreHours(mapNext) * 3600L &&
                player.resources.gold >= gold && player.resources.materialCount(mat.first) >= mat.second) {
                return "地图桌已可升级至 Lv.$mapNext，升级后更容易发现沿途线索。"
            }
        }
        return "整理行囊与装备后，随时可以再次出发探索。"
    }
}
