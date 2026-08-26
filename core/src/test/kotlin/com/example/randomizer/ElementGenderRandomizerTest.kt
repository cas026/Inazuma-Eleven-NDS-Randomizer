package com.example.randomizer

import com.example.randomizer.data.UnitBase
import com.example.randomizer.randomizer.ElementGenderRandomizer
import com.example.randomizer.randomizer.FieldMode
import com.example.randomizer.randomizer.RandomizerConfig
import kotlin.test.*

class ElementGenderRandomizerTest {

    // Representative players covering all element and gender values
    private val airMale   = UnitBase("A", "A", elementId = 1, genderId = 1, size = 0, position = 0x60)
    private val woodFem   = UnitBase("B", "B", elementId = 2, genderId = 2, size = 0, position = 0x60)
    private val fireMale  = UnitBase("C", "C", elementId = 3, genderId = 1, size = 0, position = 0x60)
    private val earthFem  = UnitBase("D", "D", elementId = 4, genderId = 2, size = 0, position = 0x60)
    private val npc       = UnitBase("", "",   elementId = 0, genderId = 0, size = 0, position = 0)

    private val allPlayers = listOf(airMale, woodFem, fireMale, earthFem, npc)

    private fun config(
        elementMode: FieldMode = FieldMode.NOT_CHANGED,
        genderMode: FieldMode  = FieldMode.NOT_CHANGED,
        seed: Long = 42L
    ) = RandomizerConfig(seed = seed, elementMode = elementMode, genderMode = genderMode)

    // ── NOT_CHANGED ───────────────────────────────────────────────────────────

    @Test fun `NOT_CHANGED returns input unchanged`() {
        assertEquals(allPlayers, ElementGenderRandomizer(config()).randomize(allPlayers))
    }

    // ── REVERSE element ───────────────────────────────────────────────────────

    @Test fun `REVERSE element swaps Air-Earth and Wood-Fire`() {
        val result = ElementGenderRandomizer(config(elementMode = FieldMode.REVERSE)).randomize(allPlayers)
        assertEquals(4, result[0].elementId) // Air   → Earth
        assertEquals(3, result[1].elementId) // Wood  → Fire
        assertEquals(2, result[2].elementId) // Fire  → Wood
        assertEquals(1, result[3].elementId) // Earth → Air
        assertEquals(0, result[4].elementId) // NPC   → unchanged
    }

    @Test fun `REVERSE element is idempotent`() {
        val cfg   = config(elementMode = FieldMode.REVERSE)
        val once  = ElementGenderRandomizer(cfg).randomize(allPlayers)
        val twice = ElementGenderRandomizer(cfg).randomize(once)
        assertEquals(allPlayers, twice)
    }

    @Test fun `REVERSE element does not touch gender`() {
        val result = ElementGenderRandomizer(config(elementMode = FieldMode.REVERSE)).randomize(allPlayers)
        allPlayers.forEachIndexed { i, p -> assertEquals(p.genderId, result[i].genderId) }
    }

    // ── REVERSE gender ────────────────────────────────────────────────────────

    @Test fun `REVERSE gender swaps Male-Female`() {
        val result = ElementGenderRandomizer(config(genderMode = FieldMode.REVERSE)).randomize(allPlayers)
        assertEquals(2, result[0].genderId) // Male   → Female
        assertEquals(1, result[1].genderId) // Female → Male
        assertEquals(2, result[2].genderId) // Male   → Female
        assertEquals(1, result[3].genderId) // Female → Male
        assertEquals(0, result[4].genderId) // NPC    → unchanged
    }

    @Test fun `REVERSE gender is idempotent`() {
        val cfg   = config(genderMode = FieldMode.REVERSE)
        val once  = ElementGenderRandomizer(cfg).randomize(allPlayers)
        val twice = ElementGenderRandomizer(cfg).randomize(once)
        assertEquals(allPlayers, twice)
    }

