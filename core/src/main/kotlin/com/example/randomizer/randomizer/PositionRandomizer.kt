package com.example.randomizer.randomizer

import com.example.randomizer.data.Position
import com.example.randomizer.data.UnitBase
import kotlin.random.Random

class PositionRandomizer(private val config: RandomizerConfig) {

    private val rng = Random(config.seed)

    fun randomize(players: List<UnitBase>): List<UnitBase> {
        if (config.positionMode == PositionMode.NOT_CHANGED) return players
        return players.map { p ->
            when (config.positionMode) {
                PositionMode.NOT_CHANGED    -> p
                PositionMode.REVERSE        -> p.copy(position = reversePosition(p.position))
                PositionMode.RANDOM_TOTALLY -> p.copy(position = randomPosition(p.position))
            }
        }
    }

    // Swap opposing role pairs; sub-index is preserved.
    // GK (0x20) ↔ FW (0x80) — extremes of the ordered list GK < DF < MF < FW
    // DF (0x40) ↔ MF (0x60) — middle pair
    // NPC / non-playable positions: unchanged.
    private fun reversePosition(position: Int): Int {
        val newRole = when (roleOf(position)) {
            0x20 -> 0x80   // GK → FW
            0x80 -> 0x20   // FW → GK
            0x40 -> 0x60   // DF → MF
            0x60 -> 0x40   // MF → DF
            else -> return position
        }
        return newRole or subOf(position)
    }

    // Pick a random role from {GK, DF, MF, FW}; preserve sub-index.
    // Sub-index semantics are unverified (§14.8), so we leave it unchanged
    // to avoid unintended side-effects.
    private fun randomPosition(position: Int): Int {
        if (!isPlayable(position)) return position
        return Position.roles[rng.nextInt(Position.roles.size)] or subOf(position)
    }

    private fun isPlayable(position: Int): Boolean = Position.fromByte(position) != null
    private fun roleOf(position: Int): Int = position and 0xF0
    private fun subOf(position: Int): Int  = position and 0x0F
}
