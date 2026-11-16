package com.dreamtrail.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dreamtrail.app.ui.theme.DreamTrailColors
import kotlinx.coroutines.delay
import kotlin.math.ceil

enum class BreathingPattern(
    val title: String,
    val subtitle: String,
    val inhaleSec: Int,
    val holdSec: Int,
    val exhaleSec: Int,
    val pauseSec: Int
) {
    SLEEP_478("4-7-8 助眠", "适合睡前或午休时放慢呼吸", 4, 7, 8, 0),
    BOX_4444("4-4-4-4 调息", "跟随均匀节奏，开始一段专注", 4, 4, 4, 4)
}

@Composable
fun BreathingGuideDialog(
    reduceMotion: Boolean,
    initialPattern: BreathingPattern = BreathingPattern.BOX_4444,
    onDismiss: () -> Unit
) {
    var pattern by remember { mutableStateOf(initialPattern) }
    val cycleLen = pattern.inhaleSec + pattern.holdSec + pattern.exhaleSec + pattern.pauseSec
    var elapsedTicks by remember(pattern) { mutableIntStateOf(0) }

    LaunchedEffect(pattern) {
        while (true) {
            delay(200L)
            elapsedTicks = (elapsedTicks + 1) % (cycleLen * 5)
        }
    }

    val secInCycle = elapsedTicks / 5f
    val (phaseName, remainingInt, radiusFraction, phaseColor) = when {
        secInCycle < pattern.inhaleSec -> {
            val left = ceil(pattern.inhaleSec - secInCycle).toInt()
            val frac = (secInCycle / pattern.inhaleSec).coerceIn(0f, 1f)
            Quadruple("深长吸气", left, 0.36f + 0.48f * frac, DreamTrailColors.SageMist)
        }
        secInCycle < pattern.inhaleSec + pattern.holdSec -> {
            val left = ceil(pattern.inhaleSec + pattern.holdSec - secInCycle).toInt()
            Quadruple("轻柔屏息", left, 0.84f, DreamTrailColors.LanternAmber)
        }
        secInCycle < pattern.inhaleSec + pattern.holdSec + pattern.exhaleSec -> {
            val pos = secInCycle - pattern.inhaleSec - pattern.holdSec
            val left = ceil(pattern.exhaleSec - pos).toInt()
            val frac = (pos / pattern.exhaleSec).coerceIn(0f, 1f)
            Quadruple("缓慢吐气", left, 0.84f - 0.48f * frac, DreamTrailColors.StarlightBlue)
        }
        else -> {
            val pos = secInCycle - pattern.inhaleSec - pattern.holdSec - pattern.exhaleSec
            val left = ceil(pattern.pauseSec - pos).toInt()
            Quadruple("静谧留白", left, 0.36f, DreamTrailColors.MutedInk)
        }
    }
    val radiusState = animateFloatAsState(
        targetValue = if (reduceMotion) 0.62f else radiusFraction,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 200, easing = LinearEasing),
        label = "breathingRadius"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DreamTrailColors.CardSurface,
            border = BorderStroke(1.dp, DreamTrailColors.CardOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "呼吸引导灯",
                    style = MaterialTheme.typography.titleMedium,
                    color = DreamTrailColors.ParchmentText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BreathingPattern.entries.forEach { p ->
                        val selected = pattern == p
                        OutlinedButton(
                            onClick = { pattern = p },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) DreamTrailColors.LanternAmber else DreamTrailColors.CardElevated,
                                contentColor = if (selected) DreamTrailColors.LanternInk else DreamTrailColors.ParchmentText
                            )
                        ) {
                            Text(p.title, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Text(
                    text = pattern.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.MutedInk
                )

                Box(
                    modifier = Modifier.size(164.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val r = (size.minDimension / 2f) * radiusState.value
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(phaseColor.copy(alpha = 0.45f), Color.Transparent)
                            ),
                            radius = size.minDimension / 2f
                        )
                        drawCircle(color = phaseColor.copy(alpha = 0.85f), radius = r)
                    }
                    Column(
                        modifier = Modifier
                            .background(DreamTrailColors.CardSurface.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = phaseName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DreamTrailColors.ParchmentText
                        )
                        Text(
                            text = "$remainingInt 秒",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = DreamTrailColors.ParchmentText
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("结束调息")
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
