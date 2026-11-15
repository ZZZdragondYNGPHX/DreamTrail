package com.dreamtrail.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.content.ContentCatalog
import com.dreamtrail.core.model.*
import kotlin.math.roundToInt

/**
 * 2D 区域节点地图画布：绘制区域地貌、蜿蜒旅途虚线与 3 个可点击路线节点，支持直接点击切换路线。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorldMapCanvas(
    regionId: RegionId,
    regionRoutes: List<RouteDef>,
    selectedRouteId: String,
    unlockedRouteIds: Set<String>,
    regionInvestigation: Int,
    nightPhenomenon: NightPhenomenon?,
    onSelectRoute: (RouteDef) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = regionAccentColor(regionId)
    val nodeFractions = listOf(
        0.18f to 0.65f,
        0.50f to 0.42f,
        0.81f to 0.30f
    )
    val boss = regionRoutes.lastOrNull()?.let { ContentCatalog.enemyById(it.bossId) }
    val bossReq = (regionRoutes.firstOrNull { it.id == selectedRouteId } ?: regionRoutes.firstOrNull())?.bossInvestigationRequired ?: 6

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        color = DreamTrailColors.CardSurface
    ) {
        Column {
            FlowRow(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TagBadge("${regionId.displayName} · 调查度 $regionInvestigation", accent)
                if (boss != null) {
                    val bossReady = regionInvestigation >= bossReq
                    TagBadge(
                        if (bossReady) "首领：${boss.name}" else "${boss.name} · 需调查 $bossReq",
                        if (bossReady) DreamTrailColors.CoralWarn else DreamTrailColors.MutedInk
                    )
                }
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(216.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))
            ) {
                val nodeWidth = (maxWidth * 0.28f).coerceAtMost(92.dp)
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRegionBackdrop(regionId, reduceMotion = true, nightPhenomenon = nightPhenomenon)

                    // Cartographic parchment border lines
                    drawRoundRect(
                        color = accent.copy(alpha = 0.25f),
                        topLeft = Offset(10f, 10f),
                        size = Size(size.width - 20f, size.height - 20f),
                        cornerRadius = CornerRadius(14f, 14f),
                        style = Stroke(1.2f)
                    )

                    // Connecting winding trail between the 3 route nodes
                    if (regionRoutes.size >= 2) {
                        for (i in 0 until minOf(regionRoutes.size - 1, nodeFractions.size - 1)) {
                            val (x1f, y1f) = nodeFractions[i]
                            val (x2f, y2f) = nodeFractions[i + 1]
                            val p1 = Offset(size.width * x1f, size.height * y1f)
                            val p2 = Offset(size.width * x2f, size.height * y2f)
                            val unlockedNext = regionRoutes[i + 1].id in unlockedRouteIds
                            val path = Path().apply {
                                moveTo(p1.x, p1.y)
                                quadraticTo(
                                    (p1.x + p2.x) * 0.5f,
                                    minOf(p1.y, p2.y) - 18f,
                                    p2.x,
                                    p2.y
                                )
                            }
                            drawPath(
                                path = path,
                                color = if (unlockedNext) accent.copy(alpha = 0.85f) else DreamTrailColors.MutedInk.copy(alpha = 0.32f),
                                style = Stroke(
                                    width = if (unlockedNext) 4f else 2.5f,
                                    cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                                )
                            )
                        }
                    }
                }

                // Interactive Route Nodes
                regionRoutes.take(3).forEachIndexed { index, route ->
                    val (xf, yf) = nodeFractions[index]
                    val unlocked = route.id in unlockedRouteIds
                    val isSelected = route.id == selectedRouteId
                    val nodeHalfW = with(density) { (nodeWidth / 2).toPx() }
                    val nodeHalfH = with(density) { 34.dp.toPx() }
                    val offsetX = (widthPx * xf - nodeHalfW).roundToInt().coerceIn(4, (widthPx - nodeHalfW * 2).roundToInt().coerceAtLeast(4))
                    val offsetY = (heightPx * yf - nodeHalfH).roundToInt().coerceIn(34, (heightPx - nodeHalfH * 2).roundToInt().coerceAtLeast(34))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .width(nodeWidth)
                            .heightIn(min = 48.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = route.name
                                stateDescription = if (unlocked) "已解锁" else "未解锁，${route.unlockDescription}"
                            }
                            .selectable(selected = isSelected, role = Role.RadioButton) { onSelectRoute(route) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> DreamTrailColors.LanternAmber
                                        unlocked -> DreamTrailColors.CardElevated
                                        else -> Color(0xCC151B24)
                                    }
                                )
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.5.dp,
                                    color = when {
                                        isSelected -> DreamTrailColors.ParchmentText
                                        unlocked -> accent
                                        else -> DreamTrailColors.CardOutline
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!unlocked) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = DreamTrailColors.MutedInk,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = when (index) {
                                        0 -> "前"
                                        1 -> "深"
                                        else -> "镇"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) DreamTrailColors.LanternInk else accent
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) DreamTrailColors.LanternAmber else Color(0xD912161E),
                            border = BorderStroke(
                                width = 0.8.dp,
                                color = if (isSelected) DreamTrailColors.LanternAmber else accent.copy(alpha = 0.45f)
                            )
                        ) {
                            Text(
                                text = route.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) DreamTrailColors.LanternInk else if (unlocked) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class CampStation(val label: String, val facility: FacilityType?) {
    WORKSHOP("工坊", FacilityType.WORKSHOP),
    TEA_HEARTH("茶歇火塘", FacilityType.TEA_HEARTH),
    CONSERVATORY("星尘花房", FacilityType.CONSERVATORY),
    MAP_TABLE("地图桌", FacilityType.MAP_TABLE),
    INN("旅舍", FacilityType.INN),
    COMMISSIONS("委托榜", null)
}

/**
 * 2D 营地全景沙盘：绘制夜空营地与 6 处互动建筑/告示板，建筑外观随等级进化，可点击切换工作台。
 */
