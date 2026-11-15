package com.dreamtrail.core.sleep

import com.dreamtrail.core.model.ExpeditionSession
import kotlinx.serialization.Serializable
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@Serializable
enum class SleepProviderAvailability {
    NOT_CONFIGURED,
    NEEDS_PERMISSION,
    READY,
    UNAVAILABLE
}

@Serializable
enum class SleepStageType {
    DEEP,
    LIGHT,
    REM,
    AWAKE,
    UNKNOWN
}

@Serializable
enum class RecordingMethod {
    DEVICE,
    MANUAL,
    UNKNOWN
}

@Serializable
enum class QualityFlag {
    STAGE_OVERBOUND,
    MISSING_STAGES,
    SUMMARY_EXCEEDS_WINDOW_NO_STAGES,
    OVERLAPPING_RECORDS,
    PARTIAL_COVERAGE
}

@Serializable
data class SleepStageInterval(
    val startAtUtcMs: Long,
    val endAtUtcMs: Long,
    val stageType: SleepStageType
)

@Serializable
data class SleepRecord(
    val provider: String,
    val dataOrigin: String,
    val sourceRecordId: String,
    val sourceRevision: String,
    val startAtUtcMs: Long,
    val endAtUtcMs: Long,
    val sourceZoneOffsetSeconds: Int = 0,
    val asleepSeconds: Long,
    val deepSeconds: Long? = null,
    val remSeconds: Long? = null,
    val lightSeconds: Long? = null,
    val awakeSeconds: Long? = null,
    val stages: List<SleepStageInterval> = emptyList(),
    val recordingMethod: RecordingMethod = RecordingMethod.UNKNOWN,
    val qualityFlags: Set<QualityFlag> = emptySet(),
    val observedAtUtcMs: Long,
    val consentEpoch: Long
)

@Serializable
data class NormalizedSleepMatch(
    val matchedAsleepSeconds: Long,
    val matchedDeepSeconds: Long?,
    val matchedRemSeconds: Long?,
    val usedRecordIds: List<String>,
    val qualityFlags: Set<QualityFlag>,
    val skippedDueToAmbiguousSummary: Boolean = false
)

/**
 * Stage-2 extension contract for optional wearable/Health Connect sleep integration.
 * Implementations never modify player balances directly; only SettlementService/GameRepository writes ledger entries.
 */
interface SleepDataProvider {
    val providerId: String
    suspend fun checkAvailability(): SleepProviderAvailability
    suspend fun currentConsentEpoch(): Long
    suspend fun readSleepRecords(
        fromUtcMs: Long,
        toUtcMs: Long,
        expectedConsentEpoch: Long
    ): List<SleepRecord>
}

/**
 * Default Stage-1 provider: manual expedition sessions only, no permissions, no fake sleep records.
 */
class ManualOnlySleepDataProvider : SleepDataProvider {
    override val providerId: String = "manual_only"
    override suspend fun checkAvailability(): SleepProviderAvailability = SleepProviderAvailability.NOT_CONFIGURED
    override suspend fun currentConsentEpoch(): Long = 0L
    override suspend fun readSleepRecords(
        fromUtcMs: Long,
        toUtcMs: Long,
        expectedConsentEpoch: Long
    ): List<SleepRecord> = emptyList()
}

data class AllocatedTimeSlice(
    val sourceIdentity: String,
    val startAtUtcMs: Long,
    val endAtUtcMs: Long
)

/**
 * Normalizes multi-source sleep records against an ExpeditionSession window using UTC intersections,
 * deduplication, stage sanity guards, and cross-session slice allocation protection.
 */
object SleepNormalizer {
    private val SLEEP_STAGES = setOf(SleepStageType.DEEP, SleepStageType.LIGHT, SleepStageType.REM)

