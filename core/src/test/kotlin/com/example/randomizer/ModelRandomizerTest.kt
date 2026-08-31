package com.example.randomizer

import com.example.randomizer.data.GameVersion
import com.example.randomizer.data.UnitBase
import com.example.randomizer.randomizer.*
import kotlin.test.*

class ModelRandomizerTest {

    private fun player(
        name: String, idx: Int,
        modelData: Int   = idx * 10 + 1,
        spriteSpecs: Int = idx * 10 + 2,
        rpgHead: Int     = idx * 10 + 3,
        rpgPalette: Int  = idx * 10 + 4,
        modelSpecs: Int  = idx * 10 + 5,
        skinTone: Int    = idx * 10 + 6
    ) = UnitBase(
        fullName = name, nickname = name, elementId = 1, genderId = 1, size = 0,
        position = 0x40,
        modelData = modelData, spriteSpecs = spriteSpecs, rpgHead = rpgHead,
        rpgPalette = rpgPalette, modelSpecs = modelSpecs, skinTone = skinTone
    )

    private val p0  = player("Alpha",   0)
    private val p1  = player("Beta",    1)
    private val p2  = player("Gamma",   2)
    private val npc = UnitBase("", "", 0, 0, 0, position = 0x00)
    private val all = listOf(p0, p1, p2, npc)

    private val version = GameVersion.IE1_EN

    private fun config(
        modelMode: ModelMode = ModelMode.RANDOM_TOTALLY,
        matchModelAndName: Boolean = false,
        onlyIe1: Boolean = false,
        nameMode: NameMode = NameMode.NOT_CHANGED,
        seed: Long = 42L
    ) = RandomizerConfig(
        seed = seed, modelMode = modelMode,
        matchModelAndName = matchModelAndName, onlyIe1Characters = onlyIe1,
        nameMode = nameMode
    )

    // All six model fields move as one block from the same source record.
    private fun UnitBase.modelTuple() = listOf(modelData, spriteSpecs, rpgHead, rpgPalette, modelSpecs, skinTone)

    // ── NOT_CHANGED ───────────────────────────────────────────────────────────

    @Test fun `NOT_CHANGED returns input unchanged`() {
        val cfg = config(modelMode = ModelMode.NOT_CHANGED)
        assertEquals(all, ModelRandomizer(cfg, version).randomize(all))
    }

    // ── RANDOM_TOTALLY ────────────────────────────────────────────────────────

    @Test fun `RANDOM every playable player gets a model from the pool`() {
        val pool = listOf(p0, p1, p2).map { it.modelTuple() }.toSet()
        val result = ModelRandomizer(config(), version).randomize(all)
        result.filter { it.genderId != 0 }.forEach { p ->
            assertTrue(p.modelTuple() in pool, "unexpected model tuple ${p.modelTuple()}")
        }
    }

    @Test fun `RANDOM five model fields always move as one block`() {
        // All source model tuples — each result player must match one complete source tuple,
        // proving fields are never mixed from different source records.
        val many = List(30) { i -> player("P$i", i) }
        val sourceTuples = many.map { it.modelTuple() }.toSet()
        val result = ModelRandomizer(config(), version).randomize(many)
        result.forEach { p -> assertTrue(p.modelTuple() in sourceTuples,
            "mixed model fields: ${p.modelTuple()} is not any single source tuple") }
    }

    @Test fun `RANDOM NPC is never modified`() {
        val result = ModelRandomizer(config(), version).randomize(all)
        assertEquals(npc, result[3])
    }

    @Test fun `RANDOM non-model fields are preserved`() {
        val result = ModelRandomizer(config(), version).randomize(all)
        all.zip(result).forEach { (orig, res) ->
            assertEquals(orig.fullName,  res.fullName)
            assertEquals(orig.genderId,  res.genderId)
            assertEquals(orig.position,  res.position)
            assertEquals(orig.elementId, res.elementId)
        }
    }

    @Test fun `same seed produces identical output`() {
        val cfg = config()
        assertEquals(
            ModelRandomizer(cfg, version).randomize(all),
            ModelRandomizer(cfg, version).randomize(all)
        )
    }

    @Test fun `different seeds produce different output`() {
        val many = List(30) { i -> player("P$i", i) }
        assertNotEquals(
            ModelRandomizer(config(seed = 1L), version).randomize(many),
            ModelRandomizer(config(seed = 2L), version).randomize(many)
        )
    }

    // ── IE3 skip ─────────────────────────────────────────────────────────────

    @Test fun `IE3_JP is returned unchanged`() {
        assertEquals(all, ModelRandomizer(config(), GameVersion.IE3_JP).randomize(all))
    }

    // ── onlyIe1Characters ────────────────────────────────────────────────────

    @Test fun `onlyIe1Characters restricts pool to indices below boundary`() {
        // Put an "IE2" player at index >= boundary; its model must NOT appear in output.
        val ie2Model = 9999
        val ie2Player = player("IE2Guy", 500, modelData = ie2Model)
        // Build a list where ie2Player sits at index >= IE1_RECORD_BOUNDARY.
        val players = List(ModelRandomizer.IE1_RECORD_BOUNDARY) { p0 } + listOf(ie2Player)
        val result = ModelRandomizer(config(onlyIe1 = true), version).randomize(players)
        result.filter { it.genderId != 0 }.forEach { p ->
            assertNotEquals(ie2Model, p.modelData, "IE2 model must not appear with onlyIe1=true")
        }
    }

    // ── matchModelAndName ─────────────────────────────────────────────────────

    @Test fun `matchModelAndName uses same permutation as NameRandomizer`() {
        val cfg = config(matchModelAndName = true, nameMode = NameMode.RANDOM_TOTALLY)
        val afterName  = NameRandomizer(cfg).randomize(all)
        val afterModel = ModelRandomizer(cfg, version).randomize(afterName)

        // After both randomizers: the model at each slot must match the model of the
        // player whose name now lives in that slot (i.e., name and model came from the same source).
        val nameToModel = listOf(p0, p1, p2).associate { it.fullName to it.modelTuple() }
        afterModel.filter { it.genderId != 0 }.forEach { p ->
            val expected = nameToModel[p.fullName]
            if (expected != null) {
                assertEquals(expected, p.modelTuple(),
                    "model must match the source player of name '${p.fullName}'")
            }
        }
    }
}
