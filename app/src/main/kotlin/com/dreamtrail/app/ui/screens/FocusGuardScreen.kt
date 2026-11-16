package com.dreamtrail.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dreamtrail.app.ui.JourneyUiState
import com.dreamtrail.app.ui.components.FocusDeskScene
import com.dreamtrail.app.ui.components.FocusSpiritAvatar
import com.dreamtrail.app.ui.components.JournalCard
import com.dreamtrail.app.ui.components.TagBadge
import com.dreamtrail.app.ui.components.formatDurationZh
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.model.ExpeditionSession
import com.dreamtrail.core.model.FocusTaskItem
import com.dreamtrail.core.model.SessionMode
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusGuardScreen(
    running: ExpeditionSession,
    uiState: JourneyUiState,
    reduceMotion: Boolean,
    muted: Boolean,
    soundscapeChannelName: String,
    flipEnabled: Boolean,
    onToggleScreenPin: () -> Unit,
    onOpenSoundscape: () -> Unit,
    onOpenBreathing: () -> Unit,
    onRequestEndConfirm: (Boolean) -> Unit,
    onEndExpedition: (String) -> Unit,
    onCancelEarly: (String) -> Unit,
    onRetrySettlement: () -> Unit,
    linkedTask: FocusTaskItem? = null
) {
    val route = ContentCatalog.routeById(running.routeId)
    val elapsedSeconds = uiState.activePreview?.effectiveElapsedSeconds ?: 0L
    val rawElapsed = uiState.activePreview?.elapsedSeconds ?: 0L
    val targetSeconds = running.targetDurationSeconds
    val companion = running.snapshot.activeCompanionId?.let { ContentCatalog.companionById(it) }
    val spirit = running.snapshot.activeSpiritId?.let { ContentCatalog.spiritById(it) }
    val focusTag = running.focusTagId?.let { tid -> ContentCatalog.defaultFocusTagById(tid) }
    var keepScreenBrightDimmed by remember { mutableStateOf(false) }

    val timerMainText = remember(rawElapsed, targetSeconds, running.sessionMode) {
        if (targetSeconds != null) {
            val diff = targetSeconds - rawElapsed
            if (diff >= 0L) {
                formatDigitalClock(diff)
            } else {
                "+${formatDigitalClock(-diff)}"
            }
        } else {
            formatDigitalClock(rawElapsed)
        }
    }

    val timerSubtitle = remember(rawElapsed, targetSeconds, running.sessionMode, uiState.targetCountdownFinished) {
        when {
            rawElapsed >= running.sessionMode.maxSingleWindowSeconds ->
                "本轮记录时长已到上限，可结束后开启下一轮"
            targetSeconds != null && uiState.targetCountdownFinished && running.sessionMode == SessionMode.POWER_NAP ->
                "小憩目标已达成，可以结束休息并结算收获"
            targetSeconds != null && uiState.targetCountdownFinished ->
                "目标时长已达成，正在继续积累超额专注心流收益"
            targetSeconds != null ->
                "${running.sessionMode.displayName}倒计时中 · 每 5 分钟推进 1 段探索"
            else ->
                "${running.sessionMode.displayName}自由计时中 · 每 5 分钟推进 1 段探索"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DreamTrailColors.NightBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TagBadge(running.sessionMode.displayName, DreamTrailColors.LanternAmber)
                if (focusTag != null) {
                    TagBadge(focusTag.name, DreamTrailColors.StarlightBlue)
                }
                running.dayPhenomenon?.let { dayPhenom ->
                    val match = route != null &&
                        (dayPhenom.favoredRegionId == route.regionId || dayPhenom.favoredEnvironment == route.environmentTag)
                    TagBadge(
                        text = if (match) "天象共鸣：${dayPhenom.name}" else "今日天象：${dayPhenom.name}",
                        accent = if (match) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                    )
                }
                if (uiState.isFaceDownFlipActive && flipEnabled) {
                    TagBadge("翻转静心生效中", DreamTrailColors.SageMist)
                }
            }

            if (linkedTask != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DreamTrailColors.CardSurface,
                    border = BorderStroke(1.dp, DreamTrailColors.LanternAmber.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TaskAlt,
                            contentDescription = null,
                            tint = DreamTrailColors.LanternAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "当前推进待办：${linkedTask.title}",
                                style = MaterialTheme.typography.titleSmall,
                                color = DreamTrailColors.ParchmentText
                            )
                            Text(
                                text = "番茄进度 ${linkedTask.completedPomodoros}/${linkedTask.targetPomodoros}",
                                style = MaterialTheme.typography.labelSmall,
                                color = DreamTrailColors.MutedInk
                            )
                        }
                    }
                }
            }

            JournalCard(
                containerColor = DreamTrailColors.CardSurface,
                borderColor = DreamTrailColors.LanternAmber.copy(alpha = 0.55f)
            ) {
                if (running.sessionMode.isFocus) {
                    FocusDeskScene(
                        isRunning = true,
                        reduceMotion = reduceMotion,
                        regionId = route?.regionId ?: com.dreamtrail.core.model.RegionId.SUNLIT_ATHENAEUM,
                        companionId = companion?.id,
                        spiritId = spirit?.id,
                        isFaceDown = uiState.isFaceDownFlipActive,
                        height = 128.dp
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = timerMainText,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.targetCountdownFinished) DreamTrailColors.SageMist else DreamTrailColors.LanternAmber
                    )
                    Text(
                        text = timerSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.MutedInk
                    )
                    Text(
                        text = "${route?.name ?: running.routeId} · 已推进 ${uiState.activePreview?.estimatedNodes ?: 0} 段（有效 ${formatDurationZh(elapsedSeconds)}）",
                        style = MaterialTheme.typography.labelLarge,
                        color = DreamTrailColors.ParchmentText
                    )

                    if (targetSeconds != null && targetSeconds > 0L) {
                        val frac = (rawElapsed.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (uiState.targetCountdownFinished) DreamTrailColors.SageMist else DreamTrailColors.LanternAmber,
                            trackColor = DreamTrailColors.CardElevated
                        )
                    }
                }
            }

            if (companion != null || spirit != null) {
                JournalCard(containerColor = DreamTrailColors.CardElevated) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (spirit != null) {
                            FocusSpiritAvatar(spirit = spirit, size = 44.dp, unlocked = true, active = true)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AutoStories,
                                contentDescription = null,
                                tint = DreamTrailColors.LanternAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = buildString {
                                    if (companion != null) append(companion.name)
                                    if (companion != null && spirit != null) append(" & ")
                                    if (spirit != null) append(spirit.name)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = DreamTrailColors.LanternAmber
                            )
                            Text(
                                text = when {
                                    uiState.phoneDistractionCount > 0 && companion?.distractionNudgeLine?.isNotBlank() == true ->
                                        companion.distractionNudgeLine
                                    companion?.studyFocusLine?.isNotBlank() == true -> companion.studyFocusLine
                                    else -> "稳步保持当前心流节拍即可，无论专心多久都会忠实化为旅途收获。"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.ParchmentText
                            )
                        }
                    }
                }
            }

            if (uiState.sleepGuardWarning != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DreamTrailColors.CardElevated,
                    border = BorderStroke(1.dp, DreamTrailColors.SageMist.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = uiState.sleepGuardWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.SageMist,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (uiState.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DreamTrailColors.WarnSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.CoralWarn
                        )
                        OutlinedButton(onClick = onRetrySettlement) {
                            Text("重试结算")
                        }
                    }
                }
            }

            // Quick ambient & mindfulness tools
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenSoundscape,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (muted || soundscapeChannelName == "不播放") "白噪音" else "声景·$soundscapeChannelName")
                }
                OutlinedButton(
                    onClick = onOpenBreathing,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.Air, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("呼吸调息")
                }
                OutlinedButton(
                    onClick = onToggleScreenPin,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (uiState.isScreenPinned) "解除锁定" else "专注锁定")
                }
                OutlinedButton(
                    onClick = { keepScreenBrightDimmed = !keepScreenBrightDimmed },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(if (keepScreenBrightDimmed) "恢复常规提示" else "包容专注说明")
                }
            }

            if (keepScreenBrightDimmed) {
                Text(
                    text = "包容激励机制：即便切出查资料也不会销毁既有基础收益；专注完整轮次、关联书签或使用翻转静心可额外赢得 +3% ~ +10% 定心加成与昼夜循环增益。",
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.MutedInk
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { onRequestEndConfirm(true) },
                enabled = !uiState.isSettling,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Text(
                    text = if (uiState.isSettling) {
                        "正在结算收获…"
                    } else if (running.sessionMode == SessionMode.POWER_NAP) {
                        "醒来并结算午休收获"
                    } else {
                        "完成本轮专注 · 结算收获"
                    }
                )
            }
        }
    }

    if (uiState.showWakeConfirm) {
        val underOneMin = rawElapsed < 60L
        AlertDialog(
            onDismissRequest = { onRequestEndConfirm(false) },
            containerColor = DreamTrailColors.CardSurface,
            title = { Text("要结束本轮${running.sessionMode.displayName}吗？", color = DreamTrailColors.ParchmentText) },
            text = {
                Text(
                    text = if (underOneMin) {
                        "当前才经过 ${formatDurationZh(rawElapsed)}（不足 1 分钟）。您可以选择确认结束，或无损取消重新配置。"
                    } else {
                        "已记录专注 ${formatDurationZh(rawElapsed)}，确认后将立即发放旅途战利与定心祝福。"
                    },
                    color = DreamTrailColors.MutedInk
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRequestEndConfirm(false)
                        onEndExpedition(running.sessionId)
                    }
                ) {
                    Text("确认结算专注战报")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (underOneMin) {
                        TextButton(
                            onClick = {
                                onRequestEndConfirm(false)
                                onCancelEarly(running.sessionId)
                            }
                        ) {
                            Text("无损取消", color = DreamTrailColors.CoralWarn)
                        }
                    }
                    TextButton(onClick = { onRequestEndConfirm(false) }) {
                        Text("继续专注")
                    }
                }
            }
        )
    }
}

private fun formatDigitalClock(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0L)
    val hrs = s / 3600L
    val mins = (s % 3600L) / 60L
    val secs = s % 60L
    return if (hrs > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}
