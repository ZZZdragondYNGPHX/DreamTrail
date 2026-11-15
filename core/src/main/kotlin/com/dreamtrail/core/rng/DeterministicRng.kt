package com.dreamtrail.core.rng

/**
 * Pure deterministic SplitMix64 PRNG.
 * Used for session seed generation and per-node simulation so identical inputs always yield identical outputs.
 */
class DeterministicRng(initialSeed: Long) {
    private var state: Long = if (initialSeed == 0L) DEFAULT_SEED else initialSeed

    fun nextLong(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr 30)) * -4658895280553007687L // 0xBF58476D1CE4E5B9
        z = (z xor (z ushr 27)) * -7723592293110705685L // 0x94D049BB133111EB
        return z xor (z ushr 31)
    }

    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive, got $bound" }
        val r = (nextLong() ushr 1) % bound.toLong()
        return r.toInt()
    }

    fun nextDouble(): Double {
        val bits = nextLong() ushr 11
        return bits.toDouble() / (1L shl 53).toDouble()
    }

    fun nextBoolean(probability: Double): Boolean {
        if (probability <= 0.0) return false
        if (probability >= 1.0) return true
        return nextDouble() < probability
    }

    fun <T> pickWeighted(items: List<T>, weightOf: (T) -> Int): T? {
        if (items.isEmpty()) return null
        var total = 0L
        for (item in items) {
            val w = weightOf(item)
            if (w > 0) total += w
        }
        if (total <= 0L) return null
        var roll = (nextLong() ushr 1) % total
        for (item in items) {
            val w = weightOf(item)
            if (w <= 0) continue
            if (roll < w) return item
            roll -= w
        }
        return items.lastOrNull { weightOf(it) > 0 }
    }

    companion object {
        private const val GOLDEN_GAMMA = -7046029254386353131L // 0x9E3779B97F4A7C15
        private const val DEFAULT_SEED = 0x1A2B3C4D5E6F7788L

        fun advanceSessionSeed(currentRngState: Long): Pair<Long, Long> {
            val rng = DeterministicRng(currentRngState)
            val sessionSeed = rng.nextLong()
            val nextState = rng.nextLong()
            return sessionSeed to nextState
        }

        fun forNode(sessionSeed: Long, nodeIndex: Int, streamTag: Int = 0): DeterministicRng {
            var mixed = sessionSeed xor ((nodeIndex.toLong() + 1L) * -7046029254386353131L)
            mixed = mixed xor ((streamTag.toLong() + 17L) * -4658895280553007687L)
            return DeterministicRng(mixed)
        }
    }
}
