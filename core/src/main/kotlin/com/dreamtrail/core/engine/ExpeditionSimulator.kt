package com.dreamtrail.core.engine

import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.model.*
import com.dreamtrail.core.rng.DeterministicRng
import com.dreamtrail.core.sleep.FocusPoisePolicy
import com.dreamtrail.core.sleep.SleepBuffPolicy
import java.time.Instant
import java.time.ZoneId
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
        val alloc = RewardBudgetCalculator.allocate(
            budgetState = budgetState,
            sessionStartUtcSec = session.startAtUtcMs / 1000L,
            rawDurationSeconds = resolvedDurationSeconds,
            sessionMode = session.sessionMode
        )
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
        val isFocus = session.sessionMode.isFocus
        val allocation = RewardBudgetCalculator.allocate(
            budgetState = bundle.budgetState,
            sessionStartUtcSec = session.startAtUtcMs / 1000L,
            rawDurationSeconds = resolvedDurationSeconds,
            sessionMode = session.sessionMode
        )

        val creditedSeconds = allocation.creditedSeconds
        val completedNodes = allocation.completedNodes.coerceAtMost(96) // upper safety bound
        val priorRemainder = if (isFocus) bundle.budgetState.focusRemainderSeconds else bundle.budgetState.remainderSeconds

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
        val phenom = if (isFocus) null else session.nightPhenomenon
        val dayPhenom = if (isFocus) session.dayPhenomenon else null
        val dayPhenomMatches = dayPhenom != null &&
            (dayPhenom.favoredRegionId == route.regionId || dayPhenom.favoredEnvironment == route.environmentTag)
        val beaconTier = session.snapshot.dreamBeaconTier.coerceIn(0, 4)
        val conservatoryLv = session.snapshot.facilityLevels[FacilityType.CONSERVATORY] ?: 0
        val scriptoriumLv = session.snapshot.facilityLevels[FacilityType.SCRIPTORIUM] ?: 0
        val nightPrepTier = if (session.sessionMode == SessionMode.SLEEP) session.snapshot.nightPrepTier.coerceIn(0, 3) else 0
        val combatSnapshot = session.snapshot.copy(nightPrepTier = nightPrepTier)
        val vigour = if (isFocus) session.snapshot.focusVigourBuff else null
        val vigourSeconds = minOf(creditedSeconds, vigour?.remainingFocusSeconds?.coerceAtLeast(0L) ?: 0L)
        val activeSpirit = session.snapshot.activeSpiritId?.let { ContentCatalog.spiritById(it) }
        val focusTag = if (isFocus) {
            session.focusTagId?.let { tid ->
                ContentCatalog.defaultFocusTagById(tid) ?: bundle.playerState.customFocusTags.firstOrNull { it.id == tid }
            }
        } else null

        var goldBonusPercent = 100L
        var xpPercent = if ("passive_ember_resolve" in passives) 108L else 100L
        if ("passive_dream_weaver" in passives) xpPercent += 6L
        if ("passive_soluna_breath" in passives) xpPercent += 7L
        if ("passive_astral_brush" in passives && route.regionId == RegionId.DREAM_SEA) goldBonusPercent += 8L
        if (AffixId.STARLIGHT_INSIGHT in affixes) {
            goldBonusPercent += 6L
            xpPercent += 6L
        }
        if (AffixId.INK_SCHOLAR in affixes) {
            goldBonusPercent += 8L
            xpPercent += 8L
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
        if ("keepsake_athenaeum_quill" in keepsakes) {
            xpPercent += 8L
        }
        if ("keepsake_soluna_sundial" in keepsakes) {
            goldBonusPercent += 6L
            xpPercent += 6L
            matPercent += 6L
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
        if (dayPhenom != null && dayPhenomMatches) {
            goldBonusPercent += dayPhenom.goldBonusPercent
            xpPercent += dayPhenom.xpBonusPercent
            matPercent += dayPhenom.materialBonusPercent
        }
        session.snapshot.activeMorningBuff?.let { buff ->
            goldBonusPercent += buff.goldBonusPercent
            xpPercent += buff.xpBonusPercent
            matPercent += buff.materialBonusPercent
        }
        session.snapshot.activeTeaEchoBuff?.let { buff ->
            goldBonusPercent += buff.goldBonusPercent
            xpPercent += buff.xpBonusPercent
            matPercent += buff.materialBonusPercent
        }
        if (nightPrepTier > 0) {
            val prepPct = nightPrepTier * 6L
            goldBonusPercent += prepPct
            xpPercent += prepPct
            matPercent += prepPct
        }
        if (activeSpirit != null) {
            goldBonusPercent += activeSpirit.goldBonusPercent
            if (isFocus) xpPercent += activeSpirit.focusXpBonusPercent
        }
        if (focusTag != null) {
            when (focusTag.id) {
                "tag_work" -> goldBonusPercent += 12L
                "tag_read" -> xpPercent += 12L
                "tag_mindful" -> matPercent += 10L
                "tag_study", "tag_code", "tag_create" -> Unit
                else -> when (focusTag.archetype) {
                    BuildArchetype.BASTION -> goldBonusPercent += 8L
                    BuildArchetype.SCHOLAR -> xpPercent += 8L
                    BuildArchetype.DREAMWEAVE -> matPercent += 8L
                    BuildArchetype.GALE -> Unit
                }
            }
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
        var baseGold = fractions.accrue("gold", route.baseGoldPerHour * strategyGoldPercent *
            (creditedSeconds * goldBonusPercent + vigourSeconds * (vigour?.goldBonusPercent ?: 0)))
        var baseXp = fractions.accrue("xp", route.baseXpPerHour * strategyXpPercent *
            (creditedSeconds * xpPercent + vigourSeconds * (vigour?.xpBonusPercent ?: 0)))
        var baseMat = fractions.accrue(route.regionId.materialId, creditedSeconds * route.baseMaterialPerHour * strategyMatPercent * matPercent)
        var inkGained = 0L
        if (isFocus && creditedSeconds > 0L) {
            var inkBonusPercent = 100L + scriptoriumLv * 12L
            if ("passive_sunlit_focus" in passives) inkBonusPercent += 10L
            if ("keepsake_athenaeum_quill" in keepsakes) inkBonusPercent += 8L
            if (AffixId.INK_SCHOLAR in affixes) inkBonusPercent += 10L
            if (tea != null) inkBonusPercent += tea.inkBonusPercent
            if (dayPhenom != null && dayPhenomMatches) inkBonusPercent += dayPhenom.inkBonusPercent
            if (activeSpirit != null) inkBonusPercent += activeSpirit.inkBonusPercent
            if (focusTag?.id == "tag_create") inkBonusPercent += 15L
            inkGained = fractions.accrue("ink", 12L * 100L *
                (creditedSeconds * inkBonusPercent + vigourSeconds * (vigour?.inkBonusPercent ?: 0)))
        }

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
        val failureLimit = 3 + (tea?.extraFailureTolerance ?: 0) + (if (nightPrepTier >= 3) 1 else 0)

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
            val creditedAtNode = ((nodeIndex + 1) * 300L - priorRemainder).coerceIn(0, creditedSeconds)
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
                    val combat = CombatSimulator.simulate(combatSnapshot, route, listOf(bossDef), combatRng, phenom, dayPhenomenon = dayPhenom)
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

                val combat = CombatSimulator.simulate(combatSnapshot, route, pickedEnemies, combatRng, phenom, eliteTrait, dayPhenom)
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
                    (!ev.focusOnly || isFocus) &&
                        ev.minInvestigation <= curInv &&
                        regionSeconds >= ev.minRegionExploreHours * 3600L &&
                        (cooldowns[ev.id] ?: 0L) <= regionSeconds &&
                        (ev.requiredStrategy == null || ev.requiredStrategy == session.strategy || switchedToSafeGather)
                }.ifEmpty { regionEvents.filter { (!it.focusOnly || isFocus) && it.kind == EventKind.GATHER && it.minRegionExploreHours == 0 && it.requiredStrategy == null } }

                val picked = nodeRng.pickWeighted(eligible) { ev ->
                    var w = ev.weight.coerceAtLeast(1)
                    if (isFocus && ev.focusOnly) w = (w * 1.4).toInt()
                    if (ev.id !in seenEvents) w = (w * 1.6).toInt()
                    if (ev.kind == EventKind.CLUE) {
                        if (session.strategy == ExpeditionStrategy.EXPLORE) w = (w * 1.7).toInt()
                        if (AffixId.CLUE_SEEKER in affixes) w = (w * 1.35).toInt()
                        if (AffixId.ASTRAL_HARMONY in affixes) w = (w * 1.15).toInt()
                        if (AffixId.INK_SCHOLAR in affixes) w = (w * 1.25).toInt()
                        if ("passive_keen_eye" in passives) w = (w * 1.25).toInt()
                        if ("passive_bestiary_scholar" in passives) w = (w * 1.18).toInt()
                        if ("passive_archivist_eye" in passives) w = (w * 1.20).toInt()
                        if (focusTag?.id == "tag_study") w = (w * 1.15).toInt()
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
                    if (AffixId.SOLUNA_DEW in affixes) {
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
                    if ("passive_soluna_breath" in passives) {
                        evGold += 6L
                        evXp += 8L
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
            (completedNodes * 300L - priorRemainder).coerceIn(0, creditedSeconds)
        bossProgress += (creditedSeconds - nodeCreditedSeconds).toInt()

        // Equipment rolls: 1 guaranteed roll per 2 cumulative credited hours (7200s)
        var equipProgressPct = 100L
        if (AffixId.TREASURE_SENSE in affixes) equipProgressPct += 15L
        if (AffixId.ASTRAL_HARMONY in affixes) equipProgressPct += 15L
        if ("passive_archivist_eye" in passives) equipProgressPct += 10L
        if ("keepsake_cloud_music_box" in keepsakes) equipProgressPct += 10L
        if ("passive_astral_brush" in passives) equipProgressPct += 8L
        if (focusTag?.id == "tag_code") equipProgressPct += 15L
        else if (focusTag != null && focusTag.archetype == BuildArchetype.GALE) equipProgressPct += 10L
        if (nightPrepTier > 0) {
            equipProgressPct += when (nightPrepTier) {
                1 -> 10L
                2 -> 18L
                else -> 25L
            }
        }
        if (tea != null) equipProgressPct += (tea.dropRateBonus * 100.0).roundToLong()
        if (phenom != null &&
            (phenom.favoredRegionId == route.regionId || phenom.favoredEnvironment == route.environmentTag)
        ) {
            equipProgressPct += phenom.dropBonusPercent
        }
        if (dayPhenom != null && dayPhenomMatches) {
            equipProgressPct += dayPhenom.dropBonusPercent
        }
        val equipNumerator = creditedSeconds * equipProgressPct + bundle.budgetState.equipmentProgressRemainder
        val addedEquipSeconds = (equipNumerator / 100L).toInt()
        var equipProgress = bundle.budgetState.equipmentProgressSeconds + addedEquipSeconds
        val equipRolls = (equipProgress / 7200).coerceIn(0, 32)
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

        // Companion Spirit discovery check (accumulates across short focus sessions; 2x progress speed in focus mode)
        val unlockedSpirits = bundle.discoveryState.unlockedSpiritIds.toMutableSet()
        val newSpiritIds = mutableListOf<String>()
        val spiritProgressGain = (creditedSeconds * (if (isFocus) 2L else 1L)).toInt().coerceAtLeast(0)
        var spiritProgress = bundle.budgetState.spiritProgressSeconds + spiritProgressGain
        val spiritRolls = spiritProgress / 3600
        spiritProgress %= 3600
        for (rollIdx in 0 until spiritRolls) {
            val missingSpirits = ContentCatalog.spiritsForRegion(route.regionId).filter { it.id !in unlockedSpirits }
            if (missingSpirits.isNotEmpty()) {
                val spiritRng = DeterministicRng.forNode(session.sessionSeed, 25_000 + rollIdx, streamTag = 5)
                val chance = (0.38 + (tea?.dropRateBonus ?: 0.0) + (if (isFocus) 0.12 else 0.0)).coerceIn(0.25, 0.90)
                if (spiritRng.nextDouble() < chance) {
                    val pickedSpirit = missingSpirits[spiritRng.nextInt(missingSpirits.size)]
                    unlockedSpirits += pickedSpirit.id
                    newSpiritIds += pickedSpirit.id
                    candidateKeyEvents += 93 to KeyEventSummary(
                        nodeIndex = (completedNodes - 1).coerceAtLeast(0),
                        title = "伴读结缘 · ${pickedSpirit.name}",
                        detail = "在${route.regionId.displayName}吸引到伴读小灵兽「${pickedSpirit.name}」：${pickedSpirit.description}",
                        isFirstDiscovery = true,
                        highlightReason = "伴读小灵兽图鉴解锁"
                    )
                }
            }
        }

        // Camp Companion Dispatch yields (credit per cumulative 2h across focus/sleep sessions)
        val materialsGainedMap = mutableMapOf<String, Long>()
        if (baseMat > 0L) {
            materialsGainedMap[route.regionId.materialId] = baseMat
        }
        val dispatchSummaries = mutableListOf<String>()
        val totalDispatchSec = bundle.budgetState.dispatchProgressSeconds.toLong() + creditedSeconds
        val dispatchCycles = (totalDispatchSec / 7200L).toInt().coerceAtMost(6)
        val nextDispatchProgress = (totalDispatchSec % 7200L).toInt()
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
                    "comp_wen" -> {
                        val extraGold = (26L * dispatchCycles * compScale).roundToLong()
                        val extraInk = (2L * dispatchCycles * compScale).roundToLong().coerceAtLeast(1L)
                        val matRegion = if ("route_athenaeum_cloister" in bundle.discoveryState.unlockedRouteIds) {
                            RegionId.SUNLIT_ATHENAEUM
                        } else {
                            RegionId.MISTWOOD
                        }
                        val extraMat = (1L * dispatchCycles * compScale).roundToLong().coerceAtLeast(1L)
                        baseGold += extraGold
                        inkGained += extraInk
                        materialsGainedMap[matRegion.materialId] =
                            (materialsGainedMap[matRegion.materialId] ?: 0L) + extraMat
                        dispatchSummaries += "温言编修抄本：+$extraGold 金币，+$extraInk 墨晶，+$extraMat ${matRegion.materialName}"
                    }
                    "comp_zhaolu" -> {
                        val extraXp = (34L * dispatchCycles * compScale).roundToLong()
                        val extraInk = (2L * dispatchCycles * compScale).roundToLong().coerceAtLeast(1L)
                        val matRegion = if ("route_spire_steps" in bundle.discoveryState.unlockedRouteIds) {
                            RegionId.DAWNLIGHT_SPIRE
                        } else {
                            RegionId.STARLIT_RIDGE
                        }
                        val extraMat = (1L * dispatchCycles * compScale).roundToLong().coerceAtLeast(1L)
                        baseXp += extraXp
                        inkGained += extraInk
                        materialsGainedMap[matRegion.materialId] =
                            (materialsGainedMap[matRegion.materialId] ?: 0L) + extraMat
                        dispatchSummaries += "朝露测绘晷针：+$extraXp 经验，+$extraInk 墨晶，+$extraMat ${matRegion.materialName}"
                    }
                }
            }
        }

        val nextAffinity = bundle.discoveryState.companionAffinity.toMutableMap()
        if (completedNodes >= 5) {
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
                    (!comm.focusOnly || isFocus) &&
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

        // Each completed timer target counts once; free-flow focus uses 25-minute units.
        val endSec = resolvedEndAtUtcMs / 1000L
        var recentTaskBounties = bundle.budgetState.taskBountyUtcSeconds.filter { it > endSec - 86_400L }
        var linkedTaskTitle: String? = null
        var taskCompletedNow = false
        var addedCompletedTasks = 0
        val updatedTasks = if (isFocus && session.linkedTaskId != null) {
            val targetTask = bundle.playerState.focusTasks.firstOrNull { it.id == session.linkedTaskId }
            linkedTaskTitle = targetTask?.title
            val unitSeconds = if (session.sessionMode == SessionMode.FOCUS_POMODORO) {
                session.targetDurationSeconds ?: 1500L
            } else 1500L
            val pomosEarned = (creditedSeconds / unitSeconds.coerceAtLeast(300L)).toInt()
            if (targetTask != null && !targetTask.completed && pomosEarned > 0) {
                val nextDonePomos = (targetTask.completedPomodoros + pomosEarned).coerceAtMost(targetTask.targetPomodoros)
                val finishedNow = nextDonePomos >= targetTask.targetPomodoros
                if (finishedNow) {
                    taskCompletedNow = true
                    addedCompletedTasks = 1
                    if (recentTaskBounties.size < 6) {
                        baseGold += 25L
                        baseXp += 30L
                        inkGained += 3L
                        recentTaskBounties = recentTaskBounties + endSec
                    }
                    candidateKeyEvents += 95 to KeyEventSummary(
                        nodeIndex = (completedNodes - 1).coerceAtLeast(0),
                        title = "悬赏达成 · ${targetTask.title}",
                        detail = "本轮专注已完成待办目标「${targetTask.title}」。悬赏每天最多发放 6 次。",
                        isFirstDiscovery = true,
                        highlightReason = "今日行囊待办完成"
                    )
                }
                bundle.playerState.focusTasks.map { t ->
                    if (t.id == targetTask.id) {
                        t.copy(
                            completedPomodoros = nextDonePomos,
                            completed = finishedNow
                        )
                    } else t
                }
            } else {
                bundle.playerState.focusTasks
            }
        } else {
            bundle.playerState.focusTasks
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

        // Blessing calculation: SleepBuffPolicy for sleep/nap (+3%..+8%), FocusPoisePolicy for focus (+3%..+10%)
        val goldCarry = fractions.remainders["blessing_gold"] ?: 0L
        val xpCarry = fractions.remainders["blessing_xp"] ?: 0L
        val baseBuff = if (isFocus) {
            FocusPoisePolicy.computeFocusBuff(
                baseGold = baseGold,
                baseXp = baseXp,
                creditedSeconds = creditedSeconds,
                targetDurationSeconds = session.targetDurationSeconds,
                distractionCount = session.distractionCount,
                flipFocusSeconds = session.flipFocusSeconds,
                scriptoriumLevel = scriptoriumLv,
                hasPoiseWard = AffixId.POISE_WARD in affixes,
                goldRemainder = goldCarry,
                xpRemainder = xpCarry
            )
        } else {
            SleepBuffPolicy.computeBuff(
                baseGold = baseGold,
                baseXp = baseXp,
                sleepMatch = null,
                goldRemainder = goldCarry,
                xpRemainder = xpCarry
            )
        }
        fractions.accrue("blessing_gold", baseGold * baseBuff.totalBasisPoints, 10000)
        fractions.accrue("blessing_xp", baseXp * baseBuff.totalBasisPoints, 10000)
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
            .addInspirationInk(inkGained)
        for ((matId, amt) in materialsMap) {
            if (amt > 0L) {
                nextRes = nextRes.addMaterial(matId, amt)
            }
        }

        // Morning echo (for night sleep >= 30m) and Tea echo (for daytime focus >= 25m)
        val pendingEcho = if (!isFocus && phenom != null && completedNodes >= 6) {
            val echoes = ContentCatalog.morningEchoesForRegion(route.regionId)
            val echoRng = DeterministicRng.forNode(session.sessionSeed, 30_000, streamTag = 4)
            echoes[echoRng.nextInt(echoes.size)].let { chosen ->
                PendingMorningEcho(echoId = chosen.id, regionId = route.regionId, sessionId = session.sessionId)
            }
        } else {
            bundle.playerState.pendingMorningEcho
        }

        val pendingTeaEcho = if (isFocus && dayPhenom != null && completedNodes >= 5) {
            val teaEchoes = ContentCatalog.teaEchoesForRegion(route.regionId)
            val echoRng = DeterministicRng.forNode(session.sessionSeed, 31_000, streamTag = 4)
            teaEchoes[echoRng.nextInt(teaEchoes.size)].let { chosen ->
                PendingTeaEcho(echoId = chosen.id, regionId = route.regionId, sessionId = session.sessionId)
            }
        } else {
            bundle.playerState.pendingTeaEcho
        }

        // Soluna cycle buffs: daytime focus charges nightPrep; night/nap grants FocusVigourBuff
        var nightPrepGained = 0
        val prepPool = bundle.budgetState.nightPrepRemainderSeconds + (if (isFocus) creditedSeconds else 0L)
        val nextNightPrepCharge = when {
            isFocus && creditedSeconds > 0L -> {
                nightPrepGained = minOf((prepPool / 60L).toInt(), 100 - bundle.playerState.nightPrepCharge)
                bundle.playerState.nightPrepCharge + nightPrepGained
            }
            session.sessionMode == SessionMode.SLEEP && creditedSeconds > 0L -> 0
            else -> bundle.playerState.nightPrepCharge
        }

        var focusVigourGrantedMinutes = 0
        val nextFocusVigour = when {
            isFocus -> {
                val curVig = bundle.playerState.focusVigourBuff
                if (curVig != null && creditedSeconds > 0L) {
                    val left = curVig.remainingFocusSeconds - creditedSeconds
                    if (left > 0L) curVig.copy(remainingFocusSeconds = left) else null
                } else curVig
            }
            session.sessionMode == SessionMode.SLEEP && creditedSeconds >= 4L * 3600L -> {
                val grantedSec = maxOf(bundle.playerState.focusVigourBuff?.remainingFocusSeconds ?: 0L, 4L * 3600L)
                focusVigourGrantedMinutes = (grantedSec / 60L).toInt()
                FocusVigourBuff("晨曦神采", remainingFocusSeconds = grantedSec, xpBonusPercent = 12, goldBonusPercent = 10, inkBonusPercent = 25)
            }
            session.sessionMode == SessionMode.POWER_NAP && creditedSeconds >= 15L * 60L -> {
                val grantedSec = maxOf(bundle.playerState.focusVigourBuff?.remainingFocusSeconds ?: 0L, 90L * 60L)
                focusVigourGrantedMinutes = (grantedSec / 60L).toInt()
                FocusVigourBuff("小憩清思", remainingFocusSeconds = grantedSec, xpBonusPercent = 12, goldBonusPercent = 10, inkBonusPercent = 25)
            }
            else -> bundle.playerState.focusVigourBuff
        }

        val nextActiveFocusTaskId = if (taskCompletedNow && bundle.playerState.activeFocusTaskId == session.linkedTaskId) {
            updatedTasks.firstOrNull { !it.completed }?.id
        } else {
            bundle.playerState.activeFocusTaskId
        }

        val nextPlayer = bundle.playerState.copy(
            level = nextLevel,
            xp = nextXp,
            resources = nextRes,
            pendingMorningEcho = pendingEcho,
            activeMorningBuff = if (creditedSeconds > 0L) null else bundle.playerState.activeMorningBuff,
            pendingTeaEcho = pendingTeaEcho,
            activeTeaEchoBuff = if (creditedSeconds > 0L) null else bundle.playerState.activeTeaEchoBuff,
            nightPrepCharge = nextNightPrepCharge,
            focusVigourBuff = nextFocusVigour,
            focusTasks = updatedTasks,
            activeFocusTaskId = nextActiveFocusTaskId
        )

        val allocatedBudget = RewardBudgetCalculator.applyAllocation(bundle.budgetState, allocation, session.sessionMode)
        val nextBudget = allocatedBudget.copy(
            equipmentProgressSeconds = equipProgress,
            equipmentProgressRemainder = (equipNumerator % 100L).toInt(),
            rarePityByRegion = rarePityMap,
            resourceRemainders = fractions.remainders.toMap(),
            eventCooldownUntilSeconds = cooldowns,
            bossProgressSecondsByRegion = bundle.budgetState.bossProgressSecondsByRegion.toMutableMap().apply {
                put(route.regionId.id, bossProgress)
            },
            dispatchProgressSeconds = nextDispatchProgress,
            spiritProgressSeconds = spiritProgress,
            taskBountyUtcSeconds = recentTaskBounties,
            nightPrepRemainderSeconds = when {
                session.sessionMode == SessionMode.SLEEP && creditedSeconds > 0L -> 0
                isFocus -> (prepPool % 60L).toInt()
                else -> bundle.budgetState.nightPrepRemainderSeconds
            }
        )

        val nextBeaconClears = bundle.discoveryState.dreamBeaconClears.toMutableMap()
        if (beaconTier > 0 && completedNodes >= 12 && !switchedToSafeGather) {
            nextBeaconClears[beaconTier] = (nextBeaconClears[beaconTier] ?: 0) + 1
        }

        val nextDailyFocus = if (isFocus && creditedSeconds > 0L) {
            addDailyCredit(bundle.discoveryState.dailyFocusSecondsByDate, allocation.newlyCreditedIntervals, session.zoneId)
        } else bundle.discoveryState.dailyFocusSecondsByDate
        val nextDailySleep = if (!isFocus && creditedSeconds > 0L) {
            addDailyCredit(bundle.discoveryState.dailySleepSecondsByDate, allocation.newlyCreditedIntervals, session.zoneId)
        } else bundle.discoveryState.dailySleepSecondsByDate
        val nextFocusByTag = if (isFocus && creditedSeconds > 0L) {
            val tagKey = session.focusTagId ?: "tag_study"
            bundle.discoveryState.focusSecondsByTag + (tagKey to ((bundle.discoveryState.focusSecondsByTag[tagKey] ?: 0L) + creditedSeconds))
        } else bundle.discoveryState.focusSecondsByTag

        val extraMilestones = if (isFocus && creditedSeconds >= 900L && baseBuff.totalBasisPoints >= FocusPoisePolicy.MAX_FOCUS_BASIS_POINTS) {
            bundle.discoveryState.unlockedMilestoneIds + "ms_poise_master"
        } else {
            bundle.discoveryState.unlockedMilestoneIds
        }

        val rawDiscovery = bundle.discoveryState.copy(
            seenEventIds = seenEvents,
            regionInvestigation = investigation,
            discoveredEnemyIds = discoveredEnemies,
            discoveredEquipmentDefIds = discoveredEquipDefs,
            discoveredAffixIds = discoveredAffixes,
            totalJourneysCompleted = bundle.discoveryState.totalJourneysCompleted + 1,
            totalCreditedSeconds = bundle.discoveryState.totalCreditedSeconds + creditedSeconds,
            onboardingCompleted = completedOnboardingNow,
            regionCreditedSeconds = bundle.discoveryState.regionCreditedSeconds + (route.regionId.id to totalRegionSecondsAfter),
            unlockedSouvenirIds = unlockedSouvenirs,
            unlockedMilestoneIds = extraMilestones,
            companionAffinity = nextAffinity,
            dreamBeaconClears = nextBeaconClears,
            chronicleProgress = nextChronicleProgress,
            enemyKillCounts = killCounts,
            completedCommissionIds = nextCompletedCommissions,
            totalFocusSessionsCompleted = bundle.discoveryState.totalFocusSessionsCompleted + (if (isFocus && creditedSeconds >= 300L) 1 else 0),
            totalFocusCreditedSeconds = bundle.discoveryState.totalFocusCreditedSeconds + (if (isFocus) creditedSeconds else 0L),
            unlockedSpiritIds = unlockedSpirits,
            focusSecondsByTag = nextFocusByTag,
            dailyFocusSecondsByDate = nextDailyFocus,
            dailySleepSecondsByDate = nextDailySleep,
            completedTasksCount = bundle.discoveryState.completedTasksCount + addedCompletedTasks,
            totalRestSessionsCompleted = bundle.discoveryState.totalRestSessionsCompleted + (if (!isFocus) 1 else 0)
        )
        val reconciledDiscovery = ProgressionEngine.reconcileUnlocks(nextPlayer, rawDiscovery, updatedInventory)

        val updatedBundle = bundle.copy(
            playerState = nextPlayer,
            budgetState = nextBudget,
            discoveryState = reconciledDiscovery,
            inventory = updatedInventory
        )

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
            newlyUnlockedRoutes = newlyUnlockedRoutes,
            isFocus = isFocus
        )

        val pendingEchoTitle = if (!isFocus && phenom != null && completedNodes >= 6) {
            pendingEcho?.let { ContentCatalog.morningEchoById(it.echoId)?.title }
        } else {
            null
        }
        val pendingTeaEchoTitle = if (isFocus && dayPhenom != null && completedNodes >= 5) {
            pendingTeaEcho?.let { ContentCatalog.teaEchoById(it.echoId)?.title }
        } else {
            null
        }
        val recommendedBreakMinutes = if (isFocus && creditedSeconds >= 300L) {
            ((creditedSeconds / 60L) / 5L).toInt().coerceIn(3, 25)
        } else {
            0
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
            initialBlessingGold = baseBuff.targetBlessingGold,
            initialBlessingXp = baseBuff.targetBlessingXp,
            sleepBonusBasisPoints = baseBuff.totalBasisPoints,
            nightPhenomenonName = phenom?.name,
            activeCompanionName = session.snapshot.activeCompanionId?.let { ContentCatalog.companionById(it)?.name },
            teaBlendName = tea?.name,
            dreamBeaconTier = beaconTier,
            newSouvenirIds = newSouvenirIds,
            dispatchSummaries = dispatchSummaries,
            pendingMorningEchoTitle = pendingEchoTitle,
            chronicleUpdateText = chronicleUpdateText,
            completedCommissionTitles = completedCommissionTitles,
            eliteVictories = eliteVictories,
            sessionMode = session.sessionMode,
            targetDurationSeconds = session.targetDurationSeconds,
            focusTagId = session.focusTagId,
            linkedTaskTitle = linkedTaskTitle,
            taskCompletedNow = taskCompletedNow,
            inspirationInkGained = inkGained,
            nightPrepGained = nightPrepGained,
            nightPrepTierUsed = nightPrepTier,
            focusVigourGrantedMinutes = focusVigourGrantedMinutes,
            distractionCount = session.distractionCount,
            dayPhenomenonName = dayPhenom?.name,
            newSpiritIds = newSpiritIds,
            pendingTeaEchoTitle = pendingTeaEchoTitle,
            recommendedBreakMinutes = recommendedBreakMinutes
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
            if (inkGained > 0L) {
                add(RewardLedgerEntry(session.sessionId, RewardKind.BASE_INSPIRATION_INK, inkGained, SleepBuffPolicy.POLICY_VERSION))
            }
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

    private fun addDailyCredit(
        previous: Map<String, Long>,
        intervals: List<SecondInterval>,
        zoneId: String
    ): Map<String, Long> {
        val zone = ZoneId.of(zoneId)
        val result = previous.toMutableMap()
        for (interval in intervals) {
            var cursor = interval.startSec
            while (cursor < interval.endSec) {
                val date = Instant.ofEpochSecond(cursor).atZone(zone).toLocalDate()
                val midnight = date.plusDays(1).atStartOfDay(zone).toEpochSecond()
                val end = minOf(interval.endSec, midnight)
                val key = date.toString()
                result[key] = (result[key] ?: 0L) + end - cursor
                cursor = end
            }
        }
        return pruneDailyMap(result)
    }

    private fun pruneDailyMap(map: Map<String, Long>): Map<String, Long> {
        if (map.size <= 90) return map
        val keptKeys = map.keys.sortedDescending().take(90).toSet()
        return map.filterKeys { it in keptKeys }
    }

    private fun materialRewardKind(regionId: RegionId): RewardKind = when (regionId) {
        RegionId.MISTWOOD -> RewardKind.BASE_MAT_MISTWOOD
        RegionId.TIDAL_FLAT -> RewardKind.BASE_MAT_TIDAL
        RegionId.STARLIT_RIDGE -> RewardKind.BASE_MAT_STARLIT
        RegionId.CLOUD_SANCTUARY -> RewardKind.BASE_MAT_CLOUD
        RegionId.DREAM_SEA -> RewardKind.BASE_MAT_DREAM_SEA
        RegionId.ASTRAL_ARCH -> RewardKind.BASE_MAT_ASTRAL_ARCH
        RegionId.SUNLIT_ATHENAEUM -> RewardKind.BASE_MAT_ATHENAEUM
        RegionId.DAWNLIGHT_SPIRE -> RewardKind.BASE_MAT_SPIRE
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
        newlyUnlockedRoutes: Set<String>,
        isFocus: Boolean = false
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
        val scriptoriumNext = player.facilityLevel(FacilityType.SCRIPTORIUM) + 1
        if (isFocus && scriptoriumNext <= 5) {
            val (gold, ink) = ContentCatalog.facilityScriptoriumCost(scriptoriumNext)
            val reqHours = ContentCatalog.facilityRequiredExploreHours(FacilityType.SCRIPTORIUM, scriptoriumNext)
            val timeOk = updatedBundle.discoveryState.totalCreditedSeconds >= reqHours * 3600L ||
                updatedBundle.discoveryState.totalFocusCreditedSeconds >= scriptoriumNext * 4 * 3600L
            if (timeOk && player.resources.gold >= gold && player.resources.inspirationInk >= ink) {
                return "研习书斋已凑齐升至 Lv.$scriptoriumNext 所需的澄明墨晶，可前往营地建设。"
            }
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
        return if (isFocus) {
            "稍作茶歇舒展肩颈，随后随时开启下一段专注旅途。"
        } else {
            "整理行囊与装备后，随时可以再次出发探索。"
        }
    }
}
