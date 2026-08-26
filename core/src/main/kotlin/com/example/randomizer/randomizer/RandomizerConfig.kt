package com.example.randomizer.randomizer

data class RandomizerConfig(
    val seed: Long,
    val variance: Double = 0.35,
    val storyVariance: Double = 0.15
)
