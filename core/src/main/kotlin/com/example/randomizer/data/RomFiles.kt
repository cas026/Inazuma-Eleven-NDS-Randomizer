package com.example.randomizer.data

import com.example.randomizer.rom.NdsRom

fun resolveGameFilePath(rom: NdsRom, version: GameVersion, filename: String): String =
    version.dataPath + filename
