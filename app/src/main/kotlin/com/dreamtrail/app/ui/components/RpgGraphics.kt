package com.dreamtrail.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamtrail.app.R
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.model.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun tierAccentColor(tier: Int): Color = when (tier) {
    1 -> DreamTrailColors.SageMist
    2 -> Color(0xFF6AB4EC)
    3 -> Color(0xFFC29AE8)
    4 -> Color(0xFF7AD6D2)
    5 -> Color(0xFF72A4FF)
    6 -> Color(0xFFF3C365)
    7 -> Color(0xFFE6B455)
    else -> Color(0xFFF0985A)
}

fun archetypeColor(archetype: BuildArchetype): Color = when (archetype) {
    BuildArchetype.BASTION -> DreamTrailColors.SageMist
    BuildArchetype.GALE -> Color(0xFF72B8EE)
    BuildArchetype.DREAMWEAVE -> Color(0xFFC99BEB)
    BuildArchetype.SCHOLAR -> DreamTrailColors.LanternAmber
}

fun regionAccentColor(region: RegionId): Color = when (region) {
    RegionId.MISTWOOD -> DreamTrailColors.SageMist
    RegionId.TIDAL_FLAT -> Color(0xFF6CB8E6)
    RegionId.STARLIT_RIDGE -> Color(0xFFBFA1E8)
    RegionId.CLOUD_SANCTUARY -> Color(0xFF83D7CE)
    RegionId.DREAM_SEA -> Color(0xFF7EA9FF)
    RegionId.ASTRAL_ARCH -> Color(0xFFF3C365)
    RegionId.SUNLIT_ATHENAEUM -> Color(0xFFE6B455)
    RegionId.DAWNLIGHT_SPIRE -> Color(0xFFF0985A)
}

fun materialAccentColor(materialId: String): Color = when (materialId) {
    RegionId.MISTWOOD.materialId -> DreamTrailColors.SageMist
    RegionId.TIDAL_FLAT.materialId -> DreamTrailColors.StarlightBlue
    RegionId.STARLIT_RIDGE.materialId -> Color(0xFFC2A7E0)
    RegionId.CLOUD_SANCTUARY.materialId -> Color(0xFF9AD4DC)
    RegionId.DREAM_SEA.materialId -> Color(0xFF8AB8FF)
    RegionId.ASTRAL_ARCH.materialId -> Color(0xFFFFD089)
    RegionId.SUNLIT_ATHENAEUM.materialId -> Color(0xFFE8C478)
    RegionId.DAWNLIGHT_SPIRE.materialId -> Color(0xFFF3A775)
    else -> DreamTrailColors.LanternAmber
}

fun portraitDrawableRes(portraitKey: String?): Int? = when (portraitKey) {
    "traveler" -> R.drawable.portrait_traveler
    "comp_airen" -> R.drawable.portrait_comp_airen
    "comp_noah" -> R.drawable.portrait_comp_noah
    "comp_lin" -> R.drawable.portrait_comp_lin
    "comp_xiu" -> R.drawable.portrait_comp_xiu
    "comp_cen" -> R.drawable.portrait_comp_cen
    else -> null
}

fun companionAccentColor(companionId: String?): Color = when (companionId) {
    "comp_airen" -> DreamTrailColors.SageMist
    "comp_noah" -> DreamTrailColors.StarlightBlue
    "comp_lin" -> Color(0xFF84D8C7)
    "comp_xiu" -> Color(0xFFC99BEB)
    "comp_cen" -> DreamTrailColors.LanternAmber
    "comp_wen" -> Color(0xFFE6B455)
    "comp_zhaolu" -> Color(0xFFF0985A)
    else -> DreamTrailColors.LanternAmber
}

@Composable
fun ArchetypeEmblem(
    archetype: BuildArchetype,
    size: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    val color = archetypeColor(archetype)
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        drawCircle(color = color.copy(alpha = 0.22f), radius = w * 0.48f, center = Offset(cx, cy))
        drawCircle(
            color = color.copy(alpha = 0.85f),
            radius = w * 0.46f,
            center = Offset(cx, cy),
            style = Stroke(width = (w * 0.08f).coerceAtLeast(1.2f))
        )
        when (archetype) {
            BuildArchetype.BASTION -> {
                val p = Path().apply {
                    moveTo(cx, h * 0.22f)
                    lineTo(w * 0.76f, h * 0.34f)
                    lineTo(w * 0.70f, h * 0.64f)
                    lineTo(cx, h * 0.80f)
                    lineTo(w * 0.30f, h * 0.64f)
                    lineTo(w * 0.24f, h * 0.34f)
                    close()
                }
                drawPath(p, color = color)
            }
            BuildArchetype.GALE -> {
                val p = Path().apply {
                    moveTo(w * 0.22f, h * 0.68f)
                    quadraticTo(w * 0.50f, h * 0.22f, w * 0.80f, h * 0.30f)
                    quadraticTo(w * 0.56f, h * 0.52f, w * 0.52f, h * 0.76f)
                    close()
                }
                drawPath(p, color = color)
            }
            BuildArchetype.DREAMWEAVE -> {
                drawCircle(color = color, radius = w * 0.23f, center = Offset(cx - w * 0.04f, cy))
                drawCircle(color = DreamTrailColors.CardSurface, radius = w * 0.18f, center = Offset(cx + w * 0.09f, cy - h * 0.06f))
            }
            BuildArchetype.SCHOLAR -> {
                val p = Path().apply {
                    moveTo(cx, h * 0.20f)
                    lineTo(cx + w * 0.10f, cy - h * 0.10f)
                    lineTo(w * 0.80f, cy)
                    lineTo(cx + w * 0.10f, cy + h * 0.10f)
                    lineTo(cx, h * 0.80f)
                    lineTo(cx - w * 0.10f, cy + h * 0.10f)
                    lineTo(w * 0.20f, cy)
                    lineTo(cx - w * 0.10f, cy - h * 0.10f)
                    close()
                }
                drawPath(p, color = color)
            }
        }
    }
}

