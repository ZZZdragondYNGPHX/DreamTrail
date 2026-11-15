package com.dreamtrail.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.model.RegionId
import com.dreamtrail.core.model.SettlementResult

/**
 * Three-layer settlement battle report:
 * 1. Harvest (totals)
 * 2. What happened (up to 3 key events + merged ordinary counts)
 * 3. Next step suggestion + single return button (no secondary claim dialog)
 */
@Composable
fun SettlementReportCard(
    report: SettlementResult,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val route = ContentCatalog.routeById(report.routeId)
    var showDetailNotes by remember(report.sessionId) { mutableStateOf(false) }

    JournalCard(
        modifier = modifier,
        containerColor = DreamTrailColors.CardSurface,
        borderColor = DreamTrailColors.LanternAmber.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "旅程战报 · ${route?.name ?: report.routeId}",
                    style = MaterialTheme.typography.titleLarge,
                    color = DreamTrailColors.ParchmentText
                )
                Text(
                    text = "历时 ${formatDurationZh(report.rawDurationSeconds)} · 完成 ${report.completedNodes} 段探索（${report.strategy.displayName}）",
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.MutedInk
                )
            }
            TagBadge(text = "已入账", accent = DreamTrailColors.SageMist)
        }

        if (report.clockAnomalyNote != null) {
            Surface(
                color = DreamTrailColors.CardElevated,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = report.clockAnomalyNote!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.LanternAmber,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
        if (report.budgetNote != null) {
            Text(
                text = report.budgetNote!!,
                style = MaterialTheme.typography.bodySmall,
                color = DreamTrailColors.MutedInk
            )
        }

        HorizontalDivider(color = DreamTrailColors.CardOutline)

        // Layer 1: Harvest
        Text(
            text = "一、本次收获",
            style = MaterialTheme.typography.titleSmall,
            color = DreamTrailColors.LanternAmber
        )
        val totalGold = report.baseGold + report.blessingGold
        val totalXp = report.baseXp + report.blessingXp
        val bonusPctText = java.math.BigDecimal(report.sleepBonusBasisPoints).movePointLeft(2).stripTrailingZeros().toPlainString() + "%"
        Text(
            text = "金币 +$totalGold（含安歇祝福 +${report.blessingGold}） · 经验 +$totalXp（含 +${report.blessingXp}）",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = DreamTrailColors.ParchmentText
        )

        val matText = report.materialsGained.entries.joinToString(" · ") { (matId, amt) ->
            val matName = RegionId.entries.firstOrNull { it.materialId == matId }?.materialName ?: matId
            "$matName +$amt"
        }
        if (matText.isNotEmpty() || report.bossMarksGained > 0L) {
            val extra = buildList {
                if (matText.isNotEmpty()) add(matText)
                if (report.bossMarksGained > 0L) add("首领印记 +${report.bossMarksGained}")
            }.joinToString(" · ")
            Text(
                text = extra,
                style = MaterialTheme.typography.bodyMedium,
                color = DreamTrailColors.SageMist
            )
        }

        if (report.leveledUpTo > report.leveledUpFrom) {
            TagBadge(
                text = "角色升级：Lv.${report.leveledUpFrom} → Lv.${report.leveledUpTo}",
                accent = DreamTrailColors.LanternAmber
            )
        }

        val journeyTags = buildList {
            report.nightPhenomenonName?.let { add("夜象：$it") }
            report.activeCompanionName?.let { add("随行：$it") }
            report.teaBlendName?.let { add("热茶：$it") }
            if (report.dreamBeaconTier > 0) add("深梦信标 阶${report.dreamBeaconTier}")
            if (report.eliteVictories > 0) add("精英击败 ${report.eliteVictories} 次")
        }
        if (journeyTags.isNotEmpty()) {
            Text(
                text = journeyTags.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = DreamTrailColors.StarlightBlue
            )
        }

        if (report.newEquipment.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "新获装备（${report.newEquipment.size} 件已收入背包）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DreamTrailColors.ParchmentText
                )
                for (eq in report.newEquipment) {
                    val def = ContentCatalog.equipmentById(eq.defId)
                    Text(
                        text = "• ${def?.name ?: eq.defId}（${def?.slot?.displayName ?: ""} · 词条「${eq.affix.displayName}」）",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.StarlightBlue
                    )
                }
            }
        }
        if (report.newSouvenirIds.isNotEmpty()) {
            val souvNames = report.newSouvenirIds.mapNotNull { ContentCatalog.souvenirById(it)?.name }
            if (souvNames.isNotEmpty()) {
                Text(
                    text = "风物志新收录：${souvNames.joinToString("、")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.LanternAmber
                )
            }
        }
        report.chronicleUpdateText?.let { chronicleNote ->
            Text(
                text = "秘闻推进：$chronicleNote",
                style = MaterialTheme.typography.bodySmall,
                color = DreamTrailColors.StarlightBlue
            )
        }
        if (report.completedCommissionTitles.isNotEmpty()) {
            Text(
                text = "营地委托完成：${report.completedCommissionTitles.joinToString("、")}",
                style = MaterialTheme.typography.bodySmall,
                color = DreamTrailColors.LanternAmber
            )
        }
        if (report.dispatchSummaries.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "营地协理收获",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = DreamTrailColors.SageMist
                )
                for (line in report.dispatchSummaries) {
                    Text(
                        text = "• $line",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.MutedInk
                    )
                }
            }
        }
        if (report.duplicateConvertedGold > 0L) {
            Text(
                text = "背包内同款重复装备已自动折算为 +${report.duplicateConvertedGold} 金币（已计入总金币）。",
                style = MaterialTheme.typography.bodySmall,
                color = DreamTrailColors.MutedInk
            )
        }

        HorizontalDivider(color = DreamTrailColors.CardOutline)

        // Layer 2: What happened
        Text(
            text = "二、沿途发生了什么",
            style = MaterialTheme.typography.titleSmall,
            color = DreamTrailColors.LanternAmber
        )

        if (report.keyEvents.isEmpty() && report.mergedActivities.isEmpty()) {
            Text(
                text = "本次离开时间较短，伙伴沿途安全行进并按有效时长带回基础旅行收益。",
                style = MaterialTheme.typography.bodyMedium,
                color = DreamTrailColors.MutedInk
            )
        } else {
            for (ev in report.keyEvents) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = DreamTrailColors.CardElevated
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = ev.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = DreamTrailColors.ParchmentText,
                                modifier = Modifier.weight(1f)
                            )
                            if (ev.isFirstDiscovery) {
                                TagBadge("首次发现", DreamTrailColors.LanternAmber)
                            }
                        }
                        Text(
                            text = ev.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DreamTrailColors.MutedInk
                        )
                        if (ev.highlightReason != null) {
                            Text(
                                text = "影响说明：${ev.highlightReason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.SageMist
                            )
                        }
                    }
                }
            }

            if (report.mergedActivities.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (act in report.mergedActivities) {
                        Text(
                            text = "• ${act.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.MutedInk
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = DreamTrailColors.CardOutline)

        // Layer 3: Next step
        Text(
            text = "三、下一步建议",
            style = MaterialTheme.typography.titleSmall,
            color = DreamTrailColors.LanternAmber
        )
        Text(
            text = report.nextStepSuggestion,
            style = MaterialTheme.typography.bodyMedium,
            color = DreamTrailColors.ParchmentText
        )
        if (report.pendingMorningEchoTitle != null) {
            Text(
                text = "晨间奇遇待抉择「${report.pendingMorningEchoTitle}」：关闭战报即可在旅程页择选。",
                style = MaterialTheme.typography.bodySmall,
                color = DreamTrailColors.StarlightBlue
            )
        }

        TextButton(
            onClick = { showDetailNotes = !showDetailNotes },
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(if (showDetailNotes) "收起安歇祝福说明" else "安歇祝福说明（+$bonusPctText 已到账）")
        }

        if (showDetailNotes) {
            Text(
                text = if (report.sleepBonusBasisPoints > 300) "已包含基础安歇祝福与睡眠补发，共 $bonusPctText。补发金币 ${report.blessingGold - report.initialBlessingGold}、经验 ${report.blessingXp - report.initialBlessingXp}，已计入上方收获。"
                    else "所有旅人都可获得 +3% 基础安歇祝福，取整余量会保留到后续旅程，不依赖手环或健康权限。",
                style = MaterialTheme.typography.bodySmall,
                color = DreamTrailColors.MutedInk
            )
        }

        Button(
            onClick = onPrimaryAction,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text(primaryActionLabel)
        }
    }
}
