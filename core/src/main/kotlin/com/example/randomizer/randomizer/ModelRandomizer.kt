package com.example.randomizer.randomizer

import com.example.randomizer.data.GameVersion
import com.example.randomizer.data.UnitBase
import kotlin.random.Random

class ModelRandomizer(private val config: RandomizerConfig, private val version: GameVersion) {

    private val rng = Random(config.seed)

    companion object {
        // IE1 JAP unitbase.dat has 1264 player records (UnitBaseEnd 0x1DA60 / 0x60 record size).
        // In the shared IE1 EUR / IE2 EUR database, records at indices >= this boundary are IE2-exclusive.
        const val IE1_RECORD_BOUNDARY = 1264
    }

    fun randomize(players: List<UnitBase>): List<UnitBase> {
        if (config.modelMode == ModelMode.NOT_CHANGED) return players
        if (version == GameVersion.IE3_JP) return players

        val playableIndices = players.indices.filter { isPlayable(players[it]) }
        if (playableIndices.isEmpty()) return players

        val result = players.toMutableList()

        // Build pools per size category so body meshes stay size-compatible.
        // modelData (0x44) and modelSpecs (0x4C) are the 3D head+body pack — they MUST come
        // from the same source record; picking from matching size ensures compatibility.
        val poolBySize = buildPoolBySize(players, playableIndices)
        val globalFallback by lazy { poolBySize.values.flatten().ifEmpty { playableIndices.map { players[it] } } }

        fun poolFor(size: Int): List<UnitBase> = poolBySize[size]?.takeIf { it.isNotEmpty() } ?: globalFallback

        if (config.matchModelAndName && config.nameMode == NameMode.RANDOM_TOTALLY) {
            // NameRandomizer is skipped in this mode; ModelRandomizer assigns both name and model.
            // IE1 players get a 1-to-1 permutation within their size group.
            // IE2 players (when onlyIe1=true) draw from the IE1 pool so they are also randomized.
            val permSourceIndices = if (config.onlyIe1Characters)
                playableIndices.filter { it < IE1_RECORD_BOUNDARY }
            else
                playableIndices

            // Build permutation: dstIdx → source player, per size group.
            val permSrc = mutableMapOf<Int, UnitBase>()
            permSourceIndices.groupBy { players[it].size }.forEach { (_, indices) ->
                val shuffled = indices.shuffled(Random(config.seed))
                indices.forEachIndexed { i, dstIdx -> permSrc[dstIdx] = players[shuffled[i]] }
            }

            // Apply to ALL playable players; IE2 destinations fall back to IE1 pool.
            playableIndices.forEach { dstIdx ->
                val src = permSrc[dstIdx]
                    ?: poolFor(players[dstIdx].size).let { it[rng.nextInt(it.size)] }
                result[dstIdx] = result[dstIdx].copy(
                    fullName    = src.fullName,
                    nickname    = src.nickname,
                    modelData   = src.modelData,
                    spriteSpecs = src.spriteSpecs,
                    rpgHead     = src.rpgHead,
                    rpgPalette  = src.rpgPalette,
                    modelSpecs  = src.modelSpecs,
                    skinTone    = src.skinTone
                )
            }
        } else {
            playableIndices.forEach { dstIdx ->
                val pool = poolFor(players[dstIdx].size)
                result[dstIdx] = result[dstIdx].copyModel(pool[rng.nextInt(pool.size)])
            }
        }

        return result
    }

    private fun buildPoolBySize(players: List<UnitBase>, playableIndices: List<Int>): Map<Int, List<UnitBase>> {
        var validIndices = playableIndices.filter { isValidModel(players[it]) }.ifEmpty { playableIndices }
        if (config.onlyIe1Characters) {
            val ie1 = validIndices.filter { it < IE1_RECORD_BOUNDARY }
            if (ie1.isNotEmpty()) validIndices = ie1
        }
        return validIndices.groupBy { players[it].size }.mapValues { (_, idxList) -> idxList.map { players[it] } }
    }

    private fun isValidModel(player: UnitBase): Boolean =
        player.spriteSpecs != 0 && player.modelData != 0 && player.modelSpecs != 0

    // modelData (0x44) and modelSpecs (0x4C) form the 3D model pack and must always come from the same source.
    // spriteSpecs (0x46) is the 2D body sprite; safe to copy because the pool is filtered by size,
    // so source and destination always share the same size category.
    private fun UnitBase.copyModel(src: UnitBase) = copy(
        modelData   = src.modelData,
        spriteSpecs = src.spriteSpecs,
        rpgHead     = src.rpgHead,
        rpgPalette  = src.rpgPalette,
        modelSpecs  = src.modelSpecs,
        skinTone    = src.skinTone
    )

    private fun isPlayable(player: UnitBase): Boolean =
        player.genderId != 0 &&
        player.fullName.firstOrNull()?.isLetter() == true &&
        player.positionRole != null
}
