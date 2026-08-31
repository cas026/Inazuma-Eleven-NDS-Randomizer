package com.example.randomizer.randomizer

import com.example.randomizer.data.GameVersion
import com.example.randomizer.data.UnitBaseParser
import com.example.randomizer.data.UnitBaseSerializer
import com.example.randomizer.data.UnitStatParser
import com.example.randomizer.data.UnitStatSerializer
import com.example.randomizer.data.resolveGameFilePath
import com.example.randomizer.rom.NdsRom
import kotlin.io.path.Path
import kotlin.io.path.writeBytes

fun main(args: Array<String>) {
    if (args.size < 2) {
        System.err.println(
            "Usage: ./gradlew :core:randomize --args=\"<input.nds> <output.nds> [seed] [variance] [statMode] [elementMode] [genderMode] [positionMode]\"\n" +
            "  seed         — integer, omit for a random seed\n" +
            "  variance     — decimal 0.0–1.5, default 0.35 (±35%)\n" +
            "  statMode     — NOT_CHANGED | SHUFFLE | RANDOM_TOTALLY  (default: RANDOM_TOTALLY)\n" +
            "  elementMode  — NOT_CHANGED | REVERSE | RANDOM_TOTALLY  (default: NOT_CHANGED)\n" +
            "  genderMode   — NOT_CHANGED | REVERSE | RANDOM_TOTALLY  (default: NOT_CHANGED)\n" +
            "  positionMode — NOT_CHANGED | SHUFFLE | RANDOM_TOTALLY  (default: NOT_CHANGED)"
        )
        return
    }

    val inputPath    = Path(args[0])
    val outputPath   = Path(args[1])
    val seed         = args.getOrNull(2)?.toLong() ?: System.currentTimeMillis()
    val variance     = args.getOrNull(3)?.toDouble() ?: 0.35
    val statMode     = args.getOrNull(4)?.uppercase()
        ?.let { name -> StatMode.entries.find { it.name == name } } ?: StatMode.RANDOM_TOTALLY
    val elementMode  = args.getOrNull(5)?.uppercase()
        ?.let { name -> FieldMode.entries.find { it.name == name } } ?: FieldMode.NOT_CHANGED
    val genderMode   = args.getOrNull(6)?.uppercase()
        ?.let { name -> FieldMode.entries.find { it.name == name } } ?: FieldMode.NOT_CHANGED
    val positionMode = args.getOrNull(7)?.uppercase()
        ?.let { name -> PositionMode.entries.find { it.name == name } } ?: PositionMode.NOT_CHANGED
    val nameMode     = args.getOrNull(8)?.uppercase()
        ?.let { name -> NameMode.entries.find { it.name == name } } ?: NameMode.NOT_CHANGED

    val rom     = NdsRom(inputPath)
    val version = GameVersion.fromGameId(rom.gameId)
        ?: error("Unknown game ID '${rom.gameId}'. Supported: ${GameVersion.entries.map { it.gameId }}")

    val config = RandomizerConfig(
        seed = seed, statMode = statMode, variance = variance,
        elementMode = elementMode, genderMode = genderMode,
        positionMode = positionMode, nameMode = nameMode
    )

    // — Stats —
    val statPath    = resolveGameFilePath(rom, version, "unitstat.dat")
    val statData    = rom.getFile(statPath)
    val stats       = UnitStatParser(version).parse(statData)
    val storyCount  = StoryPlayers.byVersion[version]?.size ?: 0
    val realCount   = stats.count { it.maxTotal > 0 }
    val newStatData = UnitStatSerializer(version).serialize(statData, StatRandomizer(config, version).randomize(stats))

    // — Element / Gender / Position / Names —
    val basePath    = resolveGameFilePath(rom, version, "unitbase.dat")
    val baseData    = rom.getFile(basePath)
    val players     = UnitBaseParser(version).parse(baseData)
    val randomizedPlayers = NameRandomizer(config)
        .randomize(PositionRandomizer(config)
            .randomize(ElementGenderRandomizer(config).randomize(players)))
    val newBaseData = UnitBaseSerializer(version).serialize(baseData, randomizedPlayers)

    // Apply both patches — chain so second patch builds on top of first.
    val patchedRom = rom.patchFile(basePath, newBaseData, rom.patchFile(statPath, newStatData))
    outputPath.writeBytes(patchedRom)

    println("ROM:      ${rom.title} (${rom.gameId}, $version)")
    println("Seed:     $seed")
    println("Stats:    $statMode  variance=±${(variance * 100).toInt()}% regular / ±${(config.storyVariance * 100).toInt()}% story  players=$realCount ($storyCount story)")
    println("Element:  $elementMode")
    println("Gender:   $genderMode")
    println("Position: $positionMode")
    println("Names:    $nameMode")
    println("Output:   $outputPath")
}
