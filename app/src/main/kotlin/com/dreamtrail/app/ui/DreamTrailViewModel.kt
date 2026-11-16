package com.dreamtrail.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dreamtrail.app.data.local.AppSettingsEntity
import com.dreamtrail.app.data.repository.AppUiDataState
import com.dreamtrail.app.data.repository.GameRepository
import com.dreamtrail.app.data.BackupFiles
import com.dreamtrail.app.time.ClockProvider
import com.dreamtrail.app.time.DurationResolver
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.ActionOutcome
import com.dreamtrail.core.engine.ExpeditionSimulator
import com.dreamtrail.core.engine.ProgressionEngine
import com.dreamtrail.core.engine.RewardBudgetCalculator
import com.dreamtrail.core.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab(val label: String) {
    JOURNEY("旅程"),
    CHARACTER("角色"),
    COMPANIONS("伙伴"),
    CAMP("营地"),
    COMPENDIUM("图鉴")
}

data class JourneyUiState(
    val selectedRouteId: String = "route_mistwood_outer",
    val selectedStrategy: ExpeditionStrategy = ExpeditionStrategy.STEADY,
    val selectedMode: SessionMode = SessionMode.SLEEP,
    val selectedPomodoroMinutes: Int = 25,
    val selectedNapMinutes: Int = 30,
    val isSubmittingDeparture: Boolean = false,
    val isSettling: Boolean = false,
    val isPerformingAction: Boolean = false,
    val activePreview: ExpeditionPreview? = null,
    val latestReport: SettlementResult? = null,
    val historyDetailReport: SettlementResult? = null,
    val errorMessage: String? = null,
    val retryKind: RetryActionKind? = null,
    val statusNotice: String? = null,
    val backupAction: BackupAction? = null,
    val sleepBlackoutMode: Boolean = false,
    val showWakeConfirm: Boolean = false,
    val phoneDistractionCount: Int = 0,
    val sleepGuardWarning: String? = null,
    val isScreenPinned: Boolean = false,
    val nightPhenomenon: NightPhenomenon = ContentCatalog.nightPhenomenonForUtcMs(0L),
    val dayPhenomenon: DayPhenomenon = ContentCatalog.dayPhenomenonForUtcMs(0L),
    val remainingFocusBudgetSeconds: Long = RewardBudgetCalculator.ROLLING_CREDIT_CAP_SECONDS,
    val isSoundscapeSheetOpen: Boolean = false,
    val isBreathingDialogOpen: Boolean = false,
    val isStatsSheetOpen: Boolean = false,
    val breakRemainingSeconds: Int = 0,
    val isFaceDownFlipActive: Boolean = false,
    val targetCountdownFinished: Boolean = false
)

enum class BackupAction(val label: String) {
    SAVE_FILE("正在保存存档…"),
    COPY_TEXT("正在复制存档…"),
    IMPORT("正在恢复存档…"),
    RESTORE("正在恢复快照…")
}

enum class RetryActionKind {
    START_EXPEDITION,
    END_EXPEDITION,
    CANCEL_EXPEDITION
}

