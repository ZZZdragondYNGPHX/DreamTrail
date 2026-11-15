package com.dreamtrail.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dreamtrail.app.data.local.DreamTrailDatabase
import com.dreamtrail.app.data.repository.GameRepository
import com.dreamtrail.app.time.ClockProvider
import com.dreamtrail.core.engine.ActionOutcome
import com.dreamtrail.core.model.*
import com.dreamtrail.core.sleep.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeClock(
    var wallUtcMs: Long = 1_700_000_000_000L,
    var elapsedMs: Long? = 10_000L,
    var bootId: String? = "boot_1"
) : ClockProvider {
    override fun currentWallTimeUtcMs(): Long = wallUtcMs
    override fun elapsedRealtimeMs(): Long? = elapsedMs
    override fun currentBootId(): String? = bootId

    fun advanceSeconds(sec: Long) {
        wallUtcMs += sec * 1000L
        elapsedMs = elapsedMs?.plus(sec * 1000L)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameRepositoryAndRecoveryTest {

    private lateinit var context: Context
    private lateinit var db: DreamTrailDatabase
    private lateinit var clock: FakeClock
    private lateinit var repo: GameRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = DreamTrailDatabase.openInMemory(context)
        clock = FakeClock()
        val provider = object : SleepDataProvider {
            override val providerId = "health_connect"
            override suspend fun checkAvailability() = SleepProviderAvailability.READY
            override suspend fun currentConsentEpoch() = 1L
            override suspend fun readSleepRecords(fromUtcMs: Long, toUtcMs: Long, expectedConsentEpoch: Long) = emptyList<SleepRecord>()
        }
        repo = GameRepository(db, clock, provider)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `vertical slice start force-stop recover end settle and camp upgrade`() = runTest {
        repo.ensureInitializedAndRecover()

        val start1 = repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY)
        assertTrue(start1 is ActionOutcome.Success)
        val session = (start1 as ActionOutcome.Success).value

        // Double tap start returns the same RUNNING session without creating duplicates
        val start2 = repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY)
        assertTrue(start2 is ActionOutcome.Success)
        assertEquals(session.sessionId, (start2 as ActionOutcome.Success).value.sessionId)

        // Simulate app closed overnight for 7.5 hours, then reopened (new Repository instance on same DB)
        clock.advanceSeconds(7 * 3600L + 1800L)
        val reopenedRepo = GameRepository(db, clock)
        val recoverRes = reopenedRepo.ensureInitializedAndRecover()
        assertTrue(recoverRes is ActionOutcome.Success)
        assertNull((recoverRes as ActionOutcome.Success).value) // endAt wasn't fixed yet, remains RUNNING

        val stateBeforeEnd = reopenedRepo.stateFlow.first { it.initialized }
        assertNotNull(stateBeforeEnd.runningSession)

        // End expedition -> atomic settlement
        val endRes = reopenedRepo.endAndSettleExpedition(session.sessionId)
        assertTrue(endRes is ActionOutcome.Success)
        val report = (endRes as ActionOutcome.Success).value
        assertEquals(90, report.completedNodes)
        assertTrue(report.baseGold > 0L)
        assertTrue(report.newEquipment.isNotEmpty())

        // Use earned resources to upgrade Workshop Lv.1 and equip new weapon
        val campRes = reopenedRepo.upgradeFacility(FacilityType.WORKSHOP)
        assertTrue(campRes is ActionOutcome.Success)
        val equipRes = reopenedRepo.equipItem(report.newEquipment.first().instanceId)
        assertTrue(equipRes is ActionOutcome.Success)

        val finalState = reopenedRepo.stateFlow.first { it.playerState.facilityLevel(FacilityType.WORKSHOP) == 1 }
        assertEquals(1, finalState.playerState.facilityLevel(FacilityType.WORKSHOP))
        assertNull(finalState.runningSession)
    }

    @Test
    fun `crash after fixing endAtUtcMs recovers exact settlement on next launch without double pay`() = runTest {
        repo.ensureInitializedAndRecover()
        val session = (repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY) as ActionOutcome.Success).value
        clock.advanceSeconds(8 * 3600L)

        // Persist endAtUtcMs only (simulates process kill before Phase 3 transaction commit)
        val fixedEndMs = repo.fixEndTimeOnlyForRecoveryTest(session.sessionId)
        assertEquals(session.startAtUtcMs + 8 * 3600_000L, fixedEndMs)

        // Advance clock another 2 hours before restart: recovery MUST still use fixedEndMs (8h), not 10h!
        clock.advanceSeconds(2 * 3600L)
        val freshRepoAfterRestart = GameRepository(db, clock)
        val recovery = freshRepoAfterRestart.ensureInitializedAndRecover()
        assertTrue(recovery is ActionOutcome.Success)
        val recoveredReport = (recovery as ActionOutcome.Success).value
        assertNotNull(recoveredReport)
        assertEquals(fixedEndMs, recoveredReport!!.endAtUtcMs)
        assertEquals(96, recoveredReport.completedNodes)

        val goldAfterRecovery = freshRepoAfterRestart.stateFlow.first { it.initialized }.playerState.resources.gold

        // Concurrent/repeated settlement attempts must NOT change balance or duplicate ledger
        val retries = listOf(
            async { freshRepoAfterRestart.endAndSettleExpedition(session.sessionId) },
            async { freshRepoAfterRestart.endAndSettleExpedition(session.sessionId) },
            async { freshRepoAfterRestart.endAndSettleExpedition(session.sessionId) }
        ).awaitAll()
        for (r in retries) {
            assertTrue(r is ActionOutcome.Success)
            assertEquals(recoveredReport, (r as ActionOutcome.Success).value)
        }
        val goldAfterRetries = freshRepoAfterRestart.stateFlow.first { it.initialized }.playerState.resources.gold
        assertEquals(goldAfterRecovery, goldAfterRetries)
    }

    @Test
    fun `early cancel within 5 min grants zero rewards and does not roll back rngState`() = runTest {
        repo.ensureInitializedAndRecover()
        val initial = repo.stateFlow.first { it.initialized }
        val initialRng = initial.discoveryState.rngState
        val initialGold = initial.playerState.resources.gold

        val s1 = (repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY) as ActionOutcome.Success).value
        clock.advanceSeconds(120L)
        val cancelRes = repo.cancelExpeditionEarly(s1.sessionId)
        assertTrue(cancelRes is ActionOutcome.Success)

        val afterCancel = repo.stateFlow.first { it.runningSession == null }
        assertEquals(initialGold, afterCancel.playerState.resources.gold)
        assertNotEquals(initialRng, afterCancel.discoveryState.rngState)
    }

    @Test
    fun `late sleep record credits only positive blessing delta through reward ledger`() = runTest {
        repo.ensureInitializedAndRecover()
        val s = (repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY) as ActionOutcome.Success).value
        clock.advanceSeconds(7 * 3600L + 1800L)
        val report = (repo.endAndSettleExpedition(s.sessionId) as ActionOutcome.Success).value

        val goldBeforeTopUp = repo.stateFlow.first { it.initialized }.playerState.resources.gold
        val sleepRec = SleepRecord(
            provider = "health_connect",
            dataOrigin = "com.xiaomi.wearable",
            sourceRecordId = "sleep_1",
            sourceRevision = "r1",
            startAtUtcMs = s.startAtUtcMs,
            endAtUtcMs = report.endAtUtcMs,
            asleepSeconds = 7 * 3600L,
            deepSeconds = 45 * 60L,
            remSeconds = 60 * 60L,
            observedAtUtcMs = report.endAtUtcMs + 60_000L,
            consentEpoch = 1L
        )

        assertTrue(repo.updateSleepConsent(true, 1) is ActionOutcome.Success)
        val topUpRes = repo.applyLateSleepRecords(s.sessionId, listOf(sleepRec), expectedConsentEpoch = 1L)
        assertTrue(topUpRes is ActionOutcome.Success)
        val delta = (topUpRes as ActionOutcome.Success).value
        assertTrue(delta.deltaGold > 0L)
        assertTrue(delta.deltaXp > 0L)

        val goldAfterTopUp = repo.stateFlow.first { it.initialized }.playerState.resources.gold
        assertEquals(goldBeforeTopUp + delta.deltaGold, goldAfterTopUp)

        // Re-applying identical record grants zero additional delta
        val duplicateTopUp = repo.applyLateSleepRecords(s.sessionId, listOf(sleepRec), expectedConsentEpoch = 1L)
        assertEquals(0L, (duplicateTopUp as ActionOutcome.Success).value.deltaGold)
        assertEquals(goldAfterTopUp, repo.stateFlow.first { it.initialized }.playerState.resources.gold)
    }

    @Test
    fun `corrupt backup import fails cleanly without overwriting current save`() = runTest {
        repo.ensureInitializedAndRecover()
        val s = (repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY) as ActionOutcome.Success).value
        clock.advanceSeconds(3600L)
        repo.endAndSettleExpedition(s.sessionId)

        val validBackup = (repo.exportBackupJson() as ActionOutcome.Success).value
        val curGold = repo.stateFlow.first { it.initialized }.playerState.resources.gold

        // Corrupt the exported JSON
        val badImport = repo.importBackupJson(validBackup.replace("gold", "g0ld"))
        assertTrue(badImport is ActionOutcome.Failure)
        assertEquals(curGold, repo.stateFlow.first { it.initialized }.playerState.resources.gold)

        // Valid import restores cleanly
        val okImport = repo.importBackupJson(validBackup)
        assertTrue(okImport is ActionOutcome.Success)
        assertEquals(curGold, repo.stateFlow.first { it.initialized }.playerState.resources.gold)
    }
}
