package com.example.randomizer.randomizer

import com.example.randomizer.data.UnitBase
import kotlin.random.Random

// Note: randomizing gender without also changing the model fields (unitbase.dat 0x44–0x4E)
// may cause a visual mismatch between gender and 3D model. Acceptable until model
// randomisation is implemented.
class ElementGenderRandomizer(private val config: RandomizerConfig) {

    // Independent RNG seeded from config.seed — changing statMode does not affect
    // element/gender output and vice versa.
    private val rng = Random(config.seed)

    fun randomize(players: List<UnitBase>): List<UnitBase> = players.map { player ->
        player.copy(
            elementId = randomizeElement(player.elementId),
            genderId  = randomizeGender(player.genderId)
        )
    }

    private fun randomizeElement(id: Int): Int = when (config.elementMode) {
        FieldMode.NOT_CHANGED    -> id
        FieldMode.REVERSE        -> reverseElement(id)
        FieldMode.RANDOM_TOTALLY -> if (id == 0) 0 else rng.nextInt(1, 5)
    }

    private fun randomizeGender(id: Int): Int = when (config.genderMode) {
        FieldMode.NOT_CHANGED    -> id
        FieldMode.REVERSE        -> reverseGender(id)
        // With only 2 valid genders, randomExcluding always returns the opposite — identical
        // to REVERSE. Use true random so the result is genuinely independent of the original.
        FieldMode.RANDOM_TOTALLY -> if (id == 0) 0 else rng.nextInt(1, 3)
    }

    private fun reverseElement(id: Int): Int = when (id) {
        // Reverse pairs based on the game's element dominance cycle
        // (Air > Earth > Fire > Wood > Air):
        // each element swaps with its direct counter, inverting matchup dominance.
        // None (0): no reverse partner — left unchanged.
        1 -> 4  // Air   → Earth
        4 -> 1  // Earth → Air
        2 -> 3  // Wood  → Fire  (Fire dominates Wood)
        3 -> 2  // Fire  → Wood
        else -> id
    }

    private fun reverseGender(id: Int): Int = when (id) {
        // Male ↔ Female swap. NPC (0): no reverse partner — left unchanged.
        1 -> 2  // Male   → Female
        2 -> 1  // Female → Male
        else -> id
    }
}