@Composable
fun CharacterPortraitBox(
    portraitKey: String?,
    size: Dp,
    accentColor: Color = DreamTrailColors.LanternAmber,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    locked: Boolean = false,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    val resId = portraitDrawableRes(portraitKey)
    val grayscaleFilter = if (locked) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.1f) })
    } else null

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.24f), DreamTrailColors.NightBackground)
                )
            )
            .border(BorderStroke(1.5.dp, if (locked) DreamTrailColors.CardOutline else accentColor.copy(alpha = 0.85f)), shape)
    ) {
        if (resId != null) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = grayscaleFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (locked) 0.42f else 1f)
            )
        } else {
            // Vector fallback silhouette
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                drawCircle(color = accentColor.copy(alpha = 0.25f), radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.45f))
                drawCircle(color = DreamTrailColors.ParchmentText.copy(alpha = 0.85f), radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.36f))
                val cloak = Path().apply {
                    moveTo(w * 0.22f, h * 0.92f)
                    lineTo(w * 0.36f, h * 0.52f)
                    lineTo(w * 0.64f, h * 0.52f)
                    lineTo(w * 0.78f, h * 0.92f)
                    close()
                }
                drawPath(cloak, color = accentColor.copy(alpha = 0.8f))
            }
        }

        // Subtle dark vignette at bottom for text legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(size * 0.28f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xAA0C1018))
                    )
                )
        )

        if (locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xBB101520)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = DreamTrailColors.MutedInk,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xCC12161E),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * 正统 RPG 方形装备图标格，根据装备原型、流派、品阶与强化等级绘制专属 2D 图元。
 */
