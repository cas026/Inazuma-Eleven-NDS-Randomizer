package com.example.randomizer

import com.example.randomizer.data.*
import com.example.randomizer.randomizer.RandomizerConfig
import com.example.randomizer.randomizer.StatMode
import com.example.randomizer.randomizer.StatRandomizer
import kotlin.test.*

class StatRandomizerTest {

    private val version = GameVersion.IE1_EUR_EN
    private val config  = RandomizerConfig(seed = 42L, statMode = StatMode.RANDOM_TOTALLY)

    private val realStat = UnitStat(
        fp      = StatGrowth(100, 300, 50),
        tp      = StatGrowth(50, 200, 30),
        kick    = StatGrowth(50, 99, 100),
        body    = StatGrowth(40, 85, 80),
        guard   = StatGrowth(45, 90, 90),
        control = StatGrowth(55, 95, 110),
        speed   = StatGrowth(60, 100, 120),
        guts    = StatGrowth(35, 80, 70),
        stamina = StatGrowth(50, 90, 100),
        moves   = listOf(MoveSlot(0x100, 1), MoveSlot(0x200, 10), MoveSlot(0, 0), MoveSlot(0, 0)),
        maxTotal = 500
    )

    private val dummyStat = UnitStat(
        fp = StatGrowth(0, 0, 0), tp = StatGrowth(0, 0, 0),
        kick = StatGrowth(0, 0, 0), body = StatGrowth(0, 0, 0),
        guard = StatGrowth(0, 0, 0), control = StatGrowth(0, 0, 0),
        speed = StatGrowth(0, 0, 0), guts = StatGrowth(0, 0, 0),
        stamina = StatGrowth(0, 0, 0),
        moves = List(4) { MoveSlot(0, 0) },
        maxTotal = 0
    )

    // ── RANDOM_TOTALLY ────────────────────────────────────────────────────────

    @Test fun `same seed produces identical output`() {
        val stats = List(20) { realStat }
        assertEquals(
            StatRandomizer(config, version).randomize(stats),
            StatRandomizer(config, version).randomize(stats)
        )
    }

    @Test fun `different seeds produce different output`() {
        val stats = List(20) { realStat }
        assertNotEquals(
            StatRandomizer(RandomizerConfig(seed = 1L, statMode = StatMode.RANDOM_TOTALLY), version).randomize(stats),
            StatRandomizer(RandomizerConfig(seed = 2L, statMode = StatMode.RANDOM_TOTALLY), version).randomize(stats)
        )
    }

    @Test fun `min never exceeds max for any stat`() {
        val stats = List(50) { realStat }
        StatRandomizer(config, version).randomize(stats).forEach { s ->
            listOf(s.fp, s.tp, s.kick, s.body, s.guard, s.control, s.speed, s.guts, s.stamina)
                .forEach { g -> assertTrue(g.min <= g.max, "min ${g.min} > max ${g.max}") }
        }
    }

    @Test fun `combat stats stay within uint8 range`() {
        val stats = List(50) { realStat }
        StatRandomizer(config, version).randomize(stats).forEach { s ->
            listOf(s.kick, s.body, s.guard, s.control, s.speed, s.guts, s.stamina).forEach { g ->
                assertTrue(g.min in 1..255, "combat min ${g.min} out of range")
                assertTrue(g.max in 1..255, "combat max ${g.max} out of range")
            }
        }
    }

    @Test fun `fp and tp stay within display-safe range`() {
        val stats = List(50) { realStat }
        StatRandomizer(config, version).randomize(stats).forEach { s ->
            assertTrue(s.fp.min in 1..9999)
            assertTrue(s.fp.max in 1..9999)
            assertTrue(s.tp.min in 1..9999)
            assertTrue(s.tp.max in 1..9999)
        }
    }

    @Test fun `growth rates are at least 1`() {
        val stats = List(50) { realStat }
        StatRandomizer(config, version).randomize(stats).forEach { s ->
            listOf(s.fp, s.tp, s.kick, s.body, s.guard, s.control, s.speed, s.guts, s.stamina)
                .forEach { g -> assertTrue(g.growthRate >= 1, "growthRate ${g.growthRate} < 1") }
        }
    }

    @Test fun `maxTotal is at least the sum of combat minimums`() {
        val stats = List(50) { realStat }
        StatRandomizer(config, version).randomize(stats).forEach { s ->
            val combatMinSum = s.kick.min + s.body.min + s.guard.min + s.control.min +
                               s.speed.min + s.guts.min + s.stamina.min
            assertTrue(s.maxTotal >= combatMinSum,
                "maxTotal ${s.maxTotal} < combatMinSum $combatMinSum")
        }
    }

    @Test fun `dummy records with maxTotal zero are left unchanged`() {
        val stats  = listOf(realStat, dummyStat, realStat, dummyStat)
        val result = StatRandomizer(config, version).randomize(stats)
        assertEquals(dummyStat, result[1])
        assertEquals(dummyStat, result[3])
    }

    @Test fun `moves are never modified`() {
        val stats  = List(20) { realStat }
        val result = StatRandomizer(config, version).randomize(stats)
        result.forEach { assertEquals(realStat.moves, it.moves) }
    }

