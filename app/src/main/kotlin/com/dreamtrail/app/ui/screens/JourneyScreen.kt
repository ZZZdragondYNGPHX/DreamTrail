package com.dreamtrail.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dreamtrail.app.data.repository.AppUiDataState
import com.dreamtrail.app.ui.JourneyUiState
import com.dreamtrail.app.ui.components.*
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.RewardBudgetCalculator
import com.dreamtrail.core.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JourneyScreen(
    dataState: AppUiDataState,
    uiState: JourneyUiState,
    onSelectRoute: (String) -> Unit,
    onSelectStrategy: (ExpeditionStrategy) -> Unit,
    onStartExpedition: () -> Unit,
    onCancelEarly: (String) -> Unit,
    onEndExpedition: (String) -> Unit,
    onDismissReport: () -> Unit,
    onOpenHistoryReport: (SettlementResult?) -> Unit,
    onRetry: () -> Unit,
    onSelectCompanion: (String?) -> Unit = {},
    onSelectTeaBlend: (String?) -> Unit = {},
    onSelectDreamBeaconTier: (Int) -> Unit = {},
    onResolveMorningEcho: (Boolean) -> Unit = {},
    onApplyLoadout: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val running = dataState.runningSession
    val reduceMotion = dataState.settings.reduceMotion
    var showRoutePicker by remember { mutableStateOf(false) }
    var showPrepPanel by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ResourceSummaryRow(resources = dataState.playerState.resources)
        }

        if (uiState.errorMessage != null) {
            item {
                JournalCard(
                    containerColor = DreamTrailColors.WarnSurface,
                    borderColor = DreamTrailColors.CoralWarn
                ) {
                    Text(
                        text = "保存未完成",
                        style = MaterialTheme.typography.titleMedium,
                        color = DreamTrailColors.CoralWarn
                    )
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DreamTrailColors.ParchmentText
                    )
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("重试保存")
                    }
                }
            }
        }

        if (uiState.statusNotice != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DreamTrailColors.CardElevated,
                    border = BorderStroke(1.dp, DreamTrailColors.SageMist.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = uiState.statusNotice!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DreamTrailColors.SageMist,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }
        }

        // State 1: Active battle report just settled (no secondary "Claim All" step)
        if (uiState.latestReport != null && running == null) {
            item {
                SettlementReportCard(
                    report = uiState.latestReport!!,
                    primaryActionLabel = "返回营地，准备下次旅程",
                    onPrimaryAction = onDismissReport
                )
            }
        } else if (running != null) {
            // State 2: Traveling / Active Expedition
            item {
                val activeRoute = ContentCatalog.routeById(running.routeId)
                val preview = uiState.activePreview
                val elapsedSec = preview?.elapsedSeconds ?: 0L
                val canCancelEarly = elapsedSec <= 300L && running.endAtUtcMs == null

                JournalCard(
                    containerColor = DreamTrailColors.CardSurface,
                    borderColor = DreamTrailColors.SageMist.copy(alpha = 0.6f)
                ) {
                    CampfireTravelerScene(isTraveling = true, reduceMotion = reduceMotion)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "正在冒险 · ${activeRoute?.name ?: running.routeId}",
                                style = MaterialTheme.typography.titleLarge,
                                color = DreamTrailColors.ParchmentText
                            )
                            Text(
                                text = "${activeRoute?.regionId?.displayName ?: ""} · 策略「${running.strategy.displayName}」",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                        }
                        TagBadge("旅途中", DreamTrailColors.SageMist)
                    }

                    Text(
                        text = "已出发时间：${formatDurationZh(elapsedSec)}（已完成约 ${preview?.estimatedNodes ?: 0} 段探路）",
                        style = MaterialTheme.typography.bodyLarge,
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
                        text = "现在可以关闭 APP 或锁屏休息。装备、策略已在出发前固定；角色页的新调整会留待下一次旅程生效。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DreamTrailColors.MutedInk
                    )

                    if (uiState.isSettling) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = DreamTrailColors.LanternAmber
                            )
                            Text(
                                text = "正在整理本次收获，请稍候…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DreamTrailColors.ParchmentText
                            )
                        }
                    } else {
                        Button(
                            onClick = { onEndExpedition(running.sessionId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                        ) {
                            Text("结束冒险并查看战报")
                        }

                        if (canCancelEarly) {
                            OutlinedButton(
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
        } else {
            // State 3: Ready to depart
            val selectedRoute = ContentCatalog.routeById(uiState.selectedRouteId)
                ?: ContentCatalog.routes.first()
            val unlockedRoutes = ContentCatalog.routes.filter { it.id in dataState.discoveryState.unlockedRouteIds }
            val remainingBudgetSec = RewardBudgetCalculator.remainingRollingBudgetSeconds(
                dataState.budgetState,
                System.currentTimeMillis() / 1000L
            )
            val player = dataState.playerState
            val pendingEcho = player.pendingMorningEcho?.let { ContentCatalog.morningEchoById(it.echoId) }

            if (pendingEcho != null && uiState.latestReport == null) {
                item {
                    JournalCard(
                        containerColor = DreamTrailColors.CardElevated,
                        borderColor = DreamTrailColors.StarlightBlue.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "晨间奇遇 · ${pendingEcho.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = DreamTrailColors.ParchmentText,
                                modifier = Modifier.weight(1f)
                            )
                            TagBadge("小抉择", DreamTrailColors.StarlightBlue)
                        }
                        Text(
                            text = pendingEcho.prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DreamTrailColors.MutedInk
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onResolveMorningEcho(true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text(pendingEcho.choiceA.label)
                            }
                            OutlinedButton(
                                onClick = { onResolveMorningEcho(false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text(pendingEcho.choiceB.label)
                            }
                        }
                    }
                }
            }

            item {
                JournalCard {
                    // Nightly Forecast Strip
                    val phenom = uiState.nightPhenomenon
                    val favoredMatch = phenom.favoredRegionId == selectedRoute.regionId ||
                        phenom.favoredEnvironment == selectedRoute.environmentTag
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DreamTrailColors.CardElevated,
                        border = BorderStroke(
                            1.dp,
                            if (favoredMatch) DreamTrailColors.LanternAmber.copy(alpha = 0.65f) else DreamTrailColors.CardOutline
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "今夜星象 · ${phenom.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DreamTrailColors.LanternAmber,
                                    modifier = Modifier.weight(1f)
                                )
                                TagBadge(
                                    text = if (favoredMatch) "契合当前路线" else "偏向${phenom.favoredRegionId.displayName}",
                                    accent = if (favoredMatch) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                                )
                            }
                            Text(
                                text = phenom.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                            player.activeMorningBuff?.let { buff ->
                                Text(
                                    text = "晨光余韵生效中：${buff.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DreamTrailColors.StarlightBlue
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "今晚去向 · ${selectedRoute.name}",
                                style = MaterialTheme.typography.titleLarge,
                                color = DreamTrailColors.ParchmentText
                            )
                            Text(
                                text = "${selectedRoute.regionId.displayName} · ${selectedRoute.dangerTag}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.LanternAmber
                            )
                        }
                        TextButton(
                            onClick = { showRoutePicker = !showRoutePicker },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(if (showRoutePicker) "收起路线" else "更换路线")
                        }
                    }

                    if (showRoutePicker) {
                        HorizontalDivider(color = DreamTrailColors.CardOutline)
                        Text(
                            text = "可选路线（已解锁 ${unlockedRoutes.size}/${ContentCatalog.routes.size}）",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.ParchmentText
                        )
                        for (route in ContentCatalog.routes) {
                            val unlocked = route.id in dataState.discoveryState.unlockedRouteIds
                            val isSelected = route.id == selectedRoute.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) DreamTrailColors.CardElevated else DreamTrailColors.NightBackground,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = unlocked) {
                                        onSelectRoute(route.id)
                                        showRoutePicker = false
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${route.regionId.displayName} · ${route.name}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (unlocked) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                                        )
                                        TagBadge(
                                            text = if (isSelected) "当前选择" else if (unlocked) route.environmentTag.displayName else "未解锁",
                                            accent = if (isSelected) DreamTrailColors.LanternAmber else if (unlocked) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                                        )
                                    }
                                    Text(
                                        text = if (unlocked) route.dangerTag else "解锁条件：${route.unlockDescription}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DreamTrailColors.MutedInk
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DreamTrailColors.CardOutline)

                    Text(
                        text = "行进策略",
                        style = MaterialTheme.typography.titleSmall,
                        color = DreamTrailColors.ParchmentText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (strategy in ExpeditionStrategy.entries) {
                            val selected = strategy == uiState.selectedStrategy
                            OutlinedButton(
                                onClick = { onSelectStrategy(strategy) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) DreamTrailColors.LanternAmber else DreamTrailColors.CardElevated,
                                    contentColor = if (selected) DreamTrailColors.LanternInk else DreamTrailColors.ParchmentText
                                )
                            ) {
                                Text(strategy.displayName, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Text(
                        text = uiState.selectedStrategy.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.MutedInk
                    )

                    val unlockedCompanions = ContentCatalog.companionStories.filter {
                        (dataState.discoveryState.companionChapters[it.id] ?: 0) >= 1
                    }
                    val activeComp = player.activeCompanionId?.let { ContentCatalog.companionById(it) }
                    val teaHearthLv = player.facilityLevel(FacilityType.TEA_HEARTH)
                    val activeTea = player.activeTeaBlendId?.let { ContentCatalog.teaBlendById(it) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val prepSummary = buildList {
                            add(activeComp?.let { "随行:${it.name}" } ?: "独自出发")
                            activeTea?.let { add("茶:${it.name}") }
                            if (player.dreamBeaconTier > 0) add("信标阶${player.dreamBeaconTier}")
                        }.joinToString(" · ")
                        Text(
                            text = "行前整备：$prepSummary",
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.StarlightBlue,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { showPrepPanel = !showPrepPanel },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(if (showPrepPanel) "收起整备" else "调整随行/热茶/信标")
                        }
                    }

                    if (showPrepPanel) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DreamTrailColors.CardElevated,
                            border = BorderStroke(1.dp, DreamTrailColors.CardOutline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "随行伙伴（其余已解锁伙伴自动留守营地协理产出）",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DreamTrailColors.ParchmentText
                                )
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onSelectCompanion(null) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.heightIn(min = 48.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (player.activeCompanionId == null) DreamTrailColors.LanternAmber else DreamTrailColors.CardSurface,
                                            contentColor = if (player.activeCompanionId == null) DreamTrailColors.LanternInk else DreamTrailColors.ParchmentText
                                        )
                                    ) {
                                        Text("独自", maxLines = 1)
                                    }
                                    for (comp in unlockedCompanions) {
                                        val isActive = player.activeCompanionId == comp.id
                                        OutlinedButton(
                                            onClick = { onSelectCompanion(comp.id) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.heightIn(min = 48.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isActive) DreamTrailColors.LanternAmber else DreamTrailColors.CardSurface,
                                                contentColor = if (isActive) DreamTrailColors.LanternInk else DreamTrailColors.ParchmentText
                                            )
                                        ) {
                                            Text(comp.name, maxLines = 1)
                                        }
                                    }
                                }
                                if (unlockedCompanions.isEmpty()) {
                                    Text(
                                        text = "推进探索并修复旅舍后，可邀请艾琳、诺亚、琳、汐、岑随行或留守营地。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DreamTrailColors.MutedInk
                                    )
                                } else if (activeComp != null) {
                                    Text(
                                        text = "战技「${activeComp.combatSkillName}」：${activeComp.combatRoleSummary}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DreamTrailColors.SageMist
                                    )
                                }

                                HorizontalDivider(color = DreamTrailColors.CardOutline)

                                Text(
                                    text = "睡前热茶（茶歇火塘 Lv.$teaHearthLv）",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DreamTrailColors.ParchmentText
                                )
                                val availableTeas = ContentCatalog.teaBlends.filter { teaHearthLv >= it.requiredTeaHearthLevel }
                                if (availableTeas.isEmpty()) {
                                    Text(
                                        text = "在营地修葺「茶歇火塘」Lv.1 后即可选择助眠增益热茶。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DreamTrailColors.MutedInk
                                    )
                                } else {
                                    for (tea in availableTeas) {
                                        val selected = player.activeTeaBlendId == tea.id
                                        OutlinedButton(
                                            onClick = { onSelectTeaBlend(if (selected) null else tea.id) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (selected) DreamTrailColors.LanternAmber else DreamTrailColors.CardSurface,
                                                contentColor = if (selected) DreamTrailColors.LanternInk else DreamTrailColors.ParchmentText
                                            )
                                        ) {
                                            Text("${tea.name} · ${tea.subtitle}")
                                        }
                                    }
                                }

                                HorizontalDivider(color = DreamTrailColors.CardOutline)

                                Text(
                                    text = "深梦信标（挑战倍率：敌人变强，收益与印记提高）",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DreamTrailColors.ParchmentText
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (tier in 0..4) {
                                        val selected = player.dreamBeaconTier == tier
                                        OutlinedButton(
                                            onClick = { onSelectDreamBeaconTier(tier) },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (selected) DreamTrailColors.LanternAmber else DreamTrailColors.CardSurface,
                                                contentColor = if (selected) DreamTrailColors.LanternInk else DreamTrailColors.ParchmentText
                                            )
                                        ) {
                                            Text(if (tier == 0) "常夜" else "阶$tier", maxLines = 1)
                                        }
                                    }
                                }
                                if (player.loadouts.isNotEmpty()) {
                                    HorizontalDivider(color = DreamTrailColors.CardOutline)
                                    Text(
                                        text = "快捷行装方案（一键应用配装、策略与伙伴）",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = DreamTrailColors.ParchmentText
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        for (slotIdx in 0..2) {
                                            val preset = player.loadouts.firstOrNull { it.slotIndex == slotIdx }
                                            if (preset != null) {
                                                OutlinedButton(
                                                    onClick = { onApplyLoadout(slotIdx) },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                ) {
                                                    Text(preset.name, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onStartExpedition,
                        enabled = !uiState.isSubmittingDeparture && !uiState.isSettling,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Text(if (uiState.isSubmittingDeparture) "正在记录出发…" else "出发，去休息")
                    }

                    CampfireTravelerScene(isTraveling = false, reduceMotion = reduceMotion)

                    if (!dataState.discoveryState.onboardingCompleted) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DreamTrailColors.CardElevated
                        ) {
                            Text(
                                text = "默认路线已备好，点击「出发，去休息」即可开始。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DreamTrailColors.SageMist,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Text(
                        text = selectedRoute.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DreamTrailColors.ParchmentText
                    )
                    Text(
                        text = "提示：${selectedRoute.recommendationHint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.MutedInk
                    )
                    Text(
                        text = "基础每有效小时（事件另计）：~${selectedRoute.baseGoldPerHour} 金币 / ~${selectedRoute.baseXpPerHour} 经验 / ~${selectedRoute.baseMaterialPerHour} ${selectedRoute.regionId.materialName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.SageMist
                    )

                    ContentCatalog.chronicleForRegion(selectedRoute.regionId)?.let { chronicle ->
                        val stageDone = (dataState.discoveryState.chronicleProgress[chronicle.id] ?: 0).coerceIn(0, 3)
                        val nextStage = chronicle.stages.firstOrNull { it.stageIndex == stageDone + 1 }
                        val stageText = if (stageDone >= 3) {
                            "秘闻「${chronicle.title}」已全数揭示（${chronicle.completionTitle}）"
                        } else {
                            "秘闻「${chronicle.title}」进度 $stageDone/3 · 下一章需调查度${nextStage?.requiredInvestigation ?: 0}"
                        }
                        Text(
                            text = stageText,
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.StarlightBlue
                        )
                    }

                    if (remainingBudgetSec <= 3600L) {
                        Text(
                            text = if (remainingBudgetSec == 0L) {
                                "伙伴今日已完成主要探索，继续出发将在营地附近休整，待额度释放后恢复收益。"
                            } else {
                                "今日主要探索额度还剩约 ${formatDurationZh(remainingBudgetSec)}，超出部分会转为营地休整。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.LanternAmber
                        )
                    }

                }
            }
        }

        // Last discovery hint when idle and no active report open
        val lastReport = dataState.recentSettlements.firstOrNull()
        if (running == null && uiState.latestReport == null && lastReport != null) {
            item {
                JournalCard(containerColor = DreamTrailColors.CardElevated) {
                    Text(
                        text = "上次归来摘要",
                        style = MaterialTheme.typography.titleSmall,
                        color = DreamTrailColors.LanternAmber
                    )
                    Text(
                        text = lastReport.keyEvents.firstOrNull()?.let { "「${it.title}」：${it.detail}" }
                            ?: "上一次旅程共收获 ${lastReport.baseGold + lastReport.blessingGold} 金币。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DreamTrailColors.ParchmentText
                    )
                    Text(
                        text = "建议：${lastReport.nextStepSuggestion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DreamTrailColors.SageMist
                    )
                }
            }
        }

        // Historical reports section
        if (dataState.recentSettlements.isNotEmpty()) {
            item {
                Text(
                    text = "历史战报（点击展开重看）",
                    style = MaterialTheme.typography.titleMedium,
                    color = DreamTrailColors.ParchmentText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(dataState.recentSettlements.take(10), key = { it.sessionId }) { hist ->
                val route = ContentCatalog.routeById(hist.routeId)
                val dateFmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DreamTrailColors.CardSurface,
                    border = BorderStroke(1.dp, DreamTrailColors.CardOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenHistoryReport(hist) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${route?.name ?: hist.routeId} · ${hist.strategy.displayName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = DreamTrailColors.ParchmentText
                            )
                            Text(
                                text = "${dateFmt.format(Date(hist.endAtUtcMs))} · ${formatDurationZh(hist.rawDurationSeconds)} · 金币 +${hist.baseGold + hist.blessingGold}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                        }
                        Text(
                            text = "查看",
                            style = MaterialTheme.typography.labelLarge,
                            color = DreamTrailColors.LanternAmber
                        )
                    }
                }
            }
        }
    }

    val detail = uiState.historyDetailReport
    if (detail != null) {
        AlertDialog(
            onDismissRequest = { onOpenHistoryReport(null) },
            containerColor = DreamTrailColors.NightBackground,
            text = {
                Box(modifier = Modifier.heightIn(max = 520.dp)) {
                    LazyColumn {
                        item {
                            SettlementReportCard(
                                report = detail,
                                primaryActionLabel = "关闭战报",
                                onPrimaryAction = { onOpenHistoryReport(null) }
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}
