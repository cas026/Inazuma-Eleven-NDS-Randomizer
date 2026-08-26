package com.example.randomizer.randomizer

data class RandomizerConfig(
    val seed: Long,
    val statMode: StatMode = StatMode.NOT_CHANGED,
    val variance: Double = 0.35,
    val storyVariance: Double = 0.15,
    val elementMode: FieldMode = FieldMode.NOT_CHANGED,
    val genderMode: FieldMode = FieldMode.NOT_CHANGED
)
