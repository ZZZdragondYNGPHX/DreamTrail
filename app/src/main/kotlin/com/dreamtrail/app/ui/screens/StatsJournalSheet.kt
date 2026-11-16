package com.dreamtrail.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dreamtrail.app.data.repository.AppUiDataState
import com.dreamtrail.app.ui.components.JournalCard
import com.dreamtrail.app.ui.components.TagBadge
import com.dreamtrail.app.ui.components.archetypeColor
import com.dreamtrail.app.ui.components.formatDurationZh
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsJournalSheet(
    dataState: AppUiDataState,
    onDismiss: () -> Unit
) {
    val discovery = dataState.discoveryState
    val player = dataState.playerState
    val shortFmt = remember { DateTimeFormatter.ofPattern("MM/dd", Locale.US) }
    val today = LocalDate.now()
    val todayKey = today.toString()
    val last7Days = remember(todayKey) {
        (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            date.toString() to date.format(shortFmt)
        }
    }

    val todayFocusSec = discovery.dailyFocusSecondsByDate[todayKey] ?: 0L
    val todaySleepSec = discovery.dailySleepSecondsByDate[todayKey] ?: 0L
    val maxDaySec = last7Days.maxOfOrNull { (k, _) ->
        maxOf(discovery.dailyFocusSecondsByDate[k] ?: 0L, discovery.dailySleepSecondsByDate[k] ?: 0L)
    }?.coerceAtLeast(3600L) ?: 3600L

    val tagNames = remember(player.customFocusTags) {
        (ContentCatalog.defaultFocusTags + player.customFocusTags).associateBy { it.id }
    }
    val totalTaggedSec = discovery.focusSecondsByTag.values.sum().coerceAtLeast(1L)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .heightIn(max = 650.dp),
            shape = RoundedCornerShape(20.dp),
            color = DreamTrailColors.NightBackground,
            border = BorderStroke(1.dp, DreamTrailColors.CardOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "昼夜时光手账",
                        style = MaterialTheme.typography.titleLarge,
                        color = DreamTrailColors.ParchmentText
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("关闭")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Today & cumulative parity grid
                    JournalCard {
                        Text(
                            text = "专注与睡眠双核总览",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.LanternAmber
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatTile(
                                title = "今日专注",
                                value = formatDurationZh(todayFocusSec),
                                sub = "累计 ${discovery.totalFocusSessionsCompleted} 轮 · ${discovery.totalFocusCreditedSeconds / 3600}h",
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                title = "今日安眠",
                                value = formatDurationZh(todaySleepSec),
                                sub = "累计 ${discovery.totalRestSessionsCompleted} 次休息 · ${(discovery.totalCreditedSeconds - discovery.totalFocusCreditedSeconds).coerceAtLeast(0L) / 3600}h",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatTile(
                                title = "完成待办",
                                value = "${discovery.completedTasksCount} 项",
                                sub = "澄明墨晶 ${player.resources.inspirationInk}",
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                title = "伴读小灵兽",
                                value = "${discovery.unlockedSpiritIds.size}/${ContentCatalog.focusSpirits.size}",
                                sub = "书斋 Lv.${player.facilityLevel(com.dreamtrail.core.model.FacilityType.SCRIPTORIUM)}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 7-day dual bar overview
                    JournalCard {
                        Text(
                            text = "近 7 日昼夜节律",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.LanternAmber
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TagBadge("白昼专注", DreamTrailColors.LanternAmber)
                            TagBadge("夜间安眠", DreamTrailColors.StarlightBlue)
                        }
                        for ((dateKey, shortLabel) in last7Days) {
                            val fSec = discovery.dailyFocusSecondsByDate[dateKey] ?: 0L
                            val sSec = discovery.dailySleepSecondsByDate[dateKey] ?: 0L
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(shortLabel, style = MaterialTheme.typography.labelMedium, color = DreamTrailColors.ParchmentText)
                                    Text(
                                        text = "专注 ${fSec / 60}m · 安眠 ${sSec / 60}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DreamTrailColors.MutedInk
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .background(DreamTrailColors.CardElevated, RoundedCornerShape(4.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((fSec.toFloat() / maxDaySec.toFloat()).coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(DreamTrailColors.LanternAmber, RoundedCornerShape(4.dp))
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .background(DreamTrailColors.CardElevated, RoundedCornerShape(4.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((sSec.toFloat() / maxDaySec.toFloat()).coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(DreamTrailColors.StarlightBlue, RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }

                    // Tag distribution
                    JournalCard {
                        Text(
                            text = "专注方向分布",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.SageMist
                        )
                        if (discovery.focusSecondsByTag.isEmpty()) {
                            Text(
                                text = "完成任意白昼专注后，这里将自动统计各标签的时间投入比重。",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                        } else {
                            for ((tagId, sec) in discovery.focusSecondsByTag.entries.sortedByDescending { it.value }) {
                                val def = tagNames[tagId]
                                val name = def?.name ?: tagId
                                val pct = ((sec * 100L) / totalTaggedSec).toInt()
                                val color = def?.let { archetypeColor(it.archetype) } ?: DreamTrailColors.LanternAmber
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(name, style = MaterialTheme.typography.labelLarge, color = DreamTrailColors.ParchmentText)
                                        Text("${formatDurationZh(sec)} ($pct%)", style = MaterialTheme.typography.labelSmall, color = DreamTrailColors.MutedInk)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .background(DreamTrailColors.CardElevated, RoundedCornerShape(4.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth((sec.toFloat() / totalTaggedSec.toFloat()).coerceIn(0f, 1f))
                                                .fillMaxHeight()
                                                .background(color, RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DreamTrailColors.CardElevated,
        border = BorderStroke(1.dp, DreamTrailColors.CardOutline)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = DreamTrailColors.MutedInk)
            Text(value, style = MaterialTheme.typography.titleSmall, color = DreamTrailColors.ParchmentText)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = DreamTrailColors.SageMist)
        }
    }
}
