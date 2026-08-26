package com.example.randomizer.randomizer

import com.example.randomizer.data.Game
import com.example.randomizer.data.GameVersion

// Player indices (0-based, after the null record) that are mandatory during story mode.
// These players cannot be permanently dismissed and must remain playable.
// IE1: Endou + the 5 core Raimon members you start with.
// IE2/IE3: Full starting 11; mandatory matches require a complete team.
object StoryPlayers {
    val byVersion: Map<GameVersion, Set<Int>> = GameVersion.entries.associateWith { v ->
        when (v.game) {
            Game.IE1 -> (0..5).toSet()
            Game.IE2 -> (0..10).toSet()
            Game.IE3 -> (0..10).toSet()
        }
    }
}
