package com.dreamtrail.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dreamtrail.app.data.local.AppSettingsEntity
import com.dreamtrail.app.data.repository.AppUiDataState
import com.dreamtrail.app.ui.BackupAction
import com.dreamtrail.app.ui.components.JournalCard
import com.dreamtrail.app.ui.theme.DreamTrailColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsAndBackupSheet(
    dataState: AppUiDataState,
    backupAction: BackupAction?,
    statusNotice: String?,
    errorMessage: String?,
    onUpdateSettings: ((AppSettingsEntity) -> AppSettingsEntity) -> Unit,
    onCopyBackup: ((String) -> Unit) -> Unit,
    onExportToFile: () -> Unit,
    onRequestImportFile: () -> Unit,
    onImportRawText: (String, (Boolean) -> Unit) -> Unit,
    onRestoreSnapshot: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val settings = dataState.settings
    val context = LocalContext.current
    // Save files can be large; keep pasted JSON out of the Activity saved-state Bundle.
    var importText by remember { mutableStateOf("") }
    var showImportBox by rememberSaveable { mutableStateOf(false) }
    val backupBusy = backupAction != null
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(20.dp),
            color = DreamTrailColors.NightBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "设置与本地存档",
                        style = MaterialTheme.typography.titleLarge,
                        color = DreamTrailColors.ParchmentText
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("关闭")
                    }
                }

                val feedback = backupAction?.label ?: errorMessage ?: statusNotice
                if (feedback != null) {
                    Text(
                        feedback,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (errorMessage != null) DreamTrailColors.CoralWarn else DreamTrailColors.SageMist,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        JournalCard {
                            Text("显示与体验设置", style = MaterialTheme.typography.titleSmall, color = DreamTrailColors.LanternAmber)
                            Text("字体大小", style = MaterialTheme.typography.bodyMedium, color = DreamTrailColors.ParchmentText)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0 to "标准", 1 to "大", 2 to "特大").forEach { (lvl, label) ->
                                    val selected = settings.fontScaleLevel == lvl
                                    OutlinedButton(
                                        onClick = { onUpdateSettings { it.copy(fontScaleLevel = lvl) } },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (selected) DreamTrailColors.LanternAmber else DreamTrailColors.CardElevated,
                                            contentColor = if (selected) DreamTrailColors.LanternInk else DreamTrailColors.ParchmentText
                                        )
                                    ) {
                                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("默认静音", style = MaterialTheme.typography.bodyMedium, color = DreamTrailColors.ParchmentText)
                                    Text("保持安静无打扰的夜间体验", style = MaterialTheme.typography.bodySmall, color = DreamTrailColors.MutedInk)
                                }
                                Switch(
                                    checked = settings.muted,
                                    onCheckedChange = { checked -> onUpdateSettings { it.copy(muted = checked) } }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("减少动画", style = MaterialTheme.typography.bodyMedium, color = DreamTrailColors.ParchmentText)
                                    Text("停止篝火与旅人微幅摆动，直接显示最终状态", style = MaterialTheme.typography.bodySmall, color = DreamTrailColors.MutedInk)
                                }
                                Switch(
                                    checked = settings.reduceMotion,
                                    onCheckedChange = { checked -> onUpdateSettings { it.copy(reduceMotion = checked) } }
                                )
                            }
                        }
                    }

                    item {
                        JournalCard {
                            Text("存档导入与导出", style = MaterialTheme.typography.titleSmall, color = DreamTrailColors.LanternAmber)
                            Text(
                                text = "保存文件可用于换机或恢复进度。恢复前会自动备份当前进度。",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                            Button(
                                onClick = onExportToFile,
                                enabled = !backupBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Text(if (backupAction == BackupAction.SAVE_FILE) "正在保存…" else "保存存档文件")
                            }

                            OutlinedButton(
                                onClick = {
                                    onCopyBackup { text ->
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("dreamtrail-save", text))
                                    }
                                },
                                enabled = !backupBusy,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) {
                                Text(if (backupAction == BackupAction.COPY_TEXT) "正在复制…" else "复制存档文本")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onRequestImportFile,
                                    enabled = !backupBusy,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("文件导入", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { showImportBox = !showImportBox },
                                    enabled = !backupBusy,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(if (showImportBox) "收起粘贴" else "粘贴导入", maxLines = 1)
                                }
                            }

                            if (showImportBox) {
                                OutlinedTextField(
                                    value = importText,
                                    onValueChange = { importText = it },
                                    enabled = !backupBusy,
                                    label = { Text("粘贴存档 JSON 文本") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 4
                                )
                                Button(
                                    onClick = {
                                        onImportRawText(importText) { ok ->
                                            if (ok) {
                                                importText = ""
                                                showImportBox = false
                                            }
                                        }
                                    },
                                    enabled = !backupBusy && importText.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("校验并恢复存档")
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "本地自动快照（最近 3 份）",
                            style = MaterialTheme.typography.titleSmall,
                            color = DreamTrailColors.ParchmentText
                        )
                    }

                    if (dataState.snapshots.isEmpty()) {
                        item {
                            Text(
                                text = "完成第一次旅程结算后会自动留存本地安全快照。",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                        }
                    } else {
                        items(dataState.snapshots, key = { it.snapshotId }) { snap ->
                            JournalCard {
                                Text(
                                    text = snap.reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DreamTrailColors.ParchmentText
                                )
                                Text(
                                    text = dateFmt.format(Date(snap.createdAtUtcMs)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DreamTrailColors.MutedInk
                                )
                                OutlinedButton(
                                    onClick = { onRestoreSnapshot(snap.snapshotId) },
                                    enabled = !backupBusy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("恢复此快照")
                                }
                            }
                        }
                    }

                    item {
                        JournalCard {
                            Text("关于「梦途」", style = MaterialTheme.typography.titleSmall, color = DreamTrailColors.LanternAmber)
                            Text(
                                text = "首版为纯本地离线单机版本，不收集个人睡眠或设备识别信息，不需要任何健康权限、定位权限或后台常驻服务。如后续版本扩展手环睡眠数据读取，仅作为可选加分项并通过独立提供者契约接入。",
                                style = MaterialTheme.typography.bodySmall,
                                color = DreamTrailColors.MutedInk
                            )
                        }
                    }
                }
            }
        }
    }
}
