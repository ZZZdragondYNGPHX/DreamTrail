package com.dreamtrail.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dreamtrail.app.audio.SoundscapeChannel
import com.dreamtrail.app.data.local.AppSettingsEntity
import com.dreamtrail.app.ui.components.TagBadge
import com.dreamtrail.app.ui.theme.DreamTrailColors
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoundscapeSheet(
    settings: AppSettingsEntity,
    onUpdateSettings: ((AppSettingsEntity) -> AppSettingsEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val activeChannel = SoundscapeChannel.fromId(settings.soundscapeChannel)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(20.dp),
            color = DreamTrailColors.CardSurface,
            border = BorderStroke(1.dp, DreamTrailColors.CardOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = DreamTrailColors.LanternAmber
                        )
                        Text(
                            text = "自然声景与白噪音台",
                            style = MaterialTheme.typography.titleMedium,
                            color = DreamTrailColors.ParchmentText,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("关闭")
                    }
                }

                Text(
                    text = "全本地实时程序化合成，无网络消耗，随时随地陪伴专研与安眠。",
                    style = MaterialTheme.typography.bodySmall,
                    color = DreamTrailColors.MutedInk
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeOff,
                                contentDescription = null,
                                tint = DreamTrailColors.MutedInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (settings.muted) "静音保护开启（未播放声音）" else "正在输出背景声景",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DreamTrailColors.ParchmentText
                            )
                        }
                        Switch(
                            checked = !settings.muted,
                            onCheckedChange = { play ->
                                onUpdateSettings { cur ->
                                    val nextChan = if (play && SoundscapeChannel.fromId(cur.soundscapeChannel) == SoundscapeChannel.NONE) {
                                        SoundscapeChannel.MIST_RAIN.id
                                    } else cur.soundscapeChannel
                                    cur.copy(muted = !play, soundscapeChannel = nextChan)
                                }
                            }
                        )
                    }

                    Text(
                        text = "声景频道",
                        style = MaterialTheme.typography.labelLarge,
                        color = DreamTrailColors.LanternAmber
                    )
                    for (channel in SoundscapeChannel.entries) {
                        val selected = activeChannel == channel
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) DreamTrailColors.LanternAmber.copy(alpha = 0.16f) else DreamTrailColors.CardElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) DreamTrailColors.LanternAmber else DreamTrailColors.CardOutline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateSettings { cur ->
                                        cur.copy(
                                            soundscapeChannel = channel.id,
                                            muted = if (channel == SoundscapeChannel.NONE) true else false
                                        )
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = channel.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DreamTrailColors.ParchmentText
                                    )
                                    Text(
                                        text = channel.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DreamTrailColors.MutedInk
                                    )
                                }
                                if (selected) {
                                    TagBadge(
                                        if (settings.muted && channel != SoundscapeChannel.NONE) "已选·静音" else "当前",
                                        DreamTrailColors.LanternAmber
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "音量（${settings.soundscapeVolume}%）",
                        style = MaterialTheme.typography.labelLarge,
                        color = DreamTrailColors.ParchmentText
                    )
                    Slider(
                        value = settings.soundscapeVolume.toFloat(),
                        onValueChange = { v ->
                            onUpdateSettings { it.copy(soundscapeVolume = v.roundToInt().coerceIn(0, 100)) }
                        },
                        valueRange = 0f..100f
                    )

                    Text(
                        text = "定时渐隐熄音",
                        style = MaterialTheme.typography.labelLarge,
                        color = DreamTrailColors.ParchmentText
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "不定时", 15 to "15 分钟", 30 to "30 分钟", 45 to "45 分钟").forEach { (min, label) ->
                            val selected = settings.soundscapeFadeMinutes == min
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateSettings { it.copy(soundscapeFadeMinutes = min) } },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }
    }
}
