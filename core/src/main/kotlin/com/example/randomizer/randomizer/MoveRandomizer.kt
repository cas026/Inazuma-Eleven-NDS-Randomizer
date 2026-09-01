package com.example.randomizer.randomizer

import com.example.randomizer.data.Game
import com.example.randomizer.data.GameVersion
import com.example.randomizer.data.IE1_EUR_MOVE_NAMES
import com.example.randomizer.data.IE1_JAP_MOVE_IDS
import com.example.randomizer.data.MoveData
import com.example.randomizer.data.MoveSlot
import com.example.randomizer.data.Position
import com.example.randomizer.data.UnitBase
import com.example.randomizer.data.UnitStat
import kotlin.random.Random

class MoveRandomizer(
    private val config: RandomizerConfig,
    private val version: GameVersion,
    // Exposed for testing: inject a known set instead of the global IE1_JAP_MOVE_IDS.
    internal val ie1JapMoveIds: Set<Int> = IE1_JAP_MOVE_IDS
) {

    private val rng = Random(config.seed)

    fun randomize(
        stats: List<UnitStat>,
        players: List<UnitBase>,
        movePool: List<MoveData>
    ): List<UnitStat> {
        if (config.moveMode == MoveMode.NOT_CHANGED) return stats
        if (version.game == Game.IE3) return stats

        val storyIndices = StoryPlayers.byVersion[version] ?: emptySet()

        // IE1/IE2 move distinction is only meaningful for JAP IE2 ROMs:
        //   IE1 JAP has 115 hissatsu; IE2 JAP added more — the set is hardcoded in IeMoveIds.kt.
        // For EUR ROMs, IE2 EUR is a strict subset of IE1 EUR (same pool, 2 IDs fewer),
        // so the filter is a no-op there — emptySet() disables it safely.
        val ie1MoveIds: Set<Int> = when {
            version.game == Game.IE2 && version.gameId.endsWith("J") -> ie1JapMoveIds
            version.game == Game.IE2 && IE1_EUR_MOVE_NAMES.isNotEmpty() ->
                movePool.filter { it.name in IE1_EUR_MOVE_NAMES }.map { it.id }.toSet()
            else -> emptySet()
        }

        val filteredPool = applyFilters(movePool, ie1MoveIds)
        val gkPool    = filteredPool.filter { it.isKeeperMove }.ifEmpty { filteredPool }
        val nonGkPool = filteredPool.filter { !it.isKeeperMove }.ifEmpty { filteredPool }
        if (filteredPool.isEmpty()) return stats

        return stats.mapIndexed { idx, stat ->
            if (config.storyMoveProtection && idx in storyIndices) return@mapIndexed stat
            val player = players.getOrNull(idx)
            val isGk = player?.positionRole == Position.GK
            val pool = if (config.samePositionMove) (if (isGk) gkPool else nonGkPool) else filteredPool

            val newMoves = stat.moves.map { slot ->
                if (slot.isEmpty) slot else assignMove(pool, slot.unlockLevel)
            }
            stat.copy(moves = newMoves)
        }
    }

    private fun assignMove(pool: List<MoveData>, originalLevel: Int): MoveSlot {
        val move = pool[rng.nextInt(pool.size)]
        val level = if (config.randomMoveLevel)
            rng.nextInt(1, config.maxMoveLevel.coerceAtLeast(1) + 1)
        else
            originalLevel
        return MoveSlot(id = move.id, unlockLevel = level)
    }

    private fun applyFilters(pool: List<MoveData>, ie1MoveIds: Set<Int>): List<MoveData> {
        var result = pool
        // ie1MoveIds is only non-empty for JAP IE2 ROMs (filled from IeMoveIds.kt).
        // For IE1 ROMs all moves are IE1 by definition; for EUR the pools are identical,
        // so an empty set disables the filter safely in both cases.
        if (ie1MoveIds.isNotEmpty()) {
            if (config.onlyIe1Moves && !config.onlyIe2Moves)
                result = result.filter { it.id in ie1MoveIds }
            else if (config.onlyIe2Moves && !config.onlyIe1Moves)
                result = result.filter { it.id !in ie1MoveIds }
        }
        if (config.limitOfSkill)
            result = result.filter { it.limit <= config.maxMoveLimit }
        return result.ifEmpty { pool }
    }
}
