package com.example.randomizer.data

data class StatGrowth(val min: Int, val max: Int, val growthRate: Int)

data class MoveSlot(val id: Int, val unlockLevel: Int) {
    val isEmpty: Boolean get() = id == 0
}

data class UnitStat(
    val fp: StatGrowth,
    val tp: StatGrowth,
    val kick: StatGrowth,
    val body: StatGrowth,
    val guard: StatGrowth,
    val control: StatGrowth,
    val speed: StatGrowth,
    val guts: StatGrowth,
    val stamina: StatGrowth,
    val moves: List<MoveSlot>,
    val maxTotal: Int
)
