package com.example.randomizer.randomizer

import com.example.randomizer.data.CommandDatParser
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
            "Usage: ./gradlew :core:randomize --args=\"<input.nds> <output.nds> [seed] [variance] " +
            "[statMode] [elementMode] [genderMode] [positionMode] [nameMode] [modelMode] " +
            "[matchModelAndName] [onlyIe1Characters] [moveMode] [samePositionMove] " +
            "[storyMoveProtection] [randomMoveLevel] [maxMoveLevel] [limitOfSkill] " +
            "[maxMoveLimit] [onlyIe1Moves] [onlyIe2Moves]\""
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
    val nameMode           = args.getOrNull(8)?.uppercase()
        ?.let { name -> NameMode.entries.find { it.name == name } } ?: NameMode.NOT_CHANGED
    val modelMode          = args.getOrNull(9)?.uppercase()
        ?.let { name -> ModelMode.entries.find { it.name == name } } ?: ModelMode.NOT_CHANGED
    val matchModelAndName  = args.getOrNull(10)?.lowercase() == "true"
    val onlyIe1Characters  = args.getOrNull(11)?.lowercase() == "true"
    val moveMode           = args.getOrNull(12)?.uppercase()
        ?.let { name -> MoveMode.entries.find { it.name == name } } ?: MoveMode.NOT_CHANGED
    val samePositionMove   = args.getOrNull(13)?.lowercase() == "true"
    val storyMoveProtection = args.getOrNull(14)?.lowercase() != "false"  // default true
    val randomMoveLevel    = args.getOrNull(15)?.lowercase() == "true"
    val maxMoveLevel       = args.getOrNull(16)?.toIntOrNull() ?: 99
    val limitOfSkill       = args.getOrNull(17)?.lowercase() == "true"
    val maxMoveLimit       = args.getOrNull(18)?.toIntOrNull() ?: Int.MAX_VALUE
    val onlyIe1Moves       = args.getOrNull(19)?.lowercase() == "true"
    val onlyIe2Moves       = args.getOrNull(20)?.lowercase() == "true"

    val rom     = NdsRom(inputPath)
    val version = GameVersion.fromGameId(rom.gameId)
        ?: error("Unknown game ID '${rom.gameId}'. Supported: ${GameVersion.entries.map { it.gameId }}")

    val config = RandomizerConfig(
        seed = seed, statMode = statMode, variance = variance,
        elementMode = elementMode, genderMode = genderMode,
        positionMode = positionMode, nameMode = nameMode,
        modelMode = modelMode, matchModelAndName = matchModelAndName,
        onlyIe1Characters = onlyIe1Characters,
        moveMode = moveMode, samePositionMove = samePositionMove,
        storyMoveProtection = storyMoveProtection, randomMoveLevel = randomMoveLevel,
        maxMoveLevel = maxMoveLevel, limitOfSkill = limitOfSkill,
        maxMoveLimit = maxMoveLimit, onlyIe1Moves = onlyIe1Moves, onlyIe2Moves = onlyIe2Moves
    )

    // — Stats + moves —
    val statPath    = resolveGameFilePath(rom, version, "unitstat.dat")
    val statData    = rom.getFile(statPath)
    val stats       = UnitStatParser(version).parse(statData)
    val storyCount  = StoryPlayers.byVersion[version]?.size ?: 0
    val realCount   = stats.count { it.maxTotal > 0 }

    val randomizedStats = if (moveMode != MoveMode.NOT_CHANGED) {
        val basePath2   = resolveGameFilePath(rom, version, "unitbase.dat")
        val players2    = UnitBaseParser(version).parse(rom.getFile(basePath2))
        val cmdPath     = resolveGameFilePath(rom, version, "command.dat")
        val calcPath    = resolveGameFilePath(rom, version, "unitcalc.dat")
        val calcData    = if (rom.hasFile(calcPath)) rom.getFile(calcPath) else null
        val movePool    = CommandDatParser(version.commandRecordSize).parse(rom.getFile(cmdPath), calcData)
        MoveRandomizer(config, version).randomize(
            StatRandomizer(config, version).randomize(stats), players2, movePool
        )
    } else {
        StatRandomizer(config, version).randomize(stats)
    }
    val newStatData = UnitStatSerializer(version).serialize(statData, randomizedStats)

    // — Element / Gender / Position / Names / Model —
    val basePath    = resolveGameFilePath(rom, version, "unitbase.dat")
    val baseData    = rom.getFile(basePath)
    val players     = UnitBaseParser(version).parse(baseData)
    val randomizedPlayers = ModelRandomizer(config, version)
        .randomize(NameRandomizer(config)
            .randomize(PositionRandomizer(config)
                .randomize(ElementGenderRandomizer(config).randomize(players))))
    val newBaseData = UnitBaseSerializer(version).serialize(baseData, randomizedPlayers)

    val patchedRom = rom.patchFile(basePath, newBaseData, rom.patchFile(statPath, newStatData))
    outputPath.writeBytes(patchedRom)

    println("ROM:      ${rom.title} (${rom.gameId}, $version)")
    println("Seed:     $seed")
    println("Stats:    $statMode  variance=±${(variance * 100).toInt()}% regular / ±${(config.storyVariance * 100).toInt()}% story  players=$realCount ($storyCount story)")
    println("Element:  $elementMode")
    println("Gender:   $genderMode")
    println("Position: $positionMode")
    println("Names:    $nameMode")
    println("Model:    $modelMode" +
        (if (modelMode != ModelMode.NOT_CHANGED && matchModelAndName) " [match name]" else "") +
        (if (modelMode != ModelMode.NOT_CHANGED && onlyIe1Characters) " [IE1 only]"   else ""))
    if (moveMode != MoveMode.NOT_CHANGED) {
        val flags = listOfNotNull(
            "same position".takeIf { samePositionMove },
            "story protected".takeIf { storyMoveProtection },
            "random level (max $maxMoveLevel)".takeIf { randomMoveLevel },
            "limit ≤ $maxMoveLimit".takeIf { limitOfSkill },
            "IE1 moves only".takeIf { onlyIe1Moves && !onlyIe2Moves },
            "IE2 moves only".takeIf { onlyIe2Moves && !onlyIe1Moves }
        ).joinToString(", ")
        println("Moves:    $moveMode" + if (flags.isNotEmpty()) " [$flags]" else "")
    }
    println("Output:   $outputPath")
}
