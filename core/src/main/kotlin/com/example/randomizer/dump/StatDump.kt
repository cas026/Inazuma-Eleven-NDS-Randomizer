package com.example.randomizer.dump

import com.example.randomizer.data.*
import com.example.randomizer.rom.NdsRom
import kotlin.io.path.Path

fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "Usage: ./gradlew :core:dumpStats --args=\"/pad/naar/rom.nds\"" }

    val rom     = NdsRom(Path(args[0]))
    val version = GameVersion.fromGameId(rom.gameId)
        ?: error("Onbekende game ID '${rom.gameId}'. Ondersteund: ${GameVersion.entries.map { it.gameId }}")

    println("ROM:     ${rom.title}")
    println("Game ID: ${rom.gameId}  ($version)")
    println()

    val baseData = loadFile(rom, version, "unitbase.dat")
    val statData = loadFile(rom, version, "unitstat.dat")

    val bases = UnitBaseParser(version).parse(baseData)
    val stats = UnitStatParser(version).parse(statData)

    val players = (0 until minOf(bases.size, stats.size))
        .filter { stats[it].maxTotal > 0 }

    println("Spelers gevonden: ${players.size}\n")
    players.forEach { i -> printPlayer(i + 1, bases[i], stats[i]) }
}

private fun loadFile(rom: NdsRom, version: GameVersion, filename: String): ByteArray {
    if (version == GameVersion.IE2_EN) {
        return try { rom.getFile("data_iz/logic/en/$filename") }
               catch (_: Exception) { rom.getFile("data_iz/logic/sp/$filename") }
    }
    return rom.getFile(version.dataPath + filename)
}

private fun printPlayer(index: Int, base: UnitBase, stat: UnitStat) {
    val w = { n: Int, width: Int -> n.toString().padStart(width) }
    println("=== #$index ${base.fullName} (${base.nickname}) ===")
    println("  ${base.element?.label ?: "?"}  ${base.gender?.label ?: "?"}  size=${base.size}  pos=${base.position}")
    println("  FP      ${w(stat.fp.min,4)} – ${w(stat.fp.max,4)}  growth ${w(stat.fp.growthRate,5)}")
    println("  TP      ${w(stat.tp.min,4)} – ${w(stat.tp.max,4)}  growth ${w(stat.tp.growthRate,5)}")
    println("  Kick    ${w(stat.kick.min,4)} – ${w(stat.kick.max,4)}  growth ${w(stat.kick.growthRate,5)}")
    println("  Body    ${w(stat.body.min,4)} – ${w(stat.body.max,4)}  growth ${w(stat.body.growthRate,5)}")
    println("  Guard   ${w(stat.guard.min,4)} – ${w(stat.guard.max,4)}  growth ${w(stat.guard.growthRate,5)}")
    println("  Control ${w(stat.control.min,4)} – ${w(stat.control.max,4)}  growth ${w(stat.control.growthRate,5)}")
    println("  Speed   ${w(stat.speed.min,4)} – ${w(stat.speed.max,4)}  growth ${w(stat.speed.growthRate,5)}")
    println("  Guts    ${w(stat.guts.min,4)} – ${w(stat.guts.max,4)}  growth ${w(stat.guts.growthRate,5)}")
    println("  Stamina ${w(stat.stamina.min,4)} – ${w(stat.stamina.max,4)}  growth ${w(stat.stamina.growthRate,5)}")
    println("  MaxTotal: ${stat.maxTotal}")
    val moves = stat.moves.filter { !it.isEmpty }
    if (moves.isNotEmpty())
        println("  Moves: " + moves.joinToString("  ") { "0x${it.id.toString(16).uppercase().padStart(4,'0')} @lv${it.unlockLevel}" })
    println()
}
