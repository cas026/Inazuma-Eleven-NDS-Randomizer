package com.example.randomizer

import com.example.randomizer.data.Position
import com.example.randomizer.data.UnitBase
import com.example.randomizer.randomizer.PositionMode
import com.example.randomizer.randomizer.PositionRandomizer
import com.example.randomizer.randomizer.RandomizerConfig
import kotlin.test.*

class PositionRandomizerTest {

    private val gk  = UnitBase("A", "A", 1, 1, 0, position = 0x22) // GK sub-2
    private val df  = UnitBase("B", "B", 1, 1, 0, position = 0x41) // DF sub-1
    private val mf  = UnitBase("C", "C", 1, 1, 0, position = 0x63) // MF sub-3
    private val fw  = UnitBase("D", "D", 1, 1, 0, position = 0x80) // FW sub-0
    private val npc = UnitBase("",  "",  0, 0, 0, position = 0x00) // NPC

    private val players = listOf(gk, df, mf, fw, npc)

    private fun config(mode: PositionMode, seed: Long = 42L) =
        RandomizerConfig(seed = seed, positionMode = mode)

    // ── NOT_CHANGED ───────────────────────────────────────────────────────────

    @Test fun `NOT_CHANGED returns input unchanged`() {
        assertEquals(players, PositionRandomizer(config(PositionMode.NOT_CHANGED)).randomize(players))
    }

    // ── REVERSE ───────────────────────────────────────────────────────────────

    @Test fun `REVERSE swaps GK with FW and DF with MF`() {
        val result = PositionRandomizer(config(PositionMode.REVERSE)).randomize(players)
        assertEquals(0x80 or 0x02, result[0].position) // GK sub-2 → FW sub-2
        assertEquals(0x60 or 0x01, result[1].position) // DF sub-1 → MF sub-1
        assertEquals(0x40 or 0x03, result[2].position) // MF sub-3 → DF sub-3
        assertEquals(0x20 or 0x00, result[3].position) // FW sub-0 → GK sub-0
        assertEquals(0x00,         result[4].position) // NPC unchanged
    }

    @Test fun `REVERSE is idempotent`() {
        val cfg   = config(PositionMode.REVERSE)
        val once  = PositionRandomizer(cfg).randomize(players)
        val twice = PositionRandomizer(cfg).randomize(once)
        assertEquals(players, twice)
    }

    @Test fun `REVERSE preserves sub-indices`() {
        val result = PositionRandomizer(config(PositionMode.REVERSE)).randomize(players)
        players.zip(result).forEach { (orig, rev) ->
            assertEquals(orig.position and 0x0F, rev.position and 0x0F,
                "sub-index changed for position 0x${orig.position.toString(16)}")
        }
    }

    @Test fun `REVERSE never changes NPC records`() {
        val result = PositionRandomizer(config(PositionMode.REVERSE)).randomize(players)
        assertEquals(npc.position, result[4].position)
    }

    // ── RANDOM_TOTALLY ────────────────────────────────────────────────────────

    @Test fun `RANDOM produces a valid position for every playable player`() {
        val result = PositionRandomizer(config(PositionMode.RANDOM_TOTALLY)).randomize(List(200) { gk })
        result.forEach { p ->
            assertNotNull(Position.fromByte(p.position),
                "invalid position 0x${p.position.toString(16)}")
        }
    }

    @Test fun `RANDOM produces all four roles across many players`() {
        val result = PositionRandomizer(config(PositionMode.RANDOM_TOTALLY)).randomize(List(200) { gk })
        val roles  = result.map { it.position and 0xF0 }.toSet()
        assertTrue(Position.roles.all { it in roles }, "not all roles present: $roles")
    }

    @Test fun `RANDOM preserves sub-indices`() {
        val fixed  = List(100) { gk.copy(position = 0x25) } // sub-index = 5
        val result = PositionRandomizer(config(PositionMode.RANDOM_TOTALLY)).randomize(fixed)
        result.forEach { p ->
            assertEquals(5, p.position and 0x0F,
                "sub-index changed: position = 0x${p.position.toString(16)}")
        }
    }

    @Test fun `RANDOM never changes NPC records`() {
        val result = PositionRandomizer(config(PositionMode.RANDOM_TOTALLY)).randomize(players)
        assertEquals(0x00, result[4].position)
    }

    @Test fun `RANDOM same seed produces identical output`() {
        val cfg = config(PositionMode.RANDOM_TOTALLY)
        assertEquals(
            PositionRandomizer(cfg).randomize(players),
            PositionRandomizer(cfg).randomize(players)
        )
    }

    @Test fun `RANDOM different seeds produce different output`() {
        val many = List(50) { gk }
        assertNotEquals(
            PositionRandomizer(config(PositionMode.RANDOM_TOTALLY, seed = 1L)).randomize(many),
            PositionRandomizer(config(PositionMode.RANDOM_TOTALLY, seed = 2L)).randomize(many)
        )
    }
}
