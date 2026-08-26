package com.example.randomizer

import com.example.randomizer.data.*
import com.example.randomizer.randomizer.RandomizerConfig
import com.example.randomizer.randomizer.StatRandomizer
import kotlin.test.*

class StatRandomizerTest {

    private val version = GameVersion.IE1_EUR_EN
    private val config  = RandomizerConfig(seed = 42L)

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
            StatRandomizer(RandomizerConfig(seed = 1L), version).randomize(stats),
            StatRandomizer(RandomizerConfig(seed = 2L), version).randomize(stats)
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
}
