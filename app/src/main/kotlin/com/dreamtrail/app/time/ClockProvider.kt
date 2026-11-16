package com.dreamtrail.app.time

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.dreamtrail.core.engine.RewardBudgetCalculator
import com.dreamtrail.core.model.ExpeditionSession

interface ClockProvider {
    fun currentWallTimeUtcMs(): Long
    fun elapsedRealtimeMs(): Long?
    fun currentBootId(): String?
}

class AndroidClockProvider(private val context: Context) : ClockProvider {
    override fun currentWallTimeUtcMs(): Long = System.currentTimeMillis()

    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()

    override fun currentBootId(): String? {
        return try {
            val count = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
            if (count >= 0) "boot_$count" else null
        } catch (_: Exception) {
            null
        }
    }
}

data class ResolvedDuration(
    val resolvedEndAtUtcMs: Long,
    val durationSeconds: Long,
    val anomalyNote: String? = null
)

/**
 * Resolves expedition duration safely across sleep/Doze, device reboots, and clock anomalies.
 * - Same boot: prefers elapsedRealtime delta (which includes deep sleep).
 * - Across reboot or missing bootId: falls back to UTC delta.
 * - Backward wall clock jump: never yields negative duration or deducts rewards.
 * - Huge forward jump: caps at the single-session 12h window and re-anchors cleanly.
 */
object DurationResolver {
    fun resolveForPreview(
        session: ExpeditionSession,
        clock: ClockProvider
    ): ResolvedDuration {
        if (session.endAtUtcMs != null) return resolveForSettlement(session, clock)
        return resolveAt(
            session = session,
            nowWallUtcMs = clock.currentWallTimeUtcMs(),
            nowElapsedRealtimeMs = clock.elapsedRealtimeMs(),
            currentBootId = clock.currentBootId()
        )
    }

    fun resolveForSettlement(
        session: ExpeditionSession,
        clock: ClockProvider
    ): ResolvedDuration {
        val storedEnd = session.endAtUtcMs
        if (storedEnd != null) {
            val sec = ((storedEnd - session.startAtUtcMs).coerceAtLeast(0L)) / 1000L
            return ResolvedDuration(
                resolvedEndAtUtcMs = storedEnd,
                durationSeconds = sec.coerceAtMost(session.sessionMode.maxSingleWindowSeconds),
                anomalyNote = null
            )
        }
        return resolveAt(
            session = session,
            nowWallUtcMs = clock.currentWallTimeUtcMs(),
            nowElapsedRealtimeMs = clock.elapsedRealtimeMs(),
            currentBootId = clock.currentBootId()
        )
    }

    fun resolveAt(
        session: ExpeditionSession,
        nowWallUtcMs: Long,
        nowElapsedRealtimeMs: Long?,
        currentBootId: String?
    ): ResolvedDuration {
        val sameBoot = session.startBootId != null &&
            currentBootId != null &&
            session.startBootId == currentBootId &&
            session.startElapsedRealtimeMs != null &&
            nowElapsedRealtimeMs != null

        val singleWindowCap = session.sessionMode.maxSingleWindowSeconds
            .coerceAtMost(RewardBudgetCalculator.MAX_SINGLE_WINDOW_SECONDS)
        val capHours = singleWindowCap / 3600L

        if (sameBoot) {
            val monoMs = nowElapsedRealtimeMs!! - session.startElapsedRealtimeMs!!
            if (monoMs >= 0L) {
                val rawSec = monoMs / 1000L
                val cappedSec = rawSec.coerceAtMost(singleWindowCap)
                val note = when {
                    nowWallUtcMs < session.startAtUtcMs ->
                        "检测到系统时间曾向前回拨，已依据设备运行时长稳妥结算。"
                    rawSec > singleWindowCap ->
                        "本次${session.sessionMode.displayName}已达单次 ${capHours} 小时记录上限，超出时段伙伴已在营地附近休整。"
                    else -> null
                }
                return ResolvedDuration(
                    resolvedEndAtUtcMs = session.startAtUtcMs + cappedSec * 1000L,
                    durationSeconds = cappedSec,
                    anomalyNote = note
                )
            }
        }

        val wallMs = nowWallUtcMs - session.startAtUtcMs
        if (wallMs < 0L) {
            return ResolvedDuration(
                resolvedEndAtUtcMs = session.startAtUtcMs,
                durationSeconds = 0L,
                anomalyNote = "检测到系统时间向前回拨，已保护既有进度并将在下次出发时重建时间锚点。"
            )
        }

        val rawSec = wallMs / 1000L
        val cappedSec = rawSec.coerceAtMost(singleWindowCap)
        val note = if (rawSec > singleWindowCap) {
            "本次离开时间较长，已按单次最长 ${capHours} 小时窗口与 24 小时滚动额度上限结算。"
        } else {
            null
        }
        return ResolvedDuration(
            resolvedEndAtUtcMs = session.startAtUtcMs + cappedSec * 1000L,
            durationSeconds = cappedSec,
            anomalyNote = note
        )
    }
}
