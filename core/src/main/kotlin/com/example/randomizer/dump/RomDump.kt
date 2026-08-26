package com.example.randomizer.dump

import com.example.randomizer.rom.NdsRom
import kotlin.io.path.Path

fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "Usage: ./gradlew :core:dumpRom --args=\"/pad/naar/rom.nds\"" }
    val rom = NdsRom(Path(args[0]))
    println("Title:   ${rom.title}")
    println("Game ID: ${rom.gameId}")
    println()
    rom.listFiles().forEach { println(it) }
}
