package com.example.randomizer.randomizer

import com.example.randomizer.data.GameVersion
import com.example.randomizer.data.StatGrowth
import com.example.randomizer.data.UnitStat
import kotlin.math.roundToInt
import kotlin.random.Random

class StatRandomizer(
    private val config: RandomizerConfig,
    private val version: GameVersion
) {
    private val rng = Random(config.seed)
    private val storyIndices = StoryPlayers.byVersion[version] ?: emptySet()

    fun randomize(stats: List<UnitStat>): List<UnitStat> =
        stats.mapIndexed { index, stat ->
            if (stat.maxTotal == 0) stat
            else randomizeStat(stat, if (index in storyIndices) config.storyVariance else config.variance)
        }

    private fun randomizeStat(stat: UnitStat, variance: Double): UnitStat {
        val fp      = scaledGrowth(stat.fp,      isUint8 = false, variance)
        val tp      = scaledGrowth(stat.tp,      isUint8 = false, variance)
        val kick    = scaledGrowth(stat.kick,    isUint8 = true,  variance)
        val body    = scaledGrowth(stat.body,    isUint8 = true,  variance)
        val guard   = scaledGrowth(stat.guard,   isUint8 = true,  variance)
        val control = scaledGrowth(stat.control, isUint8 = true,  variance)
        val speed   = scaledGrowth(stat.speed,   isUint8 = true,  variance)
        val guts    = scaledGrowth(stat.guts,    isUint8 = true,  variance)
        val stamina = scaledGrowth(stat.stamina, isUint8 = true,  variance)

        val combatMinSum = kick.min + body.min + guard.min + control.min +
                           speed.min + guts.min + stamina.min
        val maxTotal = scaleValue(stat.maxTotal, variance, combatMinSum, Short.MAX_VALUE.toInt())

        return UnitStat(fp, tp, kick, body, guard, control, speed, guts, stamina, stat.moves, maxTotal)
    }

    // Scale min and max independently, then sort — preserves original magnitude while
    // guaranteeing min ≤ max without biasing one value toward the other.
    private fun scaledGrowth(g: StatGrowth, isUint8: Boolean, variance: Double): StatGrowth {
        val upper = if (isUint8) 255 else 9999
        val rawA  = scaleValue(g.min,        variance, 1, upper)
        val rawB  = scaleValue(g.max,        variance, 1, upper)
        val rate  = scaleValue(g.growthRate, variance, 1, Short.MAX_VALUE.toInt())
        return StatGrowth(minOf(rawA, rawB), maxOf(rawA, rawB), rate)
    }

    private fun scaleValue(value: Int, variance: Double, min: Int, max: Int): Int {
        val factor = 1.0 + rng.nextDouble() * 2.0 * variance - variance
        return (value * factor).roundToInt().coerceIn(min, max)
    }
}
