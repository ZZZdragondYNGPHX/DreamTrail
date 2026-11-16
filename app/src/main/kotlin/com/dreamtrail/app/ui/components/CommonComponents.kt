package com.dreamtrail.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.model.RegionId
import com.dreamtrail.core.model.ResourceBag

@Composable
fun JournalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = DreamTrailColors.CardSurface,
    borderColor: Color = DreamTrailColors.CardOutline,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResourceSummaryRow(resources: ResourceBag, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ResourceChip(
            text = "金币 ${resources.gold}",
            accent = DreamTrailColors.LanternAmber,
            kind = ResourceVisualKind.GOLD
        )
        ResourceChip(
            text = "松木 ${resources.materialCount(RegionId.MISTWOOD.materialId)}",
            accent = materialAccentColor(RegionId.MISTWOOD.materialId),
            kind = ResourceVisualKind.MATERIAL
        )
        val tidal = resources.materialCount(RegionId.TIDAL_FLAT.materialId)
        if (tidal > 0L) {
            ResourceChip(
                text = "矿砂 $tidal",
                accent = materialAccentColor(RegionId.TIDAL_FLAT.materialId),
                kind = ResourceVisualKind.MATERIAL
            )
        }
        val star = resources.materialCount(RegionId.STARLIT_RIDGE.materialId)
        if (star > 0L) {
            ResourceChip(
                text = "碎晶 $star",
                accent = materialAccentColor(RegionId.STARLIT_RIDGE.materialId),
                kind = ResourceVisualKind.MATERIAL
            )
        }
        val cloud = resources.materialCount(RegionId.CLOUD_SANCTUARY.materialId)
        if (cloud > 0L) {
            ResourceChip(
                text = "灵绒 $cloud",
                accent = materialAccentColor(RegionId.CLOUD_SANCTUARY.materialId),
                kind = ResourceVisualKind.MATERIAL
            )
        }
        val dream = resources.materialCount(RegionId.DREAM_SEA.materialId)
        if (dream > 0L) {
            ResourceChip(
                text = "梦珠 $dream",
                accent = materialAccentColor(RegionId.DREAM_SEA.materialId),
                kind = ResourceVisualKind.MATERIAL
            )
        }
        val astral = resources.materialCount(RegionId.ASTRAL_ARCH.materialId)
        if (astral > 0L) {
            ResourceChip(
                text = "琥珀 $astral",
                accent = materialAccentColor(RegionId.ASTRAL_ARCH.materialId),
                kind = ResourceVisualKind.MATERIAL
            )
        }
        val vellum = resources.materialCount(RegionId.SUNLIT_ATHENAEUM.materialId)
        if (vellum > 0L) {
            ResourceChip(
                text = "羊皮卷 $vellum",
                accent = materialAccentColor(RegionId.SUNLIT_ATHENAEUM.materialId),
                kind = ResourceVisualKind.MATERIAL
            )
        }
        val prism = resources.materialCount(RegionId.DAWNLIGHT_SPIRE.materialId)
        if (prism > 0L) {
            ResourceChip(
                text = "棱晶 $prism",
                accent = materialAccentColor(RegionId.DAWNLIGHT_SPIRE.materialId),
                kind = ResourceVisualKind.MATERIAL
            )
        }
        if (resources.inspirationInk > 0L) {
            ResourceChip(
                text = "墨晶 ${resources.inspirationInk}",
                accent = Color(0xFF7FD1BE),
                kind = ResourceVisualKind.INK
            )
        }
        if (resources.bossMarks > 0L) {
            ResourceChip(
                text = "印记 ${resources.bossMarks}",
                accent = DreamTrailColors.CoralWarn,
                kind = ResourceVisualKind.BOSS_MARK
            )
        }
    }
}

@Composable
fun ResourceChip(
    text: String,
    accent: Color,
    kind: ResourceVisualKind? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DreamTrailColors.CardElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (kind != null) {
                ResourceBadgeIcon(
                    kind = kind,
                    accent = accent,
                    size = 14.dp
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = DreamTrailColors.ParchmentText
            )
        }
    }
}

@Composable
fun TagBadge(
    text: String,
    accent: Color = DreamTrailColors.SageMist
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

fun formatDurationZh(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val h = safe / 3600L
    val m = (safe % 3600L) / 60L
    val s = safe % 60L
    return when {
        h > 0L -> "${h} 小时 ${m} 分钟"
        m > 0L -> "${m} 分钟 ${s} 秒"
        else -> "${s} 秒"
    }
}