class DreamTrailViewModel(
    private val repository: GameRepository,
    private val clock: ClockProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(JourneyUiState())
    val uiState: StateFlow<JourneyUiState> = _uiState.asStateFlow()

    val dataState: StateFlow<AppUiDataState> = repository.stateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AppUiDataState()
        )

    private var previewTickerJob: Job? = null
    private var syncedSelectionFromSave = false
    private var flipFocusAccumulatedSeconds = 0L
    private var lastFlipSyncSeconds = 0L
    private var observedSessionId: String? = null
    private var breakDeadlineMs: Long? = null

    private fun timerNowMs(): Long = clock.elapsedRealtimeMs() ?: clock.currentWallTimeUtcMs()

    init {
        val nowMs = clock.currentWallTimeUtcMs()
        _uiState.update {
            it.copy(
                nightPhenomenon = ContentCatalog.nightPhenomenonForUtcMs(nowMs),
                dayPhenomenon = ContentCatalog.dayPhenomenonForUtcMs(nowMs)
            )
        }
        viewModelScope.launch {
            when (val res = repository.ensureInitializedAndRecover()) {
                is ActionOutcome.Success -> {
                    val recoveredSettlement = res.value
                    if (recoveredSettlement != null) {
                        _uiState.update {
                            it.copy(
                                latestReport = recoveredSettlement,
                                statusNotice = "已恢复并完成上次未结束的结算，奖励已安全入账。"
                            )
                        }
                    }
                }
                is ActionOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = res.reason,
                            retryKind = RetryActionKind.END_EXPEDITION
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            dataState.collect { data ->
                if (data.initialized && !syncedSelectionFromSave) {
                    syncedSelectionFromSave = true
                    _uiState.update {
                        it.copy(
                            selectedRouteId = data.playerState.lastRouteId,
                            selectedStrategy = data.playerState.lastStrategy,
                            selectedPomodoroMinutes = data.settings.defaultPomodoroMinutes.coerceIn(5, 180),
                            selectedNapMinutes = data.settings.defaultNapMinutes.coerceIn(5, 120)
                        )
                    }
                }
                val running = data.runningSession
                val nowSec = clock.currentWallTimeUtcMs() / 1000L
                val focusBudgetLeft = RewardBudgetCalculator.remainingFocusBudgetSeconds(data.budgetState, nowSec)
                if (running != null) {
                    if (observedSessionId != running.sessionId) {
                        observedSessionId = running.sessionId
                        flipFocusAccumulatedSeconds = running.flipFocusSeconds
                        lastFlipSyncSeconds = running.flipFocusSeconds
                        _uiState.update { it.copy(phoneDistractionCount = running.distractionCount, isFaceDownFlipActive = false) }
                    }
                    if (running.flipFocusSeconds > flipFocusAccumulatedSeconds) {
                        flipFocusAccumulatedSeconds = running.flipFocusSeconds
                    }
                    refreshPreviewNow(running, data.budgetState)
                } else {
                    observedSessionId = null
                    flipFocusAccumulatedSeconds = 0L
                    lastFlipSyncSeconds = 0L
                    _uiState.update {
                        it.copy(
                            activePreview = null,
                            sleepBlackoutMode = false,
                            showWakeConfirm = false,
                            phoneDistractionCount = 0,
                            sleepGuardWarning = null,
                            isScreenPinned = false,
                            remainingFocusBudgetSeconds = focusBudgetLeft,
                            targetCountdownFinished = false,
                            isFaceDownFlipActive = false
                        )
                    }
                }
            }
        }
    }

    fun setForegroundActive(active: Boolean) {
        previewTickerJob?.cancel()
        if (!active) return
        val nowMs = clock.currentWallTimeUtcMs()
        _uiState.update {
            it.copy(
                nightPhenomenon = ContentCatalog.nightPhenomenonForUtcMs(nowMs),
                dayPhenomenon = ContentCatalog.dayPhenomenonForUtcMs(nowMs)
            )
        }
        previewTickerJob = viewModelScope.launch {
            var tickCounter = 0
            while (isActive) {
                val snap = dataState.value
                val running = snap.runningSession
                if (running != null) {
                    if (_uiState.value.isFaceDownFlipActive && running.sessionMode.isFocus) {
                        flipFocusAccumulatedSeconds++
                        if (flipFocusAccumulatedSeconds - lastFlipSyncSeconds >= 10L) {
                            lastFlipSyncSeconds = flipFocusAccumulatedSeconds
                            val distractions = maxOf(running.distractionCount, _uiState.value.phoneDistractionCount)
                            repository.recordRunningSessionFocusStats(running.sessionId, distractions, flipFocusAccumulatedSeconds)
                        }
                    }
                    refreshPreviewNow(running, snap.budgetState)
                } else if (tickCounter % 15 == 0) {
                    val sec = clock.currentWallTimeUtcMs() / 1000L
                    val focusLeft = RewardBudgetCalculator.remainingFocusBudgetSeconds(snap.budgetState, sec)
                    _uiState.update { it.copy(remainingFocusBudgetSeconds = focusLeft) }
                }

                val brk = _uiState.value.breakRemainingSeconds
                if (brk > 0) {
                    val nextBrk = (((breakDeadlineMs ?: timerNowMs()) - timerNowMs()).coerceAtLeast(0L) + 999L)
                        .div(1000L).toInt()
                    if (nextBrk == 0) breakDeadlineMs = null
                    _uiState.update {
                        it.copy(
                            breakRemainingSeconds = nextBrk,
                            statusNotice = if (nextBrk == 0) "茶歇休息时间到，可以神清气爽地开启下一轮啦。" else it.statusNotice
                        )
                    }
                }
                tickCounter++
                delay(1_000L)
            }
        }
    }

    private fun refreshPreviewNow(session: ExpeditionSession, budget: RewardBudgetState) {
        val resolved = DurationResolver.resolveForPreview(session, clock)
        val preview = ExpeditionSimulator.previewProgress(session, resolved.durationSeconds, budget)
        val nowMs = clock.currentWallTimeUtcMs()
        val nightPhenom = ContentCatalog.nightPhenomenonForUtcMs(nowMs)
        val dayPhenom = ContentCatalog.dayPhenomenonForUtcMs(nowMs)
        val focusLeft = RewardBudgetCalculator.remainingFocusBudgetSeconds(budget, nowMs / 1000L)
        val targetSec = session.targetDurationSeconds
        val finished = targetSec != null && resolved.durationSeconds >= targetSec
        _uiState.update {
            it.copy(
                activePreview = preview,
                nightPhenomenon = nightPhenom,
                dayPhenomenon = dayPhenom,
                remainingFocusBudgetSeconds = focusLeft,
                targetCountdownFinished = finished
            )
        }
    }

    fun selectRoute(routeId: String) {
        _uiState.update { it.copy(selectedRouteId = routeId, errorMessage = null) }
    }

    fun selectStrategy(strategy: ExpeditionStrategy) {
        _uiState.update { it.copy(selectedStrategy = strategy, errorMessage = null) }
    }

    fun selectMode(mode: SessionMode) {
        _uiState.update { it.copy(selectedMode = mode, errorMessage = null) }
    }

    fun selectPomodoroMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(5, 180)
        _uiState.update { it.copy(selectedPomodoroMinutes = clamped) }
        updateSettings { it.copy(defaultPomodoroMinutes = clamped) }
    }

    fun selectNapMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(5, 120)
        _uiState.update { it.copy(selectedNapMinutes = clamped) }
        updateSettings { it.copy(defaultNapMinutes = clamped) }
    }

    fun startExpedition() {
        val cur = _uiState.value
        if (cur.isSubmittingDeparture || cur.isSettling) return
        val targetSeconds = when (cur.selectedMode) {
            SessionMode.FOCUS_POMODORO -> cur.selectedPomodoroMinutes * 60L
            SessionMode.POWER_NAP -> cur.selectedNapMinutes * 60L
            SessionMode.SLEEP, SessionMode.FOCUS_FLOW -> null
        }
        val player = dataState.value.playerState
        breakDeadlineMs = null
        _uiState.update {
            it.copy(
                isSubmittingDeparture = true,
                errorMessage = null,
                retryKind = null,
                latestReport = null,
                targetCountdownFinished = false,
                breakRemainingSeconds = 0
            )
        }
        viewModelScope.launch {
            when (
                val res = repository.startExpedition(
                    routeId = cur.selectedRouteId,
                    strategy = cur.selectedStrategy,
                    sessionMode = cur.selectedMode,
                    targetDurationSeconds = targetSeconds,
                    focusTagId = if (cur.selectedMode.isFocus) player.selectedFocusTagId else null,
                    linkedTaskId = if (cur.selectedMode.isFocus) player.activeFocusTaskId else null
                )
            ) {
                is ActionOutcome.Success -> {
                    flipFocusAccumulatedSeconds = 0L
                    lastFlipSyncSeconds = 0L
                    _uiState.update {
                        it.copy(
                            isSubmittingDeparture = false,
                            statusNotice = if (cur.selectedMode.isFocus) {
                                "已进入${cur.selectedMode.displayName}专注陪伴模式。"
                            } else {
                                "已确认出发，已开启安眠守夜占屏模式。"
                            },
                            sleepBlackoutMode = false,
                            showWakeConfirm = false,
                            phoneDistractionCount = 0,
                            sleepGuardWarning = null,
                            isScreenPinned = false,
                            targetCountdownFinished = false
                        )
                    }
                }
                is ActionOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingDeparture = false,
                            errorMessage = res.reason,
                            retryKind = RetryActionKind.START_EXPEDITION
                        )
                    }
                }
            }
        }
    }

    fun cancelExpeditionEarly(sessionId: String) {
        val cur = _uiState.value
        if (cur.isSubmittingDeparture || cur.isSettling) return
        _uiState.update { it.copy(isSettling = true, errorMessage = null, retryKind = null, statusNotice = null) }
        viewModelScope.launch {
            when (val res = repository.cancelExpeditionEarly(sessionId)) {
                is ActionOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            statusNotice = "已撤销本次出发，可以重新调整路线。",
                            isSettling = false,
                            sleepBlackoutMode = false,
                            showWakeConfirm = false,
                            phoneDistractionCount = 0,
                            sleepGuardWarning = null,
                            isScreenPinned = false
                        )
                    }
                }
                is ActionOutcome.Failure -> {
                    _uiState.update { it.copy(isSettling = false, errorMessage = res.reason, retryKind = RetryActionKind.CANCEL_EXPEDITION) }
                }
            }
        }
    }

    fun endExpedition(sessionId: String) {
        val cur = _uiState.value
        if (cur.isSettling || cur.isSubmittingDeparture) return
        _uiState.update { it.copy(isSettling = true, errorMessage = null, retryKind = null, statusNotice = null) }
        viewModelScope.launch {
            val running = dataState.value.runningSession
            if (running != null && (flipFocusAccumulatedSeconds > running.flipFocusSeconds || cur.phoneDistractionCount > running.distractionCount)) {
                repository.recordRunningSessionFocusStats(
                    sessionId = running.sessionId,
                    distractionCount = maxOf(running.distractionCount, cur.phoneDistractionCount),
                    flipFocusSeconds = maxOf(running.flipFocusSeconds, flipFocusAccumulatedSeconds)
                )
            }
            when (val res = repository.endAndSettleExpedition(sessionId)) {
                is ActionOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            isSettling = false,
                            latestReport = res.value,
                            errorMessage = null,
                            retryKind = null,
                            sleepBlackoutMode = false,
                            showWakeConfirm = false,
                            phoneDistractionCount = 0,
                            sleepGuardWarning = null,
                            isScreenPinned = false
                        )
                    }
                }
                is ActionOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSettling = false,
                            errorMessage = res.reason,
                            retryKind = RetryActionKind.END_EXPEDITION
                        )
                    }
                }
            }
        }
    }

    fun retryFailedAction() {
        val cur = _uiState.value
        when (cur.retryKind) {
            RetryActionKind.START_EXPEDITION -> startExpedition()
            RetryActionKind.CANCEL_EXPEDITION -> dataState.value.runningSession?.sessionId?.let(::cancelExpeditionEarly)
            RetryActionKind.END_EXPEDITION -> {
                val sid = dataState.value.runningSession?.sessionId
                if (sid != null) {
                    endExpedition(sid)
                } else {
                    viewModelScope.launch {
                        repository.ensureInitializedAndRecover()
                    }
                }
            }
            null -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun dismissLatestReport() {
        _uiState.update { it.copy(latestReport = null, statusNotice = null) }
    }

    fun openHistoryReport(report: SettlementResult?) {
        _uiState.update { it.copy(historyDetailReport = report) }
    }

    fun clearNotices() {
        _uiState.update { it.copy(errorMessage = null, statusNotice = null, retryKind = null) }
    }

    // Character & Camp mutations
    fun upgradeFacility(type: FacilityType) {
        runSimpleAction("${type.displayName}已升级") { repository.upgradeFacility(type) }
    }

    fun craftEquipment(defId: String) {
        val name = ContentCatalog.equipmentById(defId)?.name ?: "装备"
        runSimpleAction("已打造「$name」并收入背包") { repository.craftEquipment(defId) }
    }

    fun upgradeEquipment(instanceId: String) {
        runSimpleAction("装备强化完成") { repository.upgradeEquipment(instanceId) }
    }

    fun equipItem(instanceId: String) {
        runSimpleAction("已更换装备配置") { repository.equipItem(instanceId) }
    }

    fun unequipSlot(slot: EquipmentSlot) {
        runSimpleAction("已卸下${slot.displayName}") { repository.unequipSlot(slot) }
    }

    fun toggleLock(instanceId: String) {
        runSimpleAction("装备锁定状态已更新") { repository.toggleItemLock(instanceId) }
    }

    fun salvageSelected(selectedIds: Set<String>) {
        viewModelScope.launch {
            when (val res = repository.salvageSelected(selectedIds)) {
                is ActionOutcome.Success -> {
                    val v = res.value
                    _uiState.update {
                        it.copy(
                            statusNotice = "已拆解 ${v.salvagedCount} 件装备，返还 ${v.goldGained} 金币与材料。",
                            errorMessage = null
                        )
                    }
                }
                is ActionOutcome.Failure -> {
                    _uiState.update { it.copy(errorMessage = res.reason) }
                }
            }
        }
    }

    fun togglePassive(passiveId: String) {
        runSimpleAction("被动配置已更新") { repository.togglePassive(passiveId) }
    }

    fun craftKeepsake(keepsakeId: String) {
        val name = ContentCatalog.keepsakeById(keepsakeId)?.name ?: "首领信物"
        runSimpleAction("已铭刻「$name」，加成已永久生效") { repository.craftKeepsake(keepsakeId) }
    }

    fun reforgeEquipmentAffix(instanceId: String) {
        runSimpleAction("装备词条重铸完成") { repository.reforgeEquipmentAffix(instanceId) }
    }

    fun engraveSecondaryAffix(instanceId: String) {
        runSimpleAction("副词条铭刻完成，流派共鸣已刷新") { repository.engraveSecondaryAffix(instanceId) }
    }

    fun selectActiveCompanion(companionId: String?) {
        val label = companionId?.let { ContentCatalog.companionById(it)?.name }
        val msg = if (label != null) "已请「$label」随行" else "已调整为独自出征（伙伴自动留守营地）"
        runSimpleAction(msg) { repository.selectActiveCompanion(companionId) }
    }

    fun selectTeaBlend(teaId: String?) {
        val label = teaId?.let { ContentCatalog.teaBlendById(it)?.name }
        val msg = if (label != null) "已备好「$label」" else "已收起热茶配置"
        runSimpleAction(msg) { repository.selectTeaBlend(teaId) }
    }

    fun selectDreamBeaconTier(tier: Int) {
        val msg = if (tier == 0) "已将深梦信标切回柔和常夜" else "已点亮深梦信标 · 阶 $tier"
        runSimpleAction(msg) { repository.selectDreamBeaconTier(tier) }
    }

    fun giftCompanion(companionId: String) {
        val name = ContentCatalog.companionById(companionId)?.name ?: "伙伴"
        runSimpleAction("已向「$name」赠送心意补给，好感度 +4") { repository.giftCompanion(companionId) }
    }

    fun unlockAstralNode(nodeId: String) {
        val title = ContentCatalog.astralNodeById(nodeId)?.title ?: "星图节点"
        runSimpleAction("已点亮旅人星图「$title」") { repository.unlockAstralNode(nodeId) }
    }

    fun resetAstralNodes() {
        runSimpleAction("已重塑旅人星图，返还全部星图点数") { repository.resetAstralNodes() }
    }

    fun transmuteMaterial(recipeId: String) {
        val title = ContentCatalog.transmuteRecipeById(recipeId)?.title ?: "花房配方"
        runSimpleAction("星尘花房精炼完成：$title") { repository.transmuteMaterial(recipeId) }
    }

    fun saveLoadout(slotIndex: Int, name: String = "方案 ${slotIndex + 1}") {
        val strategy = _uiState.value.selectedStrategy
        runSimpleAction("已将当前配置保存至「方案 ${slotIndex + 1}」") { repository.saveLoadout(slotIndex, name, strategy) }
    }

    fun applyLoadout(slotIndex: Int) {
        val targetStrategy = dataState.value.playerState.loadouts
            .firstOrNull { it.slotIndex == slotIndex }?.strategy
        viewModelScope.launch {
            when (val res = repository.applyLoadout(slotIndex)) {
                is ActionOutcome.Success -> _uiState.update { cur ->
                    cur.copy(
                        selectedStrategy = targetStrategy ?: cur.selectedStrategy,
                        statusNotice = "已一键切换至「方案 ${slotIndex + 1}」",
                        errorMessage = null
                    )
                }
                is ActionOutcome.Failure -> _uiState.update { it.copy(errorMessage = res.reason) }
            }
        }
    }

    fun resolveMorningEcho(chooseOptionA: Boolean) {
        viewModelScope.launch {
            when (val res = repository.resolveMorningEcho(chooseOptionA)) {
                is ActionOutcome.Success -> _uiState.update {
                    it.copy(statusNotice = res.value, errorMessage = null)
                }
                is ActionOutcome.Failure -> _uiState.update {
                    it.copy(errorMessage = res.reason)
                }
            }
        }
    }

    fun resolveTeaEcho(chooseOptionA: Boolean) {
        viewModelScope.launch {
            when (val res = repository.resolveTeaEcho(chooseOptionA)) {
                is ActionOutcome.Success -> _uiState.update {
                    it.copy(statusNotice = "${res.value.echoTitle}：${res.value.summaryText}", errorMessage = null)
                }
                is ActionOutcome.Failure -> _uiState.update {
                    it.copy(errorMessage = res.reason)
                }
            }
        }
    }

    fun selectFocusSpirit(spiritId: String?) {
        val name = spiritId?.let { ContentCatalog.spiritById(it)?.name }
        val msg = if (name != null) "已请「$name」坐在书桌旁伴读" else "已让伴读小灵兽自由玩耍"
        runSimpleAction(msg) { repository.selectFocusSpirit(spiritId) }
    }

    fun selectFocusTag(tagId: String) {
        val label = ContentCatalog.defaultFocusTagById(tagId)?.name
            ?: dataState.value.playerState.customFocusTags.firstOrNull { it.id == tagId }?.name
            ?: "当前标签"
        runSimpleAction("专注方向已切换为「$label」") { repository.selectFocusTag(tagId) }
    }

    fun addCustomFocusTag(name: String, archetype: BuildArchetype) {
        runSimpleAction("已新增自定义专注标签「${name.trim().take(10)}」") {
            repository.addCustomFocusTag(name, archetype)
        }
    }

    fun addFocusTask(title: String, tagId: String, targetPomodoros: Int = 1) {
        runSimpleAction("已将「${title.trim().take(40)}」加入今日行囊待办") {
            repository.addFocusTask(title, tagId, targetPomodoros)
        }
    }

    fun completeFocusTask(taskId: String) {
        runSimpleAction("待办事项已完成。每天最多发放 6 次悬赏。") {
            repository.completeFocusTask(taskId)
        }
    }

    fun deleteFocusTask(taskId: String) {
        runSimpleAction("已移出该待办事项") { repository.deleteFocusTask(taskId) }
    }

    fun clearCompletedFocusTasks() {
        runSimpleAction("已清理完成的待办，完成统计继续保留。") { repository.clearCompletedFocusTasks() }
    }

    fun selectActiveFocusTask(taskId: String?) {
        val title = taskId?.let { tid ->
            dataState.value.playerState.focusTasks.firstOrNull { it.id == tid }?.title
        }
        val msg = if (title != null) "本轮专注将同步推进「$title」" else "已取消关联特定待办"
        runSimpleAction(msg) { repository.selectActiveFocusTask(taskId) }
    }

    fun setSoundscapeSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isSoundscapeSheetOpen = open) }
    }

    fun setBreathingDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isBreathingDialogOpen = open) }
    }

    fun setStatsSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isStatsSheetOpen = open) }
    }

    fun startBreakTimer(minutes: Int) {
        val safeSec = (minutes.coerceIn(1, 30)) * 60
        breakDeadlineMs = timerNowMs() + safeSec * 1000L
        _uiState.update {
            it.copy(
                latestReport = null,
                breakRemainingSeconds = safeSec,
                statusNotice = "已开启 ${safeSec / 60} 分钟茶歇放松倒计时，起身伸个懒腰吧。"
            )
        }
    }

    fun skipBreakTimer() {
        breakDeadlineMs = null
        _uiState.update { it.copy(breakRemainingSeconds = 0, statusNotice = "茶歇已结束，随时准备下一段专注。") }
    }

    fun saveReflectionMood(sessionId: String, mood: String) {
        viewModelScope.launch {
            repository.saveReflectionMood(sessionId, mood)
            _uiState.update { cur ->
                cur.copy(
                    latestReport = cur.latestReport?.let { if (it.sessionId == sessionId) it.copy(reflectionMood = mood) else it },
                    historyDetailReport = cur.historyDetailReport?.let { if (it.sessionId == sessionId) it.copy(reflectionMood = mood) else it },
                    statusNotice = "已记下今日心境：「$mood」"
                )
            }
        }
    }

    fun setFaceDownActive(faceDown: Boolean) {
        _uiState.update { it.copy(isFaceDownFlipActive = faceDown) }
    }

    fun setSleepBlackoutMode(enabled: Boolean) {
        _uiState.update { it.copy(sleepBlackoutMode = enabled) }
    }

    fun setShowWakeConfirm(show: Boolean) {
        _uiState.update { it.copy(showWakeConfirm = show) }
    }

    fun setScreenPinned(pinned: Boolean) {
        _uiState.update { it.copy(isScreenPinned = pinned) }
    }

    fun onScreenPinFailed() {
        _uiState.update { it.copy(sleepGuardWarning = "屏幕固定未能完成，请重试或按电源键锁屏休息。") }
    }

    fun onUserLeftWithScreenOn() {
        val running = dataState.value.runningSession ?: return
        val nextCount = maxOf(_uiState.value.phoneDistractionCount, running.distractionCount) + 1
        _uiState.update { cur ->
            val warning = if (running.sessionMode.isFocus) {
                "切出查看也没关系，已为你保留全部基础收益；回到书桌即可继续积累定心加成（已离屏 $nextCount 次）"
            } else {
                "检测到守夜期间亮屏切出应用（已分心 $nextCount 次）。请留在守夜屏或直接按电源键锁屏入睡，避免边挂机边玩手机！"
            }
            cur.copy(
                phoneDistractionCount = nextCount,
                sleepBlackoutMode = false,
                sleepGuardWarning = warning
            )
        }
        viewModelScope.launch {
            repository.recordRunningSessionFocusStats(running.sessionId, nextCount, flipFocusAccumulatedSeconds)
        }
    }

    fun onSleepGuardScreenTap() {
        if (dataState.value.runningSession == null) return
        _uiState.update { cur ->
            if (cur.sleepBlackoutMode) {
                cur.copy(sleepBlackoutMode = false)
            } else {
                cur.copy(
                    sleepGuardWarning = "夜深了，请不要边挂机边玩手机，把屏幕交给旅人守夜吧。"
                )
            }
        }
    }

    fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }

    fun copyBackup(copy: (String) -> Unit) {
        runBackupAction(BackupAction.COPY_TEXT) {
            when (val res = repository.exportBackupJson()) {
                is ActionOutcome.Success -> {
                    copy(res.value)
                    showBackupResult(ActionOutcome.Success(Unit), "最新存档已复制。")
                }
                is ActionOutcome.Failure -> showBackupResult(res, "")
            }
        }
    }

    // CreateDocument can return to a recreated activity; export a fresh consistent snapshot then.
    fun exportBackupFile(open: () -> java.io.OutputStream?) {
        runBackupAction(BackupAction.SAVE_FILE) {
            val result = when (val backup = repository.exportBackupJson()) {
                is ActionOutcome.Failure -> backup
                is ActionOutcome.Success -> withContext(Dispatchers.IO) {
                    BackupFiles.write(open, backup.value)
                }
            }
            showBackupResult(result, "最新进度已保存为文件。")
        }
    }

    fun importBackupFile(open: () -> java.io.InputStream?) {
        runBackupAction(BackupAction.IMPORT) {
            when (val read = withContext(Dispatchers.IO) {
                BackupFiles.read(open)
            }) {
                is ActionOutcome.Success -> finishRestore(repository.importBackupJson(read.value), "存档已验证并恢复。")
                is ActionOutcome.Failure -> showBackupResult(read, "")
            }
        }
    }

    fun importBackup(rawText: String, onDone: (Boolean) -> Unit = {}) {
        runBackupAction(BackupAction.IMPORT) {
            onDone(finishRestore(repository.importBackupJson(rawText), "存档已验证并恢复。"))
        }
    }

    fun restoreSnapshot(snapshotId: Long) {
        runBackupAction(BackupAction.RESTORE) {
            finishRestore(repository.restoreSnapshot(snapshotId), "已从本地备份快照恢复。")
        }
    }

    private fun runBackupAction(action: BackupAction, block: suspend () -> Unit) {
        if (_uiState.value.backupAction != null) return
        _uiState.update { it.copy(backupAction = action, statusNotice = null, errorMessage = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showBackupResult(ActionOutcome.Failure(e.message ?: "存档操作失败，请重试。"), "")
            } finally {
                _uiState.update { it.copy(backupAction = null) }
            }
        }
    }

    private fun showBackupResult(result: ActionOutcome<*>, successMessage: String) {
        _uiState.update {
            when (result) {
                is ActionOutcome.Success -> it.copy(statusNotice = successMessage, errorMessage = null)
                is ActionOutcome.Failure -> it.copy(errorMessage = result.reason, statusNotice = null)
            }
        }
    }

    private suspend fun finishRestore(result: ActionOutcome<Unit>, successMessage: String): Boolean {
        if (result is ActionOutcome.Failure) {
            showBackupResult(result, "")
            return false
        }
        val recovery = repository.ensureInitializedAndRecover()
        // Query fresh saved state explicitly: an observer emission may precede the import callback.
        val restored = repository.stateFlow.first { it.initialized }
        syncedSelectionFromSave = true
        _uiState.update {
            it.copy(
                selectedRouteId = restored.playerState.lastRouteId,
                selectedStrategy = restored.playerState.lastStrategy,
                latestReport = (recovery as? ActionOutcome.Success)?.value,
                historyDetailReport = null,
                retryKind = if (recovery is ActionOutcome.Failure) RetryActionKind.END_EXPEDITION else null,
                errorMessage = (recovery as? ActionOutcome.Failure)?.reason,
                statusNotice = if (recovery is ActionOutcome.Failure) null else successMessage,
                sleepBlackoutMode = false,
                showWakeConfirm = false,
                phoneDistractionCount = 0,
                sleepGuardWarning = null,
                isScreenPinned = false
            )
        }
        return recovery is ActionOutcome.Success
    }

    fun duplicateSalvageIds(): List<String> {
        val snap = dataState.value
        return ProgressionEngine.duplicateSalvageCandidates(snap.playerState, snap.inventory)
    }

    private fun runSimpleAction(successMsg: String, block: suspend () -> ActionOutcome<Unit>) {
        if (_uiState.value.isPerformingAction) return
        _uiState.update {
            it.copy(isPerformingAction = true, statusNotice = null, errorMessage = null, retryKind = null)
        }
        viewModelScope.launch {
            try {
                when (val res = block()) {
                    is ActionOutcome.Success -> _uiState.update {
                        it.copy(statusNotice = successMsg, errorMessage = null)
                    }
                    is ActionOutcome.Failure -> _uiState.update {
                        it.copy(errorMessage = res.reason, statusNotice = null)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "操作未完成，请重试。", statusNotice = null)
                }
            } finally {
                _uiState.update { it.copy(isPerformingAction = false) }
            }
        }
    }

    class Factory(
        private val repository: GameRepository,
        private val clock: ClockProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DreamTrailViewModel(repository, clock) as T
        }
    }
}