    fun normalizeForSession(
        sessionStartUtcMs: Long,
        sessionEndUtcMs: Long,
        records: List<SleepRecord>,
        expectedConsentEpoch: Long,
        preferredOriginOrder: List<String> = emptyList(),
        alreadyAllocatedSlices: List<AllocatedTimeSlice> = emptyList()
    ): NormalizedSleepMatch {
        if (sessionEndUtcMs <= sessionStartUtcMs) {
            return NormalizedSleepMatch(0L, null, null, emptyList(), emptySet())
        }

        val validConsent = records.filter {
            it.consentEpoch == expectedConsentEpoch &&
                it.endAtUtcMs > it.startAtUtcMs &&
                it.endAtUtcMs > sessionStartUtcMs &&
                it.startAtUtcMs < sessionEndUtcMs
        }
        if (validConsent.isEmpty()) {
            return NormalizedSleepMatch(0L, null, null, emptyList(), emptySet())
        }

        // Deduplicate revisions within same (provider, dataOrigin, sourceRecordId): pick latest observed / revision
        val latestByRecord = validConsent
            .groupBy { "${it.provider}|${it.dataOrigin}|${it.sourceRecordId}" }
            .values
            .map { group -> group.maxWith(compareBy<SleepRecord> { it.observedAtUtcMs }.thenBy { it.sourceRevision }) }

        // Pick a single preferred origin so two apps writing the same night are never summed
        val chosenOrigin = selectPreferredOrigin(latestByRecord, preferredOriginOrder)
        val chosenProvider = latestByRecord.filter { it.dataOrigin == chosenOrigin }
            .sortedWith(compareByDescending<SleepRecord> { it.stages.isNotEmpty() }
                .thenByDescending { it.observedAtUtcMs }.thenBy { it.provider }).first().provider
        val sourceRecords = latestByRecord.filter { it.dataOrigin == chosenOrigin && it.provider == chosenProvider }
            .sortedWith(compareByDescending<SleepRecord> { it.stages.isNotEmpty() }
                .thenByDescending { it.observedAtUtcMs }.thenBy { it.sourceRecordId })
        val flags = mutableSetOf<QualityFlag>()
        val usedIds = mutableListOf<String>()
        val staged = mutableListOf<SleepStageInterval>()
        val occupied = mutableListOf<Pair<Long, Long>>()
        // Ownership is by time, including slices recorded by another provider.
        val blocked = alreadyAllocatedSlices.map { it.startAtUtcMs to it.endAtUtcMs }
        var summaryAsleep = 0L
        var summaryDeep: Long? = null
        var summaryRem: Long? = null
        var ambiguous = false
        for (rec in sourceRecords) {
            flags += rec.qualityFlags
            if (rec.stages.isNotEmpty()) {
                val start = maxOf(rec.startAtUtcMs, sessionStartUtcMs)
                val end = minOf(rec.endAtUtcMs, sessionEndUtcMs)
                occupied += start to end
                var contributed = false
                for (stage in rec.stages) {
                    val a = maxOf(start, stage.startAtUtcMs)
                    val b = minOf(end, stage.endAtUtcMs)
                    if (a >= b) continue
                    for ((from, to) in subtractBlocked(a, b, blocked)) {
                        staged += SleepStageInterval(from, to, stage.stageType)
                        contributed = true
                    }
                }
                if (contributed) usedIds += rec.sourceRecordId
            } else {
                if (rec.startAtUtcMs < sessionStartUtcMs || rec.endAtUtcMs > sessionEndUtcMs) {
                    flags += QualityFlag.SUMMARY_EXCEEDS_WINDOW_NO_STAGES
                    ambiguous = true
                    continue
                }
                if ((occupied + blocked).any { (a, b) -> a < rec.endAtUtcMs && b > rec.startAtUtcMs }) {
                    flags += QualityFlag.OVERLAPPING_RECORDS
                    continue
                }
                val windowSeconds = (rec.endAtUtcMs - rec.startAtUtcMs) / 1000
                val asleep = rec.asleepSeconds.coerceIn(0L, windowSeconds)
                val deep = rec.deepSeconds?.coerceAtLeast(0L)
                val rem = rec.remSeconds?.coerceAtLeast(0L)
                val light = rec.lightSeconds?.coerceAtLeast(0L)
                val validStages = listOfNotNull(deep, rem, light).all { it <= asleep } &&
                    listOfNotNull(deep, rem, light).sumOf { it.toDouble() } <= asleep.toDouble()
                if (!validStages || asleep != rec.asleepSeconds) flags += QualityFlag.STAGE_OVERBOUND
                if (validStages) {
                    if (deep != null) summaryDeep = (summaryDeep ?: 0) + deep
                    if (rem != null) summaryRem = (summaryRem ?: 0) + rem
                }
                summaryAsleep += asleep
                occupied += rec.startAtUtcMs to rec.endAtUtcMs
                usedIds += rec.sourceRecordId
                flags += QualityFlag.MISSING_STAGES
            }
        }
        // Sweep shared stage boundaries. Conflicting labels contribute no DEEP/REM bonus;
        // awake wins a sleep/awake conflict. Duplicate labels count once.
        val boundaries = staged.flatMap { listOf(it.startAtUtcMs, it.endAtUtcMs) }.distinct().sorted()
        var asleepMs = 0L
        var deepMs = 0L
        var remMs = 0L
        for ((a, b) in boundaries.zipWithNext()) {
            val types = staged.filter { it.startAtUtcMs < b && it.endAtUtcMs > a }.map { it.stageType }.toSet()
            if (types.size > 1) flags += QualityFlag.STAGE_OVERBOUND
            if (SleepStageType.AWAKE !in types && types.any { it in SLEEP_STAGES }) {
                asleepMs += b - a
                if (types == setOf(SleepStageType.DEEP)) deepMs += b - a
                if (types == setOf(SleepStageType.REM)) remMs += b - a
            }
        }
        return NormalizedSleepMatch(
            matchedAsleepSeconds = summaryAsleep + asleepMs / 1000,
            matchedDeepSeconds = if (staged.isEmpty() && summaryDeep == null) null else (summaryDeep ?: 0) + deepMs / 1000,
            matchedRemSeconds = if (staged.isEmpty() && summaryRem == null) null else (summaryRem ?: 0) + remMs / 1000,
            usedRecordIds = usedIds.distinct(), qualityFlags = flags,
            skippedDueToAmbiguousSummary = ambiguous && summaryAsleep + asleepMs == 0L
        )
    }

