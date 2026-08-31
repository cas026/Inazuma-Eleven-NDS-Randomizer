package com.example.randomizer.randomizer

data class RandomizerConfig(
    val seed: Long,
    val statMode: StatMode = StatMode.NOT_CHANGED,
    val variance: Double = 0.35,
    val storyVariance: Double = 0.15,
    val elementMode: FieldMode = FieldMode.NOT_CHANGED,
    val genderMode: FieldMode = FieldMode.NOT_CHANGED,
    val positionMode: PositionMode = PositionMode.NOT_CHANGED,
    val nameMode: NameMode = NameMode.NOT_CHANGED,
    val modelMode: ModelMode = ModelMode.NOT_CHANGED,
    val matchModelAndName: Boolean = false,
    val onlyIe1Characters: Boolean = false
)
