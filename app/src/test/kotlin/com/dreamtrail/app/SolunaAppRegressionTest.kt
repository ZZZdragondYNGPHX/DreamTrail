package com.dreamtrail.app

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.dreamtrail.app.data.local.DreamTrailDatabase
import com.dreamtrail.app.data.repository.GameRepository
import com.dreamtrail.app.time.ClockProvider
import com.dreamtrail.app.ui.DreamTrailViewModel
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.ActionOutcome
import com.dreamtrail.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SolunaAppRegressionTest {

    private class MutableClock(
        var wallUtcMs: Long = 1_700_000_000_000L,
        var elapsedMs: Long? = 50_000L,
        var bootId: String? = "boot_v5"
    ) : ClockProvider {
        override fun currentWallTimeUtcMs(): Long = wallUtcMs
        override fun elapsedRealtimeMs(): Long? = elapsedMs
        override fun currentBootId(): String? = bootId

        fun advanceSeconds(sec: Long) {
            wallUtcMs += sec * 1000L
            elapsedMs = elapsedMs?.plus(sec * 1000L)
        }
    }

    @Test
    fun `repository and viewmodel support focus pomodoro tasks tags spirits mood and inclusive leave`() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context: Context = ApplicationProvider.getApplicationContext()
        val db = DreamTrailDatabase.openInMemory(context)
        val clock = MutableClock()
        val store = ViewModelStore()
        try {
            val repo = GameRepository(db, clock)
            repo.ensureInitializedAndRecover()
            val vm = DreamTrailViewModel(repo, clock)
            store.put("soluna", vm)
            withTimeout(10_000) { while (!vm.dataState.value.initialized) delay(10) }

            vm.selectMode(SessionMode.FOCUS_POMODORO)
            vm.selectPomodoroMinutes(25)
            vm.addCustomFocusTag("算法整理", BuildArchetype.GALE)
            withTimeout(10_000) {
                while (vm.dataState.value.playerState.customFocusTags.isEmpty()) delay(10)
            }
            val customTagId = vm.dataState.value.playerState.customFocusTags.first().id

            vm.addFocusTask("完成图论专题复盘", customTagId, targetPomodoros = 1)
            withTimeout(10_000) {
                while (vm.dataState.value.playerState.focusTasks.isEmpty()) delay(10)
            }
            val taskId = vm.dataState.value.playerState.focusTasks.first().id

            vm.startExpedition()
            withTimeout(10_000) {
                while (vm.dataState.value.runningSession == null) delay(10)
            }
            val sess = vm.dataState.value.runningSession!!
            assertEquals(SessionMode.FOCUS_POMODORO, sess.sessionMode)
            assertEquals(1500L, sess.targetDurationSeconds)
            assertEquals(taskId, sess.linkedTaskId)
            assertNotNull(sess.dayPhenomenon)

            // Inclusive leave: gentle copy and non-destructive
            vm.onUserLeftWithScreenOn()
            assertEquals(1, vm.uiState.value.phoneDistractionCount)
            assertTrue(vm.uiState.value.sleepGuardWarning?.contains("保留全部基础收益") == true)

            clock.advanceSeconds(1500L) // 25 minutes
            vm.endExpedition(sess.sessionId)
            withTimeout(10_000) {
                while (vm.uiState.value.latestReport == null) delay(10)
            }
            val report = vm.uiState.value.latestReport!!
            assertEquals(SessionMode.FOCUS_POMODORO, report.sessionMode)
            assertTrue(report.inspirationInkGained > 0L)
            assertTrue(report.taskCompletedNow)
            assertEquals("完成图论专题复盘", report.linkedTaskTitle)
            assertTrue(report.nightPrepGained > 0)
            assertNotNull(report.pendingTeaEchoTitle)

            // Mood reflection + Break timer + Tea Echo resolution
            vm.saveReflectionMood(report.sessionId, "状态充沛")
            withTimeout(10_000) {
                while (vm.uiState.value.latestReport?.reflectionMood != "状态充沛") delay(10)
            }
            vm.startBreakTimer(5)
            assertEquals(300, vm.uiState.value.breakRemainingSeconds)
            assertNull(vm.uiState.value.latestReport)
            vm.skipBreakTimer()
            assertEquals(0, vm.uiState.value.breakRemainingSeconds)

            vm.resolveTeaEcho(chooseOptionA = true)
            withTimeout(10_000) {
                while (vm.dataState.value.playerState.pendingTeaEcho != null) delay(10)
            }
        } finally {
            store.clear()
            db.close()
            Dispatchers.resetMain()
        }
    }
    @Test
    fun `current Room schema preserves focus snapshot and settings after reopening`() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val name = "soluna_current_${System.nanoTime()}.db"
        val clock = MutableClock()
        var db = DreamTrailDatabase.open(context, name)
        try {
            val repo = GameRepository(db, clock)
            repo.ensureInitializedAndRecover()
            repo.updateSettings { it.copy(themeMode = "DAY_PARCHMENT", defaultPomodoroMinutes = 45) }
            val started = repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY,
                SessionMode.FOCUS_POMODORO, 2700L, "tag_code") as ActionOutcome.Success
            repo.recordRunningSessionFocusStats(db.dao().getRunningSession()!!.sessionId, 3, 120L)
            db.close()
            db = DreamTrailDatabase.open(context, name)
            val reopened = GameRepository(db, clock)
            val state = reopened.stateFlow.first { it.initialized }
            assertEquals("DAY_PARCHMENT", state.settings.themeMode)
            assertEquals(45, state.settings.defaultPomodoroMinutes)
            assertEquals(started.value.zoneId, state.runningSession!!.zoneId)
            assertEquals(3, state.runningSession!!.distractionCount)
            clock.advanceSeconds(2700)
            assertTrue(reopened.endAndSettleExpedition(started.value.sessionId) is ActionOutcome.Success)
            val backup = (reopened.exportBackupJson() as ActionOutcome.Success).value
            assertTrue(reopened.importBackupJson(backup) is ActionOutcome.Success)
            assertEquals(3, reopened.stateFlow.first { it.initialized }.recentSettlements.first().distractionCount)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun `viewmodel restores distraction count and break timer catches up after background`() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val db = DreamTrailDatabase.openInMemory(ApplicationProvider.getApplicationContext())
        val clock = MutableClock()
        val store = ViewModelStore()
        try {
            val repo = GameRepository(db, clock)
            repo.ensureInitializedAndRecover()
            assertTrue(repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.STEADY,
                SessionMode.FOCUS_FLOW) is ActionOutcome.Success)
            repo.recordRunningSessionFocusStats(db.dao().getRunningSession()!!.sessionId, 3, 120L)
            val vm = DreamTrailViewModel(repo, clock)
            store.put("restored", vm)
            withTimeout(10_000) { while (vm.uiState.value.phoneDistractionCount != 3) delay(10) }
            vm.onUserLeftWithScreenOn()
            assertEquals(4, vm.uiState.value.phoneDistractionCount)
            vm.startBreakTimer(5)
            vm.setForegroundActive(false)
            clock.advanceSeconds(301)
            vm.setForegroundActive(true)
            withTimeout(10_000) { while (vm.uiState.value.breakRemainingSeconds != 0) delay(10) }
            vm.setForegroundActive(false)
            assertTrue(vm.uiState.value.statusNotice!!.contains("茶歇休息时间到"))
        } finally {
            store.clear()
            db.close()
            Dispatchers.resetMain()
        }
    }

}
