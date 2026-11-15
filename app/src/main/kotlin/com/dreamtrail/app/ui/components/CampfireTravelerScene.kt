package com.dreamtrail.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.core.model.NightPhenomenon
import com.dreamtrail.core.model.RegionId
import kotlin.math.sin

/**
 * Original night-travel journal scene rendered via Jetpack Compose Canvas.
 * Adapts terrain to the selected region, draws the active companion beside the traveler,
 * and respects reduceMotion.
 */
@Composable
fun CampfireTravelerScene(
    isTraveling: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    regionId: RegionId = RegionId.MISTWOOD,
    companionId: String? = null,
    nightPhenomenon: NightPhenomenon? = null,
    height: Dp = 122.dp
) {
    val phaseState = if (reduceMotion) {
        null
    } else {
        val transition = rememberInfiniteTransition(label = "travelScene")
        val animated = transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (isTraveling) 2800 else 3600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        animated
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val phase = phaseState?.value ?: 0f
        val w = size.width
        val h = size.height

        drawRegionBackdrop(regionId, phase, reduceMotion, nightPhenomenon)

        val bob = if (reduceMotion) 0f else sin(phase) * (if (isTraveling) 2.5f else 1.1f)
        val travelerX = if (isTraveling) w * 0.42f else w * 0.34f
        val groundY = h * 0.78f

        // Warm lantern or campfire glow
        val glowCenter = if (isTraveling) Offset(travelerX + 20f, groundY - 22f + bob) else Offset(w * 0.54f, groundY - 8f)
        val glowRadius = if (reduceMotion) 50f else 48f + sin(phase * 2f) * 4f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    DreamTrailColors.LanternAmber.copy(alpha = 0.36f),
                    Color.Transparent
                ),
                center = glowCenter,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = glowCenter
        )

        if (!isTraveling) {
            // Campfire logs & flame
            drawRect(
                color = Color(0xFF6B4C35),
                topLeft = Offset(w * 0.54f - 14f, groundY - 5f),
                size = Size(28f, 6f)
            )
            val flamePath = Path().apply {
                moveTo(w * 0.54f - 8f, groundY - 3f)
                quadraticTo(w * 0.54f, groundY - 24f - bob, w * 0.54f + 8f, groundY - 3f)
                close()
            }
            drawPath(flamePath, color = DreamTrailColors.LanternAmber)
            val innerFlame = Path().apply {
                moveTo(w * 0.54f - 4f, groundY - 3f)
                quadraticTo(w * 0.54f, groundY - 15f - bob, w * 0.54f + 4f, groundY - 3f)
                close()
            }
            drawPath(innerFlame, color = Color(0xFFFFE0A3))
        } else {
            // Lantern held forward
            drawLine(
                color = DreamTrailColors.MutedInk,
                start = Offset(travelerX + 7f, groundY - 22f + bob),
                end = Offset(travelerX + 20f, groundY - 26f + bob),
                strokeWidth = 2.5f
            )
            drawCircle(
                color = DreamTrailColors.LanternAmber,
                radius = 4.8f,
                center = Offset(travelerX + 20f, groundY - 21f + bob)
            )
        }

        // Companion sprite walking behind or resting beside the fire
        if (companionId != null) {
            val compBob = if (reduceMotion) 0f else sin(phase + 0.9f) * (if (isTraveling) 2.2f else 1.0f)
            val compX = if (isTraveling) travelerX - 34f else w * 0.67f
            drawCompanionMiniSprite(
                companionId = companionId,
                x = compX,
                groundY = groundY,
                bob = compBob,
                facingRight = isTraveling
            )
        }

        // Traveler cloak & hood silhouette
        val cloakPath = Path().apply {
            moveTo(travelerX - 12f, groundY)
            lineTo(travelerX - 5f, groundY - 29f + bob)
            lineTo(travelerX + 8f, groundY - 29f + bob)
            lineTo(travelerX + 13f, groundY)
            close()
        }
        drawPath(cloakPath, color = DreamTrailColors.SageMist)
        drawCircle(
            color = DreamTrailColors.ParchmentText,
            radius = 7f,
            center = Offset(travelerX + 1f, groundY - 35f + bob)
        )
        // Hood rim
        drawArc(
            color = DreamTrailColors.SageMist,
            startAngle = 190f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(travelerX - 7f, groundY - 43f + bob),
            size = Size(16f, 14f)
        )
    }
}

