package com.example.randomizer.randomizer

import com.example.randomizer.data.UnitBase
import kotlin.random.Random

class NameRandomizer(private val config: RandomizerConfig) {

    private val rng = Random(config.seed)

    fun randomize(players: List<UnitBase>): List<UnitBase> {
        if (config.nameMode == NameMode.NOT_CHANGED) return players
        // ModelRandomizer handles both name and model together in matchModelAndName mode.
        if (config.matchModelAndName && config.modelMode != ModelMode.NOT_CHANGED) return players

        // Collect (fullName, nickname) pairs from playable players only.
        // NPC records (genderId == 0 or blank name) are excluded from the pool
        // and left in place so their names are never replaced with a player name.
        val playableIndices = players.indices.filter { isPlayable(players[it]) }
        val namePool = playableIndices.map { players[it].fullName to players[it].nickname }
            .shuffled(rng)

        val result = players.toMutableList()
        playableIndices.forEachIndexed { i, idx ->
            val (full, nick) = namePool[i]
            result[idx] = players[idx].copy(fullName = full, nickname = nick)
        }
        return result
    }

    private fun isPlayable(player: UnitBase): Boolean =
        player.genderId != 0 &&
        player.fullName.firstOrNull()?.isLetter() == true &&
        player.positionRole != null
}
