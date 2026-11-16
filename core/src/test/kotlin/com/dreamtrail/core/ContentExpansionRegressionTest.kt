package com.dreamtrail.core

import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.*
import com.dreamtrail.core.model.*
import com.dreamtrail.core.save.SaveBackupCodec
import com.dreamtrail.core.save.SaveBackupPayload
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContentExpansionRegressionTest {
    private fun bundle() = GameStateBundle(
        PlayerState(), RewardBudgetState(), DiscoveryState(onboardingCompleted = true), emptyList())

    private fun session(bundle: GameStateBundle, startSec: Long = 1_700_000_000L) = ExpeditionSession(
        sessionId = "expansion_$startSec", status = SessionStatus.RUNNING, routeId = "route_mistwood_outer",
        strategy = ExpeditionStrategy.STEADY, startAtUtcMs = startSec * 1000L, sessionSeed = 987654321L,
        contentVersion = ContentCatalog.CONTENT_VERSION, rulesVersion = ContentCatalog.RULES_VERSION,
        snapshot = ProgressionEngine.createDepartureSnapshot(
            bundle.playerState, bundle.inventory, bundle.discoveryState.unlockedKeepsakeIds))

    private fun settle(s: ExpeditionSession, b: GameStateBundle, seconds: Long) =
        ExpeditionSimulator.simulateSettlement(s, s.startAtUtcMs + seconds * 1000L, seconds, null, b)

    @Test fun `treasure and keepsake equipment progress survives short settlements and backup round trip`() {
        val item = EquipmentInstance("treasure", "eq_pine_staff", 1, 1, AffixId.TREASURE_SENSE)
        val initial = bundle().copy(
            playerState = PlayerState(equippedWeaponId = item.instanceId), inventory = listOf(item),
            discoveryState = DiscoveryState(onboardingCompleted = true, unlockedKeepsakeIds = setOf("keepsake_cloud_music_box")))
        val single = settle(session(initial), initial, 20L).updatedBundle
        var split = initial
        repeat(20) { i ->
            split = settle(session(split, 1_700_000_000L + i), split, 1L).updatedBundle
            val backup = SaveBackupPayload(exportedAtUtcMs = 1, contentVersion = ContentCatalog.CONTENT_VERSION, rulesVersion = ContentCatalog.RULES_VERSION,
                playerState = split.playerState, budgetState = split.budgetState,
                discoveryState = split.discoveryState, inventory = split.inventory)
            val restored = (SaveBackupCodec.decodeAndValidate(SaveBackupCodec.encode(backup)) as ActionOutcome.Success).value
            split = split.copy(budgetState = restored.budgetState)
        }
        assertEquals(25, single.budgetState.equipmentProgressSeconds)
        assertEquals(single.budgetState.equipmentProgressSeconds, split.budgetState.equipmentProgressSeconds)
        assertEquals(single.budgetState.equipmentProgressRemainder, split.budgetState.equipmentProgressRemainder)
    }

    @Test fun `dream catcher multiplies base gold after strategy percentage`() {
        for (strategy in ExpeditionStrategy.entries) {
            val b = bundle().copy(discoveryState = bundle().discoveryState.copy(unlockedKeepsakeIds = setOf("keepsake_dream_catcher")))
            val s = session(b).copy(strategy = strategy)
            val out = settle(s, b, 100L)
            val strategyPercent = when (strategy) {
                ExpeditionStrategy.STEADY -> 100L
                ExpeditionStrategy.EXPLORE -> 92L
                ExpeditionStrategy.GATHER -> 95L
            }
            val numerator = 100L * ContentCatalog.routeById(s.routeId)!!.baseGoldPerHour * strategyPercent * 105L
            assertEquals(numerator / ResourceAccrual.RATE_DENOMINATOR, out.settlement.baseGold)
            assertEquals(numerator % ResourceAccrual.RATE_DENOMINATOR, out.updatedBundle.budgetState.resourceRemainders["gold"])
        }
    }

    @Test fun `cloud region drops settle with matching material ledger and valid backup`() {
        val b = bundle().copy(playerState = PlayerState(level = 30))
        for (route in ContentCatalog.routes.filter { it.regionId == RegionId.CLOUD_SANCTUARY }) {
            val s = session(b).copy(routeId = route.id)
            val out = settle(s, b, 28_800L)
            val material = out.settlement.materialsGained[RegionId.CLOUD_SANCTUARY.materialId]
            assertNotNull(material)
            assertEquals(material, out.ledgerEntries.single { it.rewardKind == RewardKind.BASE_MAT_CLOUD }.creditedTotal)
            assertTrue(out.settlement.newEquipment.all { ContentCatalog.equipmentById(it.defId)!!.regionId == RegionId.CLOUD_SANCTUARY })
            assertTrue(out.settlement.newEquipment.isNotEmpty())
            val backup = SaveBackupPayload(exportedAtUtcMs = s.startAtUtcMs + 28_800_000L, contentVersion = ContentCatalog.CONTENT_VERSION, rulesVersion = ContentCatalog.RULES_VERSION,
                playerState = out.updatedBundle.playerState, budgetState = out.updatedBundle.budgetState,
                discoveryState = out.updatedBundle.discoveryState, inventory = out.updatedBundle.inventory,
                recentSettlements = listOf(out.settlement), ledgerEntries = out.ledgerEntries)
            assertTrue(SaveBackupCodec.decodeAndValidate(SaveBackupCodec.encode(backup)) is ActionOutcome.Success)
        }
    }

    @Test fun `max equipment upgrade unlocks artisan milestone immediately and permanently`() {
        val item = EquipmentInstance("artisan", "eq_pine_staff", 1, 4, AffixId.STEADY_RECOVERY)
        val b = bundle().copy(playerState = PlayerState(resources = ResourceBag(
            gold = 10_000L, materials = mapOf(RegionId.MISTWOOD.materialId to 1000L))), inventory = listOf(item))
        val upgraded = (ProgressionEngine.upgradeEquipment(b, item.instanceId) as ActionOutcome.Success).value
        assertTrue("ms_master_artisan" in upgraded.discoveryState.unlockedMilestoneIds)
        val salvaged = (ProgressionEngine.salvageSelected(upgraded, setOf(item.instanceId)) as ActionOutcome.Success).value.updatedState
        assertTrue("ms_master_artisan" in salvaged.discoveryState.unlockedMilestoneIds)
    }

    @Test fun `dream sea routes unlock after 248h and simulate fifth region yields cleanly`() {
        val preDiscovery = DiscoveryState(
            onboardingCompleted = true,
            totalCreditedSeconds = 248 * 3600L,
            regionInvestigation = mapOf(
                RegionId.MISTWOOD.id to 8,
                RegionId.TIDAL_FLAT.id to 8,
                RegionId.STARLIT_RIDGE.id to 9,
                RegionId.CLOUD_SANCTUARY.id to 4
            )
        )
        val reconciled = ProgressionEngine.reconcileUnlocks(PlayerState(level = 32), preDiscovery)
        assertTrue("route_dream_shoal" in reconciled.unlockedRouteIds)
        assertTrue("passive_astral_brush" in reconciled.unlockedPassiveIds)

        val highBundle = bundle().copy(
            playerState = PlayerState(level = 36),
            discoveryState = reconciled
        )
        val s = session(highBundle).copy(routeId = "route_dream_shoal")
        val out = settle(s, highBundle, 28_800L)
        val dreamMat = out.settlement.materialsGained[RegionId.DREAM_SEA.materialId] ?: 0L
        assertTrue(dreamMat > 0L)
        assertEquals(dreamMat, out.ledgerEntries.single { it.rewardKind == RewardKind.BASE_MAT_DREAM_SEA }.creditedTotal)
        assertTrue(out.settlement.newEquipment.all { ContentCatalog.equipmentById(it.defId)!!.regionId == RegionId.DREAM_SEA })
    }

    @Test fun `archetype resonances and secondary affix engraving work deterministically`() {
        val weapon = EquipmentInstance("w1", "eq_pine_staff", 1, 3, AffixId.RAIN_WARD)
        val armor = EquipmentInstance("a1", "eq_moss_cloak", 1, 3, AffixId.STEADY_RECOVERY)
        val charm = EquipmentInstance("c1", "eq_dew_charm", 1, 3, AffixId.SHIELD_STRIKE)

        // With Lv.1 Workshop engraving fails
        val lowWs = GameStateBundle(
            playerState = PlayerState(
                level = 20,
                equippedWeaponId = "w1",
                equippedArmorId = "a1",
                equippedCharmId = "c1",
                equippedPassives = listOf("passive_forest_ward", "passive_star_whisper"),
                facilities = mapOf(FacilityType.WORKSHOP to 1),
                resources = ResourceBag(gold = 5000L, materials = mapOf(RegionId.MISTWOOD.materialId to 200L))
            ),
            budgetState = RewardBudgetState(),
            discoveryState = DiscoveryState(onboardingCompleted = true),
            inventory = listOf(weapon, armor, charm)
        )
        assertTrue(ProgressionEngine.engraveSecondaryAffix(lowWs, "w1") is ActionOutcome.Failure)

        // Upgrade Workshop to Lv.2 -> engrave succeeds & never duplicates main affix
        val ready = lowWs.copy(
            playerState = lowWs.playerState.copy(facilities = mapOf(FacilityType.WORKSHOP to 2))
        )
        val engravedRes = ProgressionEngine.engraveSecondaryAffix(ready, "w1")
        assertTrue(engravedRes is ActionOutcome.Success)
        val engravedBundle = (engravedRes as ActionOutcome.Success).value
        val updatedWeapon = engravedBundle.inventory.single { it.instanceId == "w1" }
        assertNotNull(updatedWeapon.secondaryAffix)
        assertNotEquals(updatedWeapon.affix, updatedWeapon.secondaryAffix)

        // Check BASTION 4-piece resonance (3 Bastion affixes + 2 Bastion passives = 5 points)
        val resList = ProgressionEngine.computeActiveResonances(
            engravedBundle.inventory,
            engravedBundle.playerState.equippedPassives
        )
        val bastion = resList.single { it.archetype == BuildArchetype.BASTION }
        assertEquals(2, bastion.tier)
    }

    @Test fun `companions tea blends beacon tiers morning echoes and souvenirs behave as designed`() {
        val baseDiscovery = DiscoveryState(
            onboardingCompleted = true,
            totalCreditedSeconds = 260 * 3600L,
            regionInvestigation = mapOf(
                RegionId.MISTWOOD.id to 8,
                RegionId.TIDAL_FLAT.id to 8,
                RegionId.STARLIT_RIDGE.id to 9,
                RegionId.CLOUD_SANCTUARY.id to 6,
                RegionId.DREAM_SEA.id to 4
            ),
            companionChapters = mapOf("comp_airen" to 2, "comp_noah" to 2, "comp_lin" to 2, "comp_xiu" to 1)
        )
        val prepPlayer = PlayerState(
            level = 30,
            facilities = mapOf(
                FacilityType.MAP_TABLE to 4,
                FacilityType.WORKSHOP to 4,
                FacilityType.INN to 4,
                FacilityType.TEA_HEARTH to 5
            ),
            activeCompanionId = "comp_xiu",
            activeTeaBlendId = "tea_pine_needle",
            dreamBeaconTier = 2
        )
        val prepBundle = GameStateBundle(prepPlayer, RewardBudgetState(), baseDiscovery, emptyList())
        val snap = ProgressionEngine.createDepartureSnapshot(
            playerState = prepPlayer,
            inventory = emptyList(),
            unlockedKeepsakeIds = emptySet(),
            discoveryState = baseDiscovery
        )
        assertEquals("comp_xiu", snap.activeCompanionId)
        assertEquals(listOf("comp_airen", "comp_noah", "comp_lin"), snap.dispatchCompanionIds)
        assertEquals("tea_pine_needle", snap.activeTeaBlendId)
        assertEquals(2, snap.dreamBeaconTier)

        val nightMs = 1_700_000_000_000L
        val phenom = ContentCatalog.nightPhenomenonForUtcMs(nightMs)
        val sess = ExpeditionSession(
            sessionId = "v3_full_run",
            status = SessionStatus.RUNNING,
            routeId = "route_mistwood_outer",
            strategy = ExpeditionStrategy.STEADY,
            startAtUtcMs = nightMs,
            sessionSeed = 123456789L,
            contentVersion = ContentCatalog.CONTENT_VERSION,
            rulesVersion = ContentCatalog.RULES_VERSION,
            snapshot = snap,
            nightPhenomenon = phenom
        )
        val out = settle(sess, prepBundle, 28_800L)
        assertEquals(phenom.name, out.settlement.nightPhenomenonName)
        assertEquals("汐", out.settlement.activeCompanionName)
        assertEquals("古松长夜煎", out.settlement.teaBlendName)
        assertEquals(2, out.settlement.dreamBeaconTier)
        assertEquals(3, out.settlement.dispatchSummaries.size)
        assertNotNull(out.updatedBundle.playerState.pendingMorningEcho)
        assertNotNull(out.settlement.pendingMorningEchoTitle)
        assertTrue((out.updatedBundle.discoveryState.companionAffinity["comp_xiu"] ?: 0) >= 2)
        assertTrue((out.updatedBundle.discoveryState.companionAffinity["comp_airen"] ?: 0) >= 1)
        assertTrue((out.updatedBundle.discoveryState.dreamBeaconClears[2] ?: 0) >= 1)
        assertTrue("ms_beacon_conqueror" in out.updatedBundle.discoveryState.unlockedMilestoneIds)

        // Resolve Morning Echo option B -> grants buff for next expedition
        val goldBefore = out.updatedBundle.playerState.resources.gold
        val echoOutcome = ProgressionEngine.resolveMorningEcho(out.updatedBundle, chooseOptionA = false)
        assertTrue(echoOutcome is ActionOutcome.Success)
        val afterEcho = (echoOutcome as ActionOutcome.Success).value.updatedBundle
        assertNull(afterEcho.playerState.pendingMorningEcho)
        assertNotNull(afterEcho.playerState.activeMorningBuff)
        assertTrue(afterEcho.playerState.resources.gold >= goldBefore)

        // Next expedition on the following day (after 24h budget resets) consumes activeMorningBuff cleanly
        val snap2 = ProgressionEngine.createDepartureSnapshot(afterEcho.playerState, afterEcho.inventory, discoveryState = afterEcho.discoveryState)
        assertNotNull(snap2.activeMorningBuff)
        val nextDaySess = sess.copy(
            sessionId = "v3_run_2",
            startAtUtcMs = nightMs + 24 * 3600_000L,
            snapshot = snap2
        )
        val out2 = settle(nextDaySess, afterEcho, 300L)
        assertNull(out2.updatedBundle.playerState.activeMorningBuff)

        // Boss traits validate in combat simulator across all bosses
        for (boss in ContentCatalog.enemies.filter { it.isBoss }) {
            val route = ContentCatalog.routes.first { it.bossId == boss.id }
            val result = CombatSimulator.simulate(snap, route, listOf(boss), com.dreamtrail.core.rng.DeterministicRng(42L), phenom)
            assertTrue(result.roundsPlayed in 1..CombatSimulator.MAX_ROUNDS)
        }
    }

    @Test fun `v4 astral arch chronicles bestiary mastery resonance tier 3 astral chart transmute commissions and loadouts work end to end`() {
        // 1. Catalog counts & versions
        assertEquals(5, ContentCatalog.CONTENT_VERSION)
        assertEquals(5, ContentCatalog.RULES_VERSION)
        assertEquals(8, RegionId.entries.size)
        assertEquals(24, ContentCatalog.routes.size)
        assertEquals(56, ContentCatalog.enemies.size)
        assertEquals(40, ContentCatalog.equipmentDefs.size)
        assertEquals(128, ContentCatalog.eventTemplates.size)
        assertEquals(7, ContentCatalog.companionStories.size)
        assertEquals(13, ContentCatalog.teaBlends.size)
        assertEquals(7, ContentCatalog.nightPhenomena.size)
        assertEquals(8, ContentCatalog.dayPhenomena.size)
        assertEquals(24, ContentCatalog.souvenirs.size)
        assertEquals(16, ContentCatalog.morningEchoes.size)
        assertEquals(16, ContentCatalog.teaEchoes.size)
        assertEquals(16, ContentCatalog.focusSpirits.size)
        assertEquals(6, ContentCatalog.defaultFocusTags.size)
        assertEquals(8, ContentCatalog.chronicles.size)
        assertEquals(16, ContentCatalog.astralNodes.size)
        assertEquals(9, ContentCatalog.transmuteRecipes.size)
        assertEquals(10, ContentCatalog.commissions.size)
        assertEquals(30, ContentCatalog.milestones.size)

        // 2. Tier-3 (6-point) resonance + Dual-Archetype Synergy + 3rd passive slot
        val w6 = EquipmentInstance("w6", "eq_arch_chronoblade", 6, 5, AffixId.ASTRAL_HARMONY, AffixId.STARLIGHT_INSIGHT)
        val a6 = EquipmentInstance("a6", "eq_arch_sanctuary_mail", 6, 5, AffixId.CLUE_SEEKER, AffixId.TREASURE_SENSE)
        val c6 = EquipmentInstance("c6", "eq_arch_censer_charm", 6, 5, AffixId.CLUE_SEEKER, AffixId.ASTRAL_HARMONY)
        val tier3Res = ProgressionEngine.computeActiveResonances(listOf(w6, a6, c6), emptyList())
        assertEquals(3, tier3Res.first { it.archetype == BuildArchetype.SCHOLAR }.tier)

        val dualArmor = a6.copy(affix = AffixId.SANCTUARY_BULWARK, secondaryAffix = AffixId.THORN_BASTION)
        assertTrue(ProgressionEngine.hasDualArchetypeSynergy(listOf(w6, dualArmor), emptyList()))

        assertEquals(2, ProgressionEngine.maxPassiveSlots(PlayerState(level = 24, facilities = mapOf(FacilityType.INN to 3))))
        assertEquals(3, ProgressionEngine.maxPassiveSlots(PlayerState(level = 25, facilities = mapOf(FacilityType.INN to 3))))
        assertEquals(3, ProgressionEngine.maxPassiveSlots(PlayerState(level = 10, facilities = mapOf(FacilityType.INN to 4))))

        // 3. Lv.6-7 Stardust Masterwork gated by Workshop >= 5
        val richMat = RegionId.entries.associate { it.materialId to 400L }
        val ws4Bundle = GameStateBundle(
            playerState = PlayerState(
                level = 28,
                facilities = FacilityType.entries.associateWith { if (it == FacilityType.WORKSHOP) 4 else 5 },
                resources = ResourceBag(gold = 20_000L, bossMarks = 20L, materials = richMat)
            ),
            budgetState = RewardBudgetState(),
            discoveryState = DiscoveryState(
                onboardingCompleted = true,
                totalCreditedSeconds = 400 * 3600L,
                regionCreditedSeconds = RegionId.entries.associate { it.id to 40 * 3600L },
                regionInvestigation = RegionId.entries.associate { it.id to 20 },
                unlockedRouteIds = ContentCatalog.routes.map { it.id }.toSet(),
                unlockedPassiveIds = ContentCatalog.passives.map { it.id }.toSet()
            ),
            inventory = listOf(w6, a6, c6)
        )
        assertTrue(ProgressionEngine.upgradeEquipment(ws4Bundle, "w6") is ActionOutcome.Failure)

        val ws5Bundle = ws4Bundle.copy(
            playerState = ws4Bundle.playerState.copy(facilities = FacilityType.entries.associateWith { 5 })
        )
        val afterLv6 = (ProgressionEngine.upgradeEquipment(ws5Bundle, "w6") as ActionOutcome.Success).value
        val afterLv7 = (ProgressionEngine.upgradeEquipment(afterLv6, "w6") as ActionOutcome.Success).value
        assertEquals(7, afterLv7.inventory.first { it.instanceId == "w6" }.level)

        // 4. Astral Chart sequential tier gating & reset
        assertTrue(ProgressionEngine.unlockAstralNode(afterLv7, "astral_gale_2") is ActionOutcome.Failure)
        val ast1 = (ProgressionEngine.unlockAstralNode(afterLv7, "astral_gale_1") as ActionOutcome.Success).value
        val ast2 = (ProgressionEngine.unlockAstralNode(ast1, "astral_gale_2") as ActionOutcome.Success).value
        assertEquals(2, ProgressionEngine.usedAstralPoints(ast2.playerState))
        assertTrue(ProgressionEngine.totalAstralPoints(28) >= 7)
        val astReset = (ProgressionEngine.resetAstralNodes(ast2) as ActionOutcome.Success).value
        assertTrue(astReset.playerState.unlockedAstralNodeIds.isEmpty())
        val astFinal = (ProgressionEngine.unlockAstralNode(astReset, "astral_bastion_1") as ActionOutcome.Success).value

        // 5. Affinity ranks + gifting comp_cen + Bestiary Mastery stats & combat multiplier
        assertEquals(1, ProgressionEngine.companionAffinityRank(0))
        assertEquals(2, ProgressionEngine.companionAffinityRank(3))
        assertEquals(3, ProgressionEngine.companionAffinityRank(7))
        assertEquals(4, ProgressionEngine.companionAffinityRank(12))
        assertEquals(5, ProgressionEngine.companionAffinityRank(18))

        val reconciledForGift = ProgressionEngine.reconcileUnlocks(astFinal.playerState, astFinal.discoveryState, astFinal.inventory)
        val readyToGift = astFinal.copy(discoveryState = reconciledForGift)
        val gifted = (ProgressionEngine.giftCompanion(readyToGift, "comp_cen") as ActionOutcome.Success).value
        assertTrue((gifted.discoveryState.companionAffinity["comp_cen"] ?: 0) >= 4)

        val baseStats = ProgressionEngine.computeEffectiveStats(gifted.playerState, gifted.inventory, discoveryState = gifted.discoveryState)
        val masterDiscovery = gifted.discoveryState.copy(
            companionAffinity = ContentCatalog.companionStories.associate { it.id to 18 },
            enemyKillCounts = mapOf("enemy_arch_chronicler" to 15, "enemy_mist_slime" to 5)
        )
        val masterStats = ProgressionEngine.computeEffectiveStats(gifted.playerState, gifted.inventory, discoveryState = masterDiscovery)
        assertTrue(masterStats.maxHp > baseStats.maxHp)
        assertTrue(masterStats.attack > baseStats.attack)

        // 6. Conservatory transmutation (wood -> ore & amber -> boss mark)
        val tx1 = (ProgressionEngine.transmuteMaterial(gifted, "tx_wood_to_ore") as ActionOutcome.Success).value
        assertEquals(richMat.getValue(RegionId.TIDAL_FLAT.materialId) + 10L, tx1.playerState.resources.materialCount(RegionId.TIDAL_FLAT.materialId))
        val tx2 = (ProgressionEngine.transmuteMaterial(tx1, "tx_amber_to_mark") as ActionOutcome.Success).value
        assertEquals(21L, tx2.playerState.resources.bossMarks)

        // 7. Loadout presets save & apply
        val prepared = tx2.copy(
            playerState = tx2.playerState.copy(
                equippedWeaponId = "w6",
                equippedArmorId = "a6",
                equippedCharmId = "c6",
                equippedPassives = listOf("passive_forest_ward", "passive_amber_anchor", "passive_bestiary_scholar"),
                activeCompanionId = "comp_cen",
                activeTeaBlendId = "tea_astral_amber_brew",
                lastStrategy = ExpeditionStrategy.EXPLORE,
                dreamBeaconTier = 4
            ),
            discoveryState = masterDiscovery
        )
        val savedPreset = (ProgressionEngine.saveLoadout(prepared, 0, "星拱冲刺") as ActionOutcome.Success).value
        val mutated = savedPreset.copy(
            playerState = savedPreset.playerState.copy(activeCompanionId = null, equippedPassives = emptyList(), lastStrategy = ExpeditionStrategy.GATHER)
        )
        val applied = (ProgressionEngine.applyLoadout(mutated, 0) as ActionOutcome.Success).value
        assertEquals("comp_cen", applied.playerState.activeCompanionId)
        assertEquals(3, applied.playerState.equippedPassives.size)
        assertEquals(ExpeditionStrategy.EXPLORE, applied.playerState.lastStrategy)

        // 8. 8h ASTRAL_ARCH settlement with Beacon Tier 4, comp_cen active + 4 dispatch companions, Chronicle + Commission + Elite combat + v4 Backup
        val snap = ProgressionEngine.createDepartureSnapshot(
            playerState = applied.playerState,
            inventory = applied.inventory,
            unlockedKeepsakeIds = applied.discoveryState.unlockedKeepsakeIds,
            discoveryState = applied.discoveryState
        )
        assertEquals("comp_cen", snap.activeCompanionId)
        assertEquals(ContentCatalog.companionStories.size - 1, snap.dispatchCompanionIds.size)

        val archRoute = ContentCatalog.routeById("route_astral_arcade")!!
        val sentinel = ContentCatalog.enemyById("enemy_arch_chronicler")!!
        val eliteRes = CombatSimulator.simulate(
            snapshot = snap,
            route = archRoute,
            enemies = listOf(sentinel),
            rng = com.dreamtrail.core.rng.DeterministicRng(99L),
            nightPhenomenon = ContentCatalog.nightPhenomena.last(),
            eliteTrait = EliteTrait.IRON_SHELL
        )
        assertEquals(CombatOutcome.VICTORY, eliteRes.outcome)

        val nightMs = 1_700_000_000_000L
        val archSess = ExpeditionSession(
            sessionId = "v4_arch_run",
            status = SessionStatus.RUNNING,
            routeId = archRoute.id,
            strategy = ExpeditionStrategy.EXPLORE,
            startAtUtcMs = nightMs,
            sessionSeed = 7777777L,
            contentVersion = ContentCatalog.CONTENT_VERSION,
            rulesVersion = ContentCatalog.RULES_VERSION,
            snapshot = snap,
            nightPhenomenon = ContentCatalog.nightPhenomena.last()
        )
        val settled = settle(archSess, applied, 28_800L)
        assertEquals("岑", settled.settlement.activeCompanionName)
        assertEquals(ContentCatalog.companionStories.size - 1, settled.settlement.dispatchSummaries.size)
        assertEquals(4, settled.settlement.dreamBeaconTier)
        assertTrue("comm_astral_pendulums" in settled.updatedBundle.discoveryState.completedCommissionIds)
        assertTrue((settled.updatedBundle.discoveryState.chronicleProgress["chron_astral_arch"] ?: 0) >= 1)
        assertTrue(settled.updatedBundle.discoveryState.enemyKillCounts.isNotEmpty())

        val backup = SaveBackupPayload(
            exportedAtUtcMs = nightMs + 28_800_000L,
            contentVersion = ContentCatalog.CONTENT_VERSION,
            rulesVersion = ContentCatalog.RULES_VERSION,
            playerState = settled.updatedBundle.playerState,
            budgetState = settled.updatedBundle.budgetState,
            discoveryState = settled.updatedBundle.discoveryState,
            inventory = settled.updatedBundle.inventory
        )
        val encoded = SaveBackupCodec.encode(backup)
        val decoded = SaveBackupCodec.decodeAndValidate(encoded)
        assertTrue(decoded is ActionOutcome.Success)
        assertEquals(backup.playerState.loadouts, (decoded as ActionOutcome.Success).value.playerState.loadouts)
    }

    @Test fun `v5 soluna dual core focus pomodoro flowmodoro poise tasks spirits scriptorium and regions 7 to 8 work end to end`() {
        // 1. Dual budget isolation: 8h sleep does NOT block daytime 8h focus, and vice versa
        val initial = bundle()
        val nightStart = 1_700_000_000L
        val sleepSess = session(initial, startSec = nightStart).copy(sessionMode = SessionMode.SLEEP)
        val afterSleep = settle(sleepSess, initial, 8 * 3600L)
        assertEquals(28_800L, afterSleep.settlement.effectiveCreditedSeconds)
        assertEquals(0L, afterSleep.settlement.inspirationInkGained)
        assertEquals(240, afterSleep.settlement.focusVigourGrantedMinutes)
        assertNotNull(afterSleep.updatedBundle.playerState.focusVigourBuff)
        assertEquals(0L, RewardBudgetCalculator.remainingRollingBudgetSeconds(afterSleep.updatedBundle.budgetState, nightStart + 8 * 3600L))
        assertEquals(8 * 3600L, RewardBudgetCalculator.remainingFocusBudgetSeconds(afterSleep.updatedBundle.budgetState, nightStart + 8 * 3600L))

        // 2. Add a linked task + run 25m (1500s) uninterrupted Pomodoro with focusVigour active
        val withTask = (ProgressionEngine.addFocusTask(
            afterSleep.updatedBundle, "复习线性代数", "tag_study", 1, (nightStart + 9 * 3600L) * 1000L, "task_1"
        ) as ActionOutcome.Success).value
        val snapFocus = ProgressionEngine.createDepartureSnapshot(
            withTask.playerState, withTask.inventory, withTask.discoveryState.unlockedKeepsakeIds, withTask.discoveryState
        )
        assertNotNull(snapFocus.focusVigourBuff)
        val focusStart = nightStart + 9 * 3600L
        val focusSess = ExpeditionSession(
            sessionId = "focus_pomo_1",
            status = SessionStatus.RUNNING,
            routeId = "route_mistwood_outer",
            strategy = ExpeditionStrategy.EXPLORE,
            startAtUtcMs = focusStart * 1000L,
            sessionSeed = 11223344L,
            contentVersion = ContentCatalog.CONTENT_VERSION,
            rulesVersion = ContentCatalog.RULES_VERSION,
            snapshot = snapFocus,
            sessionMode = SessionMode.FOCUS_POMODORO,
            targetDurationSeconds = 1500L,
            focusTagId = "tag_study",
            linkedTaskId = "task_1",
            dayPhenomenon = ContentCatalog.dayPhenomenonForUtcMs(focusStart * 1000L),
            distractionCount = 0,
            flipFocusSeconds = 600L
        )
        val afterFocus1 = settle(focusSess, withTask, 1500L)
        assertEquals(1500L, afterFocus1.settlement.effectiveCreditedSeconds)
        assertEquals(1000, afterFocus1.settlement.sleepBonusBasisPoints)
        assertTrue(afterFocus1.settlement.inspirationInkGained > 0L)
        assertTrue(afterFocus1.settlement.taskCompletedNow)
        assertEquals(5, afterFocus1.settlement.recommendedBreakMinutes)
        assertEquals(25, afterFocus1.settlement.nightPrepGained)
        assertEquals(25, afterFocus1.updatedBundle.playerState.nightPrepCharge)
        assertTrue("ms_first_focus" in afterFocus1.updatedBundle.discoveryState.unlockedMilestoneIds)
        assertTrue("ms_poise_master" in afterFocus1.updatedBundle.discoveryState.unlockedMilestoneIds)
        assertTrue("ms_soluna_cycle" in afterFocus1.updatedBundle.discoveryState.unlockedMilestoneIds)
        assertTrue(afterFocus1.ledgerEntries.any { it.rewardKind == RewardKind.BASE_INSPIRATION_INK })

        // 3. Inclusive distraction poise step-down: 1 distraction -> 600 bps, 3 distractions -> 300 bps
        assertEquals(600, com.dreamtrail.core.sleep.FocusPoisePolicy.previewBasisPoints(1500L, 1500L, distractionCount = 1))
        assertEquals(450, com.dreamtrail.core.sleep.FocusPoisePolicy.previewBasisPoints(1500L, 1500L, distractionCount = 2))
        assertEquals(300, com.dreamtrail.core.sleep.FocusPoisePolicy.previewBasisPoints(1500L, 1500L, distractionCount = 3))

        // 4. Task completion daily bounty cap <= 6 per 24h
        var bountyBundle = afterFocus1.updatedBundle
        val tNow = (focusStart + 2000L) * 1000L
        repeat(7) { idx ->
            bountyBundle = (ProgressionEngine.addFocusTask(bountyBundle, "任务$idx", "tag_work", 1, tNow, "t_cap_$idx") as ActionOutcome.Success).value
        }
        val goldBeforeCap = bountyBundle.playerState.resources.gold
        repeat(7) { idx ->
            bountyBundle = (ProgressionEngine.toggleOrCompleteFocusTask(bountyBundle, "t_cap_$idx", tNow) as ActionOutcome.Success).value
        }
        // Only 5 more bounties could succeed (1 already awarded during focusSess -> total 6 in 24h)
        assertEquals(goldBeforeCap + 5 * 25L, bountyBundle.playerState.resources.gold)

        // 5. Night prep tier boosts subsequent night sleep and resets charge
        val prepSnap = ProgressionEngine.createDepartureSnapshot(bountyBundle.playerState, bountyBundle.inventory, discoveryState = bountyBundle.discoveryState)
        assertEquals(1, prepSnap.nightPrepTier)
        val night2 = ExpeditionSession(
            sessionId = "night_prep_used",
            status = SessionStatus.RUNNING,
            routeId = "route_mistwood_outer",
            strategy = ExpeditionStrategy.STEADY,
            startAtUtcMs = (focusStart + 16 * 3600L) * 1000L,
            sessionSeed = 556677L,
            contentVersion = ContentCatalog.CONTENT_VERSION,
            rulesVersion = ContentCatalog.RULES_VERSION,
            snapshot = prepSnap,
            sessionMode = SessionMode.SLEEP
        )
        val afterNight2 = settle(night2, bountyBundle, 6 * 3600L)
        assertEquals(1, afterNight2.settlement.nightPrepTierUsed)
        assertEquals(0, afterNight2.updatedBundle.playerState.nightPrepCharge)

        // 6. Scriptorium upgrade + Regions 7 & 8 + Level 65 + Tier 4 Astral + Focus Spirits + v5 Backup
        val endgameBundle = GameStateBundle(
            playerState = PlayerState(
                level = 64,
                facilities = FacilityType.entries.associateWith { if (it == FacilityType.SCRIPTORIUM) 0 else 5 },
                resources = ResourceBag(gold = 50_000L, bossMarks = 20L, inspirationInk = 500L,
                    materials = RegionId.entries.associate { it.materialId to 400L }),
                activeCompanionId = "comp_wen",
                activeTeaBlendId = "tea_soluna_nectar"
            ),
            budgetState = RewardBudgetState(),
            discoveryState = DiscoveryState(
                onboardingCompleted = true,
                totalCreditedSeconds = 650 * 3600L,
                totalFocusCreditedSeconds = 80 * 3600L,
                totalFocusSessionsCompleted = 30,
                regionInvestigation = RegionId.entries.associate { it.id to 12 },
                regionCreditedSeconds = RegionId.entries.associate { it.id to 30 * 3600L }
            ),
            inventory = listOf(
                EquipmentInstance("w8", "eq_spire_soluna_halberd", 8, 7, AffixId.POMO_BLITZ, AffixId.INK_SCHOLAR),
                EquipmentInstance("a8", "eq_spire_bastion_aegis", 8, 7, AffixId.POISE_WARD, AffixId.SOLUNA_DEW),
                EquipmentInstance("c8", "eq_spire_prism_astrolabe", 8, 7, AffixId.POISE_WARD, AffixId.INK_SCHOLAR)
            )
        )
        val reEndgame = endgameBundle.copy(
            playerState = endgameBundle.playerState.copy(equippedWeaponId = "w8", equippedArmorId = "a8", equippedCharmId = "c8"),
            discoveryState = ProgressionEngine.reconcileUnlocks(endgameBundle.playerState, endgameBundle.discoveryState)
        )
        assertTrue("route_athenaeum_cloister" in reEndgame.discoveryState.unlockedRouteIds)
        assertTrue("route_spire_zenith" in reEndgame.discoveryState.unlockedRouteIds)
        assertTrue((reEndgame.discoveryState.companionChapters["comp_wen"] ?: 0) >= 1)
        assertTrue((reEndgame.discoveryState.companionChapters["comp_zhaolu"] ?: 0) >= 1)

        var built = reEndgame
        repeat(5) {
            built = (ProgressionEngine.upgradeFacility(built, FacilityType.SCRIPTORIUM) as ActionOutcome.Success).value
        }
        assertEquals(5, built.playerState.facilityLevel(FacilityType.SCRIPTORIUM))
        built = (ProgressionEngine.unlockAstralNode(built, "astral_scholar_1") as ActionOutcome.Success).value
        built = (ProgressionEngine.unlockAstralNode(built, "astral_scholar_2") as ActionOutcome.Success).value
        built = (ProgressionEngine.unlockAstralNode(built, "astral_scholar_3") as ActionOutcome.Success).value
        built = (ProgressionEngine.unlockAstralNode(built, "astral_scholar_4") as ActionOutcome.Success).value

        val spireSnap = ProgressionEngine.createDepartureSnapshot(built.playerState, built.inventory, discoveryState = built.discoveryState)
        val spireStart = 1_700_500_000L
        val spireFocus = ExpeditionSession(
            sessionId = "spire_focus",
            status = SessionStatus.RUNNING,
            routeId = "route_spire_zenith",
            strategy = ExpeditionStrategy.EXPLORE,
            startAtUtcMs = spireStart * 1000L,
            sessionSeed = 999888777L,
            contentVersion = ContentCatalog.CONTENT_VERSION,
            rulesVersion = ContentCatalog.RULES_VERSION,
            snapshot = spireSnap,
            sessionMode = SessionMode.FOCUS_FLOW,
            focusTagId = "tag_create",
            dayPhenomenon = ContentCatalog.dayPhenomenonForUtcMs(spireStart * 1000L)
        )
        val spireOut = settle(spireFocus, built, 4 * 3600L)
        assertEquals(SessionMode.FOCUS_FLOW, spireOut.settlement.sessionMode)
        assertTrue((spireOut.settlement.materialsGained[RegionId.DAWNLIGHT_SPIRE.materialId] ?: 0L) > 0L)
        assertTrue(spireOut.ledgerEntries.any { it.rewardKind == RewardKind.BASE_MAT_SPIRE })
        assertNotNull(spireOut.updatedBundle.playerState.pendingTeaEcho)
        val afterTeaEcho = (ProgressionEngine.resolveTeaEcho(spireOut.updatedBundle, chooseOptionA = false) as ActionOutcome.Success).value.updatedBundle
        assertNotNull(afterTeaEcho.playerState.activeTeaEchoBuff)

        val v5Backup = SaveBackupPayload(
            exportedAtUtcMs = spireStart * 1000L + 4 * 3600_000L,
            contentVersion = ContentCatalog.CONTENT_VERSION,
            rulesVersion = ContentCatalog.RULES_VERSION,
            playerState = afterTeaEcho.playerState,
            budgetState = afterTeaEcho.budgetState,
            discoveryState = afterTeaEcho.discoveryState,
            inventory = afterTeaEcho.inventory,
            recentSettlements = listOf(spireOut.settlement),
            ledgerEntries = spireOut.ledgerEntries
        )
        val decodedV5 = SaveBackupCodec.decodeAndValidate(SaveBackupCodec.encode(v5Backup))
        assertTrue(decodedV5 is ActionOutcome.Success, decodedV5.toString())
    }
}
