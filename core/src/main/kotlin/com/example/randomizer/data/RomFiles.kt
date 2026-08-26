package com.example.randomizer.data

import com.example.randomizer.rom.NdsRom

fun resolveGameFilePath(rom: NdsRom, version: GameVersion, filename: String): String {
    if (version == GameVersion.IE2_EN) {
        val enPath = "data_iz/logic/en/$filename"
        return if (rom.hasFile(enPath)) enPath else "data_iz/logic/sp/$filename"
    }
    return version.dataPath + filename
}
