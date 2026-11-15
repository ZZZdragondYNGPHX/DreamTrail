package com.dreamtrail.core.engine

import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.model.*
import com.dreamtrail.core.rng.DeterministicRng
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

enum class CombatOutcome {
    VICTORY,
    DEFEAT,
    DRAW_RETREAT
}

data class CombatResult(
    val outcome: CombatOutcome,
    val roundsPlayed: Int,
    val remainingHp: Int,
    val maxHp: Int,
    val damageBlockedByShield: Int,
    val enemyNames: List<String>,
    val buildHighlight: String?,
    val failureReason: String?
)

/**
 * Small deterministic turn-based combat simulator (1-3 enemies, max 20 rounds).
 * No floating-point currency side effects; temporary combat state resets after each encounter.
 */
object CombatSimulator {
    const val MAX_ROUNDS = 20

    private data class UnitState(
        val isPlayer: Boolean,
        val name: String,
        var hp: Int,
        val maxHp: Int,
        val attack: Int,
        val defense: Int,
        val speed: Int,
        val skillCoeff: Double,
        var shield: Int,
        val tieKey: Long
    )

    fun simulate(
        snapshot: ExpeditionPlayerSnapshot,
        route: RouteDef,
        enemies: List<EnemyDef>,
        rng: DeterministicRng,
        nightPhenomenon: NightPhenomenon? = null,
        eliteTrait: EliteTrait? = null
    ): CombatResult {
        require(enemies.isNotEmpty() && enemies.size <= 3) { "Encounter must have 1..3 enemies" }

        val affixes = snapshot.equippedItems.flatMap { listOfNotNull(it.affix, it.secondaryAffix) }.toSet()
        val passives = snapshot.equippedPassives.toSet()
        val resonances = ProgressionEngine.computeActiveResonancesFromSnapshot(snapshot)
        val dualSynergy = ProgressionEngine.hasDualArchetypeSynergy(snapshot.equippedItems, snapshot.equippedPassives)
        val resonanceTierByArchetype = resonances.associate { it.archetype to it.tier }
        val isBossFight = enemies.any { it.isBoss }
        val companion = snapshot.activeCompanionId?.let { ContentCatalog.companionById(it) }
        val activeAffinityRank = companion?.let {
            ProgressionEngine.companionAffinityRank(snapshot.companionAffinity[it.id] ?: 0)
        } ?: 1
        val assistScale = if (activeAffinityRank >= 3) 1.25 else 1.0
        val beaconTier = snapshot.dreamBeaconTier.coerceIn(0, 4)

        var playerAtk = snapshot.effectiveStats.attack
        var playerDef = snapshot.effectiveStats.defense
        var playerSpd = snapshot.effectiveStats.speed
        var openingShield = 0

        if (AffixId.STEADY_RECOVERY in affixes) openingShield += 10
        if (AffixId.COUNTER_PULSE in affixes) openingShield += 8
        if (AffixId.THORN_BASTION in affixes) openingShield += 6
        if (AffixId.SANCTUARY_BULWARK in affixes) openingShield += 10
        if (AffixId.RAIN_WARD in affixes && (route.environmentTag == EnvironmentTag.DAMP || route.environmentTag == EnvironmentTag.CHILL)) {
            openingShield += 12
        }
        if ("passive_forest_ward" in passives) openingShield += 12
        if ("passive_ember_resolve" in passives) openingShield += 8
        if ("passive_amber_anchor" in passives) openingShield += 14
        if ("passive_cloud_aegis" in passives) {
            openingShield += 10
            if (route.environmentTag == EnvironmentTag.CLOUD) playerDef += 5
        }
        if ("keepsake_everwarm_kettle" in snapshot.unlockedKeepsakeIds) openingShield += 8
        if (AffixId.CLOUD_WALKER in affixes) playerSpd += 3
        if ("passive_tidal_stride" in passives) {
            if (route.environmentTag == EnvironmentTag.DAMP) playerDef += 4
        }
        if ("passive_star_whisper" in passives) {
            openingShield = (openingShield * 1.15).roundToInt()
            if (route.environmentTag == EnvironmentTag.CHILL || isBossFight) {
                playerAtk = (playerAtk * 1.12).roundToInt()
            }
        }
        if (AffixId.COUNTER_PULSE in affixes && (isBossFight || enemies.size > 1)) {
            playerAtk = (playerAtk * 1.12).roundToInt()
        }
        if (AffixId.BOSS_HUNTER in affixes && isBossFight) {
            playerAtk = (playerAtk * 1.20).roundToInt()
            playerDef = (playerDef * 1.15).roundToInt()
        }

        // Build Archetype Resonances (Tier 1/2/3) & Dual Synergy
        val bastionTier = resonanceTierByArchetype[BuildArchetype.BASTION] ?: 0
        val galeTier = resonanceTierByArchetype[BuildArchetype.GALE] ?: 0
        val dreamweaveTier = resonanceTierByArchetype[BuildArchetype.DREAMWEAVE] ?: 0
        val scholarTier = resonanceTierByArchetype[BuildArchetype.SCHOLAR] ?: 0

        when {
            bastionTier >= 3 -> {
                openingShield += 48
                playerDef = (playerDef * 1.30).roundToInt()
            }
            bastionTier == 2 -> {
                openingShield += 30
                playerDef = (playerDef * 1.20).roundToInt()
            }
            bastionTier == 1 -> {
                openingShield += 14
                playerDef = (playerDef * 1.10).roundToInt()
            }
        }
        when {
            galeTier >= 3 -> playerSpd += 10
            galeTier == 2 -> playerSpd += 6
            galeTier == 1 -> playerSpd += 3
        }
        when {
            dreamweaveTier >= 3 -> openingShield += 34
            dreamweaveTier == 2 -> openingShield += 22
            dreamweaveTier == 1 -> openingShield += 10
        }
        when {
            scholarTier >= 3 -> playerAtk = (playerAtk * 1.28).roundToInt()
            scholarTier == 2 -> playerAtk = (playerAtk * 1.18).roundToInt()
            scholarTier == 1 -> playerAtk = (playerAtk * 1.10).roundToInt()
        }
        if (dualSynergy) {
            openingShield += 12
            playerAtk = (playerAtk * 1.06).roundToInt()
            playerDef = (playerDef * 1.06).roundToInt()
        }

        // Companion Combat Skill Opening Modifiers
        when (companion?.id) {
            "comp_airen" -> playerSpd += (3 * assistScale).roundToInt()
            "comp_noah" -> openingShield += (12 * assistScale).roundToInt()
            "comp_xiu" -> openingShield += (16 * assistScale).roundToInt()
            "comp_cen" -> openingShield += (12 * assistScale).roundToInt()
        }
        // Rank 5 non-active companion passive opening aura (50% effect)
        for ((compId, aff) in snapshot.companionAffinity) {
            if (compId != companion?.id && ProgressionEngine.companionAffinityRank(aff) >= 5) {
                when (compId) {
                    "comp_airen" -> playerSpd += 2
                    "comp_noah" -> openingShield += 6
                    "comp_lin" -> openingShield += 5
                    "comp_xiu" -> openingShield += 8
                    "comp_cen" -> openingShield += 6
                }
            }
        }

        // Night Phenomenon combat blessing
        if (nightPhenomenon != null &&
            (nightPhenomenon.favoredRegionId == route.regionId || nightPhenomenon.favoredEnvironment == route.environmentTag)
        ) {
            if (nightPhenomenon.combatAtkPercent > 0) {
                playerAtk = (playerAtk * (1.0 + nightPhenomenon.combatAtkPercent / 100.0)).roundToInt()
            }
            if (nightPhenomenon.combatDefPercent > 0) {
                playerDef = (playerDef * (1.0 + nightPhenomenon.combatDefPercent / 100.0)).roundToInt()
            }
        }

        val xiuCoeff = when {
            companion?.id != "comp_xiu" -> 1.0
            activeAffinityRank >= 3 -> 1.13
            else -> 1.10
        }
        val astralCombatDamagePercent = snapshot.unlockedAstralNodeIds.sumOf {
            ContentCatalog.astralNodeById(it)?.combatDamagePercent ?: 0
        }

        val playerUnit = UnitState(
            isPlayer = true,
            name = "旅人",
            hp = snapshot.effectiveStats.maxHp,
            maxHp = snapshot.effectiveStats.maxHp,
            attack = playerAtk,
            defense = playerDef,
            speed = playerSpd,
            skillCoeff = xiuCoeff,
            shield = openingShield,
            tieKey = rng.nextLong()
        )

        val hpScale = 1.0 + beaconTier * 0.22
        val atkScale = 1.0 + beaconTier * 0.14
        val defScale = 1.0 + beaconTier * 0.12
        val shieldScale = 1.0 + beaconTier * 0.25

        val enemyUnits = enemies.mapIndexed { idx, def ->
            val isEliteUnit = eliteTrait != null && !def.isBoss && idx == 0
            val eHpScale = if (isEliteUnit) hpScale * 1.18 else hpScale
            val eAtkScale = if (isEliteUnit) atkScale * 1.08 else atkScale
            val eSpd = def.stats.speed + beaconTier + (if (isEliteUnit && eliteTrait == EliteTrait.GALE_PROWL) 4 else 0)
            val eShield = (def.openingShield * shieldScale).roundToInt() +
                (if (isEliteUnit && eliteTrait == EliteTrait.IRON_SHELL) 10 else 0)
            val prefix = if (isEliteUnit) "[精英] " else ""
            val suffix = if (enemies.size > 1 && !def.isBoss) "${idx + 1}" else ""
            UnitState(
                isPlayer = false,
                name = "$prefix${def.name}$suffix",
                hp = (def.stats.maxHp * eHpScale).roundToInt(),
                maxHp = (def.stats.maxHp * eHpScale).roundToInt(),
                attack = (def.stats.attack * eAtkScale).roundToInt(),
                defense = (def.stats.defense * defScale).roundToInt(),
                speed = eSpd,
                skillCoeff = def.skillCoeff,
                shield = eShield,
                tieKey = rng.nextLong()
            )
        }
        val enemyBossTraits = enemies.map { it.bossTrait }
        val bossTraitTriggerCounts = IntArray(enemies.size)
        val starJudgmentActivated = BooleanArray(enemies.size)

        var shieldAbsorbed = 0
        var shieldStrikeReady = openingShield > 0 && AffixId.SHIELD_STRIKE in affixes
        var shieldStrikeTriggered = false
        val rainWardTriggered = AffixId.RAIN_WARD in affixes &&
            (route.environmentTag == EnvironmentTag.DAMP || route.environmentTag == EnvironmentTag.CHILL)
        val cloudWalkerTriggered = AffixId.CLOUD_WALKER in affixes &&
            (route.environmentTag == EnvironmentTag.CLOUD || route.environmentTag == EnvironmentTag.FOREST)
        var firstStrikeTriggered = false
        var comboTriggered = false
        var thornReflected = false
        var resonanceTriggered = false
        var rounds = 0

        val defIgnoreRatio = when {
            scholarTier >= 3 && AffixId.STARLIGHT_INSIGHT in affixes -> 0.55
            scholarTier >= 3 -> 0.40
            scholarTier == 2 && AffixId.STARLIGHT_INSIGHT in affixes -> 0.43
            scholarTier == 2 -> 0.28
            AffixId.STARLIGHT_INSIGHT in affixes && scholarTier == 1 -> 0.27
            AffixId.STARLIGHT_INSIGHT in affixes -> 0.15
            scholarTier == 1 -> 0.12
            else -> 0.0
        }

        while (playerUnit.hp > 0 && enemyUnits.any { it.hp > 0 } && rounds < MAX_ROUNDS) {
            rounds++
            val playerSpeedThisRound = playerUnit.speed +
                (if (rounds == 1 && AffixId.SWIFT_FIRST_STRIKE in affixes) 4 else 0) +
                (if (rounds >= 3 && AffixId.CHRONO_STRIKE in affixes) 2 else 0)
            val turnUnits = (listOf(playerUnit) + enemyUnits.filter { it.hp > 0 })
                .sortedWith(
                    compareByDescending<UnitState> {
                        if (it.isPlayer) playerSpeedThisRound else it.speed
                    }.thenBy { it.tieKey }
                )

            for (actor in turnUnits) {
                if (actor.hp <= 0) continue
                if (actor.isPlayer) {
                    val targetIdx = enemyUnits.indexOfFirst { it.hp > 0 }
                    if (targetIdx < 0) break
                    val target = enemyUnits[targetIdx]
                    val targetDef = enemies[targetIdx]
                    var coeff = actor.skillCoeff
                    if (galeTier >= 1) coeff *= 1.06
                    if ("passive_amber_anchor" in passives && route.regionId == RegionId.ASTRAL_ARCH) {
                        coeff *= 1.10
                    }
                    if (AffixId.ASTRAL_HARMONY in affixes && resonances.isNotEmpty()) {
                        coeff *= 1.0 + 0.04 * resonances.size
                    }
                    if (astralCombatDamagePercent > 0) {
                        coeff *= 1.0 + astralCombatDamagePercent / 100.0
                    }
                    val targetKills = snapshot.enemyKillCounts[targetDef.id] ?: 0
                    if (targetKills >= 5) {
                        coeff *= if ("passive_bestiary_scholar" in passives) 1.16 else 1.08
                    }
                    if (rounds == 1 && AffixId.SWIFT_FIRST_STRIKE in affixes) {
                        coeff *= 1.15
                        firstStrikeTriggered = true
                    }
                    if (shieldStrikeReady) {
                        coeff *= 1.25
                        shieldStrikeReady = false
                        shieldStrikeTriggered = true
                    }
                    var totalDefIgnore = defIgnoreRatio
                    if (AffixId.CHRONO_STRIKE in affixes && (rounds == 1 || playerSpeedThisRound > target.speed)) {
                        totalDefIgnore = (totalDefIgnore + 0.20).coerceAtMost(0.75)
                    }
                    val effTargetDef = (target.defense * (1.0 - totalDefIgnore)).roundToInt()
                    var dmg = computeDamage(actor.attack, coeff, effTargetDef)
                    if (eliteTrait == EliteTrait.IRON_SHELL && targetIdx == 0 && !targetDef.isBoss) {
                        dmg = max(1, (dmg * 0.90).roundToInt())
                    }
                    applyDamage(target, dmg)

                    // Follow-up combo strike (Gale resonance / Gale Combo affix)
                    val resonanceCombo = galeTier >= 2 && (playerSpeedThisRound > target.speed || rounds % 2 == 0)
                    val affixCombo = AffixId.GALE_COMBO in affixes && playerSpeedThisRound > target.speed && rounds % 2 == 0
                    if (target.hp > 0 && (resonanceCombo || affixCombo)) {
                        val bonusCoeff = when {
                            resonanceCombo && galeTier >= 3 -> 0.55
                            resonanceCombo -> 0.35
                            else -> 0.0
                        } + (if (affixCombo) 0.45 else 0.0) +
                            (if ("passive_thorns_oath" in passives) 0.10 else 0.0)
                        val followDmg = computeDamage(actor.attack, bonusCoeff, effTargetDef)
                        applyDamage(target, followDmg)
                        comboTriggered = true
                    }
                    // Chrono Step 4th-round pierce follow-up
                    if (target.hp > 0 && "passive_chrono_step" in passives && rounds % 4 == 0) {
                        applyDamage(target, computeDamage(actor.attack, 0.30, effTargetDef))
                    }

                    // Companion active combat assist
                    if (target.hp > 0) {
                        when (companion?.id) {
                            "comp_airen" -> if (rounds >= 2 && rounds % 2 == 0) {
                                applyDamage(target, computeDamage(actor.attack, 0.28 * assistScale, target.defense))
                            }
                            "comp_noah" -> if (rounds % 3 == 0) {
                                val pierceDef = (target.defense * 0.70).roundToInt()
                                applyDamage(target, computeDamage(actor.attack, 0.42 * assistScale, pierceDef))
                            }
                            "comp_cen" -> if (rounds % 3 == 0) {
                                applyDamage(target, computeDamage(actor.attack, 0.30 * assistScale, effTargetDef))
                            }
                        }
                    }
                } else {
                    if (playerUnit.hp <= 0) break
                    val enemyIdx = enemyUnits.indexOf(actor)
                    val trait = enemyBossTraits.getOrNull(enemyIdx)
                    val isEliteUnit = eliteTrait != null && !enemies[enemyIdx].isBoss && enemyIdx == 0

                    var effectiveEnemyAtk = actor.attack
                    if (trait == BossTrait.STAR_JUDGMENT) {
                        if (!starJudgmentActivated[enemyIdx] && actor.hp <= actor.maxHp / 2) {
                            starJudgmentActivated[enemyIdx] = true
                            bossTraitTriggerCounts[enemyIdx]++
                        }
                        if (starJudgmentActivated[enemyIdx]) {
                            effectiveEnemyAtk = (effectiveEnemyAtk * 1.18).roundToInt()
                        }
                    }

                    var rawDmg = computeDamage(effectiveEnemyAtk, actor.skillCoeff, playerUnit.defense)
                    if (isEliteUnit && eliteTrait == EliteTrait.GALE_PROWL && rounds == 1) {
                        rawDmg = (rawDmg * 1.12).roundToInt()
                    }
                    if (rainWardTriggered) {
                        rawDmg = max(1, floor(rawDmg * 0.85).toInt())
                    }
                    if (cloudWalkerTriggered) {
                        rawDmg = max(1, floor(rawDmg * 0.88).toInt())
                    }
                    val blocked = minOf(playerUnit.shield, rawDmg)
                    shieldAbsorbed += blocked
                    applyDamage(playerUnit, rawDmg)

                    // Thorn Bastion / Bastion tier 2/3 reflect
                    if (playerUnit.hp > 0 && blocked > 0 && (AffixId.THORN_BASTION in affixes || bastionTier >= 2)) {
                        val ratio = (if (AffixId.THORN_BASTION in affixes) 0.35 else 0.0) +
                            when {
                                bastionTier >= 3 -> 0.38
                                bastionTier == 2 -> 0.28
                                else -> 0.0
                            }
                        val reflect = max(2, (playerUnit.defense * ratio).roundToInt())
                        applyDamage(actor, reflect)
                        thornReflected = true
                    }

                    // Elite regen spore
                    if (actor.hp > 0 && isEliteUnit && eliteTrait == EliteTrait.REGEN_SPORE && rounds % 3 == 0) {
                        actor.hp = minOf(actor.maxHp, actor.hp + max(6, (actor.maxHp * 0.06).roundToInt()))
                    }

                    // Boss periodic traits (if enemy survived player turn/reflect)
                    if (actor.hp > 0 && playerUnit.hp > 0 && trait != null) {
                        when (trait) {
                            BossTrait.MIST_VEIL -> if (rounds % 4 == 0) {
                                actor.shield += max(10, (actor.maxHp * 0.12).roundToInt())
                                bossTraitTriggerCounts[enemyIdx]++
                            }
                            BossTrait.TIDE_SURGE -> if (rounds % 3 == 0) {
                                val surgeDmg = computeDamage(effectiveEnemyAtk, 0.35, playerUnit.defense)
                                shieldAbsorbed += minOf(playerUnit.shield, surgeDmg)
                                applyDamage(playerUnit, surgeDmg)
                                bossTraitTriggerCounts[enemyIdx]++
                            }
                            BossTrait.SKY_GALE -> if (rounds % 3 == 0) {
                                val breezeDmg = computeDamage(effectiveEnemyAtk, 0.38, playerUnit.defense)
                                shieldAbsorbed += minOf(playerUnit.shield, breezeDmg)
                                applyDamage(playerUnit, breezeDmg)
                                actor.hp = minOf(actor.maxHp, actor.hp + max(8, (actor.maxHp * 0.04).roundToInt()))
                                bossTraitTriggerCounts[enemyIdx]++
                            }
                            BossTrait.DREAM_TIDE_BARRIER -> if (rounds % 4 == 0) {
                                actor.shield += max(16, (actor.maxHp * 0.10).roundToInt())
                                actor.hp = minOf(actor.maxHp, actor.hp + max(10, (actor.maxHp * 0.05).roundToInt()))
                                bossTraitTriggerCounts[enemyIdx]++
                            }
                            BossTrait.CHRONO_RESONANCE -> if (rounds % 3 == 0) {
                                actor.shield += max(18, (actor.maxHp * 0.11).roundToInt())
                                val chronoDmg = computeDamage(effectiveEnemyAtk, 0.36, playerUnit.defense)
                                shieldAbsorbed += minOf(playerUnit.shield, chronoDmg)
                                applyDamage(playerUnit, chronoDmg)
                                bossTraitTriggerCounts[enemyIdx]++
                            }
                            BossTrait.STAR_JUDGMENT -> Unit
                        }
                    }
                }
            }

            // End-of-round sustain effects
            if (playerUnit.hp > 0 && enemyUnits.any { it.hp > 0 }) {
                val shieldBeforeSustain = playerUnit.shield
                if (AffixId.DREAM_MENDING in affixes && rounds % 3 == 0) {
                    playerUnit.hp = minOf(playerUnit.maxHp, playerUnit.hp + max(1, (playerUnit.maxHp * 0.10).roundToInt()))
                    playerUnit.shield += 8
                    resonanceTriggered = true
                }
                if (dreamweaveTier >= 2 && rounds % 2 == 0) {
                    val healPct = when {
                        dreamweaveTier >= 3 -> 0.11
                        dreamweaveTier == 2 -> 0.08
                        else -> 0.05
                    }
                    val healAmt = max(4, (playerUnit.maxHp * healPct).roundToInt())
                    playerUnit.hp = minOf(playerUnit.maxHp, playerUnit.hp + healAmt)
                    playerUnit.shield += when {
                        dreamweaveTier >= 3 -> 12
                        dreamweaveTier == 2 -> 8
                        else -> 4
                    }
                    resonanceTriggered = true
                } else if (dreamweaveTier == 1 && rounds % 3 == 0) {
                    val healAmt = max(4, (playerUnit.maxHp * 0.05).roundToInt())
                    playerUnit.hp = minOf(playerUnit.maxHp, playerUnit.hp + healAmt)
                    resonanceTriggered = true
                }
                if (AffixId.LUCID_PULSE in affixes && rounds % 2 == 0) {
                    playerUnit.hp = minOf(playerUnit.maxHp, playerUnit.hp + max(4, (playerUnit.maxHp * 0.06).roundToInt()))
                    resonanceTriggered = true
                }
                if (AffixId.SANCTUARY_BULWARK in affixes && playerUnit.shield < 14) {
                    playerUnit.shield = minOf(14, playerUnit.shield + 6)
                }
                if (companion?.id == "comp_lin" && rounds % 2 == 0) {
                    val healAmt = max(6, (playerUnit.maxHp * 0.06 * assistScale).roundToInt())
                    playerUnit.hp = minOf(playerUnit.maxHp, playerUnit.hp + healAmt)
                    playerUnit.shield += (6 * assistScale).roundToInt()
                }
                if (companion?.id == "comp_cen" && rounds % 3 == 0) {
                    val healAmt = max(5, (playerUnit.maxHp * 0.05 * assistScale).roundToInt())
                    playerUnit.hp = minOf(playerUnit.maxHp, playerUnit.hp + healAmt)
                    playerUnit.shield += (8 * assistScale).roundToInt()
                }
                if (playerUnit.shield > shieldBeforeSustain && AffixId.SHIELD_STRIKE in affixes) {
                    shieldStrikeReady = true
                }
            }
        }

        val outcome = when {
            playerUnit.hp <= 0 -> CombatOutcome.DEFEAT
            enemyUnits.none { it.hp > 0 } -> CombatOutcome.VICTORY
            else -> CombatOutcome.DRAW_RETREAT
        }

        val topResonance = resonances.maxByOrNull { it.tier * 10 + it.points }
        val triggeredBossTrait = enemies.firstOrNull { it.isBoss }?.bossTrait
        val buildHighlight = when {
            outcome == CombatOutcome.VICTORY && topResonance != null && topResonance.tier >= 3 ->
                "激活「${topResonance.archetype.displayName} 6点极鸣」，强力压制${enemies.first().name}"
            outcome == CombatOutcome.VICTORY && topResonance != null && topResonance.tier >= 2 ->
                "激活「${topResonance.archetype.displayName} 4件共鸣」，压制${enemies.first().name}"
            outcome == CombatOutcome.VICTORY && eliteTrait != null ->
                "击败精英特性「${eliteTrait.title}」的${enemies.first().name}"
            outcome == CombatOutcome.VICTORY && isBossFight && triggeredBossTrait != null ->
                "顶住首领特性「${triggeredBossTrait.title}」并取得胜利"
            outcome == CombatOutcome.VICTORY && thornReflected ->
                "「荆棘壁垒」反震伤害并护盾吸收 $shieldAbsorbed 点"
            outcome == CombatOutcome.VICTORY && comboTriggered ->
                "高速连击触发追伤，迅速结束战斗"
            outcome == CombatOutcome.VICTORY && companion != null && shieldAbsorbed >= 0 ->
                "${companion.name}施展「${companion.combatSkillName}」协助制胜"
            outcome == CombatOutcome.VICTORY && shieldStrikeTriggered && shieldAbsorbed > 0 ->
                "护盾抵挡了 $shieldAbsorbed 点伤害并触发「护盾猛击」"
            outcome == CombatOutcome.VICTORY && rainWardTriggered && shieldAbsorbed > 0 ->
                "「御雨壁垒」适应了${route.environmentTag.displayName}并吸收 $shieldAbsorbed 点伤害"
            outcome == CombatOutcome.VICTORY && cloudWalkerTriggered && shieldAbsorbed > 0 ->
                "「云行轻步」顺风化解了敌人攻势，护盾吸收 $shieldAbsorbed 点伤害"
            outcome == CombatOutcome.VICTORY && firstStrikeTriggered ->
                "「先发制人」抢占首轮节奏，顺利结束遭遇"
            outcome == CombatOutcome.VICTORY && AffixId.COUNTER_PULSE in affixes && (isBossFight || enemies.size > 1) ->
                "「震荡反制」在压迫下稳住了反击节奏"
            outcome == CombatOutcome.VICTORY && isBossFight && AffixId.BOSS_HUNTER in affixes ->
                "「首领克星」有效击穿了${enemies.first().name}的护卫"
            resonanceTriggered && shieldAbsorbed > 0 ->
                "织梦回气稳住血线并抵挡 $shieldAbsorbed 点伤害"
            shieldAbsorbed >= 15 ->
                "开局护盾成功抵挡了 $shieldAbsorbed 点伤害"
            else -> null
        }

        val failureReason = when (outcome) {
            CombatOutcome.VICTORY -> null
            CombatOutcome.DEFEAT -> "遭遇「${enemies.joinToString("、") { it.name }}」火力较猛，可先强化防具或采用稳健策略"
            CombatOutcome.DRAW_RETREAT -> "与「${enemies.joinToString("、") { it.name }}」僵持满 $MAX_ROUNDS 回合后主动撤离，已保留前期收获"
        }

        return CombatResult(
            outcome = outcome,
            roundsPlayed = rounds,
            remainingHp = max(0, playerUnit.hp),
            maxHp = playerUnit.maxHp,
            damageBlockedByShield = shieldAbsorbed,
            enemyNames = enemies.map { it.name },
            buildHighlight = buildHighlight,
            failureReason = failureReason
        )
    }

    fun computeDamage(attack: Int, skillCoeff: Double, targetDefense: Int): Int {
        val raw = attack * skillCoeff * 100.0 / (100.0 + max(0, targetDefense))
        return max(1, floor(raw).toInt())
    }

    private fun applyDamage(target: UnitState, damage: Int) {
        var remaining = damage
        if (target.shield > 0) {
            val absorbed = minOf(target.shield, remaining)
            target.shield -= absorbed
            remaining -= absorbed
        }
        if (remaining > 0) {
            target.hp = max(0, target.hp - remaining)
        }
    }
}
