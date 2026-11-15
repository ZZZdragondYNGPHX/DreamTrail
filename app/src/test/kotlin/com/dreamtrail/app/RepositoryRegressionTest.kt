package com.dreamtrail.app

import androidx.test.core.app.ApplicationProvider
import com.dreamtrail.app.data.local.DreamTrailDatabase
import com.dreamtrail.app.data.local.DiscoveryStateEntity
import com.dreamtrail.app.data.local.PlayerStateEntity
import com.dreamtrail.core.save.SaveBackupCodec
import com.dreamtrail.app.data.repository.GameRepository
import com.dreamtrail.app.time.ClockProvider
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.ActionOutcome
import com.dreamtrail.core.model.*
import com.dreamtrail.core.sleep.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryRegressionTest {
    private class Clock(var t: Long = 1_700_000_000_000L) : ClockProvider {
        override fun currentWallTimeUtcMs() = t
        override fun elapsedRealtimeMs() = t - 1_600_000_000_000L
        override fun currentBootId() = "boot_1"
    }

    @Test fun `import preserves the frozen night phenomenon across repository recreation`() = runTest {
        val db = DreamTrailDatabase.openInMemory(ApplicationProvider.getApplicationContext())
        try {
            val clock = Clock()
            val repo = GameRepository(db, clock)
            repo.ensureInitializedAndRecover()
            repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY)
            val payload = (SaveBackupCodec.decodeAndValidate((repo.exportBackupJson() as ActionOutcome.Success).value) as ActionOutcome.Success).value
            val phenomenon = ContentCatalog.nightPhenomena.first().copy(name = "Frozen night", goldBonusPercent = 17)
            val changed = payload.copy(activeSession = payload.activeSession!!.copy(nightPhenomenon = phenomenon))
            assertTrue(repo.importBackupJson(SaveBackupCodec.encode(changed)) is ActionOutcome.Success)
            val recreated = GameRepository(db, clock)
            val running = recreated.stateFlow.first { it.initialized }.runningSession!!
            assertEquals(phenomenon, running.nightPhenomenon)
            clock.t += 8 * 3600_000L
            val report = (recreated.endAndSettleExpedition(running.sessionId) as ActionOutcome.Success).value
            assertEquals("Frozen night", report.nightPhenomenonName)
        } finally { db.close() }
    }

    @Test fun `initialization persists newly eligible route passive and milestone unlocks`() = runTest {
        val db = DreamTrailDatabase.openInMemory(ApplicationProvider.getApplicationContext())
        try {
            val repo = GameRepository(db, Clock())
            repo.ensureInitializedAndRecover()
            val player = PlayerState(facilities = mapOf(FacilityType.INN to 4))
            val discovery = DiscoveryState(totalCreditedSeconds = 240 * 3600L,
                regionInvestigation = RegionId.entries.associate { it.id to 20 }, totalJourneysCompleted = 30)
            db.dao().upsertPlayerState(PlayerStateEntity(1, player.version,
                SaveBackupCodec.json.encodeToString(PlayerState.serializer(), player)))
            db.dao().upsertDiscoveryState(DiscoveryStateEntity(1,
                SaveBackupCodec.json.encodeToString(DiscoveryState.serializer(), discovery)))
            repo.ensureInitializedAndRecover()
            val saved = SaveBackupCodec.json.decodeFromString(DiscoveryState.serializer(), db.dao().getDiscoveryState()!!.payloadJson)
            assertEquals(12, saved.unlockedRouteIds.size)
            assertTrue("passive_cloud_aegis" in saved.unlockedPassiveIds)
            assertTrue("ms_cloud_walker" in saved.unlockedMilestoneIds)
            assertTrue("ms_camp_builder" in saved.unlockedMilestoneIds)
        } finally { db.close() }
    }
    @Test fun `restored backup preserves completed session for repeat settlement and sleep topup`() = runTest {
        val db = DreamTrailDatabase.openInMemory(ApplicationProvider.getApplicationContext())
        try {
            val clock = Clock()
            val repo = GameRepository(db, clock)
            repo.ensureInitializedAndRecover()
            val s = (repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY) as ActionOutcome.Success).value
            clock.t += 8 * 3600_000L
            assertTrue(repo.endAndSettleExpedition(s.sessionId) is ActionOutcome.Success)
            val backup = (repo.exportBackupJson() as ActionOutcome.Success).value
            assertTrue(repo.importBackupJson(backup) is ActionOutcome.Success)
            val retry = repo.endAndSettleExpedition(s.sessionId)
            println("retryAfterRestore=$retry, sessionRow=${db.dao().getSessionById(s.sessionId)}")
            assertTrue("completed session was removed: $retry", retry is ActionOutcome.Success)
        } finally { db.close() }
    }
    @Test fun `revoked provider epoch rejects an already in flight old record`() = runTest {
        val db = DreamTrailDatabase.openInMemory(ApplicationProvider.getApplicationContext())
        try {
            val provider = object : SleepDataProvider {
                override val providerId = "fake"
                override suspend fun checkAvailability() = SleepProviderAvailability.NEEDS_PERMISSION
                override suspend fun currentConsentEpoch() = 2L
                override suspend fun readSleepRecords(fromUtcMs: Long, toUtcMs: Long, expectedConsentEpoch: Long) = emptyList<SleepRecord>()
            }
            val clock = Clock()
            val repo = GameRepository(db, clock, provider)
            repo.ensureInitializedAndRecover()
            val s = (repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY) as ActionOutcome.Success).value
            clock.t += 8 * 3600_000L
            repo.endAndSettleExpedition(s.sessionId)
            val old = SleepRecord("fake", "mi", "old", "1", s.startAtUtcMs, clock.t, asleepSeconds = 8*3600, observedAtUtcMs = clock.t, consentEpoch = 1)
            val result = repo.applyLateSleepRecords(s.sessionId, listOf(old), expectedConsentEpoch = 1)
            println("providerEpoch=2 requestedEpoch=1 result=$result")
            assertTrue("revoked epoch must not grant rewards: $result", result is ActionOutcome.Failure || (result as ActionOutcome.Success).value.deltaGold == 0L)
        } finally { db.close() }
    }
    @Test fun `backup on another device must discard elapsed realtime and boot count anchor`() = runTest {
        val db = DreamTrailDatabase.openInMemory(ApplicationProvider.getApplicationContext())
        try {
            val sourceClock = Clock()
            val source = GameRepository(db, sourceClock)
            source.ensureInitializedAndRecover()
            val s = (source.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY) as ActionOutcome.Success).value
            val backup = (source.exportBackupJson() as ActionOutcome.Success).value
            val destinationClock = object : ClockProvider {
                override fun currentWallTimeUtcMs() = s.startAtUtcMs + 3600_000L
                override fun currentBootId() = "boot_1"
                override fun elapsedRealtimeMs() = s.startElapsedRealtimeMs!! + 10 * 3600_000L
            }
            val destination = GameRepository(db, destinationClock)
            assertTrue(destination.importBackupJson(backup) is ActionOutcome.Success)
            val report = (destination.endAndSettleExpedition(s.sessionId) as ActionOutcome.Success).value
            println("crossDevice wallElapsed=3600 monoElapsed=36000 actual=${report.rawDurationSeconds} credited=${report.effectiveCreditedSeconds}")
            assertEquals(3600L, report.rawDurationSeconds)
        } finally { db.close() }
    }


    private class Provider : SleepDataProvider {
        var epoch = 1L
        var enabled = true
        var checks = 0
        var revokeOnCheck = Int.MAX_VALUE
        override val providerId = "fake"
        override suspend fun checkAvailability(): SleepProviderAvailability {
            checks++
            if (checks == revokeOnCheck) { epoch++; enabled = false }
            return if (enabled) SleepProviderAvailability.READY else SleepProviderAvailability.NEEDS_PERMISSION
        }
        override suspend fun currentConsentEpoch() = epoch
        override suspend fun readSleepRecords(fromUtcMs: Long, toUtcMs: Long, expectedConsentEpoch: Long) = emptyList<SleepRecord>()
    }

    @Test fun `revoke during commit rolls back balances ledger and report then rejects old response after regrant`() = runTest {
        val db = DreamTrailDatabase.openInMemory(ApplicationProvider.getApplicationContext())
        try {
            val clock = Clock()
            val provider = Provider()
            val repo = GameRepository(db, clock, provider)
            repo.ensureInitializedAndRecover()
            repo.updateSleepConsent(true, 1)
            val s = (repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY) as ActionOutcome.Success).value
            clock.t += 8 * 3600_000L
            repo.endAndSettleExpedition(s.sessionId)
            val before = repo.stateFlow.first { it.initialized }
            val ledger = db.dao().getLedgerForSession(s.sessionId)
            val record = SleepRecord("fake", "mi", "a", "1", s.startAtUtcMs, clock.t, asleepSeconds = 28800, observedAtUtcMs = clock.t, consentEpoch = 1)
            provider.revokeOnCheck = provider.checks + 2
            assertTrue(repo.applyLateSleepRecords(s.sessionId, listOf(record), 1) is ActionOutcome.Failure)
            assertEquals(before.playerState, repo.stateFlow.first { it.initialized }.playerState)
            assertEquals(before.recentSettlements, repo.stateFlow.first { it.initialized }.recentSettlements)
            assertEquals(ledger, db.dao().getLedgerForSession(s.sessionId))
            assertTrue(repo.updateSleepConsent(false, 2) is ActionOutcome.Success)
            provider.epoch = 3; provider.enabled = true
            assertTrue(repo.updateSleepConsent(true, 3) is ActionOutcome.Success)
            assertTrue(repo.applyLateSleepRecords(s.sessionId, listOf(record), 1) is ActionOutcome.Failure)
            val success = repo.applyLateSleepRecords(s.sessionId, listOf(record.copy(consentEpoch = 3)), 3)
            assertTrue(success is ActionOutcome.Success)
        } finally { db.close() }
    }

    @Test fun `backup and snapshot retain report based topups without restoring health authorization`() = runTest {
        val db = DreamTrailDatabase.openInMemory(ApplicationProvider.getApplicationContext())
        try {
            val clock = Clock()
            val provider = Provider()
            val repo = GameRepository(db, clock, provider)
            repo.ensureInitializedAndRecover()
            repo.updateSleepConsent(true, 1)
            val s = (repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY) as ActionOutcome.Success).value
            clock.t += 8 * 3600_000L
            repo.endAndSettleExpedition(s.sessionId)
            val snapshot = db.dao().getLatestSnapshots().first().snapshotId
            val backup = (repo.exportBackupJson() as ActionOutcome.Success).value
            assertFalse(backup.contains("sleep_consent"))
            repo.importBackupJson(backup)
            val record = SleepRecord("fake", "mi", "a", "1", s.startAtUtcMs, clock.t, asleepSeconds = 28800, deepSeconds = 2700, remSeconds = 3600, observedAtUtcMs = clock.t, consentEpoch = 1)
            val delta = (repo.applyLateSleepRecords(s.sessionId, listOf(record), 1) as ActionOutcome.Success).value
            val updated = repo.stateFlow.first { it.initialized }.recentSettlements.first()
            assertEquals(775, updated.sleepBonusBasisPoints)
            assertEquals(updated.initialBlessingGold + delta.deltaGold, updated.blessingGold)
            assertEquals(0L, (repo.applyLateSleepRecords(s.sessionId, listOf(record), 1) as ActionOutcome.Success).value.deltaGold)
            repo.restoreSnapshot(snapshot)
            assertTrue(repo.endAndSettleExpedition(s.sessionId) is ActionOutcome.Success)
            assertTrue(repo.applyLateSleepRecords(s.sessionId, listOf(record), 1) is ActionOutcome.Success)
        } finally { db.close() }
    }

    @Test fun `file IO limits input and reports output failure`() {
        val oversized = ByteArray(com.dreamtrail.core.save.SaveBackupCodec.MAX_IMPORT_BYTES + 1)
        assertTrue(com.dreamtrail.app.data.BackupFiles.read { oversized.inputStream() } is ActionOutcome.Failure)
        assertTrue(com.dreamtrail.app.data.BackupFiles.read { null } is ActionOutcome.Failure)
        assertTrue(com.dreamtrail.app.data.BackupFiles.write({ object : java.io.OutputStream() {
            override fun write(value: Int) { throw java.io.IOException("storage unavailable") }
        } }, "save") is ActionOutcome.Failure)
        val output = java.io.ByteArrayOutputStream()
        assertTrue(com.dreamtrail.app.data.BackupFiles.write({ output }, "梦途") is ActionOutcome.Success)
        assertEquals("梦途", (com.dreamtrail.app.data.BackupFiles.read { output.toByteArray().inputStream() } as ActionOutcome.Success).value)
    }

    @Test fun `dual affix engraving companion tea echo and backup round trip persist in Room`() = runTest {
        val db = DreamTrailDatabase.openInMemory(ApplicationProvider.getApplicationContext())
        try {
            val clock = Clock()
            val provider = Provider()
            val repo = GameRepository(db, clock, provider)
            repo.ensureInitializedAndRecover()

            // Seed upgraded Workshop Lv.2 + Lv.3 weapon + Tea Hearth Lv.1 + unlocked comp_airen via backup import
            val sword = EquipmentInstance("w_dual", "eq_pine_staff", 1, 3, AffixId.RAIN_WARD)
            val seededPayload = com.dreamtrail.core.save.SaveBackupPayload(
                exportedAtUtcMs = clock.t,
                contentVersion = ContentCatalog.CONTENT_VERSION,
                rulesVersion = ContentCatalog.RULES_VERSION,
                playerState = PlayerState(
                    level = 12,
                    equippedWeaponId = sword.instanceId,
                    facilities = mapOf(
                        FacilityType.MAP_TABLE to 1,
                        FacilityType.WORKSHOP to 2,
                        FacilityType.INN to 1,
                        FacilityType.TEA_HEARTH to 1
                    ),
                    resources = ResourceBag(gold = 2000L, materials = mapOf(RegionId.MISTWOOD.materialId to 50L))
                ),
                budgetState = RewardBudgetState(),
                discoveryState = DiscoveryState(
                    onboardingCompleted = true,
                    totalCreditedSeconds = 40 * 3600L,
                    regionInvestigation = mapOf(RegionId.MISTWOOD.id to 5)
                ),
                inventory = listOf(sword)
            )
            assertTrue(repo.importBackupJson(com.dreamtrail.core.save.SaveBackupCodec.encode(seededPayload)) is ActionOutcome.Success)

            // Reload both affixes through the database before starting a journey.
            assertTrue(repo.engraveSecondaryAffix("w_dual") is ActionOutcome.Success)
            val savedSword = db.dao().getAllInventory().single()
            assertEquals(AffixId.RAIN_WARD.name, savedSword.affix)
            assertNotNull(savedSword.secondaryAffix)
            assertNotEquals(savedSword.affix, savedSword.secondaryAffix)

            // Select companion, tea, beacon tier, run 4h expedition, verify Morning Echo & dispatch & Room session recovery
            assertTrue(repo.selectActiveCompanion("comp_airen") is ActionOutcome.Success)
            assertTrue(repo.selectTeaBlend("tea_chamomile") is ActionOutcome.Success)
            assertTrue(repo.selectDreamBeaconTier(1) is ActionOutcome.Success)

            val sess = (repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY) as ActionOutcome.Success).value
            assertNotNull(sess.nightPhenomenon)
            clock.t += 4 * 3600_000L
            val report = (repo.endAndSettleExpedition(sess.sessionId) as ActionOutcome.Success).value
            assertEquals("甘菊安神饮", report.teaBlendName)
            assertEquals("艾琳", report.activeCompanionName)
            assertEquals(1, report.dreamBeaconTier)
            assertNotNull(report.pendingMorningEchoTitle)

            // Resolve pending morning echo and verify persistence via backup export/decode
            val echoRes = repo.resolveMorningEcho(chooseOptionA = false)
            assertTrue(echoRes is ActionOutcome.Success)
            val stateAfter = repo.stateFlow.first { it.initialized && it.playerState.pendingMorningEcho == null }
            assertNotNull(stateAfter.playerState.activeMorningBuff)
            assertNotNull(stateAfter.inventory.first { it.instanceId == "w_dual" }.secondaryAffix)

            val backupJson = (repo.exportBackupJson() as ActionOutcome.Success).value
            val decoded = com.dreamtrail.core.save.SaveBackupCodec.decodeAndValidate(backupJson)
            assertTrue(decoded is ActionOutcome.Success)
            assertNotNull((decoded as ActionOutcome.Success).value.inventory.first { it.instanceId == "w_dual" }.secondaryAffix)
        } finally { db.close() }
    }

    @Test fun `v4 astral chart loadouts gift companion transmute commission and starforge persist across db`() = runTest {
        val db = DreamTrailDatabase.openInMemory(ApplicationProvider.getApplicationContext())
        try {
            val clock = Clock()
            val provider = Provider()
            val repo = GameRepository(db, clock, provider)
            repo.ensureInitializedAndRecover()

            // Seed endgame-capable state with Workshop Lv.5, Conservatory Lv.5, Lv.28 traveler, Astral Arch unlocked
            val t6Staff = EquipmentInstance("w_t6", "eq_arch_chronoblade", 6, 5, AffixId.CHRONO_STRIKE, AffixId.ASTRAL_HARMONY)
            val seedPayload = com.dreamtrail.core.save.SaveBackupPayload(
                exportedAtUtcMs = clock.t,
                contentVersion = ContentCatalog.CONTENT_VERSION,
                rulesVersion = ContentCatalog.RULES_VERSION,
                playerState = PlayerState(
                    level = 28,
                    equippedWeaponId = t6Staff.instanceId,
                    facilities = mapOf(
                        FacilityType.MAP_TABLE to 5,
                        FacilityType.WORKSHOP to 5,
                        FacilityType.INN to 4,
                        FacilityType.TEA_HEARTH to 5,
                        FacilityType.CONSERVATORY to 5
                    ),
                    resources = ResourceBag(
                        gold = 12000L,
                        bossMarks = 15L,
                        materials = RegionId.entries.associate { it.materialId to 250L }
                    )
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
                inventory = listOf(t6Staff)
            )
            assertTrue(repo.importBackupJson(com.dreamtrail.core.save.SaveBackupCodec.encode(seedPayload)) is ActionOutcome.Success)

            // 1. Stardust Masterwork Lv.5 -> Lv.6 -> Lv.7
            assertTrue(repo.upgradeEquipment("w_t6") is ActionOutcome.Success)
            assertTrue(repo.upgradeEquipment("w_t6") is ActionOutcome.Success)
            val upgraded = db.dao().getAllInventory().single()
            assertEquals(7, upgraded.level)

            // 2. Equip 3rd passive slot (unlocked at Lv.25 / Inn Lv.4)
            assertTrue(repo.togglePassive("passive_amber_anchor") is ActionOutcome.Success)
            assertTrue(repo.togglePassive("passive_chrono_step") is ActionOutcome.Success)
            val withPassives = repo.stateFlow.first { it.initialized && it.playerState.equippedPassives.size == 3 }
            assertEquals(3, withPassives.playerState.equippedPassives.size)

            // 3. Astral chart unlock & reset
            assertTrue(repo.unlockAstralNode("astral_scholar_1") is ActionOutcome.Success)
            assertTrue(repo.unlockAstralNode("astral_scholar_2") is ActionOutcome.Success)
            val withAstral = repo.stateFlow.first { it.initialized && "astral_scholar_2" in it.playerState.unlockedAstralNodeIds }
            assertEquals(2, withAstral.playerState.unlockedAstralNodeIds.size)
            assertTrue(repo.resetAstralNodes() is ActionOutcome.Success)
            assertTrue(repo.unlockAstralNode("astral_bastion_1") is ActionOutcome.Success)

            // 4. Conservatory transmutation & gifting companion comp_cen
            val marksBefore = repo.stateFlow.first { it.initialized && "astral_bastion_1" in it.playerState.unlockedAstralNodeIds }.playerState.resources.bossMarks
            assertTrue(repo.transmuteMaterial("tx_amber_to_mark") is ActionOutcome.Success)
            val marksAfter = repo.stateFlow.first { it.initialized && it.playerState.resources.bossMarks > marksBefore }.playerState.resources.bossMarks
            assertTrue(marksAfter > marksBefore)

            assertTrue(repo.giftCompanion("comp_cen") is ActionOutcome.Success)
            val afterGift = repo.stateFlow.first { it.initialized && (it.discoveryState.companionAffinity["comp_cen"] ?: 0) >= 4 }
            assertTrue((afterGift.discoveryState.companionAffinity["comp_cen"] ?: 0) >= 4)

            // 5. Save loadout preset, mutate active setup, then restore loadout & run Tier-4 beacon expedition in ASTRAL_ARCH
            assertTrue(repo.selectActiveCompanion("comp_cen") is ActionOutcome.Success)
            assertTrue(repo.selectTeaBlend("tea_astral_amber_brew") is ActionOutcome.Success)
            assertTrue(repo.saveLoadout(0, "星拱冲刺") is ActionOutcome.Success)
            assertTrue(repo.selectActiveCompanion(null) is ActionOutcome.Success)
            assertTrue(repo.applyLoadout(0) is ActionOutcome.Success)
            val restoredState = repo.stateFlow.first { it.initialized && it.playerState.activeCompanionId == "comp_cen" }
            assertEquals("tea_astral_amber_brew", restoredState.playerState.activeTeaBlendId)

            assertTrue(repo.selectDreamBeaconTier(4) is ActionOutcome.Success)
            val sess = (repo.startExpedition("route_astral_arcade", ExpeditionStrategy.EXPLORE) as ActionOutcome.Success).value
            clock.t += 4 * 3600_000L
            val report = (repo.endAndSettleExpedition(sess.sessionId) as ActionOutcome.Success).value
            assertEquals("岑", report.activeCompanionName)
            assertEquals("琥珀星归晚露", report.teaBlendName)
            assertEquals(4, report.dreamBeaconTier)
            assertTrue("comm_astral_pendulums" in repo.stateFlow.first { it.initialized && it.runningSession == null }.discoveryState.completedCommissionIds)

            // 6. Export/decode preserves v4 player/discovery fields
            val backupJson = (repo.exportBackupJson() as ActionOutcome.Success).value
            val decoded = (com.dreamtrail.core.save.SaveBackupCodec.decodeAndValidate(backupJson) as ActionOutcome.Success).value
            assertEquals(setOf("astral_bastion_1"), decoded.playerState.unlockedAstralNodeIds)
            assertEquals(1, decoded.playerState.loadouts.size)
            assertTrue((decoded.discoveryState.chronicleProgress["chron_astral_arch"] ?: 0) >= 1)
            assertTrue(decoded.discoveryState.enemyKillCounts.isNotEmpty())
        } finally { db.close() }
    }
}