    private fun selectPreferredOrigin(
        records: List<SleepRecord>,
        preferredOriginOrder: List<String>
    ): String {
        val origins = records.map { it.dataOrigin }.distinct()
        for (pref in preferredOriginOrder) {
            if (pref in origins) return pref
        }
        // Deterministic fallback: prefer records with stages, then latest observedAt
        return records
            .sortedWith(compareByDescending<SleepRecord> { it.stages.isNotEmpty() }.thenByDescending { it.observedAtUtcMs })
            .first()
            .dataOrigin
    }

    private fun subtractBlocked(
        startMs: Long,
        endMs: Long,
        blocked: List<Pair<Long, Long>>
    ): List<Pair<Long, Long>> {
        var pieces = listOf(startMs to endMs)
        for ((bs, be) in blocked) {
            val next = mutableListOf<Pair<Long, Long>>()
            for ((ps, pe) in pieces) {
                if (be <= ps || bs >= pe) {
                    next += ps to pe
                } else {
                    if (bs > ps) next += ps to bs
                    if (be < pe) next += be to pe
                }
            }
            pieces = next
        }
        return pieces
    }

    private fun unionDurationMillis(intervals: List<Pair<Long, Long>>): Long {
        if (intervals.isEmpty()) return 0L
        val sorted = intervals.sortedBy { it.first }
        var total = 0L
        var curStart = sorted[0].first
        var curEnd = sorted[0].second
        for (i in 1 until sorted.size) {
            val (s, e) = sorted[i]
            if (s <= curEnd) {
                curEnd = max(curEnd, e)
            } else {
                total += (curEnd - curStart).coerceAtLeast(0L)
                curStart = s
                curEnd = e
            }
        }
        total += (curEnd - curStart).coerceAtLeast(0L)
        return total
    }
}

data class SleepBuffComputation(
    val totalBasisPoints: Int, // 300 = 3.00%, max 800 = 8.00%
    val targetBlessingGold: Long,
    val targetBlessingXp: Long
)