@Composable
fun CampDioramaCanvas(
    playerState: PlayerState,
    discoveryState: DiscoveryState,
    selectedStation: CampStation,
    onSelectStation: (CampStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val stationsLayout = listOf(
        CampStation.MAP_TABLE to (0.17f to 0.34f),
        CampStation.INN to (0.50f to 0.28f),
        CampStation.CONSERVATORY to (0.83f to 0.34f),
        CampStation.WORKSHOP to (0.20f to 0.72f),
        CampStation.TEA_HEARTH to (0.52f to 0.74f),
        CampStation.COMMISSIONS to (0.83f to 0.72f)
    )

    val unlockedCompanionCount = ContentCatalog.companionStories.count {
        (discoveryState.companionChapters[it.id] ?: 0) >= 1
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DreamTrailColors.CardOutline),
        color = DreamTrailColors.CardSurface
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))
        ) {
            val nodeWidth = (maxWidth * 0.28f).coerceAtMost(92.dp)
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF0E1420), Color(0xFF1A2535), Color(0xFF161E29))
                    )
                )
                // Stars & crescent moon
                for ((sx, sy) in listOf(0.12f to 0.14f, 0.30f to 0.10f, 0.68f to 0.12f, 0.90f to 0.16f)) {
                    drawCircle(DreamTrailColors.ParchmentText.copy(alpha = 0.55f), radius = 1.8f, center = Offset(w * sx, h * sy))
                }
                drawCircle(DreamTrailColors.LanternAmber.copy(alpha = 0.75f), radius = 10f, center = Offset(w * 0.90f, h * 0.18f))
                drawCircle(Color(0xFF0E1420), radius = 8.5f, center = Offset(w * 0.90f + 4f, h * 0.18f - 2f))

                // Clearing ground & stepping stone paths
                drawOval(
                    color = Color(0xFF202A3A),
                    topLeft = Offset(w * 0.10f, h * 0.34f),
                    size = Size(w * 0.80f, h * 0.58f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(DreamTrailColors.LanternAmber.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    radius = w * 0.16f,
                    center = Offset(w * 0.50f, h * 0.53f)
                )
                // Central hearth fire
                val flame = Path().apply {
                    moveTo(w * 0.50f - 8f, h * 0.54f)
                    quadraticTo(w * 0.50f, h * 0.45f, w * 0.50f + 8f, h * 0.54f)
                    close()
                }
                drawPath(flame, color = DreamTrailColors.LanternAmber)
            }

            stationsLayout.forEach { (station, pos) ->
                val (xf, yf) = pos
                val lv = station.facility?.let { playerState.facilityLevel(it) } ?: 1
                val isSelected = station == selectedStation
                val canUpgrade = station.facility?.let { f ->
                    if (lv >= 5) false else {
                        val target = lv + 1
                        val reqHours = ContentCatalog.facilityRequiredExploreHours(f, target)
                        val (gCost, matPair) = ContentCatalog.facilityUpgradeCost(f, target)
                        discoveryState.totalCreditedSeconds >= reqHours * 3600L &&
                            playerState.resources.gold >= gCost &&
                            playerState.resources.materialCount(matPair.first) >= matPair.second
                    }
                } ?: false

                val boxHalfW = with(density) { (nodeWidth / 2).toPx() }
                val boxHalfH = with(density) { 34.dp.toPx() }
                val ox = (widthPx * xf - boxHalfW).roundToInt().coerceIn(4, (widthPx - boxHalfW * 2).roundToInt().coerceAtLeast(4))
                val oy = (heightPx * yf - boxHalfH).roundToInt().coerceIn(4, (heightPx - boxHalfH * 2).roundToInt().coerceAtLeast(4))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset { IntOffset(ox, oy) }
                        .width(nodeWidth)
                        .heightIn(min = 48.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = station.label
                            stateDescription = if (station.facility == null) "委托" else "设施等级 $lv"
                        }
                        .selectable(selected = isSelected, role = Role.Tab) { onSelectStation(station) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp, 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) DreamTrailColors.CardElevated else Color(0xCC121720))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = when {
                                    isSelected -> DreamTrailColors.LanternAmber
                                    lv > 0 -> DreamTrailColors.SageMist.copy(alpha = 0.6f)
                                    else -> DreamTrailColors.CardOutline
                                },
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                            drawCampStationBuilding(station, lv)
                        }
                        if (canUpgrade) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(DreamTrailColors.LanternAmber)
                            )
                        }
                        if (station == CampStation.INN && unlockedCompanionCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DreamTrailColors.SageMist,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(2.dp)
                            ) {
                                Text(
                                    text = "${unlockedCompanionCount}友",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DreamTrailColors.SageInk,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = if (isSelected) DreamTrailColors.LanternAmber else Color(0xD912161E)
                    ) {
                        val tag = if (station.facility != null) {
                            if (lv == 0) "${station.label}·待建" else "${station.label} Lv.$lv"
                        } else {
                            "${station.label} ${discoveryState.completedCommissionIds.size}/${ContentCatalog.commissions.size}"
                        }
                        Text(
                            text = tag,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) DreamTrailColors.LanternInk else DreamTrailColors.ParchmentText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCampStationBuilding(station: CampStation, level: Int) {
    val w = size.width
    val h = size.height
    val lit = level > 0
    val roofColor = if (lit) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk.copy(alpha = 0.5f)
    val bodyColor = if (lit) DreamTrailColors.SageMist else DreamTrailColors.CardOutline

    when (station) {
        CampStation.INN -> {
            // Cabin / two-story lodge when Lv >= 4
            val roof = Path().apply {
                moveTo(w * 0.14f, h * 0.45f)
                lineTo(w * 0.50f, h * (if (level >= 4) 0.12f else 0.20f))
                lineTo(w * 0.86f, h * 0.45f)
                close()
            }
            drawPath(roof, color = roofColor)
            drawRect(bodyColor.copy(alpha = 0.85f), Offset(w * 0.22f, h * 0.45f), Size(w * 0.56f, h * 0.45f))
            if (lit) {
                drawRect(DreamTrailColors.LanternAmber, Offset(w * 0.34f, h * 0.55f), Size(w * 0.14f, h * 0.16f))
                drawRect(DreamTrailColors.LanternAmber, Offset(w * 0.54f, h * 0.55f), Size(w * 0.14f, h * 0.16f))
            }
        }
        CampStation.WORKSHOP -> {
            // Anvil + forge glow
            drawRect(Color(0xFF7D8590), Offset(w * 0.22f, h * 0.54f), Size(w * 0.52f, h * 0.18f))
            drawRect(Color(0xFF5D6673), Offset(w * 0.36f, h * 0.72f), Size(w * 0.24f, h * 0.18f))
            if (lit) {
                drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.14f, center = Offset(w * 0.72f, h * 0.38f))
            }
            if (level >= 4) {
                drawCircle(DreamTrailColors.StarlightBlue, radius = w * 0.08f, center = Offset(w * 0.32f, h * 0.32f))
            }
        }
        CampStation.TEA_HEARTH -> {
            // Copper kettle & hearth fire
            drawRoundRect(
                color = if (lit) Color(0xFFD68D54) else bodyColor,
                topLeft = Offset(w * 0.28f, h * 0.36f),
                size = Size(w * 0.44f, h * 0.38f),
                cornerRadius = CornerRadius(10f, 10f)
            )
            if (lit) {
                drawLine(DreamTrailColors.ParchmentText.copy(alpha = 0.7f), Offset(w * 0.42f, h * 0.28f), Offset(w * 0.40f, h * 0.12f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                drawLine(DreamTrailColors.ParchmentText.copy(alpha = 0.7f), Offset(w * 0.56f, h * 0.28f), Offset(w * 0.58f, h * 0.12f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            }
        }
        CampStation.CONSERVATORY -> {
            // Arched glass greenhouse
            val dome = Path().apply {
                moveTo(w * 0.18f, h * 0.86f)
                lineTo(w * 0.18f, h * 0.46f)
                quadraticTo(w * 0.50f, h * 0.08f, w * 0.82f, h * 0.46f)
                lineTo(w * 0.82f, h * 0.86f)
                close()
            }
            drawPath(dome, color = if (lit) Color(0xFF47888E).copy(alpha = 0.65f) else bodyColor.copy(alpha = 0.35f))
            drawPath(dome, color = if (lit) Color(0xFF9AD4DC) else bodyColor, style = Stroke(2.2f))
            if (lit) {
                drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.10f, center = Offset(w * 0.50f, h * 0.62f))
            }
        }
        CampStation.MAP_TABLE -> {
            // Wooden table with map scroll & brass compass
            drawRect(Color(0xFF7B573C), Offset(w * 0.16f, h * 0.56f), Size(w * 0.68f, h * 0.14f))
            drawRect(Color(0xFF5D402B), Offset(w * 0.22f, h * 0.70f), Size(w * 0.10f, h * 0.20f))
            drawRect(Color(0xFF5D402B), Offset(w * 0.68f, h * 0.70f), Size(w * 0.10f, h * 0.20f))
            drawRect(if (lit) DreamTrailColors.ParchmentText else bodyColor, Offset(w * 0.28f, h * 0.40f), Size(w * 0.44f, h * 0.16f))
            if (level >= 2) {
                drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.09f, center = Offset(w * 0.66f, h * 0.34f))
            }
        }
        CampStation.COMMISSIONS -> {
            // Notice board with pinned parchments
            drawRect(Color(0xFF7B573C), Offset(w * 0.18f, h * 0.18f), Size(w * 0.64f, h * 0.56f))
            drawRect(DreamTrailColors.ParchmentText.copy(alpha = 0.9f), Offset(w * 0.24f, h * 0.26f), Size(w * 0.22f, h * 0.28f))
            drawRect(DreamTrailColors.LanternAmber.copy(alpha = 0.9f), Offset(w * 0.52f, h * 0.26f), Size(w * 0.22f, h * 0.30f))
            drawLine(Color(0xFF5D402B), Offset(w * 0.26f, h * 0.74f), Offset(w * 0.26f, h * 0.92f), strokeWidth = 4f)
            drawLine(Color(0xFF5D402B), Offset(w * 0.74f, h * 0.74f), Offset(w * 0.74f, h * 0.92f), strokeWidth = 4f)
        }
    }
}

/**
 * 2D 旅人星盘画布：中心罗盘向 4 大流派象限伸展连线星点，点击星点查看详情或点亮。
 */
@Composable
fun AstralConstellationCanvas(
    unlockedNodeIds: Set<String>,
    selectedNodeId: String,
    onSelectNode: (AstralNodeDef) -> Unit,
    modifier: Modifier = Modifier
) {
    val branchTargets = mapOf(
        BuildArchetype.BASTION to listOf(0.35f to 0.38f, 0.23f to 0.27f, 0.13f to 0.17f),
        BuildArchetype.GALE to listOf(0.65f to 0.38f, 0.77f to 0.27f, 0.87f to 0.17f),
        BuildArchetype.DREAMWEAVE to listOf(0.35f to 0.62f, 0.23f to 0.73f, 0.13f to 0.83f),
        BuildArchetype.SCHOLAR to listOf(0.65f to 0.62f, 0.77f to 0.73f, 0.87f to 0.83f)
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DreamTrailColors.StarlightBlue.copy(alpha = 0.45f)),
        color = Color(0xFF0E1320)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val center = Offset(w * 0.5f, h * 0.5f)

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1D2742), Color(0xFF0B101B)),
                        center = center,
                        radius = w * 0.65f
                    )
                )

                // Astrolabe concentric rings
                drawCircle(DreamTrailColors.StarlightBlue.copy(alpha = 0.18f), radius = h * 0.22f, center = center, style = Stroke(1.2f))
                drawCircle(DreamTrailColors.StarlightBlue.copy(alpha = 0.15f), radius = h * 0.36f, center = center, style = Stroke(1.2f))
                drawCircle(DreamTrailColors.LanternAmber.copy(alpha = 0.14f), radius = h * 0.47f, center = center, style = Stroke(1f))

                // Constellation lines per branch
                for (arch in BuildArchetype.entries) {
                    val coords = branchTargets[arch] ?: continue
                    val nodes = ContentCatalog.astralNodes.filter { it.archetype == arch }.sortedBy { it.tier }
                    val branchColor = archetypeColor(arch)
                    var prevPoint = center
                    nodes.forEachIndexed { idx, node ->
                        val (xf, yf) = coords[idx]
                        val pt = Offset(w * xf, h * yf)
                        val lit = node.id in unlockedNodeIds
                        drawLine(
                            color = if (lit) branchColor else branchColor.copy(alpha = 0.24f),
                            start = prevPoint,
                            end = pt,
                            strokeWidth = if (lit) 4.5f else 2f,
                            cap = StrokeCap.Round
                        )
                        prevPoint = pt
                    }
                }

                // Center Traveler Core Star
                drawCircle(DreamTrailColors.LanternAmber.copy(alpha = 0.28f), radius = 22f, center = center)
                drawCircle(DreamTrailColors.LanternAmber, radius = 11f, center = center)
                drawCircle(DreamTrailColors.ParchmentText, radius = 4.5f, center = center)
            }

            // Branch Corner Labels
            Text(
                text = "坚盾星脉",
                style = MaterialTheme.typography.labelSmall,
                color = archetypeColor(BuildArchetype.BASTION),
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
            )
            Text(
                text = "疾风星脉",
                style = MaterialTheme.typography.labelSmall,
                color = archetypeColor(BuildArchetype.GALE),
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
            )
            Text(
                text = "织梦星脉",
                style = MaterialTheme.typography.labelSmall,
                color = archetypeColor(BuildArchetype.DREAMWEAVE),
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
            )
            Text(
                text = "博学星脉",
                style = MaterialTheme.typography.labelSmall,
                color = archetypeColor(BuildArchetype.SCHOLAR),
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
            )

            // Interactive Star Node Medallions
            for (arch in BuildArchetype.entries) {
                val coords = branchTargets[arch] ?: continue
                val nodes = ContentCatalog.astralNodes.filter { it.archetype == arch }.sortedBy { it.tier }
                val branchColor = archetypeColor(arch)
                nodes.forEachIndexed { idx, node ->
                    val (xf, yf) = coords[idx]
                    val unlocked = node.id in unlockedNodeIds
                    val isSelected = node.id == selectedNodeId
                    val half = with(density) { 24.dp.toPx() }
                    val ox = (widthPx * xf - half).roundToInt()
                    val oy = (heightPx * yf - half).roundToInt()

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(ox, oy) }
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    unlocked -> branchColor
                                    isSelected -> DreamTrailColors.CardElevated
                                    else -> Color(0xCC121724)
                                }
                            )
                            .border(
                                width = if (isSelected) 2.5.dp else 1.4.dp,
                                color = if (isSelected) DreamTrailColors.LanternAmber else if (unlocked) DreamTrailColors.ParchmentText else branchColor.copy(alpha = 0.55f),
                                shape = CircleShape
                            )
                            .semantics {
                                contentDescription = node.title
                                stateDescription = if (unlocked) "已点亮" else "未点亮"
                            }
                            .selectable(selected = isSelected, role = Role.RadioButton) { onSelectNode(node) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${node.tier}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (unlocked) DreamTrailColors.LanternInk else DreamTrailColors.ParchmentText
                        )
                    }
                }
            }
        }
    }
}