fun DrawScope.drawRegionBackdrop(
    regionId: RegionId,
    phase: Float = 0f,
    reduceMotion: Boolean = true,
    nightPhenomenon: NightPhenomenon? = null
) {
    val w = size.width
    val h = size.height

    val (skyTop, skyBottom, hillColor, groundColor) = when (regionId) {
        RegionId.MISTWOOD -> listOf(Color(0xFF0E161E), Color(0xFF19282E), Color(0xFF1F3234), Color(0xFF142122))
        RegionId.TIDAL_FLAT -> listOf(Color(0xFF0D1524), Color(0xFF17273C), Color(0xFF1D334C), Color(0xFF132234))
        RegionId.STARLIT_RIDGE -> listOf(Color(0xFF121226), Color(0xFF20203E), Color(0xFF2A2B4C), Color(0xFF1B1C31))
        RegionId.CLOUD_SANCTUARY -> listOf(Color(0xFF0F1A28), Color(0xFF1A313D), Color(0xFF24414B), Color(0xFF192E38))
        RegionId.DREAM_SEA -> listOf(Color(0xFF0A1228), Color(0xFF162448), Color(0xFF1A2D58), Color(0xFF111E3D))
        RegionId.ASTRAL_ARCH -> listOf(Color(0xFF1A1424), Color(0xFF2A2136), Color(0xFF342A3E), Color(0xFF1E1829))
    }

    drawRect(
        brush = Brush.verticalGradient(colors = listOf(skyTop, skyBottom)),
        size = size
    )

    // Stars
    val starOffsets = listOf(
        0.10f to 0.20f, 0.22f to 0.13f, 0.36f to 0.24f,
        0.52f to 0.14f, 0.67f to 0.20f, 0.79f to 0.12f, 0.91f to 0.23f
    )
    starOffsets.forEachIndexed { idx, (sx, sy) ->
        val alpha = if (reduceMotion) 0.55f else 0.42f + 0.30f * sin(phase + idx * 1.1f)
        drawCircle(
            color = DreamTrailColors.ParchmentText.copy(alpha = alpha.coerceIn(0.2f, 0.85f)),
            radius = if (idx % 2 == 0) 2.2f else 1.5f,
            center = Offset(w * sx, h * sy)
        )
    }

    // Moon / celestial motif
    val moonTint = when (nightPhenomenon?.favoredRegionId) {
        regionId -> DreamTrailColors.LanternAmber
        else -> DreamTrailColors.LanternAmber.copy(alpha = 0.82f)
    }
    drawCircle(color = moonTint, radius = 13f, center = Offset(w * 0.85f, h * 0.24f))
    drawCircle(color = skyTop, radius = 10.5f, center = Offset(w * 0.85f + 5f, h * 0.24f - 3f))

    // Region-specific silhouettes
    when (regionId) {
        RegionId.MISTWOOD -> {
            // Pine silhouettes & lantern glow blossoms
            for (xFrac in listOf(0.10f, 0.20f, 0.72f, 0.88f)) {
                val tx = w * xFrac
                val tree = Path().apply {
                    moveTo(tx, h * 0.34f)
                    lineTo(tx - 16f, h * 0.78f)
                    lineTo(tx + 16f, h * 0.78f)
                    close()
                }
                drawPath(tree, color = hillColor)
            }
            drawCircle(DreamTrailColors.LanternAmber.copy(alpha = 0.45f), radius = 4f, center = Offset(w * 0.15f, h * 0.72f))
            drawCircle(DreamTrailColors.LanternAmber.copy(alpha = 0.45f), radius = 4f, center = Offset(w * 0.78f, h * 0.70f))
        }
        RegionId.TIDAL_FLAT -> {
            // Distant lighthouse & ocean beam
            val lhX = w * 0.76f
            drawRect(hillColor, Offset(lhX - 7f, h * 0.42f), Size(14f, h * 0.36f))
            drawCircle(DreamTrailColors.LanternAmber.copy(alpha = 0.65f), radius = 6f, center = Offset(lhX, h * 0.41f))
            val beam = Path().apply {
                moveTo(lhX, h * 0.41f)
                lineTo(lhX - 70f, h * 0.28f)
                lineTo(lhX - 60f, h * 0.50f)
                close()
            }
            drawPath(beam, color = DreamTrailColors.LanternAmber.copy(alpha = 0.12f))
        }
        RegionId.STARLIT_RIDGE -> {
            // Sharp mountain peaks & observatory dome
            val peaks = Path().apply {
                moveTo(0f, h * 0.78f)
                lineTo(w * 0.18f, h * 0.42f)
                lineTo(w * 0.36f, h * 0.66f)
                lineTo(w * 0.62f, h * 0.38f)
                lineTo(w * 0.84f, h * 0.64f)
                lineTo(w, h * 0.46f)
                lineTo(w, h * 0.78f)
                close()
            }
            drawPath(peaks, color = hillColor)
            drawArc(
                color = Color(0xFF42456D),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.68f, h * 0.46f),
                size = Size(34f, 32f)
            )
        }
        RegionId.CLOUD_SANCTUARY -> {
            // Soft cloud banks & floating organ spire
            for (cx in listOf(0.18f, 0.50f, 0.80f)) {
                drawCircle(hillColor.copy(alpha = 0.75f), radius = 26f, center = Offset(w * cx, h * 0.72f))
                drawCircle(hillColor.copy(alpha = 0.65f), radius = 18f, center = Offset(w * cx + 20f, h * 0.74f))
            }
            drawRect(Color(0xFF335B66), Offset(w * 0.74f, h * 0.38f), Size(8f, 34f))
            drawRect(Color(0xFF335B66), Offset(w * 0.79f, h * 0.32f), Size(8f, 40f))
        }
        RegionId.DREAM_SEA -> {
            // Glowing jellyfish & starry sea reflection
            for (jx in listOf(0.16f, 0.75f)) {
                val jy = h * (if (jx < 0.5f) 0.38f else 0.44f) + (if (reduceMotion) 0f else sin(phase + jx * 4f) * 3f)
                drawArc(Color(0xFF8AB8FF).copy(alpha = 0.55f), 180f, 180f, true, Offset(w * jx - 10f, jy - 8f), Size(20f, 16f))
                drawLine(Color(0xFF8AB8FF).copy(alpha = 0.38f), Offset(w * jx - 4f, jy), Offset(w * jx - 5f, jy + 12f), strokeWidth = 1.5f)
                drawLine(Color(0xFF8AB8FF).copy(alpha = 0.38f), Offset(w * jx + 4f, jy), Offset(w * jx + 5f, jy + 12f), strokeWidth = 1.5f)
            }
        }
        RegionId.ASTRAL_ARCH -> {
            // Floating stone astral arch & golden sundial ring
            val archX = w * 0.74f
            drawArc(
                color = Color(0xFF564762),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(archX - 24f, h * 0.34f),
                size = Size(48f, 44f),
                style = Stroke(width = 6f)
            )
            drawLine(Color(0xFF564762), Offset(archX - 24f, h * 0.56f), Offset(archX - 24f, h * 0.78f), strokeWidth = 6f)
            drawLine(Color(0xFF564762), Offset(archX + 24f, h * 0.56f), Offset(archX + 24f, h * 0.78f), strokeWidth = 6f)
            drawCircle(
                color = DreamTrailColors.LanternAmber.copy(alpha = 0.35f),
                radius = 22f,
                center = Offset(w * 0.24f, h * 0.42f),
                style = Stroke(width = 1.6f)
            )
        }
    }

    // Distant rolling ridge
    val hillPath = Path().apply {
        moveTo(0f, h)
        lineTo(0f, h * 0.74f)
        quadraticTo(w * 0.28f, h * 0.62f, w * 0.58f, h * 0.73f)
        quadraticTo(w * 0.82f, h * 0.80f, w, h * 0.68f)
        lineTo(w, h)
        close()
    }
    drawPath(hillPath, color = hillColor.copy(alpha = 0.82f))

    // Ground strip
    drawRect(
        color = groundColor,
        topLeft = Offset(0f, h * 0.78f),
        size = Size(w, h * 0.22f)
    )
}