    @Test fun `story player variance is capped to user variance, not fixed 15pct`() {
        // variance=0 must mean zero variance for story players too
        val cfg    = RandomizerConfig(seed = 42L, statMode = StatMode.RANDOM_TOTALLY, variance = 0.0, storyVariance = 0.15)
        val stats  = List(10) { realStat }  // indices 0–5 are story players for IE1_EUR_EN
        val result = StatRandomizer(cfg, version).randomize(stats)
        // With variance=0, every scaleValue call returns the original value exactly
        result.forEachIndexed { i, s ->
            assertEquals(realStat.fp.min,  s.fp.min,  "index $i fp.min changed with 0% variance")
            assertEquals(realStat.kick.min, s.kick.min, "index $i kick.min changed with 0% variance")
        }
    }

    // ── NOT_CHANGED ───────────────────────────────────────────────────────────

    @Test fun `NOT_CHANGED returns input list unchanged`() {
        val stats  = List(20) { realStat }
        val cfg    = RandomizerConfig(seed = 42L, statMode = StatMode.NOT_CHANGED)
        val result = StatRandomizer(cfg, version).randomize(stats)
        assertEquals(stats, result)
    }

    @Test fun `NOT_CHANGED leaves dummy records unchanged`() {
        val stats  = listOf(realStat, dummyStat)
        val cfg    = RandomizerConfig(seed = 42L, statMode = StatMode.NOT_CHANGED)
        val result = StatRandomizer(cfg, version).randomize(stats)
        assertEquals(realStat,  result[0])
        assertEquals(dummyStat, result[1])
    }

    // ── SHUFFLE ───────────────────────────────────────────────────────────────

    @Test fun `SHUFFLE same seed produces identical output`() {
        val cfg   = RandomizerConfig(seed = 99L, statMode = StatMode.SHUFFLE)
        val stats = List(20) { realStat }
        assertEquals(
            StatRandomizer(cfg, version).randomize(stats),
            StatRandomizer(cfg, version).randomize(stats)
        )
    }

    @Test fun `SHUFFLE with zero variance preserves exact set of combat triples`() {
        val cfg    = RandomizerConfig(seed = 42L, statMode = StatMode.SHUFFLE, variance = 0.0, storyVariance = 0.0)
        val result = StatRandomizer(cfg, version).randomize(listOf(realStat)).single()
        val order  = compareBy<StatGrowth>({ it.min }, { it.max }, { it.growthRate })
        val inputSet  = listOf(realStat.kick, realStat.body, realStat.guard, realStat.control,
                               realStat.speed, realStat.guts, realStat.stamina).sortedWith(order)
        val outputSet = listOf(result.kick, result.body, result.guard, result.control,
                               result.speed, result.guts, result.stamina).sortedWith(order)
        assertEquals(inputSet, outputSet)
    }

    @Test fun `SHUFFLE leaves FP and TP unchanged`() {
        val cfg    = RandomizerConfig(seed = 42L, statMode = StatMode.SHUFFLE, variance = 0.0, storyVariance = 0.0)
        val result = StatRandomizer(cfg, version).randomize(listOf(realStat)).single()
        assertEquals(realStat.fp, result.fp)
        assertEquals(realStat.tp, result.tp)
    }

    @Test fun `SHUFFLE leaves moves unchanged`() {
        val cfg    = RandomizerConfig(seed = 42L, statMode = StatMode.SHUFFLE)
        val result = StatRandomizer(cfg, version).randomize(List(20) { realStat })
        result.forEach { assertEquals(realStat.moves, it.moves) }
    }

    @Test fun `SHUFFLE leaves dummy records unchanged`() {
        val cfg    = RandomizerConfig(seed = 42L, statMode = StatMode.SHUFFLE)
        val result = StatRandomizer(cfg, version).randomize(listOf(realStat, dummyStat))
        assertEquals(dummyStat, result[1])
    }

    @Test fun `SHUFFLE respects all section-13 invariants`() {
        val cfg   = RandomizerConfig(seed = 42L, statMode = StatMode.SHUFFLE)
        val stats = List(50) { realStat }
        StatRandomizer(cfg, version).randomize(stats).forEach { s ->
            listOf(s.kick, s.body, s.guard, s.control, s.speed, s.guts, s.stamina).forEach { g ->
                assertTrue(g.min in 1..255,  "SHUFFLE combat min ${g.min} out of range")
                assertTrue(g.max in 1..255,  "SHUFFLE combat max ${g.max} out of range")
                assertTrue(g.min <= g.max,   "SHUFFLE min ${g.min} > max ${g.max}")
                assertTrue(g.growthRate >= 1,"SHUFFLE growthRate ${g.growthRate} < 1")
            }
            val combatMinSum = s.kick.min + s.body.min + s.guard.min + s.control.min +
                               s.speed.min + s.guts.min + s.stamina.min
            assertTrue(s.maxTotal >= combatMinSum,
                "SHUFFLE maxTotal ${s.maxTotal} < combatMinSum $combatMinSum")
        }
    }
}
