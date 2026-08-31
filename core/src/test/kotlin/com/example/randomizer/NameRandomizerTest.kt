package com.example.randomizer

import com.example.randomizer.data.UnitBase
import com.example.randomizer.randomizer.NameMode
import com.example.randomizer.randomizer.NameRandomizer
import com.example.randomizer.randomizer.RandomizerConfig
import kotlin.test.*

class NameRandomizerTest {

    private fun player(full: String, nick: String, gender: Int = 1) =
        UnitBase(full, nick, gender, 1, 0, position = 0x40)

    private val alice = player("Alice", "Ali")
    private val bob   = player("Bob",   "Bob")
    private val carol = player("Carol", "Car")
    private val npc1  = player("",      "",   gender = 0)   // blank name
    private val npc2  = UnitBase("NPC",  "N",  0, 0, 0, position = 0x00) // genderId = 0

    private val allPlayers = listOf(alice, bob, carol, npc1, npc2)

    private fun config(mode: NameMode, seed: Long = 42L) =
        RandomizerConfig(seed = seed, nameMode = mode)

    @Test fun `NOT_CHANGED returns input unchanged`() {
        assertEquals(allPlayers, NameRandomizer(config(NameMode.NOT_CHANGED)).randomize(allPlayers))
    }

    @Test fun `RANDOM_TOTALLY shuffles names among playable players`() {
        val result = NameRandomizer(config(NameMode.RANDOM_TOTALLY)).randomize(allPlayers)
        val origNames   = listOf(alice, bob, carol).map { it.fullName to it.nickname }.toSet()
        val resultNames = result.take(3).map { it.fullName to it.nickname }.toSet()
        assertEquals(origNames, resultNames, "multiset of playable names must be preserved")
    }

    @Test fun `RANDOM_TOTALLY does not touch NPC records`() {
        val result = NameRandomizer(config(NameMode.RANDOM_TOTALLY)).randomize(allPlayers)
        assertEquals(npc1, result[3], "NPC with blank name must be unchanged")
        assertEquals(npc2, result[4], "NPC with genderId=0 must be unchanged")
    }

    @Test fun `same seed produces identical output`() {
        val cfg = config(NameMode.RANDOM_TOTALLY)
        assertEquals(
            NameRandomizer(cfg).randomize(allPlayers),
            NameRandomizer(cfg).randomize(allPlayers)
        )
    }

    @Test fun `different seeds produce different output`() {
        val many = List(20) { player("P$it", "p$it") }
        assertNotEquals(
            NameRandomizer(config(NameMode.RANDOM_TOTALLY, seed = 1L)).randomize(many),
            NameRandomizer(config(NameMode.RANDOM_TOTALLY, seed = 2L)).randomize(many)
        )
    }

    @Test fun `non-name fields are preserved after shuffle`() {
        val result = NameRandomizer(config(NameMode.RANDOM_TOTALLY)).randomize(allPlayers)
        result.take(3).forEachIndexed { i, r ->
            assertEquals(allPlayers[i].elementId,  r.elementId)
            assertEquals(allPlayers[i].genderId,   r.genderId)
            assertEquals(allPlayers[i].position,   r.position)
        }
    }
}
