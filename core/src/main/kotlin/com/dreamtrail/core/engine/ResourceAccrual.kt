package com.dreamtrail.core.engine

/** Fixed-point balances. Gold/XP share their currency carry; each region material has its own carry.
 * Multipliers are applied before accumulation, so switching a route never reprices earlier seconds.
 */
class ResourceAccrual(saved: Map<String, Long>) {
    val remainders = saved.toMutableMap()

    fun accrue(key: String, numerator: Long, denominator: Long = RATE_DENOMINATOR): Long {
        require(numerator >= 0 && denominator > 0)
        val total = Math.addExact(numerator, remainders[key] ?: 0L)
        remainders[key] = total % denominator
        return total / denominator
    }

    companion object {
        const val RATE_DENOMINATOR = 3600L * 100L * 100L
    }
}
