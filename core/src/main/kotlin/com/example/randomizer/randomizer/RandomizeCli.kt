package com.example.randomizer.randomizer

import com.example.randomizer.data.GameVersion
import com.example.randomizer.data.UnitStatParser
import com.example.randomizer.data.UnitStatSerializer
import com.example.randomizer.data.resolveGameFilePath
import com.example.randomizer.rom.NdsRom
import kotlin.io.path.Path
import kotlin.io.path.writeBytes

fun main(args: Array<String>) {
    if (args.size < 2) {
        System.err.println(
            "Usage: ./gradlew :core:randomize --args=\"<input.nds> <output.nds> [seed] [variance] [mode]\"\n" +
            "  seed     — integer, omit for a random seed\n" +
            "  variance — decimal 0.0–0.99, default 0.35 (±35%)\n" +
            "  mode     — NOT_CHANGED | SHUFFLE | RANDOM_TOTALLY  (default: RANDOM_TOTALLY)"
        )
        return
    }

    val inputPath  = Path(args[0])
    val outputPath = Path(args[1])
    val seed       = args.getOrNull(2)?.toLong() ?: System.currentTimeMillis()
    val variance   = args.getOrNull(3)?.toDouble() ?: 0.35
    val mode       = args.getOrNull(4)
        ?.uppercase()
        ?.let { name -> StatMode.entries.find { it.name == name } }
        ?: StatMode.RANDOM_TOTALLY

    val rom     = NdsRom(inputPath)
    val version = GameVersion.fromGameId(rom.gameId)
        ?: error("Unknown game ID '${rom.gameId}'. Supported: ${GameVersion.entries.map { it.gameId }}")

    val statPath   = resolveGameFilePath(rom, version, "unitstat.dat")
    val statData   = rom.getFile(statPath)
    val stats      = UnitStatParser(version).parse(statData)
    val storyCount = StoryPlayers.byVersion[version]?.size ?: 0
    val realCount  = stats.count { it.maxTotal > 0 }

    val config     = RandomizerConfig(seed = seed, statMode = mode, variance = variance)
    val randomized = StatRandomizer(config, version).randomize(stats)

    val newStatData = UnitStatSerializer(version).serialize(statData, randomized)
    val patchedRom  = rom.patchFile(statPath, newStatData)
    outputPath.writeBytes(patchedRom)

    println("ROM:      ${rom.title} (${rom.gameId}, $version)")
    println("Seed:     $seed")
    println("Mode:     $mode")
    println("Variance: ±${(variance * 100).toInt()}% regular / ±${(config.storyVariance * 100).toInt()}% story")
    println("Players:  $realCount total, $storyCount with reduced variance (story)")
    println("Output:   $outputPath")
}
