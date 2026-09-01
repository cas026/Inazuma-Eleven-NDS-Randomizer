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
    val onlyIe1Characters: Boolean = false,
    // — Move randomisation —
    val moveMode: MoveMode = MoveMode.NOT_CHANGED,
    val samePositionMove: Boolean = false,
    val storyMoveProtection: Boolean = true,
    val randomMoveLevel: Boolean = false,
    val maxMoveLevel: Int = 99,
    val limitOfSkill: Boolean = false,
    val maxMoveLimit: Int = Int.MAX_VALUE,  // lower = weaker-move pool; verify safe ceiling in emulator
    val onlyIe1Moves: Boolean = false,
    val onlyIe2Moves: Boolean = false
)
