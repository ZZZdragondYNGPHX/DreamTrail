package com.dreamtrail.core.engine

import com.dreamtrail.core.model.RewardBudgetState
import com.dreamtrail.core.model.SecondInterval
import com.dreamtrail.core.model.SessionMode
import kotlin.math.max
import kotlin.math.min

data class BudgetAllocation(
    val windowCappedSeconds: Long,
    val creditedSeconds: Long,
    val completedNodes: Int,
    val newRemainderSeconds: Int,
    val updatedIntervals: List<SecondInterval>,
    val budgetNote: String?,
    val newlyCreditedIntervals: List<SecondInterval>
)

/**
 * Enforces independent rolling 24-hour 8-hour credit caps for rest (sleep/nap)
 * and daytime focus (Pomodoro/Flowmodoro) sessions, using compressed UTC second
 * intervals retained for 36 hours.
 */
object RewardBudgetCalculator {
    const val SECONDS_PER_NODE = 300L
    const val MAX_SINGLE_WINDOW_SECONDS = 12L * 3600L
    const val ROLLING_WINDOW_SECONDS = 24L * 3600L
    const val ROLLING_CREDIT_CAP_SECONDS = 8L * 3600L
    const val HISTORY_RETENTION_SECONDS = 36L * 3600L

    fun allocate(
        budgetState: RewardBudgetState,
        sessionStartUtcSec: Long,
        rawDurationSeconds: Long,
        sessionMode: SessionMode = SessionMode.SLEEP
    ): BudgetAllocation {
        val safeRaw = rawDurationSeconds.coerceAtLeast(0L)
        val singleCap = sessionMode.maxSingleWindowSeconds.coerceAtMost(MAX_SINGLE_WINDOW_SECONDS)
        val windowCapped = safeRaw.coerceAtMost(singleCap)
        val sessionEndUtcSec = sessionStartUtcSec + windowCapped

        val sourceIntervals = if (sessionMode.isFocus) budgetState.focusCreditedIntervals else budgetState.creditedIntervals
        val sourceRemainder = if (sessionMode.isFocus) budgetState.focusRemainderSeconds else budgetState.remainderSeconds

        val working = coalesceIntervals(
            sourceIntervals.filter { it.endSec > sessionEndUtcSec - HISTORY_RETENTION_SECONDS }
        ).toMutableList()

        var t = sessionStartUtcSec
        var creditedSeconds = 0L
        val newlyCredited = mutableListOf<SecondInterval>()

        while (t < sessionEndUtcSec) {
            val covering = working.firstOrNull { it.startSec <= t && t < it.endSec }
            if (covering != null) {
                t = min(sessionEndUtcSec, covering.endSec)
                continue
            }

            val nextExistingStart = working.firstOrNull { it.startSec > t }?.startSec
            val segmentEnd = if (nextExistingStart != null) min(sessionEndUtcSec, nextExistingStart) else sessionEndUtcSec

            val lookbackStart = t - ROLLING_WINDOW_SECONDS
            val used = usedInWindow(working, lookbackStart, t)
            val avail = (ROLLING_CREDIT_CAP_SECONDS - used).coerceAtLeast(0L)

            if (avail > 0L) {
                val step = min(segmentEnd - t, avail)
                if (step > 0L) {
                    insertAndCoalesce(working, SecondInterval(t, t + step))
                    newlyCredited += SecondInterval(t, t + step)
                    creditedSeconds += step
                    t += step
                } else {
                    t = segmentEnd
                }
            } else {
                val exiting = working.firstOrNull { it.startSec <= lookbackStart && lookbackStart < it.endSec }
                if (exiting != null) {
                    val step = min(segmentEnd - t, exiting.endSec + ROLLING_WINDOW_SECONDS - t)
                    if (step > 0L) {
                        insertAndCoalesce(working, SecondInterval(t, t + step))
                        newlyCredited += SecondInterval(t, t + step)
                        creditedSeconds += step
                        t += step
                    } else {
                        t = segmentEnd
                    }
                } else {
                    val nextExitStart = working
                        .mapNotNull { iv ->
                            val exitAt = iv.startSec + ROLLING_WINDOW_SECONDS
                            if (exitAt > t) exitAt else null
                        }
                        .minOrNull()
                    t = if (nextExitStart != null && nextExitStart < segmentEnd) nextExitStart else segmentEnd
                }
            }
        }

        val pruned = coalesceIntervals(
            working.filter { it.endSec > sessionEndUtcSec - HISTORY_RETENTION_SECONDS }
        )

        val totalPoolSeconds = sourceRemainder.toLong().coerceIn(0L, SECONDS_PER_NODE - 1) + creditedSeconds
        val completedNodes = (totalPoolSeconds / SECONDS_PER_NODE).toInt()
        val newRemainderSeconds = (totalPoolSeconds % SECONDS_PER_NODE).toInt()

        val budgetNote = when {
            windowCapped > 0L && creditedSeconds == 0L -> if (sessionMode.isFocus) {
                "当前 24 小时专注研习额度已用完，伙伴在书桌旁安静陪伴。"
            } else {
                "当前 24 小时主要探索额度已用完，伙伴在营地附近安全休整。"
            }
            creditedSeconds < windowCapped -> if (sessionMode.isFocus) {
                "今日专注核心收益额度已满，超出时段转为轻松伴读。"
            } else {
                "伙伴今日已完成主要探索，超出额度的时段转为营地休整。"
            }
            else -> null
        }

        return BudgetAllocation(
            windowCappedSeconds = windowCapped,
            creditedSeconds = creditedSeconds,
            completedNodes = completedNodes,
            newRemainderSeconds = newRemainderSeconds,
            updatedIntervals = pruned,
            budgetNote = budgetNote,
            newlyCreditedIntervals = coalesceIntervals(newlyCredited)
        )
    }

