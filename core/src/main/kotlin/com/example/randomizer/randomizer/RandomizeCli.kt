package com.example.randomizer.randomizer

import com.example.randomizer.data.GameVersion
import com.example.randomizer.data.UnitStatParser
import com.example.randomizer.data.UnitStatSerializer
import com.example.randomizer.rom.NdsRom
import kotlin.io.path.Path
import kotlin.io.path.writeBytes

fun main(args: Array<String>) {
    if (args.size < 2) {
        System.err.println(
            "Usage: ./gradlew :core:randomize --args=\"<input.nds> <output.nds> [seed] [variance]\"\n" +
            "  seed     — integer, omit for a random seed\n" +
            "  variance — decimal 0.0–1.0, default 0.35 (±35%)"
        )
        return
    }

    val inputPath  = Path(args[0])
    val outputPath = Path(args[1])
    val seed       = args.getOrNull(2)?.toLong() ?: System.currentTimeMillis()
    val variance   = args.getOrNull(3)?.toDouble() ?: 0.35

    val rom     = NdsRom(inputPath)
    val version = GameVersion.fromGameId(rom.gameId)
        ?: error("Unknown game ID '${rom.gameId}'. Supported: ${GameVersion.entries.map { it.gameId }}")

    val statPath   = resolveFilePath(rom, version, "unitstat.dat")
    val statData   = rom.getFile(statPath)
    val stats      = UnitStatParser(version).parse(statData)
    val storyCount = StoryPlayers.byVersion[version]?.size ?: 0
    val realCount  = stats.count { it.maxTotal > 0 }

    val config     = RandomizerConfig(seed = seed, variance = variance)
    val randomized = StatRandomizer(config, version).randomize(stats)

    val newStatData = UnitStatSerializer(version).serialize(statData, randomized)
    val patchedRom  = rom.patchFile(statPath, newStatData)
    outputPath.writeBytes(patchedRom)

    println("ROM:      ${rom.title} (${rom.gameId}, $version)")
    println("Seed:     $seed")
    println("Variance: ±${(variance * 100).toInt()}% regular / ±${(config.storyVariance * 100).toInt()}% story")
    println("Players:  $realCount total, $storyCount with reduced variance (story)")
    println("Output:   $outputPath")
}

internal fun resolveFilePath(rom: NdsRom, version: GameVersion, filename: String): String {
    if (version == GameVersion.IE2_EN) {
        val enPath = "data_iz/logic/en/$filename"
        return if (rom.hasFile(enPath)) enPath else "data_iz/logic/sp/$filename"
    }
    return version.dataPath + filename
}
