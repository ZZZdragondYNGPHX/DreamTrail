package com.dreamtrail.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "player_state")
data class PlayerStateEntity(
    @PrimaryKey val id: Int = 1,
    val version: Int,
    @ColumnInfo(name = "payload_json") val payloadJson: String
)

@Entity(
    tableName = "expedition_sessions",
    indices = [Index(value = ["status"])]
)
data class ExpeditionSessionEntity(
    @PrimaryKey @ColumnInfo(name = "session_id") val sessionId: String,
    val status: String,
    @ColumnInfo(name = "route_id") val routeId: String,
    val strategy: String,
    @ColumnInfo(name = "start_at_utc_ms") val startAtUtcMs: Long,
    @ColumnInfo(name = "end_at_utc_ms") val endAtUtcMs: Long?,
    @ColumnInfo(name = "start_elapsed_realtime_ms") val startElapsedRealtimeMs: Long?,
    @ColumnInfo(name = "start_boot_id") val startBootId: String?,
    @ColumnInfo(name = "session_seed") val sessionSeed: Long,
    @ColumnInfo(name = "content_version") val contentVersion: Int,
    @ColumnInfo(name = "rules_version") val rulesVersion: Int,
    @ColumnInfo(name = "snapshot_json") val snapshotJson: String,
    @ColumnInfo(name = "is_onboarding") val isOnboarding: Boolean,
    @ColumnInfo(name = "night_phenomenon_json") val nightPhenomenonJson: String? = null,
    @ColumnInfo(name = "session_mode", defaultValue = "'SLEEP'") val sessionMode: String = "SLEEP",
    @ColumnInfo(name = "target_duration_sec") val targetDurationSec: Long? = null,
    @ColumnInfo(name = "focus_tag_id") val focusTagId: String? = null,
    @ColumnInfo(name = "linked_task_id") val linkedTaskId: String? = null,
    @ColumnInfo(name = "day_phenomenon_json") val dayPhenomenonJson: String? = null,
    @ColumnInfo(name = "distraction_count", defaultValue = "0") val distractionCount: Int = 0,
    @ColumnInfo(name = "flip_focus_sec", defaultValue = "0") val flipFocusSec: Long = 0L,
    @ColumnInfo(name = "zone_id") val zoneId: String = "UTC"
)

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "route_id") val routeId: String,
    @ColumnInfo(name = "settled_at_utc_ms") val settledAtUtcMs: Long,
    @ColumnInfo(name = "rules_version") val rulesVersion: Int,
    @ColumnInfo(name = "payload_json") val payloadJson: String
)

@Entity(
    tableName = "reward_ledger",
    primaryKeys = ["session_id", "reward_kind"]
)
data class RewardLedgerEntity(
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "reward_kind") val rewardKind: String,
    @ColumnInfo(name = "credited_total") val creditedTotal: Long,
    @ColumnInfo(name = "policy_version") val policyVersion: Int,
    @ColumnInfo(name = "source_record_ids_json") val sourceRecordIdsJson: String
)

@Entity(tableName = "reward_budget")
data class RewardBudgetEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "payload_json") val payloadJson: String
)

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey @ColumnInfo(name = "instance_id") val instanceId: String,
    @ColumnInfo(name = "def_id") val defId: String,
    val tier: Int,
    val level: Int,
    val affix: String,
    val locked: Boolean,
    @ColumnInfo(name = "secondary_affix") val secondaryAffix: String? = null
)

@Entity(tableName = "discovery_state")
data class DiscoveryStateEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "payload_json") val payloadJson: String
)

@Entity(tableName = "save_snapshots")
data class SaveSnapshotEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "snapshot_id") val snapshotId: Long = 0L,
    @ColumnInfo(name = "created_at_utc_ms") val createdAtUtcMs: Long,
    val reason: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "font_scale_level") val fontScaleLevel: Int = 0,
    val muted: Boolean = true,
    @ColumnInfo(name = "reduce_motion") val reduceMotion: Boolean = false,
    @ColumnInfo(name = "theme_mode", defaultValue = "'NIGHT_INK'") val themeMode: String = "NIGHT_INK",
    @ColumnInfo(name = "soundscape_channel", defaultValue = "'NONE'") val soundscapeChannel: String = "NONE",
    @ColumnInfo(name = "soundscape_volume", defaultValue = "60") val soundscapeVolume: Int = 60,
    @ColumnInfo(name = "soundscape_fade_min", defaultValue = "0") val soundscapeFadeMinutes: Int = 0,
    @ColumnInfo(name = "flip_focus_enabled", defaultValue = "0") val flipFocusEnabled: Boolean = false,
    @ColumnInfo(name = "default_pomodoro_min", defaultValue = "25") val defaultPomodoroMinutes: Int = 25,
    @ColumnInfo(name = "default_nap_min", defaultValue = "30") val defaultNapMinutes: Int = 30
)

// Device-local authorization state, deliberately excluded from game backups.
@Entity(tableName = "sleep_consent")
data class SleepConsentEntity(
    @PrimaryKey val providerId: String,
    val epoch: Long,
    val enabled: Boolean
)