@Composable
fun EquipmentIconTile(
    def: EquipmentDef,
    level: Int = 1,
    affix: AffixId = def.defaultAffix,
    secondaryAffix: AffixId? = null,
    isEquipped: Boolean = false,
    isLocked: Boolean = false,
    isSelected: Boolean = false,
    showBulkCheck: Boolean? = null,
    dimmed: Boolean = false,
    tileSize: Dp = 74.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tierColor = tierAccentColor(def.tier)
    val borderColor = when {
        isSelected -> DreamTrailColors.LanternAmber
        isEquipped -> DreamTrailColors.LanternAmber.copy(alpha = 0.88f)
        dimmed -> DreamTrailColors.CardOutline
        else -> tierColor.copy(alpha = 0.68f)
    }

    Surface(
        modifier = modifier
            .size(tileSize)
            .semantics(mergeDescendants = true) {
                contentDescription = "${def.name}，${def.slot.displayName}，${affix.displayName}" +
                    (secondaryAffix?.let { "，副词条${it.displayName}" } ?: "")
                stateDescription = listOfNotNull(
                    "强化等级 $level",
                    if (isEquipped) "已穿戴" else null,
                    if (isLocked) "已锁定" else null,
                    showBulkCheck?.let { if (it) "已选中拆解" else "未选中拆解" }
                ).joinToString("，")
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) DreamTrailColors.CardElevated else DreamTrailColors.NightBackground,
        border = BorderStroke(if (isSelected || isEquipped) 1.8.dp else 1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 2D Equipment illustration canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .alpha(if (dimmed) 0.26f else 1f)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(tierColor.copy(alpha = 0.24f), Color.Transparent)
                    ),
                    radius = size.minDimension * 0.56f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
                drawEquipmentArtwork(def, tierColor)
            }

            // Top-left: primary & secondary affix archetype pips
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(archetypeColor(affix.archetype))
                        .border(0.8.dp, Color(0xCC0D1118), CircleShape)
                )
                if (secondaryAffix != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(archetypeColor(secondaryAffix.archetype))
                            .border(0.8.dp, Color(0xCC0D1118), CircleShape)
                    )
                }
            }

            // Top-right: equipped / lock / bulk checkbox
            if (showBulkCheck != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (showBulkCheck) DreamTrailColors.LanternAmber else Color(0xCC161C26))
                        .border(1.dp, if (showBulkCheck) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk, RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (showBulkCheck) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = DreamTrailColors.LanternInk,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            } else if (isEquipped || isLocked) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (isLocked) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = DreamTrailColors.SageMist,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    if (isEquipped) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DreamTrailColors.LanternAmber
                        ) {
                            Text(
                                text = "装",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = DreamTrailColors.LanternInk,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.dp)
                            )
                        }
                    }
                }
            }

            // Bottom-left: tier badge
            Text(
                text = "${def.tier}阶",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = tierColor,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 5.dp, vertical = 3.dp)
            )

            // Bottom-right: enhancement level badge
            if (level > 0) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (level >= 6) DreamTrailColors.LanternAmber.copy(alpha = 0.25f) else Color(0xBB161C26),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "+$level",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (level >= 6) DreamTrailColors.LanternAmber else DreamTrailColors.ParchmentText,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawEquipmentArtwork(def: EquipmentDef, accent: Color) {
    val w = size.width
    val h = size.height
    val metal = Color(0xFFDAD3C6)
    val darkWood = Color(0xFF8C623E)
    val id = def.id

    when (def.slot) {
        EquipmentSlot.WEAPON -> when {
            id.contains("bow") -> {
                // Curved harp bow + string
                val bow = Path().apply {
                    moveTo(w * 0.30f, h * 0.14f)
                    quadraticTo(w * 0.84f, h * 0.50f, w * 0.30f, h * 0.86f)
                }
                drawPath(bow, color = accent, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
                drawLine(metal, Offset(w * 0.30f, h * 0.16f), Offset(w * 0.30f, h * 0.84f), strokeWidth = w * 0.03f)
                drawLine(DreamTrailColors.LanternAmber, Offset(w * 0.24f, h * 0.50f), Offset(w * 0.82f, h * 0.50f), strokeWidth = w * 0.045f, cap = StrokeCap.Round)
            }
            id.contains("staff") || id.contains("rod") || id.contains("orbiter") -> {
                // Magic staff / lantern rod
                drawLine(darkWood, Offset(w * 0.24f, h * 0.84f), Offset(w * 0.68f, h * 0.30f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                drawCircle(accent.copy(alpha = 0.35f), radius = w * 0.21f, center = Offset(w * 0.71f, h * 0.26f))
                drawCircle(accent, radius = w * 0.12f, center = Offset(w * 0.71f, h * 0.26f))
                drawCircle(DreamTrailColors.ParchmentText, radius = w * 0.055f, center = Offset(w * 0.71f, h * 0.26f))
            }
            id.contains("lance") || id.contains("halberd") -> {
                // Long spear / halberd
                drawLine(darkWood, Offset(w * 0.20f, h * 0.86f), Offset(w * 0.74f, h * 0.26f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                val tip = Path().apply {
                    moveTo(w * 0.86f, h * 0.14f)
                    lineTo(w * 0.78f, h * 0.36f)
                    lineTo(w * 0.62f, h * 0.22f)
                    close()
                }
                drawPath(tip, color = accent)
                drawPath(tip, color = metal, style = Stroke(width = 1.5f))
            }
            else -> {
                // Sword / blade
                drawLine(accent.copy(alpha = 0.45f), Offset(w * 0.32f, h * 0.70f), Offset(w * 0.82f, h * 0.18f), strokeWidth = w * 0.15f, cap = StrokeCap.Round)
                drawLine(metal, Offset(w * 0.34f, h * 0.68f), Offset(w * 0.82f, h * 0.18f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                drawLine(DreamTrailColors.LanternAmber, Offset(w * 0.24f, h * 0.56f), Offset(w * 0.44f, h * 0.76f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                drawLine(darkWood, Offset(w * 0.20f, h * 0.80f), Offset(w * 0.34f, h * 0.66f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
            }
        }
        EquipmentSlot.ARMOR -> when {
            id.contains("buckler") || id.contains("plate") || id.contains("mail") -> {
                // Cuirass / shield armor
                val chest = Path().apply {
                    moveTo(w * 0.26f, h * 0.22f)
                    lineTo(w * 0.40f, h * 0.18f)
                    lineTo(w * 0.50f, h * 0.28f)
                    lineTo(w * 0.60f, h * 0.18f)
                    lineTo(w * 0.74f, h * 0.22f)
                    lineTo(w * 0.82f, h * 0.44f)
                    lineTo(w * 0.72f, h * 0.78f)
                    lineTo(w * 0.50f, h * 0.86f)
                    lineTo(w * 0.28f, h * 0.78f)
                    lineTo(w * 0.18f, h * 0.44f)
                    close()
                }
                drawPath(chest, color = accent.copy(alpha = 0.85f))
                drawPath(chest, color = metal, style = Stroke(width = w * 0.05f, join = StrokeJoin.Round))
                drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.48f))
            }
            else -> {
                // Cloak / mantle / robe
                val cloak = Path().apply {
                    moveTo(w * 0.38f, h * 0.16f)
                    lineTo(w * 0.62f, h * 0.16f)
                    lineTo(w * 0.82f, h * 0.82f)
                    lineTo(w * 0.50f, h * 0.74f)
                    lineTo(w * 0.18f, h * 0.82f)
                    close()
                }
                drawPath(cloak, color = accent.copy(alpha = 0.85f))
                drawPath(cloak, color = DreamTrailColors.ParchmentText.copy(alpha = 0.7f), style = Stroke(width = w * 0.045f, join = StrokeJoin.Round))
                val collar = Path().apply {
                    moveTo(w * 0.34f, h * 0.26f)
                    quadraticTo(w * 0.50f, h * 0.40f, w * 0.66f, h * 0.26f)
                }
                drawPath(collar, color = DreamTrailColors.LanternAmber, style = Stroke(width = w * 0.055f, cap = StrokeCap.Round))
            }
        }
        EquipmentSlot.CHARM -> when {
            id.contains("flute") -> {
                drawLine(accent, Offset(w * 0.20f, h * 0.76f), Offset(w * 0.80f, h * 0.24f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
                drawCircle(DreamTrailColors.NightBackground, radius = w * 0.03f, center = Offset(w * 0.44f, h * 0.55f))
                drawCircle(DreamTrailColors.NightBackground, radius = w * 0.03f, center = Offset(w * 0.56f, h * 0.45f))
                drawCircle(DreamTrailColors.NightBackground, radius = w * 0.03f, center = Offset(w * 0.68f, h * 0.35f))
            }
            id.contains("bell") -> {
                val bell = Path().apply {
                    moveTo(w * 0.50f, h * 0.22f)
                    quadraticTo(w * 0.74f, h * 0.30f, w * 0.76f, h * 0.68f)
                    lineTo(w * 0.24f, h * 0.68f)
                    quadraticTo(w * 0.26f, h * 0.30f, w * 0.50f, h * 0.22f)
                    close()
                }
                drawPath(bell, color = accent)
                drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.74f))
                drawCircle(metal, radius = w * 0.07f, center = Offset(w * 0.50f, h * 0.18f), style = Stroke(width = 2.5f))
            }
            id.contains("lantern") || id.contains("censer") -> {
                drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.07f, center = Offset(w * 0.50f, h * 0.20f), style = Stroke(width = 2.5f))
                drawRoundRect(
                    color = accent.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.32f, h * 0.28f),
                    size = Size(w * 0.36f, h * 0.48f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.11f, center = Offset(w * 0.50f, h * 0.52f))
            }
            else -> {
                // Compass / Prism / Pocket Watch charm
                drawCircle(color = accent, radius = w * 0.30f, center = Offset(w * 0.50f, h * 0.54f), style = Stroke(width = w * 0.07f))
                drawCircle(color = accent.copy(alpha = 0.25f), radius = w * 0.27f, center = Offset(w * 0.50f, h * 0.54f))
                drawCircle(color = DreamTrailColors.LanternAmber, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.20f), style = Stroke(width = 2.5f))
                val needle = Path().apply {
                    moveTo(w * 0.50f, h * 0.34f)
                    lineTo(w * 0.58f, h * 0.54f)
                    lineTo(w * 0.50f, h * 0.72f)
                    lineTo(w * 0.42f, h * 0.54f)
                    close()
                }
                drawPath(needle, color = DreamTrailColors.LanternAmber)
            }
        }
    }
}

/**
 * 魔物与区域首领 2D 生物图腾宫格项。
 */
@Composable
fun EnemyPortraitTile(
    enemy: EnemyDef,
    discovered: Boolean,
    killCount: Int,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = if (enemy.isBoss) DreamTrailColors.CoralWarn else regionAccentColor(enemy.regionId)
    val masteryStars = when {
        killCount >= 15 -> 2
        killCount >= 5 -> 1
        else -> 0
    }

    Surface(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) DreamTrailColors.CardElevated else DreamTrailColors.CardSurface,
        border = BorderStroke(
            width = if (isSelected || enemy.isBoss) 1.5.dp else 1.dp,
            color = when {
                isSelected -> DreamTrailColors.LanternAmber
                !discovered -> DreamTrailColors.CardOutline
                else -> accent.copy(alpha = 0.65f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DreamTrailColors.NightBackground)
                    .border(1.dp, accent.copy(alpha = if (discovered) 0.45f else 0.2f), RoundedCornerShape(10.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .alpha(if (discovered) 1f else 0.28f)
                ) {
                    drawEnemyArtwork(enemy, if (discovered) accent else DreamTrailColors.MutedInk)
                }
                if (enemy.isBoss) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = DreamTrailColors.CoralWarn,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(3.dp)
                    ) {
                        Text(
                            text = "首领",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A0A0A),
                            modifier = Modifier.padding(horizontal = 3.dp)
                        )
                    }
                }
                if (discovered && masteryStars > 0) {
                    Text(
                        text = if (masteryStars >= 2) "★★" else "★",
                        fontSize = 10.sp,
                        color = DreamTrailColors.LanternAmber,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                if (!discovered) {
                    Text(
                        text = "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DreamTrailColors.MutedInk,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Text(
                text = if (discovered) enemy.name else "未遭遇生灵",
                style = MaterialTheme.typography.labelSmall,
                color = if (discovered) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun DrawScope.drawEnemyArtwork(enemy: EnemyDef, accent: Color) {
    val w = size.width
    val h = size.height
    val id = enemy.id
    val eyeColor = DreamTrailColors.LanternAmber

    when {
        id.contains("slime") || id.contains("jelly") -> {
            // Blob or glowing jellyfish dome + tentacles
            val dome = Path().apply {
                moveTo(w * 0.18f, h * 0.62f)
                quadraticTo(w * 0.50f, h * 0.14f, w * 0.82f, h * 0.62f)
                close()
            }
            drawPath(dome, color = accent.copy(alpha = 0.82f))
            if (id.contains("jelly")) {
                for (i in -1..1) {
                    drawLine(accent.copy(alpha = 0.7f), Offset(w * (0.5f + i * 0.16f), h * 0.62f), Offset(w * (0.48f + i * 0.18f), h * 0.88f), strokeWidth = 3f, cap = StrokeCap.Round)
                }
            } else {
                drawCircle(eyeColor, radius = 3.5f, center = Offset(w * 0.40f, h * 0.50f))
                drawCircle(eyeColor, radius = 3.5f, center = Offset(w * 0.60f, h * 0.50f))
            }
        }
        id.contains("moth") || id.contains("hawk") || id.contains("gull") || id.contains("harpy") -> {
            // Winged creature
            val wings = Path().apply {
                moveTo(w * 0.50f, h * 0.45f)
                quadraticTo(w * 0.18f, h * 0.16f, w * 0.14f, h * 0.52f)
                lineTo(w * 0.50f, h * 0.62f)
                lineTo(w * 0.86f, h * 0.52f)
                quadraticTo(w * 0.82f, h * 0.16f, w * 0.50f, h * 0.45f)
                close()
            }
            drawPath(wings, color = accent.copy(alpha = 0.85f))
            drawCircle(DreamTrailColors.ParchmentText, radius = w * 0.10f, center = Offset(w * 0.50f, h * 0.46f))
            drawCircle(eyeColor, radius = 2.5f, center = Offset(w * 0.46f, h * 0.44f))
            drawCircle(eyeColor, radius = 2.5f, center = Offset(w * 0.54f, h * 0.44f))
        }
        id.contains("stag") || id.contains("ram") -> {
            // Antlered / horned silhouette
            drawLine(DreamTrailColors.ParchmentText, Offset(w * 0.40f, h * 0.44f), Offset(w * 0.22f, h * 0.16f), strokeWidth = 4f, cap = StrokeCap.Round)
            drawLine(DreamTrailColors.ParchmentText, Offset(w * 0.60f, h * 0.44f), Offset(w * 0.78f, h * 0.16f), strokeWidth = 4f, cap = StrokeCap.Round)
            drawLine(accent, Offset(w * 0.30f, h * 0.30f), Offset(w * 0.18f, h * 0.28f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(accent, Offset(w * 0.70f, h * 0.30f), Offset(w * 0.82f, h * 0.28f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawCircle(accent, radius = w * 0.22f, center = Offset(w * 0.50f, h * 0.60f))
            drawCircle(eyeColor, radius = 3f, center = Offset(w * 0.42f, h * 0.56f))
            drawCircle(eyeColor, radius = 3f, center = Offset(w * 0.58f, h * 0.56f))
        }
        id.contains("whale") || id.contains("leviathan") || id.contains("manta") || id.contains("ray") || id.contains("eel") -> {
            // Marine / sky leviathan silhouette
            val body = Path().apply {
                moveTo(w * 0.14f, h * 0.54f)
                quadraticTo(w * 0.46f, h * 0.22f, w * 0.84f, h * 0.52f)
                quadraticTo(w * 0.52f, h * 0.78f, w * 0.14f, h * 0.54f)
                close()
            }
            drawPath(body, color = accent.copy(alpha = 0.88f))
            drawCircle(eyeColor, radius = 4f, center = Offset(w * 0.68f, h * 0.48f))
            drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.08f, center = Offset(w * 0.46f, h * 0.30f))
        }
        id.contains("crab") || id.contains("tortoise") || id.contains("beetle") || id.contains("urchin") -> {
            // Shelled creature
            drawCircle(accent.copy(alpha = 0.85f), radius = w * 0.30f, center = Offset(w * 0.50f, h * 0.56f))
            drawCircle(DreamTrailColors.ParchmentText.copy(alpha = 0.55f), radius = w * 0.20f, center = Offset(w * 0.50f, h * 0.54f), style = Stroke(width = 2.5f))
            drawCircle(eyeColor, radius = 3f, center = Offset(w * 0.40f, h * 0.46f))
            drawCircle(eyeColor, radius = 3f, center = Offset(w * 0.60f, h * 0.46f))
        }
        id.contains("chronokeeper") || id.contains("automaton") || id.contains("warden") || id.contains("golem") || id.contains("sentinel") || id.contains("guard") || id.contains("chronicler") -> {
            // Construct / clockwork guardian
            drawRoundRect(accent.copy(alpha = 0.86f), Offset(w * 0.26f, h * 0.22f), Size(w * 0.48f, h * 0.62f), CornerRadius(10f, 10f))
            drawCircle(DreamTrailColors.NightBackground, radius = w * 0.14f, center = Offset(w * 0.50f, h * 0.46f))
            drawCircle(eyeColor, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.46f))
            drawLine(DreamTrailColors.ParchmentText, Offset(w * 0.34f, h * 0.70f), Offset(w * 0.66f, h * 0.70f), strokeWidth = 3f)
        }
        else -> {
            // Fox / Hound / Lynx / Wisp / Sprite
            val head = Path().apply {
                moveTo(w * 0.24f, h * 0.22f)
                lineTo(w * 0.40f, h * 0.38f)
                lineTo(w * 0.60f, h * 0.38f)
                lineTo(w * 0.76f, h * 0.22f)
                lineTo(w * 0.76f, h * 0.64f)
                lineTo(w * 0.50f, h * 0.84f)
                lineTo(w * 0.24f, h * 0.64f)
                close()
            }
            drawPath(head, color = accent.copy(alpha = 0.88f))
            drawCircle(eyeColor, radius = 3.5f, center = Offset(w * 0.40f, h * 0.55f))
            drawCircle(eyeColor, radius = 3.5f, center = Offset(w * 0.60f, h * 0.55f))
        }
    }
}

@Composable
fun PassiveEmblemTile(
    passive: PassiveDef,
    unlocked: Boolean,
    equipped: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = archetypeColor(passive.archetype)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (equipped) DreamTrailColors.CardElevated else DreamTrailColors.CardSurface,
        border = BorderStroke(
            width = if (equipped) 1.6.dp else 1.dp,
            color = when {
                equipped -> DreamTrailColors.LanternAmber
                unlocked -> accent.copy(alpha = 0.5f)
                else -> DreamTrailColors.CardOutline
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DreamTrailColors.NightBackground)
                    .alpha(if (unlocked) 1f else 0.4f),
                contentAlignment = Alignment.Center
            ) {
                ArchetypeEmblem(archetype = passive.archetype, size = 28.dp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = passive.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (unlocked) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (equipped) {
                        TagBadge("生效", DreamTrailColors.LanternAmber)
                    } else if (!unlocked) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = DreamTrailColors.MutedInk,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = if (unlocked) passive.description else passive.unlockHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (unlocked) DreamTrailColors.MutedInk else DreamTrailColors.MutedInk.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SouvenirIconTile(
    souvenir: SouvenirDef,
    unlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = regionAccentColor(souvenir.regionId)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (unlocked) DreamTrailColors.CardSurface else DreamTrailColors.NightBackground,
        border = BorderStroke(1.dp, if (unlocked) accent.copy(alpha = 0.7f) else DreamTrailColors.CardOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DreamTrailColors.CardElevated.copy(alpha = if (unlocked) 0.9f else 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(9.dp)
                        .alpha(if (unlocked) 1f else 0.25f)
                ) {
                    drawCurioIcon(souvenir.id, accent)
                }
                if (!unlocked) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = DreamTrailColors.MutedInk,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = if (unlocked) souvenir.name else "未收录风物",
                style = MaterialTheme.typography.labelSmall,
                color = if (unlocked) DreamTrailColors.ParchmentText else DreamTrailColors.MutedInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = souvenir.regionId.displayName,
                fontSize = 10.sp,
                color = accent
            )
        }
    }
}

@Composable
fun KeepsakeIconTile(
    keepsake: KeepsakeDef,
    unlocked: Boolean,
    affordable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = regionAccentColor(keepsake.regionId)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (unlocked) DreamTrailColors.CardElevated else DreamTrailColors.CardSurface,
        border = BorderStroke(
            width = if (unlocked || affordable) 1.5.dp else 1.dp,
            color = when {
                unlocked -> DreamTrailColors.LanternAmber
                affordable -> DreamTrailColors.SageMist
                else -> DreamTrailColors.CardOutline
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DreamTrailColors.NightBackground),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(9.dp)
                        .alpha(if (unlocked) 1f else if (affordable) 0.85f else 0.45f)
                ) {
                    drawCurioIcon(keepsake.id, if (unlocked) DreamTrailColors.LanternAmber else accent)
                }
                if (unlocked) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = DreamTrailColors.LanternAmber,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                    ) {
                        Text(
                            text = "陈列",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = DreamTrailColors.LanternInk,
                            modifier = Modifier.padding(horizontal = 3.dp)
                        )
                    }
                } else if (affordable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(DreamTrailColors.SageMist)
                    )
                }
            }
            Text(
                text = keepsake.name,
                style = MaterialTheme.typography.labelSmall,
                color = DreamTrailColors.ParchmentText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (unlocked) "永久生效中" else "需${keepsake.bossMarkCost}印记",
                fontSize = 10.sp,
                color = if (unlocked) DreamTrailColors.LanternAmber else DreamTrailColors.MutedInk
            )
        }
    }
}

private fun DrawScope.drawCurioIcon(id: String, accent: Color) {
    val w = size.width
    val h = size.height
    drawCircle(accent.copy(alpha = 0.18f), radius = w * 0.46f, center = Offset(w / 2f, h / 2f))
    when {
        id.contains("lantern") || id.contains("vial") -> {
            drawRoundRect(accent, Offset(w * 0.30f, h * 0.24f), Size(w * 0.40f, h * 0.56f), CornerRadius(8f, 8f))
            drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.12f, center = Offset(w * 0.50f, h * 0.54f))
        }
        id.contains("key") || id.contains("pendulum") -> {
            drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.15f, center = Offset(w * 0.50f, h * 0.32f), style = Stroke(3f))
            drawLine(accent, Offset(w * 0.50f, h * 0.47f), Offset(w * 0.50f, h * 0.84f), strokeWidth = 4f, cap = StrokeCap.Round)
            drawCircle(accent, radius = w * 0.10f, center = Offset(w * 0.50f, h * 0.80f))
        }
        id.contains("orrery") || id.contains("astrolabe") || id.contains("catcher") -> {
            drawCircle(accent, radius = w * 0.32f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(2.5f))
            drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.18f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(2f))
            drawCircle(DreamTrailColors.ParchmentText, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.50f))
        }
        id.contains("kettle") || id.contains("porcelain") -> {
            drawRoundRect(accent, Offset(w * 0.24f, h * 0.38f), Size(w * 0.46f, h * 0.40f), CornerRadius(10f, 10f))
            drawLine(DreamTrailColors.LanternAmber, Offset(w * 0.70f, h * 0.48f), Offset(w * 0.84f, h * 0.34f), strokeWidth = 4f, cap = StrokeCap.Round)
        }
        else -> {
            // Gem / conch / leaf / chime relic
            val diamond = Path().apply {
                moveTo(w * 0.50f, h * 0.14f)
                lineTo(w * 0.82f, h * 0.50f)
                lineTo(w * 0.50f, h * 0.86f)
                lineTo(w * 0.18f, h * 0.50f)
                close()
            }
            drawPath(diamond, color = accent.copy(alpha = 0.88f))
            drawPath(diamond, color = DreamTrailColors.ParchmentText.copy(alpha = 0.7f), style = Stroke(2f))
        }
    }
}

@Composable
fun TeaCupIconBox(
    tea: TeaBlendDef?,
    unlocked: Boolean = true,
    active: Boolean = false,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val teaTint = when (tea?.id) {
        "tea_chamomile" -> Color(0xFFE5C15C)
        "tea_sea_salt_mint" -> Color(0xFF7FD1BE)
        "tea_star_anise" -> Color(0xFFE08C64)
        "tea_cloud_oolong" -> Color(0xFF9ACCE0)
        "tea_pine_needle" -> DreamTrailColors.SageMist
        "tea_silver_moon_oolong" -> Color(0xFFC8B4EB)
        "tea_astral_amber_brew" -> DreamTrailColors.LanternAmber
        "tea_rosemary_lemon" -> Color(0xFFCEE376)
        "tea_acorn_cocoa" -> Color(0xFFC68B59)
        "tea_sea_breeze_cold_brew" -> Color(0xFF6BB2DF)
        "tea_cloud_matcha" -> Color(0xFF7FD89B)
        "tea_sunlit_pour_over" -> Color(0xFFE6A24C)
        "tea_soluna_nectar" -> Color(0xFFF3BA78)
        else -> DreamTrailColors.MutedInk
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) DreamTrailColors.LanternAmber.copy(alpha = 0.18f) else DreamTrailColors.NightBackground)
            .border(
                1.dp,
                if (active) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline,
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(7.dp)
                .alpha(if (unlocked) 1f else 0.35f)
        ) {
            val w = this.size.width
            val h = this.size.height
            // Steam lines
            drawLine(teaTint.copy(alpha = 0.7f), Offset(w * 0.36f, h * 0.30f), Offset(w * 0.32f, h * 0.12f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            drawLine(teaTint.copy(alpha = 0.7f), Offset(w * 0.56f, h * 0.28f), Offset(w * 0.60f, h * 0.10f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            // Cup body
            val cup = Path().apply {
                moveTo(w * 0.20f, h * 0.42f)
                lineTo(w * 0.76f, h * 0.42f)
                quadraticTo(w * 0.72f, h * 0.84f, w * 0.48f, h * 0.84f)
                quadraticTo(w * 0.24f, h * 0.84f, w * 0.20f, h * 0.42f)
                close()
            }
            drawPath(cup, color = DreamTrailColors.ParchmentText.copy(alpha = 0.88f))
            drawLine(teaTint, Offset(w * 0.24f, h * 0.48f), Offset(w * 0.72f, h * 0.48f), strokeWidth = 4f, cap = StrokeCap.Round)
            // Handle
            drawCircle(
                color = DreamTrailColors.ParchmentText.copy(alpha = 0.85f),
                radius = w * 0.12f,
                center = Offset(w * 0.79f, h * 0.58f),
                style = Stroke(width = 2.5f)
            )
            // Saucer
            drawLine(DreamTrailColors.MutedInk, Offset(w * 0.18f, h * 0.88f), Offset(w * 0.78f, h * 0.88f), strokeWidth = 3f, cap = StrokeCap.Round)
        }
    }
}

enum class ResourceVisualKind {
    GOLD,
    XP,
    MATERIAL,
    BOSS_MARK,
    INK
}

@Composable
fun ResourceBadgeIcon(
    kind: ResourceVisualKind,
    accent: Color,
    size: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        when (kind) {
            ResourceVisualKind.GOLD -> {
                drawCircle(accent, radius = w * 0.42f, center = Offset(cx, cy))
                drawCircle(DreamTrailColors.LanternInk.copy(alpha = 0.55f), radius = w * 0.24f, center = Offset(cx, cy), style = Stroke(1.6f))
            }
            ResourceVisualKind.XP -> {
                val star = Path().apply {
                    for (i in 0 until 8) {
                        val r = if (i % 2 == 0) w * 0.45f else w * 0.18f
                        val a = i * PI / 4.0 - PI / 2.0
                        val x = cx + (r * cos(a)).toFloat()
                        val y = cy + (r * sin(a)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(star, color = accent)
            }
            ResourceVisualKind.MATERIAL -> {
                val hex = Path().apply {
                    for (i in 0 until 6) {
                        val a = i * PI / 3.0 - PI / 6.0
                        val x = cx + (w * 0.42f * cos(a)).toFloat()
                        val y = cy + (h * 0.42f * sin(a)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(hex, color = accent.copy(alpha = 0.88f))
                drawCircle(DreamTrailColors.ParchmentText.copy(alpha = 0.7f), radius = w * 0.12f, center = Offset(cx, cy))
            }
            ResourceVisualKind.BOSS_MARK -> {
                drawCircle(accent.copy(alpha = 0.25f), radius = w * 0.46f, center = Offset(cx, cy))
                drawCircle(accent, radius = w * 0.34f, center = Offset(cx, cy), style = Stroke(2f))
                drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.14f, center = Offset(cx, cy))
            }
            ResourceVisualKind.INK -> {
                val drop = Path().apply {
                    moveTo(cx, h * 0.12f)
                    lineTo(w * 0.76f, h * 0.56f)
                    lineTo(cx, h * 0.88f)
                    lineTo(w * 0.24f, h * 0.56f)
                    close()
                }
                drawPath(drop, color = accent.copy(alpha = 0.9f))
                drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.12f, center = Offset(cx, cy + h * 0.04f))
            }
        }
    }
}

@Composable
fun FocusSpiritAvatar(
    spirit: FocusSpiritDef,
    size: Dp = 44.dp,
    unlocked: Boolean = true,
    active: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accent = regionAccentColor(spirit.regionId)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) accent.copy(alpha = 0.22f) else DreamTrailColors.NightBackground)
            .border(
                width = if (active) 1.5.dp else 1.dp,
                color = if (active) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .alpha(if (unlocked) 1f else 0.35f)
        ) {
            val w = this.size.width
            val h = this.size.height
            drawCircle(accent.copy(alpha = 0.20f), radius = w * 0.44f, center = Offset(w * 0.5f, h * 0.52f))
            drawCircle(accent, radius = w * 0.28f, center = Offset(w * 0.5f, h * 0.56f))
            // Cute ears
            val ears = Path().apply {
                moveTo(w * 0.28f, h * 0.42f)
                lineTo(w * 0.32f, h * 0.18f)
                lineTo(w * 0.44f, h * 0.34f)
                moveTo(w * 0.56f, h * 0.34f)
                lineTo(w * 0.68f, h * 0.18f)
                lineTo(w * 0.72f, h * 0.42f)
            }
            drawPath(ears, color = accent)
            drawCircle(DreamTrailColors.ParchmentText, radius = w * 0.06f, center = Offset(w * 0.42f, h * 0.54f))
            drawCircle(DreamTrailColors.ParchmentText, radius = w * 0.06f, center = Offset(w * 0.58f, h * 0.54f))
        }
        if (!unlocked) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = DreamTrailColors.MutedInk,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun StrategyEmblem(
    strategy: ExpeditionStrategy,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val accent = when (strategy) {
        ExpeditionStrategy.STEADY -> DreamTrailColors.SageMist
        ExpeditionStrategy.EXPLORE -> DreamTrailColors.StarlightBlue
        ExpeditionStrategy.GATHER -> DreamTrailColors.LanternAmber
    }
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        drawCircle(accent.copy(alpha = 0.18f), radius = w * 0.48f, center = Offset(cx, cy))
        when (strategy) {
            ExpeditionStrategy.STEADY -> {
                val p = Path().apply {
                    moveTo(cx, h * 0.18f)
                    lineTo(w * 0.76f, h * 0.32f)
                    lineTo(w * 0.70f, h * 0.65f)
                    lineTo(cx, h * 0.84f)
                    lineTo(w * 0.30f, h * 0.65f)
                    lineTo(w * 0.24f, h * 0.32f)
                    close()
                }
                drawPath(p, color = accent)
            }
            ExpeditionStrategy.EXPLORE -> {
                drawCircle(accent, radius = w * 0.34f, center = Offset(cx, cy), style = Stroke(2.2f))
                val needle = Path().apply {
                    moveTo(w * 0.66f, h * 0.34f)
                    lineTo(w * 0.56f, h * 0.56f)
                    lineTo(w * 0.34f, h * 0.66f)
                    lineTo(w * 0.44f, h * 0.44f)
                    close()
                }
                drawPath(needle, color = DreamTrailColors.LanternAmber)
            }
            ExpeditionStrategy.GATHER -> {
                drawRoundRect(accent, Offset(w * 0.24f, h * 0.34f), Size(w * 0.52f, h * 0.46f), CornerRadius(6f, 6f))
                drawCircle(accent, radius = w * 0.14f, center = Offset(cx, h * 0.30f), style = Stroke(2.4f))
            }
        }
    }
}

@Composable
fun EventBadgeIcon(
    isFirstDiscovery: Boolean,
    title: String,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val accent = when {
        isFirstDiscovery -> DreamTrailColors.LanternAmber
        title.contains("首领") || title.contains("战") || title.contains("伏击") -> DreamTrailColors.CoralWarn
        title.contains("休") || title.contains("茶") || title.contains("营") -> DreamTrailColors.SageMist
        else -> DreamTrailColors.StarlightBlue
    }
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        drawCircle(accent.copy(alpha = 0.20f), radius = w * 0.48f, center = Offset(cx, cy))
        drawCircle(accent.copy(alpha = 0.7f), radius = w * 0.46f, center = Offset(cx, cy), style = Stroke(1.6f))
        if (isFirstDiscovery) {
            drawCircle(DreamTrailColors.LanternAmber, radius = w * 0.18f, center = Offset(cx, cy))
        } else {
            drawCircle(accent, radius = w * 0.16f, center = Offset(cx, cy))
        }
    }
}
