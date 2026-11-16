package com.dreamtrail.core

import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.*
import com.dreamtrail.core.model.*
import com.dreamtrail.core.save.SaveBackupCodec
import com.dreamtrail.core.save.SaveBackupPayload
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class SolunaMechanicsRegressionTest {
    private fun bundle(player: PlayerState = PlayerState()) = GameStateBundle(
        player, RewardBudgetState(), DiscoveryState(onboardingCompleted = true), emptyList())

    private fun session(b: GameStateBundle, mode: SessionMode = SessionMode.FOCUS_FLOW, startSec: Long = 1_700_000_000L) = ExpeditionSession(
        sessionId = "soluna_$startSec", status = SessionStatus.RUNNING, routeId = "route_mistwood_outer",
        strategy = ExpeditionStrategy.STEADY, startAtUtcMs = startSec * 1000L, sessionSeed = 123L,
        contentVersion = ContentCatalog.CONTENT_VERSION, rulesVersion = ContentCatalog.RULES_VERSION,
        snapshot = ProgressionEngine.createDepartureSnapshot(b.playerState, b.inventory, discoveryState = b.discoveryState),
        sessionMode = mode, focusTagId = if (mode.isFocus) "tag_study" else null)

    private fun settle(b: GameStateBundle, s: ExpeditionSession, seconds: Long) =
        ExpeditionSimulator.simulateSettlement(s, s.startAtUtcMs + seconds * 1000L, seconds, null, b)

    @Test fun `long pomodoro counts completed targets and cannot finish a task early`() {
        for (minutes in listOf(15, 25, 45, 60)) {
            val b = bundle(PlayerState(focusTasks = listOf(FocusTaskItem("task", "Study", targetPomodoros = 2))))
            val s = session(b, SessionMode.FOCUS_POMODORO).copy(targetDurationSeconds = minutes * 60L, linkedTaskId = "task")
            val early = settle(b, s, minutes * 60L - 1)
            assertEquals(0, early.updatedBundle.playerState.focusTasks.single().completedPomodoros)
            val done = settle(b, s, minutes * 60L)
            assertEquals(1, done.updatedBundle.playerState.focusTasks.single().completedPomodoros)
            assertFalse(done.settlement.taskCompletedNow)
        }
    }

    @Test fun `vigour affects only remaining seconds and preserves resource fractions`() {
        val plain = bundle()
        val buffed = bundle(PlayerState(focusVigourBuff = FocusVigourBuff("Rested", 30L)))
        val a = settle(plain, session(plain), 180L)
        val b = settle(buffed, session(buffed), 180L)
        val rate = ContentCatalog.routeById("route_mistwood_outer")!!.baseGoldPerHour
        val numerator = rate * 100L * (180L * 100L + 30L * 10L)
        assertEquals(numerator / ResourceAccrual.RATE_DENOMINATOR, b.settlement.baseGold)
        assertEquals(numerator % ResourceAccrual.RATE_DENOMINATOR, b.updatedBundle.budgetState.resourceRemainders["gold"])
        assertEquals(a.settlement.inspirationInkGained, b.settlement.inspirationInkGained)
        assertNull(b.updatedBundle.playerState.focusVigourBuff)
    }

    @Test fun `night preparation has the same yield across short splits and reports actual capped gain`() {
        val start = 1_700_000_000L
        val initial = bundle()
        val one = settle(initial, session(initial), 120L)
        var split = initial
        repeat(4) { index -> split = settle(split, session(split, startSec = start + index * 30L), 30L).updatedBundle }
        assertEquals(2, one.settlement.nightPrepGained)
        assertEquals(one.updatedBundle.playerState.nightPrepCharge, split.playerState.nightPrepCharge)
        val full = bundle(PlayerState(nightPrepCharge = 99))
        assertEquals(1, settle(full, session(full), 300L).settlement.nightPrepGained)
    }

    @Test fun `focus battle cannot consume or receive the night preparation shield`() {
        val a = bundle()
        val b = bundle(PlayerState(nightPrepCharge = 100))
        val plain = settle(a, session(a), 3600L)
        val prepared = settle(b, session(b), 3600L)
        assertEquals(plain.settlement.baseGold, prepared.settlement.baseGold)
        assertEquals(plain.settlement.keyEvents, prepared.settlement.keyEvents)
        assertEquals(100, prepared.updatedBundle.playerState.nightPrepCharge)
        assertEquals(0, prepared.settlement.nightPrepTierUsed)
    }

    @Test fun `zero and tiny focus settlements do not unlock companions by session spam`() {
        var b = bundle()
        repeat(12) { index -> b = settle(b, session(b, startSec = 1_700_000_000L + index), 1L).updatedBundle }
        assertEquals(0, b.discoveryState.totalFocusSessionsCompleted)
        assertFalse("comp_wen" in b.discoveryState.companionChapters)
        assertFalse("passive_soluna_breath" in b.discoveryState.unlockedPassiveIds)
        assertEquals(1, settle(b, session(b, startSec = 1_700_001_000L), 300L).updatedBundle.discoveryState.totalFocusSessionsCompleted)
    }

    @Test fun `daily credit is split at local midnight using only newly credited intervals`() {
        val start = ZonedDateTime.parse("2026-11-15T23:50:00-08:00[America/Los_Angeles]").toEpochSecond()
        val b = bundle().copy(budgetState = RewardBudgetState(focusCreditedIntervals = listOf(SecondInterval(start, start + 300))))
        val s = session(b, startSec = start).copy(zoneId = "America/Los_Angeles")
        val out = settle(b, s, 1800L)
        assertEquals(1500L, out.settlement.effectiveCreditedSeconds)
        assertEquals(mapOf("2026-11-15" to 300L, "2026-11-16" to 1200L), out.updatedBundle.discoveryState.dailyFocusSecondsByDate)
    }

    @Test fun `daily credit survives daylight saving clock repetition`() {
        val start = ZonedDateTime.parse("2026-11-01T00:30:00-07:00[America/Los_Angeles]").toEpochSecond()
        val b = bundle()
        val out = settle(b, session(b, startSec = start).copy(zoneId = "America/Los_Angeles"), 10800L)
        assertEquals(mapOf("2026-11-01" to 10800L), out.updatedBundle.discoveryState.dailyFocusSecondsByDate)
    }

    @Test fun `malformed new save counters and references are rejected before simulation`() {
        val b = bundle()
        val valid = SaveBackupPayload(exportedAtUtcMs = 1, contentVersion = ContentCatalog.CONTENT_VERSION,
            rulesVersion = ContentCatalog.RULES_VERSION, playerState = b.playerState,
            budgetState = b.budgetState, discoveryState = b.discoveryState, inventory = emptyList())
        assertNull(SaveBackupCodec.validatePayload(valid))
        assertNotNull(SaveBackupCodec.validatePayload(valid.copy(budgetState = b.budgetState.copy(spiritProgressSeconds = Int.MAX_VALUE))))
        assertNotNull(SaveBackupCodec.validatePayload(valid.copy(budgetState = b.budgetState.copy(focusCreditedIntervals = listOf(SecondInterval(10, 20), SecondInterval(15, 25))))))
        assertNotNull(SaveBackupCodec.validatePayload(valid.copy(playerState = PlayerState(focusTasks = listOf(FocusTaskItem("bad", "Task", completedPomodoros = 9))))))
        assertNotNull(SaveBackupCodec.validatePayload(valid.copy(activeSession = session(b).copy(zoneId = "Invalid/Zone"))))
        assertNotNull(SaveBackupCodec.validatePayload(valid.copy(activeSession = session(b).copy(
            dayPhenomenon = ContentCatalog.dayPhenomena.first().copy(dropBonusPercent = Int.MAX_VALUE)))))
        assertTrue(ProgressionEngine.addFocusTask(b, "Task", "missing_tag", 1, 0, "task") is ActionOutcome.Failure)
    }

    @Test fun `clearing completed tasks frees capacity while preserving rewards and statistics`() {
        val tasks = (0 until 20).map { FocusTaskItem("done_$it", "Task $it", completedPomodoros = 1, completed = true) }
        val full = bundle(PlayerState(focusTasks = tasks, resources = ResourceBag(gold = 500)))
            .copy(discoveryState = DiscoveryState(completedTasksCount = 20))
        assertTrue(ProgressionEngine.addFocusTask(full, "New", "tag_study", 1, 0, "new") is ActionOutcome.Failure)
        val cleared = (ProgressionEngine.clearCompletedFocusTasks(full) as ActionOutcome.Success).value
        assertEquals(full.discoveryState, cleared.discoveryState)
        assertEquals(full.playerState.resources, cleared.playerState.resources)
        assertTrue(ProgressionEngine.addFocusTask(cleared, "New", "tag_study", 1, 0, "new") is ActionOutcome.Success)
    }

    @Test fun `focus progression unlocks both advanced regions in one reconciliation`() {
        val discovery = DiscoveryState(totalCreditedSeconds = 48 * 3600L,
            totalFocusCreditedSeconds = 48 * 3600L, onboardingCompleted = true)
        val unlocked = ProgressionEngine.reconcileUnlocks(PlayerState(level = 42), discovery)
        assertTrue("route_athenaeum_cloister" in unlocked.unlockedRouteIds)
        assertTrue("route_spire_steps" in unlocked.unlockedRouteIds)
    }
}