    fun applyAllocation(
        budgetState: RewardBudgetState,
        allocation: BudgetAllocation,
        sessionMode: SessionMode
    ): RewardBudgetState = if (sessionMode.isFocus) {
        budgetState.copy(
            focusCreditedIntervals = allocation.updatedIntervals,
            focusRemainderSeconds = allocation.newRemainderSeconds
        )
    } else {
        budgetState.copy(
            creditedIntervals = allocation.updatedIntervals,
            remainderSeconds = allocation.newRemainderSeconds
        )
    }

    fun remainingRollingBudgetSeconds(budgetState: RewardBudgetState, nowUtcSec: Long): Long {
        val used = usedInWindow(budgetState.creditedIntervals, nowUtcSec - ROLLING_WINDOW_SECONDS, nowUtcSec)
        return (ROLLING_CREDIT_CAP_SECONDS - used).coerceAtLeast(0L)
    }

    fun remainingFocusBudgetSeconds(budgetState: RewardBudgetState, nowUtcSec: Long): Long {
        val used = usedInWindow(budgetState.focusCreditedIntervals, nowUtcSec - ROLLING_WINDOW_SECONDS, nowUtcSec)
        return (ROLLING_CREDIT_CAP_SECONDS - used).coerceAtLeast(0L)
    }

    private fun usedInWindow(intervals: List<SecondInterval>, windowStart: Long, windowEnd: Long): Long {
        if (windowEnd <= windowStart) return 0L
        var total = 0L
        for (iv in intervals) {
            val s = max(iv.startSec, windowStart)
            val e = min(iv.endSec, windowEnd)
            if (e > s) total += (e - s)
        }
        return total
    }

    private fun insertAndCoalesce(list: MutableList<SecondInterval>, addition: SecondInterval) {
        list.add(addition)
        val merged = coalesceIntervals(list)
        list.clear()
        list.addAll(merged)
    }

    fun coalesceIntervals(intervals: List<SecondInterval>): List<SecondInterval> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedBy { it.startSec }
        val result = ArrayList<SecondInterval>(sorted.size)
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.startSec <= current.endSec) {
                current = SecondInterval(current.startSec, max(current.endSec, next.endSec))
            } else {
                result.add(current)
                current = next
            }
        }
        result.add(current)
        return result
    }
}