data class SleepTopUpDelta(
    val deltaGold: Long,
    val deltaXp: Long,
    val targetBlessingGold: Long,
    val targetBlessingXp: Long,
    val totalBasisPoints: Int
)

/**
 * Calculates capped rest blessings:
 * - Everyone gets base +3.00% (300 bps) gold & XP
 * - Measured sleep adds up to +3.00% (at 6h), deep adds up to +1.00% (at 60m), REM adds up to +1.00% (at 60m)
 * - Total capped at +8.00% (800 bps)
 */
object SleepBuffPolicy {
    const val POLICY_VERSION = 1
    const val BASE_BASIS_POINTS = 300
    const val MAX_TOTAL_BASIS_POINTS = 800
    const val SEVEN_DAYS_MS = 7L * 24L * 3600L * 1000L

    fun computeBuff(
        baseGold: Long,
        baseXp: Long,
        sleepMatch: NormalizedSleepMatch? = null,
        goldRemainder: Long = 0L,
        xpRemainder: Long = 0L
    ): SleepBuffComputation {
        val asleepBps = if (sleepMatch != null && sleepMatch.matchedAsleepSeconds > 0L) {
            val ratio = (sleepMatch.matchedAsleepSeconds.toDouble() / 21600.0).coerceIn(0.0, 1.0)
            floor(300.0 * ratio).toInt()
        } else 0

        val deepBps = if (sleepMatch?.matchedDeepSeconds != null && sleepMatch.matchedDeepSeconds > 0L) {
            val ratio = (sleepMatch.matchedDeepSeconds.toDouble() / 3600.0).coerceIn(0.0, 1.0)
            floor(100.0 * ratio).toInt()
        } else 0

        val remBps = if (sleepMatch?.matchedRemSeconds != null && sleepMatch.matchedRemSeconds > 0L) {
            val ratio = (sleepMatch.matchedRemSeconds.toDouble() / 3600.0).coerceIn(0.0, 1.0)
            floor(100.0 * ratio).toInt()
        } else 0

        val totalBps = (BASE_BASIS_POINTS + asleepBps + deepBps + remBps).coerceAtMost(MAX_TOTAL_BASIS_POINTS)
        val goldBlessing = blessing(baseGold, totalBps, goldRemainder)
        val xpBlessing = blessing(baseXp, totalBps, xpRemainder)

        return SleepBuffComputation(
            totalBasisPoints = totalBps,
            targetBlessingGold = goldBlessing,
            targetBlessingXp = xpBlessing
        )
    }

    fun computeTopUpDelta(
        baseGold: Long,
        baseXp: Long,
        alreadyCreditedGold: Long,
        alreadyCreditedXp: Long,
        sleepMatch: NormalizedSleepMatch,
        goldRemainder: Long = 0L,
        xpRemainder: Long = 0L
    ): SleepTopUpDelta {
        val target = computeBuff(baseGold, baseXp, sleepMatch, goldRemainder, xpRemainder)
        val deltaGold = (target.targetBlessingGold - alreadyCreditedGold).coerceAtLeast(0L)
        val deltaXp = (target.targetBlessingXp - alreadyCreditedXp).coerceAtLeast(0L)
        return SleepTopUpDelta(
            deltaGold = deltaGold,
            deltaXp = deltaXp,
            targetBlessingGold = max(alreadyCreditedGold, target.targetBlessingGold),
            targetBlessingXp = max(alreadyCreditedXp, target.targetBlessingXp),
            totalBasisPoints = target.totalBasisPoints
        )
    }

    private fun blessing(amount: Long, bps: Int, remainder: Long): Long {
        val safe = amount.coerceAtLeast(0)
        return safe / 10000 * bps + (safe % 10000 * bps + remainder.coerceIn(0, 9999)) / 10000
    }

    fun isWithinAdjustmentWindow(sessionEndUtcMs: Long, nowUtcMs: Long): Boolean {
        return nowUtcMs >= sessionEndUtcMs && (nowUtcMs - sessionEndUtcMs) <= SEVEN_DAYS_MS
    }
}
