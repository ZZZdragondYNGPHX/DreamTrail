package com.dreamtrail.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.model.EquipmentInstance
import com.dreamtrail.core.model.RegionId
import com.dreamtrail.core.model.SettlementResult
import com.dreamtrail.core.model.SouvenirDef

/**
 * Three-layer settlement battle report with 2D RPG loot chest grid & event badges:
 * 1. Harvest (totals & 2D chest tiles)
 * 2. What happened (up to 3 key events + merged ordinary counts)
 * 3. Next step suggestion + single return button (no secondary claim dialog)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettlementReportCard(
    report: SettlementResult,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    workshopLevel: Int = 0
) {
    val route = remember(report.routeId) { ContentCatalog.routeById(report.routeId) }
    val regionId = route?.regionId ?: RegionId.MISTWOOD
    val companionDef = remember(report.activeCompanionName) {
        report.activeCompanionName?.let { name ->
            ContentCatalog.companionStories.firstOrNull { it.name == name }
        }
    }
    var showDetailNotes by remember(report.sessionId) { mutableStateOf(false) }
    var inspectedEquip by remember(report.sessionId) { mutableStateOf<EquipmentInstance?>(null) }
    var inspectedSouvenir by remember(report.sessionId) { mutableStateOf<SouvenirDef?>(null) }

    JournalCard(
        modifier = modifier,
        containerColor = DreamTrailColors.CardSurface,
        borderColor = DreamTrailColors.LanternAmber.copy(alpha = 0.55f)
    ) {
        // Illustrated scenic report banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRegionBackdrop(regionId = regionId, phase = 0.25f, reduceMotion = true)
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0x550C111B),
                            Color(0xCC0D131E)
                        )
                    )
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (companionDef != null) {
                    CharacterPortraitBox(
                        portraitKey = companionDef.id,
                        size = 56.dp,
                        accentColor = companionAccentColor(companionDef.id)
                    )
                } else {
                    StrategyEmblem(strategy = report.strategy, size = 46.dp)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "旅程战报 · ${route?.name ?: report.routeId}",
                        style = MaterialTheme.typography.titleMedium,
                        color = DreamTrailColors.ParchmentText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatDurationZh(report.rawDurationSeconds)} · ${report.completedNodes} 段探索 · ${report.strategy.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.MutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TagBadge(text = "已入账", accent = DreamTrailColors.SageMist)
            }
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

        // Layer 1: Harvest 2D Loot Chest
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "一、本次战利箱",
                style = MaterialTheme.typography.titleSmall,
                color = DreamTrailColors.LanternAmber
            )
            if (report.leveledUpTo > report.leveledUpFrom) {
                TagBadge(
                    text = "角色升级 Lv.${report.leveledUpFrom} → Lv.${report.leveledUpTo}",
                    accent = DreamTrailColors.LanternAmber
                )
            }
        }

        val totalGold = report.baseGold + report.blessingGold
        val totalXp = report.baseXp + report.blessingXp
        val bonusPctText = java.math.BigDecimal(report.sleepBonusBasisPoints)
            .movePointLeft(2)
            .stripTrailingZeros()
            .toPlainString() + "%"

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LootCurrencyCell(
                kind = ResourceVisualKind.GOLD,
                accent = DreamTrailColors.LanternAmber,
                title = "+$totalGold 金币",
                subtitle = if (report.blessingGold > 0L) "含祝福 +${report.blessingGold}" else "旅行收益"
            )
            LootCurrencyCell(
                kind = ResourceVisualKind.XP,
                accent = DreamTrailColors.StarlightBlue,
                title = "+$totalXp 经验",
                subtitle = if (report.blessingXp > 0L) "含祝福 +${report.blessingXp}" else "旅途历练"
            )
            for ((matId, amt) in report.materialsGained) {
                if (amt <= 0L) continue
                val matName = RegionId.entries.firstOrNull { it.materialId == matId }?.materialName ?: matId
                LootCurrencyCell(
                    kind = ResourceVisualKind.MATERIAL,
                    accent = materialAccentColor(matId),
                    title = "+$amt $matName",
                    subtitle = "区域素材"
                )
            }
            if (report.bossMarksGained > 0L) {
                LootCurrencyCell(
                    kind = ResourceVisualKind.BOSS_MARK,
                    accent = DreamTrailColors.CoralWarn,
                    title = "+${report.bossMarksGained} 首领印记",
                    subtitle = "首领战利"
                )
            }
        }

        val journeyTags = buildList {
            report.nightPhenomenonName?.let { add("夜象：$it" to DreamTrailColors.StarlightBlue) }
            report.activeCompanionName?.let { add("随行：$it" to DreamTrailColors.SageMist) }
            report.teaBlendName?.let { add("热茶：$it" to DreamTrailColors.LanternAmber) }
            if (report.dreamBeaconTier > 0) add("深梦信标 阶${report.dreamBeaconTier}" to Color(0xFFB39DFF))
            if (report.eliteVictories > 0) add("精英击败 ${report.eliteVictories}" to DreamTrailColors.CoralWarn)
        }
        if (journeyTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for ((label, accent) in journeyTags) {
                    TagBadge(text = label, accent = accent)
                }
            }
        }

        if (report.newEquipment.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DreamTrailColors.CardElevated,
                border = BorderStroke(1.dp, DreamTrailColors.CardOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "新获装备（${report.newEquipment.size} 件，点击查看详情）",
                        style = MaterialTheme.typography.labelLarge,
                        color = DreamTrailColors.ParchmentText
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (eq in report.newEquipment) {
                            val def = ContentCatalog.equipmentById(eq.defId) ?: continue
                            EquipmentIconTile(
                                def = def,
                                level = eq.level,
                                affix = eq.affix,
                                secondaryAffix = eq.secondaryAffix,
                                isSelected = inspectedEquip?.instanceId == eq.instanceId,
                                tileSize = 62.dp,
                                onClick = {
                                    inspectedEquip = if (inspectedEquip?.instanceId == eq.instanceId) null else eq
                                }
                            )
                        }
                    }
                    inspectedEquip?.let { selected ->
                        val def = ContentCatalog.equipmentById(selected.defId)
                        if (def != null) {
                            val stats = ContentCatalog.effectiveEquipmentStats(def, selected.level, workshopLevel = workshopLevel)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DreamTrailColors.CardSurface,
                                border = BorderStroke(1.dp, tierAccentColor(def.tier).copy(alpha = 0.55f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "${def.name} · T${def.tier} ${def.slot.displayName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = tierAccentColor(def.tier)
                                    )
                                    Text(
                                        text = "词条「${selected.affix.displayName}」：${selected.affix.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DreamTrailColors.StarlightBlue
                                    )
                                    Text(
                                        text = "装备属性：生命+${stats.maxHp} 攻击+${stats.attack} 防御+${stats.defense} 速+${stats.speed}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DreamTrailColors.MutedInk
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (report.newSouvenirIds.isNotEmpty()) {
            val souvenirs = report.newSouvenirIds.mapNotNull { ContentCatalog.souvenirById(it) }
            if (souvenirs.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DreamTrailColors.CardElevated,
                    border = BorderStroke(1.dp, DreamTrailColors.LanternAmber.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "风物志新收录（${souvenirs.size} 件）",
                            style = MaterialTheme.typography.labelLarge,
                            color = DreamTrailColors.LanternAmber
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (souv in souvenirs) {
                                SouvenirIconTile(
                                    souvenir = souv,
                                    unlocked = true,
                                    onClick = {
                                        inspectedSouvenir = if (inspectedSouvenir?.id == souv.id) null else souv
                                    }
                                )
                            }
                        }
                        val focused = inspectedSouvenir ?: souvenirs.first()
                        val souvBonuses = buildList {
                            if (focused.bonusStats.maxHp > 0) add("生命+${focused.bonusStats.maxHp}")
                            if (focused.bonusStats.attack > 0) add("攻击+${focused.bonusStats.attack}")
                            if (focused.bonusStats.defense > 0) add("防护+${focused.bonusStats.defense}")
                            if (focused.bonusStats.speed > 0) add("速度+${focused.bonusStats.speed}")
                            if (focused.goldBonusPercent > 0) add("金币+${focused.goldBonusPercent}%")
                            if (focused.xpBonusPercent > 0) add("经验+${focused.xpBonusPercent}%")
                            if (focused.materialBonusPercent > 0) add("材料+${focused.materialBonusPercent}%")
                        }.joinToString(" · ")
                        Text(
                            text = "${focused.name}（$souvBonuses）：${focused.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.MutedInk
                        )
                    }
                }
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
                    color = DreamTrailColors.CardElevated,
                    border = BorderStroke(1.dp, DreamTrailColors.CardOutline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        EventBadgeIcon(
                            isFirstDiscovery = ev.isFirstDiscovery,
                            title = ev.title,
                            size = 34.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
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
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                            if (ev.highlightReason != null) {
                                Text(
                                    text = "影响：${ev.highlightReason}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DreamTrailColors.SageMist
                                )
                            }
                        }
                    }
                }
            }

            if (report.mergedActivities.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (act in report.mergedActivities) {
                        TagBadge(text = act.label, accent = DreamTrailColors.MutedInk)
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
                text = if (report.sleepBonusBasisPoints > 300) {
                    "已包含基础安歇祝福与睡眠补发，共 $bonusPctText。补发金币 ${report.blessingGold - report.initialBlessingGold}、经验 ${report.blessingXp - report.initialBlessingXp}，已计入上方收获。"
                } else {
                    "所有旅人都可获得 +3% 基础安歇祝福，取整余量会保留到后续旅程，不依赖手环或健康权限。"
                },
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

@Composable
private fun LootCurrencyCell(
    kind: ResourceVisualKind,
    accent: Color,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DreamTrailColors.CardElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier.widthIn(min = 128.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResourceBadgeIcon(kind = kind, accent = accent, size = 24.dp)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = DreamTrailColors.ParchmentText,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = DreamTrailColors.MutedInk
                )
            }
        }
    }
}
