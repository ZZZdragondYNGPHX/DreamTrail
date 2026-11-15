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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ResourceChip("金币 ${resources.gold}", DreamTrailColors.LanternAmber)
        ResourceChip("松木 ${resources.materialCount(RegionId.MISTWOOD.materialId)}", DreamTrailColors.SageMist)
        val tidal = resources.materialCount(RegionId.TIDAL_FLAT.materialId)
        if (tidal > 0L) ResourceChip("矿砂 $tidal", DreamTrailColors.StarlightBlue)
        val star = resources.materialCount(RegionId.STARLIT_RIDGE.materialId)
        if (star > 0L) ResourceChip("碎晶 $star", Color(0xFFC2A7E0))
        val cloud = resources.materialCount(RegionId.CLOUD_SANCTUARY.materialId)
        if (cloud > 0L) ResourceChip("灵绒 $cloud", Color(0xFF9AD4DC))
        val dream = resources.materialCount(RegionId.DREAM_SEA.materialId)
        if (dream > 0L) ResourceChip("梦珠 $dream", Color(0xFF8AB8FF))
        val astral = resources.materialCount(RegionId.ASTRAL_ARCH.materialId)
        if (astral > 0L) ResourceChip("琥珀 $astral", Color(0xFFFFD089))
        if (resources.bossMarks > 0L) ResourceChip("印记 ${resources.bossMarks}", DreamTrailColors.CoralWarn)
    }
}

@Composable
fun ResourceChip(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DreamTrailColors.CardElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = DreamTrailColors.ParchmentText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
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