    @Test fun `REVERSE gender does not touch element`() {
        val result = ElementGenderRandomizer(config(genderMode = FieldMode.REVERSE)).randomize(allPlayers)
        allPlayers.forEachIndexed { i, p -> assertEquals(p.elementId, result[i].elementId) }
    }

    // ── RANDOM element ────────────────────────────────────────────────────────

    @Test fun `RANDOM element stays in 1-4 for non-NPC players`() {
        val cfg     = config(elementMode = FieldMode.RANDOM_TOTALLY)
        val players = (1..4).flatMap { e -> List(50) { airMale.copy(elementId = e) } }
        val result  = ElementGenderRandomizer(cfg).randomize(players)
        result.forEach { assertTrue(it.elementId in 1..4, "elementId ${it.elementId} out of range") }
    }

    @Test fun `RANDOM element produces all 4 elements across many players`() {
        val cfg     = config(elementMode = FieldMode.RANDOM_TOTALLY)
        val players = List(200) { airMale.copy(elementId = 1) }
        val result  = ElementGenderRandomizer(cfg).randomize(players)
        val elements = result.map { it.elementId }.toSet()
        assertTrue((1..4).all { it in elements }, "expected all 4 elements in output, got $elements")
    }

    @Test fun `RANDOM element never changes NPC records`() {
        val cfg    = config(elementMode = FieldMode.RANDOM_TOTALLY)
        val result = ElementGenderRandomizer(cfg).randomize(listOf(npc))
        assertEquals(0, result[0].elementId)
    }

    // ── RANDOM gender ─────────────────────────────────────────────────────────

    @Test fun `RANDOM gender stays in 1-2 for non-NPC players`() {
        val cfg     = config(genderMode = FieldMode.RANDOM_TOTALLY)
        val players = listOf(1, 2).flatMap { g -> List(50) { airMale.copy(genderId = g) } }
        val result  = ElementGenderRandomizer(cfg).randomize(players)
        result.forEach { assertTrue(it.genderId in 1..2, "genderId ${it.genderId} out of range") }
    }

    @Test fun `RANDOM gender produces both 1 and 2 across many players`() {
        // With only 2 valid genders, randomExcluding would always return the opposite (= REVERSE).
        // True random (nextInt(1,3)) independently assigns each gender, so both values must appear
        // across a large sample — P(all-same after 100 draws) ≈ 2^-99.
        val cfg     = config(genderMode = FieldMode.RANDOM_TOTALLY)
        val players = List(100) { airMale.copy(genderId = 1) }
        val result  = ElementGenderRandomizer(cfg).randomize(players)
        val genders = result.map { it.genderId }.toSet()
        assertTrue(1 in genders && 2 in genders, "expected both genders in output, got $genders")
    }

    @Test fun `RANDOM gender never changes NPC records`() {
        val cfg    = config(genderMode = FieldMode.RANDOM_TOTALLY)
        val result = ElementGenderRandomizer(cfg).randomize(listOf(npc))
        assertEquals(0, result[0].genderId)
    }

    // ── Reproducibility ───────────────────────────────────────────────────────

    @Test fun `same seed produces identical output`() {
        val cfg = config(elementMode = FieldMode.RANDOM_TOTALLY, genderMode = FieldMode.RANDOM_TOTALLY)
        assertEquals(
            ElementGenderRandomizer(cfg).randomize(allPlayers),
            ElementGenderRandomizer(cfg).randomize(allPlayers)
        )
    }

    @Test fun `different seeds produce different output`() {
        val players = List(20) { airMale }
        assertNotEquals(
            ElementGenderRandomizer(config(elementMode = FieldMode.RANDOM_TOTALLY, seed = 1L)).randomize(players),
            ElementGenderRandomizer(config(elementMode = FieldMode.RANDOM_TOTALLY, seed = 2L)).randomize(players)
        )
    }
}
