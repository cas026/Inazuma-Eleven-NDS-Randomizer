package com.example.randomizer.data

data class MoveData(
    val id: Int,
    val type: Int,   // from command.dat 0x0; see MoveType constants below
    val power: Int,  // from command.dat 0x6
    val limit: Int,  // from unitcalc.dat 0x0 — max effective power cap in battle
    val name: String = ""
) {
    // GK moves: type 4 (Normal Catch) or type 8 (Keeper hissatsu)
    val isKeeperMove: Boolean get() = type == MoveType.NORMAL_CATCH || type == MoveType.KEEPER
}

object MoveType {
    const val NONE         = 0
    const val NORMAL_DRIB  = 1
    const val NORMAL_BLOCK = 2
    const val NORMAL_SHOT  = 3
    const val NORMAL_CATCH = 4  // GK basic
    const val DRIBBLE      = 5
    const val BLOCK        = 6
    const val SHOOT        = 7
    const val KEEPER       = 8  // GK hissatsu
    const val SKILL        = 9
}