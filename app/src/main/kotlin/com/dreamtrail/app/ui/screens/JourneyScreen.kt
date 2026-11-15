package com.dreamtrail.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dreamtrail.app.data.repository.AppUiDataState
import com.dreamtrail.app.ui.JourneyUiState
import com.dreamtrail.app.ui.components.CampfireTravelerScene
import com.dreamtrail.app.ui.components.CharacterPortraitBox
import com.dreamtrail.app.ui.components.EventBadgeIcon
import com.dreamtrail.app.ui.components.JournalCard
import com.dreamtrail.app.ui.components.ResourceBadgeIcon
import com.dreamtrail.app.ui.components.ResourceSummaryRow
import com.dreamtrail.app.ui.components.ResourceVisualKind
import com.dreamtrail.app.ui.components.SettlementReportCard
import com.dreamtrail.app.ui.components.StrategyEmblem
import com.dreamtrail.app.ui.components.TagBadge
import com.dreamtrail.app.ui.components.TeaCupIconBox
import com.dreamtrail.app.ui.components.WorldMapCanvas
import com.dreamtrail.app.ui.components.companionAccentColor
import com.dreamtrail.app.ui.components.formatDurationZh
import com.dreamtrail.app.ui.components.materialAccentColor
import com.dreamtrail.app.ui.components.regionAccentColor
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.engine.RewardBudgetCalculator
import com.dreamtrail.core.model.ExpeditionStrategy
import com.dreamtrail.core.model.FacilityType
import com.dreamtrail.core.model.PlayerState
import com.dreamtrail.core.model.RegionId
import com.dreamtrail.core.model.RouteDef
import com.dreamtrail.core.model.SettlementResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PrepDockKind {
    COMPANION,
    TEA,
    STRATEGY,
    BEACON
}

