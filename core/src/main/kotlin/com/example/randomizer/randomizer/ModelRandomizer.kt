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
            // Mirror the exact same shuffle that NameRandomizer uses so model and name travel together.
            val shuffled = playableIndices.shuffled(Random(config.seed))
            playableIndices.forEachIndexed { i, dstIdx ->
                val src = players[shuffled[i]]
                val model = if (isValidModel(src)) src else poolFor(players[dstIdx].size)[rng.nextInt(poolFor(players[dstIdx].size).size)]
                result[dstIdx] = result[dstIdx].copyModel(model)
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