private fun DrawScope.drawCompanionMiniSprite(
    companionId: String,
    x: Float,
    groundY: Float,
    bob: Float,
    facingRight: Boolean
) {
    val cloakColor = companionAccentColor(companionId)
    val body = Path().apply {
        moveTo(x - 9f, groundY)
        lineTo(x - 4f, groundY - 25f + bob)
        lineTo(x + 5f, groundY - 25f + bob)
        lineTo(x + 9f, groundY)
        close()
    }
    drawPath(body, color = cloakColor.copy(alpha = 0.92f))
    drawCircle(DreamTrailColors.ParchmentText, radius = 5.8f, center = Offset(x, groundY - 30f + bob))

    val dir = if (facingRight) 1f else -1f
    when (companionId) {
        "comp_airen" -> {
            // Bow & ribbon
            drawLine(Color(0xFF6AB4EC), Offset(x - 5f * dir, groundY - 28f + bob), Offset(x - 13f * dir, groundY - 22f + bob), strokeWidth = 2.2f, cap = StrokeCap.Round)
            drawArc(DreamTrailColors.LanternAmber, -60f, 120f, false, Offset(x - 4f, groundY - 32f + bob), Size(14f, 18f), style = Stroke(2f))
        }
        "comp_noah" -> {
            // Brass telescope
            drawLine(DreamTrailColors.LanternAmber, Offset(x + 2f * dir, groundY - 22f + bob), Offset(x + 12f * dir, groundY - 28f + bob), strokeWidth = 2.8f, cap = StrokeCap.Round)
        }
        "comp_lin" -> {
            // Glowing flower
            drawCircle(Color(0xFFFFD88F), radius = 4f, center = Offset(x + 9f * dir, groundY - 18f + bob))
        }
        "comp_xiu" -> {
            // Starlight paintbrush
            drawLine(DreamTrailColors.StarlightBlue, Offset(x + 2f * dir, groundY - 16f + bob), Offset(x + 11f * dir, groundY - 26f + bob), strokeWidth = 2.2f, cap = StrokeCap.Round)
            drawCircle(DreamTrailColors.LanternAmber, radius = 2.6f, center = Offset(x + 12f * dir, groundY - 27f + bob))
        }
        "comp_cen" -> {
            // Crystal lantern
            drawCircle(DreamTrailColors.LanternAmber, radius = 3.8f, center = Offset(x + 10f * dir, groundY - 15f + bob))
        }
    }
}