@OptIn(ExperimentalLayoutApi::class)
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
    var activePrepDialog by remember { mutableStateOf<PrepDockKind?>(null) }
    var lockedPreviewRoute by remember { mutableStateOf<RouteDef?>(null) }

    val selectedRoute = ContentCatalog.routeById(uiState.selectedRouteId)
        ?: ContentCatalog.routes.first()
    var focusedRegion by rememberSaveable(selectedRoute.id) { mutableStateOf(selectedRoute.regionId) }

    val listState = rememberLazyListState()
    LaunchedEffect(uiState.latestReport?.sessionId) {
        if (uiState.latestReport != null) listState.scrollToItem(0)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "resources") {
            ResourceSummaryRow(resources = dataState.playerState.resources)
        }

        if (uiState.errorMessage != null && uiState.retryKind != null) {
            item(key = "retry_error") {
                JournalCard(
                    containerColor = DreamTrailColors.WarnSurface,
                    borderColor = DreamTrailColors.CoralWarn
                ) {
                    Text(
                        text = "操作未完成",
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
                        Text("重试")
                    }
                }
            }
        }

        // State 1: Active battle report just settled
        if (uiState.latestReport != null && running == null) {
            item(key = "settlement_${uiState.latestReport!!.sessionId}") {
                SettlementReportCard(
                    report = uiState.latestReport!!,
                    workshopLevel = dataState.playerState.facilityLevel(FacilityType.WORKSHOP),
                    primaryActionLabel = "继续准备旅程",
                    onPrimaryAction = onDismissReport
                )
            }
        } else if (running != null) {
            // State 2: Traveling / Active Expedition
            item(key = "active_journey") {
                val activeRoute = ContentCatalog.routeById(running.routeId)
                val preview = uiState.activePreview
                val elapsedSec = preview?.elapsedSeconds ?: 0L
                val canCancelEarly = elapsedSec <= 300L && running.endAtUtcMs == null
                val activeComp = running.snapshot.activeCompanionId?.let { ContentCatalog.companionById(it) }

                JournalCard(
                    containerColor = DreamTrailColors.CardSurface,
                    borderColor = DreamTrailColors.SageMist.copy(alpha = 0.65f)
                ) {
                    CampfireTravelerScene(
                        isTraveling = true,
                        reduceMotion = reduceMotion,
                        regionId = activeRoute?.regionId ?: RegionId.MISTWOOD,
                        companionId = running.snapshot.activeCompanionId,
                        nightPhenomenon = running.nightPhenomenon,
                        height = 136.dp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StrategyEmblem(strategy = running.strategy, size = 38.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "正在冒险 · ${activeRoute?.name ?: running.routeId}",
                                style = MaterialTheme.typography.titleMedium,
                                color = DreamTrailColors.ParchmentText
                            )
                            Text(
                                text = "${activeRoute?.regionId?.displayName ?: ""} · 策略「${running.strategy.displayName}」" +
                                    (activeComp?.let { " · ${it.name}同行" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                        }
                        TagBadge("旅途中", DreamTrailColors.SageMist)
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TagBadge("已出发 ${formatDurationZh(elapsedSec)}", DreamTrailColors.LanternAmber)
                        TagBadge("探路约 ${preview?.estimatedNodes ?: 0} 段", DreamTrailColors.StarlightBlue)
                        running.nightPhenomenon?.let {
                            TagBadge("星象·${it.name}", Color(0xFFB39DFF))
                        }
                    }

                    if (preview?.budgetNotice != null) {
                        Text(
                            text = preview.budgetNotice!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.MutedInk
                        )
                    }

                    Text(
                        text = "现在可以关闭 APP 或锁屏休息。装备、策略已在出发前固定；角色页的新调整会留待下一次旅程生效。",
                        style = MaterialTheme.typography.bodySmall,
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
            // State 3: Ready to depart — Spatial 2D Map + 4-Slot Prep Dock
            val unlockedRouteIds = dataState.discoveryState.unlockedRouteIds
            val remainingBudgetSec = RewardBudgetCalculator.remainingRollingBudgetSeconds(
                dataState.budgetState,
                System.currentTimeMillis() / 1000L
            )
            val player = dataState.playerState
            val pendingEcho = player.pendingMorningEcho?.let { ContentCatalog.morningEchoById(it.echoId) }

            if (pendingEcho != null && uiState.latestReport == null) {
                item(key = "morning_echo_${pendingEcho.id}") {
                    JournalCard(
                        containerColor = DreamTrailColors.CardElevated,
                        borderColor = DreamTrailColors.StarlightBlue.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EventBadgeIcon(
                                isFirstDiscovery = true,
                                title = pendingEcho.title,
                                size = 34.dp
                            )
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

            // Nightly Forecast Strip
            item(key = "night_forecast") {
                val phenom = uiState.nightPhenomenon
                val favoredMatch = phenom.favoredRegionId == selectedRoute.regionId ||
                    phenom.favoredEnvironment == selectedRoute.environmentTag
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DreamTrailColors.CardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (favoredMatch) DreamTrailColors.LanternAmber.copy(alpha = 0.7f) else DreamTrailColors.CardOutline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NightsStay,
                            contentDescription = null,
                            tint = if (favoredMatch) DreamTrailColors.LanternAmber else DreamTrailColors.StarlightBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "今夜星象 · ${phenom.name}",
                                style = MaterialTheme.typography.labelLarge,
                                color = DreamTrailColors.LanternAmber
                            )
                            Text(
                                text = phenom.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            player.activeMorningBuff?.let { buff ->
                                Text(
                                    text = "晨光余韵：${buff.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DreamTrailColors.StarlightBlue
                                )
                            }
                        }
                        TagBadge(
                            text = if (favoredMatch) "契合路线" else phenom.favoredRegionId.displayName,
                            accent = if (favoredMatch) DreamTrailColors.SageMist else DreamTrailColors.MutedInk
                        )
                    }
                }
            }

            // World Map Selector + Interactive 2D Canvas
            item(key = "world_map") {
                val regionRoutes = remember(focusedRegion) {
                    ContentCatalog.routes.filter { it.regionId == focusedRegion }
                }
                val regionInv = dataState.discoveryState.regionInvestigation[focusedRegion.id] ?: 0
                val accent = regionAccentColor(focusedRegion)

                JournalCard(borderColor = accent.copy(alpha = 0.45f)) {
                    // 6-region horizontal switcher pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (region in RegionId.entries) {
                            val rAccent = regionAccentColor(region)
                            val isFocused = region == focusedRegion
                            val isChosenRegion = region == selectedRoute.regionId
                            val anyUnlocked = ContentCatalog.routes.any { it.regionId == region && it.id in unlockedRouteIds }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isFocused) rAccent.copy(alpha = 0.22f) else DreamTrailColors.CardElevated,
                                border = BorderStroke(
                                    width = if (isFocused) 1.5.dp else 1.dp,
                                    color = if (isFocused) rAccent else DreamTrailColors.CardOutline
                                ),
                                modifier = Modifier.heightIn(min = 48.dp).selectable(selected = isFocused, role = Role.Tab) {
                                    focusedRegion = region
                                    lockedPreviewRoute = null
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    if (!anyUnlocked) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = DreamTrailColors.MutedInk,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = region.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isFocused) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk
                                    )
                                    if (isChosenRegion) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = DreamTrailColors.LanternAmber,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2D Route Node Map Canvas
                    WorldMapCanvas(
                        regionId = focusedRegion,
                        regionRoutes = regionRoutes,
                        selectedRouteId = selectedRoute.id,
                        unlockedRouteIds = unlockedRouteIds,
                        regionInvestigation = regionInv,
                        nightPhenomenon = uiState.nightPhenomenon,
                        onSelectRoute = { clicked ->
                            if (clicked.id in unlockedRouteIds) {
                                onSelectRoute(clicked.id)
                                lockedPreviewRoute = null
                            } else {
                                lockedPreviewRoute = clicked
                            }
                        }
                    )

                    lockedPreviewRoute?.let { locked ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DreamTrailColors.CardElevated,
                            border = BorderStroke(1.dp, DreamTrailColors.CoralWarn.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = DreamTrailColors.CoralWarn,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${locked.name} 未解锁：${locked.unlockDescription}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DreamTrailColors.ParchmentText
                                )
                            }
                        }
                    }

                    // Selected destination detail summary
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "今晚去向 · ${selectedRoute.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DreamTrailColors.ParchmentText
                                )
                                Text(
                                    text = "${selectedRoute.regionId.displayName} · ${selectedRoute.environmentTag.displayName} · ${selectedRoute.dangerTag}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DreamTrailColors.LanternAmber
                                )
                            }
                            ContentCatalog.chronicleForRegion(selectedRoute.regionId)?.let { chronicle ->
                                val stageDone = (dataState.discoveryState.chronicleProgress[chronicle.id] ?: 0).coerceIn(0, 3)
                                TagBadge(
                                    text = "秘闻 $stageDone/3",
                                    accent = DreamTrailColors.StarlightBlue
                                )
                            }
                        }

                        Text(
                            text = selectedRoute.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.MutedInk
                        )

                        // Visual hourly yield chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            YieldPill(ResourceVisualKind.GOLD, DreamTrailColors.LanternAmber, "~${selectedRoute.baseGoldPerHour}金/时")
                            YieldPill(ResourceVisualKind.XP, DreamTrailColors.StarlightBlue, "~${selectedRoute.baseXpPerHour}经验/时")
                            YieldPill(
                                ResourceVisualKind.MATERIAL,
                                materialAccentColor(selectedRoute.regionId.materialId),
                                "~${selectedRoute.baseMaterialPerHour}${selectedRoute.regionId.materialName}/时"
                            )
                        }

                        Text(
                            text = "提示：${selectedRoute.recommendationHint}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DreamTrailColors.SageMist
                        )
                    }
                }
            }

            // 4-Slot Visual Prep Dock + Departure CTA + Campfire Scene
            item(key = "departure_prep") {
                val activeComp = player.activeCompanionId?.let { ContentCatalog.companionById(it) }
                val activeTea = player.activeTeaBlendId?.let { ContentCatalog.teaBlendById(it) }
                val teaHearthLv = player.facilityLevel(FacilityType.TEA_HEARTH)

                JournalCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "行前准备",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.ParchmentText
                        )
                        if (player.loadouts.isNotEmpty()) {
                            TextButton(
                                onClick = { activePrepDialog = PrepDockKind.BEACON },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("快捷行装", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // 4-slot horizontal visual dock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrepDockSlot(
                            modifier = Modifier.weight(1f),
                            label = "伙伴",
                            value = activeComp?.name ?: "独行",
                            accent = companionAccentColor(activeComp?.id),
                            onClick = { activePrepDialog = PrepDockKind.COMPANION }
                        ) {
                            CharacterPortraitBox(
                                portraitKey = activeComp?.id,
                                size = 42.dp,
                                accentColor = companionAccentColor(activeComp?.id),
                                locked = activeComp == null
                            )
                        }

                        PrepDockSlot(
                            modifier = Modifier.weight(1f),
                            label = "热茶",
                            value = activeTea?.name ?: if (teaHearthLv > 0) "未选" else "需火塘",
                            accent = if (activeTea != null) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk,
                            onClick = { activePrepDialog = PrepDockKind.TEA }
                        ) {
                            TeaCupIconBox(
                                tea = activeTea,
                                unlocked = teaHearthLv > 0,
                                active = activeTea != null,
                                size = 42.dp
                            )
                        }

                        PrepDockSlot(
                            modifier = Modifier.weight(1f),
                            label = "策略",
                            value = uiState.selectedStrategy.displayName,
                            accent = DreamTrailColors.SageMist,
                            onClick = { activePrepDialog = PrepDockKind.STRATEGY }
                        ) {
                            StrategyEmblem(
                                strategy = uiState.selectedStrategy,
                                size = 42.dp
                            )
                        }

                        val beaconColor = if (player.dreamBeaconTier > 0) Color(0xFFB39DFF) else DreamTrailColors.MutedInk
                        PrepDockSlot(
                            modifier = Modifier.weight(1f),
                            label = "信标",
                            value = if (player.dreamBeaconTier == 0) "常夜" else "阶${player.dreamBeaconTier}",
                            accent = beaconColor,
                            onClick = { activePrepDialog = PrepDockKind.BEACON }
                        ) {
                            DreamBeaconEmblem(tier = player.dreamBeaconTier)
                        }
                    }

                    CampfireTravelerScene(
                        isTraveling = false,
                        reduceMotion = reduceMotion,
                        regionId = selectedRoute.regionId,
                        companionId = player.activeCompanionId,
                        nightPhenomenon = uiState.nightPhenomenon,
                        height = 104.dp
                    )

                    Button(
                        onClick = onStartExpedition,
                        enabled = !uiState.isSubmittingDeparture && !uiState.isSettling && !uiState.isPerformingAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Text(if (uiState.isSubmittingDeparture) "正在记录出发…" else "出发，去休息")
                    }

                    if (!dataState.discoveryState.onboardingCompleted) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DreamTrailColors.CardElevated
                        ) {
                            Text(
                                text = "默认路线已备好，点击「出发，去休息」即可开始。",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.SageMist,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
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
            item(key = "last_report_summary") {
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
            item(key = "history_heading") {
                Text(
                    text = "历史战报",
                    style = MaterialTheme.typography.titleMedium,
                    color = DreamTrailColors.ParchmentText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(dataState.recentSettlements.take(10), key = { it.sessionId }) { hist ->
                val route = ContentCatalog.routeById(hist.routeId)
                val accent = route?.let { regionAccentColor(it.regionId) } ?: DreamTrailColors.LanternAmber
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
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StrategyEmblem(strategy = hist.strategy, size = 34.dp)
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
                        TagBadge("战报", accent)
                    }
                }
            }
        }
    }

    // Modal picker dialog for the 4 prep slots
    activePrepDialog?.let { kind ->
        JourneyPrepModal(
            kind = kind,
            player = dataState.playerState,
            discoveryUnlockedChapters = dataState.discoveryState.companionChapters,
            selectedStrategy = uiState.selectedStrategy,
            onDismiss = { activePrepDialog = null },
            onSelectCompanion = onSelectCompanion,
            onSelectTeaBlend = onSelectTeaBlend,
            onSelectStrategy = onSelectStrategy,
            onSelectDreamBeaconTier = onSelectDreamBeaconTier,
            onApplyLoadout = onApplyLoadout
        )
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
                                workshopLevel = dataState.playerState.facilityLevel(FacilityType.WORKSHOP),
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

@Composable
private fun YieldPill(kind: ResourceVisualKind, accent: Color, text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DreamTrailColors.CardElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ResourceBadgeIcon(kind = kind, accent = accent, size = 14.dp)
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = DreamTrailColors.ParchmentText)
        }
    }
}

