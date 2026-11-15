package com.dreamtrail.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamtrail.app.ui.JourneyUiState
import com.dreamtrail.app.ui.components.CampfireTravelerScene
import com.dreamtrail.app.ui.components.JournalCard
import com.dreamtrail.app.ui.components.TagBadge
import com.dreamtrail.app.ui.components.formatDurationZh
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.model.ExpeditionSession
import com.dreamtrail.core.model.RegionId
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SleepGuardScreen(
    running: ExpeditionSession,
    uiState: JourneyUiState,
    reduceMotion: Boolean,
    onExitBlackout: () -> Unit,
    onEnterBlackout: () -> Unit,
    onToggleScreenPin: () -> Unit,
    onRequestWakeConfirm: (Boolean) -> Unit,
    onEndExpedition: (String) -> Unit,
    onCancelEarly: (String) -> Unit,
    onBackgroundTap: () -> Unit,
    onRetrySettlement: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running.sessionId) {
        while (isActive) {
            nowMs = System.currentTimeMillis()
            delay(10_000L)
        }
    }

    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault()) }
    val activeRoute = remember(running.routeId) { ContentCatalog.routeById(running.routeId) }
    val preview = uiState.activePreview?.takeIf { it.sessionId == running.sessionId }
    val elapsedSec = preview?.elapsedSeconds ?: 0L
    val canCancelEarly = preview != null && elapsedSec <= 300L && running.endAtUtcMs == null

    if (uiState.sleepBlackoutMode) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF030508))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onExitBlackout
                )
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bedtime,
                    contentDescription = "极暗守夜遮罩",
                    tint = DreamTrailColors.MutedInk.copy(alpha = 0.28f),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = timeFmt.format(Date(nowMs)),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    color = DreamTrailColors.ParchmentText.copy(alpha = 0.24f)
                )
                Text(
                    text = "守夜旅途已进行 ${formatDurationZh(elapsedSec)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DreamTrailColors.MutedInk.copy(alpha = 0.25f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "轻触屏幕唤醒守夜面板 · 请安心入睡",
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.MutedInk.copy(alpha = 0.22f),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF090C12)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBackgroundTap
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val hasDistraction = uiState.phoneDistractionCount > 0
            TagBadge(
                text = if (hasDistraction) {
                    "安眠守夜占用中 · 已检测切出 ${uiState.phoneDistractionCount} 次"
                } else {
                    "安眠守夜占用中 · 已锁定其他功能页防玩手机"
                },
                accent = if (hasDistraction) DreamTrailColors.CoralWarn else DreamTrailColors.SageMist
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = timeFmt.format(Date(nowMs)),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    color = DreamTrailColors.ParchmentText
                )
                Text(
                    text = dateFmt.format(Date(nowMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.MutedInk
                )
                Text(
                    text = "${activeRoute?.regionId?.displayName ?: ""} · ${activeRoute?.name ?: running.routeId} · 策略「${running.strategy.displayName}」",
                    style = MaterialTheme.typography.titleSmall,
                    color = DreamTrailColors.LanternAmber,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                CampfireTravelerScene(
                    isTraveling = true,
                    reduceMotion = reduceMotion,
                    regionId = activeRoute?.regionId ?: RegionId.MISTWOOD,
                    companionId = running.snapshot.activeCompanionId,
                    nightPhenomenon = running.nightPhenomenon
                )
            }

            JournalCard(
                modifier = Modifier.widthIn(max = 560.dp),
                containerColor = DreamTrailColors.CardSurface,
                borderColor = DreamTrailColors.CardOutline
            ) {
                Text(
                    text = "已守夜出发：${formatDurationZh(elapsedSec)}（约完成 ${preview?.estimatedNodes ?: 0} 段探路）",
                    style = MaterialTheme.typography.titleSmall,
                    color = DreamTrailColors.LanternAmber
                )
                if (preview?.budgetNotice != null) {
                    Text(
                        text = preview.budgetNotice!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.MutedInk
                    )
                }
                Text(
                    text = "请保持本页占用屏幕并放下手机，或直接按电源键锁屏睡觉。切勿切出应用刷手机。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DreamTrailColors.ParchmentText
                )
                Text(
                    text = "本次旅程使用出发时的装备与加成；醒来结束守夜即可查看收获。",
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.MutedInk
                )
            }

            if (uiState.sleepGuardWarning != null) {
                JournalCard(
                    modifier = Modifier.widthIn(max = 560.dp),
                    containerColor = DreamTrailColors.WarnSurface,
                    borderColor = DreamTrailColors.CoralWarn
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "分心提醒",
                            tint = DreamTrailColors.CoralWarn
                        )
                        Text(
                            text = "防玩手机提醒",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.CoralWarn
                        )
                    }
                    Text(
                        text = uiState.sleepGuardWarning!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DreamTrailColors.ParchmentText
                    )
                }
            }

            if (uiState.errorMessage != null) {
                JournalCard(
                    modifier = Modifier.widthIn(max = 560.dp),
                    containerColor = DreamTrailColors.WarnSurface,
                    borderColor = DreamTrailColors.CoralWarn
                ) {
                    Text(
                        text = "操作未完成",
                        style = MaterialTheme.typography.titleSmall,
                        color = DreamTrailColors.CoralWarn
                    )
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DreamTrailColors.ParchmentText
                    )
                    if (uiState.retryKind != null) Button(
                        onClick = onRetrySettlement,
                        enabled = !uiState.isSettling,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("重试")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onEnterBlackout,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DreamTrailColors.CardSurface,
                        contentColor = DreamTrailColors.ParchmentText
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DarkMode,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("极暗守夜", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onToggleScreenPin,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (uiState.isScreenPinned) DreamTrailColors.SageMist.copy(alpha = 0.2f) else DreamTrailColors.CardSurface,
                        contentColor = if (uiState.isScreenPinned) DreamTrailColors.SageMist else DreamTrailColors.ParchmentText
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isScreenPinned) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (uiState.isScreenPinned) "解除固定" else "固定屏幕", maxLines = 1)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            ) {
                if (uiState.isSettling) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = DreamTrailColors.LanternAmber
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "正在整理本次收获，请稍候…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DreamTrailColors.ParchmentText
                        )
                    }
                } else if (!uiState.showWakeConfirm) {
                    OutlinedButton(
                        onClick = { onRequestWakeConfirm(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        border = BorderStroke(1.dp, DreamTrailColors.MutedInk.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DreamTrailColors.CardSurface,
                            contentColor = DreamTrailColors.MutedInk
                        )
                    ) {
                        Text("我已醒来 · 准备结束守夜")
                    }
                } else {
                    JournalCard(
                        containerColor = DreamTrailColors.CardElevated,
                        borderColor = DreamTrailColors.LanternAmber.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "确认结束当前守夜并查看收获战报吗？",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.ParchmentText
                        )
                        Button(
                            onClick = { onEndExpedition(running.sessionId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                        ) {
                            Text("确认结束冒险并结算战报")
                        }
                        OutlinedButton(
                            onClick = { onRequestWakeConfirm(false) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text("继续守夜休息")
                        }
                        if (canCancelEarly) {
                            TextButton(
                                onClick = { onCancelEarly(running.sessionId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Text("撤销出发（5 分钟内误触可取消，不发放奖励）")
                            }
                        }
                    }
                }
            }
        }
    }
}
