package com.dreamtrail.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.dreamtrail.app.ui.theme.DreamTrailColors
import kotlin.math.sin

/**
 * Original night-travel journal scene rendered via Jetpack Compose Canvas.
 * Animation runs only while foregrounded and reduceMotion is false.
 */
@Composable
fun CampfireTravelerScene(
    isTraveling: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val phase = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "travelScene")
        val animated by transition.animateFloat(
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
            .height(108.dp)
    ) {
        val w = size.width
        val h = size.height

        // Sky backdrop
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF101520), Color(0xFF1A2434))
            ),
            size = size
        )

        // Subtle stars
        val starOffsets = listOf(
            0.12f to 0.22f, 0.28f to 0.14f, 0.44f to 0.26f,
            0.66f to 0.16f, 0.81f to 0.25f, 0.90f to 0.12f
        )
        starOffsets.forEachIndexed { idx, (sx, sy) ->
            val alpha = if (reduceMotion) 0.55f else 0.45f + 0.25f * sin(phase + idx * 1.1f)
            drawCircle(
                color = DreamTrailColors.ParchmentText.copy(alpha = alpha.coerceIn(0.2f, 0.8f)),
                radius = if (idx % 2 == 0) 2.2f else 1.5f,
                center = Offset(w * sx, h * sy)
            )
        }

        // Crescent moon
        drawCircle(
            color = DreamTrailColors.LanternAmber.copy(alpha = 0.85f),
            radius = 12f,
            center = Offset(w * 0.84f, h * 0.26f)
        )
        drawCircle(
            color = Color(0xFF121826),
            radius = 10f,
            center = Offset(w * 0.84f + 5f, h * 0.26f - 3f)
        )

        // Distant rolling ridge
        val hillPath = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.70f)
            quadraticTo(w * 0.28f, h * 0.52f, w * 0.56f, h * 0.68f)
            quadraticTo(w * 0.80f, h * 0.80f, w, h * 0.62f)
            lineTo(w, h)
            close()
        }
        drawPath(hillPath, color = Color(0xFF212C3C))

        // Ground strip
        drawRect(
            color = Color(0xFF161E29),
            topLeft = Offset(0f, h * 0.78f),
            size = Size(w, h * 0.22f)
        )

        val bob = if (reduceMotion) 0f else sin(phase) * (if (isTraveling) 2.6f else 1.2f)
        val travelerX = if (isTraveling) w * 0.36f else w * 0.32f
        val groundY = h * 0.78f

        // Warm lantern or campfire glow
        val glowCenter = if (isTraveling) Offset(travelerX + 18f, groundY - 22f + bob) else Offset(w * 0.54f, groundY - 6f)
        val glowRadius = if (reduceMotion) 46f else 44f + sin(phase * 2f) * 4f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    DreamTrailColors.LanternAmber.copy(alpha = 0.34f),
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
                topLeft = Offset(w * 0.54f - 12f, groundY - 4f),
                size = Size(24f, 5f)
            )
            val flamePath = Path().apply {
                moveTo(w * 0.54f - 7f, groundY - 3f)
                quadraticTo(w * 0.54f, groundY - 22f - bob, w * 0.54f + 7f, groundY - 3f)
                close()
            }
            drawPath(flamePath, color = DreamTrailColors.LanternAmber)
        } else {
            // Lantern held forward
            drawLine(
                color = DreamTrailColors.MutedInk,
                start = Offset(travelerX + 6f, groundY - 22f + bob),
                end = Offset(travelerX + 18f, groundY - 25f + bob),
                strokeWidth = 2.5f
            )
            drawCircle(
                color = DreamTrailColors.LanternAmber,
                radius = 4.5f,
                center = Offset(travelerX + 18f, groundY - 21f + bob)
            )
        }

        // Traveler cloak & hood silhouette
        val cloakPath = Path().apply {
            moveTo(travelerX - 11f, groundY)
            lineTo(travelerX - 5f, groundY - 28f + bob)
            lineTo(travelerX + 7f, groundY - 28f + bob)
            lineTo(travelerX + 12f, groundY)
            close()
        }
        drawPath(cloakPath, color = DreamTrailColors.SageMist)
        drawCircle(
            color = DreamTrailColors.ParchmentText,
            radius = 7f,
            center = Offset(travelerX + 1f, groundY - 34f + bob)
        )
    }
}
