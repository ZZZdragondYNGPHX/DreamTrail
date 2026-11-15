package com.dreamtrail.app

import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.dreamtrail.app.data.local.DreamTrailDatabase
import com.dreamtrail.app.data.repository.GameRepository
import com.dreamtrail.app.time.ClockProvider
import com.dreamtrail.app.ui.BackupAction
import com.dreamtrail.app.ui.DreamTrailViewModel
import com.dreamtrail.app.ui.RetryActionKind
import com.dreamtrail.core.engine.ActionOutcome
import com.dreamtrail.core.model.ExpeditionStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupViewModelTest {
    private class Clock : ClockProvider {
        override fun currentWallTimeUtcMs() = 1_700_000_000_000L
        override fun elapsedRealtimeMs() = 10_000L
        override fun currentBootId() = "test_boot"
    }

    private suspend fun await(condition: () -> Boolean) = withTimeout(10_000) {
        while (!condition()) delay(10)
    }

    private fun fixture(block: suspend (DreamTrailViewModel, GameRepository) -> Unit) = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val db = DreamTrailDatabase.openInMemory(ApplicationProvider.getApplicationContext())
        val store = ViewModelStore()
        try {
            val repository = GameRepository(db, Clock())
            repository.ensureInitializedAndRecover()
            val vm = DreamTrailViewModel(repository, Clock())
            store.put("backup", vm)
            await { vm.dataState.value.initialized }
            block(vm, repository)
        } finally {
            store.clear()
            db.close()
            Dispatchers.resetMain()
        }
    }

    @Test fun `import failure preserves selection and valid restore uses saved strategy immediately`() = fixture { vm, repo ->
        repo.startExpedition("route_mistwood_outer", ExpeditionStrategy.EXPLORE)
        val backup = (repo.exportBackupJson() as ActionOutcome.Success).value
        vm.selectStrategy(ExpeditionStrategy.GATHER)
        var finished: Boolean? = null
        vm.importBackup("invalid") { finished = it }
        await { finished != null && vm.uiState.value.backupAction == null }
        assertFalse(finished!!)
        assertEquals(ExpeditionStrategy.GATHER, vm.uiState.value.selectedStrategy)
        assertNotNull(vm.uiState.value.errorMessage)
        finished = null
        vm.importBackup(backup) { finished = it }
        await { finished != null && vm.uiState.value.backupAction == null }
        assertTrue(finished!!)
        assertEquals(ExpeditionStrategy.EXPLORE, vm.uiState.value.selectedStrategy)
        assertNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.historyDetailReport)
    }

    @Test fun `busy file import ignores repeated actions and releases state after read failure`() = fixture { vm, _ ->
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        try {
            vm.importBackupFile {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                ByteArrayInputStream("invalid".toByteArray())
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            assertEquals(BackupAction.IMPORT, vm.uiState.value.backupAction)
            var duplicateOpened = false
            vm.exportBackupFile { duplicateOpened = true; ByteArrayOutputStream() }
            vm.copyBackup { duplicateOpened = true }
            assertFalse(duplicateOpened)
            release.countDown()
            await { vm.uiState.value.backupAction == null }
            assertNotNull(vm.uiState.value.errorMessage)
            val output = ByteArrayOutputStream()
            vm.exportBackupFile { output }
            await { vm.uiState.value.backupAction == null }
            assertTrue(output.size() > 0)
            assertNull(vm.uiState.value.errorMessage)
        } finally {
            release.countDown()
        }
    }

    @Test
    fun `sleep guard tracks distractions blackout toggle wake confirmation and resets on settle`() = fixture { vm, _ ->
        vm.startExpedition()
        await { vm.dataState.value.runningSession != null && !vm.uiState.value.isSubmittingDeparture }

        vm.setSleepBlackoutMode(true)
        assertTrue(vm.uiState.value.sleepBlackoutMode)

        vm.onUserLeftWithScreenOn()
        assertEquals(1, vm.uiState.value.phoneDistractionCount)
        assertFalse(vm.uiState.value.sleepBlackoutMode)
        assertNotNull(vm.uiState.value.sleepGuardWarning)

        vm.setShowWakeConfirm(true)
        assertTrue(vm.uiState.value.showWakeConfirm)

        val sessionId = vm.dataState.value.runningSession!!.sessionId
        vm.endExpedition(sessionId)
        await { vm.dataState.value.runningSession == null && !vm.uiState.value.isSettling }

        assertEquals(0, vm.uiState.value.phoneDistractionCount)
        assertFalse(vm.uiState.value.sleepBlackoutMode)
        assertFalse(vm.uiState.value.showWakeConfirm)
        assertNull(vm.uiState.value.sleepGuardWarning)
        assertNotNull(vm.uiState.value.latestReport)
    }

    @Test fun `failed cancellation retries cancellation without settling or awarding rewards`() = fixture { vm, _ ->
        vm.startExpedition()
        await { vm.dataState.value.runningSession != null && !vm.uiState.value.isSubmittingDeparture }
        val resources = vm.dataState.value.playerState.resources
        vm.cancelExpeditionEarly("missing_session")
        await { !vm.uiState.value.isSettling && vm.uiState.value.errorMessage != null }
        assertEquals(RetryActionKind.CANCEL_EXPEDITION, vm.uiState.value.retryKind)
        vm.retryFailedAction()
        await { vm.dataState.value.runningSession == null && !vm.uiState.value.isSettling }
        assertNull(vm.uiState.value.latestReport)
        assertEquals(resources, vm.dataState.value.playerState.resources)
        assertNull(vm.uiState.value.retryKind)
    }

    @Test fun `restoring a running journey clears guard state from the replaced journey`() = fixture { vm, repo ->
        vm.startExpedition()
        await { vm.dataState.value.runningSession != null && !vm.uiState.value.isSubmittingDeparture }
        val backup = (repo.exportBackupJson() as ActionOutcome.Success).value
        vm.onUserLeftWithScreenOn()
        vm.setSleepBlackoutMode(true)
        vm.setShowWakeConfirm(true)
        var done = false
        vm.importBackup(backup) { done = it }
        await { done && vm.uiState.value.backupAction == null }
        assertEquals(0, vm.uiState.value.phoneDistractionCount)
        assertNull(vm.uiState.value.sleepGuardWarning)
        assertFalse(vm.uiState.value.sleepBlackoutMode)
        assertFalse(vm.uiState.value.showWakeConfirm)
    }

    @Test fun `loadout saves the currently selected strategy before the next departure`() = fixture { vm, _ ->
        vm.selectStrategy(ExpeditionStrategy.EXPLORE)
        vm.saveLoadout(0)
        await { vm.dataState.value.playerState.loadouts.isNotEmpty() }
        assertEquals(ExpeditionStrategy.EXPLORE, vm.dataState.value.playerState.loadouts.single().strategy)
        vm.selectStrategy(ExpeditionStrategy.GATHER)
        vm.applyLoadout(0)
        await { vm.uiState.value.selectedStrategy == ExpeditionStrategy.EXPLORE && vm.dataState.value.playerState.lastStrategy == ExpeditionStrategy.EXPLORE }
        assertEquals(ExpeditionStrategy.EXPLORE, vm.dataState.value.playerState.lastStrategy)
    }
}
