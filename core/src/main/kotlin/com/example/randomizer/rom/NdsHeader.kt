package com.example.randomizer.rom

data class NdsHeader(
    val title: String,
    val gameId: String,
    val fntOffset: Int,
    val fntSize: Int,
    val fatOffset: Int,
    val fatSize: Int
) {
    companion object {
        fun parse(rom: ByteArray) = NdsHeader(
            title     = rom.readString(0x000, 12),
            gameId    = String(rom, 0x00C, 4, Charsets.US_ASCII),
            fntOffset = rom.readInt32LE(0x040),
            fntSize   = rom.readInt32LE(0x044),
            fatOffset = rom.readInt32LE(0x048),
            fatSize   = rom.readInt32LE(0x04C)
        )
    }
}