@Composable
private fun PrepDockSlot(
    label: String,
    value: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DreamTrailColors.CardElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.48f)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = DreamTrailColors.MutedInk,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = DreamTrailColors.ParchmentText,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DreamBeaconEmblem(tier: Int) {
    val active = tier > 0
    val accent = if (active) Color(0xFFB39DFF) else DreamTrailColors.MutedInk
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) accent.copy(alpha = 0.16f) else DreamTrailColors.NightBackground,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = Modifier.size(42.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension * 0.28f
            val diamond = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r, cy)
                lineTo(cx, cy + r)
                lineTo(cx - r, cy)
                close()
            }
            drawPath(diamond, accent.copy(alpha = if (active) 0.35f else 0.12f))
            drawPath(diamond, accent, style = Stroke(width = 2.dp.toPx()))
            if (active) {
                drawCircle(Color(0xFFFFD875), radius = 3.dp.toPx(), center = Offset(cx, cy))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JourneyPrepModal(
    kind: PrepDockKind,
    player: PlayerState,
    discoveryUnlockedChapters: Map<String, Int>,
    selectedStrategy: ExpeditionStrategy,
    onDismiss: () -> Unit,
    onSelectCompanion: (String?) -> Unit,
    onSelectTeaBlend: (String?) -> Unit,
    onSelectStrategy: (ExpeditionStrategy) -> Unit,
    onSelectDreamBeaconTier: (Int) -> Unit,
    onApplyLoadout: (Int) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DreamTrailColors.CardSurface,
            border = BorderStroke(1.dp, DreamTrailColors.CardOutline),
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val modalTitle = when (kind) {
                    PrepDockKind.COMPANION -> "选择今夜随行伙伴"
                    PrepDockKind.TEA -> "泡一杯睡前热茶"
                    PrepDockKind.STRATEGY -> "切换行进策略"
                    PrepDockKind.BEACON -> "深梦信标与快捷行装"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = DreamTrailColors.LanternAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = modalTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = DreamTrailColors.ParchmentText,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("完成") }
                }
                HorizontalDivider(color = DreamTrailColors.CardOutline)

                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                when (kind) {
                    PrepDockKind.COMPANION -> {
                        val unlockedCompanions = ContentCatalog.companionStories.filter {
                            (discoveryUnlockedChapters[it.id] ?: 0) >= 1
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (player.activeCompanionId == null) DreamTrailColors.LanternAmber.copy(alpha = 0.16f) else DreamTrailColors.CardElevated,
                            border = BorderStroke(1.dp, if (player.activeCompanionId == null) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectCompanion(null)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CharacterPortraitBox(portraitKey = null, size = 44.dp, locked = true)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("独自出发", style = MaterialTheme.typography.titleSmall, color = DreamTrailColors.ParchmentText)
                                    Text("所有已结识伙伴均留守营地自动协理产出", style = MaterialTheme.typography.bodySmall, color = DreamTrailColors.MutedInk)
                                }
                            }
                        }

                        if (unlockedCompanions.isEmpty()) {
                            Text(
                                text = "推进探索并修复旅舍后，可邀请艾琳、诺亚、琳、汐、岑随行或留守营地。",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                        } else {
                            for (comp in unlockedCompanions) {
                                val isActive = player.activeCompanionId == comp.id
                                val accent = companionAccentColor(comp.id)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isActive) accent.copy(alpha = 0.16f) else DreamTrailColors.CardElevated,
                                    border = BorderStroke(1.dp, if (isActive) accent else DreamTrailColors.CardOutline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectCompanion(comp.id)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CharacterPortraitBox(
                                            portraitKey = comp.id,
                                            size = 48.dp,
                                            accentColor = accent,
                                            badgeText = if (isActive) "随行" else null
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${comp.name} · ${comp.combatSkillName}", style = MaterialTheme.typography.titleSmall, color = DreamTrailColors.ParchmentText)
                                            Text(comp.combatRoleSummary, style = MaterialTheme.typography.bodySmall, color = DreamTrailColors.SageMist)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PrepDockKind.TEA -> {
                        val teaHearthLv = player.facilityLevel(FacilityType.TEA_HEARTH)
                        val availableTeas = ContentCatalog.teaBlends.filter { teaHearthLv >= it.requiredTeaHearthLevel }
                        if (availableTeas.isEmpty()) {
                            Text(
                                text = "在营地修葺「茶歇火塘」Lv.1 后即可泡制助眠热茶。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DreamTrailColors.MutedInk
                            )
                        } else {
                            for (tea in availableTeas) {
                                val selected = player.activeTeaBlendId == tea.id
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) DreamTrailColors.LanternAmber.copy(alpha = 0.18f) else DreamTrailColors.CardElevated,
                                    border = BorderStroke(1.dp, if (selected) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectTeaBlend(if (selected) null else tea.id)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        TeaCupIconBox(tea = tea, unlocked = true, active = selected, size = 44.dp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tea.name, style = MaterialTheme.typography.titleSmall, color = DreamTrailColors.ParchmentText)
                                            if (selected) TagBadge("已选热茶", DreamTrailColors.LanternAmber)
                                            Text(tea.subtitle, style = MaterialTheme.typography.bodySmall, color = DreamTrailColors.MutedInk)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PrepDockKind.STRATEGY -> {
                        for (strategy in ExpeditionStrategy.entries) {
                            val selected = strategy == selectedStrategy
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) DreamTrailColors.LanternAmber.copy(alpha = 0.16f) else DreamTrailColors.CardElevated,
                                border = BorderStroke(1.dp, if (selected) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectStrategy(strategy)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StrategyEmblem(strategy = strategy, size = 42.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(strategy.displayName, style = MaterialTheme.typography.titleSmall, color = DreamTrailColors.ParchmentText)
                                        Text(strategy.summary, style = MaterialTheme.typography.bodySmall, color = DreamTrailColors.MutedInk)
                                    }
                                    if (selected) TagBadge("当前", DreamTrailColors.LanternAmber)
                                }
                            }
                        }
                    }

                    PrepDockKind.BEACON -> {
                        Text(
                            text = "深梦信标挑战阶位（敌人更强，金币/素材与印记掉率提升）",
                            style = MaterialTheme.typography.bodySmall,
                            color = DreamTrailColors.MutedInk
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (tier in 0..4) {
                                val selected = player.dreamBeaconTier == tier
                                OutlinedButton(
                                    onClick = {
                                        onSelectDreamBeaconTier(tier)
                                        onDismiss()
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 46.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) DreamTrailColors.LanternAmber else DreamTrailColors.CardElevated,
                                        contentColor = if (selected) DreamTrailColors.LanternInk else DreamTrailColors.ParchmentText
                                    )
                                ) {
                                    Text(if (tier == 0) "常夜" else "阶$tier", maxLines = 1)
                                }
                            }
                        }

                        if (player.loadouts.isNotEmpty()) {
                            HorizontalDivider(color = DreamTrailColors.CardOutline)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = DreamTrailColors.StarlightBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "快捷行装方案（一键套用配装、被动与伙伴）",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DreamTrailColors.ParchmentText
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (slotIdx in 0..2) {
                                    val preset = player.loadouts.firstOrNull { it.slotIndex == slotIdx }
                                    if (preset != null) {
                                        OutlinedButton(
                                            onClick = {
                                                onApplyLoadout(slotIdx)
                                                onDismiss()
                                            },
                                            modifier = Modifier.heightIn(min = 46.dp)
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
            }
        }
    }
}
