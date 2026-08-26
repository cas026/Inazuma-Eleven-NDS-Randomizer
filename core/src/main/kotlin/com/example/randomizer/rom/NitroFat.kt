package com.example.randomizer.rom

class NitroFat(private val rom: ByteArray, private val fatOffset: Int, fatSize: Int) {
    val fileCount: Int = fatSize / 8

    fun range(fileId: Int): IntRange {
        val base  = fatOffset + fileId * 8
        val start = rom.readInt32LE(base)
        val end   = rom.readInt32LE(base + 4)
        return start until end
    }
}
